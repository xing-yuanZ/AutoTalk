package com.autotalk.app.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.autotalk.app.domain.ASRBackend
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.RecognitionMode
import com.autotalk.app.data.prefs.SettingsSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** 语音识别回调事件流。 */
sealed class SpeechEvent {
    data class Partial(val text: String) : SpeechEvent()
    data class Final(val text: String) : SpeechEvent()
    object Listening : SpeechEvent()
    object Idle : SpeechEvent()
    data class Error(val message: String) : SpeechEvent()
}

/**
 * 语音识别服务封装。
 *
 * 当前默认使用 Android 系统内置 SpeechRecognizer（国内手机厂商通常内置讯飞/百度引擎，
 * 中文识别效果尚可）。后续可按 SettingsSnapshot.asrBackend 切换为豆包 SDK。
 */
class SpeechRecognitionService(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var preferOffline = false
    private var locale: String = "zh-CN"
    private var asrBackend: ASRBackend = ASRBackend.SYSTEM
    private var doubaoCredentials: Triple<String, String, String>? = null

    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SpeechEvent> = _events

    /** 是否可用。 */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun configure(language: ConversationLanguage, mode: RecognitionMode, network: NetworkMonitor) {
        this.locale = language.speechLocale
        this.preferOffline = when (mode) {
            RecognitionMode.ON_DEVICE -> true
            RecognitionMode.CLOUD -> false
            RecognitionMode.AUTO -> !(network.isConnected.value && network.isWifi.value)
        }
    }

    /** 携带完整 ASR 后端的配置（推荐入口）。 */
    fun configure(settings: SettingsSnapshot, language: ConversationLanguage, network: NetworkMonitor) {
        this.asrBackend = settings.asrBackend
        this.doubaoCredentials = Triple(settings.doubaoAppID, settings.doubaoAccessToken, settings.doubaoCluster)
        this.locale = language.speechLocale
        this.preferOffline = when (settings.recognitionMode) {
            RecognitionMode.ON_DEVICE -> true
            RecognitionMode.CLOUD -> false
            RecognitionMode.AUTO -> !(network.isConnected.value && network.isWifi.value)
        }
    }

    @Synchronized
    fun start() {
        if (asrBackend == ASRBackend.DOUBAO) {
            startDoubao()
        } else {
            startSystem()
        }
    }

    private fun startSystem() {
        if (recognizer != null) stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Android 12+ 支持偏好离线识别。
            if (preferOffline) putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer?.startListening(intent)
        _events.tryEmit(SpeechEvent.Listening)
    }

    /** 豆包 ASR 启动占位。实际集成时需替换为火山引擎 SDK。 */
    private fun startDoubao() {
        val (appID, token, cluster) = doubaoCredentials ?: Triple("", "", "")
        if (appID.isBlank() || token.isBlank()) {
            _events.tryEmit(SpeechEvent.Error("豆包 AppID 或 AccessToken 未配置"))
            _events.tryEmit(SpeechEvent.Idle)
            return
        }
        // TODO: 接入火山引擎 SpeechEngineAsrToB Android SDK。
        // 当前先用系统识别兜底，避免编译依赖和运行崩溃。
        _events.tryEmit(SpeechEvent.Error("豆包 ASR Android SDK 尚未接入，已回退系统识别"))
        startSystem()
    }

    @Synchronized
    fun stop() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        _events.tryEmit(SpeechEvent.Idle)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                SpeechRecognizer.ERROR_AUDIO -> "录音错误"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "未授权麦克风/语音识别"
                else -> "识别错误($error)"
            }
            // 噪声类错误不打扰，继续监听。
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _events.tryEmit(SpeechEvent.Error(msg))
            }
            _events.tryEmit(SpeechEvent.Idle)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotEmpty()) _events.tryEmit(SpeechEvent.Final(text))
            _events.tryEmit(SpeechEvent.Idle)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotEmpty()) _events.tryEmit(SpeechEvent.Partial(text))
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
