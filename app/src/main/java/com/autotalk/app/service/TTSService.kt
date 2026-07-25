package com.autotalk.app.service

import android.content.Context
import android.speech.tts.TextToSpeech
import com.autotalk.app.domain.ConversationLanguage
import java.util.Locale

/** 基于 Android TextToSpeech 的语音合成服务。 */
class TTSService(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) ready = true
        }
    }

    fun speak(text: String, language: ConversationLanguage) {
        if (text.isEmpty()) return
        val t = tts ?: return
        t.language = Locale(language.ttsLang)
        t.stop()
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "autotalk_utt")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
