package hu.nova.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.nova.mobile.data.repository.SettingsRepository
import hu.nova.mobile.domain.model.AiProviderType
import hu.nova.mobile.domain.model.AppLanguage
import hu.nova.mobile.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val aiProviderType: AiProviderType = AiProviderType.LOCAL,
    val language: AppLanguage = AppLanguage.HUNGARIAN,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val wakeWordEnabled: Boolean = false,
    val continuousConversation: Boolean = true,
    val pushToTalk: Boolean = false,
    /** Saved values, surfaced here so the Settings screen can show what's already stored
     * instead of a field that always starts blank and looks like the key "disappeared". */
    val remoteApiKey: String = "",
    val remoteApiBaseUrl: String = "",
    val searchApiKey: String = ""
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private data class CoreSettings(
        val provider: AiProviderType,
        val language: AppLanguage,
        val theme: AppThemeMode,
        val wakeWord: Boolean,
        val continuous: Boolean
    )

    private data class ApiSettings(
        val pushToTalk: Boolean,
        val remoteApiKey: String,
        val remoteApiBaseUrl: String,
        val searchApiKey: String
    )

    private val coreSettings: Flow<CoreSettings> = combine(
        settingsRepository.aiProviderType,
        settingsRepository.language,
        settingsRepository.themeMode,
        settingsRepository.wakeWordEnabled,
        settingsRepository.continuousConversation
    ) { provider, language, theme, wakeWord, continuous ->
        CoreSettings(provider, language, theme, wakeWord, continuous)
    }

    private val apiSettings: Flow<ApiSettings> = combine(
        settingsRepository.pushToTalk,
        settingsRepository.remoteApiKey,
        settingsRepository.remoteApiBaseUrl,
        settingsRepository.searchApiKey
    ) { pushToTalk, remoteApiKey, remoteApiBaseUrl, searchApiKey ->
        ApiSettings(pushToTalk, remoteApiKey, remoteApiBaseUrl, searchApiKey)
    }

    val uiState: StateFlow<SettingsUiState> = combine(coreSettings, apiSettings) { core, api ->
        SettingsUiState(
            aiProviderType = core.provider,
            language = core.language,
            themeMode = core.theme,
            wakeWordEnabled = core.wakeWord,
            continuousConversation = core.continuous,
            pushToTalk = api.pushToTalk,
            remoteApiKey = api.remoteApiKey,
            remoteApiBaseUrl = api.remoteApiBaseUrl,
            searchApiKey = api.searchApiKey
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setAiProviderType(type: AiProviderType) = viewModelScope.launch { settingsRepository.setAiProviderType(type) }
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settingsRepository.setLanguage(language) }
    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setWakeWordEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWakeWordEnabled(enabled) }
    fun setContinuousConversation(enabled: Boolean) = viewModelScope.launch { settingsRepository.setContinuousConversation(enabled) }
    fun setPushToTalk(enabled: Boolean) = viewModelScope.launch { settingsRepository.setPushToTalk(enabled) }
    fun setRemoteApiKey(key: String) = viewModelScope.launch { settingsRepository.setRemoteApiKey(key) }
    fun setRemoteApiBaseUrl(url: String) = viewModelScope.launch { settingsRepository.setRemoteApiBaseUrl(url) }
    fun setSearchApiKey(key: String) = viewModelScope.launch { settingsRepository.setSearchApiKey(key) }
}
