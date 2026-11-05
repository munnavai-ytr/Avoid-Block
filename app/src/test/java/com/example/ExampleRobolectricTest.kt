package com.example

import com.example.data.local.BundledAdultDomains
import org.junit.Assert.*
import org.junit.Test

class PureLockDomainLogicTest {

    @Test
    fun testBundledDomainMatching() {
        assertTrue(BundledAdultDomains.matchesBlockedDomain("pornhub.com", "pornhub.com"))
        assertTrue(BundledAdultDomains.matchesBlockedDomain("video.pornhub.com", "pornhub.com"))
        assertTrue(BundledAdultDomains.matchesBlockedDomain("cdn.video.pornhub.com", "pornhub.com"))
        assertFalse(BundledAdultDomains.matchesBlockedDomain("notpornhub.com", "pornhub.com"))
        assertFalse(BundledAdultDomains.matchesBlockedDomain("pornhub.com.br", "pornhub.com"))
        assertFalse(BundledAdultDomains.matchesBlockedDomain("google.com", "pornhub.com"))
    }

    @Test
    fun testDomainNormalization() {
        assertEquals("pornhub.com", BundledAdultDomains.normalizeDomain("https://pornhub.com/video/123"))
        assertEquals("pornhub.com", BundledAdultDomains.normalizeDomain("HTTP://PornHub.com:8080/"))
        assertEquals("chaturbate.com", BundledAdultDomains.normalizeDomain("  chaturbate.com.  "))
    }

    @Test
    fun testBundledBlocklistContainsCoreAdultDomains() {
        assertTrue(BundledAdultDomains.INITIAL_ADULT_DOMAINS.contains("pornhub.com"))
        assertTrue(BundledAdultDomains.INITIAL_ADULT_DOMAINS.contains("xvideos.com"))
        assertTrue(BundledAdultDomains.INITIAL_ADULT_DOMAINS.contains("chaturbate.com"))
        assertTrue(BundledAdultDomains.INITIAL_ADULT_DOMAINS.contains("onlyfans.com"))
        assertTrue(BundledAdultDomains.INITIAL_ADULT_DOMAINS.contains("redgifs.com"))
    }
}
