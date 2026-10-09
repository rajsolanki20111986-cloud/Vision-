package com.vision.memory

import java.io.Serializable
import java.util.UUID

data class MemoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: MemoryType = MemoryType.SESSION,
    val folder: String,
    val key: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val isImportant: Boolean = false,
    val isSensitive: Boolean = false,
    val isEncrypted: Boolean = false,
    val version: Int = 1,
    val accessCount: Int = 0
) : Serializable

data class SessionMemoryEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String,
    val text: String,
    val context: String = "",
    val id: String = UUID.randomUUID().toString()
) : Serializable

data class PersonalMemory(
    val name: String = "",
    val friends: List<String> = emptyList(),
    val family: List<String> = emptyList(),
    val contacts: Map<String, String> = emptyMap(),
    val preferences: Map<String, String> = emptyMap(),
    val relationships: Map<String, String> = emptyMap()
) : Serializable

data class DailyRoutine(
    val wakeUpTime: String = "6:00 AM",
    val exerciseTime: String = "6:30 AM",
    val exerciseDuration: String = "1 hour",
    val workStartTime: String = "9:00 AM",
    val workEndTime: String = "6:00 PM",
    val sleepTime: String = "11:00 PM",
    val habits: List<String> = emptyList(),
    val schedule: Map<String, String> = emptyMap()
) : Serializable

data class ProjectMemory(
    val projectName: String,
    val description: String = "",
    val status: ProjectStatus = ProjectStatus.IN_PROGRESS,
    val currentBugs: List<BugEntry> = emptyList(),
    val featuresToAdd: List<FeatureEntry> = emptyList(),
    val progress: String = "",
    val notes: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis()
) : Serializable

data class BugEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val severity: BugSeverity = BugSeverity.MEDIUM,
    val status: BugStatus = BugStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val fixedAt: Long? = null
) : Serializable

data class FeatureEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val priority: FeaturePriority = FeaturePriority.MEDIUM,
    val status: FeatureStatus = FeatureStatus.PLANNED,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

data class CustomMemory(
    val folderName: String,
    val description: String = "",
    val content: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val isAutoCreated: Boolean = false,
    val suggestedByVision: Boolean = false
) : Serializable

data class ImportantMemory(
    val title: String,
    val content: String,
    val category: ImportantCategory,
    val isPinned: Boolean = false,
    val isEncrypted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: ImportancePriority = ImportancePriority.HIGH
) : Serializable

data class SearchResult(
    val memoryEntry: MemoryEntry,
    val relevanceScore: Float,
    val matchedIn: String,
    val folder: String
) : Serializable

data class MemoryStats(
    val totalSize: Long,
    val folderBreakdown: Map<String, Long> = emptyMap(),
    val totalFolders: Int = 0,
    val totalEntries: Int = 0,
    val mostAccessedFolders: List<Pair<String, Int>> = emptyList(),
    val lastBackup: Long? = null
) : Serializable

data class MemorySuggestion(
    val id: String = UUID.randomUUID().toString(),
    val suggestedContent: String,
    val suggestedFolder: String,
    val reason: String,
    val confidence: Float,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

enum class MemoryType { SESSION, PERSONAL, PROJECT, ROUTINE, CUSTOM, IMPORTANT, ARCHIVE }
enum class ProjectStatus { NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, ON_HOLD }
enum class BugSeverity { LOW, MEDIUM, HIGH, CRITICAL }
enum class BugStatus { OPEN, IN_PROGRESS, FIXED, VERIFIED, CLOSED }
enum class FeaturePriority { LOW, MEDIUM, HIGH, CRITICAL }
enum class FeatureStatus { PLANNED, IN_PROGRESS, COMPLETED, TESTING, DEPLOYED }
enum class ImportantCategory { CRITICAL_DECISION, LONG_TERM_GOAL, API_KEY, TOKEN, URGENT_TODO, IMPORTANT_NOTE }
enum class ImportancePriority { LOW, MEDIUM, HIGH, CRITICAL }

data class FolderInfo(
    val name: String,
    val type: MemoryType,
    val description: String = "",
    val entryCount: Int = 0,
    val sizeInBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val isCustom: Boolean = false
) : Serializable

data class MemoryIndex(
    val allEntries: List<MemoryEntry> = emptyList(),
    val folderList: List<FolderInfo> = emptyList(),
    val tagIndex: Map<String, List<String>> = emptyMap(),
    val searchIndex: Map<String, List<String>> = emptyMap(),
    val lastUpdated: Long = System.currentTimeMillis()
) : Serializable
