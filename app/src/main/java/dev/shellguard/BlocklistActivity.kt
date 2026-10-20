package dev.shellguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class BlocklistActivity : ComponentActivity() {
    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlocklistScreen()
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun BlocklistScreen() {
    val blocked by NetworkMonitorService.blockedDomains.collectAsState()
    var newDomain by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("Dominios bloqueados") }) }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = {
                            newDomain = it
                            error = null
                        },
                        label = { Text("Dominio") },
                        isError = error != null,
                        supportingText = { error?.let { Text(it) } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val domain = newDomain.trim().lowercase()
                            when {
                                !isValidDomain(domain) -> error = "Formato invalido"
                                blocked.contains(domain) -> error = "Ya existe"
                                else -> {
                                    NetworkMonitorService.blockedDomains.value =
                                        blocked + domain
                                    newDomain = ""
                                }
                            }
                        },
                    ) { Text("Agregar") }
                }

                Text(
                    "${blocked.size} dominios bloqueados",
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )

                LazyColumn(Modifier.fillMaxSize()) {
                    items(blocked.sorted()) { domain ->
                        BlockedDomainRow(
                            domain = domain,
                            onRemove = {
                                NetworkMonitorService.blockedDomains.value =
                                    blocked - domain
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedDomainRow(domain: String, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(domain, Modifier.weight(1f))
            TextButton(onClick = onRemove) { Text("Quitar") }
        }
    }
}
