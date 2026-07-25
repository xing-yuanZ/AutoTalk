package com.autotalk.app.service

import com.autotalk.app.data.prefs.SettingsSnapshot
import com.autotalk.app.domain.Conversation
import com.autotalk.app.domain.Speaker
import com.autotalk.app.domain.Suggestion
import com.autotalk.app.domain.StyleProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 实时对话引擎：协调语音识别 → 上下文拼装 → AI 建议 → (可选)TTS。
 *
 * Android SpeechRecognizer 为单次识别，引擎在收到 Idle 后自动重启以实现连续监听。
 * 转录落库通过 [onPersistTranscript] 回调交由 ViewModel 完成，引擎不直接依赖 Room。
 */
class ConversationEngine(
    val conversation: Conversation,
    private val aiService: AIService,
    private val tts: TTSService,
    private val speech: SpeechRecognitionService,
    private val network: NetworkMonitor,
    private val settings: SettingsSnapshot,
    private val styleProfile: StyleProfile
) {
    /** 转录落库回调。 */
    var onPersistTranscript: ((Speaker, String) -> Unit)? = null

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _finalTranscripts = MutableStateFlow<List<String>>(emptyList())
    val finalTranscripts: StateFlow<List<String>> = _finalTranscripts.asStateFlow()

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var generateJob: Job? = null
    private var pendingGenerate = false
    private var userWantsListening = false

    /** 开始监听。 */
    fun start() {
        userWantsListening = true
        speech.configure(settings, conversation.language, network)
        collectEvents()
        speech.start()
    }

    /** 暂停监听。 */
    fun pause() {
        userWantsListening = false
        speech.stop()
        _isListening.value = false
    }

    /** 结束会话。 */
    fun end() {
        generateJob?.cancel()
        userWantsListening = false
        speech.stop()
        tts.stop()
        _isListening.value = false
    }

    /** 用户手动输入对方发言。 */
    fun handleManualInput(text: String) {
        if (text.isBlank()) return
        appendFinal(text)
        generate()
    }

    /** 朗读建议。 */
    fun speak(suggestion: Suggestion) {
        tts.speak(suggestion.text, conversation.language)
        updateSuggestion(suggestion.id) { it.copy(spoken = true) }
    }

    /** 采纳建议（记录用户发言并落库）。 */
    fun adopt(suggestion: Suggestion) {
        updateSuggestion(suggestion.id) { it.copy(adopted = true) }
        onPersistTranscript?.invoke(Speaker.USER, suggestion.text)
    }

    /** 删除建议。 */
    fun remove(suggestion: Suggestion) {
        _suggestions.value = _suggestions.value.filterNot { it.id == suggestion.id }
    }

    // MARK: - 内部

    private var eventsCollected = false
    private fun collectEvents() {
        if (eventsCollected) return
        eventsCollected = true
        scope.launch {
            speech.events.collect { event ->
                when (event) {
                    is SpeechEvent.Partial -> _partialTranscript.value = event.text
                    is SpeechEvent.Final -> {
                        _partialTranscript.value = ""
                        appendFinal(event.text)
                        generate()
                    }
                    is SpeechEvent.Listening -> _isListening.value = true
                    is SpeechEvent.Idle -> {
                        _isListening.value = false
                        // 连续监听：用户仍想听且非思考中，短暂延迟后重启。
                        if (userWantsListening) {
                            scope.launch {
                                delay(200)
                                if (userWantsListening) speech.start()
                            }
                        }
                    }
                    is SpeechEvent.Error -> {
                        _error.value = event.message
                        // 权限类错误不自动重启。
                        if (event.message.contains("授权")) userWantsListening = false
                    }
                }
            }
        }
    }

    private fun appendFinal(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _finalTranscripts.value = _finalTranscripts.value + trimmed
        onPersistTranscript?.invoke(Speaker.OTHER, trimmed)
    }

    private fun generate() {
        if (_isThinking.value) { pendingGenerate = true; return }
        runGeneration()
    }

    private fun runGeneration() {
        val transcripts = _finalTranscripts.value
        if (transcripts.isEmpty()) return
        val recent = transcripts.takeLast(6).joinToString("\n")
        _isThinking.value = true
        _error.value = null
        generateJob = scope.launch {
            try {
                val result = aiService.generateSuggestions(conversation, recent, styleProfile)
                _suggestions.value = result + _suggestions.value
                if (settings.autoSpeakSuggestions && result.isNotEmpty()) {
                    tts.speak(result.first().text, conversation.language)
                    updateSuggestion(result.first().id) { it.copy(spoken = true) }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isThinking.value = false
                if (pendingGenerate) {
                    pendingGenerate = false
                    runGeneration()
                }
            }
        }
    }

    private fun updateSuggestion(id: String, transform: (Suggestion) -> Suggestion) {
        _suggestions.value = _suggestions.value.map { if (it.id == id) transform(it) else it }
    }
}
