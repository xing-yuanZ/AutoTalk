package com.autotalk.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.autotalk.app.AppContainer
import com.autotalk.app.data.db.ChatMessageEntity
import com.autotalk.app.data.db.ChatSessionEntity
import com.autotalk.app.domain.AIChatMessage
import com.autotalk.app.domain.ChatRole
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.Speaker
import com.autotalk.app.domain.Suggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// MARK: - 会话列表
class ConversationListViewModel(private val container: AppContainer) : ViewModel() {
    val conversations: StateFlow<List<com.autotalk.app.domain.Conversation>> =
        container.repository.observeConversations()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(conversation: com.autotalk.app.domain.Conversation) {
        viewModelScope.launch { container.repository.delete(conversation) }
    }
}

// MARK: - 创建会话
class ConversationSetupViewModel(private val container: AppContainer) : ViewModel() {
    fun save(
        title: String, content: String, goal: String,
        language: ConversationLanguage,
        participants: List<com.autotalk.app.domain.Participant>,
        onSaved: (String) -> Unit
    ) {
        val convo = com.autotalk.app.domain.Conversation(
            id = java.util.UUID.randomUUID().toString(),
            title = title, content = content, goal = goal,
            language = language, status = com.autotalk.app.domain.ConversationStatus.UPCOMING,
            createdAt = System.currentTimeMillis(), startedAt = null,
            participants = participants
        )
        viewModelScope.launch {
            container.repository.save(convo)
            onSaved(convo.id)
        }
    }
}

// MARK: - 实时会话
class LiveSessionViewModel(
    private val container: AppContainer,
    private val conversationId: String
) : ViewModel() {

    private var engineRef: com.autotalk.app.service.ConversationEngine? = null
    val engine: StateFlow<com.autotalk.app.service.ConversationEngine?> = kotlinx.coroutines.flow.MutableStateFlow<com.autotalk.app.service.ConversationEngine?>(null).asStateFlow2()

    val transcripts: StateFlow<List<com.autotalk.app.data.db.TranscriptEntity>> =
        container.repository.observeTranscripts(conversationId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val convo = container.repository.getConversation(conversationId) ?: return@launch
            container.repository.updateStatus(conversationId, com.autotalk.app.domain.ConversationStatus.ACTIVE)
            val eng = container.makeEngine(convo)
            eng.onPersistTranscript = { speaker, text ->
                viewModelScope.launch { container.repository.addTranscript(conversationId, speaker, text) }
            }
            engineRef = eng
            (engine as kotlinx.coroutines.flow.MutableStateFlow).value = eng
        }
    }

    fun start() = engineRef?.start()
    fun pause() = engineRef?.pause()
    fun end() { engineRef?.end() }
    fun handleManualInput(text: String) = engineRef?.handleManualInput(text)
    fun speak(s: Suggestion) = engineRef?.speak(s)
    fun adopt(s: Suggestion) = engineRef?.adopt(s)
    fun remove(s: Suggestion) = engineRef?.remove(s)

    override fun onCleared() {
        super.onCleared()
        engineRef?.pause()
    }
}

// MARK: - 助手会话列表
class SessionListViewModel(private val container: AppContainer) : ViewModel() {
    val sessions: StateFlow<List<ChatSessionEntity>> =
        container.repository.observeSessions()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun createSession() {
        viewModelScope.launch { container.repository.createSession() }
    }

    fun renameSession(id: String, title: String) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                container.repository.updateSession(id, title, System.currentTimeMillis(), "")
            }
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { container.repository.deleteSession(id) }
    }
}

// MARK: - 助手聊天
class ChatViewModel(
    private val container: AppContainer,
    private val sessionId: String
) : ViewModel() {

    private val agent = container.makeAssistant()

    val messages: StateFlow<List<ChatMessageEntity>> =
        container.repository.observeSessionMessages(sessionId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sessionTitle: StateFlow<String> =
        container.repository.observeSessions()
            .map { sessions -> sessions.find { it.id == sessionId }?.title ?: "助手" }
            .stateIn(viewModelScope, SharingStarted.Eagerly, "助手")

    val isThinking: StateFlow<Boolean> = agent.isThinking

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _error.value = null
            try {
                val currentMessages = messages.value

                // ✅ 先保存用户消息到数据库（确保消息不会消失）
                container.repository.addSessionMessage(sessionId, ChatRole.USER, text)

                // 前置检查：必须有 API Key
                val settings = container.settingsState.value
                if (settings.cloudAPIKey.isBlank()) {
                    _error.value = "未配置云端 API Key，请先在设置中填写。"
                    return@launch
                }

                // agent.send() 自己会把当前消息追加到历史，这里只传之前的消息
                val history = currentMessages.map {
                    AIChatMessage(
                        if (it.role == ChatRole.USER.name) AIChatMessage.ROLE_USER else AIChatMessage.ROLE_ASSISTANT,
                        it.text
                    )
                }

                // 如果是第一条消息，自动生成标题
                val currentTitle = sessionTitle.value
                val title = if (currentMessages.isEmpty()) {
                    agent.generateTitle(text, settings.appLanguage)
                } else {
                    currentTitle
                }

                // 获取 AI 回复
                val reply = agent.send(text, history, settings.appLanguage)
                if (reply.isNotEmpty()) {
                    container.repository.addSessionMessage(sessionId, ChatRole.ASSISTANT, reply)
                } else if (agent.error.value != null) {
                    // 同时显示 AI 错误
                    _error.value = agent.error.value
                }

                // 更新会话预览
                container.repository.updateSession(
                    sessionId, title, System.currentTimeMillis(), reply.take(50)
                )
            } catch (e: Exception) {
                _error.value = "发送失败：${e.message ?: "未知错误"}"
            }
        }
    }

    fun updateProfile() {
        viewModelScope.launch {
            try {
                val history = messages.value.map {
                    AIChatMessage(
                        if (it.role == ChatRole.USER.name) AIChatMessage.ROLE_USER else AIChatMessage.ROLE_ASSISTANT,
                        it.text
                    )
                }
                val lang = if (container.settingsState.value.appLanguage?.startsWith("en") == true)
                    ConversationLanguage.EN else ConversationLanguage.ZH
                val profile = agent.updateProfile(history, lang)
                container.updateStyleProfile(profile)
            } catch (_: Exception) { }
        }
    }
}

// MARK: - 设置
class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<com.autotalk.app.data.prefs.SettingsSnapshot> = container.settingsState

    fun setBackend(v: com.autotalk.app.domain.AIBackend) = viewModelScope.launch { container.settingsStore.setBackend(v) }
    fun setAsrBackend(v: com.autotalk.app.domain.ASRBackend) = viewModelScope.launch { container.settingsStore.setAsrBackend(v) }
    fun setRecognitionMode(v: com.autotalk.app.domain.RecognitionMode) = viewModelScope.launch { container.settingsStore.setRecognitionMode(v) }
    fun setAutoSpeak(v: Boolean) = viewModelScope.launch { container.settingsStore.setAutoSpeak(v) }
    fun setAppLanguage(v: String?) = viewModelScope.launch { container.settingsStore.setAppLanguage(v) }
    fun setCloudBaseURL(v: String) = viewModelScope.launch { container.settingsStore.setCloudBaseURL(v) }
    fun setCloudModel(v: String) = viewModelScope.launch { container.settingsStore.setCloudModel(v) }
    fun setCloudAPIKey(v: String) = viewModelScope.launch { container.settingsStore.setCloudAPIKey(v) }
    fun setSelectedPreset(presetId: String) = viewModelScope.launch {
        container.settingsStore.setSelectedPreset(presetId, settings.value)
    }
    fun setDoubaoAppID(v: String) = viewModelScope.launch { container.settingsStore.setDoubaoAppID(v) }
    fun setDoubaoAccessToken(v: String) = viewModelScope.launch { container.settingsStore.setDoubaoAccessToken(v) }
    fun setDoubaoCluster(v: String) = viewModelScope.launch { container.settingsStore.setDoubaoCluster(v) }
    fun clearAll() = viewModelScope.launch { container.repository.clearAll() }
    fun clearStyle() { container.styleStore.clear(); container.updateStyleProfile(com.autotalk.app.domain.StyleProfile.EMPTY) }
    fun setOnboardingDone(value: Boolean) = viewModelScope.launch { container.settingsStore.setOnboardingDone(value) }

    fun backendAvailability(): Pair<com.autotalk.app.domain.AIBackend, Boolean> =
        container.aiFactory.availability(settings.value)

    fun asrAvailability(): Pair<com.autotalk.app.domain.ASRBackend, Boolean> =
        container.aiFactory.asrAvailability(settings.value)
}

// MARK: - Onboarding
class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<com.autotalk.app.data.prefs.SettingsSnapshot> = container.settingsState
    fun setBackend(v: com.autotalk.app.domain.AIBackend) = viewModelScope.launch { container.settingsStore.setBackend(v) }
    fun setAsrBackend(v: com.autotalk.app.domain.ASRBackend) = viewModelScope.launch { container.settingsStore.setAsrBackend(v) }
    fun setCloudBaseURL(v: String) = viewModelScope.launch { container.settingsStore.setCloudBaseURL(v) }
    fun setCloudModel(v: String) = viewModelScope.launch { container.settingsStore.setCloudModel(v) }
    fun setCloudAPIKey(v: String) = viewModelScope.launch { container.settingsStore.setCloudAPIKey(v) }
    fun setDoubaoAppID(v: String) = viewModelScope.launch { container.settingsStore.setDoubaoAppID(v) }
    fun setDoubaoAccessToken(v: String) = viewModelScope.launch { container.settingsStore.setDoubaoAccessToken(v) }
    fun setDoubaoCluster(v: String) = viewModelScope.launch { container.settingsStore.setDoubaoCluster(v) }
    fun complete() = viewModelScope.launch { container.settingsStore.setOnboardingDone(true) }
}

// MARK: - 工厂
object AppVMFactory {
    fun list(container: AppContainer) = viewModelFactory { initializer { ConversationListViewModel(container) } }
    fun setup(container: AppContainer) = viewModelFactory { initializer { ConversationSetupViewModel(container) } }
    fun sessions(container: AppContainer) = viewModelFactory { initializer { SessionListViewModel(container) } }
    fun chat(container: AppContainer, sessionId: String) = viewModelFactory {
        initializer { ChatViewModel(container, sessionId) }
    }
    fun settings(container: AppContainer) = viewModelFactory { initializer { SettingsViewModel(container) } }
    fun onboarding(container: AppContainer) = viewModelFactory { initializer { OnboardingViewModel(container) } }
    fun live(container: AppContainer, conversationId: String) = viewModelFactory {
        initializer { LiveSessionViewModel(container, conversationId) }
    }
}

// 小工具：把 MutableStateFlow 暴露为 StateFlow 的桥接（避免泛型擦除问题）
private fun <T> kotlinx.coroutines.flow.MutableStateFlow<T>.asStateFlow2(): StateFlow<T> = this
