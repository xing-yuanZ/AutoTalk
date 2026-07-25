package com.autotalk.app

import android.content.Context
import com.autotalk.app.data.ConversationRepository
import com.autotalk.app.data.StyleProfileStore
import com.autotalk.app.data.db.AppDatabase
import com.autotalk.app.data.prefs.SettingsSnapshot
import com.autotalk.app.data.prefs.SettingsStore
import com.autotalk.app.domain.StyleProfile
import com.autotalk.app.service.AIService
import com.autotalk.app.service.AIServiceFactory
import com.autotalk.app.service.ConversationEngine
import com.autotalk.app.service.NetworkMonitor
import com.autotalk.app.service.SpeechRecognitionService
import com.autotalk.app.service.StyleLearningAgent
import com.autotalk.app.service.TTSService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 手动依赖容器（替代 Hilt，减少配置）。
 * 持有全部单例服务与全局状态，由 [AutoTalkApp] 创建并在 Compose 中通过 LocalAppContainer 提供。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database: AppDatabase = AppDatabase.get(appContext)
    val repository: ConversationRepository = ConversationRepository(
        conversationDao = database.conversationDao(),
        transcriptDao = database.transcriptDao(),
        chatMessageDao = database.chatMessageDao()
    )
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val styleStore: StyleProfileStore = StyleProfileStore(appContext)
    val network: NetworkMonitor = NetworkMonitor(appContext)
    val tts: TTSService = TTSService(appContext)
    val speech: SpeechRecognitionService = SpeechRecognitionService(appContext)
    val aiFactory: AIServiceFactory = AIServiceFactory()

    /** 设置快照（StateFlow，启动时开始收集）。 */
    private val _settingsState = MutableStateFlow(SettingsSnapshot())
    val settingsState: StateFlow<SettingsSnapshot> = _settingsState.asStateFlow()

    /** 风格画像（启动时从磁盘加载）。 */
    private val _styleProfile = MutableStateFlow(StyleProfile.EMPTY)
    val styleProfile: StateFlow<StyleProfile> = _styleProfile.asStateFlow()

    /** 启动初始化：收集设置、加载风格画像、刷新网络。 */
    fun bootstrap() {
        network.refresh()
        scope.launch { settingsStore.snapshot().collect { _settingsState.value = it } }
        _styleProfile.value = styleStore.load()
    }

    /** 当前 AIService（基于最新设置）。 */
    fun currentAIService(): AIService = aiFactory.make(_settingsState.value)

    /** 创建实时会话引擎。 */
    fun makeEngine(conversation: com.autotalk.app.domain.Conversation): ConversationEngine =
        ConversationEngine(
            conversation = conversation,
            aiService = currentAIService(),
            tts = tts,
            speech = speech,
            network = network,
            settings = _settingsState.value,
            styleProfile = _styleProfile.value
        )

    /** 创建风格学习 Agent。 */
    fun makeStyleAgent(): StyleLearningAgent =
        StyleLearningAgent(aiService = currentAIService(), styleStore = styleStore)

    /** 更新风格画像（抽取后调用）。 */
    fun updateStyleProfile(profile: StyleProfile) {
        _styleProfile.value = profile
    }
}
