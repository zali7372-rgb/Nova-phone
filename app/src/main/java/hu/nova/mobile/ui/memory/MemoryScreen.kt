package hu.nova.mobile.ui.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.nova.mobile.R
import hu.nova.mobile.domain.model.MemoryCategory
import hu.nova.mobile.ui.NovaViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(viewModelFactory: NovaViewModelFactory) {
    val viewModel: MemoryViewModel = viewModel(factory = viewModelFactory)
    val memories by viewModel.memories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.memory_title)) },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.memory_clear_all))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.memory_add))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (memories.isEmpty()) {
                Text(
                    text = stringResource(R.string.memory_empty_state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(memories, key = { it.id }) { memory ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(memory.content, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${categoryLabel(memory.category)} · ${dateFormatter.format(Date(memory.createdAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteMemory(memory) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.chat_delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { content, category ->
                viewModel.addMemory(content, category)
                showAddDialog = false
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.memory_clear_all)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) { Text(stringResource(R.string.memory_clear_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, MemoryCategory) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MemoryCategory.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.memory_add)) },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = false)
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    listOf(
                        MemoryCategory.PREFERENCE to stringResource(R.string.memory_category_preference),
                        MemoryCategory.FACT to stringResource(R.string.memory_category_fact),
                        MemoryCategory.OTHER to stringResource(R.string.memory_category_other)
                    ).forEach { (cat, label) ->
                        TextButton(onClick = { category = cat }) {
                            Text(label, color = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text, category) }) { Text(stringResource(R.string.memory_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
private fun categoryLabel(category: MemoryCategory): String = when (category) {
    MemoryCategory.PREFERENCE -> stringResource(R.string.memory_category_preference)
    MemoryCategory.FACT -> stringResource(R.string.memory_category_fact)
    MemoryCategory.OTHER -> stringResource(R.string.memory_category_other)
}
