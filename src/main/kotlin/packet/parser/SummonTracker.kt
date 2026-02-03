package com.tbread.packet.parser

import com.tbread.DataStorage
import com.tbread.logging.DebugLogWriter
import com.tbread.packet.StreamProcessor
import org.slf4j.LoggerFactory

/**
 * Handles summon packet parsing and mob mapping
 */
class SummonTracker(private val dataStorage: DataStorage) {
    private val logger = LoggerFactory.getLogger(SummonTracker::class.java)

    /**
     * Parses summon packets (opcode 0x40 0x36) to map summoned entities to their summoner
     *
     * Packet structure:
     * - Length header (varint)
     * - Opcode: 0x40 0x36
     * - Summon entity ID (varint)
     * - +28 bytes offset
     * - Mob code (varint) - appears twice for validation
     * - FF marker (8x 0xFF bytes)
     * - Secondary opcode: 0x07 0x02 0x06
     * - +11 bytes offset
     * - Real actor ID (uint16le)
     *
     * @param packet The packet to parse
     * @return true if a valid summon packet was parsed, false otherwise
     */
    fun parseSummonPacket(packet: ByteArray): Boolean {
        var offset = 0
        val packetLengthInfo = readVarInt(packet)
        if (packetLengthInfo.length < 0) return false
        offset += packetLengthInfo.length

        // Check opcode: 0x40 0x36
        if (packet[offset] != 0x40.toByte()) return false
        if (packet[offset + 1] != 0x36.toByte()) return false
        offset += 2

        // Read summon entity ID
        val summonInfo = readVarInt(packet, offset)
        if (summonInfo.length < 0) return false
        offset += summonInfo.length + 28
        if (packet.size <= offset) return false

        // Read mob code (appears twice for validation)
        val mobInfo = readVarInt(packet, offset)
        if (mobInfo.length < 0) return false
        offset += mobInfo.length
        if (packet.size <= offset) return false

        val mobInfo2 = readVarInt(packet, offset)
        if (mobInfo2.length < 0) return false

        // Both mob codes must match
        if (mobInfo.value == mobInfo2.value) {
            logger.debug("mid: {}, code: {}", summonInfo.value, mobInfo.value)
            DebugLogWriter.debug(logger, "mid: {}, code: {}", summonInfo.value, mobInfo.value)
            dataStorage.appendMob(summonInfo.value, mobInfo.value)
        }

        // Find FF marker (8x 0xFF bytes)
        val keyIdx = findArrayIndex(packet, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff)
        if (keyIdx == -1) return false
        val afterPacket = packet.copyOfRange(keyIdx + 8, packet.size)

        // Find secondary opcode: 0x07 0x02 0x06
        val opcodeIdx = findArrayIndex(afterPacket, 0x07, 0x02, 0x06)
        if (opcodeIdx == -1) return false
        offset = keyIdx + opcodeIdx + 11

        // Read real actor ID
        if (offset + 2 > packet.size) return false
        val realActorId = parseUInt16le(packet, offset)

        logger.debug("Summon mob mapping succeeded {},{}", realActorId, summonInfo.value)
        DebugLogWriter.debug(logger, "Summon mob mapping succeeded {},{}", realActorId, summonInfo.value)
        dataStorage.appendSummon(realActorId, summonInfo.value)
        return true
    }

    private fun readVarInt(bytes: ByteArray, offset: Int = 0): StreamProcessor.VarIntOutput {
        if (offset < 0 || offset >= bytes.size) {
            return StreamProcessor.VarIntOutput(-1, -1)
        }

        var value = 0
        var shift = 0
        var count = 0

        while (true) {
            if (offset + count >= bytes.size) {
                logger.trace("Array out of bounds, offset {} count {}", offset, count)
                return StreamProcessor.VarIntOutput(-1, -1)
            }

            val byteVal = bytes[offset + count].toInt() and 0xff
            count++

            value = value or (byteVal and 0x7F shl shift)

            if ((byteVal and 0x80) == 0) {
                return StreamProcessor.VarIntOutput(value, count)
            }

            shift += 7
            if (shift >= 32) {
                logger.trace("Varint overflow, offset {} shift {}", offset, shift)
                return StreamProcessor.VarIntOutput(-1, -1)
            }
        }
    }

    private fun findArrayIndex(data: ByteArray, vararg pattern: Int): Int {
        if (pattern.isEmpty()) return 0

        val p = ByteArray(pattern.size) { pattern[it].toByte() }

        val lps = IntArray(p.size)
        var len = 0
        for (i in 1 until p.size) {
            while (len > 0 && p[i] != p[len]) len = lps[len - 1]
            if (p[i] == p[len]) len++
            lps[i] = len
        }

        var i = 0
        var j = 0
        while (i < data.size) {
            if (data[i] == p[j]) {
                i++; j++
                if (j == p.size) return i - j
            } else if (j > 0) {
                j = lps[j - 1]
            } else {
                i++
            }
        }
        return -1
    }

    private fun parseUInt16le(packet: ByteArray, offset: Int = 0): Int {
        return (packet[offset].toInt() and 0xff) or ((packet[offset + 1].toInt() and 0xff) shl 8)
    }
}
