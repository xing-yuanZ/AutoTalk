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

/** 云端模型预设。一键切换时同时改 baseURL / 模型名 / API Key。 */
data class ModelPreset(
    val id: String,
    val displayName: String,
    val provider: String,
    val baseURL: String,
    val model: String,
    val supportsVision: Boolean,
    val supportsTools: Boolean,
    val isCustom: Boolean
) {
    /** R1 等推理模型输出含 <think>...</think>，需剥离后返回。 */
    val needsThinkTagStripping: Boolean
        get() = model.contains("reasoner") || model.contains("r1")

    companion object {
        val CUSTOM = ModelPreset(
            id = "custom", displayName = "自定义", provider = "custom",
            baseURL = "", model = "",
            supportsVision = false, supportsTools = false, isCustom = true
        )

        /** 内置预设清单（与 iOS 端一致）。 */
        val PRESETS: List<ModelPreset> = listOf(
            ModelPreset("deepseek_v3", "DeepSeek V3", "deepseek",
                "https://api.deepseek.com/v1", "deepseek-chat",
                supportsVision = false, supportsTools = false, isCustom = false),
            ModelPreset("deepseek_r1", "DeepSeek R1", "deepseek",
                "https://api.deepseek.com/v1", "deepseek-reasoner",
                supportsVision = false, supportsTools = false, isCustom = false),
            ModelPreset("gpt_4o", "GPT-4o", "openai",
                "https://api.openai.com/v1", "gpt-4o",
                supportsVision = true, supportsTools = true, isCustom = false),
            ModelPreset("gpt_4o_mini", "GPT-4o mini", "openai",
                "https://api.openai.com/v1", "gpt-4o-mini",
                supportsVision = true, supportsTools = true, isCustom = false),
            ModelPreset("qwen_plus", "通义千问 Plus", "qwen",
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "qwen-plus",
                supportsVision = false, supportsTools = true, isCustom = false),
            ModelPreset("glm_4_flash", "智谱 GLM-4 Flash", "glm",
                "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash",
                supportsVision = false, supportsTools = true, isCustom = false),
            ModelPreset("moonshot_8k", "月之暗面 8K", "moonshot",
                "https://api.moonshot.cn/v1", "moonshot-v1-8k",
                supportsVision = false, supportsTools = true, isCustom = false),
            CUSTOM
        )

        /** 按 id 查预设，找不到返回 CUSTOM。 */
        fun find(id: String): ModelPreset = PRESETS.firstOrNull { it.id == id } ?: CUSTOM
    }
}
