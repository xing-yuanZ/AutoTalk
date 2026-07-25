package com.autotalk.app.data

import android.content.Context
import com.autotalk.app.domain.StyleProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 风格画像持久化：JSON 文件存于应用私有目录。隐私优先，不上云。 */
class StyleProfileStore(context: Context) {

    private val file: File = File(context.filesDir, "style_profile.json")

    fun load(): StyleProfile {
        if (!file.exists()) return StyleProfile.EMPTY
        return runCatching {
            val o = JSONObject(file.readText())
            StyleProfile(
                formality = o.optDouble("formality", 0.5),
                averageSentenceLength = o.optDouble("averageSentenceLength", 12.0),
                toneDescription = o.optString("toneDescription"),
                catchphrases = o.optJSONArray("catchphrases")?.toStringList() ?: emptyList(),
                responseStrategies = o.optJSONArray("responseStrategies")?.toStringList() ?: emptyList(),
                summary = o.optString("summary"),
                updatedAt = o.optLong("updatedAt", 0)
            )
        }.getOrDefault(StyleProfile.EMPTY)
    }

    fun save(profile: StyleProfile) {
        val o = JSONObject()
        o.put("formality", profile.formality)
        o.put("averageSentenceLength", profile.averageSentenceLength)
        o.put("toneDescription", profile.toneDescription)
        o.put("catchphrases", JSONArray(profile.catchphrases))
        o.put("responseStrategies", JSONArray(profile.responseStrategies))
        o.put("summary", profile.summary)
        o.put("updatedAt", profile.updatedAt)
        file.writeText(o.toString())
    }

    fun clear() { file.delete() }

    private fun JSONArray.toStringList(): List<String> =
        (0 until length()).map { optString(it) }
}
