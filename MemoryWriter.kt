package com.vision.memory

import kotlin.math.abs

class MemoryWriter(private val store: MemoryStore) {
    
    // Detect save intent from user command
    suspend fun detectSaveIntent(command: String): SaveIntent? {
        val lowerCommand = command.lowercase()
        
        return when {
            lowerCommand.contains("memory") && (lowerCommand.contains("save") || lowerCommand.contains("store")) -> {
                SaveIntent.EXPLICIT_SAVE
            }
            lowerCommand.contains("भूल गया") || lowerCommand.contains("forgot") -> {
                SaveIntent.FORGET_INTENT
            }
            lowerCommand.contains("delete") || lowerCommand.contains("remove") -> {
                SaveIntent.DELETE_INTENT
            }
            else -> null
        }
    }
    
    // Save to Personal Memory
    suspend fun saveToPersonal(key: String, value: String, contacts: Map<String, String>? = null): Boolean {
        return try {
            val existing = store.loadPersonal() ?: PersonalMemory()
            
            val updated = when {
                key.contains("phone") || key.contains("number") -> {
                    existing.copy(
                        contacts = contacts ?: existing.contacts.toMutableMap().apply {
                            // Extract name and number from value
                            val parts = value.split(":")
                            if (parts.size == 2) {
                                put(parts[0].trim(), parts[1].trim())
                            }
                        }
                    )
                }
                key.contains("name") -> existing.copy(name = value)
                key.contains("friend") -> existing.copy(friends = existing.friends + value)
                key.contains("preference") -> {
                    existing.copy(
                        preferences = existing.preferences.toMutableMap().apply {
                            put(key, value)
                        }
                    )
                }
                else -> existing.copy(
                    preferences = existing.preferences.toMutableMap().apply {
                        put(key, value)
                    }
                )
            }
            
            store.savePersonal(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Save to Daily Routine
    suspend fun saveToRoutine(timeSlot: String, activity: String): Boolean {
        return try {
            val existing = store.loadRoutine() ?: DailyRoutine()
            
            val updated = existing.copy(
                schedule = existing.schedule.toMutableMap().apply {
                    put(timeSlot, activity)
                }
            )
            
            store.saveRoutine(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Save to Project
    suspend fun saveToProject(projectName: String, bugOrFeature: String, type: String): Boolean {
        return try {
            val key = projectName.lowercase().replace(" ", "_")
            val existing = store.loadProject(projectName) ?: ProjectMemory(projectName)
            
            val updated = when (type.lowercase()) {
                "bug" -> {
                    existing.copy(
                        currentBugs = existing.currentBugs + BugEntry(
                            title = bugOrFeature,
                            description = bugOrFeature
                        )
                    )
                }
                "feature" -> {
                    existing.copy(
                        featuresToAdd = existing.featuresToAdd + FeatureEntry(
                            title = bugOrFeature,
                            description = bugOrFeature
                        )
                    )
                }
                "note" -> {
                    existing.copy(
                        notes = existing.notes + bugOrFeature
                    )
                }
                else -> existing
            }
            
            store.saveProject(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Save to Custom Folder
    suspend fun saveToCustom(folderName: String, key: String, value: String): Boolean {
        return try {
            val existing = store.loadCustom(folderName) ?: CustomMemory(folderName)
            
            val updated = existing.copy(
                content = existing.content.toMutableMap().apply {
                    put(key, value)
                }
            )
            
            store.saveCustom(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Save to Important
    suspend fun saveToImportant(title: String, content: String, category: ImportantCategory, isEncrypted: Boolean = false): Boolean {
        return try {
            val important = ImportantMemory(
                title = title,
                content = content,
                category = category,
                isEncrypted = isEncrypted,
                priority = ImportancePriority.HIGH
            )
            
            store.saveImportant(important)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Detect and save important info automatically
    suspend fun autoSaveIfImportant(text: String): Pair<Boolean, String> {
        return when {
            // API Key detection
            text.contains("api key", ignoreCase = true) || text.contains("apikey", ignoreCase = true) -> {
                Pair(true, "Sensitive API key detected. Save to /important with encryption?")
            }
            // Phone number detection
            text.matches(Regex(".*\\d{10}.*")) && text.contains("phone", ignoreCase = true) -> {
                Pair(true, "Phone number detected. Save to /personal/contacts?")
            }
            // Password/token detection
            text.contains("password", ignoreCase = true) || text.contains("token", ignoreCase = true) -> {
                Pair(true, "Sensitive credential detected. Save to /important with encryption?")
            }
            // Project-related
            text.contains("bug", ignoreCase = true) && text.contains("vision", ignoreCase = true) -> {
                Pair(true, "Bug found in Vision project. Save to /projects/vision/bugs?")
            }
            // Routine-related
            text.contains("wake up", ignoreCase = true) || text.contains("sleep", ignoreCase = true) -> {
                Pair(true, "Routine information detected. Save to /daily_routine?")
            }
            else -> Pair(false, "")
        }
    }
    
    // Update existing entry
    suspend fun updateMemory(folder: String, key: String, newValue: String): Boolean {
        return try {
            val existing = store.loadMemory(folder, key) ?: return false
            
            val updated = existing.copy(
                content = newValue,
                lastUpdatedAt = System.currentTimeMillis(),
                version = existing.version + 1
            )
            
            store.updateMemory(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Delete memory
    suspend fun deleteMemory(folder: String, key: String): Boolean {
        return store.deleteMemory(folder, key)
    }
    
    // Delete custom folder
    suspend fun deleteCustomFolder(folderName: String): Boolean {
        return store.deleteCustomFolder(folderName)
    }
    
    // Create new custom folder
    suspend fun createCustomFolder(folderName: String, description: String = ""): Boolean {
        return store.createCustomFolder(folderName, description)
    }
}

enum class SaveIntent {
    EXPLICIT_SAVE,      // User says "save करो"
    FORGET_INTENT,      // User says "forget"
    DELETE_INTENT,      // User says "delete"
    AUTO_SAVE,          // Vision detects important info
    ROUTINE_SAVE,       // Auto-save routine changes
    PROJECT_SAVE        // Auto-save project updates
}
