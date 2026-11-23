package com.example.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

data class DevLogEntry(
    val id: Long,
    val level: String, // "DEBUG", "INFO", "WARN", "ERROR"
    val tag: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Centralized Developer Logging utility wrapping android.util.Log and maintaining
 * an observable thread-safe ring buffer for the in-app Developer Logs Console.
 */
object DevLogger {
    private const val MAX_BUFFER_SIZE = 300
    private val nextId = AtomicLong(1L)
    private val _logs = MutableStateFlow<List<DevLogEntry>>(emptyList())
    val logs: StateFlow<List<DevLogEntry>> = _logs.asStateFlow()

    @Volatile
    private var persistenceListener: ((level: String, tag: String, message: String, timestamp: Long) -> Unit)? = null

    fun attachPersistenceListener(
        listener: ((level: String, tag: String, message: String, timestamp: Long) -> Unit)?
    ) {
        persistenceListener = listener
    }

    fun d(tag: String, message: String) = log("DEBUG", tag, message)

    fun i(tag: String, message: String) = log("INFO", tag, message)

    fun w(tag: String, message: String) = log("WARN", tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) {
            "$message | ${throwable::class.java.simpleName}: ${throwable.message}"
        } else {
            message
        }
        log("ERROR", tag, fullMsg, throwable)
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        try {
            when (level) {
                "DEBUG" -> Log.d(tag, message)
                "INFO" -> Log.i(tag, message)
                "WARN" -> Log.w(tag, message)
                "ERROR" -> Log.e(tag, message, throwable)
                else -> Log.d(tag, message)
            }
        } catch (_: Throwable) {
            // Safe fallback in pure JVM unit tests
        }

        val now = System.currentTimeMillis()
        val entry = DevLogEntry(
            id = nextId.getAndIncrement(),
            level = level,
            tag = tag,
            message = message,
            timestamp = now
        )
        synchronized(this) {
            val updated = (listOf(entry) + _logs.value).take(MAX_BUFFER_SIZE)
            _logs.value = updated
        }
        try {
            persistenceListener?.invoke(level, tag, message, now)
        } catch (_: Throwable) {
        }
    }

    fun clear() {
        synchronized(this) {
            _logs.value = emptyList()
        }
    }

    fun exportLogsAsText(entries: List<DevLogEntry> = _logs.value): String {
        if (entries.isEmpty()) return "No developer logs recorded."
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return entries.joinToString("\n") { entry ->
            "[${fmt.format(Date(entry.timestamp))}] ${entry.level}/${entry.tag}: ${entry.message}"
        }
    }
}
