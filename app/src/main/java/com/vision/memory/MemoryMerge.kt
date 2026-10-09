package com.vision.memory

class MemoryMerge(private val store: MemoryStore, private val searcher: MemorySearcher) {
    suspend fun smartSave(folder: String, key: String, newContent: String): Boolean {
        val old = store.loadMemory(folder, key)
        return if (old == null) store.saveMemory(MemoryEntry(folder = folder, key = key, content = newContent, type = when {
            folder.contains("personal") -> MemoryType.PERSONAL
            folder.contains("project") -> MemoryType.PROJECT
            folder.contains("routine") -> MemoryType.ROUTINE
            folder.contains("important") -> MemoryType.IMPORTANT
            folder.contains("custom") -> MemoryType.CUSTOM
            else -> MemoryType.SESSION
        })) else store.updateMemory(old.copy(content = newContent, lastUpdatedAt = System.currentTimeMillis(), version = old.version + 1))
    }
    suspend fun mergePersonal(data: PersonalMemory): Boolean {
        val old = store.loadPersonal() ?: PersonalMemory()
        return store.savePersonal(PersonalMemory(data.name.ifEmpty { old.name }, (old.friends + data.friends).distinct(), (old.family + data.family).distinct(), old.contacts + data.contacts, old.preferences + data.preferences, old.relationships + data.relationships))
    }
    suspend fun mergeProject(data: ProjectMemory): Boolean {
        val old = store.loadProject(data.projectName) ?: return store.saveProject(data)
        return store.saveProject(data.copy(description = data.description.ifEmpty { old.description }, currentBugs = (old.currentBugs + data.currentBugs).distinctBy { it.title }, featuresToAdd = (old.featuresToAdd + data.featuresToAdd).distinctBy { it.title }, notes = (old.notes + data.notes).distinct(), createdAt = old.createdAt))
    }
    suspend fun mergeRoutine(data: DailyRoutine): Boolean {
        val old = store.loadRoutine() ?: DailyRoutine()
        return store.saveRoutine(data.copy(habits = (old.habits + data.habits).distinct(), schedule = old.schedule + data.schedule))
    }
    suspend fun deduplicateFolder(folder: String): Int {
        val seen = mutableSetOf<String>()
        var removed = 0
        store.loadAllMemories(folder).forEach { if (!seen.add(it.content)) { store.deleteMemory(folder, it.key); removed++ } }
        return removed
    }
}