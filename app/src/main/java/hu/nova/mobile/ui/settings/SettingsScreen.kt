package hu.nova.mobile.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import hu.nova.mobile.domain.model.AiProviderType
import hu.nova.mobile.domain.model.AppLanguage
import hu.nova.mobile.domain.model.AppThemeMode
import hu.nova.mobile.ui.NovaViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModelFactory: NovaViewModelFactory) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    var remoteApiKeyField by remember { mutableStateOf("") }
    var searchApiKeyField by remember { mutableStateOf("") }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle(stringResource(R.string.settings_ai_provider))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AiProviderType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = uiState.aiProviderType == type,
                        onClick = { viewModel.setAiProviderType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, AiProviderType.entries.size)
                    ) { Text(if (type == AiProviderType.LOCAL) "Local" else "Remote") }
                }
            }

            if (uiState.aiProviderType == AiProviderType.REMOTE) {
                OutlinedTextField(
                    value = remoteApiKeyField,
                    onValueChange = {
                        remoteApiKeyField = it
                        viewModel.setRemoteApiKey(it)
                    },
                    label = { Text("Remote AI API key (never stored in the app's source)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle(stringResource(R.string.settings_language))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { index, lang ->
                    SegmentedButton(
                        selected = uiState.language == lang,
                        onClick = { viewModel.setLanguage(lang) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size)
                    ) { Text(if (lang == AppLanguage.HUNGARIAN) "Magyar" else "English") }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle(stringResource(R.string.settings_theme))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppThemeMode.entries.size)
                    ) {
                        Text(
                            when (mode) {
                                AppThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                                AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                                AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                            }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle(stringResource(R.string.settings_voice))
            SettingsSwitchRow(
                label = stringResource(R.string.settings_wake_word),
                checked = uiState.wakeWordEnabled,
                onCheckedChange = viewModel::setWakeWordEnabled
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_continuous_conversation),
                checked = uiState.continuousConversation,
                onCheckedChange = viewModel::setContinuousConversation
            )
            SettingsSwitchRow(
                label = stringResource(R.string.settings_push_to_talk),
                checked = uiState.pushToTalk,
                onCheckedChange = viewModel::setPushToTalk
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle("Web search")
            OutlinedTextField(
                value = searchApiKeyField,
                onValueChange = {
                    searchApiKeyField = it
                    viewModel.setSearchApiKey(it)
                },
                label = { Text("Search provider API key (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle(stringResource(R.string.settings_privacy))
            Text(
                "A beszélgetéseid és a memóriád helyben, a telefonon tárolódnak. Csak akkor hagyják el a készüléket, ha távoli AI szolgáltatót vagy webes keresést használsz - ezeket te kapcsolod be.",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            SectionTitle(stringResource(R.string.settings_about))
            Text("NOVA Mobile - személyes AI asszisztens.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
