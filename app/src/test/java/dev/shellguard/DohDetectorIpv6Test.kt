package dev.shellguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DohDetectorIpv6Test {

    @Test
    fun `detects IPv6 DoH addresses`() {
        assertEquals("Cloudflare", DohDetector.isDohIp("2606:4700:4700::1111"))
        assertEquals("Cloudflare", DohDetector.isDohIp("2606:4700:4700::1001"))
        assertEquals("Google", DohDetector.isDohIp("2001:4860:4860::8888"))
        assertEquals("Google", DohDetector.isDohIp("2001:4860:4860::8844"))
        assertEquals("Quad9", DohDetector.isDohIp("2620:fe::fe"))
        assertEquals("Quad9", DohDetector.isDohIp("2620:fe::9"))
        assertEquals("AdGuard", DohDetector.isDohIp("2a00:5a60::ad1:0ff"))
        assertEquals("AdGuard", DohDetector.isDohIp("2a00:5a60::ad2:0ff"))
        assertEquals("NextDNS", DohDetector.isDohIp("2a07:a8c0::"))
    }

    @Test
    fun `rejects non-DoH IPv6 addresses`() {
        assertNull(DohDetector.isDohIp("2001:db8::1"))
        assertNull(DohDetector.isDohIp("fe80::1"))
        assertNull(DohDetector.isDohIp("fd00:1:fd00:1:fd00:1:fd00:1"))
    }

    @Test
    fun `IPv6 CIDR matching works`() {
        assertEquals(true, DohDetector.ipInRange("2606:4700:4700::1", "2606:4700:4700::/48"))
        assertEquals(false, DohDetector.ipInRange("2606:4701:4700::1", "2606:4700:4700::/48"))
        assertEquals(true, DohDetector.ipInRange("2001:4860:4860::1", "2001:4860:4860::/48"))
        assertEquals(false, DohDetector.ipInRange("2001:4860:4861::1", "2001:4860:4860::/48"))
    }
}
