package com.vision.app.v0_4

/**
 * Conservative decision layer for notifications and screen context.
 * Sending messages or taking consequential actions requires a separate,
 * explicit, permission-aware executor.
 */
data class NotificationDecision(
    val action: String,
    val message: String,
    val reason: String
)

class IntelligentDecisionMaker {
    fun makeNotificationDecision(
        sender: String,
        messageContent: String
    ): NotificationDecision {
        val message = messageContent.trim()
        val urgent = listOf("urgent", "emergency", "help", "asap")
            .any { message.contains(it, ignoreCase = true) }

        return when {
            message.isBlank() -> NotificationDecision(
                action = "ignore",
                message = "",
                reason = "Notification contains no readable message"
            )
            urgent -> NotificationDecision(
                action = "inform_user",
                message = "Urgent notification from $sender",
                reason = "Message contains an urgency indicator"
            )
            else -> NotificationDecision(
                action = "inform_user",
                message = "New message from $sender",
                reason = "A new notification was received"
            )
        }
    }

    fun chooseScreenAction(screenText: String, userGoal: String): String {
        if (userGoal.isBlank()) return "ask_for_goal"
        if (screenText.isBlank()) return "request_screen_read"
        return "analyze_screen_and_propose_next_step"
    }
}
