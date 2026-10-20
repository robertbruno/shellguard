package dev.shellguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import kotlinx.coroutines.flow.collectLatest

class NetworkMonitorActivity : ComponentActivity() {
    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetworkMonitorScreen()
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun NetworkMonitorScreen() {
    val context = LocalContext.current
    val isRunning by NetworkMonitorService.isRunning.collectAsState()
    var connections by remember { mutableStateOf<List<NetworkMonitorService.ConnectionEvent>>(emptyList()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        NetworkMonitorService.connectionLog.collectLatest { events ->
            connections = events
            if (events.isNotEmpty()) {
                listState.animateScrollToItem(events.size - 1)
            }
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Network Monitor") }) }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                                action = NetworkMonitorService.ACTION_START
                            }
                            context.startForegroundService(intent)
                        },
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f),
                    ) { Text("Iniciar") }

                    Button(
                        onClick = {
                            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                                action = NetworkMonitorService.ACTION_STOP
                            }
                            context.startService(intent)
                        },
                        enabled = isRunning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                    ) { Text("Detener") }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(connections.reversed()) { conn ->
                        ConnectionRow(conn)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionRow(conn: NetworkMonitorService.ConnectionEvent) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        "${conn.protocol} ${conn.destIp}:${conn.destPort}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (conn.blocked) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "BLOQUEADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text(
                    "Desde ${conn.sourceIp}:${conn.sourcePort}",
                    style = MaterialTheme.typography.bodySmall,
                )
                conn.appName?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                conn.dohProvider?.let { provider ->
                    Text(
                        "DoH detectado: $provider",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Text(
                formatTimestamp(conn.timestamp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}
