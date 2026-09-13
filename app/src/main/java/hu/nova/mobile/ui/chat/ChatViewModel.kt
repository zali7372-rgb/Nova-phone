package hu.nova.mobile.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.nova.mobile.ai.AIProviderFactory
import hu.nova.mobile.commands.CommandRouter
import hu.nova.mobile.data.repository.ChatRepository
import hu.nova.mobile.data.repository.MemoryRepository
import hu.nova.mobile.data.repository.SettingsRepository
import hu.nova.mobile.domain.model.ChatMessage
import hu.nova.mobile.domain.model.VoiceState
import hu.nova.mobile.ui.ConversationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val voiceState: VoiceState = VoiceState.IDLE,
    val inputText: String = "",
    val errorMessage: String? = null
)

class ChatViewModel(
    context: Context,
    private val chatRepository: ChatRepository,
    memoryRepository: MemoryRepository,
    settingsRepository: SettingsRepository,
    commandRouter: CommandRouter,
    aiProviderFactory: AIProviderFactory
) : ViewModel() {

    private val engine = ConversationEngine(
        context, chatRepository, memoryRepository, settingsRepository, commandRouter, aiProviderFactory, viewModelScope
    )

    private val _inputText = MutableStateFlow("")

    val uiState: StateFlow<ChatUiState> = combine(
        messagesFlow(), engine.voiceState, _inputText, engine.lastError
    ) { messages, voiceState, input, error ->
        ChatUiState(messages = messages, voiceState = voiceState, inputText = input, errorMessage = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    private fun messagesFlow() = flow {
        val conversationId = engine.ensureConversation()
        emitAll(chatRepository.observeMessages(conversationId))
    }

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun sendTypedMessage() {
        val text = _inputText.value
        _inputText.value = ""
        engine.sendMessage(text, speakReply = false)
    }

    fun onMicTapped() {
        engine.listenOnce { transcript -> engine.sendMessage(transcript, speakReply = true) }
    }

    fun onInterrupt() = engine.interruptSpeaking()

    fun onNewConversation() = engine.startNewConversation()

    fun onClearConversation() {
        viewModelScope.launch { engine.clearConversation(engine.ensureConversation()) }
    }

    fun onDeleteMessage(message: ChatMessage) {
        viewModelScope.launch { engine.deleteMessage(message) }
    }

    fun onRegenerate(message: ChatMessage) {
        if (message.sender.name != "USER") return
        engine.sendMessage(message.text, speakReply = false)
    }

    fun clearError() = engine.clearError()

    override fun onCleared() {
        super.onCleared()
        engine.shutdown()
    }
}
