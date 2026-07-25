package com.autotalk.app.service

import com.autotalk.app.domain.AIChatMessage

/**
 * 端侧大模型实现占位。
 *
 * Android 端侧大模型（Gemini Nano via AICore / Google AI Edge）仅特定设备与系统版本可用，
 * 接入需额外依赖与权限。本类保持架构对齐：当 supportsOnDevice=false 时 Factory 自动回退云端。
 * 如需启用，可在此接入 com.google.ai.edge.generativeai 并实现 chat()。
 */
class OnDeviceLLMService : AIService {
    override val displayName: String = "端侧模型"
    override val isAvailable: Boolean = false

    override suspend fun chat(messages: List<AIChatMessage>): String {
        throw AIError.NotAvailable()
    }
}
