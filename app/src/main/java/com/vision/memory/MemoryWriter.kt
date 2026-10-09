package com.vision.memory

class MemoryWriter(private val store: MemoryStore) {
    suspend fun detectSaveIntent(command: String): SaveIntent? {
        val c = command.lowercase()
        return when {
            c.contains("memory") && (c.contains("save") || c.contains("store")) -> SaveIntent.EXPLICIT_SAVE
            c.contains("भूल गया") || c.contains("forgot") -> SaveIntent.FORGET_INTENT
            c.contains("delete") || c.contains("remove") -> SaveIntent.DELETE_INTENT
            else -> null
        }
    }
    suspend fun saveToPersonal(key: String, value: String, contacts: Map<String, String>? = null): Boolean {
        val old = store.loadPersonal() ?: PersonalMemory()
        val updated = when {
            key.contains("phone", true) || key.contains("number", true) -> old.copy(contacts = contacts ?: old.contacts + (value.substringBefore(":").trim() to value.substringAfter(":", "").trim()))
            key.contains("name", true) -> old.copy(name = value)
            key.contains("friend", true) -> old.copy(friends = (old.friends + value).distinct())
            else -> old.copy(preferences = old.preferences + (key to value))
        }
        return store.savePersonal(updated)
    }
    suspend fun saveToRoutine(timeSlot: String, activity: String): Boolean {
        val old = store.loadRoutine() ?: DailyRoutine()
        return store.saveRoutine(old.copy(schedule = old.schedule + (timeSlot to activity)))
    }
    suspend fun saveToProject(projectName: String, bugOrFeature: String, type: String): Boolean {
        val old = store.loadProject(projectName) ?: ProjectMemory(projectName)
        val updated = when (type.lowercase()) {
            "bug" -> old.copy(currentBugs = old.currentBugs + BugEntry(title = bugOrFeature, description = bugOrFeature))
            "feature" -> old.copy(featuresToAdd = old.featuresToAdd + FeatureEntry(title = bugOrFeature, description = bugOrFeature))
            else -> old.copy(notes = old.notes + bugOrFeature)
        }
        return store.saveProject(updated)
    }
    suspend fun saveToCustom(folderName: String, key: String, value: String): Boolean {
        val old = store.loadCustom(folderName) ?: CustomMemory(folderName)
        return store.saveCustom(folderName, old.copy(content = old.content + (key to value)))
    }
    suspend fun saveToImportant(title: String, content: String, category: ImportantCategory, isEncrypted: Boolean = false): Boolean =
        store.saveImportant(ImportantMemory(title, content, category, isEncrypted = isEncrypted))
    suspend fun updateMemory(folder: String, key: String, newValue: String): Boolean {
        val old = store.loadMemory(folder, key) ?: return false
        return store.updateMemory(old.copy(content = newValue, lastUpdatedAt = System.currentTimeMillis(), version = old.version + 1))
    }
    suspend fun deleteMemory(folder: String, key: String) = store.deleteMemory(folder, key)
    suspend fun deleteCustomFolder(folderName: String) = store.deleteCustomFolder(folderName)
    suspend fun createCustomFolder(folderName: String, description: String = "") = store.createCustomFolder(folderName, description)
}
enum class SaveIntent { EXPLICIT_SAVE, FORGET_INTENT, DELETE_INTENT, AUTO_SAVE, ROUTINE_SAVE, PROJECT_SAVE }