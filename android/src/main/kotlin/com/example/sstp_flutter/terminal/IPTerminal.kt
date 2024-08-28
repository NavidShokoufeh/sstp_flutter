package com.example.sstp_flutter.terminal

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import com.example.sstp_flutter.FlutterCaller
import com.example.sstp_flutter.client.ClientBridge
import com.example.sstp_flutter.client.ControlMessage
import com.example.sstp_flutter.client.Result
import com.example.sstp_flutter.client.Where
import com.example.sstp_flutter.extension.isSame
import com.example.sstp_flutter.extension.toHexByteArray
import com.example.sstp_flutter.preference.OscPrefKey
import com.example.sstp_flutter.preference.accessor.getBooleanPrefValue
import com.example.sstp_flutter.preference.accessor.getStringPrefValue
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer


internal class IPTerminal(private val bridge: ClientBridge) {
    private var fd: ParcelFileDescriptor? = null

    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null

    private val isAppBasedRuleEnabled = bridge.allowedApps.isNotEmpty()
    private val isDefaultRouteAdded = getBooleanPrefValue(OscPrefKey.ROUTE_DO_ADD_DEFAULT_ROUTE, bridge.prefs)
    private val isPrivateAddressesRouted = getBooleanPrefValue(OscPrefKey.ROUTE_DO_ROUTE_PRIVATE_ADDRESSES, bridge.prefs)
    private val isCustomDNSServerUsed = getBooleanPrefValue(OscPrefKey.DNS_DO_USE_CUSTOM_SERVER, bridge.prefs)
    private val isCustomRoutesAdded = getBooleanPrefValue(OscPrefKey.ROUTE_DO_ADD_CUSTOM_ROUTES, bridge.prefs)

    private var totalDownload = 0;
    private var totalUpload = 0;

    @SuppressLint("NewApi")
    internal suspend fun initialize() {
        totalDownload = 0;
        totalUpload = 0;
        if (bridge.PPP_IPv4_ENABLED) {
            if (bridge.currentIPv4.isSame(ByteArray(4))) {
                bridge.controlMailbox.send(ControlMessage(Where.IPv4, Result.ERR_INVALID_ADDRESS))
                return
            }

            InetAddress.getByAddress(bridge.currentIPv4).also {
                bridge.builder.addAddress(it, 32)
            }

            if (isCustomDNSServerUsed) {
                bridge.builder.addDnsServer(getStringPrefValue(OscPrefKey.DNS_CUSTOM_ADDRESS, bridge.prefs))
            }

            if (!bridge.currentProposedDNS.isSame(ByteArray(4))) {
                InetAddress.getByAddress(bridge.currentProposedDNS).also {
                    bridge.builder.addDnsServer(it)
                }
            }

            setIPv4BasedRouting()
        }

        if (bridge.PPP_IPv6_ENABLED) {
            if (bridge.currentIPv6.isSame(ByteArray(8))) {
                bridge.controlMailbox.send(ControlMessage(Where.IPv6, Result.ERR_INVALID_ADDRESS))
                return
            }

            ByteArray(16).also { // for link local addresses
                "FE80".toHexByteArray().copyInto(it)
                ByteArray(6).copyInto(it, destinationOffset = 2)
                bridge.currentIPv6.copyInto(it, destinationOffset = 8)
                bridge.builder.addAddress(InetAddress.getByAddress(it), 64)
            }

            setIPv6BasedRouting()
        }

        if (isCustomRoutesAdded) {
            addCustomRoutes()
        }

        if (isAppBasedRuleEnabled) {
            addAppBasedRules()
        }

        bridge.builder.setMtu(bridge.PPP_MTU)
        bridge.builder.setBlocking(true)

        fd = bridge.builder.establish()!!.also {
            inputStream = FileInputStream(it.fileDescriptor)
            outputStream = FileOutputStream(it.fileDescriptor)
        }

        bridge.controlMailbox.send(ControlMessage(Where.IP, Result.PROCEEDED))
    }

    private fun setIPv4BasedRouting() {
        if (isDefaultRouteAdded) {
            bridge.builder.addRoute("0.0.0.0", 0)
        }

        if (isPrivateAddressesRouted) {
            bridge.builder.addRoute("10.0.0.0", 8)
            bridge.builder.addRoute("172.16.0.0", 12)
            bridge.builder.addRoute("192.168.0.0", 16)
        }
    }

    private fun setIPv6BasedRouting() {
        if (isDefaultRouteAdded) {
            bridge.builder.addRoute("::", 0)
        }

        if (isPrivateAddressesRouted) {
            bridge.builder.addRoute("fc00::", 7)
        }
    }

    @SuppressLint("NewApi")
    private fun addAppBasedRules() {
        bridge.allowedApps.forEach {
            bridge.builder.addAllowedApplication(it.packageName)
        }
    }

    private suspend fun addCustomRoutes(): Boolean {
        getStringPrefValue(OscPrefKey.ROUTE_CUSTOM_ROUTES, bridge.prefs).split("\n").filter { it.isNotEmpty() }.forEach {
            val parsed = it.split("/")
            if (parsed.size != 2) {
                bridge.controlMailbox.send(ControlMessage(Where.ROUTE, Result.ERR_PARSING_FAILED))
                return false
            }

            val address = parsed[0]
            val prefix = parsed[1].toIntOrNull()
            if (prefix == null){
                bridge.controlMailbox.send(ControlMessage(Where.ROUTE, Result.ERR_PARSING_FAILED))
                return false
            }

            try {
                bridge.builder.addRoute(address, prefix)
            } catch (_: IllegalArgumentException) {
                bridge.controlMailbox.send(ControlMessage(Where.ROUTE, Result.ERR_PARSING_FAILED))
                return false
            }
        }

        return true
    }

    internal suspend fun writePacket(start: Int, size: Int, buffer: ByteBuffer) {
        val startTimeMillis = System.currentTimeMillis()
        // nothing will be written until initialized
        // the position won't be changed
        outputStream?.write(buffer.array(), start, size)
        val endTimeMillis = System.currentTimeMillis()
        val elapsedTimeMillis = endTimeMillis - startTimeMillis

        // Calculate and log download speed based on the packet size and elapsed time
        if (elapsedTimeMillis > 0) {
            val downloadSpeedKb = calculateSpeedKb(size, elapsedTimeMillis)
            logDownloadSpeed(downloadSpeedKb, size)
        }
    }

    internal suspend fun readPacket(buffer: ByteBuffer) {
        val startTimeMillis = System.currentTimeMillis()
        buffer.clear()
        val bytesRead = inputStream?.read(buffer.array(), 0, bridge.PPP_MTU) ?: 0
        buffer.position(bytesRead)
        buffer.flip()

        val endTimeMillis = System.currentTimeMillis()
        val elapsedTimeMillis = endTimeMillis - startTimeMillis

        if (elapsedTimeMillis > 0) {
            val uploadSpeedKb = calculateSpeedKb(bytesRead, elapsedTimeMillis)
            logUploadSpeed(uploadSpeedKb, bytesRead)
        }
    }

    private fun calculateSpeedKb(bytes: Int, elapsedTimeMillis: Long): Double {
        val bitsInByte = 8
        val bytes = bytes.toDouble() // Convert bytes to double for calculations
        val bits = bytes * bitsInByte
        val speedKbps = bits / (elapsedTimeMillis.toDouble() / 1000) / 1_000
        return speedKbps
    }

    private suspend fun logDownloadSpeed(downloadSpeedKb: Double, bytes: Int) {
        val roundedDownloadSpeed = String.format("%.2f", downloadSpeedKb)
        totalDownload += bytes
        FlutterCaller().DownloadSpeed(bytes, totalDownload)
    }

    private suspend fun logUploadSpeed(uploadSpeedKb: Double, bytes: Int) {
        val roundedUploadSpeed = String.format("%.2f", uploadSpeedKb)
        totalUpload += bytes
        FlutterCaller().UploadSpeed(bytes, totalUpload)
    }


    internal fun close() {
        totalDownload = 0;
        totalUpload = 0;
        fd?.close()
    }
}
