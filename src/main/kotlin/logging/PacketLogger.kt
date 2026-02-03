package com.tbread.logging

import com.tbread.entity.ParsedDamagePacket
import com.tbread.DpsCalculator
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Packet Logger - Now delegates to UnifiedLogger
 * Preserves backward-compatible API while using unified logging infrastructure
 */
object PacketLogger {
    private val logger = LoggerFactory.getLogger(PacketLogger::class.java)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    /**
     * Enable packet logging
     * Creates a new log file with timestamp
     */
    fun enable() {
        if (isEnabled()) return
        UnifiedLogger.setPacketEnabled(true)
        logger.info("Packet logging enabled")
    }

    /**
     * Disable packet logging
     */
    fun disable() {
        if (!isEnabled()) return
        UnifiedLogger.setPacketEnabled(false)
        logger.info("Packet logging disabled")
    }

    /**
     * Check if logging is enabled
     */
    fun isEnabled(): Boolean = UnifiedLogger.isPacketEnabled()

    /**
     * Log a single packet with all details
     */
    fun logPacket(packet: ParsedDamagePacket, nickname: String? = null) {
        if (!isEnabled()) return

        try {
            val timestamp = dateFormat.format(Date(packet.getTimeStamp()))
            val skillName = DpsCalculator.SKILL_MAP[packet.getSkillCode1()] ?: "Unknown Skill"

            // Format specials list
            val specials = packet.getSpecials().joinToString(", ") { it.name }
            val specialsStr = if (specials.isEmpty()) "None" else specials

            // Determine damage type
            val damageType = when {
                packet.isCrit() -> "Critical"
                packet.isDoT() -> "DoT"
                else -> "Normal"
            }

            val actorName = nickname ?: "Player ${packet.getActorId()}"

            // Additional analysis
            val switchMask = packet.getSwitchVariable() and 0x0f
            val flagBinary = packet.getFlag().toString(2).padStart(8, '0')

            val message = """
[$timestamp] DAMAGE PACKET
  Actor: $actorName (ID: ${packet.getActorId()})
  Target ID: ${packet.getTargetId()}
  Skill: $skillName (ID: ${packet.getSkillCode1()})
  Damage: ${packet.getDamage()}
  Type: $damageType (${packet.getType()})
  Flag: 0x${packet.getFlag().toString(16).uppercase().padStart(4, '0')} (binary: $flagBinary)
  Switch Variable: ${packet.getSwitchVariable()} (masked: $switchMask)
  Specials: $specialsStr
  DoT: ${packet.isDoT()}
  Unknown: ${packet.getUnknown()}
  Loop: ${packet.getLoop()}
  UUID: ${packet.getUuid()}

  📊 Analysis:
    • Opcode: ${if (packet.isDoT()) "0x05 0x38 (DoT)" else "0x04 0x38 (Direct Damage)"}
    • Switch Mask: $switchMask → ${when(switchMask) {
        4 -> "8 bytes special flags"
        5 -> "12 bytes special flags"
        6 -> "10 bytes special flags"
        7 -> "14 bytes special flags"
        else -> "Unknown format"
    }}
    • Crit: ${packet.isCrit()}
    • Has Specials: ${packet.getSpecials().isNotEmpty()}
---
            """.trimIndent()

            UnifiedLogger.logPacket(message)
        } catch (e: Exception) {
            logger.error("Error logging packet", e)
        }
    }

    /**
     * Log a summary of combat
     */
    fun logCombatSummary(
        targetName: String,
        battleTime: Long,
        totalDamage: Double,
        playerCount: Int
    ) {
        if (!isEnabled()) return

        val timestamp = dateFormat.format(Date())
        val summary = """

===========================================
[$timestamp] COMBAT SUMMARY
===========================================
Target: $targetName
Battle Time: ${battleTime}ms (${battleTime / 1000.0}s)
Total Damage: ${totalDamage.toLong()}
Players: $playerCount
Average DPS: ${(totalDamage / (battleTime / 1000.0)).toLong()}
===========================================

        """.trimIndent()

        UnifiedLogger.logPacket(summary)
    }

    /**
     * Log raw packet hex data (for deep debugging)
     */
    fun logRawPacket(data: ByteArray, description: String = "") {
        if (!isEnabled()) return

        val timestamp = dateFormat.format(Date())
        val hex = data.joinToString(" ") {
            "%02X".format(it)
        }

        val message = """
[$timestamp] RAW PACKET${if (description.isNotEmpty()) " - $description" else ""}
  Length: ${data.size} bytes
  Hex: $hex
---
        """.trimIndent()

        UnifiedLogger.logPacket(message)
    }

    /**
     * Shutdown the logger gracefully
     * Now no-op since UnifiedLogger handles shutdown
     */
    fun shutdown() {
        // Delegated to UnifiedLogger.shutdown()
    }
}
