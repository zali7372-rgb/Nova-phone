package hu.nova.mobile.ai

import hu.nova.mobile.data.repository.SettingsRepository
import hu.nova.mobile.domain.model.AiProviderType
import kotlinx.coroutines.flow.first

/**
 * Resolves which [AIProvider] implementation is active right now, based on user
 * settings. This is the single seam the rest of the app depends on, so switching or
 * adding providers never requires touching ViewModels or UI.
 */
class AIProviderFactory(private val settingsRepository: SettingsRepository) {

    private val localProvider by lazy { LocalAIProvider() }
    private val remoteProvider by lazy { RemoteAIProvider(settingsRepository) }

    suspend fun getActiveProvider(): AIProvider {
        return when (settingsRepository.aiProviderType.first()) {
            AiProviderType.LOCAL -> localProvider
            AiProviderType.REMOTE -> remoteProvider
        }
    }
}
