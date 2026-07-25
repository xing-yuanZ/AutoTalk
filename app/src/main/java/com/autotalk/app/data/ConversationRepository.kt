package com.autotalk.app.data

import com.autotalk.app.data.db.ChatMessageDao
import com.autotalk.app.data.db.ChatMessageEntity
import com.autotalk.app.data.db.ChatSessionDao
import com.autotalk.app.data.db.ChatSessionEntity
import com.autotalk.app.data.db.ConversationDao
import com.autotalk.app.data.db.ConversationEntity
import com.autotalk.app.data.db.TranscriptDao
import com.autotalk.app.data.db.TranscriptEntity
import com.autotalk.app.domain.ChatRole
import com.autotalk.app.domain.Conversation
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.ConversationStatus
import com.autotalk.app.domain.Participant
import com.autotalk.app.domain.Speaker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** 仓库：在领域模型与 Room 实体之间转换，并统一数据访问。 */
class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val transcriptDao: TranscriptDao,
    private val chatMessageDao: ChatMessageDao,
    private val chatSessionDao: ChatSessionDao
) {
    fun observeConversations(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getConversation(id: String): Conversation? =
        conversationDao.getById(id)?.toDomain()

    suspend fun save(conversation: Conversation) {
        conversationDao.upsert(conversation.toEntity())
    }

    suspend fun updateStatus(id: String, status: ConversationStatus, startedAt: Long? = null) {
        val entity = conversationDao.getById(id) ?: return
        conversationDao.upsert(
            entity.copy(
                status = status.name,
                startedAt = startedAt ?: if (status == ConversationStatus.ACTIVE && entity.startedAt == null) System.currentTimeMillis() else entity.startedAt
            )
        )
    }

    suspend fun delete(conversation: Conversation) {
        conversationDao.delete(conversation.toEntity())
    }

    fun observeTranscripts(conversationId: String): Flow<List<TranscriptEntity>> =
        transcriptDao.observeForConversation(conversationId)

    suspend fun addTranscript(conversationId: String, speaker: Speaker, text: String) {
        transcriptDao.insert(
            TranscriptEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                speaker = speaker.name,
                text = text,
                timestamp = System.currentTimeMillis(),
                isFinal = true
            )
        )
    }

    fun observeChatMessages(): Flow<List<ChatMessageEntity>> = chatMessageDao.observeForSession("")
        // 保持 API 兼容但不再使用，建议用 observeSessionMessages(sessionId)

    fun observeSessionMessages(sessionId: String): Flow<List<ChatMessageEntity>> =
        chatMessageDao.observeForSession(sessionId)

    suspend fun addChatMessage(role: ChatRole, text: String) {
        chatMessageDao.insert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = "",
                role = role.name,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun addSessionMessage(sessionId: String, role: ChatRole, text: String) {
        chatMessageDao.insert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = role.name,
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAll() {
        conversationDao.deleteAll()
        transcriptDao.deleteAll()
        chatMessageDao.deleteAll()
        chatSessionDao.deleteAll()
    }

    // MARK: - 助手会话

    fun observeSessions(): Flow<List<ChatSessionEntity>> = chatSessionDao.observeAll()

    suspend fun createSession(title: String = "新对话"): String {
        val id = UUID.randomUUID().toString()
        chatSessionDao.upsert(
            ChatSessionEntity(
                id = id, title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastMessagePreview = ""
            )
        )
        return id
    }

    suspend fun updateSession(id: String, title: String, updatedAt: Long, lastMessagePreview: String) {
        chatSessionDao.update(id, title, updatedAt, lastMessagePreview)
    }

    suspend fun deleteSession(id: String) {
        chatSessionDao.deleteById(id)
    }

    // MARK: - 映射

    private fun ConversationEntity.toDomain(): Conversation {
        val parts = runCatching {
            val arr = JSONArray(participantsJson)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Participant(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name"),
                    role = o.optString("role"),
                    notes = o.optString("notes")
                )
            }
        }.getOrDefault(emptyList())

        return Conversation(
            id = id,
            title = title,
            content = content,
            goal = goal,
            language = runCatching { ConversationLanguage.valueOf(language) }.getOrDefault(ConversationLanguage.ZH),
            status = runCatching { ConversationStatus.valueOf(status) }.getOrDefault(ConversationStatus.UPCOMING),
            createdAt = createdAt,
            startedAt = startedAt,
            participants = parts
        )
    }

    private fun Conversation.toEntity(): ConversationEntity {
        val arr = JSONArray()
        participants.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("role", p.role)
                put("notes", p.notes)
            })
        }
        return ConversationEntity(
            id = id,
            title = title,
            content = content,
            goal = goal,
            language = language.name,
            status = status.name,
            createdAt = createdAt,
            startedAt = startedAt,
            participantsJson = arr.toString()
        )
    }
}
