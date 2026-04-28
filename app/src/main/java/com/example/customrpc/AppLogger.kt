package com.example.customrpc

import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val timestamp: String,
    val level: String,
    val message: String
)

object AppLogger {
    private const val MAX_ENTRIES = 500
    private val entries = ArrayDeque<LogEntry>()
    private val listeners = mutableListOf<(LogEntry) -> Unit>()
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun log(level: String, message: String) {
        val entry = LogEntry(
            timestamp = formatter.format(Date()),
            level = level,
            message = message
        )
        entries.addLast(entry)
        if (entries.size > MAX_ENTRIES) entries.removeFirst()
        val snapshot = listeners.toList()
        snapshot.forEach { it(entry) }
    }

    fun info(msg: String) = log("INFO", msg)
    fun warn(msg: String) = log("WARN", msg)
    fun error(msg: String) = log("ERROR", msg)

    @Synchronized
    fun getAll(): List<LogEntry> = entries.toList()

    @Synchronized
    fun clear() = entries.clear()

    fun addListener(listener: (LogEntry) -> Unit) {
        synchronized(listeners) { listeners.add(listener) }
    }

    fun removeListener(listener: (LogEntry) -> Unit) {
        synchronized(listeners) { listeners.remove(listener) }
    }
}
