package com.autotalk.app.service

import com.autotalk.app.data.StyleProfileStore
import com.autotalk.app.domain.AIChatMessage
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.StyleProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 风格学习 Agent：与用户闲聊，并周期性抽取风格画像。不直接依赖 Room。 */
class StyleLearningAgent(
    private val aiService: AIService,
    private val styleStore: StyleProfileStore
) {
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastProfile = MutableStateFlow(StyleProfile.EMPTY)
    val lastProfile: StateFlow<StyleProfile> = _lastProfile.asStateFlow()

    /** 发送一条用户消息并获取助手回复。 */
    suspend fun send(text: String, history: List<AIChatMessage>, appLanguage: String?): String {
        val messages = listOf(
            AIChatMessage(AIChatMessage.ROLE_SYSTEM, PromptTemplates.coachSystem(appLanguage))
        ) + history + listOf(AIChatMessage(AIChatMessage.ROLE_USER, text))

        _isThinking.value = true
        _error.value = null
        return try {
            aiService.chat(messages)
        } catch (e: Exception) {
            _error.value = e.message
            ""
        } finally {
            _isThinking.value = false
        }
    }

    /** 抽取并保存用户风格画像。 */
    suspend fun updateProfile(history: List<AIChatMessage>, language: ConversationLanguage): StyleProfile {
        _isExtracting.value = true
        return try {
            val profile = aiService.extractStyle(history, language)
            styleStore.save(profile)
            _lastProfile.value = profile
            profile
        } finally {
            _isExtracting.value = false
        }
    }
}
