package com.memorytalk.app

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 本地存储：人设、对话、设置全部存 App 私有目录，不上传。 */
class Store(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val root: File get() = File(ctx.filesDir, "memorytalk").apply { mkdirs() }
    private val personsFile get() = File(root, "persons.json")
    private val settingsFile get() = File(root, "settings.json")
    private fun chatFile(id: String) = File(root, "chat_$id.json")

    fun loadPersons(): List<Person> = try {
        if (personsFile.exists()) json.decodeFromString(personsFile.readText()) else emptyList()
    } catch (e: Exception) { emptyList() }

    fun savePersons(list: List<Person>) {
        personsFile.writeText(json.encodeToString(list))
    }

    fun loadChat(id: String): List<ChatMessage> = try {
        val f = chatFile(id)
        if (f.exists()) json.decodeFromString(f.readText()) else emptyList()
    } catch (e: Exception) { emptyList() }

    fun saveChat(id: String, list: List<ChatMessage>) {
        chatFile(id).writeText(json.encodeToString(list.takeLast(200)))
    }

    fun deleteChat(id: String) { chatFile(id).delete() }

    fun loadSettings(): Settings = try {
        if (settingsFile.exists()) json.decodeFromString(settingsFile.readText()) else Settings()
    } catch (e: Exception) { Settings() }

    fun saveSettings(s: Settings) {
        settingsFile.writeText(json.encodeToString(s))
    }
}
