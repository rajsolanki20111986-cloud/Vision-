# Vision v0.4 Memory System — Design

Purpose: add session context, personal information, routines, projects, custom folders, important notes, search, merge/deduplication, and save suggestions.

Components: MemoryModels.kt defines models; SessionMemory.kt tracks recent context; MemoryStore.kt persists data locally; MemoryWriter.kt handles save/update/delete; MemorySearcher.kt searches entries; MemoryMerge.kt merges updates and removes duplicates; MemorySuggester.kt proposes candidate memories; MemoryManager.kt is the facade for app integration.

Privacy: the current store uses app-private SharedPreferences and Gson, but is not encrypted. Do not store passwords, API keys, access tokens, or other secrets. Implement and test Android Keystore-backed encryption before storing sensitive data.

Integration boundary: the manager must be called by the app chat or voice flow; adding these classes alone does not automatically connect them to the UI. Suggestions should be confirmed by the user before saving.

Reliability: persistence methods return Boolean where practical. Review destructive operations and deduplication behavior before deleting user-created data.