package com.autotalk.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationEntity)

    @Update
    suspend fun update(entity: ConversationEntity)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(entity: ConversationEntity)
}

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcripts WHERE conversationId = :cid ORDER BY timestamp ASC")
    fun observeForConversation(cid: String): Flow<List<TranscriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranscriptEntity)

    @Query("DELETE FROM transcripts")
    suspend fun deleteAll()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getById(id: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt, lastMessagePreview = :lastMessagePreview WHERE id = :id")
    suspend fun update(id: String, title: String, updatedAt: Long, lastMessagePreview: String)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAll()
}
