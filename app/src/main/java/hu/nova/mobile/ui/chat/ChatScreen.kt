package hu.nova.mobile.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalContext
import hu.nova.mobile.utils.PermissionUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.nova.mobile.R
import hu.nova.mobile.ui.NovaViewModelFactory
import hu.nova.mobile.ui.components.VoiceStateIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModelFactory: NovaViewModelFactory) {
    val viewModel: ChatViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.onMicTapped() }

    val requestMicAndListen: () -> Unit = {
        if (PermissionUtils.hasMicrophonePermission(context)) {
            viewModel.onMicTapped()
        } else {
            micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.chat_title)) },
                actions = {
                    IconButton(onClick = viewModel::onNewConversation) {
                        Icon(Icons.Filled.AddComment, contentDescription = stringResource(R.string.chat_new_conversation))
                    }
                    IconButton(onClick = viewModel::onClearConversation) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.chat_clear_conversation))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            VoiceStateIndicator(voiceState = uiState.voiceState, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(32.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatMessageBubble(
                                message = message,
                                onDelete = viewModel::onDeleteMessage,
                                onRegenerate = viewModel::onRegenerate
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = requestMicAndListen) {
                    Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.home_mic_content_description))
                }
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.home_input_placeholder)) }
                )
                IconButton(onClick = viewModel::sendTypedMessage) {
                    Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.chat_send))
                }
            }
        }
    }
}
