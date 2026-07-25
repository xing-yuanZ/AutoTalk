package com.autotalk.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autotalk.app.domain.AIBackend
import com.autotalk.app.domain.ASRBackend
import com.autotalk.app.domain.RecognitionMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "autotalk_settings")

/** 用户设置（DataStore 持久化）。 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("hasCompletedOnboarding")
        val BACKEND = stringPreferencesKey("aiBackend")
        val ASR_BACKEND = stringPreferencesKey("asrBackend")
        val RECOGNITION = stringPreferencesKey("recognitionMode")
        val AUTO_SPEAK = booleanPreferencesKey("autoSpeakSuggestions")
        val APP_LANGUAGE = stringPreferencesKey("appLanguage")
        val CLOUD_BASE_URL = stringPreferencesKey("cloudBaseURL")
        val CLOUD_MODEL = stringPreferencesKey("cloudModel")
        val CLOUD_API_KEY = stringPreferencesKey("cloudAPIKey")
        val DOUBAO_APP_ID = stringPreferencesKey("doubaoAppID")
        val DOUBAO_ACCESS_TOKEN = stringPreferencesKey("doubaoAccessToken")
        val DOUBAO_CLUSTER = stringPreferencesKey("doubaoCluster")
    }

    val hasCompletedOnboarding: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING] ?: false }

    val backend: Flow<AIBackend> =
        context.dataStore.data.map { runCatching { AIBackend.valueOf(it[Keys.BACKEND] ?: "") }.getOrDefault(AIBackend.CLOUD) }

    val asrBackend: Flow<ASRBackend> =
        context.dataStore.data.map { runCatching { ASRBackend.valueOf(it[Keys.ASR_BACKEND] ?: "") }.getOrDefault(ASRBackend.SYSTEM) }

    val recognitionMode: Flow<RecognitionMode> =
        context.dataStore.data.map { runCatching { RecognitionMode.valueOf(it[Keys.RECOGNITION] ?: "") }.getOrDefault(RecognitionMode.CLOUD) }

    val autoSpeakSuggestions: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_SPEAK] ?: false }

    val appLanguage: Flow<String?> =
        context.dataStore.data.map { it[Keys.APP_LANGUAGE] }

    val cloudBaseURL: Flow<String> =
        context.dataStore.data.map { it[Keys.CLOUD_BASE_URL] ?: "https://api.deepseek.com/v1" }

    val cloudModel: Flow<String> =
        context.dataStore.data.map { it[Keys.CLOUD_MODEL] ?: "deepseek-chat" }

    val cloudAPIKey: Flow<String> =
        context.dataStore.data.map { it[Keys.CLOUD_API_KEY] ?: "" }

    val doubaoAppID: Flow<String> =
        context.dataStore.data.map { it[Keys.DOUBAO_APP_ID] ?: "" }

    val doubaoAccessToken: Flow<String> =
        context.dataStore.data.map { it[Keys.DOUBAO_ACCESS_TOKEN] ?: "" }

    val doubaoCluster: Flow<String> =
        context.dataStore.data.map { it[Keys.DOUBAO_CLUSTER] ?: "volcengine_streaming_common" }

    suspend fun setOnboardingDone(value: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING] = value }
    suspend fun setBackend(value: AIBackend) = context.dataStore.edit { it[Keys.BACKEND] = value.name }
    suspend fun setAsrBackend(value: ASRBackend) = context.dataStore.edit { it[Keys.ASR_BACKEND] = value.name }
    suspend fun setRecognitionMode(value: RecognitionMode) = context.dataStore.edit { it[Keys.RECOGNITION] = value.name }
    suspend fun setAutoSpeak(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_SPEAK] = value }
    suspend fun setAppLanguage(value: String?) = context.dataStore.edit {
        if (value == null) it.remove(Keys.APP_LANGUAGE) else it[Keys.APP_LANGUAGE] = value
    }
    suspend fun setCloudBaseURL(value: String) = context.dataStore.edit { it[Keys.CLOUD_BASE_URL] = value }
    suspend fun setCloudModel(value: String) = context.dataStore.edit { it[Keys.CLOUD_MODEL] = value }
    suspend fun setCloudAPIKey(value: String) = context.dataStore.edit { it[Keys.CLOUD_API_KEY] = value }
    suspend fun setDoubaoAppID(value: String) = context.dataStore.edit { it[Keys.DOUBAO_APP_ID] = value }
    suspend fun setDoubaoAccessToken(value: String) = context.dataStore.edit { it[Keys.DOUBAO_ACCESS_TOKEN] = value }
    suspend fun setDoubaoCluster(value: String) = context.dataStore.edit { it[Keys.DOUBAO_CLUSTER] = value }

    /** 一次性聚合全部设置，供 AppContainer 缓存为 StateFlow 供 Factory 读取。 */
    fun snapshot(): Flow<SettingsSnapshot> = context.dataStore.data.map { prefs ->
        SettingsSnapshot(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING] ?: false,
            backend = runCatching { AIBackend.valueOf(prefs[Keys.BACKEND] ?: "") }.getOrDefault(AIBackend.CLOUD),
            asrBackend = runCatching { ASRBackend.valueOf(prefs[Keys.ASR_BACKEND] ?: "") }.getOrDefault(ASRBackend.SYSTEM),
            recognitionMode = runCatching { RecognitionMode.valueOf(prefs[Keys.RECOGNITION] ?: "") }.getOrDefault(RecognitionMode.CLOUD),
            autoSpeakSuggestions = prefs[Keys.AUTO_SPEAK] ?: false,
            appLanguage = prefs[Keys.APP_LANGUAGE],
            cloudBaseURL = prefs[Keys.CLOUD_BASE_URL] ?: "https://api.deepseek.com/v1",
            cloudModel = prefs[Keys.CLOUD_MODEL] ?: "deepseek-chat",
            cloudAPIKey = prefs[Keys.CLOUD_API_KEY] ?: "",
            doubaoAppID = prefs[Keys.DOUBAO_APP_ID] ?: "",
            doubaoAccessToken = prefs[Keys.DOUBAO_ACCESS_TOKEN] ?: "",
            doubaoCluster = prefs[Keys.DOUBAO_CLUSTER] ?: "volcengine_streaming_common"
        )
    }
}

/** 全部设置的快照。 */
data class SettingsSnapshot(
    val hasCompletedOnboarding: Boolean = false,
    val backend: AIBackend = AIBackend.CLOUD,
    val asrBackend: ASRBackend = ASRBackend.SYSTEM,
    val recognitionMode: RecognitionMode = RecognitionMode.CLOUD,
    val autoSpeakSuggestions: Boolean = false,
    val appLanguage: String? = null,
    val cloudBaseURL: String = "https://api.deepseek.com/v1",
    val cloudModel: String = "deepseek-chat",
    val cloudAPIKey: String = "",
    val doubaoAppID: String = "",
    val doubaoAccessToken: String = "",
    val doubaoCluster: String = "volcengine_streaming_common"
) {
    /** 当前设备是否支持端侧模型（Android 端 Gemini Nano 接入门槛高，默认返回 false，回退云端）。 */
    val supportsOnDevice: Boolean get() = false
}
