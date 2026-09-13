package hu.nova.mobile.voice

import android.content.Context
import hu.nova.mobile.ai.AIFailureReason
import hu.nova.mobile.ai.AIProviderFactory
import hu.nova.mobile.ai.AIRequest
import hu.nova.mobile.ai.AIResult
import hu.nova.mobile.commands.CommandOutcome
import hu.nova.mobile.commands.CommandRouter
import hu.nova.mobile.data.repository.ChatRepository
import hu.nova.mobile.data.repository.MemoryRepository
import hu.nova.mobile.data.repository.SettingsRepository
import hu.nova.mobile.domain.model.AppLanguage
import hu.nova.mobile.domain.model.Sender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives the "listen for 'Nova', then take a command" loop described in
 * [WakeWordManager]'s documentation, from inside [hu.nova.mobile.service.NovaVoiceService]'s
 * own foreground-service lifetime rather than a ViewModel's.
 *
 * This is what lets NOVA keep listening for the wake word after the user leaves the app
 * (presses Home, switches to another app, locks the screen) - as long as this foreground
 * service is running and its ongoing notification is visible. That part is real and
 * Android-legitimate: a foreground service with a visible notification is explicitly
 * allowed to keep using the microphone while the app itself isn't in the foreground.
 *
 * What this does NOT do, and cannot legitimately do on stock Android: keep listening after
 * the user force-stops the app, swipes it away with "clear all" in some OEM launchers that
 * kill services on swipe, or after the system kills the process under memory pressure
 * without START_STICKY being able to restart it in time. See WakeWordManager.kt for the
 * full explanation of why true "app fully closed, still always listening" wake word isn't
 * possible without a licensed hotword SDK or OEM-level privileges.
 */
class BackgroundListeningEngine(
    private val context: Context,
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository,
    private val commandRouter: CommandRouter,
    private val aiProviderFactory: AIProviderFactory,
    private val scope: CoroutineScope,
    /** true while NOVA has heard the wake word and is actively handling a command. */
    private val onActivityChanged: (Boolean) -> Unit
) {
    private val speechRecognitionManager = SpeechRecognitionManager(context)
    private val textToSpeechManager = TextToSpeechManager(context)
    private val wakeWordManager = WakeWordManager(context, speechRecognitionManager)

    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true
        scope.launch { loop() }
    }

    fun stop() {
        running = false
        textToSpeechManager.stop()
        textToSpeechManager.shutdown()
    }

    val isRunning: Boolean get() = running

    private suspend fun loop() {
        while (running) {
            onActivityChanged(false)
            val language = settingsRepository.language.first()

            val heardWakeWord = waitForWakeWord(language)
            if (!running) break
            if (!heardWakeWord) continue // recognizer timed out or errored - just re-listen

            onActivityChanged(true)
            speak(if (language == AppLanguage.HUNGARIAN) "Igen?" else "Yes?", language)
            if (!running) break

            val command = listenForCommand(language)
            if (!running) break
            if (command != null) {
                handleCommand(command, language)
            }
        }
        onActivityChanged(false)
    }

    private suspend fun waitForWakeWord(language: AppLanguage): Boolean {
        var detected = false
        try {
            speechRecognitionManager.listen(language, preferOffline = false).collect { event ->
                if (event is SpeechRecognitionEvent.FinalResult && wakeWordManager.containsWakeWord(event.text)) {
                    detected = true
                }
            }
        } catch (e: Exception) {
            // A transient recognizer error (no match, timeout, busy) must not stop the
            // whole background session - just retry after a short backoff.
        }
        if (!detected) delay(400)
        return detected
    }

    private suspend fun listenForCommand(language: AppLanguage): String? {
        var result: String? = null
        try {
            speechRecognitionManager.listen(language).collect { event ->
                if (event is SpeechRecognitionEvent.FinalResult && event.text.isNotBlank()) {
                    result = event.text
                }
            }
        } catch (e: Exception) {
            // No command heard - fall through to null and go back to wake-word listening.
        }
        return result
    }

    private suspend fun handleCommand(text: String, language: AppLanguage) {
        val conversationId = chatRepository.getOrCreateActiveConversation()
        chatRepository.addMessage(conversationId, Sender.USER, text)

        val reply = try {
            when (val outcome = commandRouter.route(text, context, language)) {
                is CommandOutcome.Handled -> outcome.spokenReply
                CommandOutcome.NotACommand -> askAi(text, conversationId, language)
            }
        } catch (e: Exception) {
            if (language == AppLanguage.HUNGARIAN) "Váratlan hiba történt." else "Something went wrong."
        }

        chatRepository.addMessage(conversationId, Sender.NOVA, reply)
        speak(reply, language)
    }

    private suspend fun askAi(text: String, conversationId: Long, language: AppLanguage): String {
        val history = chatRepository.getRecentContext(conversationId)
        val memories = memoryRepository.getAllOnce().map { it.content }
        val provider = aiProviderFactory.getActiveProvider()
        val request = AIRequest(
            history = history,
            userMessage = text,
            memorySnippets = memories,
            languageTag = if (language == AppLanguage.HUNGARIAN) "hu" else "en"
        )
        return when (val result = provider.generateReply(request)) {
            is AIResult.Success -> result.text
            is AIResult.Failure -> describeFailure(result.reason, language)
        }
    }

    private fun describeFailure(reason: AIFailureReason, language: AppLanguage): String {
        val hu = language == AppLanguage.HUNGARIAN
        return when (reason) {
            AIFailureReason.NO_INTERNET -> if (hu) "Nincs internetkapcsolat." else "There's no internet connection."
            AIFailureReason.PROVIDER_UNAVAILABLE -> if (hu) "Az AI szolgáltató jelenleg nem elérhető." else "The AI provider is currently unavailable."
            AIFailureReason.QUOTA_EXHAUSTED -> if (hu) "Elfogyott a keret az AI szolgáltatónál." else "The AI provider quota has been exhausted."
            AIFailureReason.INVALID_CONFIG -> if (hu) "Nincs megfelelően beállítva a távoli AI szolgáltató." else "The remote AI provider isn't configured correctly."
            AIFailureReason.UNKNOWN -> if (hu) "Váratlan hiba történt." else "An unexpected error occurred."
        }
    }

    private suspend fun speak(text: String, language: AppLanguage) {
        try {
            textToSpeechManager.speak(text, language).collect { /* suspend until done/error */ }
        } catch (e: Exception) {
            // Ignore TTS failures in the background loop - the session should keep going.
        }
    }
}
