package com.autotalk.app.service

import com.autotalk.app.data.StyleProfileStore
import com.autotalk.app.domain.AIChatMessage
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.StyleProfile
import com.autotalk.app.domain.ModelPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 通用助手 Agent：多会话聊天 + 风格学习。 */
class AgentAssistant(
    private val aiService: AIService,
    private val styleStore: StyleProfileStore
) {
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    /** 通用聊天。 */
    suspend fun send(text: String, history: List<AIChatMessage>, appLanguage: String?): String {
        _isThinking.value = true
        _error.value = null
        try {
            val messages = listOf(
                AIChatMessage(AIChatMessage.ROLE_SYSTEM, assistantSystem(appLanguage))
            ) + history + AIChatMessage(AIChatMessage.ROLE_USER, text)
            return aiService.chat(messages)
        } catch (e: Exception) {
            _error.value = e.message
            return ""
        } finally {
            _isThinking.value = false
        }
    }

    /** 自动生成会话标题。 */
    suspend fun generateTitle(firstUserMessage: String, appLanguage: String?): String {
        val prompt = if (appLanguage?.startsWith("en") == true)
            "Generate a concise title (2~6 words) for a chat conversation that starts with this message. Output ONLY the title.\n\nMessage: \"$firstUserMessage\""
        else
            "为一段对话生成一个简洁的标题（2~6 个字），对话以这条消息开头。只输出标题。\n\n消息：\"$firstUserMessage\""
        return try {
            val reply = aiService.chat(listOf(AIChatMessage(AIChatMessage.ROLE_USER, prompt)))
            reply.trim().replace("\"", "").take(20).ifEmpty { "新对话" }
        } catch (_: Exception) { "新对话" }
    }

    /** 抽取并保存风格画像。 */
    suspend fun updateProfile(history: List<AIChatMessage>, language: ConversationLanguage): StyleProfile {
        _isExtracting.value = true
        try {
            val profile = aiService.extractStyle(messages = history, language = language)
            styleStore.save(profile)
            return profile
        } finally {
            _isExtracting.value = false
        }
    }

    companion object {
        fun assistantSystem(appLanguage: String?): String {
            val useEnglish = appLanguage?.startsWith("en") == true
            return if (useEnglish) """
You are AutoTalk's AI Assistant, a helpful and versatile AI. \
You can answer questions, write, translate, code, analyze, brainstorm, and more. \
Be concise and practical. If the user asks for real-time information, \
include [NEED_SEARCH: keywords] in your reply to trigger a search.
""".trimIndent() else """
你是 AutoTalk 的 AI 助手，一个通用、能干的 AI。\
你可以回答问题、写作、翻译、编程、分析、头脑风暴等。\
回复要简洁实用。如果用户询问实时信息，可以在回复中包含 \
[NEED_SEARCH: 关键词] 以触发搜索。
""".trimIndent()
        }
    }
}