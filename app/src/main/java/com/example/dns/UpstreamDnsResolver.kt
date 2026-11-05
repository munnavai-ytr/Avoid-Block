package com.example.dns

import android.net.VpnService
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UpstreamDnsResolver(
    private val vpnService: VpnService
) {
    fun resolve(
        dnsQueryPayload: ByteArray,
        upstreamIp: String,
        timeoutMs: Int = 3000
    ): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            // CRITICAL: Protect the upstream socket to bypass the VPN interface and avoid routing loops
            val protected = vpnService.protect(socket)
            if (!protected) {
                Log.w(TAG, "Failed to protect upstream DNS socket")
            }
            socket.soTimeout = timeoutMs

            val address = InetAddress.getByName(upstreamIp)
            val sendPacket = DatagramPacket(dnsQueryPayload, dnsQueryPayload.size, address, 53)
            socket.send(sendPacket)

            val receiveBuffer = ByteArray(1500)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)

            val response = ByteArray(receivePacket.length)
            System.arraycopy(receiveBuffer, 0, response, 0, receivePacket.length)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Upstream DNS query failed for $upstreamIp: ${e.message}")
            null
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore socket close exception
            }
        }
    }

    companion object {
        private const val TAG = "UpstreamDnsResolver"
    }
}
