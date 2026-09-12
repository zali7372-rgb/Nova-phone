package hu.nova.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalContext
import hu.nova.mobile.utils.PermissionUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import hu.nova.mobile.domain.model.VoiceState
import hu.nova.mobile.ui.NovaViewModelFactory
import hu.nova.mobile.ui.components.NovaOrb
import hu.nova.mobile.ui.components.VoiceStateIndicator

private val quickCommands = listOf(
    "Nyisd meg a YouTube-ot",
    "Nyisd meg a Wifi beállításokat",
    "Jegyezd meg, hogy szeretem az autókat",
    "Mit tudsz rólam"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModelFactory: NovaViewModelFactory) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
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

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            NovaOrb(voiceState = uiState.voiceState)
            Spacer(Modifier.height(12.dp))
            VoiceStateIndicator(voiceState = uiState.voiceState)

            Spacer(Modifier.height(28.dp))
            MicRow(
                voiceState = uiState.voiceState,
                onMicTapped = requestMicAndListen,
                onInterrupt = viewModel::onInterrupt
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = viewModel::onInputChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.home_input_placeholder)) },
                trailingIcon = {
                    androidx.compose.material3.IconButton(onClick = viewModel::sendTypedMessage) {
                        Icon(Icons.Filled.Send, contentDescription = stringResource(R.string.chat_send))
                    }
                },
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_quick_commands_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(quickCommands) { command ->
                    AssistChip(onClick = { viewModel.onQuickCommand(command) }, label = { Text(command) })
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_recent_conversation_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            uiState.recentMessages.takeLast(4).forEach { message ->
                Text(
                    text = "${if (message.sender.name == "USER") "Te" else "NOVA"}: ${message.text}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    // Errors are surfaced inline; auto-dismiss handled by ViewModel state.
                }
                Snackbar(modifier = Modifier.padding(top = 12.dp)) { Text(error) }
            }
        }
    }
}

@Composable
private fun MicRow(voiceState: VoiceState, onMicTapped: () -> Unit, onInterrupt: () -> Unit) {
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (voiceState == VoiceState.SPEAKING) {
            FloatingActionButton(onClick = onInterrupt) {
                Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.chat_send))
            }
        } else {
            FloatingActionButton(onClick = onMicTapped) {
                Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.home_mic_content_description))
            }
        }
    }
}
