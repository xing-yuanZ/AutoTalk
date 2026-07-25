package com.autotalk.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 对话实体。参与者以 JSON 数组字符串存储。 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val goal: String,
    val language: String,
    val status: String,
    val createdAt: Long,
    val startedAt: Long?,
    val participantsJson: String
)

/** 转录条目实体。 */
@Entity(
    tableName = "transcripts",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("conversationId")]
)
data class TranscriptEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val speaker: String,
    val text: String,
    val timestamp: Long,
    val isFinal: Boolean
)

/** 风格教练聊天消息实体。 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val text: String,
    val timestamp: Long
)
