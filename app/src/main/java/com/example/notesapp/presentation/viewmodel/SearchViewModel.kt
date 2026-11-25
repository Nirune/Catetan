package com.example.notesapp.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.example.notesapp.data.database.NotesDatabase
import com.example.notesapp.data.model.NoteEntity
import com.example.notesapp.domain.repository.NoteRepository
import java.util.concurrent.ConcurrentLinkedQueue

class SearchViewModel(application: Application, private val noteRepository: NoteRepository) : AndroidViewModel(application) {
    private val prefs: android.content.SharedPreferences

    val allNotes: StateFlow<List<NoteEntity>>
    val searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchQuery = MutableStateFlow<String>("")
    val searchResults: StateFlow<List<NoteEntity>>
    val isSearching = MutableStateFlow(false)

    init {
        prefs = application.getSharedPreferences("catetan_search_prefs", Context.MODE_PRIVATE)
        allNotes = noteRepository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        // Reactive search results: combine query + notes, filter case-insensitively
        searchResults = combine(searchQuery, allNotes) { query, notes ->
            val trimmed = query.trim()
            if (trimmed.isEmpty()) {
                emptyList<NoteEntity>()
            } else {
                val lowerQuery = trimmed.lowercase()
                notes.filter { note ->
                    note.title.lowercase().contains(lowerQuery) ||
                    note.content.lowercase().contains(lowerQuery)
                }
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        loadSearchHistory()
    }

    private fun loadSearchHistory() {
        val historyJson = prefs.getString("search_history", "[]") ?: "[]"
        // Parse manual JSON format: ["item1", "item2", ...]
        val history = if (historyJson.startsWith("[") && historyJson.endsWith("]")) {
            val content = historyJson.substring(1, historyJson.length - 1)
            if (content.isBlank()) emptyList<String>()
            else content.split("\", \"").map { it.trim('"') }
        } else {
            // Fallback for old format
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                .decodeFromString<List<String>>(historyJson)
        }
        searchHistory.value = history
    }

    private fun saveSearchHistory(history: List<String>) {
        // Manual JSON serialization for List<String> to avoid InternalSerializationApi issues
        val json = history.joinToString("\", \"", "[\"", "\"]")
        prefs.edit().putString("search_history", json).apply()
        searchHistory.value = history
    }

    fun addToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        var history = searchHistory.value.toMutableList()
        history.remove(trimmed)
        history.add(0, trimmed)
        if (history.size > 10) history = history.take(10).toMutableList()
        saveSearchHistory(history)
    }

    fun removeFromHistory(query: String) {
        var history = searchHistory.value.toMutableList()
        history.remove(query)
        saveSearchHistory(history.toMutableList())
    }

    fun clearHistory() {
        saveSearchHistory(listOf<String>())
    }

    fun onSearchSubmit(query: String) {
        if (query.trim().isNotEmpty()) {
            addToHistory(query.trim())
        }
    }

    fun onQueryChange(query: String) {
        searchQuery.value = query
    }
}
