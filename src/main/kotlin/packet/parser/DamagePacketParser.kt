package com.tbread.packet.parser

import com.tbread.entity.ParsedDamagePacket
import com.tbread.entity.SpecialDamage
import com.tbread.logging.DebugLogWriter
import org.slf4j.LoggerFactory

class DamagePacketParser {
    private val logger = LoggerFactory.getLogger(DamagePacketParser::class.java)

    data class VarIntOutput(val value: Int, val length: Int)

    private val mask = 0x0f

    data class DamagePacketParseResult(
        val pdp: ParsedDamagePacket,
        val damageType: Byte,
        val flagsOffset: Int,
        val flagsLength: Int
    )

    fun parseDamagePacket(packet: ByteArray): DamagePacketParseResult? {
        if (packet[0] == 0x20.toByte()) return null
        val reader = DamagePacketReader(packet)
        return reader.parse()
    }

    private fun canReadVarInt(bytes: ByteArray, offset: Int): Boolean {
        return offset >= 0 && offset < bytes.size
    }

    private fun readVarInt(bytes: ByteArray, offset: Int = 0): VarIntOutput {
        if (!canReadVarInt(bytes, offset)) {
            return VarIntOutput(-1, -1)
        }
        var value = 0
        var shift = 0
        var count = 0

        while (true) {
            if (offset + count >= bytes.size) {
                logger.trace("Array out of bounds, packet {} offset {} count {}", toHex(bytes), offset, count)
                return VarIntOutput(-1, -1)
            }

            val byteVal = bytes[offset + count].toInt() and 0xff
            count++

            value = value or (byteVal and 0x7F shl shift)

            if ((byteVal and 0x80) == 0) {
                return VarIntOutput(value, count)
            }

            shift += 7
            if (shift >= 32) {
                logger.trace(
                    "Varint overflow, packet {} offset {} shift {}",
                    toHex(bytes.copyOfRange(offset, offset + 4)),
                    offset,
                    shift
                )
                return VarIntOutput(-1, -1)
            }
        }
    }

    private fun parseUInt32le(packet: ByteArray, offset: Int = 0): Int {
        require(offset + 4 <= packet.size) { "Packet length is shorter than required" }
        return ((packet[offset].toInt() and 0xFF)) or
                ((packet[offset + 1].toInt() and 0xFF) shl 8) or
                ((packet[offset + 2].toInt() and 0xFF) shl 16) or
                ((packet[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun parseSpecialDamageFlags(packet: ByteArray, offset: Int = 0, length: Int = packet.size): List<SpecialDamage> {
        val flags = mutableListOf<SpecialDamage>()

        if (length == 8) {
            return emptyList()
        }
        if (length >= 10 && offset < packet.size) {
            val flagByte = packet[offset].toInt() and 0xFF

            if ((flagByte and 0x01) != 0) {
                flags.add(SpecialDamage.BACK)
            }
            if ((flagByte and 0x02) != 0) {
                flags.add(SpecialDamage.UNKNOWN)
            }

            if ((flagByte and 0x04) != 0) {
                flags.add(SpecialDamage.PARRY)
            }

            if ((flagByte and 0x08) != 0) {
                flags.add(SpecialDamage.PERFECT)
            }

            if ((flagByte and 0x10) != 0) {
                flags.add(SpecialDamage.DOUBLE)
            }

            if ((flagByte and 0x20) != 0) {
                flags.add(SpecialDamage.ENDURE)
            }

            if ((flagByte and 0x40) != 0) {
                flags.add(SpecialDamage.UNKNOWN4)
            }

            if ((flagByte and 0x80) != 0) {
                flags.add(SpecialDamage.POWER_SHARD)
            }
        }

        return flags
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }

    private inner class DamagePacketReader(private val packet: ByteArray) {
        private var offset = 0

        fun parse(): DamagePacketParseResult? {
            if (!readAndValidateHeader()) return null

            val targetInfo = readVarIntAt() ?: return null
            if (targetInfo.value <= 0) return null

            val switchInfo = readVarIntAt() ?: return null
            val switchValue = switchInfo.value and mask
            if (switchValue !in 4..7) return null

            val flagInfo = readVarIntAt() ?: return null

            val actorInfo = readVarIntAt() ?: return null
            if (actorInfo.value <= 0) return null

            if (!hasRemaining(5)) return null
            val skillCode = parseUInt32le(packet, offset)
            offset += 5

            val typeInfo = readVarIntAt() ?: return null
            if (!hasRemaining()) return null
            val damageType = packet[offset]

            val flagsOffset = offset
            val flagsLength = getSpecialBlockSize(switchValue)
            if (!hasRemaining(flagsLength)) return null
            val specialFlags = parseSpecialDamageFlags(packet, flagsOffset, flagsLength)
            offset += flagsLength

            val unknownInfo = readVarIntAt() ?: return null
            val damageInfo = readVarIntAt() ?: return null
            val loopInfo = readVarIntAt() ?: return null

            val pdp = ParsedDamagePacket()
            pdp.setTargetId(targetInfo.value)
            pdp.setSwitchVariable(switchInfo.value)
            pdp.setFlag(flagInfo.value)
            pdp.setActorId(actorInfo.value)
            pdp.setSkillCode(skillCode)
            pdp.setType(typeInfo.value)
            pdp.setSpecials(specialFlags)
            pdp.setUnknown(unknownInfo.value)
            pdp.setDamage(damageInfo.value)
            pdp.setLoop(loopInfo.value)
            return DamagePacketParseResult(pdp, damageType, flagsOffset, flagsLength)
        }

        private fun readAndValidateHeader(): Boolean {
            val lengthInfo = readVarInt(packet)
            if (lengthInfo.length < 0) return false
            offset += lengthInfo.length
            if (!hasRemaining(2)) return false
            if (packet[offset] != 0x04.toByte() || packet[offset + 1] != 0x38.toByte()) return false
            offset += 2
            return hasRemaining()
        }

        private fun readVarIntAt(): VarIntOutput? {
            val info = readVarInt(packet, offset)
            if (info.length < 0) return null
            offset += info.length
            return info
        }

        private fun hasRemaining(count: Int = 1): Boolean {
            return offset + count <= packet.size
        }

        private fun getSpecialBlockSize(switchValue: Int): Int {
            return when (switchValue) {
                4 -> 8
                5 -> 12
                6 -> 10
                7 -> 14
                else -> 0
            }
        }
    }
}
