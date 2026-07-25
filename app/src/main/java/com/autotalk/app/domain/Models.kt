package com.autotalk.app.domain

import java.util.UUID

/** AI 后端类型。 */
enum class AIBackend(val displayName: String) {
    ON_DEVICE("端侧模型"),
    CLOUD("云端 API")
}

/** 语音识别后端。 */
enum class ASRBackend(val displayName: String) {
    SYSTEM("系统内置"),
    DOUBAO("豆包语音识别")
}

/** 语音识别模式。 */
enum class RecognitionMode(val displayName: String) {
    AUTO("自动选择"),
    ON_DEVICE("端侧（离线）"),
    CLOUD("云端")
}

/** 对话语言。 */
enum class ConversationLanguage(val displayName: String, val speechLocale: String, val ttsLang: String) {
    ZH("中文", "zh-CN", "zh-CN"),
    YUE("粤语", "zh-HK", "zh-HK"),
    EN("English", "en-US", "en-US")
}

/** 对话状态。 */
enum class ConversationStatus { UPCOMING, ACTIVE, FINISHED }

/** 说话者。 */
enum class Speaker { OTHER, USER }

/** 聊天角色。 */
enum class ChatRole { USER, ASSISTANT }

/** 对话参与者。 */
data class Participant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val role: String,
    val notes: String = ""
)

/** AI 生成的一条回复建议。 */
data class Suggestion(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val reasoning: String,
    val adopted: Boolean = false,
    val spoken: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** 用户说话风格画像。 */
data class StyleProfile(
    val formality: Double = 0.5,
    val averageSentenceLength: Double = 12.0,
    val toneDescription: String = "",
    val catchphrases: List<String> = emptyList(),
    val responseStrategies: List<String> = emptyList(),
    val summary: String = "",
    val updatedAt: Long = 0
) {
    val isEmpty: Boolean get() = summary.isEmpty() && catchphrases.isEmpty()

    /** 渲染为提示词可读文本。 */
    val promptSnippet: String
        get() = if (isEmpty) "（暂无用户风格画像，使用通用自然口语风格）"
        else buildString {
            append("- 正式度: ${(formality * 100).toInt()}%\n")
            append("- 平均句长: ${averageSentenceLength.toInt()}\n")
            if (toneDescription.isNotEmpty()) append("- 语气: $toneDescription\n")
            if (catchphrases.isNotEmpty()) append("- 口头禅/高频词: ${catchphrases.joinToString("、")}\n")
            if (responseStrategies.isNotEmpty()) append("- 回应策略偏好: ${responseStrategies.joinToString("；")}\n")
            if (summary.isNotEmpty()) append("- 综合描述: $summary")
        }

    companion object {
        val EMPTY = StyleProfile()
    }
}

/** 传输给 AI 的聊天消息。 */
data class AIChatMessage(val role: String, val content: String) {
    companion object {
        const val ROLE_SYSTEM = "system"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
    }
}
