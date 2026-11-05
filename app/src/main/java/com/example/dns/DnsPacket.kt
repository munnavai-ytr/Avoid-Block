package com.example.dns

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

data class ParsedDnsQuery(
    val id: Short,
    val flags: Int,
    val qname: String,
    val qtype: Int,
    val qclass: Int,
    val questionBytes: ByteArray
)

object DnsPacket {

    fun parseQuery(dnsPayload: ByteArray, offset: Int, length: Int): ParsedDnsQuery? {
        if (length < 12) return null
        val buffer = ByteBuffer.wrap(dnsPayload, offset, length)

        val id = buffer.short
        val flags = buffer.short.toInt() and 0xFFFF
        val qdcount = buffer.short.toInt() and 0xFFFF
        val ancount = buffer.short.toInt() and 0xFFFF
        val nscount = buffer.short.toInt() and 0xFFFF
        val arcount = buffer.short.toInt() and 0xFFFF

        // Only parse standard queries with at least 1 question
        if (qdcount < 1) return null

        val questionStart = buffer.position()
        val qnameBuilder = StringBuilder()
        var jumped = false
        var currentPos = questionStart

        // Parse QNAME
        while (buffer.hasRemaining()) {
            val labelLen = buffer.get().toInt() and 0xFF
            if (labelLen == 0) break
            if (labelLen > 63) {
                // Pointer or invalid format
                return null
            }
            if (!buffer.hasRemaining() || buffer.remaining() < labelLen) return null

            val labelBytes = ByteArray(labelLen)
            buffer.get(labelBytes)
            if (qnameBuilder.isNotEmpty()) qnameBuilder.append('.')
            qnameBuilder.append(String(labelBytes, Charsets.US_ASCII))
        }

        if (buffer.remaining() < 4) return null
        val qtype = buffer.short.toInt() and 0xFFFF
        val qclass = buffer.short.toInt() and 0xFFFF
        val questionEnd = buffer.position()

        val questionLen = questionEnd - questionStart
        val questionBytes = ByteArray(questionLen)
        System.arraycopy(dnsPayload, offset + questionStart, questionBytes, 0, questionLen)

        return ParsedDnsQuery(
            id = id,
            flags = flags,
            qname = qnameBuilder.toString().lowercase(),
            qtype = qtype,
            qclass = qclass,
            questionBytes = questionBytes
        )
    }

    /**
     * Builds standard blocked DNS sinkhole response (0.0.0.0 for A, :: for AAAA, or NXDOMAIN).
     */
    fun buildBlockedResponse(query: ParsedDnsQuery): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Transaction ID
        out.write((query.id.toInt() shr 8) and 0xFF)
        out.write(query.id.toInt() and 0xFF)

        // 2. Flags: Response, Opcode 0, Authoritative Answer, Recursion Desired, Recursion Available, RCODE=0 (NoError)
        val flags = 0x8180
        out.write((flags shr 8) and 0xFF)
        out.write(flags and 0xFF)

        // 3. QDCOUNT = 1
        out.write(0x00)
        out.write(0x01)

        // 4. ANCOUNT = 1 (Answer with sinkhole IP)
        out.write(0x00)
        out.write(0x01)

        // 5. NSCOUNT = 0
        out.write(0x00)
        out.write(0x00)

        // 6. ARCOUNT = 0
        out.write(0x00)
        out.write(0x00)

        // 7. Question Section (copied from original)
        out.write(query.questionBytes)

        // 8. Answer Section:
        // Name pointer to QNAME at byte 12 (0xC00C)
        out.write(0xC0)
        out.write(0x0C)

        // TYPE
        out.write((query.qtype shr 8) and 0xFF)
        out.write(query.qtype and 0xFF)

        // CLASS = IN (1)
        out.write(0x00)
        out.write(0x01)

        // TTL = 300 seconds
        out.write(0x00)
        out.write(0x00)
        out.write(0x01)
        out.write(0x2C)

        if (query.qtype == 28) {
            // AAAA record: 16 bytes sinkhole :: (0x00 * 16)
            out.write(0x00)
            out.write(0x10) // RDLENGTH = 16
            out.write(ByteArray(16))
        } else {
            // A record (or fallback): 4 bytes sinkhole 0.0.0.0
            out.write(0x00)
            out.write(0x04) // RDLENGTH = 4
            out.write(0x00)
            out.write(0x00)
            out.write(0x00)
            out.write(0x00)
        }

        return out.toByteArray()
    }
}
