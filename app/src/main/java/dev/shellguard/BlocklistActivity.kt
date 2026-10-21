package dev.shellguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.shellguard.ui.components.HelpCard
import dev.shellguard.ui.theme.MonoStyle
import dev.shellguard.ui.theme.Motion
import dev.shellguard.ui.theme.ShellGuardTheme
import dev.shellguard.ui.theme.Spacing

class BlocklistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShellGuardTheme {
                BlocklistScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistScreen() {
    val blocked by NetworkMonitorService.blockedDomains.collectAsState()
    var newDomain by remember { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    fun submit() {
        val domain = newDomain.trim().lowercase()
        errorRes = when {
            !isValidDomain(domain) -> R.string.blocklist_error_format
            blocked.contains(domain) -> R.string.blocklist_error_duplicate
            else -> {
                NetworkMonitorService.blockedDomains.value = blocked + domain
                newDomain = ""
                null
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.blocklist_title)) },
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
                title = stringResource(R.string.blocklist_help_title),
                body = stringResource(R.string.blocklist_help_body),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = newDomain,
                    onValueChange = {
                        newDomain = it
                        errorRes = null
                    },
                    label = { Text(stringResource(R.string.blocklist_domain_label)) },
                    isError = errorRes != null,
                    supportingText = {
                        AnimatedVisibility(
                            visible = errorRes != null,
                            enter = fadeIn(tween(Motion.FAST)),
                            exit = fadeOut(tween(Motion.FAST)),
                        ) {
                            errorRes?.let { Text(stringResource(it)) }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { submit() },
                    ),
                    modifier = Modifier.weight(1f),
                )

                Spacer(Modifier.width(Spacing.sm))

                Button(
                    onClick = { submit() },
                    modifier = Modifier.padding(top = Spacing.sm),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.blocklist_add))
                }
            }

            Text(
                stringResource(R.string.blocklist_count, blocked.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.lg),
            ) {
                items(blocked.sorted(), key = { it }) { domain ->
                    BlockedDomainRow(
                        domain = domain,
                        onRemove = {
                            NetworkMonitorService.blockedDomains.value = blocked - domain
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun BlockedDomainRow(domain: String, onRemove: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Row(
            Modifier
                .padding(start = Spacing.md, end = Spacing.sm)
                .heightIn(min = 48.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(domain, style = MonoStyle, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.blocklist_remove),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
