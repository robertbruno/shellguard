package dev.shellguard

object DohDetector {
    // Known DoH provider IP ranges
    // Sources: Cloudflare, Google, Quad9, AdGuard, NextDNS
    data class DohProvider(
        val name: String,
        val ipRangesV4: List<String>,
        val ipRangesV6: List<String> = emptyList(),
    )

    val PROVIDERS: List<DohProvider> = listOf(
        DohProvider(
            "Cloudflare",
            listOf("1.1.1.1", "1.0.0.1", "1.1.1.0/24", "1.0.0.0/24"),
            listOf("2606:4700:4700::1111", "2606:4700:4700::1001", "2606:4700:4700::/48")
        ),
        DohProvider(
            "Google",
            listOf("8.8.8.8", "8.8.4.4", "8.8.8.0/24", "8.8.4.0/24"),
            listOf("2001:4860:4860::8888", "2001:4860:4860::8844", "2001:4860:4860::/48")
        ),
        DohProvider(
            "Quad9",
            listOf("9.9.9.9", "149.112.112.112", "9.9.9.0/24", "149.112.112.0/24"),
            listOf("2620:fe::fe", "2620:fe::9", "2620:fe::/48")
        ),
        DohProvider(
            "AdGuard",
            listOf("176.103.130.130", "176.103.130.131", "176.103.130.0/24"),
            listOf("2a00:5a60::ad1:0ff", "2a00:5a60::ad2:0ff", "2a00:5a60::/48")
        ),
        DohProvider(
            "NextDNS",
            listOf("45.90.28.0/24", "45.90.30.0/24"),
            listOf("2a07:a8c0::/48", "2a07:a8c1::/48")
        ),
    )

    fun isDohIp(ip: String): String? {
        val isIpv6 = ip.contains(":")
        for (provider in PROVIDERS) {
            val ranges = if (isIpv6) provider.ipRangesV6 else provider.ipRangesV4
            for (range in ranges) {
                if (ipInRange(ip, range)) return provider.name
            }
        }
        return null
    }

    internal fun ipInRange(ip: String, range: String): Boolean {
        // Handle single IP
        if (!range.contains("/")) return normalizeIp(ip) == normalizeIp(range)

        // Handle CIDR
        val parts = range.split("/")
        if (parts.size != 2) return false

        return if (ip.contains(":")) {
            ipv6InRange(ip, parts[0], parts[1].toIntOrNull() ?: return false)
        } else {
            ipv4InRange(ip, parts[0], parts[1].toIntOrNull() ?: return false)
        }
    }

    private fun ipv4InRange(ip: String, rangeIp: String, prefix: Int): Boolean {
        val ipLong = ipv4ToLong(ip) ?: return false
        val rangeLong = ipv4ToLong(rangeIp) ?: return false
        val mask = if (prefix == 0) 0L else (-1L shl (32 - prefix))
        return (ipLong and mask) == (rangeLong and mask)
    }

    private fun ipv6InRange(ip: String, rangeIp: String, prefix: Int): Boolean {
        val ipBytes = ipv6ToBytes(ip) ?: return false
        val rangeBytes = ipv6ToBytes(rangeIp) ?: return false
        val prefixBytes = prefix / 8
        val prefixBits = prefix % 8

        for (i in 0 until prefixBytes) {
            if (ipBytes[i] != rangeBytes[i]) return false
        }

        if (prefixBits > 0 && prefixBytes < 16) {
            val mask = (0xFF shl (8 - prefixBits)) and 0xFF
            if ((ipBytes[prefixBytes].toInt() and mask) != (rangeBytes[prefixBytes].toInt() and mask)) {
                return false
            }
        }

        return true
    }

    private fun ipv4ToLong(ip: String): Long? {
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

    private fun ipv6ToBytes(ip: String): ByteArray? {
        return try {
            val addr = java.net.InetAddress.getByName(ip)
            if (addr is java.net.Inet6Address) addr.address else null
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeIp(ip: String): String {
        return try {
            java.net.InetAddress.getByName(ip).hostAddress ?: ip
        } catch (_: Exception) {
            ip
        }
    }
}
