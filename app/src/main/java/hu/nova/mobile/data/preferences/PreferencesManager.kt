package hu.nova.mobile.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import hu.nova.mobile.domain.model.AiProviderType
import hu.nova.mobile.domain.model.AppLanguage
import hu.nova.mobile.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nova_prefs")

/**
 * Thin wrapper around Jetpack DataStore for the scalar settings that don't need SQL
 * querying: AI provider selection, language, theme, and the voice-interaction toggles.
 * No API keys are ever stored here - see [hu.nova.mobile.ai.RemoteAIProvider] for how
 * remote credentials are handled (BYO key supplied at runtime, never hard-coded).
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val CONTINUOUS_CONVERSATION = booleanPreferencesKey("continuous_conversation")
        val PUSH_TO_TALK = booleanPreferencesKey("push_to_talk")
        val REMOTE_API_BASE_URL = stringPreferencesKey("remote_api_base_url")
        val REMOTE_API_KEY = stringPreferencesKey("remote_api_key")
        val SEARCH_API_KEY = stringPreferencesKey("search_api_key")
    }

    val aiProviderType: Flow<AiProviderType> = context.dataStore.data.map {
        AiProviderType.valueOf(it[Keys.AI_PROVIDER] ?: AiProviderType.LOCAL.name)
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map {
        AppLanguage.valueOf(it[Keys.LANGUAGE] ?: AppLanguage.HUNGARIAN.name)
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map {
        AppThemeMode.valueOf(it[Keys.THEME] ?: AppThemeMode.SYSTEM.name)
    }

    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WAKE_WORD_ENABLED] ?: false }

    val continuousConversation: Flow<Boolean> = context.dataStore.data.map { it[Keys.CONTINUOUS_CONVERSATION] ?: true }

    val pushToTalk: Flow<Boolean> = context.dataStore.data.map { it[Keys.PUSH_TO_TALK] ?: false }

    /** Empty by default; the user must paste their own key in Settings. Never bundled. */
    val remoteApiKey: Flow<String> = context.dataStore.data.map { it[Keys.REMOTE_API_KEY] ?: "" }

    val remoteApiBaseUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.REMOTE_API_BASE_URL] ?: "https://api.anthropic.com/v1/messages"
    }

    /** Separate from [remoteApiKey]: this key is for the web-search provider only. */
    val searchApiKey: Flow<String> = context.dataStore.data.map { it[Keys.SEARCH_API_KEY] ?: "" }

    suspend fun setSearchApiKey(key: String) {
        context.dataStore.edit { it[Keys.SEARCH_API_KEY] = key }
    }

    suspend fun setAiProviderType(type: AiProviderType) {
        context.dataStore.edit { it[Keys.AI_PROVIDER] = type.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setContinuousConversation(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONTINUOUS_CONVERSATION] = enabled }
    }

    suspend fun setPushToTalk(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PUSH_TO_TALK] = enabled }
    }

    suspend fun setRemoteApiKey(key: String) {
        context.dataStore.edit { it[Keys.REMOTE_API_KEY] = key }
    }

    suspend fun setRemoteApiBaseUrl(url: String) {
        context.dataStore.edit { it[Keys.REMOTE_API_BASE_URL] = url }
    }
}
