package com.example.sstp_flutter.client.control

import com.example.sstp_flutter.MainActivity
import com.example.sstp_flutter.client.ClientBridge
import com.example.sstp_flutter.client.ControlMessage
import com.example.sstp_flutter.client.OutgoingClient
import com.example.sstp_flutter.client.Result
import com.example.sstp_flutter.client.SSTP_REQUEST_TIMEOUT
import com.example.sstp_flutter.client.SstpClient
import com.example.sstp_flutter.client.Where
import com.example.sstp_flutter.client.incoming.IncomingClient
import com.example.sstp_flutter.client.ppp.ChapClient
import com.example.sstp_flutter.client.ppp.IpcpClient
import com.example.sstp_flutter.client.ppp.Ipv6cpClient
import com.example.sstp_flutter.client.ppp.LCPClient
import com.example.sstp_flutter.client.ppp.PAPClient
import com.example.sstp_flutter.client.ppp.PPPClient
import com.example.sstp_flutter.client.ppp.PPP_NEGOTIATION_TIMEOUT
import com.example.sstp_flutter.debug.assertAlways
import com.example.sstp_flutter.preference.OscPrefKey
import com.example.sstp_flutter.preference.accessor.getBooleanPrefValue
import com.example.sstp_flutter.preference.accessor.getIntPrefValue
import com.example.sstp_flutter.preference.accessor.resetReconnectionLife
import com.example.sstp_flutter.service.NOTIFICATION_ERROR_ID
import com.example.sstp_flutter.terminal.SSL_REQUEST_INTERVAL
import com.example.sstp_flutter.unit.ppp.option.AuthOptionMSChapv2
import com.example.sstp_flutter.unit.ppp.option.AuthOptionPAP
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_ABORT
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull


internal class ControlClient(internal val bridge: ClientBridge) {
    private var observer: NetworkObserver? = null

//    lateinit var _channel: MethodChannel
    private var sstpClient: SstpClient? = null
    private var pppClient: PPPClient? = null
    private var incomingClient: IncomingClient? = null
    private var outgoingClient: OutgoingClient? = null

    private var lcpClient: LCPClient? = null
    private var papClient: PAPClient? = null
    private var chapClient: ChapClient? = null
    private var ipcpClient: IpcpClient? = null
    private var ipv6cpClient: Ipv6cpClient? = null
    val mainActivity = MainActivity.instance

    private var jobMain: Job? = null

    private val mutex = Mutex()

    private val isReconnectionEnabled = getBooleanPrefValue(OscPrefKey.RECONNECTION_ENABLED, bridge.prefs)
    private val isReconnectionAvailable: Boolean
        get() = getIntPrefValue(OscPrefKey.RECONNECTION_LIFE, bridge.prefs) > 0

    private fun attachHandler() {
        bridge.handler = CoroutineExceptionHandler { _, throwable ->
            kill(isReconnectionEnabled) {
//                FlutterCaller().disconnect()
                val header = "OSC: ERR_UNEXPECTED"
                bridge.service.logWriter?.report(header + "\n" + throwable.stackTraceToString())
                bridge.service.makeNotification(NOTIFICATION_ERROR_ID, header)
            }
        }
    }

    internal fun launchJobMain() {
        attachHandler()

        jobMain = bridge.service.scope.launch(bridge.handler) {
            bridge.attachSSLTerminal()
            bridge.attachIPTerminal()


            bridge.sslTerminal!!.initialize()
            if (!expectProceeded(Where.SSL, SSL_REQUEST_INTERVAL)) {
                return@launch
            }


            IncomingClient(bridge).also {
                it.launchJobMain()
                incomingClient = it
            }


            SstpClient(bridge).also {
                sstpClient = it
                incomingClient!!.registerMailbox(it)
                it.launchJobRequest()

                if (!expectProceeded(Where.SSTP_REQUEST, SSTP_REQUEST_TIMEOUT)) {
                    return@launch
                }

                sstpClient!!.launchJobControl()
            }


            PPPClient(bridge).also {
                pppClient = it
                incomingClient!!.registerMailbox(it)
                it.launchJobControl()
            }


            LCPClient(bridge).also {
                incomingClient!!.registerMailbox(it)
                it.launchJobNegotiation()

                if (!expectProceeded(Where.LCP, PPP_NEGOTIATION_TIMEOUT)) {
                    return@launch
                }

                incomingClient!!.unregisterMailbox(it)
            }


            val authTimeout = getIntPrefValue(OscPrefKey.PPP_AUTH_TIMEOUT, bridge.prefs) * 1000L
            when (bridge.currentAuth) {
                is AuthOptionPAP -> PAPClient(bridge).also {
                    incomingClient!!.registerMailbox(it)
                    it.launchJobAuth()

                    if (!expectProceeded(Where.PAP, authTimeout)) {
                        return@launch
                    }

                    incomingClient!!.unregisterMailbox(it)
                }

                is AuthOptionMSChapv2 -> ChapClient(bridge).also {
                    chapClient = it
                    incomingClient!!.registerMailbox(it)
                    it.launchJobAuth()

                    if (!expectProceeded(Where.CHAP, authTimeout)) {
                        return@launch
                    }
                }

                else -> throw NotImplementedError(bridge.currentAuth.protocol.toString())
            }

            println("before send call connected")
            sstpClient!!.sendCallConnected()


            if (bridge.PPP_IPv4_ENABLED) {
                IpcpClient(bridge).also {
                    incomingClient!!.registerMailbox(it)
                    it.launchJobNegotiation()

                    if (!expectProceeded(Where.IPCP, PPP_NEGOTIATION_TIMEOUT)) {
                        return@launch
                    }

                    incomingClient!!.unregisterMailbox(it)
                }
            }


            if (bridge.PPP_IPv6_ENABLED) {
                Ipv6cpClient(bridge).also {
                    incomingClient!!.registerMailbox(it)
                    it.launchJobNegotiation()

                    if (!expectProceeded(Where.IPV6CP, PPP_NEGOTIATION_TIMEOUT)) {
                        return@launch
                    }

                    incomingClient!!.unregisterMailbox(it)
                }
            }


            bridge.ipTerminal!!.initialize()
            if (!expectProceeded(Where.IP, null)) {
                return@launch
            }


            OutgoingClient(bridge).also {
                it.launchJobMain()
                outgoingClient = it
            }


            observer = NetworkObserver(bridge)

            if (isReconnectionEnabled) {
                resetReconnectionLife(bridge.prefs)
            }


            expectProceeded(Where.SSTP_CONTROL, null) // wait ERR_ message until disconnection
        }
    }

    private suspend fun expectProceeded(where: Where, timeout: Long?): Boolean {
        val received = if (timeout != null) {
//            println("before invoke 2")
//            val tempMap : HashMap<String,Any>  = HashMap()
//
//            tempMap.put("disconnected",true)
//            tempMap.put("dns",dnsEnabled)
//            tempMap.put("dnsCount", dnsCount)
//            flutterChannel.invokeMethod("connectResponse", tempMap)
            withTimeoutOrNull(timeout) {
                bridge.controlMailbox.receive()
            } ?: ControlMessage(where, Result.ERR_TIMEOUT)

        } else {
            bridge.controlMailbox.receive()
        }

        if (received.result == Result.PROCEEDED) {
            assertAlways(received.from == where)

            return true
        }

        val lastPacketType = if (received.result == Result.ERR_DISCONNECT_REQUESTED) {
            SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
        } else {
            SSTP_MESSAGE_TYPE_CALL_ABORT
        }

        kill(isReconnectionEnabled) {
            sstpClient?.sendLastPacket(lastPacketType)

            val message = "${received.from.name}: ${received.result.name}"
            bridge.service.logWriter?.report(message)
            bridge.service.makeNotification(NOTIFICATION_ERROR_ID, message)
        }

        return false
    }

    internal fun disconnect() { // use if the user want to normally disconnect
        kill(false) {
            sstpClient?.sendLastPacket(SSTP_MESSAGE_TYPE_CALL_DISCONNECT)
        }
    }

    internal fun kill(isReconnectionRequested: Boolean, cleanup: (suspend () -> Unit)?) {
        if (!mutex.tryLock()) return

        bridge.service.scope.launch {
            observer?.close()

            jobMain?.cancel()
            cancelClients()

            cleanup?.invoke()

            closeTerminals()

            if (isReconnectionRequested && isReconnectionAvailable) {
                bridge.service.launchJobReconnect()
            } else {
                bridge.service.close()
            }
        }
    }

    private fun cancelClients() {
        lcpClient?.cancel()
        papClient?.cancel()
        chapClient?.cancel()
        ipcpClient?.cancel()
        ipv6cpClient?.cancel()
        sstpClient?.cancel()
        pppClient?.cancel()
        incomingClient?.cancel()
        outgoingClient?.cancel()
    }

    private fun closeTerminals() {
        bridge.sslTerminal?.close()
        bridge.ipTerminal?.close()
    }
}
