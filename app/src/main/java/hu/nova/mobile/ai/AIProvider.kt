package hu.nova.mobile.ai

import hu.nova.mobile.domain.model.ChatMessage

data class AIRequest(
    /** Prior turns for context, oldest first. Does not include the new user message. */
    val history: List<ChatMessage>,
    val userMessage: String,
    /** Free-text memory snippets ("My favorite game is GTA V.") injected as context. */
    val memorySnippets: List<String>,
    val languageTag: String // "hu" or "en"
)

sealed class AIResult {
    data class Success(val text: String) : AIResult()
    data class Failure(val reason: AIFailureReason, val message: String) : AIResult()
}

enum class AIFailureReason { NO_INTERNET, PROVIDER_UNAVAILABLE, QUOTA_EXHAUSTED, INVALID_CONFIG, UNKNOWN }

/**
 * Abstraction over "something that can turn a conversation + a new user message into a
 * reply". NOVA is never permanently tied to one implementation - see
 * [AIProviderFactory] for how the active implementation is chosen at runtime based on
 * user settings, and [LocalAIProvider] / [RemoteAIProvider] for the two shipped
 * implementations.
 */
interface AIProvider {
    val id: String
    suspend fun generateReply(request: AIRequest): AIResult
}
