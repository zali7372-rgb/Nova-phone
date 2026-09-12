package hu.nova.mobile.ui

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
import hu.nova.mobile.domain.model.ChatMessage
import hu.nova.mobile.domain.model.Sender
import hu.nova.mobile.domain.model.VoiceState
import hu.nova.mobile.voice.SpeechRecognitionEvent
import hu.nova.mobile.voice.SpeechRecognitionManager
import hu.nova.mobile.voice.TextToSpeechManager
import hu.nova.mobile.voice.TtsEvent
import hu.nova.mobile.web.WebSearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Shared conversation logic used by both the Home quick-chat surface and the full Chat
 * screen, so "send message -> route command or ask AI -> persist -> optionally speak"
 * is implemented exactly once. Owns the [VoiceState] shown in both UIs.
 */
class ConversationEngine(
    private val context: Context,
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository,
    private val settingsRepository: SettingsRepository,
    private val commandRouter: CommandRouter,
    private val aiProviderFactory: AIProviderFactory,
    private val scope: CoroutineScope
) {
    private val speechRecognitionManager = SpeechRecognitionManager(context)
    private val textToSpeechManager = TextToSpeechManager(context)

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    var activeConversationId: Long = 0L
        private set

    suspend fun ensureConversation(): Long {
        if (activeConversationId == 0L) {
            activeConversationId = chatRepository.getOrCreateActiveConversation()
        }
        return activeConversationId
    }

    fun startNewConversation() {
        scope.launch {
            activeConversationId = chatRepository.startNewConversation()
        }
    }

    fun clearError() {
        _lastError.value = null
    }

    /** Sends [text] as the user's message, routes/answers it, persists both turns, optionally speaks the reply. */
    fun sendMessage(text: String, speakReply: Boolean) {
        if (text.isBlank()) return
        scope.launch {
            val conversationId = ensureConversation()
            chatRepository.addMessage(conversationId, Sender.USER, text)

            _voiceState.value = VoiceState.THINKING
            val language = settingsRepository.language.first()

            val reply = try {
                when (val outcome = commandRouter.route(text, context, language)) {
                    is CommandOutcome.Handled -> outcome.spokenReply
                    CommandOutcome.NotACommand -> askAiProvider(text, conversationId, language)
                }
            } catch (e: Exception) {
                _lastError.value = e.message
                fallbackError(language)
            }

            chatRepository.addMessage(conversationId, Sender.NOVA, reply)

            if (speakReply) {
                speak(reply, language)
            } else {
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    private suspend fun askAiProvider(text: String, conversationId: Long, language: AppLanguage): String {
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
            is AIResult.Failure -> {
                _lastError.value = result.message
                describeFailure(result.reason, language)
            }
        }
    }

    private fun describeFailure(reason: AIFailureReason, language: AppLanguage): String {
        val hu = language == AppLanguage.HUNGARIAN
        return when (reason) {
            AIFailureReason.NO_INTERNET -> if (hu) "Nincs internetkapcsolat, ezért ezt csak korlátozottan tudom megoldani." else "There's no internet connection, so this is limited."
            AIFailureReason.PROVIDER_UNAVAILABLE -> if (hu) "Az AI szolgáltató jelenleg nem elérhető." else "The AI provider is currently unavailable."
            AIFailureReason.QUOTA_EXHAUSTED -> if (hu) "Elfogyott a keret az AI szolgáltatónál." else "The AI provider quota has been exhausted."
            AIFailureReason.INVALID_CONFIG -> if (hu) "Nincs megfelelően beállítva a távoli AI szolgáltató. Ellenőrizd a Beállításokat." else "The remote AI provider isn't configured correctly. Check Settings."
            AIFailureReason.UNKNOWN -> if (hu) "Váratlan hiba történt." else "An unexpected error occurred."
        }
    }

    private fun fallbackError(language: AppLanguage): String =
        if (language == AppLanguage.HUNGARIAN) "Váratlan hiba történt a válasz elkészítése közben." else "Something went wrong while preparing a reply."

    /** Starts a push-to-talk / manual listening window; result is delivered via [sendMessage]. */
    fun listenOnce(onTranscript: (String) -> Unit) {
        scope.launch {
            val language = settingsRepository.language.first()
            _voiceState.value = VoiceState.LISTENING
            speechRecognitionManager.listen(language).collect { event ->
                when (event) {
                    is SpeechRecognitionEvent.FinalResult -> {
                        _voiceState.value = VoiceState.IDLE
                        if (event.text.isNotBlank()) onTranscript(event.text)
                    }
                    is SpeechRecognitionEvent.Error -> {
                        _lastError.value = event.message
                        _voiceState.value = VoiceState.IDLE
                    }
                    else -> Unit
                }
            }
        }
    }

    fun stopListening() {
        _voiceState.value = VoiceState.IDLE
    }

    fun interruptSpeaking() {
        textToSpeechManager.stop()
        _voiceState.value = VoiceState.IDLE
    }

    private fun speak(text: String, language: AppLanguage) {
        scope.launch {
            _voiceState.value = VoiceState.SPEAKING
            textToSpeechManager.speak(text, language).collect { event ->
                when (event) {
                    is TtsEvent.Done -> _voiceState.value = VoiceState.IDLE
                    is TtsEvent.Error -> {
                        _lastError.value = event.message
                        _voiceState.value = VoiceState.IDLE
                    }
                    TtsEvent.Started -> Unit
                }
            }
        }
    }

    fun observeMessages(conversationId: Long) = chatRepository.observeMessages(conversationId)

    fun shutdown() {
        textToSpeechManager.shutdown()
    }

    suspend fun deleteMessage(message: ChatMessage) = chatRepository.deleteMessage(message)

    suspend fun clearConversation(conversationId: Long) = chatRepository.clearConversation(conversationId)
}
