package com.vision.app.v0_4

/**
 * Typed representation of a screen-reading result.
 * Connect this interface to Android MediaProjection, AccessibilityService,
 * OCR, or the project's chosen screen-capture implementation.
 */
data class ScreenTextItem(
    val text: String,
    val role: String = "text",
    val clickable: Boolean = false
)

data class ScreenEnemy(
    val id: String,
    val threat: String = "unknown",
    val weapon: String? = null
)

data class GameState(
    val health: Int = 100,
    val ammo: Int = 0,
    val inventory: List<String> = emptyList(),
    val enemies: List<ScreenEnemy> = emptyList(),
    val minimap: MinimapState? = null
)

data class MinimapState(val playerPosition: String? = null)

data class ScreenSnapshot(
    val items: List<ScreenTextItem> = emptyList(),
    val gameState: GameState? = null,
    val capturedAtMillis: Long = System.currentTimeMillis()
)

class CompleteScreenReader(
    private val captureProvider: (suspend () -> ScreenSnapshot?)? = null
) {
    suspend fun readScreenOnce(): ScreenSnapshot? = captureProvider?.invoke()

    fun findButton(snapshot: ScreenSnapshot, label: String): ScreenTextItem? =
        snapshot.items.firstOrNull {
            it.clickable && it.text.contains(label, ignoreCase = true)
        }

    fun visibleText(snapshot: ScreenSnapshot): String =
        snapshot.items.joinToString("\n") { it.text }

    // A capture provider must be supplied by an authorized Android screen reader.
}
