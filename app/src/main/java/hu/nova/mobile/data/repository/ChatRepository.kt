package hu.nova.mobile.data.repository

import hu.nova.mobile.database.dao.ConversationDao
import hu.nova.mobile.database.dao.MessageDao
import hu.nova.mobile.database.entity.ConversationEntity
import hu.nova.mobile.database.entity.MessageEntity
import hu.nova.mobile.database.entity.MessageSender as EntitySender
import hu.nova.mobile.domain.model.ChatMessage
import hu.nova.mobile.domain.model.Sender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {

    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun observeMessages(conversationId: Long): Flow<List<ChatMessage>> =
        messageDao.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun getOrCreateActiveConversation(): Long {
        val existing = conversationDao.getMostRecent()
        if (existing != null) return existing.id
        val now = System.currentTimeMillis()
        return conversationDao.insert(ConversationEntity(title = "Beszélgetés", createdAt = now, updatedAt = now))
    }

    suspend fun startNewConversation(): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(ConversationEntity(title = "Beszélgetés", createdAt = now, updatedAt = now))
    }

    suspend fun addMessage(conversationId: Long, sender: Sender, text: String, isPending: Boolean = false): Long {
        touchConversation(conversationId)
        return messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                sender = sender.toEntity(),
                text = text,
                timestamp = System.currentTimeMillis(),
                isPending = isPending
            )
        )
    }

    suspend fun updateMessageText(message: ChatMessage, newText: String, isPending: Boolean = false) {
        messageDao.update(
            MessageEntity(
                id = message.id,
                conversationId = message.conversationId,
                sender = message.sender.toEntity(),
                text = newText,
                timestamp = message.timestamp,
                isPending = isPending
            )
        )
    }

    suspend fun deleteMessage(message: ChatMessage) {
        messageDao.delete(
            MessageEntity(
                id = message.id,
                conversationId = message.conversationId,
                sender = message.sender.toEntity(),
                text = message.text,
                timestamp = message.timestamp,
                isPending = message.isPending
            )
        )
    }

    suspend fun clearConversation(conversationId: Long) = messageDao.deleteForConversation(conversationId)

    suspend fun getRecentContext(conversationId: Long, limit: Int = 12): List<ChatMessage> =
        messageDao.getRecentForConversation(conversationId, limit).map { it.toDomain() }.reversed()

    private suspend fun touchConversation(conversationId: Long) {
        val conversation = conversationDao.getById(conversationId) ?: return
        conversationDao.update(conversation.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        conversationId = conversationId,
        sender = if (sender == EntitySender.USER) Sender.USER else Sender.NOVA,
        text = text,
        timestamp = timestamp,
        isPending = isPending
    )

    private fun Sender.toEntity() = if (this == Sender.USER) EntitySender.USER else EntitySender.NOVA
}
