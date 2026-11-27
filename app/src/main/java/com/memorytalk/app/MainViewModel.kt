package com.memorytalk.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(ctx: Context) : ViewModel() {
    private val store = Store(ctx.applicationContext)
    private val ai = AiClient { settings.value }

    private val _persons = MutableStateFlow(store.loadPersons())
    val persons: StateFlow<List<Person>> = _persons

    private val _settings = MutableStateFlow(store.loadSettings())
    val settings: StateFlow<Settings> = _settings

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending

    private val _editing = MutableStateFlow<Person?>(null)
    val editing: StateFlow<Person?> = _editing

    private var currentPersonId: String? = null

    fun openChat(id: String) {
        currentPersonId = id
        _messages.value = store.loadChat(id)
    }

    fun send(text: String) {
        val id = currentPersonId ?: return
        val person = _persons.value.find { it.id == id } ?: return
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        val history = _messages.value
        _messages.value = history + ChatMessage("user", text.trim(), now)
        _sending.value = true
        viewModelScope.launch {
            val result = ai.chat(person, history, text.trim())
            _messages.value = _messages.value + ChatMessage("assistant", result.text, System.currentTimeMillis(), result.flags)
            store.saveChat(id, _messages.value)
            _sending.value = false
        }
    }

    fun startNewPerson() { _editing.value = Person() }
    fun editPerson(id: String) { _editing.value = _persons.value.find { it.id == id } }
    fun closeEditor() { _editing.value = null }

    fun savePerson(p: Person) {
        val saved = if (p.id.isBlank()) p.copy(id = UUID.randomUUID().toString().take(12)) else p
        val list = _persons.value.toMutableList()
        val idx = list.indexOfFirst { it.id == saved.id }
        if (idx >= 0) list[idx] = saved else list.add(saved)
        store.savePersons(list)
        _persons.value = list
        _editing.value = null
    }

    fun deletePerson(id: String) {
        store.savePersons(_persons.value.filterNot { it.id == id })
        store.deleteChat(id)
        _persons.value = store.loadPersons()
        _editing.value = null
    }

    fun updateSettings(s: Settings) {
        store.saveSettings(s)
        _settings.value = s
    }

    companion object {
        fun factory(ctx: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(ctx) as T
        }
    }
}
