package com.autotalk.app.domain

/** 对话领域模型（引擎与 UI 使用，与 Room 实体解耦）。 */
data class Conversation(
    val id: String,
    val title: String,
    val content: String,
    val goal: String,
    val language: ConversationLanguage,
    val status: ConversationStatus,
    val createdAt: Long,
    val startedAt: Long?,
    val participants: List<Participant>
)
