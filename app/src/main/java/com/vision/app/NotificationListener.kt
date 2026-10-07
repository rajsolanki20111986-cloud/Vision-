package com.vision.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Monitors incoming notifications (messages from WhatsApp, Telegram, Instagram, etc).
 * When a new message arrives, Vision tells Raj who messaged.
 * Raj can then say "reply to [person]" and Vision will continue the conversation.
 */
class VisionNotificationListener : NotificationListenerService() {

    companion object {
        val newMessage = MutableStateFlow<Pair<String, String>?>(null) // (sender, message)
        @Volatile var inst: VisionNotificationListener? = null
    }

    override fun onServiceConnected() { inst = this }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { processNotification(sbn) }
    override fun onDestroy() { inst = null; super.onDestroy() }

    private fun processNotification(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        val n = sbn.notification ?: return

        // Block payment/banking apps
        if (listOf("paisa", "phonepe", "paytm", "paypal", "bank", "wallet").any { pkg.contains(it, ignoreCase = true) })
            return

        // Extract sender and message text from notification
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
            pkg.contains("sms") -> "SMS"
            else -> title
        }

        newMessage.value = appName to text
    }
}
