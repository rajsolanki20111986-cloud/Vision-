package com.vision.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow

class VisionNotificationListener : NotificationListenerService() {

    companion object {
        val newMessage = MutableStateFlow<Pair<String, String>?>(null)
        @Volatile var inst: VisionNotificationListener? = null
    }

    override fun onListenerConnected() { inst = this }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { processNotification(sbn) }
    override fun onDestroy() { inst = null; super.onDestroy() }

    private fun processNotification(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName
        val n = sbn.notification

        if (listOf("paisa", "phonepe", "paytm", "paypal", "bank", "wallet").any { pkg.contains(it, ignoreCase = true) })
            return

        val text = n.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
            ?: n.extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return

        val title = n.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: pkg
        if (text.isBlank()) return

        val appName = when {
            pkg.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
            pkg.contains("telegram", ignoreCase = true) -> "Telegram"
            pkg.contains("instagram", ignoreCase = true) -> "Instagram"
            pkg.contains("messenger", ignoreCase = true) -> "Messenger"
            pkg.contains("sms", ignoreCase = true) -> "SMS"
            else -> title
        }

        newMessage.value = appName to text
        ConversationManager.getConversation()?.let { conv ->
            if (conv.autoReplyEnabled && conv.personName.equals(title, ignoreCase = true)) {
                ConversationManager.addMessage(title, text)
            }
        }
    }
}
