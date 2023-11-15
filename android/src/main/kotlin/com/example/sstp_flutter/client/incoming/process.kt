package com.example.sstp_flutter.client.incoming

import com.example.sstp_flutter.client.ControlMessage
import com.example.sstp_flutter.client.Result
import com.example.sstp_flutter.client.Where
import com.example.sstp_flutter.extension.move
import com.example.sstp_flutter.unit.DataUnit
import com.example.sstp_flutter.unit.ppp.CHAP_CODE_CHALLENGE
import com.example.sstp_flutter.unit.ppp.CHAP_CODE_FAILURE
import com.example.sstp_flutter.unit.ppp.CHAP_CODE_RESPONSE
import com.example.sstp_flutter.unit.ppp.CHAP_CODE_SUCCESS
import com.example.sstp_flutter.unit.ppp.ChapChallenge
import com.example.sstp_flutter.unit.ppp.ChapFailure
import com.example.sstp_flutter.unit.ppp.ChapResponse
import com.example.sstp_flutter.unit.ppp.ChapSuccess
import com.example.sstp_flutter.unit.ppp.IpcpConfigureAck
import com.example.sstp_flutter.unit.ppp.IpcpConfigureNak
import com.example.sstp_flutter.unit.ppp.IpcpConfigureReject
import com.example.sstp_flutter.unit.ppp.IpcpConfigureRequest
import com.example.sstp_flutter.unit.ppp.Ipv6cpConfigureAck
import com.example.sstp_flutter.unit.ppp.Ipv6cpConfigureNak
import com.example.sstp_flutter.unit.ppp.Ipv6cpConfigureReject
import com.example.sstp_flutter.unit.ppp.Ipv6cpConfigureRequest
import com.example.sstp_flutter.unit.ppp.LCPCodeReject
import com.example.sstp_flutter.unit.ppp.LCPConfigureAck
import com.example.sstp_flutter.unit.ppp.LCPConfigureNak
import com.example.sstp_flutter.unit.ppp.LCPConfigureReject
import com.example.sstp_flutter.unit.ppp.LCPConfigureRequest
import com.example.sstp_flutter.unit.ppp.LCPEchoReply
import com.example.sstp_flutter.unit.ppp.LCPEchoRequest
import com.example.sstp_flutter.unit.ppp.LCPProtocolReject
import com.example.sstp_flutter.unit.ppp.LCPTerminalAck
import com.example.sstp_flutter.unit.ppp.LCPTerminalRequest
import com.example.sstp_flutter.unit.ppp.LCP_CODE_CODE_REJECT
import com.example.sstp_flutter.unit.ppp.LCP_CODE_CONFIGURE_ACK
import com.example.sstp_flutter.unit.ppp.LCP_CODE_CONFIGURE_NAK
import com.example.sstp_flutter.unit.ppp.LCP_CODE_CONFIGURE_REJECT
import com.example.sstp_flutter.unit.ppp.LCP_CODE_CONFIGURE_REQUEST
import com.example.sstp_flutter.unit.ppp.LCP_CODE_DISCARD_REQUEST
import com.example.sstp_flutter.unit.ppp.LCP_CODE_ECHO_REPLY
import com.example.sstp_flutter.unit.ppp.LCP_CODE_ECHO_REQUEST
import com.example.sstp_flutter.unit.ppp.LCP_CODE_PROTOCOL_REJECT
import com.example.sstp_flutter.unit.ppp.LCP_CODE_TERMINATE_ACK
import com.example.sstp_flutter.unit.ppp.LCP_CODE_TERMINATE_REQUEST
import com.example.sstp_flutter.unit.ppp.LcpDiscardRequest
import com.example.sstp_flutter.unit.ppp.PAPAuthenticateAck
import com.example.sstp_flutter.unit.ppp.PAPAuthenticateNak
import com.example.sstp_flutter.unit.ppp.PAPAuthenticateRequest
import com.example.sstp_flutter.unit.ppp.PAP_CODE_AUTHENTICATE_ACK
import com.example.sstp_flutter.unit.ppp.PAP_CODE_AUTHENTICATE_NAK
import com.example.sstp_flutter.unit.ppp.PAP_CODE_AUTHENTICATE_REQUEST
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_ABORT
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECTED
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECT_ACK
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECT_NAK
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECT_REQUEST
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_ECHO_REQUEST
import com.example.sstp_flutter.unit.sstp.SSTP_MESSAGE_TYPE_ECHO_RESPONSE
import com.example.sstp_flutter.unit.sstp.SstpCallAbort
import com.example.sstp_flutter.unit.sstp.SstpCallConnectAck
import com.example.sstp_flutter.unit.sstp.SstpCallConnectNak
import com.example.sstp_flutter.unit.sstp.SstpCallConnectRequest
import com.example.sstp_flutter.unit.sstp.SstpCallConnected
import com.example.sstp_flutter.unit.sstp.SstpCallDisconnect
import com.example.sstp_flutter.unit.sstp.SstpCallDisconnectAck
import com.example.sstp_flutter.unit.sstp.SstpEchoRequest
import com.example.sstp_flutter.unit.sstp.SstpEchoResponse
import java.nio.ByteBuffer


private suspend fun IncomingClient.tryReadDataUnit(unit: DataUnit, buffer: ByteBuffer): Exception? {
    try {
        unit.read(buffer)
    } catch (e: Exception) { // need to save packet log
        bridge.controlMailbox.send(
            ControlMessage(Where.INCOMING, Result.ERR_PARSING_FAILED)
        )

        return e
    }

    return null
}

internal suspend fun IncomingClient.processControlPacket(type: Short, buffer: ByteBuffer): Boolean {
    val packet = when (type) {
        SSTP_MESSAGE_TYPE_CALL_CONNECT_REQUEST -> SstpCallConnectRequest()
        SSTP_MESSAGE_TYPE_CALL_CONNECT_ACK -> SstpCallConnectAck()
        SSTP_MESSAGE_TYPE_CALL_CONNECT_NAK -> SstpCallConnectNak()
        SSTP_MESSAGE_TYPE_CALL_CONNECTED -> SstpCallConnected()
        SSTP_MESSAGE_TYPE_CALL_ABORT -> SstpCallAbort()
        SSTP_MESSAGE_TYPE_CALL_DISCONNECT -> SstpCallDisconnect()
        SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK -> SstpCallDisconnectAck()
        SSTP_MESSAGE_TYPE_ECHO_REQUEST -> SstpEchoRequest()
        SSTP_MESSAGE_TYPE_ECHO_RESPONSE -> SstpEchoResponse()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.SSTP_CONTROL, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(packet, buffer)?.also {
        return false
    }

    sstpMailbox?.send(packet)

    return true
}

internal suspend fun IncomingClient.processLcpFrame(code: Byte, buffer: ByteBuffer): Boolean {
    if (code in 1..4) {
        val configureFrame = when (code) {
            LCP_CODE_CONFIGURE_REQUEST -> LCPConfigureRequest()
            LCP_CODE_CONFIGURE_ACK -> LCPConfigureAck()
            LCP_CODE_CONFIGURE_NAK -> LCPConfigureNak()
            LCP_CODE_CONFIGURE_REJECT -> LCPConfigureReject()
            else -> throw NotImplementedError(code.toString())
        }

        tryReadDataUnit(configureFrame, buffer)?.also {
            return false
        }

        lcpMailbox?.send(configureFrame)
        return true
    }

    if (code in 5..11) {
        val frame = when (code) {
            LCP_CODE_TERMINATE_REQUEST -> LCPTerminalRequest()
            LCP_CODE_TERMINATE_ACK -> LCPTerminalAck()
            LCP_CODE_CODE_REJECT -> LCPCodeReject()
            LCP_CODE_PROTOCOL_REJECT -> LCPProtocolReject()
            LCP_CODE_ECHO_REQUEST -> LCPEchoRequest()
            LCP_CODE_ECHO_REPLY -> LCPEchoReply()
            LCP_CODE_DISCARD_REQUEST -> LcpDiscardRequest()
            else -> throw NotImplementedError(code.toString())
        }

        tryReadDataUnit(frame, buffer)?.also {
            return false
        }

        pppMailbox?.send(frame)
        return true
    }

    bridge.controlMailbox.send(
        ControlMessage(Where.LCP, Result.ERR_UNKNOWN_TYPE)
    )

    return false
}

internal suspend fun IncomingClient.processPAPFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        PAP_CODE_AUTHENTICATE_REQUEST -> PAPAuthenticateRequest()
        PAP_CODE_AUTHENTICATE_ACK -> PAPAuthenticateAck()
        PAP_CODE_AUTHENTICATE_NAK -> PAPAuthenticateNak()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.PAP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    papMailbox?.send(frame)
    return true
}

internal suspend fun IncomingClient.processChapFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        CHAP_CODE_CHALLENGE -> ChapChallenge()
        CHAP_CODE_RESPONSE -> ChapResponse()
        CHAP_CODE_SUCCESS -> ChapSuccess()
        CHAP_CODE_FAILURE -> ChapFailure()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.CHAP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    chapMailbox?.send(frame)
    return true
}

internal suspend fun IncomingClient.processIpcpFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        LCP_CODE_CONFIGURE_REQUEST -> IpcpConfigureRequest()
        LCP_CODE_CONFIGURE_ACK -> IpcpConfigureAck()
        LCP_CODE_CONFIGURE_NAK -> IpcpConfigureNak()
        LCP_CODE_CONFIGURE_REJECT -> IpcpConfigureReject()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.IPCP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    ipcpMailbox?.send(frame)
    return true
}

internal suspend fun IncomingClient.processIpv6cpFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        LCP_CODE_CONFIGURE_REQUEST -> Ipv6cpConfigureRequest()
        LCP_CODE_CONFIGURE_ACK -> Ipv6cpConfigureAck()
        LCP_CODE_CONFIGURE_NAK -> Ipv6cpConfigureNak()
        LCP_CODE_CONFIGURE_REJECT -> Ipv6cpConfigureReject()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.IPV6CP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    ipv6cpMailbox?.send(frame)
    return true
}

internal suspend fun IncomingClient.processUnknownProtocol(protocol: Short, packetSize: Int, buffer: ByteBuffer): Boolean {
    LCPProtocolReject().also {
        it.rejectedProtocol = protocol
        it.id = bridge.allocateNewFrameID()
        val infoStart = buffer.position() + 8
        val infoStop = buffer.position() + packetSize
        it.holder = buffer.array().sliceArray(infoStart until infoStop)

        bridge.sslTerminal!!.sendDataUnit(it)
    }

    buffer.move(packetSize)

    return true
}

internal suspend fun IncomingClient.processIPPacket(isEnabledProtocol: Boolean, packetSize: Int, buffer: ByteBuffer) {
    if (isEnabledProtocol) {
        val start = buffer.position() + 8
        val ipPacketSize = packetSize - 8

        bridge.ipTerminal!!.writePacket(start, ipPacketSize, buffer)
    }

    buffer.move(packetSize)
}
