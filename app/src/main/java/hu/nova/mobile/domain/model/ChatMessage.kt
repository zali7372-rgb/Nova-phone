package hu.nova.mobile.domain.model

enum class Sender { USER, NOVA }

data class ChatMessage(
    val id: Long,
    val conversationId: Long,
    val sender: Sender,
    val text: String,
    val timestamp: Long,
    val isPending: Boolean = false
)
