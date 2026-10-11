package com.vision.app.v0_4

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Small orchestration layer for the v0.4 components.
 * Voice, Gemini, accessibility actions, and screen capture are injected by
 * the host app; this class does not pretend those integrations are configured.
 */
class UpdatedLiveSession(
    private val screenReader: CompleteScreenReader,
    private val appUnderstanding: UniversalAppUnderstanding,
    private val decisionMaker: IntelligentDecisionMaker
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var sessionJob: Job? = null
    @Volatile private var active = false

    fun start(onUpdate: (String) -> Unit = {}) {
        if (active) return
        active = true
        sessionJob = scope.launch {
            val snapshot = screenReader.readScreenOnce()
            if (!active) return@launch
            val description = snapshot?.let { screenReader.visibleText(it) }
                ?.ifBlank { "Screen was captured but no text was recognized." }
                ?: "Screen capture is not connected or permission is unavailable."
            onUpdate(description)
        }
    }

    fun analyzeGoal(goal: String): String =
        decisionMaker.chooseScreenAction("screen context supplied separately", goal)

    fun stopSession() {
        active = false
        sessionJob?.cancel()
        sessionJob = null
    }

    fun close() {
        stopSession()
        scope.cancel()
    }
}
