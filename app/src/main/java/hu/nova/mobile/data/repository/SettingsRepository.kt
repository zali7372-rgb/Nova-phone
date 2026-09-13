package hu.nova.mobile.data.repository

import hu.nova.mobile.data.preferences.PreferencesManager
import hu.nova.mobile.domain.model.AiProviderType
import hu.nova.mobile.domain.model.AppLanguage
import hu.nova.mobile.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Facade over [PreferencesManager] exposed to the rest of the app. Keeping this as a
 * separate repository (rather than using PreferencesManager directly everywhere) means
 * ViewModels and other repositories depend on an interface-shaped surface, not on
 * DataStore specifically.
 */
class SettingsRepository(private val preferences: PreferencesManager) {

    val aiProviderType: Flow<AiProviderType> = preferences.aiProviderType
    val language: Flow<AppLanguage> = preferences.language
    val themeMode: Flow<AppThemeMode> = preferences.themeMode
    val wakeWordEnabled: Flow<Boolean> = preferences.wakeWordEnabled
    val continuousConversation: Flow<Boolean> = preferences.continuousConversation
    val pushToTalk: Flow<Boolean> = preferences.pushToTalk
    val remoteApiKey: Flow<String> = preferences.remoteApiKey
    val remoteApiBaseUrl: Flow<String> = preferences.remoteApiBaseUrl
    val searchApiKey: Flow<String> = preferences.searchApiKey

    suspend fun setSearchApiKey(key: String) = preferences.setSearchApiKey(key)

    suspend fun setAiProviderType(type: AiProviderType) = preferences.setAiProviderType(type)
    suspend fun setLanguage(language: AppLanguage) = preferences.setLanguage(language)
    suspend fun setThemeMode(mode: AppThemeMode) = preferences.setThemeMode(mode)
    suspend fun setWakeWordEnabled(enabled: Boolean) = preferences.setWakeWordEnabled(enabled)
    suspend fun setContinuousConversation(enabled: Boolean) = preferences.setContinuousConversation(enabled)
    suspend fun setPushToTalk(enabled: Boolean) = preferences.setPushToTalk(enabled)
    suspend fun setRemoteApiKey(key: String) = preferences.setRemoteApiKey(key)
    suspend fun setRemoteApiBaseUrl(url: String) = preferences.setRemoteApiBaseUrl(url)
}
