package dev.shellguard

fun isValidDomain(domain: String): Boolean {
    if (domain.isEmpty()) return false
    if (domain.length > 253) return false
    if (domain.startsWith(".") || domain.endsWith(".")) return false
    if (domain.count { it == '.' } < 1) return false
    if (domain.contains("..")) return false
    return domain.all {
        it.isLetterOrDigit() || it == '.' || it == '-' || it == '_'
    }
}
