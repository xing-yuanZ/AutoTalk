package com.autotalk.app.service

import com.autotalk.app.domain.AIChatMessage
import com.autotalk.app.domain.AIParse
import com.autotalk.app.domain.Conversation
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.StyleProfile
import com.autotalk.app.domain.Suggestion

/** AI 后端可能抛出的错误。 */
sealed class AIError(message: String) : Exception(message) {
    class NotAvailable : AIError("当前后端不可用。")
    class MissingAPIKey : AIError("未配置云端 API Key。")
    class BadResponse(reason: String) : AIError("后端返回异常：$reason")
    class ParseFailed(reason: String) : AIError("解析模型输出失败：$reason")
    class Network(reason: String) : AIError("网络错误：$reason")
}

/** AI 服务统一接口。端侧 / 云端实现均遵循，引擎与 Agent 无感知后端差异。 */
interface AIService {
    val displayName: String
    val isAvailable: Boolean

    /** 通用聊天补全。 */
    suspend fun chat(messages: List<AIChatMessage>): String

    /** 针对实时对话生成回复建议。默认实现依赖 chat() + 提示词 + JSON 解析。 */
    suspend fun generateSuggestions(
        conversation: Conversation,
        recentTranscripts: String,
        styleProfile: StyleProfile
    ): List<Suggestion> {
        val messages = listOf(
            AIChatMessage(AIChatMessage.ROLE_SYSTEM,
                PromptTemplates.suggestionSystem(conversation, styleProfile)),
            AIChatMessage(AIChatMessage.ROLE_USER,
                PromptTemplates.suggestionUser(recentTranscripts, conversation.language))
        )
        val raw = chat(messages)
        val json = AIParse.extractJsonObject(raw)
        return runCatching {
            val arr = org.json.JSONObject(json).optJSONArray("suggestions") ?: return emptyList()
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Suggestion(text = o.optString("text"), reasoning = o.optString("reasoning"))
            }
        }.getOrElse { throw AIError.ParseFailed(it.message ?: "suggestions 解析失败") }
    }

    /** 从聊天记录中抽取用户风格画像。 */
    suspend fun extractStyle(
        messages: List<AIChatMessage>,
        language: ConversationLanguage
    ): StyleProfile {
        val full = listOf(
            AIChatMessage(AIChatMessage.ROLE_SYSTEM, PromptTemplates.extractStyleSystem(language))
        ) + messages.filter { it.role == AIChatMessage.ROLE_USER }
        val raw = chat(full)
        val json = AIParse.extractJsonObject(raw)
        return runCatching {
            val o = org.json.JSONObject(json)
            StyleProfile(
                formality = o.optDouble("formality", 0.5),
                averageSentenceLength = o.optDouble("averageSentenceLength", 12.0),
                toneDescription = o.optString("toneDescription"),
                catchphrases = o.optJSONArray("catchphrases")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                responseStrategies = o.optJSONArray("responseStrategies")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList(),
                summary = o.optString("summary"),
                updatedAt = System.currentTimeMillis()
            )
        }.getOrElse { throw AIError.ParseFailed(it.message ?: "style 解析失败") }
    }
}
