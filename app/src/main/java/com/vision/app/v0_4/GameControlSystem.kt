package com.vision.app.v0_4

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

enum class GamePhase { LANDING, LOOTING, MOVING_TO_ZONE, COMBAT, ESCAPING, LATE_GAME }

data class GameStatus(
    val phase: GamePhase,
    val health: Int,
    val ammo: Int,
    val inventory: List<String>,
    val enemies: List<String>,
    val gameOver: Boolean = false
)

/**
 * v0.4 tactical game-state helper.
 * Screen recognition and touch execution must be connected by the host app.
 */
class GameControlSystem(
    private val screenReader: CompleteScreenReader,
    private val decisionMaker: IntelligentDecisionMaker
) {
    @Volatile private var active = false

    suspend fun initializeGameControl(task: String): String {
        active = true
        return "Game-control loop prepared for task: $task"
    }

    suspend fun manageGame(maxIterations: Int = 20): List<String> =
        withContext(Dispatchers.Default) {
            val actions = mutableListOf<String>()
            repeat(maxIterations.coerceAtLeast(0)) {
                if (!active) return@withContext actions
                val snapshot = screenReader.readScreenOnce() ?: return@repeat
                val state = snapshot.gameState ?: return@repeat
                val status = GameStatus(
                    phase = when {
                        state.enemies.isNotEmpty() -> GamePhase.COMBAT
                        state.inventory.isEmpty() -> GamePhase.LOOTING
                        state.health < 30 -> GamePhase.ESCAPING
                        else -> GamePhase.MOVING_TO_ZONE
                    },
                    health = state.health,
                    ammo = state.ammo,
                    inventory = state.inventory,
                    enemies = state.enemies.map { it.id }
                )
                val action = when {
                    status.health < 20 -> "seek_cover_and_heal"
                    status.enemies.isNotEmpty() && status.ammo > 0 -> "assess_threat_before_engaging"
                    status.ammo <= 0 -> "look_for_ammunition"
                    status.inventory.isEmpty() -> "look_for_equipment"
                    else -> "move_toward_safe_area"
                }
                actions.add(action)
                delay(500)
            }
            actions
        }

    fun stop() { active = false }
}
