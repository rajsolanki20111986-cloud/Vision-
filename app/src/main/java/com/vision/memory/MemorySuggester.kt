package com.vision.memory

class MemorySuggester {
    suspend fun analyzAndSuggest(text: String, context: String = ""): List<MemorySuggestion> {
        val t = text.lowercase()
        val result = mutableListOf<MemorySuggestion>()
        fun add(folder: String, reason: String, confidence: Float) {
            result.add(MemorySuggestion(suggestedContent = text, suggestedFolder = folder, reason = reason, confidence = confidence))
        }
        if ((t.contains("phone") || t.contains("नंबर") || t.contains("call")) && Regex("\\d{10}").containsMatchIn(text)) add("personal", "Possible contact information", .95f)
        if ((t.contains("bug") || t.contains("issue") || t.contains("error")) && (context.contains("vision", true) || context.contains("project", true))) add("projects/vision", "Project issue", .85f)
        if (t.contains("feature") || (t.contains("add") && context.contains("project", true))) add("projects/vision", "Possible feature idea", .80f)
        if (t.contains("wake up") || t.contains("sleep") || t.contains("exercise") || t.contains("routine")) add("daily_routine", "Routine information", .75f)
        if (t.contains("prefer") || t.contains("like") || t.contains("don't like")) add("personal", "Possible preference", .70f)
        if (t.contains("important") || t.contains("critical") || t.contains("goal")) add("important", "Potentially important note", .80f)
        return result.sortedByDescending { it.confidence }
    }
    suspend fun generateSuggestionMessage(suggestion: MemorySuggestion): String =
        if (suggestion.confidence > .9f) "यह महत्वपूर्ण लग रहा है। ${suggestion.suggestedFolder} में save करूँ?"
        else "क्या इसे ${suggestion.suggestedFolder} में save करूँ?"
    suspend fun suggestCustomFolder(text: String): String? = when {
        text.contains("youtube", true) && text.contains("video", true) -> "youtube_videos"
        text.contains("book", true) || text.contains("read", true) -> "books_to_read"
        text.contains("idea", true) && text.length > 50 -> "ideas"
        text.contains("website", true) || text.contains("link", true) -> "useful_links"
        text.contains("recipe", true) || text.contains("cook", true) -> "recipes"
        else -> null
    }
    suspend fun contextualSuggestion(userInput: String, conversationHistory: List<String>): MemorySuggestion? {
        val combined = (conversationHistory.takeLast(5) + userInput).joinToString(" ")
        val person = Regex("\\b[A-Z][a-z]{2,}\\b").findAll(combined).map { it.value }.groupingBy { it }.eachCount().entries.firstOrNull { it.value > 2 }?.key ?: return null
        return MemorySuggestion(suggestedContent = "Mentioned person: $person", suggestedFolder = "personal", reason = "Repeated mention", confidence = .75f)
    }
    suspend fun suggestFolderCreation(conversationHistory: List<String>): String? {
        val t = conversationHistory.joinToString(" ").lowercase()
        return when {
            t.split("book").size > 3 -> "books_to_read"
            t.split("youtube").size > 3 -> "youtube_videos"
            t.split("idea").size > 3 -> "ideas"
            t.split("link").size > 3 -> "useful_links"
            else -> null
        }
    }
}