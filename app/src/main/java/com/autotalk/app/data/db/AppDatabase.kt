package com.autotalk.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, TranscriptEntity::class, ChatMessageEntity::class, ChatSessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatSessionDao(): ChatSessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autotalk.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
