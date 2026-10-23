package dev.shellguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.shellguard.ui.components.HelpCard
import dev.shellguard.ui.theme.MonoStyle
import dev.shellguard.ui.theme.Motion
import dev.shellguard.ui.theme.ShellGuardTheme
import dev.shellguard.ui.theme.Spacing
import dev.shellguard.ui.theme.statusColors
import kotlinx.coroutines.launch

class RecoveryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShellGuardTheme {
                RecoveryScreen()
            }
        }
    }
}

/** Media permissions differ by API level; pre-33 uses the legacy read flag. */
private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<RecoveryStore.TrashedItem>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var loading by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            requiredPermissions().any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    fun reload() {
        scope.launch {
            loading = true
            items = RecoveryStore.listTrashed(context)
            selected = emptySet()
            loading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasPermission = grants.values.any { it }
        if (hasPermission) reload()
    }

    // Restoring media owned by other apps needs the system consent dialog.
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) reload()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) reload()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.recovery_title)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selected.isNotEmpty(),
                enter = fadeIn(tween(Motion.FAST)),
                exit = fadeOut(tween(Motion.FAST)),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val uris = items.filter { it.id in selected }.map { it.uri }
                        RecoveryStore.buildRestoreRequest(context, uris)?.let { sender ->
                            restoreLauncher.launch(IntentSenderRequest.Builder(sender).build())
                        }
                    },
                    icon = { Icon(Icons.Default.Restore, contentDescription = null) },
                    text = { Text(stringResource(R.string.recovery_restore, selected.size)) },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = loading,
                enter = fadeIn(tween(Motion.FAST)),
                exit = fadeOut(tween(Motion.FAST)),
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            HelpCard(
                title = stringResource(R.string.recovery_help_title),
                body = stringResource(R.string.recovery_help_body),
            )
            HelpCard(
                title = stringResource(R.string.recovery_help_limits_title),
                body = stringResource(R.string.recovery_help_limits_body),
            )

            when {
                !RecoveryStore.isSupported -> CenteredMessage(
                    stringResource(R.string.recovery_unsupported)
                )

                !hasPermission -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        stringResource(R.string.recovery_permission_rationale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Button(onClick = { permissionLauncher.launch(requiredPermissions()) }) {
                        Text(stringResource(R.string.recovery_grant_permission))
                    }
                }

                items.isEmpty() && !loading -> CenteredMessage(
                    stringResource(R.string.recovery_empty)
                )

                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        TrashedItemRow(
                            item = item,
                            checked = item.id in selected,
                            onToggle = {
                                selected = if (item.id in selected) {
                                    selected - item.id
                                } else {
                                    selected + item.id
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Spacing.xl),
        )
    }
}

@Composable
private fun TrashedItemRow(
    item: RecoveryStore.TrashedItem,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val status = statusColors
    val days = daysUntil(item.expiresAt, System.currentTimeMillis())

    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .clickable(onClick = onToggle),
    ) {
        Row(
            Modifier
                .padding(Spacing.md)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })

            Icon(
                iconForMime(item.mimeType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )

            Spacer(Modifier.width(Spacing.md))

            Column(Modifier.weight(1f)) {
                Text(
                    item.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
                Text(
                    formatBytes(item.sizeBytes),
                    style = MonoStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (days != null) {
                    Text(
                        if (days == 0) {
                            stringResource(R.string.recovery_expires_today)
                        } else {
                            stringResource(R.string.recovery_expires_in, days)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (days <= 3) status.critical else status.warning,
                    )
                }
            }
        }
    }
}

private fun iconForMime(mime: String?): ImageVector = when {
    mime == null -> Icons.Default.InsertDriveFile
    mime.startsWith("image/") -> Icons.Default.Image
    mime.startsWith("video/") -> Icons.Default.Videocam
    mime.startsWith("audio/") -> Icons.Default.Audiotrack
    else -> Icons.Default.InsertDriveFile
}
