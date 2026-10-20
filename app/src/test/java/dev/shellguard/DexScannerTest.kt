package dev.shellguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DexScannerTest {

    @Test
    fun `finds known tracker signatures in fake dex content`() {
        val fakeDex = "Lcom/google/ads/AdView; Lcom/facebook/ads/Ad; Lcom/legit/app/Main;"
            .toByteArray()
        val found = DexScanner.findTrackers(fakeDex)
        assertEquals(2, found.size)
        assertTrue(found.any { it.name == "Google Ads" })
        assertTrue(found.any { it.name == "Facebook Ads" })
    }

    @Test
    fun `returns empty when no trackers present`() {
        val cleanDex = "Lcom/legit/app/Main; Landroid/app/Activity;".toByteArray()
        assertTrue(DexScanner.findTrackers(cleanDex).isEmpty())
    }
}
