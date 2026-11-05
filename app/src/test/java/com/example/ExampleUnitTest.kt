package com.example

import com.example.dns.DnsPacket
import com.example.dns.IpPacketHelper
import com.example.dns.ParsedIpPacket
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class ExampleUnitTest {

    @Test
    fun testDnsQueryParsingAndBlockedResponse() {
        // Construct a sample DNS query for "pornhub.com", Type A (1), Class IN (1)
        val out = ByteArrayOutputStream()
        // ID: 0x1234
        out.write(0x12)
        out.write(0x34)
        // Flags: Standard query (0x0100)
        out.write(0x01)
        out.write(0x00)
        // QDCOUNT: 1
        out.write(0x00)
        out.write(0x01)
        // ANCOUNT, NSCOUNT, ARCOUNT = 0
        out.write(0x00); out.write(0x00)
        out.write(0x00); out.write(0x00)
        out.write(0x00); out.write(0x00)

        // QNAME: 7 pornhub 3 com 0
        out.write(7)
        out.write("pornhub".toByteArray())
        out.write(3)
        out.write("com".toByteArray())
        out.write(0)

        // QTYPE: 1 (A)
        out.write(0x00); out.write(0x01)
        // QCLASS: 1 (IN)
        out.write(0x00); out.write(0x01)

        val queryData = out.toByteArray()
        val query = DnsPacket.parseQuery(queryData, 0, queryData.size)

        assertNotNull(query)
        assertEquals(0x1234.toShort(), query!!.id)
        assertEquals("pornhub.com", query.qname)
        assertEquals(1, query.qtype)

        // Build blocked response
        val response = DnsPacket.buildBlockedResponse(query)
        assertNotNull(response)
        assertTrue(response.size > queryData.size)

        // Verify response header has Response bit set
        val respFlags = ((response[2].toInt() and 0xFF) shl 8) or (response[3].toInt() and 0xFF)
        assertEquals(0x8180, respFlags)

        // Verify ANCOUNT = 1
        assertEquals(1, response[7].toInt())

        // Verify answer IP is 0.0.0.0
        val last4 = response.sliceArray(response.size - 4 until response.size)
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), last4)
    }

    @Test
    fun testIpPacketHelperUdpIpv4Response() {
        val dummyOriginal = ParsedIpPacket(
            isIpv6 = false,
            protocol = 17,
            srcIp = byteArrayOf(10, 0, 0, 2),
            dstIp = byteArrayOf(10, 254.toByte(), 1, 1),
            srcPort = 54321,
            dstPort = 53,
            transportPayloadOffset = 28,
            transportPayloadLength = 12
        )

        val dummyResponsePayload = byteArrayOf(1, 2, 3, 4, 5, 6)
        val packet = IpPacketHelper.buildUdpResponsePacket(dummyOriginal, dummyResponsePayload)

        assertNotNull(packet)
        // Check IPv4 version
        assertEquals(0x45.toByte(), packet[0])
        // Check protocol UDP (17)
        assertEquals(17.toByte(), packet[9])
        // Check src IP swapped to 10.254.1.1
        assertEquals(10.toByte(), packet[12])
        assertEquals(254.toByte(), packet[13].toInt().toByte())
        // Check dst IP swapped to 10.0.0.2
        assertEquals(10.toByte(), packet[16])
        assertEquals(2.toByte(), packet[19])
    }

    @Test
    fun testLauncherIconManagerConstants() {
        assertEquals("com.example.LauncherActivityAlias", com.example.core.security.LauncherIconManager.ALIAS_CLASS_NAME)
        assertEquals("purelock://open", com.example.core.security.LauncherIconManager.RECOVERY_DEEP_LINK)
        assertTrue(com.example.core.security.LauncherIconManager.ADB_LAUNCH_COMMAND.contains("com.purelock.app"))
        val notes = com.example.core.security.LauncherIconManager.getCompatibilityNotes()
        assertTrue(notes.contains("DONT_KILL_APP"))
        assertTrue(notes.contains("purelock://open"))
    }
}
