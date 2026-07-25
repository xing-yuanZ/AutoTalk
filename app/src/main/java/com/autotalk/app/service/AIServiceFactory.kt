package com.autotalk.app.service

import com.autotalk.app.data.prefs.SettingsSnapshot

/** 根据设置快照返回对应 AIService 实现；端侧不可用时自动回退云端。 */
class AIServiceFactory {
    fun make(snapshot: SettingsSnapshot): AIService {
        val onDevice = OnDeviceLLMService()
        return when (snapshot.backend) {
            com.autotalk.app.domain.AIBackend.ON_DEVICE ->
                if (onDevice.isAvailable) onDevice else makeCloud(snapshot)
            com.autotalk.app.domain.AIBackend.CLOUD -> makeCloud(snapshot)
        }
    }

    fun availability(snapshot: SettingsSnapshot): Pair<com.autotalk.app.domain.AIBackend, Boolean> =
        when (snapshot.backend) {
            com.autotalk.app.domain.AIBackend.ON_DEVICE -> snapshot.backend to OnDeviceLLMService().isAvailable
            com.autotalk.app.domain.AIBackend.CLOUD -> snapshot.backend to snapshot.cloudAPIKey.isNotEmpty()
        }

    fun asrAvailability(snapshot: SettingsSnapshot): Pair<com.autotalk.app.domain.ASRBackend, Boolean> =
        when (snapshot.asrBackend) {
            com.autotalk.app.domain.ASRBackend.SYSTEM -> snapshot.asrBackend to true
            com.autotalk.app.domain.ASRBackend.DOUBAO -> snapshot.asrBackend to
                (snapshot.doubaoAppID.isNotEmpty() && snapshot.doubaoAccessToken.isNotEmpty())
        }

    private fun makeCloud(snapshot: SettingsSnapshot): CloudLLMService =
        CloudLLMService(
            baseURL = snapshot.cloudBaseURL,
            model = snapshot.cloudModel,
            apiKey = snapshot.cloudAPIKey,
            stripThinkTag = snapshot.currentPreset.needsThinkTagStripping
        )
}
