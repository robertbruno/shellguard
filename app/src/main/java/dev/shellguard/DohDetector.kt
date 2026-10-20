package dev.shellguard

object DohDetector {
    // Known DoH provider IP ranges (IPv4 only for now)
    // Sources: Cloudflare, Google, Quad9, AdGuard, NextDNS
    data class DohProvider(
        val name: String,
        val ipRanges: List<String>,
    )

    val PROVIDERS: List<DohProvider> = listOf(
        DohProvider(
            "Cloudflare",
            listOf("1.1.1.1", "1.0.0.1", "1.1.1.0/24", "1.0.0.0/24")
        ),
        DohProvider(
            "Google",
            listOf("8.8.8.8", "8.8.4.4", "8.8.8.0/24", "8.8.4.0/24")
        ),
        DohProvider(
            "Quad9",
            listOf("9.9.9.9", "149.112.112.112", "9.9.9.0/24", "149.112.112.0/24")
        ),
        DohProvider(
            "AdGuard",
            listOf("176.103.130.130", "176.103.130.131", "176.103.130.0/24")
        ),
        DohProvider(
            "NextDNS",
            listOf("45.90.28.0/24", "45.90.30.0/24")
        ),
    )

    fun isDohIp(ip: String): String? {
        for (provider in PROVIDERS) {
            for (range in provider.ipRanges) {
                if (ipInRange(ip, range)) return provider.name
            }
        }
        return null
    }

    internal fun ipInRange(ip: String, range: String): Boolean {
        // Handle single IP
        if (!range.contains("/")) return ip == range

        // Handle CIDR
        val parts = range.split("/")
        if (parts.size != 2) return false

        val rangeIp = ipToLong(parts[0]) ?: return false
        val prefix = parts[1].toIntOrNull() ?: return false
        val ipLong = ipToLong(ip) ?: return false

        val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix))
        return (ipLong and mask) == (rangeIp and mask)
    }

    private fun ipToLong(ip: String): Long? {
        val parts = ip.split(".")
        if (parts.size != 4) return null

        var result = 0L
        for (part in parts) {
            val octet = part.toIntOrNull() ?: return null
            if (octet < 0 || octet > 255) return null
            result = (result shl 8) or octet.toLong()
        }
        return result
    }
}
