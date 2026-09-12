package hu.nova.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.nova.mobile.data.repository.SettingsRepository
import hu.nova.mobile.domain.model.AiProviderType
import hu.nova.mobile.domain.model.AppLanguage
import hu.nova.mobile.domain.model.AppThemeMode
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
    val pushToTalk: Boolean = false
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private data class FirstFive(
        val provider: AiProviderType,
        val language: AppLanguage,
        val theme: AppThemeMode,
        val wakeWord: Boolean,
        val continuous: Boolean
    )

    private val firstFive: kotlinx.coroutines.flow.Flow<FirstFive> = combine(
        settingsRepository.aiProviderType,
        settingsRepository.language,
        settingsRepository.themeMode,
        settingsRepository.wakeWordEnabled,
        settingsRepository.continuousConversation
    ) { provider, language, theme, wakeWord, continuous ->
        FirstFive(provider, language, theme, wakeWord, continuous)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        firstFive,
        settingsRepository.pushToTalk
    ) { five, pushToTalk ->
        SettingsUiState(
            aiProviderType = five.provider,
            language = five.language,
            themeMode = five.theme,
            wakeWordEnabled = five.wakeWord,
            continuousConversation = five.continuous,
            pushToTalk = pushToTalk
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
