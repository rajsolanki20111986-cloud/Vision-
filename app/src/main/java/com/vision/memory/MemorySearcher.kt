package com.vision.memory

class MemorySearcher(private val store: MemoryStore) {
    suspend fun search(query: String): List<SearchResult> = store.searchMemories(query).sortedByDescending { it.relevanceScore }
    suspend fun searchFolder(folder: String, query: String): List<MemoryEntry> =
        store.loadAllMemories(folder).filter { it.content.contains(query, true) || it.key.contains(query, true) || it.tags.any { tag -> tag.contains(query, true) } }
    suspend fun getContact(contactName: String): String? = store.loadPersonal()?.contacts?.get(contactName)
    suspend fun getAllContacts(): Map<String, String> = store.loadPersonal()?.contacts ?: emptyMap()
    suspend fun getRoutine(): DailyRoutine? = store.loadRoutine()
    suspend fun getProject(projectName: String): ProjectMemory? = store.loadProject(projectName)
    suspend fun getPersonal(): PersonalMemory? = store.loadPersonal()
    suspend fun getFeature(projectName: String, featureTitle: String): FeatureEntry? =
        store.loadProject(projectName)?.featuresToAdd?.find { it.title.contains(featureTitle, true) }
    suspend fun advancedSearch(query: String, folder: String? = null, tags: List<String>? = null, importanceFilter: ImportancePriority? = null): List<SearchResult> =
        search(query).filter { (folder == null || it.folder == folder) && (tags == null || tags.any { tag -> it.memoryEntry.tags.contains(tag) }) }
    suspend fun getStats(): MemoryStats = store.getMemoryStats()
    suspend fun getAllFolders(): List<FolderInfo> = store.getAllFolders()
    suspend fun exists(folder: String, key: String): Boolean = store.loadMemory(folder, key) != null
}