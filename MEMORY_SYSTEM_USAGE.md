# Vision v0.4 Memory System — Usage

Create MemoryManager with an Android Context and call suspend methods from a coroutine.

Session: addToSessionMemory(sender, text, context) records recent conversation context; getSessionSummary() returns a summary; endSession() clears session context.

Save and retrieve: handleSaveCommand(content, folder, key) saves to supported folders; search(query) returns ranked results; getContact(name), getRoutine(), getProject(name), and getPersonalInfo() retrieve structured memories. createCustomFolder(name, description) creates a folder. deleteMemory(folder, key) and deleteCustomFolder(name) remove information.

Suggestions: call analyzAndSuggestSave(text, context). Present suggestions to the user and save only after confirmation. The current plaintext store must not be used for sensitive credentials.

Maintenance: getMemoryStats() and getMemorySizeBreakdown() report storage estimates. deduplicateAllMemories() removes repeated content from supported folders. validateMemoryIntegrity() performs a basic store-read check.

Example flow: in a coroutine, request suggestions for recognized user text, show them for confirmation, then call the relevant save method only after the user agrees. This module does not automatically activate; app or voice-service integration is required.