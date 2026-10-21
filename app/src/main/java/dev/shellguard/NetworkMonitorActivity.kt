package dev.shellguard

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shellguard.ui.components.HelpCard
import dev.shellguard.ui.components.StatusPill
import dev.shellguard.ui.theme.MonoStyle
import dev.shellguard.ui.theme.Motion
import dev.shellguard.ui.theme.ShellGuardTheme
import dev.shellguard.ui.theme.Spacing
import dev.shellguard.ui.theme.statusColors

class NetworkMonitorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShellGuardTheme {
                NetworkMonitorScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkMonitorScreen() {
    val context = LocalContext.current
    val isRunning by NetworkMonitorService.isRunning.collectAsState()
    val connections by NetworkMonitorService.connectionLog.collectAsState()
    val listState = rememberLazyListState()

    // VpnService requires explicit user consent before establish() works.
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            context.startForegroundService(
                Intent(context, NetworkMonitorService::class.java).apply {
                    action = NetworkMonitorService.ACTION_START
                }
            )
        }
    }

    LaunchedEffect(connections.size) {
        if (connections.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.monitor_title)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HelpCard(
                title = stringResource(R.string.monitor_help_title),
                body = stringResource(R.string.monitor_help_body),
            )
            HelpCard(
                title = stringResource(R.string.monitor_help_doh_title),
                body = stringResource(R.string.monitor_help_doh_body),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Button(
                    onClick = {
                        val consent = VpnService.prepare(context)
                        if (consent != null) {
                            vpnPermissionLauncher.launch(consent)
                        } else {
                            context.startForegroundService(
                                Intent(context, NetworkMonitorService::class.java).apply {
                                    action = NetworkMonitorService.ACTION_START
                                }
                            )
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.monitor_start))
                }

                Button(
                    onClick = {
                        context.startService(
                            Intent(context, NetworkMonitorService::class.java).apply {
                                action = NetworkMonitorService.ACTION_STOP
                            }
                        )
                    },
                    enabled = isRunning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.monitor_stop))
                }
            }

            if (connections.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.monitor_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(Spacing.xl),
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.lg),
                ) {
                    items(connections.asReversed()) { conn ->
                        ConnectionRow(conn)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionRow(conn: NetworkMonitorService.ConnectionEvent) {
    val status = statusColors

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(tween(Motion.FAST)) + fadeIn(tween(Motion.FAST)),
        exit = fadeOut(tween(Motion.FAST)),
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        ) {
            Column(Modifier.padding(Spacing.md)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${conn.protocol}  ${conn.destIp}:${conn.destPort}",
                        style = MonoStyle,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatTimestamp(conn.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    stringResource(R.string.monitor_from, conn.sourceIp, conn.sourcePort),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    conn.appName ?: stringResource(R.string.monitor_unknown_app),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (conn.appName != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = Spacing.xs),
                )

                if (conn.blocked || conn.dohProvider != null) {
                    Row(
                        Modifier.padding(top = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        if (conn.blocked) {
                            StatusPill(
                                text = stringResource(R.string.monitor_blocked),
                                color = status.critical,
                                icon = Icons.Default.Block,
                            )
                        }
                        conn.dohProvider?.let { provider ->
                            StatusPill(
                                text = stringResource(R.string.monitor_doh, provider),
                                color = status.warning,
                                icon = Icons.Default.Lock,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(ts))
}
