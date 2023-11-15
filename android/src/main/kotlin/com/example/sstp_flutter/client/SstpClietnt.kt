package com.example.sstp_flutter.client

import com.example.sstp_flutter.FlutterCaller
import com.example.sstp_flutter.cipher.sstp.HashSetting
import com.example.sstp_flutter.cipher.sstp.generateChapHLAK
import com.example.sstp_flutter.connectionStatus
import com.example.sstp_flutter.preference.OscPrefKey
import com.example.sstp_flutter.unit.ppp.option.AuthOptionMSChapv2
import com.example.sstp_flutter.unit.ppp.option.AuthOptionPAP
import com.example.sstp_flutter.unit.sstp.CERT_HASH_PROTOCOL_SHA1
import com.example.sstp_flutter.unit.sstp.CERT_HASH_PROTOCOL_SHA256
import com.example.sstp_flutter.unit.sstp.ControlPacket
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_ABORT
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
import com.example.sstp_flutter.unit.sstp.SstpCallAbort
import com.example.sstp_flutter.unit.sstp.SstpCallConnectAck
import com.example.sstp_flutter.unit.sstp.SstpCallConnectNak
import com.example.sstp_flutter.unit.sstp.SstpCallConnectRequest
import com.example.sstp_flutter.unit.sstp.SstpCallConnected
import com.example.sstp_flutter.unit.sstp.SstpCallDisconnect
import com.example.sstp_flutter.unit.sstp.SstpCallDisconnectAck
import com.example.sstp_flutter.unit.sstp.SstpEchoRequest
import com.example.sstp_flutter.unit.sstp.SstpEchoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


private const val SSTP_REQUEST_INTERVAL = 60_000L
private const val SSTP_REQUEST_COUNT = 3
internal const val SSTP_REQUEST_TIMEOUT = SSTP_REQUEST_INTERVAL * SSTP_REQUEST_COUNT

internal class SstpClient(val bridge: ClientBridge) {
    internal val mailbox = Channel<ControlPacket>(Channel.BUFFERED)

    private var jobRequest: Job? = null
    private var jobControl: Job? = null

    internal suspend fun launchJobControl() {
        jobControl = bridge.service.scope.launch(bridge.handler) {
            while (isActive) {
                when (mailbox.receive()) {
                    is SstpEchoRequest -> {
                        SstpEchoResponse().also {
                            bridge.sslTerminal!!.sendDataUnit(it)
                        }
                    }

                    is SstpEchoResponse -> { }

                    is SstpCallDisconnect -> {
//                        withContext(Dispatchers.Main) {
//                            println("before invoke")
//                            FlutterCaller().disconnect()
//                        }
                        bridge.controlMailbox.send(
                            ControlMessage(Where.SSTP_CONTROL, Result.ERR_DISCONNECT_REQUESTED)
                        )
                    }

                    is SstpCallAbort -> {
//                        withContext(Dispatchers.Main) {
//                            println("before invoke")
//
//                            FlutterCaller().disconnect()
//                        }
                        bridge.controlMailbox.send(
                            ControlMessage(Where.SSTP_CONTROL, Result.ERR_ABORT_REQUESTED)
                        )
                    }

                    else -> {
//                        withContext(Dispatchers.Main) {
//                            println("before invoke")
//                            FlutterCaller().disconnect()
//                        }
                        bridge.controlMailbox.send(
                            ControlMessage(Where.SSTP_CONTROL, Result.ERR_UNEXPECTED_MESSAGE)
                        )
                    }

                }
            }

        }
    }

    internal suspend fun launchJobRequest() {
        jobRequest = bridge.service.scope.launch(bridge.handler) {
            val request = SstpCallConnectRequest()
            var requestCount = SSTP_REQUEST_COUNT

            val received: ControlPacket
            while (true) {
                requestCount--
                if (requestCount < 0) {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_COUNT_EXHAUSTED)
                    )

                    return@launch
                }

                bridge.sslTerminal!!.sendDataUnit(request)

                received = withTimeoutOrNull(SSTP_REQUEST_INTERVAL) { mailbox.receive() } ?: continue

                break
            }

            when (received) {
                is SstpCallConnectAck -> {
                    bridge.hashProtocol = when (received.request.bitmask.toInt()) {
                        in 2..3 -> CERT_HASH_PROTOCOL_SHA256
                        1 -> CERT_HASH_PROTOCOL_SHA1
                        else -> {
                            bridge.controlMailbox.send(
                                ControlMessage(Where.SSTP_HASH, Result.ERR_UNKNOWN_TYPE)
                            )

                            return@launch
                        }
                    }

                    received.request.nonce.copyInto(bridge.nonce)

                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.PROCEEDED)
                    )
                }

                is SstpCallConnectNak -> {
//                   FlutterCaller().disconnect()
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_NEGATIVE_ACKNOWLEDGED)
                    )
                }

                is SstpCallDisconnect -> {
//                   FlutterCaller().disconnect()
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_DISCONNECT_REQUESTED)
                    )
                }

                is SstpCallAbort -> {
//                   FlutterCaller().disconnect()
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_ABORT_REQUESTED)
                    )
                }

                else -> {
//                   FlutterCaller().disconnect()
                    bridge.controlMailbox.send(
                        ControlMessage(Where.SSTP_REQUEST, Result.ERR_UNEXPECTED_MESSAGE)
                    )
                }
            }
        }
    }

    internal suspend fun sendCallConnected() {
        val call = SstpCallConnected()

        val cmkInputBuffer = ByteBuffer.allocate(32)
        val cmacInputBuffer = ByteBuffer.allocate(call.length)
        val hashSetting = HashSetting(bridge.hashProtocol)

        bridge.nonce.copyInto(call.binding.nonce)
        MessageDigest.getInstance(hashSetting.digestProtocol).also {
            it.digest(bridge.sslTerminal!!.getServerCertificate()).copyInto(call.binding.certHash)
        }

        call.binding.hashProtocol = bridge.hashProtocol
        call.write(cmacInputBuffer)

        val hlak = when (bridge.currentAuth) {
            is AuthOptionPAP -> ByteArray(32)
            is AuthOptionMSChapv2 -> generateChapHLAK(bridge.HOME_PASSWORD, bridge.chapMessage)
            else -> throw NotImplementedError(bridge.currentAuth.protocol.toString())
        }

        val cmkSeed = "SSTP inner method derived CMK".toByteArray(Charset.forName("US-ASCII"))
        cmkInputBuffer.put(cmkSeed)
        cmkInputBuffer.putShort(hashSetting.cmacSize)
        cmkInputBuffer.put(1)

        Mac.getInstance(hashSetting.macProtocol).also {
            it.init(SecretKeySpec(hlak, hashSetting.macProtocol))
            val cmk = it.doFinal(cmkInputBuffer.array())
            it.init(SecretKeySpec(cmk, hashSetting.macProtocol))
            val cmac = it.doFinal(cmacInputBuffer.array())
            cmac.copyInto(call.binding.compoundMac)
        }
        withContext(Dispatchers.Main) {
            FlutterCaller().connect()
            connectionStatus = OscPrefKey.Connected.name
        }
        bridge.sslTerminal!!.sendDataUnit(call)
    }

    internal suspend fun sendLastPacket(type: Short) {
        val packet = when (type) {
            SSTP_MESSAGE_TYPE_CALL_DISCONNECT ->{
                SstpCallDisconnect()
            }
            SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK ->{
                SstpCallDisconnectAck()
            }
            SSTP_MESSAGE_TYPE_CALL_ABORT ->{
                SstpCallAbort()
            }


            else -> {
                throw NotImplementedError(type.toString())
            }
        }

        try { // maybe the socket is no longer available
            bridge.sslTerminal!!.sendDataUnit(packet)
        } catch (_: Throwable) { }
    }

    internal fun cancel() {
        jobRequest?.cancel()
        jobControl?.cancel()
        mailbox.close()
    }
}
