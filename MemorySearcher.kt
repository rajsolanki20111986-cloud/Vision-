package com.vision.memory

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class MemoryStore(private val context: Context) {
    
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val memoryDir = File(context.filesDir, "vision_memory")
    
    // Encrypted SharedPreferences for sensitive data
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "vision_encrypted_memory",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val normalPrefs = context.getSharedPreferences("vision_memory", Context.MODE_PRIVATE)
    
    init {
        if (!memoryDir.exists()) {
            memoryDir.mkdirs()
        }
        initializeFolders()
    }
    
    private fun initializeFolders() {
        val folders = listOf("personal", "projects", "daily_routine", "custom", "important", "archive")
        for (folder in folders) {
            val folderFile = File(memoryDir, folder)
            if (!folderFile.exists()) {
                folderFile.mkdirs()
            }
        }
    }
    
    // ========== SAVE OPERATIONS ==========
    
    suspend fun saveMemory(entry: MemoryEntry): Boolean {
        return try {
            val folderPath = File(memoryDir, entry.folder)
            val fileName = "${entry.key}.json"
            val file = File(folderPath, fileName)
            
            val json = gson.toJson(entry)
            
            if (entry.isEncrypted) {
                // Save encrypted data
                encryptedPrefs.edit().putString("${entry.folder}_${entry.key}", json).apply()
            } else {
                // Save to JSON file
                file.writeText(json)
            }
            
            // Update memory index
            updateMemoryIndex()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun savePersonal(personal: PersonalMemory): Boolean {
        return try {
            val entry = MemoryEntry(
                folder = "personal",
                key = "profile",
                content = gson.toJson(personal),
                type = MemoryType.PERSONAL,
                isSensitive = true
            )
            saveMemory(entry)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun saveRoutine(routine: DailyRoutine): Boolean {
        return try {
            val entry = MemoryEntry(
                folder = "daily_routine",
                key = "schedule",
                content = gson.toJson(routine),
                type = MemoryType.ROUTINE
            )
            saveMemory(entry)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun saveProject(project: ProjectMemory): Boolean {
        return try {
            val entry = MemoryEntry(
                folder = "projects",
                key = project.projectName.lowercase().replace(" ", "_"),
                content = gson.toJson(project),
                type = MemoryType.PROJECT
            )
            saveMemory(entry)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun saveCustom(custom: CustomMemory): Boolean {
        return try {
            val folderName = custom.folderName.lowercase().replace(" ", "_")
            val folderPath = File(memoryDir, "custom/$folderName")
            if (!folderPath.exists()) {
                folderPath.mkdirs()
            }
            
            val entry = MemoryEntry(
                folder = "custom/$folderName",
                key = "data",
                content = gson.toJson(custom),
                type = MemoryType.CUSTOM
            )
            saveMemory(entry)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun saveImportant(important: ImportantMemory): Boolean {
        return try {
            val key = important.title.lowercase().replace(" ", "_")
            val entry = MemoryEntry(
                folder = "important",
                key = key,
                content = gson.toJson(important),
                type = MemoryType.IMPORTANT,
                isSensitive = important.isEncrypted,
                isEncrypted = important.isEncrypted,
                isImportant = true
            )
            saveMemory(entry)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ========== LOAD OPERATIONS ==========
    
    suspend fun loadMemory(folder: String, key: String): MemoryEntry? {
        return try {
            val folderPath = File(memoryDir, folder)
            val fileName = "$key.json"
            val file = File(folderPath, fileName)
            
            val json = if (file.exists()) {
                file.readText()
            } else {
                // Try encrypted storage
                encryptedPrefs.getString("${folder}_$key", null) ?: return null
            }
            
            gson.fromJson(json, MemoryEntry::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun loadPersonal(): PersonalMemory? {
        return try {
            val entry = loadMemory("personal", "profile") ?: return null
            gson.fromJson(entry.content, PersonalMemory::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun loadRoutine(): DailyRoutine? {
        return try {
            val entry = loadMemory("daily_routine", "schedule") ?: return null
            gson.fromJson(entry.content, DailyRoutine::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun loadProject(projectName: String): ProjectMemory? {
        return try {
            val key = projectName.lowercase().replace(" ", "_")
            val entry = loadMemory("projects", key) ?: return null
            gson.fromJson(entry.content, ProjectMemory::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun loadCustom(folderName: String): CustomMemory? {
        return try {
            val key = folderName.lowercase().replace(" ", "_")
            val entry = loadMemory("custom/$key", "data") ?: return null
            gson.fromJson(entry.content, CustomMemory::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun loadAllMemories(folder: String): List<MemoryEntry> {
        return try {
            val folderPath = File(memoryDir, folder)
            val entries = mutableListOf<MemoryEntry>()
            
            if (folderPath.exists()) {
                folderPath.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".json")) {
                        val json = file.readText()
                        val entry = gson.fromJson(json, MemoryEntry::class.java)
                        entries.add(entry)
                    }
                }
            }
            
            entries
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // ========== UPDATE OPERATIONS ==========
    
    suspend fun updateMemory(entry: MemoryEntry): Boolean {
        return try {
            val updated = entry.copy(
                lastUpdatedAt = System.currentTimeMillis(),
                version = entry.version + 1
            )
            saveMemory(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ========== DELETE OPERATIONS ==========
    
    suspend fun deleteMemory(folder: String, key: String): Boolean {
        return try {
            val folderPath = File(memoryDir, folder)
            val file = File(folderPath, "$key.json")
            
            val deleted = if (file.exists()) {
                file.delete()
            } else {
                encryptedPrefs.edit().remove("${folder}_$key").commit()
            }
            
            updateMemoryIndex()
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun deleteCustomFolder(folderName: String): Boolean {
        return try {
            val key = folderName.lowercase().replace(" ", "_")
            val folderPath = File(memoryDir, "custom/$key")
            
            val deleted = folderPath.deleteRecursively()
            updateMemoryIndex()
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ========== SEARCH OPERATIONS ==========
    
    suspend fun searchMemories(query: String): List<SearchResult> {
        return try {
            val results = mutableListOf<SearchResult>()
            val folders = listOf("personal", "projects", "daily_routine", "custom", "important")
            
            for (folder in folders) {
                val entries = loadAllMemories(folder)
                entries.forEach { entry ->
                    val contentMatches = entry.content.contains(query, ignoreCase = true)
                    val keyMatches = entry.key.contains(query, ignoreCase = true)
                    val tagMatches = entry.tags.any { it.contains(query, ignoreCase = true) }
                    
                    if (contentMatches || keyMatches || tagMatches) {
                        val score = when {
                            keyMatches -> 1.0f
                            contentMatches -> 0.8f
                            tagMatches -> 0.6f
                            else -> 0.5f
                        }
                        
                        results.add(SearchResult(
                            memoryEntry = entry,
                            relevanceScore = score,
                            matchedIn = when {
                                keyMatches -> "key"
                                contentMatches -> "content"
                                else -> "tags"
                            },
                            folder = folder
                        ))
                    }
                }
            }
            
            // Sort by relevance
            results.sortByDescending { it.relevanceScore }
            results
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    // ========== UTILITY OPERATIONS ==========
    
    suspend fun getMemoryStats(): MemoryStats {
        return try {
            val folders = listOf("personal", "projects", "daily_routine", "custom", "important", "archive")
            var totalSize = 0L
            var totalEntries = 0
            val folderBreakdown = mutableMapOf<String, Long>()
            val mostAccessedFolders = mutableListOf<Pair<String, Int>>()
            
            for (folder in folders) {
                val folderPath = File(memoryDir, folder)
                if (folderPath.exists()) {
                    val size = folderPath.walk().sumOf { it.length() }
                    val entryCount = folderPath.listFiles()?.size ?: 0
                    
                    folderBreakdown[folder] = size
                    totalSize += size
                    totalEntries += entryCount
                    
                    if (entryCount > 0) {
                        mostAccessedFolders.add(folder to entryCount)
                    }
                }
            }
            
            MemoryStats(
                totalSize = totalSize,
                folderBreakdown = folderBreakdown,
                totalFolders = folders.size,
                totalEntries = totalEntries,
                mostAccessedFolders = mostAccessedFolders.sortByDescending { it.second }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MemoryStats(0)
        }
    }
    
    suspend fun getAllFolders(): List<FolderInfo> {
        return try {
            val folderInfos = mutableListOf<FolderInfo>()
            
            // Standard folders
            val standard = listOf(
                FolderInfo("Personal", MemoryType.PERSONAL),
                FolderInfo("Projects", MemoryType.PROJECT),
                FolderInfo("Daily Routine", MemoryType.ROUTINE),
                FolderInfo("Important", MemoryType.IMPORTANT)
            )
            
            // Custom folders
            val customPath = File(memoryDir, "custom")
            val custom = customPath.listFiles()?.mapNotNull { folder ->
                if (folder.isDirectory) {
                    FolderInfo(
                        name = folder.name.replace("_", " "),
                        type = MemoryType.CUSTOM,
                        isCustom = true,
                        entryCount = folder.listFiles()?.size ?: 0
                    )
                } else null
            } ?: emptyList()
            
            folderInfos.addAll(standard)
            folderInfos.addAll(custom)
            folderInfos
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private suspend fun updateMemoryIndex() {
        // Rebuild search index for faster searches
        normalPrefs.edit().putLong("last_index_update", System.currentTimeMillis()).apply()
    }
    
    suspend fun createCustomFolder(folderName: String, description: String = ""): Boolean {
        return try {
            val custom = CustomMemory(
                folderName = folderName,
                description = description,
                isAutoCreated = false
            )
            saveCustom(custom)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun clearSessionMemory() {
        // Delete session memory when session ends
        try {
            val sessionFile = File(memoryDir, "session")
            sessionFile.deleteRecursively()
            sessionFile.mkdirs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
