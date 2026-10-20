package dev.shellguard

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class AppAudit(
    val name: String,
    val packageName: String,
    val isSystem: Boolean,
    val permissions: List<String>,
    val trackers: List<TrackerDb.Tracker> = emptyList(),
    val scanning: Boolean = false,
)

class MainActivity : ComponentActivity() {
    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShellGuardApp()
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ShellGuardApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppAudit>>(emptyList()) }
    var filter by remember { mutableStateOf("") }
    var scanProgress by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        apps = auditInstalledApps(context.packageManager)
    }

    MaterialTheme {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("ShellGuard") }) }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                scanProgress?.let {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(it, Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall)
                }

                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Buscar app") },
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )

                Button(
                    onClick = {
                        scope.launch {
                            scanProgress = "Escaneando trackers..."
                            apps = apps.map { app ->
                                if (app.isSystem) return@map app
                                val updated = app.copy(scanning = true)
                                val trackers = DexScanner.scanApp(context, app.packageName)
                                updated.copy(trackers = trackers, scanning = false)
                            }
                            val found = apps.count { it.trackers.isNotEmpty() }
                            scanProgress = null
                            android.util.Log.i("ShellGuard", "Scan complete: $found apps con trackers")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    enabled = scanProgress == null,
                ) {
                    Text("Escanear trackers")
                }

                LazyColumn(Modifier.fillMaxSize()) {
                    items(apps.filter {
                        filter.isEmpty() || it.name.contains(filter, true)
                    }) { app ->
                        AppRow(app)
                    }
                }
            }
        }
    }
}

@Composable
fun AppRow(app: AppAudit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(app.name, style = MaterialTheme.typography.titleMedium)
                if (app.scanning) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
            Text(
                if (app.isSystem) "Sistema" else "Usuario",
                style = MaterialTheme.typography.labelSmall,
            )
            if (app.permissions.isNotEmpty()) {
                Text(
                    "${app.permissions.size} permisos",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (app.trackers.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(
                    "⚠ ${app.trackers.size} tracker(s):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                app.trackers.forEach { tracker ->
                    Text(
                        "• ${tracker.name} (${tracker.signature})",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (!app.scanning && app.trackers.isEmpty()) {
                Text(
                    "✓ Sin trackers detectados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
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
