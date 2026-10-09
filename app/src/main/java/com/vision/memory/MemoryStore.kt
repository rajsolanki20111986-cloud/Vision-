package com.vision.memory

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MemoryStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vision_memory_v04", Context.MODE_PRIVATE)
    private val gson = Gson()
    private fun key(folder: String, key: String) = "memory:$folder:$key"
    private inline fun <reified T> decode(value: String?): T? = try { if (value == null) null else gson.fromJson(value, object : TypeToken<T>() {}.type) } catch (_: Exception) { null }

    fun saveMemory(entry: MemoryEntry): Boolean = runCatching { prefs.edit().putString(key(entry.folder, entry.key), gson.toJson(entry)).commit() }.getOrDefault(false)
    fun updateMemory(entry: MemoryEntry): Boolean = saveMemory(entry)
    fun loadMemory(folder: String, key: String): MemoryEntry? = decode(prefs.getString(key(folder, key), null))
    fun loadAllMemories(folder: String): List<MemoryEntry> = prefs.all.filterKeys { it.startsWith("memory:$folder:") }.values.mapNotNull { decode<MemoryEntry>(it as? String) }
    fun deleteMemory(folder: String, key: String): Boolean = prefs.edit().remove(key(folder, key)).commit()
    fun savePersonal(data: PersonalMemory): Boolean = put("personal", data)
    fun loadPersonal(): PersonalMemory? = get("personal")
    fun saveProject(data: ProjectMemory): Boolean = put("project:${data.projectName}", data)
    fun loadProject(name: String): ProjectMemory? = get("project:$name")
    fun saveRoutine(data: DailyRoutine): Boolean = put("routine", data)
    fun loadRoutine(): DailyRoutine? = get("routine")
    fun saveCustom(folder: String, data: CustomMemory): Boolean = put("custom:$folder", data)
    fun loadCustom(folder: String): CustomMemory? = get("custom:$folder")
    fun saveImportant(data: ImportantMemory): Boolean = put("important:${data.title}", data)
    fun loadImportant(title: String): ImportantMemory? = get("important:$title")
    fun createCustomFolder(name: String, description: String): Boolean = put("folder:$name", FolderInfo(name, MemoryType.CUSTOM, description, isCustom = true))
    fun deleteCustomFolder(name: String): Boolean {
        val edit = prefs.edit().remove("folder:$name")
        prefs.all.keys.filter { it.contains(":$name:") }.forEach { edit.remove(it) }
        return edit.commit()
    }
    fun clearSessionMemory() { prefs.edit().remove("session").apply() }
    fun allFolders(): List<FolderInfo> = prefs.all.filterKeys { it.startsWith("folder:") }.values.mapNotNull { decode<FolderInfo>(it as? String) }
    fun stats(): MemoryStats {
        val entries = prefs.all.filterKeys { it.startsWith("memory:") }.values.mapNotNull { decode<MemoryEntry>(it as? String) }
        return MemoryStats(entries.sumOf { it.content.toByteArray().size.toLong() }, entries.groupBy { it.folder }.mapValues { (_, v) -> v.sumOf { it.content.toByteArray().size.toLong() } }, allFolders().size, entries.size)
    }
    private fun put(k: String, value: Any): Boolean = runCatching { prefs.edit().putString(k, gson.toJson(value)).commit() }.getOrDefault(false)
    private inline fun <reified T> get(k: String): T? = decode(prefs.getString(k, null))
}