package com.tbread.entity

import com.tbread.logging.PacketLogger
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PersonalData(
    @Required var job: String = "",
    var dps: Double = 0.0,
    var burstDps: Double = 0.0, // Peak 1-second DPS
    var amount: Double = 0.0,
    @Required var damageContribution: Double = 0.0,
    var totalCritPct: Double = 0.0, // Overall crit rate percentage
    var totalBackPct: Double = 0.0, // Overall back attack rate percentage
    @Transient val analyzedData: MutableMap<Int, AnalyzedSkill> = mutableMapOf(),
    @Transient val recentDamage: MutableList<Pair<Long, Int>> = mutableListOf(), // <timestamp, damage>
    val nickname: String
) {
    companion object {
        const val DPS_WINDOW_MS = 5000L // 5 second rolling window
    }

    private fun addDamage(damage: Double) {
        amount += damage
    }

    fun processPdp(pdp: ParsedDamagePacket) {
        addDamage(pdp.getDamage().toDouble())

        // Log packet details if logging is enabled
        PacketLogger.logPacket(pdp, nickname)

        // Track recent damage for rolling DPS
        synchronized(recentDamage) {
            recentDamage.add(Pair(pdp.getTimeStamp(), pdp.getDamage()))
        }

        if (!analyzedData.containsKey(pdp.getSkillCode1())) {
            val analyzedSkill = AnalyzedSkill(pdp)
            analyzedData[pdp.getSkillCode1()] = analyzedSkill
        }
        val analyzedSkill = analyzedData[pdp.getSkillCode1()]!!
        if (pdp.isDoT()) {
            analyzedSkill.dotTimes ++
            analyzedSkill.dotDamageAmount += pdp.getDamage()
        } else {
            analyzedSkill.times++
            analyzedSkill.damageAmount += pdp.getDamage()
            if (pdp.isCrit()) analyzedSkill.critTimes++
            if (pdp.getSpecials().contains(SpecialDamage.BACK)) analyzedSkill.backTimes++
            if (pdp.getSpecials().contains(SpecialDamage.PARRY)) analyzedSkill.parryTimes++
            if (pdp.getSpecials().contains(SpecialDamage.DOUBLE)) analyzedSkill.doubleTimes++
            if (pdp.getSpecials().contains(SpecialDamage.PERFECT)) analyzedSkill.perfectTimes++
        }
    }

    /**
     * Calculate DPS using rolling window (Hybrid Window approach)
     * Returns current DPS based on recent damage
     * Uses elapsed time for first 5s (responsive), then fixed 5s window (accurate)
     */
    fun calculateRollingDPS(currentTime: Long = System.currentTimeMillis()): Double {
        synchronized(recentDamage) {
            // Remove old damage outside the window
            val cutoffTime = currentTime - DPS_WINDOW_MS
            val iterator = recentDamage.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.first < cutoffTime) {
                    iterator.remove()
                }
            }

            if (recentDamage.isEmpty()) return 0.0

            // Calculate total damage in window
            val totalDamage = recentDamage.sumOf { it.second.toDouble() }

            // Hybrid Window: Use max(elapsed, windowSize) for best UX
            // - Early combat (< 5s): Responsive, shows real DPS immediately
            // - Late combat (≥ 5s): Accurate, prevents burst inflation
            val oldestTime = recentDamage.minOf { it.first }
            val elapsedSeconds = (currentTime - oldestTime) / 1000.0
            val windowSizeSeconds = DPS_WINDOW_MS / 1000.0

            val effectiveWindow = maxOf(elapsedSeconds, windowSizeSeconds)

            return totalDamage / effectiveWindow
        }
    }

    /**
     * Calculate burst DPS (peak 1-second damage)
     * Returns highest DPS in any 1-second window
     */
    fun calculateBurstDPS(currentTime: Long = System.currentTimeMillis()): Double {
        synchronized(recentDamage) {
            if (recentDamage.isEmpty()) return 0.0

            // Use 1-second window for burst detection
            val burstWindowMs = 1000L
            val cutoffTime = currentTime - DPS_WINDOW_MS // Still look within 5s window

            // Get all damage within the 5s window
            val windowDamage = recentDamage.filter { it.first >= cutoffTime }
            if (windowDamage.isEmpty()) return 0.0

            // Find the 1-second period with highest damage
            var maxBurstDps = 0.0

            // Check each possible 1-second window within the 5-second range
            val startTime = windowDamage.minOf { it.first }
            val endTime = windowDamage.maxOf { it.first }
            val totalDuration = endTime - startTime

            if (totalDuration < burstWindowMs) {
                // If all damage is within 1 second, just sum it
                val totalDamage = windowDamage.sumOf { it.second.toDouble() }
                return totalDamage // Already per second for burst window
            }

            // Slide a 1-second window across the data
            var windowStart = startTime
            while (windowStart <= endTime) {
                val windowEnd = windowStart + burstWindowMs
                val burstDamage = windowDamage
                    .filter { it.first >= windowStart && it.first < windowEnd }
                    .sumOf { it.second.toDouble() }

                maxBurstDps = maxOf(maxBurstDps, burstDamage)
                windowStart += 100 // Slide by 100ms for precision
            }

            return maxBurstDps
        }
    }

    /**
     * Calculate overall crit rate from all skills
     * Returns percentage of critical hits
     */
    fun calculateCritRate(): Double {
        var totalCrits = 0
        var totalHits = 0

        analyzedData.values.forEach { skill ->
            totalCrits += skill.critTimes
            totalHits += skill.times
        }

        return if (totalHits > 0) {
            (totalCrits.toDouble() / totalHits) * 100
        } else {
            0.0
        }
    }

    /**
     * Calculate overall back attack rate from all skills
     * Returns percentage of back attacks
     */
    fun calculateBackRate(): Double {
        var totalBacks = 0
        var totalHits = 0

        analyzedData.values.forEach { skill ->
            totalBacks += skill.backTimes
            totalHits += skill.times
        }

        return if (totalHits > 0) {
            (totalBacks.toDouble() / totalHits) * 100
        } else {
            0.0
        }
    }
}
