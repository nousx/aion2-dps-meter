package com.tbread.logging

import com.tbread.entity.ParsedDamagePacket
import com.tbread.DpsCalculator
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Skill Analysis Logger - Now delegates to UnifiedLogger
 * Preserves backward-compatible API while using unified logging infrastructure
 */
object SkillAnalysisLogger {
    private val logger = LoggerFactory.getLogger(SkillAnalysisLogger::class.java)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    /**
     * Enable skill analysis logging for a specific skill ID
     */
    fun enable(skillId: Int = 13350000) {
        if (isEnabled() && getTargetSkillId() == skillId) return
        UnifiedLogger.setSkillAnalysisEnabled(true, skillId)
        logger.info("Skill analysis logging enabled for skill ID {}", skillId)
    }

    /**
     * Disable skill analysis logging
     */
    fun disable() {
        if (!isEnabled()) return
        UnifiedLogger.setSkillAnalysisEnabled(false)
        logger.info("Skill analysis logging disabled")
    }

    /**
     * Check if logging is enabled
     */
    fun isEnabled(): Boolean = UnifiedLogger.isSkillAnalysisEnabled()

    /**
     * Get current target skill ID
     */
    fun getTargetSkillId(): Int = UnifiedLogger.getSkillAnalysisTargetId()

    /**
     * Infer base skill code from raw skill code
     * Same logic as DpsCalculator.inferOriginalSkillCode()
     */
    private fun inferSkillCode(skillCode: Int): Int {
        val possibleOffsets = intArrayOf(
            0, 10, 20, 30, 40, 50,
            120, 130, 140, 150,
            230, 240, 250,
            340, 350,
            450,
            1230, 1240, 1250,
            1340, 1350,
            1450,
            2340, 2350,
            2450,
            3450
        )

        for (offset in possibleOffsets) {
            val possibleOrigin = skillCode - offset
            if (DpsCalculator.SKILL_MAP.containsKey(possibleOrigin)) {
                return possibleOrigin
            }
        }
        return skillCode // No inference found, return original
    }

    /**
     * Log a damage packet containing the target skill ID
     * This includes BOTH raw hex and parsed data
     */
    fun logPacket(
        rawPacket: ByteArray,
        pdp: ParsedDamagePacket,
        nickname: String? = null
    ) {
        if (!isEnabled()) return

        // Check if this packet matches target skill ID
        // Need to check BOTH raw skill code AND inferred base skill code
        val targetSkillId = getTargetSkillId()
        val rawSkillCode = pdp.getSkillCode1()
        val inferredSkillCode = inferSkillCode(rawSkillCode)

        val matches = rawSkillCode == targetSkillId || inferredSkillCode == targetSkillId
        if (!matches) return

        try {
            val timestamp = dateFormat.format(Date(pdp.getTimeStamp()))
            val skillName = DpsCalculator.SKILL_MAP[pdp.getSkillCode1()] ?: "Unknown Skill"
            val actorName = nickname ?: "Actor ${pdp.getActorId()}"

            // Format raw hex
            val rawHex = rawPacket.joinToString(" ") { "%02X".format(it) }

            // Analyze switch variable
            val switchVar = pdp.getSwitchVariable()
            val switchMask = switchVar and 0x0f
            val switchUpperBits = switchVar shr 4
            val specialFlagSize = when (switchMask) {
                4 -> 8
                5 -> 12
                6 -> 10
                7 -> 14
                else -> 0
            }

            // Format flag in multiple bases
            val flag = pdp.getFlag()
            val flagHex = "0x${flag.toString(16).uppercase().padStart(4, '0')}"
            val flagBinary = flag.toString(2).padStart(16, '0')
            val flagDecimal = flag

            // Format specials
            val specials = pdp.getSpecials().joinToString(", ") { it.name }
            val specialsStr = if (specials.isEmpty()) "None" else specials

            // Damage type
            val damageType = when {
                pdp.isCrit() -> "Critical"
                pdp.isDoT() -> "DoT"
                else -> "Normal"
            }

            val message = """
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[$timestamp] SKILL PACKET CAPTURED
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📦 RAW PACKET DATA (${rawPacket.size} bytes):
$rawHex

───────────────────────────────────────────────────────────

🎯 PARSED PACKET FIELDS:

Basic Info:
  • Actor: $actorName (ID: ${pdp.getActorId()})
  • Target ID: ${pdp.getTargetId()}
  • Skill: $skillName (ID: ${pdp.getSkillCode1()})
  • Damage: ${pdp.getDamage()}
  • Type: $damageType (${pdp.getType()})
  • DoT: ${pdp.isDoT()}

🚩 FLAG Field (ต้องสังเกต!):
  • Hex: $flagHex
  • Binary: $flagBinary
  • Decimal: $flagDecimal

🔀 SWITCH VARIABLE (อาจบอก specialty!):
  • Full Value: $switchVar
  • Lower 4 bits (mask): $switchMask → $specialFlagSize bytes special flags
  • Upper bits: $switchUpperBits (0x${switchUpperBits.toString(16).uppercase()})

⭐ SPECIAL FLAGS:
  • Values: $specialsStr

❓ UNKNOWN Field:
  • Value: ${pdp.getUnknown()}

🔁 LOOP Field:
  • Value: ${pdp.getLoop()}

📊 PACKET UUID:
  • ${pdp.getUuid()}

───────────────────────────────────────────────────────────

💡 ANALYSIS HINTS:
  1. Compare FLAG values when using different specialties
  2. Check if SWITCH upper bits change with specialty
  3. Look at UNKNOWN field - might encode specialty index
  4. Check if LOOP has meaning for specialty

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            """.trimIndent()

            UnifiedLogger.logSkillAnalysis(message)
        } catch (e: Exception) {
            logger.error("Error logging skill analysis packet", e)
        }
    }

    /**
     * Log raw packet data (for packets we can't parse yet)
     */
    fun logRawPacket(packet: ByteArray, description: String = "") {
        if (!isEnabled()) return

        val targetSkillId = getTargetSkillId()

        // Check if packet contains target skill ID
        // Search for skill ID as 4 bytes (little endian)
        val skillIdBytes = byteArrayOf(
            (targetSkillId and 0xFF).toByte(),
            ((targetSkillId shr 8) and 0xFF).toByte(),
            ((targetSkillId shr 16) and 0xFF).toByte(),
            ((targetSkillId shr 24) and 0xFF).toByte()
        )

        // Simple search for skill ID in packet
        var found = false
        for (i in 0..(packet.size - 4)) {
            if (packet[i] == skillIdBytes[0] &&
                packet[i + 1] == skillIdBytes[1] &&
                packet[i + 2] == skillIdBytes[2] &&
                packet[i + 3] == skillIdBytes[3]
            ) {
                found = true
                break
            }
        }

        if (!found) return

        val timestamp = dateFormat.format(Date())
        val hex = packet.joinToString(" ") { "%02X".format(it) }

        val message = """
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[$timestamp] RAW PACKET${if (description.isNotEmpty()) " - $description" else ""}
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📦 Skill ID $targetSkillId found in packet!
Length: ${packet.size} bytes
Hex: $hex

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        """.trimIndent()

        UnifiedLogger.logSkillAnalysis(message)
    }

    /**
     * Shutdown the logger gracefully
     * Now no-op since UnifiedLogger handles shutdown
     */
    fun shutdown() {
        // Delegated to UnifiedLogger.shutdown()
    }
}
