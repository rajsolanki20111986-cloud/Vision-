package com.vision.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MemoryManager(context: Context) {
    private val store = MemoryStore(context)
    private val sessionMemory = SessionMemory()
    private val writer = MemoryWriter(store)
    private val searcher = MemorySearcher(store)
    private val merger = MemoryMerge(store, searcher)
    private val suggester = MemorySuggester()

    suspend fun addToSessionMemory(sender: String, text: String, context: String = "") = sessionMemory.addMessage(sender, text, context)
    suspend fun getSessionSummary(): String = sessionMemory.getSessionSummary()
    suspend fun endSession() { sessionMemory.clear(); store.clearSessionMemory() }
    suspend fun saveUserMessage(text: String, folder: String = "session"): Boolean { addToSessionMemory("user", text); return true }
    suspend fun handleSaveCommand(content: String, folder: String?, key: String? = null): Boolean = withContext(Dispatchers.IO) {
        when (folder?.lowercase()) {
            "personal" -> { val parts = content.split(":", limit = 2); writer.saveToPersonal(if (parts.size == 2) parts[0].trim() else key ?: "info", if (parts.size == 2) parts[1].trim() else content) }
            "routine" -> writer.saveToRoutine(key ?: "custom", content)
            "project" -> writer.saveToProject(key ?: "general", content, "note")
            "important" -> writer.saveToImportant(key ?: "note", content, ImportantCategory.IMPORTANT_NOTE)
            "custom" -> if (key != null) writer.saveToCustom(key, "info", content) else false
            else -> false
        }
    }
    suspend fun createCustomFolder(folderName: String, description: String = "") = withContext(Dispatchers.IO) { store.createCustomFolder(folderName, description) }
    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) { searcher.search(query) }
    suspend fun getContact(name: String): String? = withContext(Dispatchers.IO) { searcher.getContact(name) }
    suspend fun getContacts(): Map<String, String> = withContext(Dispatchers.IO) { searcher.getAllContacts() }
    suspend fun getRoutine(): DailyRoutine? = withContext(Dispatchers.IO) { searcher.getRoutine() }
    suspend fun getProject(name: String): ProjectMemory? = withContext(Dispatchers.IO) { searcher.getProject(name) }
    suspend fun getPersonalInfo(): PersonalMemory? = withContext(Dispatchers.IO) { searcher.getPersonal() }
    suspend fun analyzAndSuggestSave(text: String, context: String = ""): List<MemorySuggestion> = withContext(Dispatchers.IO) { suggester.analyzAndSuggest(text, context) }
    suspend fun getSuggestionMessage(suggestion: MemorySuggestion): String = suggester.generateSuggestionMessage(suggestion)
    suspend fun suggestCustomFolder(text: String): String? = suggester.suggestCustomFolder(text)
    suspend fun deleteMemory(folder: String, key: String) = withContext(Dispatchers.IO) { writer.deleteMemory(folder, key) }
    suspend fun deleteCustomFolder(folderName: String) = withContext(Dispatchers.IO) { writer.deleteCustomFolder(folderName) }
    suspend fun getMemoryStats() = searcher.getStats()
    suspend fun getAllFolders() = searcher.getAllFolders()
    suspend fun getMemorySizeBreakdown(): Map<String, String> = withContext(Dispatchers.IO) {
        searcher.getStats().folderBreakdown.mapValues { (_, size) -> when { size < 1024 -> "$size B"; size < 1024 * 1024 -> "${size / 1024} KB"; else -> "${size / (1024 * 1024)} MB" } }
    }
    suspend fun processVoiceCommand(command: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        when {
            command.contains("memory", true) && command.contains("save", true) -> Pair(true, "किस folder में save करूँ? (Personal/Projects/Routine/Custom)")
            command.contains("क्या") || command.contains("what", true) -> search(command).let { if (it.isEmpty()) Pair(false, "Memory में कुछ नहीं मिला") else Pair(true, it.take(5).joinToString("\n") { r -> "- ${r.memoryEntry.key} (${r.folder}): ${r.memoryEntry.content}" }) }
            command.contains("delete", true) || command.contains("भूल गया") -> Pair(true, "किस memory को delete करूँ?")
            command.contains("routine", true) || command.contains("schedule", true) -> getRoutine()?.let { Pair(true, "Daily Routine: ${it.schedule}") } ?: Pair(false, "Routine memory में कुछ नहीं है")
            command.contains("number", true) || command.contains("call", true) || command.contains("नंबर") -> { val name = command.substringAfterLast(" "); getContact(name)?.let { Pair(true, "$name का नंबर है: $it") } ?: Pair(false, "$name का नंबर memory में नहीं है") }
            else -> Pair(false, "")
        }
    }
    suspend fun deduplicateAllMemories(): Int = withContext(Dispatchers.IO) { listOf("personal", "projects", "daily_routine", "custom", "important").sumOf { merger.deduplicateFolder(it) } }
    suspend fun validateMemoryIntegrity(): Boolean = runCatching { searcher.getStats(); true }.getOrDefault(false)
}