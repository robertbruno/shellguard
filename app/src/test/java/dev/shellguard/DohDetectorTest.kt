package dev.shellguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DohDetectorTest {

    @Test
    fun `detects known DoH IPs`() {
        assertEquals("Cloudflare", DohDetector.isDohIp("1.1.1.1"))
        assertEquals("Cloudflare", DohDetector.isDohIp("1.0.0.1"))
        assertEquals("Google", DohDetector.isDohIp("8.8.8.8"))
        assertEquals("Google", DohDetector.isDohIp("8.8.4.4"))
        assertEquals("Quad9", DohDetector.isDohIp("9.9.9.9"))
        assertEquals("Quad9", DohDetector.isDohIp("149.112.112.112"))
        assertEquals("AdGuard", DohDetector.isDohIp("176.103.130.130"))
        assertEquals("AdGuard", DohDetector.isDohIp("176.103.130.131"))
        assertEquals("NextDNS", DohDetector.isDohIp("45.90.28.10"))
    }

    @Test
    fun `rejects non-DoH IPs`() {
        assertNull(DohDetector.isDohIp("142.250.184.46"))
        assertNull(DohDetector.isDohIp("192.168.1.1"))
        assertNull(DohDetector.isDohIp("10.0.0.2"))
    }

    @Test
    fun `CIDR range matching works`() {
        assertEquals(true, DohDetector.ipInRange("1.1.1.50", "1.1.1.0/24"))
        assertEquals(false, DohDetector.ipInRange("1.1.2.50", "1.1.1.0/24"))
        assertEquals(true, DohDetector.ipInRange("45.90.28.100", "45.90.28.0/24"))
        assertEquals(false, DohDetector.ipInRange("45.90.29.100", "45.90.28.0/24"))
    }
}
