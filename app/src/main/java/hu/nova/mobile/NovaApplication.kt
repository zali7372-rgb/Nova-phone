package hu.nova.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import hu.nova.mobile.ai.AIProviderFactory
import hu.nova.mobile.data.preferences.PreferencesManager
import hu.nova.mobile.data.repository.ChatRepository
import hu.nova.mobile.data.repository.MemoryRepository
import hu.nova.mobile.data.repository.SettingsRepository
import hu.nova.mobile.database.NovaDatabase
import hu.nova.mobile.apps.AppDiscoveryRepository
import hu.nova.mobile.commands.CommandRouter
import hu.nova.mobile.web.WebSearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Application-wide dependency graph.
 *
 * This project intentionally avoids pulling in a DI framework (Hilt/Koin) so the whole
 * dependency graph is readable in one file. Everything below is a plain, lazily
 * constructed singleton, which is enough for an app of this size.
 */
class NovaApplication : Application() {

    /** Long-lived scope for work that must survive individual screens (voice session, sync). */
    val applicationScope = CoroutineScope(SupervisorJob())

    val database: NovaDatabase by lazy { NovaDatabase.getInstance(this) }

    val preferencesManager: PreferencesManager by lazy { PreferencesManager(this) }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.messageDao(), database.conversationDao())
    }

    val memoryRepository: MemoryRepository by lazy {
        MemoryRepository(database.memoryDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(preferencesManager)
    }

    val appDiscoveryRepository: AppDiscoveryRepository by lazy {
        AppDiscoveryRepository(this)
    }

    val webSearchRepository: WebSearchRepository by lazy {
        WebSearchRepository(settingsRepository)
    }

    val aiProviderFactory: AIProviderFactory by lazy {
        AIProviderFactory(settingsRepository)
    }

    val commandRouter: CommandRouter by lazy {
        CommandRouter(
            appDiscoveryRepository = appDiscoveryRepository,
            memoryRepository = memoryRepository,
            webSearchRepository = webSearchRepository,
            settingsRepository = settingsRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val voiceChannel = NotificationChannel(
                VOICE_SESSION_CHANNEL_ID,
                getString(R.string.wake_word_active_notification_title),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(voiceChannel)
        }
    }

    companion object {
        const val VOICE_SESSION_CHANNEL_ID = "nova_voice_session"
    }
}
