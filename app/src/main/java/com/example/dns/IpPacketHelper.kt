package com.example.dns

import java.nio.ByteBuffer

data class ParsedIpPacket(
    val isIpv6: Boolean,
    val protocol: Int, // 17 for UDP, 6 for TCP
    val srcIp: ByteArray,
    val dstIp: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val transportPayloadOffset: Int,
    val transportPayloadLength: Int
)

object IpPacketHelper {

    fun parsePacket(packet: ByteArray, length: Int): ParsedIpPacket? {
        if (length < 28) return null // Need at least IPv4 (20) + UDP (8)
        val version = (packet[0].toInt() shr 4) and 0x0F

        if (version == 4) {
            val ihl = (packet[0].toInt() and 0x0F) * 4
            if (length < ihl + 8) return null
            val protocol = packet[9].toInt() and 0xFF

            if (protocol != 17) {
                // Not UDP
                return null
            }

            val srcIp = ByteArray(4)
            System.arraycopy(packet, 12, srcIp, 0, 4)
            val dstIp = ByteArray(4)
            System.arraycopy(packet, 16, dstIp, 0, 4)

            val udpOffset = ihl
            val srcPort = ((packet[udpOffset].toInt() and 0xFF) shl 8) or (packet[udpOffset + 1].toInt() and 0xFF)
            val dstPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or (packet[udpOffset + 3].toInt() and 0xFF)
            val udpLength = ((packet[udpOffset + 4].toInt() and 0xFF) shl 8) or (packet[udpOffset + 5].toInt() and 0xFF)

            val payloadOffset = udpOffset + 8
            val payloadLength = (udpLength - 8).coerceAtMost(length - payloadOffset)
            if (payloadLength < 0) return null

            return ParsedIpPacket(
                isIpv6 = false,
                protocol = protocol,
                srcIp = srcIp,
                dstIp = dstIp,
                srcPort = srcPort,
                dstPort = dstPort,
                transportPayloadOffset = payloadOffset,
                transportPayloadLength = payloadLength
            )
        } else if (version == 6) {
            if (length < 48) return null // IPv6 (40) + UDP (8)
            val nextHeader = packet[6].toInt() and 0xFF
            if (nextHeader != 17) return null

            val srcIp = ByteArray(16)
            System.arraycopy(packet, 8, srcIp, 0, 16)
            val dstIp = ByteArray(16)
            System.arraycopy(packet, 24, dstIp, 0, 16)

            val udpOffset = 40
            val srcPort = ((packet[udpOffset].toInt() and 0xFF) shl 8) or (packet[udpOffset + 1].toInt() and 0xFF)
            val dstPort = ((packet[udpOffset + 2].toInt() and 0xFF) shl 8) or (packet[udpOffset + 3].toInt() and 0xFF)
            val udpLength = ((packet[udpOffset + 4].toInt() and 0xFF) shl 8) or (packet[udpOffset + 5].toInt() and 0xFF)

            val payloadOffset = udpOffset + 8
            val payloadLength = (udpLength - 8).coerceAtMost(length - payloadOffset)
            if (payloadLength < 0) return null

            return ParsedIpPacket(
                isIpv6 = true,
                protocol = nextHeader,
                srcIp = srcIp,
                dstIp = dstIp,
                srcPort = srcPort,
                dstPort = dstPort,
                transportPayloadOffset = payloadOffset,
                transportPayloadLength = payloadLength
            )
        }

        return null
    }

    /**
     * Builds a response IP+UDP packet swapping src and dst IP/ports with the given response payload.
     */
    fun buildUdpResponsePacket(
        original: ParsedIpPacket,
        responsePayload: ByteArray
    ): ByteArray {
        val payloadLen = responsePayload.size
        val udpLen = 8 + payloadLen

        if (!original.isIpv6) {
            val totalLen = 20 + udpLen
            val packet = ByteArray(totalLen)
            val buffer = ByteBuffer.wrap(packet)

            // IPv4 Header
            buffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
            buffer.put(0x00.toByte()) // DSCP / ECN
            buffer.putShort(totalLen.toShort()) // Total length
            buffer.putShort(0x0001.toShort()) // Identification
            buffer.putShort(0x4000.toShort()) // Flags (Don't Fragment)
            buffer.put(64.toByte()) // TTL
            buffer.put(17.toByte()) // Protocol UDP
            buffer.putShort(0.toShort()) // Header checksum (initially 0)

            // Swap IP addresses (src -> dst, dst -> src)
            buffer.put(original.dstIp)
            buffer.put(original.srcIp)

            // Compute IPv4 header checksum
            val ipChecksum = computeIpChecksum(packet, 0, 20)
            packet[10] = ((ipChecksum shr 8) and 0xFF).toByte()
            packet[11] = (ipChecksum and 0xFF).toByte()

            // UDP Header
            buffer.putShort(original.dstPort.toShort()) // Source port (53)
            buffer.putShort(original.srcPort.toShort()) // Destination port
            buffer.putShort(udpLen.toShort())
            buffer.putShort(0.toShort()) // UDP Checksum (0 is valid in IPv4 UDP)

            // Payload
            buffer.put(responsePayload)

            return packet
        } else {
            // IPv6 Header (40 bytes)
            val totalLen = 40 + udpLen
            val packet = ByteArray(totalLen)
            val buffer = ByteBuffer.wrap(packet)

            buffer.putInt((0x60 shl 24) or (udpLen and 0xFFFF)) // Version 6, Payload length
            buffer.put(17.toByte()) // Next header (UDP)
            buffer.put(64.toByte()) // Hop limit

            // Swap IPv6 addresses
            buffer.put(original.dstIp)
            buffer.put(original.srcIp)

            // UDP Header
            buffer.putShort(original.dstPort.toShort())
            buffer.putShort(original.srcPort.toShort())
            buffer.putShort(udpLen.toShort())
            buffer.putShort(0.toShort())

            // Payload
            buffer.put(responsePayload)

            return packet
        }
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val high = data[i].toInt() and 0xFF
            val low = if (i + 1 < offset + length) data[i + 1].toInt() and 0xFF else 0
            val word = (high shl 8) or low
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
