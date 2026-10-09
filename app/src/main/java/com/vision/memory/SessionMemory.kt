package com.vision.memory

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class SessionMemory {
    private val messages = mutableListOf<SessionMemoryEntry>()
    private val mutex = Mutex()
    private var currentContext = ""
    private var sessionStartTime = System.currentTimeMillis()
    private val maxMessages = 50

    suspend fun addMessage(sender: String, text: String, context: String = "") {
        mutex.withLock {
            messages.add(
                SessionMemoryEntry(
                    sender = sender,
                    text = text,
                    context = context.ifEmpty { currentContext }
                )
            )
            if (messages.size > maxMessages) messages.removeAt(0)
            if (context.isNotEmpty()) currentContext = context
        }
    }

    suspend fun getLastMessages(count: Int): List<SessionMemoryEntry> = mutex.withLock {
        val startIndex = maxOf(0, messages.size - count.coerceAtLeast(0))
        messages.subList(startIndex, messages.size).toList()
    }

    suspend fun getCurrentContext(): String = mutex.withLock { currentContext }

    suspend fun setContext(context: String) {
        mutex.withLock { currentContext = context }
    }

    suspend fun getSessionSummary(): String = mutex.withLock {
        if (messages.isEmpty()) return@withLock "No messages in session"
        buildString {
            append("Session Summary (last 10 messages):\n")
            messages.takeLast(10).forEach { append("${it.sender}: ${it.text}\n") }
            append("\nCurrent Context: $currentContext")
        }
    }

    suspend fun clear() {
        mutex.withLock {
            messages.clear()
            currentContext = ""
            sessionStartTime = System.currentTimeMillis()
        }
    }

    suspend fun getAllMessages(): List<SessionMemoryEntry> = mutex.withLock { messages.toList() }

    suspend fun getSessionDuration(): Long = System.currentTimeMillis() - sessionStartTime
}