package dev.shellguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocklistValidatorTest {

    @Test
    fun `accepts valid domains`() {
        assertTrue(isValidDomain("example.com"))
        assertTrue(isValidDomain("sub.example.co.uk"))
        assertTrue(isValidDomain("tracker-1.ads.net"))
    }

    @Test
    fun `rejects invalid domains`() {
        assertFalse(isValidDomain(""))
        assertFalse(isValidDomain("no-dots"))
        assertFalse(isValidDomain("a".repeat(254) + ".com"))
    }
}
