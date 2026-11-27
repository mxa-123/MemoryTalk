package com.memorytalk.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AiClient(private val settingsProvider: () -> Settings) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    data class Result(val text: String, val flags: List<String>)

    /** 聊天 AI 生成完整回复 → 审查 AI 审核 → 通过后返回；fail-closed。 */
    suspend fun chat(person: Person, history: List<ChatMessage>, userText: String): Result = withContext(Dispatchers.IO) {
        val s = settingsProvider()
        if (s.apiKey.isBlank()) return@withContext Result("（请先在「我 → 设置」填写 API Key。）", listOf("no_key"))

        val crisis = Safety.isCrisis(userText)
        val messages = mutableListOf<Msg>()
        messages += Msg("system", Safety.buildSystemPrompt(person))
        if (crisis) messages += Msg("system", "【安全事件】用户消息命中危机信号。必须执行危机回应模板，停止角色扮演。")
        history.takeLast(20).forEach { messages += Msg(it.role, it.content) }
        messages += Msg("user", userText)

        val raw = call(messages, s.model, s.temperature)
            ?: return@withContext Result("（连接失败，请检查网络、Key 和接口地址。）", listOf("network_error"))

        val flags = mutableListOf<String>()
        var finalText = raw

        if (s.monitorEnabled) {
            val monitorMsgs = listOf(
                Msg("system", Safety.MONITOR_SYSTEM),
                Msg("user", "用户消息：$userText\n\nAI回复：$raw\n\n审查结果（只输出JSON）：")
            )
            val reviewRaw = call(monitorMsgs, s.monitorModel.ifBlank { s.model }, 0.1)
            if (reviewRaw == null) {
                finalText = Safety.SAFE_FALLBACK
                flags += "monitor_error"
            } else {
                val safe = Regex("\"safe\"\\s*:\\s*true").containsMatchIn(reviewRaw)
                val risk = Regex("\"risk\"\\s*:\\s*\"([^\"]+)\"").find(reviewRaw)?.groupValues?.get(1) ?: "unsafe"
                if (!safe) {
                    flags += "monitor_$risk"
                    finalText = if (crisis) {
                        Safety.CRISIS_FALLBACK
                    } else {
                        val rewrite = Regex("\"rewrite\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                            .find(reviewRaw)?.groupValues?.get(1)
                            ?.replace("\\n", "\n")?.replace("\\\"", "\"")
                        if (rewrite.isNullOrBlank()) Safety.SAFE_FALLBACK else rewrite
                    }
                }
            }
        }

        if (crisis) flags += "crisis"
        if (Safety.hasBadIdentity(finalText)) {
            flags += "identity"
            finalText += "\n\n提醒：这是基于你提供的回忆生成的 AI 模拟，不是真人本人。"
        }
        val userCount = history.count { it.role == "user" }
        if (userCount > 0 && userCount % 20 == 0) {
            flags += "dependency"
            finalText += "\n\n（记得也要和现实中的家人朋友聊聊，他们会想你的。）"
        }
        Result(finalText.trim(), flags.distinct())
    }

    private fun call(messages: List<Msg>, model: String, temperature: Double): String? {
        val body = json.encodeToString(ChatRequest.serializer(), ChatRequest(model, messages, false, temperature))
            .toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url(settingsProvider().baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${settingsProvider().apiKey}")
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val text = resp.body?.string() ?: return null
                json.decodeFromString<ChatResponse>(text).choices.firstOrNull()?.message?.content
            }
        } catch (e: Exception) {
            null
        }
    }
}
