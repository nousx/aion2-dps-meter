package com.tbread.logging

import com.tbread.packet.PropertyHandler
import org.slf4j.Logger
import org.slf4j.helpers.MessageFormatter

/**
 * DebugLogWriter - Now delegates to UnifiedLogger
 * Preserves backward-compatible API while using unified logging infrastructure
 */
object DebugLogWriter {
    const val SETTING_KEY = "dpsMeter.debugLoggingEnabled"
    private const val MAX_MESSAGE_LENGTH = 240

    fun loadFromSettings() {
        setEnabled(false)
        PropertyHandler.setProperty(SETTING_KEY, false.toString())
    }

    fun setEnabled(enabled: Boolean) {
        UnifiedLogger.setDebugEnabled(enabled)
    }

    fun isEnabled(): Boolean = UnifiedLogger.isDebugEnabled()

    fun debug(logger: Logger, message: String, vararg args: Any?) {
        write("DEBUG", logger.name, message, args)
    }

    fun info(logger: Logger, message: String, vararg args: Any?) {
        write("INFO", logger.name, message, args)
    }

    private fun write(level: String, loggerName: String, message: String, args: Array<out Any?>) {
        if (!isEnabled()) return
        val result = MessageFormatter.arrayFormat(message, args)
        val formattedMessage = truncate(result.message ?: "")
        val shortLoggerName = loggerName.substringAfterLast('.')
        UnifiedLogger.debug(shortLoggerName, level, formattedMessage, result.throwable)
    }

    private fun truncate(message: String): String {
        if (message.length <= MAX_MESSAGE_LENGTH) return message
        return message.take(MAX_MESSAGE_LENGTH - 1) + "…"
    }
}
