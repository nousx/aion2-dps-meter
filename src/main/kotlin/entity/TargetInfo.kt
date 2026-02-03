package com.tbread.entity

import java.util.UUID

data class TargetInfo(
    private val targetId: Int,
    private var damagedAmount: Int = 0,
    private var targetDamageStarted: Long,
    private var targetDamageEnded: Long,
    private val processedUuid: MutableSet<UUID> = mutableSetOf(),
    private val recentPackets: MutableList<Pair<Long, Int>> = mutableListOf(), // <timestamp, damage>
) {
    companion object {
        const val DPS_WINDOW_MS = 5000L // 5 second rolling window
    }
    fun processedUuid(): MutableSet<UUID> {
        return processedUuid
    }

    fun damagedAmount(): Int {
        return damagedAmount
    }

    fun targetId(): Int {
        return targetId
    }

    fun firstDamageTime(): Long {
        return targetDamageStarted
    }

    fun lastDamageTime(): Long {
        return targetDamageEnded
    }

    fun processPdp(pdp:ParsedDamagePacket){
        if (processedUuid.contains(pdp.getUuid())) return
        damagedAmount += pdp.getDamage()
        val ts = pdp.getTimeStamp()

        // Store recent packet for rolling DPS calculation
        recentPackets.add(Pair(ts, pdp.getDamage()))

        if (ts < targetDamageStarted){
            targetDamageStarted = ts
        } else if (ts > targetDamageEnded){
            targetDamageEnded = ts
        }
        processedUuid.add(pdp.getUuid())
    }

    /**
     * Calculate DPS using rolling window (Hybrid Window approach)
     * Returns DPS based on recent damage within the window
     * Uses elapsed time for first 5s (responsive), then fixed 5s window (accurate)
     */
    fun calculateRollingDPS(currentTime: Long = System.currentTimeMillis()): Double {
        // Remove old packets outside the window
        val cutoffTime = currentTime - DPS_WINDOW_MS
        recentPackets.removeAll { it.first < cutoffTime }

        if (recentPackets.isEmpty()) return 0.0

        // Calculate total damage in window
        val totalDamage = recentPackets.sumOf { it.second.toDouble() }

        // Hybrid Window: Use max(elapsed, windowSize) for best UX
        // - Early combat (< 5s): Responsive, shows real DPS immediately
        // - Late combat (≥ 5s): Accurate, prevents burst inflation
        val oldestTime = recentPackets.minOf { it.first }
        val elapsedSeconds = (currentTime - oldestTime) / 1000.0
        val windowSizeSeconds = DPS_WINDOW_MS / 1000.0

        val effectiveWindow = maxOf(elapsedSeconds, windowSizeSeconds)

        return totalDamage / effectiveWindow
    }

    fun parseBattleTime():Long{
        return targetDamageEnded - targetDamageStarted
    }
}
