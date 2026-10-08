package com.vision.app

import androidx.compose.runtime.mutableStateOf

data class ActiveConversation(val personName: String, val phoneNumber: String? = null, val app: String, val startTime: Long = System.currentTimeMillis(), val messageCount: Int = 0, val lastMessage: String = "", val lastMessageTime: Long = System.currentTimeMillis(), val autoReplyEnabled: Boolean = false, val language: String = "en")

object ConversationManager {
    private var activeConversation = mutableStateOf<ActiveConversation?>(null)
    private val messageHistory = mutableListOf<Pair<String, String>>()
    fun startConversation(personName: String, phoneNumber: String? = null, app: String, language: String = "en") { activeConversation.value = ActiveConversation(personName, phoneNumber, app, language = language); messageHistory.clear() }
    fun enableAutoReply() { activeConversation.value?.let { activeConversation.value = it.copy(autoReplyEnabled = true) } }
    fun disableAutoReply() { activeConversation.value?.let { activeConversation.value = it.copy(autoReplyEnabled = false) } }
    fun addMessage(sender: String, message: String) { messageHistory.add(sender to message); activeConversation.value?.let { activeConversation.value = it.copy(messageCount = it.messageCount + 1, lastMessage = message, lastMessageTime = System.currentTimeMillis()) } }
    fun getConversation(): ActiveConversation? = activeConversation.value
    fun getMessageHistory(): List<Pair<String, String>> = messageHistory.toList()
    fun getContextString(): String { val conv = activeConversation.value ?: return ""; val historyStr = messageHistory.takeLast(5).joinToString("\n") { (sender, msg) -> "$sender: $msg" }; return "Conversation with ${conv.personName} on ${conv.app}\nLanguage: ${conv.language}\nDuration: ${(System.currentTimeMillis() - conv.startTime) / 1000}s\nMessage count: ${conv.messageCount}\n\nRecent messages:\n$historyStr" }
    fun endConversation() { activeConversation.value = null; messageHistory.clear() }
    fun isInAutoReplyMode(): Boolean = activeConversation.value?.autoReplyEnabled ?: false
    fun shouldReplyAutonomously(): Boolean { val conv = activeConversation.value ?: return false; return conv.autoReplyEnabled && System.currentTimeMillis() - conv.lastMessageTime < 30000 }
}
