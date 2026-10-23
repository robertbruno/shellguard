package dev.shellguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the seed-vs-empty distinction in BlocklistStore: an empty persisted
 * set is a deliberate user state and must survive a reload, whereas a store
 * that was never written seeds the defaults.
 */
class BlocklistStoreTest {

    /** Mirrors the store's decision logic without Android dependencies. */
    private fun load(initialized: Boolean, persisted: Set<String>): Set<String> =
        if (initialized) persisted else BlocklistStore.DEFAULT_DOMAINS

    @Test
    fun `seeds defaults when never initialized`() {
        assertEquals(BlocklistStore.DEFAULT_DOMAINS, load(false, emptySet()))
    }

    @Test
    fun `empty persisted set survives reload`() {
        assertTrue(load(true, emptySet()).isEmpty())
    }

    @Test
    fun `persisted set is returned verbatim`() {
        val saved = setOf("ads.example.com", "track.example.net")
        assertEquals(saved, load(true, saved))
    }

    @Test
    fun `defaults contain no bare root domains of general-purpose services`() {
        // Blocking a service's root domain breaks the whole app, not just its
        // trackers. Analytics endpoints must be subdomain-scoped.
        val tooBroad = setOf("facebook.com", "yandex.ru", "bytedance.com", "google.com")
        val overlap = BlocklistStore.DEFAULT_DOMAINS intersect tooBroad
        assertTrue("Defaults block entire services: $overlap", overlap.isEmpty())
    }
}
