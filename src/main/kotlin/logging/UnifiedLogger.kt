package com.tbread.logging

import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unified Logger - Consolidates DebugLogWriter, PacketLogger, and SkillAnalysisLogger
 *
 * Features:
 * - Atomic boolean flags for thread-safe enable/disable
 * - Single background thread for all logging I/O
 * - ConcurrentLinkedQueue for thread-safe log entry queuing
 * - Proper shutdown with log flushing
 *
 * All three logging subsystems now delegate to this unified implementation.
 */
object UnifiedLogger {
    private val logger = LoggerFactory.getLogger(UnifiedLogger::class.java)

    // Atomic flags for each log type
    private val debugEnabled = AtomicBoolean(false)
    private val packetEnabled = AtomicBoolean(false)
    private val skillAnalysisEnabled = AtomicBoolean(false)

    // Thread-safe queue for log entries
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()

    // Single background thread for all I/O operations
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "UnifiedLogger-Writer") }

    // Writers for each log type (lazy initialized)
    private var debugWriter: FileWriter? = null
    private var packetWriter: PrintWriter? = null
    private var skillAnalysisWriter: PrintWriter? = null
    private var currentSkillAnalysisId: Int? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val fileFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    init {
        // Start background writer thread
        executor.submit { processLogs() }
    }

    sealed class LogEntry(val timestamp: LocalDateTime) {
        class Debug(timestamp: LocalDateTime, val message: String, val throwable: Throwable?) : LogEntry(timestamp)
        class Packet(timestamp: LocalDateTime, val message: String) : LogEntry(timestamp)
        class SkillAnalysis(timestamp: LocalDateTime, val message: String) : LogEntry(timestamp)
    }

    // ============================================================
    // DEBUG LOGGING
    // ============================================================

    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled.set(enabled)
        if (enabled) {
            logQueue.offer(LogEntry.Debug(LocalDateTime.now(), "Debug logging enabled", null))
        }
    }

    fun isDebugEnabled(): Boolean = debugEnabled.get()

    fun debug(loggerName: String, level: String, message: String, throwable: Throwable? = null) {
        if (!debugEnabled.get()) return
        logQueue.offer(LogEntry.Debug(LocalDateTime.now(), "$level $loggerName - $message", throwable))
    }

    // ============================================================
    // PACKET LOGGING
    // ============================================================

    fun setPacketEnabled(enabled: Boolean) {
        if (enabled && !packetEnabled.get()) {
            packetEnabled.set(true)
            logQueue.offer(LogEntry.Packet(LocalDateTime.now(), createPacketHeader()))
        } else if (!enabled && packetEnabled.get()) {
            packetEnabled.set(false)
            logQueue.offer(LogEntry.Packet(LocalDateTime.now(), "=== Packet logging disabled ==="))
        }
    }

    fun isPacketEnabled(): Boolean = packetEnabled.get()

    fun logPacket(message: String) {
        if (!packetEnabled.get()) return
        logQueue.offer(LogEntry.Packet(LocalDateTime.now(), message))
    }

    private fun createPacketHeader(): String {
        return """
===========================================
PACKET LOGGER - Detailed Packet Analysis
Started: ${LocalDateTime.now().format(dateFormatter)}
===========================================

Packet Fields:
- Timestamp: When packet was received
- Actor ID: The entity dealing damage (player/pet/summon)
- Target ID: The entity receiving damage (monster/player)
- Skill ID: The skill code used
- Skill Name: Skill name from database
- Damage: Amount of damage dealt
- Type: Damage type (1=normal, 3=critical)
- Flag: Damage flags
- Specials: Special damage types (BACK, PARRY, PERFECT, DOUBLE, etc.)
- DoT: Is damage over time
- Unknown: Unknown field value
- Switch: Switch variable value
- Loop: Loop counter

===========================================
""".trimIndent()
    }

    // ============================================================
    // SKILL ANALYSIS LOGGING
    // ============================================================

    fun setSkillAnalysisEnabled(enabled: Boolean, skillId: Int = 13350000) {
        if (enabled) {
            // Re-initialize if skill ID changed
            if (skillAnalysisEnabled.get() && currentSkillAnalysisId != skillId) {
                skillAnalysisEnabled.set(false)
                skillAnalysisWriter?.close()
                skillAnalysisWriter = null
            }

            if (!skillAnalysisEnabled.get()) {
                currentSkillAnalysisId = skillId
                skillAnalysisEnabled.set(true)
                logQueue.offer(LogEntry.SkillAnalysis(LocalDateTime.now(), createSkillAnalysisHeader(skillId)))
            }
        } else if (!enabled && skillAnalysisEnabled.get()) {
            skillAnalysisEnabled.set(false)
            currentSkillAnalysisId = null
            logQueue.offer(LogEntry.SkillAnalysis(LocalDateTime.now(), "=== Skill analysis logging disabled ==="))
        }
    }

    fun isSkillAnalysisEnabled(): Boolean = skillAnalysisEnabled.get()

    fun getSkillAnalysisTargetId(): Int = currentSkillAnalysisId ?: 0

    fun logSkillAnalysis(message: String) {
        if (!skillAnalysisEnabled.get()) return
        logQueue.offer(LogEntry.SkillAnalysis(LocalDateTime.now(), message))
    }

    private fun createSkillAnalysisHeader(skillId: Int): String {
        return """
===========================================
SKILL ANALYSIS LOGGER
Started: ${LocalDateTime.now().format(dateFormatter)}
Target Skill ID: $skillId
===========================================

This log captures ALL packets containing skill ID $skillId.
Each entry shows:
  1. Raw packet hex data
  2. Parsed packet fields
  3. All unknown/mysterious fields that might contain specialty info

Look for patterns in:
  - Flag field (VarInt)
  - Switch Variable (VarInt) - especially upper bits
  - Unknown field (VarInt)
  - Loop field (VarInt)
  - Special Flags bytes

===========================================
""".trimIndent()
    }

    // ============================================================
    // BACKGROUND PROCESSING
    // ============================================================

    private fun processLogs() {
        val debugFile = File("debug.log")
        val packetLogsDir = File("logs/packets")
        val skillLogsDir = File("logs/skill_analysis")

        while (!Thread.currentThread().isInterrupted) {
            try {
                val entry = logQueue.poll()
                if (entry != null) {
                    when (entry) {
                        is LogEntry.Debug -> {
                            ensureDebugWriter(debugFile)
                            debugWriter?.let { writer ->
                                val timestamp = entry.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
                                writer.append("$timestamp ${entry.message}\n")
                                entry.throwable?.let { throwable ->
                                    writer.append(throwable.stackTraceToString()).append('\n')
                                }
                                writer.flush()
                            }
                        }
                        is LogEntry.Packet -> {
                            ensurePacketWriter(packetLogsDir)
                            packetWriter?.let { writer ->
                                writer.println(entry.message)
                                writer.flush()
                            }
                        }
                        is LogEntry.SkillAnalysis -> {
                            ensureSkillAnalysisWriter(skillLogsDir)
                            skillAnalysisWriter?.let { writer ->
                                writer.println(entry.message)
                                writer.flush()
                            }
                        }
                    }
                } else {
                    Thread.sleep(10)
                }
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                logger.error("Error writing log entry", e)
            }
        }
    }

    private fun ensureDebugWriter(file: File) {
        if (debugWriter == null && debugEnabled.get()) {
            file.parentFile?.mkdirs()
            debugWriter = FileWriter(file, true)
        }
    }

    private fun ensurePacketWriter(dir: File) {
        if (packetWriter == null && packetEnabled.get()) {
            dir.mkdirs()
            val timestamp = LocalDateTime.now().format(fileFormatter)
            val file = File(dir, "packets_$timestamp.log")
            packetWriter = PrintWriter(FileWriter(file, true), true)
        }
    }

    private fun ensureSkillAnalysisWriter(dir: File) {
        if (skillAnalysisWriter == null && skillAnalysisEnabled.get()) {
            dir.mkdirs()
            val timestamp = LocalDateTime.now().format(fileFormatter)
            val skillId = currentSkillAnalysisId ?: 0
            val file = File(dir, "skill_${skillId}_${timestamp}.log")
            skillAnalysisWriter = PrintWriter(FileWriter(file, true), true)
        }
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================

    fun shutdown() {
        try {
            logger.info("Shutting down UnifiedLogger...")
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // Close all writers
            debugWriter?.close()
            packetWriter?.close()
            skillAnalysisWriter?.close()

            logger.info("UnifiedLogger shutdown complete")
        } catch (e: Exception) {
            logger.error("Error shutting down UnifiedLogger", e)
        }
    }
}
