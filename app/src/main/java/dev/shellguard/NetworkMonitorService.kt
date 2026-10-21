package dev.shellguard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class NetworkMonitorService : VpnService() {
    companion object {
        const val ACTION_START = "dev.shellguard.START_VPN"
        const val ACTION_STOP = "dev.shellguard.STOP_VPN"
        const val CHANNEL_ID = "network_monitor"
        const val NOTIFICATION_ID = 1

        val isRunning = MutableStateFlow(false)
        val connectionLog = MutableStateFlow<List<ConnectionEvent>>(emptyList())

        // Domains to block. ponytail: add when user-managed blocklist UI exists
        val blockedDomains = MutableStateFlow<Set<String>>(
            setOf(
                "doubleclick.net",
                "googleadservices.com",
                "googlesyndication.com",
                "facebook.com",
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
                "yandex.ru",
                "appboy.com",
                "onesignal.com",
                "kochava.com",
                "tapjoy.com",
                "vungle.com",
                "bytedance.com",
                "amazon-adsystem.com",
            )
        )
    }

    data class ConnectionEvent(
        val timestamp: Long,
        val sourceIp: String,
        val sourcePort: Int,
        val destIp: String,
        val destPort: Int,
        val protocol: String,
        val packageName: String?,
        val appName: String?,
        val blocked: Boolean = false,
        val isDoh: Boolean = false,
        val dohProvider: String? = null,
    )

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var packageManager: PackageManager

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        packageManager = applicationContext.packageManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning.value) return
        isRunning.value = true

        startForeground(NOTIFICATION_ID, createNotification())

        val builder = Builder()
            .setSession("ShellGuard")
            .addAddress("10.0.0.2", 32)
            .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
            .addDnsServer("8.8.8.8")
            .addDnsServer("2001:4860:4860::8888")
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .setMtu(1500)

        vpnInterface = builder.establish()

        vpnInterface?.let { pfd ->
            scope.launch { monitorTraffic(pfd) }
        }
    }

    private fun stopVpn() {
        isRunning.value = false
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun monitorTraffic(pfd: ParcelFileDescriptor) {
        withContext(Dispatchers.IO) {
            val input = FileInputStream(pfd.fileDescriptor)
            val output = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteArray(32767)

            while (isRunning.value) {
                try {
                    val length = input.read(buffer)
                    if (length > 0) {
                        val packet = ByteBuffer.wrap(buffer, 0, length)
                        parsePacket(packet, output)
                    }
                } catch (e: Exception) {
                    if (isRunning.value) Log.e("ShellGuard", "Read error", e)
                    break
                }
            }
        }
    }

    private fun parsePacket(buf: ByteBuffer, output: FileOutputStream) {
        if (buf.remaining() < 1) return

        buf.position(0)
        val version = buf.get().toInt() shr 4

        when (version) {
            4 -> parseIpv4Packet(buf, output)
            6 -> parseIpv6Packet(buf, output)
            else -> return
        }
    }

    private fun parseIpv4Packet(buf: ByteBuffer, output: FileOutputStream) {
        if (buf.remaining() < 20) return

        buf.position(0)
        val headerLength = (buf.get().toInt() and 0x0F) * 4
        buf.position(9)
        val protocol = buf.get().toInt() and 0xFF

        buf.position(12)
        val sourceIp = readIpv4(buf)
        val destIp = readIpv4(buf)

        if (protocol != OsConstants.IPPROTO_TCP && protocol != OsConstants.IPPROTO_UDP) return

        buf.position(headerLength)
        if (buf.remaining() < 4) return

        val sourcePort = buf.short.toInt() and 0xFFFF
        val destPort = buf.short.toInt() and 0xFFFF

        if (protocol == OsConstants.IPPROTO_UDP && destPort == 53) {
            val dnsPayload = ByteArray(buf.remaining())
            buf.get(dnsPayload)
            val response = handleDnsQuery(dnsPayload, sourceIp, sourcePort, destIp, 4)
            if (response != null) {
                output.write(response)
                output.flush()
            }
            return
        }

        logConnection(protocol, sourceIp, sourcePort, destIp, destPort)
    }

    private fun parseIpv6Packet(buf: ByteBuffer, output: FileOutputStream) {
        if (buf.remaining() < 40) return

        buf.position(0)
        buf.get() // version + traffic class high
        buf.get() // traffic class low + flow label high
        buf.get() // flow label mid
        buf.get() // flow label low
        buf.short // payload length
        val nextHeader = buf.get().toInt() and 0xFF // next header (protocol)
        buf.get() // hop limit

        val sourceIp = readIpv6(buf)
        val destIp = readIpv6(buf)

        if (nextHeader != OsConstants.IPPROTO_TCP && nextHeader != OsConstants.IPPROTO_UDP) return

        if (buf.remaining() < 4) return

        val sourcePort = buf.short.toInt() and 0xFFFF
        val destPort = buf.short.toInt() and 0xFFFF

        // DNS over IPv6 UDP port 53
        if (nextHeader == OsConstants.IPPROTO_UDP && destPort == 53) {
            val dnsPayload = ByteArray(buf.remaining())
            buf.get(dnsPayload)
            val response = handleDnsQuery(dnsPayload, sourceIp, sourcePort, destIp, 6)
            if (response != null) {
                output.write(response)
                output.flush()
            }
            return
        }

        logConnection(nextHeader, sourceIp, sourcePort, destIp, destPort)
    }

    private fun logConnection(protocol: Int, sourceIp: String, sourcePort: Int, destIp: String, destPort: Int) {
        val protocolName = if (protocol == OsConstants.IPPROTO_TCP) "TCP" else "UDP"
        val (pkgName, appLabel) = resolveAppName(protocol, sourceIp, sourcePort, destIp, destPort)

        val dohProvider = if (destPort == 443 || destPort == 853) DohDetector.isDohIp(destIp) else null

        val event = ConnectionEvent(
            timestamp = System.currentTimeMillis(),
            sourceIp = sourceIp,
            sourcePort = sourcePort,
            destIp = destIp,
            destPort = destPort,
            protocol = protocolName,
            packageName = pkgName,
            appName = appLabel,
            blocked = false,
            isDoh = dohProvider != null,
            dohProvider = dohProvider,
        )

        connectionLog.value = (connectionLog.value + event).takeLast(1000)
    }

    private fun readIpv4(buf: ByteBuffer): String {
        return "${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}.${buf.get().toInt() and 0xFF}"
    }

    private fun readIpv6(buf: ByteBuffer): String {
        val bytes = ByteArray(16)
        buf.get(bytes)
        val sb = StringBuilder()
        for (i in bytes.indices step 2) {
            if (i > 0) sb.append(":")
            val value = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            sb.append(value.toString(16))
        }
        return sb.toString()
    }

    private fun handleDnsQuery(query: ByteArray, clientIp: String, clientPort: Int, dnsServerIp: String, ipVersion: Int): ByteArray? {
        return try {
            val domain = extractDnsDomain(query) ?: return null
            val blocked = isBlockedDomain(domain)

            val (pkgName, appLabel) = resolveAppName(
                OsConstants.IPPROTO_UDP, clientIp, clientPort, dnsServerIp, 53
            )

            val event = ConnectionEvent(
                timestamp = System.currentTimeMillis(),
                sourceIp = clientIp,
                sourcePort = clientPort,
                destIp = dnsServerIp,
                destPort = 53,
                protocol = "DNS",
                packageName = pkgName,
                appName = appLabel,
                blocked = blocked,
            )
            connectionLog.value = (connectionLog.value + event).takeLast(1000)

            if (blocked) {
                Log.i("ShellGuard", "DNS bloqueado: $domain ($appLabel)")
                buildDnsResponse(query, dnsServerIp, clientIp, clientPort, ipVersion)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("ShellGuard", "DNS parse error", e)
            null
        }
    }

    private fun isBlockedDomain(domain: String): Boolean {
        val blocked = blockedDomains.value
        return blocked.any { domain == it || domain.endsWith(".$it") }
    }

    private fun extractDnsDomain(query: ByteArray): String? {
        if (query.size < 12) return null
        val flags = ((query[2].toInt() and 0xFF) shl 8) or (query[3].toInt() and 0xFF)
        val isQuery = flags shr 15 == 0
        if (!isQuery) return null

        val labels = mutableListOf<String>()
        var pos = 12
        while (pos < query.size) {
            val len = query[pos].toInt() and 0xFF
            if (len == 0) break
            if (len > 63 || pos + len >= query.size) return null
            labels.add(String(query, pos + 1, len, Charsets.UTF_8))
            pos += 1 + len
        }
        return labels.joinToString(".")
    }

    private fun buildDnsResponse(query: ByteArray, dnsServerIp: String, clientIp: String, clientPort: Int, ipVersion: Int): ByteArray {
        // Truncate query at end of question section
        var pos = 12
        while (pos < query.size) {
            val len = query[pos].toInt() and 0xFF
            if (len == 0) { pos++; break }
            pos += 1 + len
        }
        pos += 4 // QTYPE + QCLASS

        val header = ByteArray(12)
        System.arraycopy(query, 0, header, 0, 12)
        header[2] = ((header[2].toInt() and 0xFF) or 0x80).toByte() // QR=1 (response)
        header[3] = (header[3].toInt() or 0x03).toByte() // NXDOMAIN
        header[6] = 0; header[7] = 0 // ANCOUNT=0
        header[8] = 0; header[9] = 0 // NSCOUNT=0
        header[10] = 0; header[11] = 0 // ARCOUNT=0

        val question = query.copyOfRange(12, pos)
        val response = header + question

        return buildIpPacket(response, dnsServerIp, 53, clientIp, clientPort, ipVersion)
    }

    private fun buildIpPacket(payload: ByteArray, srcIp: String, srcPort: Int, destIp: String, destPort: Int, ipVersion: Int): ByteArray {
        val udpHeader = ByteArray(8)
        udpHeader[0] = (srcPort shr 8).toByte()
        udpHeader[1] = (srcPort and 0xFF).toByte()
        udpHeader[2] = (destPort shr 8).toByte()
        udpHeader[3] = (destPort and 0xFF).toByte()
        val udpLen = 8 + payload.size
        udpHeader[4] = (udpLen shr 8).toByte()
        udpHeader[5] = (udpLen and 0xFF).toByte()
        udpHeader[6] = 0; udpHeader[7] = 0

        return if (ipVersion == 6) {
            buildIpv6Packet(udpHeader + payload, srcIp, destIp)
        } else {
            buildIpv4Packet(udpHeader + payload, srcIp, destIp)
        }
    }

    private fun buildIpv4Packet(payload: ByteArray, srcIp: String, destIp: String): ByteArray {
        val ipHeader = ByteArray(20)
        ipHeader[0] = 0x45 // IPv4, IHL=5
        ipHeader[1] = 0
        val totalLen = 20 + payload.size
        ipHeader[2] = (totalLen shr 8).toByte()
        ipHeader[3] = (totalLen and 0xFF).toByte()
        ipHeader[4] = 0; ipHeader[5] = 0
        ipHeader[6] = 0x40; ipHeader[7] = 0 // DF flag
        ipHeader[8] = 64 // TTL
        ipHeader[9] = 17 // UDP

        val srcParts = srcIp.split(".").map { it.toInt() }
        val destParts = destIp.split(".").map { it.toInt() }
        srcParts.forEachIndexed { i, p -> ipHeader[12 + i] = p.toByte() }
        destParts.forEachIndexed { i, p -> ipHeader[16 + i] = p.toByte() }

        return ipHeader + payload
    }

    private fun buildIpv6Packet(payload: ByteArray, srcIp: String, destIp: String): ByteArray {
        val ipHeader = ByteArray(40)

        // Version (6) + traffic class (0) + flow label (0)
        ipHeader[0] = (6 shl 4).toByte()
        ipHeader[1] = 0
        ipHeader[2] = 0
        ipHeader[3] = 0

        // Payload length
        val payloadLen = payload.size
        ipHeader[4] = (payloadLen shr 8).toByte()
        ipHeader[5] = (payloadLen and 0xFF).toByte()

        ipHeader[6] = 17 // Next header: UDP
        ipHeader[7] = 64 // Hop limit

        // Source address (16 bytes)
        writeIpv6Address(ipHeader, 8, srcIp)
        // Destination address (16 bytes)
        writeIpv6Address(ipHeader, 24, destIp)

        return ipHeader + payload
    }

    private fun writeIpv6Address(buffer: ByteArray, offset: Int, ip: String) {
        val bytes = java.net.InetAddress.getByName(ip).address
        System.arraycopy(bytes, 0, buffer, offset, 16)
    }

    private fun resolveAppName(
        protocol: Int,
        localIp: String,
        localPort: Int,
        remoteIp: String,
        remotePort: Int,
    ): Pair<String?, String?> {
        return try {
            val localAddr = InetAddress.getByName(localIp)
            val remoteAddr = InetAddress.getByName(remoteIp)

            val localSock = InetSocketAddress(localAddr, localPort)
            val remoteSock = InetSocketAddress(remoteAddr, remotePort)

            val uid = if (android.os.Build.VERSION.SDK_INT >= 29) {
                connectivityManager.getConnectionOwnerUid(protocol, localSock, remoteSock)
            } else {
                android.os.Process.INVALID_UID
            }

            if (uid == android.os.Process.INVALID_UID) return null to null

            val packageName = packageManager.getNameForUid(uid) ?: return null to null
            val appInfo = try {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } catch (_: Exception) {
                null
            }
            val appLabel = appInfo?.let {
                packageManager.getApplicationLabel(it).toString()
            } ?: packageName

            packageName to appLabel
        } catch (e: Exception) {
            Log.w("ShellGuard", "UID resolution failed", e)
            null to null
        }
    }



    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, "Network Monitor", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)

        val intent = Intent(this, NetworkMonitorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ShellGuard Network Monitor")
            .setContentText("Monitoreando conexiones")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning.value = false
        vpnInterface?.close()
        scope.cancel()
    }
}
