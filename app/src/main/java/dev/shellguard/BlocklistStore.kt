package dev.shellguard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists the blocked-domain set across process restarts.
 *
 * SharedPreferences is enough here: the payload is a flat string set read once
 * at startup and rewritten only on user edits. The in-memory StateFlow stays
 * the single source of truth for readers; disk is written behind it.
 */
object BlocklistStore {

    private const val PREFS_NAME = "blocklist"
    private const val KEY_DOMAINS = "domains"
    private const val KEY_INITIALIZED = "initialized"

    /** Tracker domains seeded on first launch. The user may remove any of them. */
    val DEFAULT_DOMAINS: Set<String> = setOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "graph.facebook.com",
        "appsflyer.com",
        "adjust.com",
        "mixpanel.com",
        "flurry.com",
        "applovin.com",
        "unityads.unity3d.com",
        "chartboost.com",
        "ironsrc.com",
        "adcolony.com",
        "inmobi.com",
        "startapp.com",
        "appboy.com",
        "onesignal.com",
        "kochava.com",
        "tapjoy.com",
        "vungle.com",
        "amazon-adsystem.com",
    )

    private val _domains = MutableStateFlow(DEFAULT_DOMAINS)
    val domains: StateFlow<Set<String>> = _domains

    private var prefs: SharedPreferences? = null

    /**
     * Loads the persisted set. Safe to call more than once; later calls are
     * no-ops once a store is attached.
     */
    fun init(context: Context) {
        if (prefs != null) return

        val p = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        // An empty set is a legitimate user choice (everything removed), so it
        // must not be mistaken for "never seeded". A separate flag disambiguates.
        _domains.value = if (p.getBoolean(KEY_INITIALIZED, false)) {
            p.getStringSet(KEY_DOMAINS, emptySet())?.toSet() ?: emptySet()
        } else {
            persist(DEFAULT_DOMAINS)
            DEFAULT_DOMAINS
        }
    }

    fun add(domain: String) {
        update(_domains.value + domain)
    }

    fun remove(domain: String) {
        update(_domains.value - domain)
    }

    fun resetToDefaults() {
        update(DEFAULT_DOMAINS)
    }

    private fun update(next: Set<String>) {
        _domains.value = next
        persist(next)
    }

    private fun persist(domains: Set<String>) {
        prefs?.edit()
            ?.putStringSet(KEY_DOMAINS, domains)
            ?.putBoolean(KEY_INITIALIZED, true)
            ?.apply()
    }
}
