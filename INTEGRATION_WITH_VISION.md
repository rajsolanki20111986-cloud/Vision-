# Integrating Memory System with Vision v0.4

1. The app module needs Gson and Kotlin coroutines; the v0.4 Gradle update adds these and updates the app version to 0.4.
2. Create one MemoryManager using applicationContext for the chat or voice feature and reuse it during the app session.
3. When text is recognized, add it to session memory. Call analyzAndSuggestSave with the current topic as context. If suggestions exist, ask the user before saving.
4. For explicit save commands, call handleSaveCommand with the selected folder and key. Use search or structured getters to answer memory questions.
5. Clear session-only context when the session ends while retaining intentionally saved long-term entries.
6. Call suspend APIs from a coroutine. Expose list, search, delete, and clear controls; confirm destructive operations.
7. Security limitation: current persistence uses ordinary app-private SharedPreferences with JSON serialization and is not encrypted. Do not store secrets until encrypted storage is implemented and tested.
8. Build the debug APK after adding the module. Compilation does not prove the UI/voice flow calls the manager. Device testing should cover save, retrieve, update, delete, app restart persistence, and session clearing before calling the feature fully integrated.