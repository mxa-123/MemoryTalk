package com.memorytalk.app

import kotlinx.serialization.Serializable

@Serializable
data class Person(
    val id: String = "",
    val title: String = "",
    val name: String = "",
    val relation: String = "",
    val years: String = "",
    val traits: String = "",
    val speech: String = "",
    val habits: String = "",
    val memories: String = "",
    val avoid: String = "",
    val extra: String = "",
)

@Serializable
data class ChatMessage(
    val role: String = "user",
    val content: String = "",
    val time: Long = 0L,
    val flags: List<String> = emptyList(),
)

@Serializable
data class Settings(
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val apiKey: String = "",
    val temperature: Double = 0.7,
    val monitorEnabled: Boolean = true,
    val monitorModel: String = "",
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Msg>,
    val stream: Boolean = false,
    val temperature: Double = 0.7,
)

@Serializable
data class Msg(val role: String, val content: String)

@Serializable
data class ChatResponse(val choices: List<Choice> = emptyList())

@Serializable
data class Choice(val message: Msg = Msg("", ""))
