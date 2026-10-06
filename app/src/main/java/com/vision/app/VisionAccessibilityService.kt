package com.vision.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Vision's hands and eyes: reads the UI tree of any app and taps, types and scrolls. */
class VisionAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var inst: VisionAccessibilityService? = null
        // Payment / banking style apps are never operated. Extend as needed.
        private val BLOCK = listOf("paisa", "phonepe", "paytm", "paypal", "upi", "bhim", "bank", "wallet", "razorpay")
    }

    override fun onServiceConnected() { inst = this; Prefs.init(this) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: android.content.Intent?): Boolean { inst = null; return super.onUnbind(intent) }
    override fun onDestroy() { inst = null; super.onDestroy() }

    /** Returns a refusal message, or null when acting is allowed. */
    fun blockedReason(): String? {
        if (!Prefs.controlEnabled) return "Phone control is switched off by the user in Vision settings."
        val pkg = rootInActiveWindow?.packageName?.toString() ?: return null
        if (BLOCK.any { pkg.contains(it, ignoreCase = true) })
            return "Blocked: this looks like a payment or banking app. Do not operate it."
        return null
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "No screen available"
        val sb = StringBuilder("app=${root.packageName}
")
        val r = Rect()
        var n = 0
        fun walk(nd: AccessibilityNodeInfo?) {
            if (nd == null || n >= 120) return
            val label = (nd.text ?: nd.contentDescription)?.toString()?.take(80)
            if (!label.isNullOrBlank() && nd.isVisibleToUser) {
                nd.getBoundsInScreen(r)
                val tag = when { nd.isEditable -> "[input] "; nd.isClickable -> "[btn] "; else -> "" }
                sb.append(tag).append(label).append(" @").append(r.centerX()).append(',').append(r.centerY()).append('
')
                n++
            }
            for (i in 0 until nd.childCount) walk(nd.getChild(i))
        }
        walk(root)
        return sb.toString()
    }

    fun clickText(t: String): String {
        val root = rootInActiveWindow ?: return "No screen available"
        val node = root.findAccessibilityNodeInfosByText(t).firstOrNull { it.isVisibleToUser } ?: return "Not found: $t"
        var c: AccessibilityNodeInfo? = node
        while (c != null && !c.isClickable) c = c.parent
        if (c != null && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return "Tapped $t"
        val r = Rect(); node.getBoundsInScreen(r)
        return if (tap(r.centerX(), r.centerY())) "Tapped $t by position" else "Could not tap $t"
    }

    fun tap(x: Int, y: Int): Boolean {
        val p = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        return dispatchGesture(
            GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null
        )
    }

    fun typeText(t: String): String {
        val root = rootInActiveWindow ?: return "No screen available"
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: findEditable(root) ?: return "No input field found"
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, t) }
        return if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) "Typed" else "Typing failed"
    }

    private fun findEditable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isEditable) return n
        for (i in 0 until n.childCount) findEditable(n.getChild(i))?.let { return it }
        return null
    }

    fun pressEnter(): String {
        if (Build.VERSION.SDK_INT < 30) return "Enter key is not supported on this Android; tap the search or send button instead."
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return "No focused input"
        return if (node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)) "Enter pressed" else "Enter failed"
    }

    /** direction = which part of the content you want to see: "down" shows content further down. */
    fun scroll(dir: String): String {
        val m = resources.displayMetrics
        val w = m.widthPixels; val h = m.heightPixels
        val ok = when (dir) {
            "down" -> swipe(w / 2, h * 7 / 10, w / 2, h * 3 / 10)
            "up" -> swipe(w / 2, h * 3 / 10, w / 2, h * 7 / 10)
            "right" -> swipe(w * 3 / 4, h / 2, w / 4, h / 2)
            "left" -> swipe(w / 4, h / 2, w * 3 / 4, h / 2)
            else -> false
        }
        return if (ok) "Scrolled $dir" else "Scroll failed"
    }

    private fun swipe(x1: Int, y1: Int, x2: Int, y2: Int): Boolean {
        val p = Path().apply { moveTo(x1.toFloat(), y1.toFloat()); lineTo(x2.toFloat(), y2.toFloat()) }
        return dispatchGesture(
            GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p, 0, 300)).build(), null, null
        )
    }

    fun press(key: String): String {
        val a = when (key) {
            "back" -> GLOBAL_ACTION_BACK
            "home" -> GLOBAL_ACTION_HOME
            "recents" -> GLOBAL_ACTION_RECENTS
            "notifications" -> GLOBAL_ACTION_NOTIFICATIONS
            else -> return "Unknown key: $key"
        }
        return if (performGlobalAction(a)) "Done" else "Failed"
    }
}
