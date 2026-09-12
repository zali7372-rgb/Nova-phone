package hu.nova.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hu.nova.mobile.NovaApplication
import hu.nova.mobile.ui.chat.ChatViewModel
import hu.nova.mobile.ui.home.HomeViewModel
import hu.nova.mobile.ui.memory.MemoryViewModel
import hu.nova.mobile.ui.settings.SettingsViewModel

/**
 * Single factory for every ViewModel in the app, wiring each one to the repositories
 * built in [NovaApplication]. Avoids pulling in a DI framework for a project this size
 * while still keeping ViewModels fully constructor-injected and unit-testable.
 */
class NovaViewModelFactory(private val app: NovaApplication) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(
                context = app,
                chatRepository = app.chatRepository,
                memoryRepository = app.memoryRepository,
                settingsRepository = app.settingsRepository,
                commandRouter = app.commandRouter,
                aiProviderFactory = app.aiProviderFactory
            ) as T

            ChatViewModel::class.java -> ChatViewModel(
                context = app,
                chatRepository = app.chatRepository,
                memoryRepository = app.memoryRepository,
                settingsRepository = app.settingsRepository,
                commandRouter = app.commandRouter,
                aiProviderFactory = app.aiProviderFactory
            ) as T

            MemoryViewModel::class.java -> MemoryViewModel(app.memoryRepository) as T

            SettingsViewModel::class.java -> SettingsViewModel(app.settingsRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
