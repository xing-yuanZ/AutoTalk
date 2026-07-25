package com.autotalk.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import com.autotalk.app.AppContainer
import com.autotalk.app.domain.AIChatMessage
import com.autotalk.app.domain.ChatRole
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.Speaker
import com.autotalk.app.domain.Suggestion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

// MARK: - 风格教练
class StyleCoachViewModel(private val container: AppContainer) : ViewModel() {

    private val agent = container.makeStyleAgent()

    val messages: StateFlow<List<com.autotalk.app.data.db.ChatMessageEntity>> =
        container.repository.observeChatMessages()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isThinking: StateFlow<Boolean> = agent.isThinking
    val isExtracting: StateFlow<Boolean> = agent.isExtracting
    val error: StateFlow<String?> = agent.error
    val profileSummary: StateFlow<String> = kotlinx.coroutines.flow.flow {
        container.styleProfile.collect { p ->
            emit(if (p.isEmpty) "尚未学习。多和我聊聊，再点\"更新画像\"。"
                 else if (p.summary.isEmpty()) "正式度 ${(p.formality * 100).toInt()}% · 句长 ${p.averageSentenceLength.toInt()}"
                 else p.summary)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            container.repository.addChatMessage(ChatRole.USER, text)
            val history = messages.value.map {
                AIChatMessage(if (it.role == ChatRole.USER.name) AIChatMessage.ROLE_USER else AIChatMessage.ROLE_ASSISTANT, it.text)
            }
            val reply = agent.send(text, history, container.settingsState.value.appLanguage)
            if (reply.isNotEmpty()) container.repository.addChatMessage(ChatRole.ASSISTANT, reply)
        }
    }

    fun greetIfNeeded() {
        if (messages.value.isNotEmpty()) return
        val lang = container.settingsState.value.appLanguage
        val greeting = if (lang?.startsWith("en") == true)
            "Hi! I'm your Style Coach. Let's chat so I can learn how you speak — tell me about your day?"
        else
            "你好呀！我是你的风格教练。咱们随便聊聊，我好学习你平时怎么说话——说说你今天怎么样？"
        viewModelScope.launch { container.repository.addChatMessage(ChatRole.ASSISTANT, greeting) }
    }

    fun updateProfile() {
        viewModelScope.launch {
            try {
                val history = messages.value.map {
                    AIChatMessage(if (it.role == ChatRole.USER.name) AIChatMessage.ROLE_USER else AIChatMessage.ROLE_ASSISTANT, it.text)
                }
                val lang = if (container.settingsState.value.appLanguage?.startsWith("en") == true) ConversationLanguage.EN else ConversationLanguage.ZH
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
    fun coach(container: AppContainer) = viewModelFactory { initializer { StyleCoachViewModel(container) } }
    fun settings(container: AppContainer) = viewModelFactory { initializer { SettingsViewModel(container) } }
    fun onboarding(container: AppContainer) = viewModelFactory { initializer { OnboardingViewModel(container) } }
    fun live(container: AppContainer, conversationId: String) = viewModelFactory {
        initializer { LiveSessionViewModel(container, conversationId) }
    }
}

// 小工具：把 MutableStateFlow 暴露为 StateFlow 的桥接（避免泛型擦除问题）
private fun <T> kotlinx.coroutines.flow.MutableStateFlow<T>.asStateFlow2(): StateFlow<T> = this
