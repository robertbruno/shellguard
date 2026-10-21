package dev.shellguard

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shellguard.ui.components.HelpCard
import dev.shellguard.ui.components.StatusPill
import dev.shellguard.ui.theme.MonoStyle
import dev.shellguard.ui.theme.Motion
import dev.shellguard.ui.theme.ShellGuardTheme
import dev.shellguard.ui.theme.Spacing
import dev.shellguard.ui.theme.statusColors
import kotlinx.coroutines.launch

data class AppAudit(
    val name: String,
    val packageName: String,
    val isSystem: Boolean,
    val permissions: List<String>,
    val trackers: List<TrackerDb.Tracker> = emptyList(),
    val scanning: Boolean = false,
    val scanned: Boolean = false,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShellGuardTheme {
                ShellGuardApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellGuardApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppAudit>>(emptyList()) }
    var filter by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = auditInstalledApps(context.packageManager)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.audit_title)) },
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
            AnimatedVisibility(
                visible = scanning,
                enter = fadeIn(tween(Motion.FAST)),
                exit = fadeOut(tween(Motion.FAST)),
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            HelpCard(
                title = stringResource(R.string.audit_help_title),
                body = stringResource(R.string.audit_help_body),
            )

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text(stringResource(R.string.audit_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )

            Button(
                onClick = {
                    scope.launch {
                        scanning = true
                        apps = apps.map { app ->
                            if (app.isSystem) {
                                app
                            } else {
                                val trackers = DexScanner.scanApp(context, app.packageName)
                                app.copy(trackers = trackers, scanned = true)
                            }
                        }
                        scanning = false
                    }
                },
                enabled = !scanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm),
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    stringResource(
                        if (scanning) R.string.audit_scanning else R.string.audit_scan_trackers
                    )
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, NetworkMonitorActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Lan, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.nav_network_monitor))
                }

                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, BlocklistActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.nav_blocklist))
                }
            }

            val visible = apps.filter {
                filter.isEmpty() || it.name.contains(filter, ignoreCase = true)
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.lg),
            ) {
                items(visible, key = { it.packageName }) { app ->
                    AppRow(app)
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppAudit) {
    val status = statusColors

    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .animateContentSize(tween(Motion.MEDIUM)),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(app.name, style = MaterialTheme.typography.titleMedium)

            Text(
                app.packageName,
                style = MonoStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    text = stringResource(
                        if (app.isSystem) R.string.audit_system_app else R.string.audit_user_app
                    ),
                    color = status.neutral,
                    icon = null,
                )

                if (app.permissions.isNotEmpty()) {
                    StatusPill(
                        text = stringResource(R.string.audit_permissions_count, app.permissions.size),
                        color = status.info,
                        icon = null,
                    )
                }

                when {
                    app.trackers.isNotEmpty() -> StatusPill(
                        text = stringResource(R.string.audit_trackers_found, app.trackers.size),
                        color = status.critical,
                        icon = Icons.Default.Warning,
                    )

                    app.scanned -> StatusPill(
                        text = stringResource(R.string.audit_no_trackers),
                        color = status.ok,
                        icon = Icons.Default.CheckCircle,
                    )
                }
            }

            AnimatedVisibility(
                visible = app.trackers.isNotEmpty(),
                enter = slideInVertically(tween(Motion.MEDIUM)) + fadeIn(tween(Motion.MEDIUM)),
                exit = fadeOut(tween(Motion.FAST)),
            ) {
                Column(Modifier.padding(top = Spacing.sm)) {
                    HorizontalDivider()
                    app.trackers.forEach { tracker ->
                        Row(Modifier.padding(top = Spacing.xs)) {
                            Text(
                                tracker.name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                tracker.signature,
                                style = MonoStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun auditInstalledApps(pm: PackageManager): List<AppAudit> {
    return pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        .map { info ->
            val perms = try {
                pm.getPackageInfo(
                    info.packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_PERMISSIONS.toLong()
                    ),
                ).requestedPermissions?.toList() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            AppAudit(
                name = pm.getApplicationLabel(info).toString(),
                packageName = info.packageName,
                isSystem = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                permissions = perms,
            )
        }
        .sortedBy { it.name.lowercase() }
}
