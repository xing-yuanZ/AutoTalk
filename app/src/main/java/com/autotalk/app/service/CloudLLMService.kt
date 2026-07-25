package com.autotalk.app.service

import com.autotalk.app.domain.AIChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 基于 OpenAI 兼容 Chat Completions 接口的云端 AI 服务实现。 */
class CloudLLMService(
    private val baseURL: String,
    private val model: String,
    private val apiKey: String
) : AIService {

    override val displayName: String = "云端 API"
    override val isAvailable: Boolean = apiKey.isNotEmpty()

    override suspend fun chat(messages: List<AIChatMessage>): String = withContext(Dispatchers.IO) {
        if (!isAvailable) throw AIError.MissingAPIKey()
        val endpoint = baseURL.trimEnd('/') + "/chat/completions"
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = 60_000
            readTimeout = 60_000
            doOutput = true
        }
        try {
            val body = JSONObject().apply {
                put("model", model)
                put("temperature", 0.7)
                put("messages", JSONArray().also { arr ->
                    messages.forEach { m -> arr.put(JSONObject().put("role", m.role).put("content", m.content)) }
                })
            }.toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw AIError.BadResponse("HTTP $code：${text.take(500)}")

            val resp = JSONObject(text)
            val content = resp.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?: throw AIError.BadResponse("响应缺少 message.content")
            content
        } catch (e: AIError) {
            throw e
        } catch (e: Exception) {
            throw AIError.Network(e.message ?: "未知网络错误")
        } finally {
            conn.disconnect()
        }
    }
}
