package dev.shellguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class FormattingTest {

    @Test
    fun `formats byte counts across unit boundaries`() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("1.0 KB", formatBytes(1024))
        assertEquals("1.5 KB", formatBytes(1536))
        assertEquals("1.0 MB", formatBytes(1024L * 1024))
        assertEquals("2.5 GB", formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `drops decimal above one hundred to keep the column narrow`() {
        assertEquals("500 KB", formatBytes(512_000))
    }

    @Test
    fun `negative size renders as placeholder rather than garbage`() {
        assertEquals("-", formatBytes(-1))
    }

    @Test
    fun `counts whole days until expiry`() {
        val now = 1_000_000_000_000L
        assertEquals(30, daysUntil(now + TimeUnit.DAYS.toMillis(30), now))
        assertEquals(1, daysUntil(now + TimeUnit.DAYS.toMillis(1), now))
    }

    @Test
    fun `partial day floors instead of rounding up`() {
        val now = 1_000_000_000_000L
        // 1.9 days left is "1 day", never "2": overstating time left on a
        // file about to be purged is the harmful direction to err.
        val oneAndAHalf = TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(22)
        assertEquals(1, daysUntil(now + oneAndAHalf, now))
    }

    @Test
    fun `expired and unknown deadlines are distinguishable`() {
        val now = 1_000_000_000_000L
        assertEquals(0, daysUntil(now - TimeUnit.DAYS.toMillis(5), now))
        assertNull(daysUntil(null, now))
    }
}
