package dev.shellguard

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

data class TrackerMatch(
    val appPackage: String,
    val appName: String,
    val trackersFound: List<TrackerDb.Tracker>,
)

object DexScanner {

    suspend fun scanApp(context: Context, packageName: String): List<TrackerDb.Tracker> =
        withContext(Dispatchers.IO) {
            val apkPath = getApkPath(context, packageName) ?: return@withContext emptyList()
            val dexBytes = readAllDex(apkPath) ?: return@withContext emptyList()
            findTrackers(dexBytes)
        }

    private fun getApkPath(context: Context, packageName: String): String? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0),
            )
            appInfo.sourceDir
        } catch (_: Exception) {
            null
        }
    }

    private fun readAllDex(apkPath: String): ByteArray? {
        return try {
            ZipFile(File(apkPath)).use { zip ->
                val dexEntries = zip.entries().toList()
                    .filter { it.name.startsWith("classes") && it.name.endsWith(".dex") }
                    .sortedBy { it.name }
                val total = dexEntries.sumOf { it.size.toInt() }.coerceAtMost(200 * 1024 * 1024)
                val combined = ByteArray(total)
                var offset = 0
                for (entry in dexEntries) {
                    zip.getInputStream(entry).use { input ->
                        val bytes = input.readBytes()
                        System.arraycopy(bytes, 0, combined, offset, bytes.size)
                        offset += bytes.size
                    }
                }
                combined.copyOf(offset)
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun findTrackers(dexBytes: ByteArray): List<TrackerDb.Tracker> {
        // DEX stores class names as "Lcom/foo/Bar;" — search signature prefix
        return TrackerDb.TRACKERS.filter { tracker ->
            val needle = tracker.signature.replace('.', '/')
            containsBytes(dexBytes, needle)
        }
    }

    private fun containsBytes(haystack: ByteArray, needle: String): Boolean {
        val needleBytes = needle.toByteArray(Charsets.UTF_8)
        val maxOffset = haystack.size - needleBytes.size
        if (maxOffset < 0) return false

        var i = 0
        while (i <= maxOffset) {
            var match = true
            for (j in needleBytes.indices) {
                if (haystack[i + j] != needleBytes[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
            i++
        }
        return false
    }
}
