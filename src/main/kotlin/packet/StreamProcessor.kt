package com.tbread.packet

import com.tbread.DataStorage
import com.tbread.entity.ParsedDamagePacket
import com.tbread.entity.SpecialDamage
import com.tbread.logging.DebugLogWriter
import com.tbread.logging.SkillAnalysisLogger
import com.tbread.packet.parser.DamagePacketParser
import com.tbread.packet.parser.NameResolver
import com.tbread.packet.parser.SummonTracker
import org.slf4j.LoggerFactory

class StreamProcessor(private val dataStorage: DataStorage) {
    private val logger = LoggerFactory.getLogger(StreamProcessor::class.java)
    private val damagePacketParser = DamagePacketParser()
    private val nameResolver = NameResolver(dataStorage)
    private val summonTracker = SummonTracker(dataStorage)

    data class VarIntOutput(val value: Int, val length: Int)

    private val mask = 0x0f
    private val packetStartMarker = byteArrayOf(0x06, 0x00, 0x36)

    // 🔍 DEBUG: ตั้งค่าการดัก packet
    private val ENABLE_OPCODE_FILTER = false  // เปิดเพื่อดัก opcode เฉพาะ (ปิดเพื่อประสิทธิภาพ)
    private val FILTER_OPCODES = setOf(
        0x04 to 0x38,  // Damage
        0x05 to 0x38,  // DoT
        0x04 to 0x8D,  // Nickname
        // เพิ่ม opcode ที่ต้องการดักได้ที่นี่
    )

    private val ENABLE_STRING_SEARCH = false  // เปิดเพื่อค้นหาข้อความใน packet (ปิดเพื่อประสิทธิภาพ)
    private val SEARCH_STRINGS = listOf(
        "Training Scarecrow",  // ชื่อมอน
        "Grim",  // ชื่อผู้เล่น
        "Boss",
        // เพิ่มคำที่ต้องการค้นหาได้ที่นี่
    )

    fun onPacketReceived(packet: ByteArray) {
        // 🔍 DEBUG: ดัก packet ทั้งหมด
        if (packet.size >= 3) {
            logPacketDebug(packet)
        }

        val packetLengthInfo = readVarInt(packet)
        if (packetLengthInfo.length < 0) {
            logger.warn("Broken packet: failed to read varint length {}", toHex(packet))
            return
        }
        val packetSize = computePacketSize(packetLengthInfo)
        if (packetSize <= 0) {
            logger.trace(
                "Broken packet: invalid computed size {} from length {} (varint length {})",
                packetSize,
                packetLengthInfo.value,
                packetLengthInfo.length
            )
            return
        }
        if (packet.size == packetSize) {
            logger.trace(
                "Current byte length matches expected length: {}",
                toHex(packet.copyOfRange(0, packetSize))
            )
            if (isUnknownFFPacket(packet)) return
            parsePerfectPacket(packet.copyOfRange(0, packetSize))
            //더이상 자를필요가 없는 최종 패킷뭉치
            return
        }
        if (packet.size <= 3) return
        // 매직패킷 단일로 올때 무시
        if (packetSize > packet.size) {
            logger.debug("Broken packet: current byte length is shorter than expected: {}", toHex(packet))
            val resyncIdx = findArrayIndex(packet, packetStartMarker)
            if (resyncIdx > 0) {
                onPacketReceived(packet.copyOfRange(resyncIdx, packet.size))
            } else {
                if (isUnknownFFPacket(packet)) return
                parseBrokenLengthPacket(packet)
            }
            //길이헤더가 실제패킷보다 김 보통 여기 닉네임이 몰려있는듯?
            return
        }

        try {
            if (packet.copyOfRange(0, packetSize).size != 3) {
                if (packet.copyOfRange(0, packetSize).isNotEmpty()) {
                    logger.trace(
                        "Packet split succeeded: {}",
                        toHex(packet.copyOfRange(0, packetSize))
                    )
                    if (!isUnknownFFPacket(packet.copyOfRange(0, packetSize))) {
                        parsePerfectPacket(packet.copyOfRange(0, packetSize))
                    }
                    //매직패킷이 빠져있는 패킷뭉치
                }
            }

            onPacketReceived(packet.copyOfRange(packetSize, packet.size))
            //남은패킷 재처리
        } catch (e: Exception) {
            logger.error("Exception while consuming packet {}", toHex(packet), e)
            return
        }

    }

    private fun parseBrokenLengthPacket(packet: ByteArray, flag: Boolean = true) {
        logger.debug("Broken packet buffer detected: {}", toHex(packet))
        if (packet[2] != 0xff.toByte() || packet[3] != 0xff.toByte()) {
            logger.trace("Remaining packet buffer: {}", toHex(packet))
            val target = dataStorage.getCurrentTarget()
            var processed = false
            if (target != 0) {
                val targetBytes = convertVarInt(target)
                val damageOpcodes = byteArrayOf(0x04, 0x38)
                val dotOpcodes = byteArrayOf(0x05, 0x38)
                val damageKeyword = damageOpcodes + targetBytes
                val dotKeyword = dotOpcodes + targetBytes
                val damageIdx = findArrayIndex(packet, damageKeyword)
                val dotIdx = findArrayIndex(packet, dotKeyword)
                val (idx, handler) = when {
                    damageIdx > 0 && dotIdx > 0 -> {
                        if (damageIdx < dotIdx) damageIdx to { slice: ByteArray -> parsingDamage(slice) }
                        else dotIdx to { slice: ByteArray -> parseDoTPacket(slice); true }
                    }
                    damageIdx > 0 -> damageIdx to { slice: ByteArray -> parsingDamage(slice) }
                    dotIdx > 0 -> dotIdx to { slice: ByteArray -> parseDoTPacket(slice); true }
                    else -> -1 to null
                }
                processed = processBrokenPacketSlice(packet, idx, handler)
            }
            if (!processed) {
                val damageIdx = findArrayIndex(packet, byteArrayOf(0x04, 0x38))
                val dotIdx = findArrayIndex(packet, byteArrayOf(0x05, 0x38))
                val (idx, handler) = when {
                    damageIdx > 0 && dotIdx > 0 -> {
                        if (damageIdx < dotIdx) damageIdx to { slice: ByteArray -> parsingDamage(slice) }
                        else dotIdx to { slice: ByteArray -> parseDoTPacket(slice); true }
                    }
                    damageIdx > 0 -> damageIdx to { slice: ByteArray -> parsingDamage(slice) }
                    dotIdx > 0 -> dotIdx to { slice: ByteArray -> parseDoTPacket(slice); true }
                    else -> -1 to null
                }
                processed = processBrokenPacketSlice(packet, idx, handler)
            }
            if (flag && !processed) {
                logger.debug("Remaining packet {}", toHex(packet))
                nameResolver.parseEntityNameBindingRules(packet)
                nameResolver.parseLootAttributionActorName(packet)
            }
            return
        }
        val newPacket = packet.copyOfRange(10, packet.size)
        onPacketReceived(newPacket)
    }

    private fun processBrokenPacketSlice(
        packet: ByteArray,
        idx: Int,
        handler: ((ByteArray) -> Boolean)?
    ): Boolean {
        if (idx <= 0 || handler == null) return false
        val packetLengthInfo = readVarInt(packet, idx - 1)
        if (packetLengthInfo.length != 1) return false
        val startIdx = idx - 1
        val packetSize = computePacketSize(packetLengthInfo)
        if (packetSize <= 0) return false
        val endIdx = startIdx + packetSize
        if (startIdx !in 0..<endIdx || endIdx > packet.size) return false
        val extractedPacket = packet.copyOfRange(startIdx, endIdx)
        val handled = handler(extractedPacket)
        if (handled && endIdx < packet.size) {
            val remainingPacket = packet.copyOfRange(endIdx, packet.size)
            parseBrokenLengthPacket(remainingPacket, false)
        }
        return handled
    }

    private fun canReadVarInt(bytes: ByteArray, offset: Int): Boolean {
        return offset >= 0 && offset < bytes.size
    }

    private fun isUnknownFFPacket(packet: ByteArray): Boolean {
        if (packet.size < 2) return false
        return packet[packet.size - 2] == 0xff.toByte() &&
                packet[packet.size - 1] == 0xff.toByte()
    }

    private fun parsePerfectPacket(packet: ByteArray) {
        if (packet.size < 3) return
        var flag = parsingDamage(packet)
        if (flag) return
        flag = nameResolver.parseNickname(packet)
        if (flag) return
        flag = nameResolver.parseEntityNameBindingRules(packet)
        if (flag) return
        flag = nameResolver.parseLootAttributionActorName(packet)
        if (flag) return
        flag = summonTracker.parseSummonPacket(packet)
        if (flag) return
        parseDoTPacket(packet)

    }

    private fun parseDoTPacket(packet:ByteArray){
        var offset = 0
        val pdp = ParsedDamagePacket()
        pdp.setDot(true)
        val packetLengthInfo = readVarInt(packet)
        if (packetLengthInfo.length < 0) return
        offset += packetLengthInfo.length

        if (packet[offset] != 0x05.toByte()) return
        if (packet[offset+1] != 0x38.toByte()) return
        offset += 2
        if (packet.size < offset) return

        val targetInfo = readVarInt(packet,offset)
        if (targetInfo.length < 0) return
        offset += targetInfo.length
        if (packet.size < offset) return
        pdp.setTargetId(targetInfo)

        offset += 1
        if (packet.size < offset) return

        val actorInfo = readVarInt(packet,offset)
        if (actorInfo.length < 0) return
        if (actorInfo.value == targetInfo.value) return
        offset += actorInfo.length
        if (packet.size < offset) return
        pdp.setActorId(actorInfo)

        val unknownInfo = readVarInt(packet,offset)
        if (unknownInfo.length <0) return
        offset += unknownInfo.length

        val skillCode:Int = parseUInt32le(packet,offset) / 100
        offset += 4
        if (packet.size <= offset) return
        pdp.setSkillCode(skillCode)

        val damageInfo = readVarInt(packet,offset)
        if (damageInfo.length < 0) return
        pdp.setDamage(damageInfo)

        logger.debug("{}", toHex(packet))
        DebugLogWriter.debug(logger, "{}", toHex(packet))
        logger.debug(
            "Dot damage actor {}, target {}, skill {}, damage {}",
            pdp.getActorId(),
            pdp.getTargetId(),
            pdp.getSkillCode1(),
            pdp.getDamage()
        )
        DebugLogWriter.debug(
            logger,
            "Dot damage actor {}, target {}, skill {}, damage {}",
            pdp.getActorId(),
            pdp.getTargetId(),
            pdp.getSkillCode1(),
            pdp.getDamage()
        )
        logger.debug("----------------------------------")
        DebugLogWriter.debug(logger, "----------------------------------")

        // Log for skill analysis if enabled
        // SkillAnalysisLogger.logPacket(packet, pdp)

        if (pdp.getActorId() != pdp.getTargetId()) {
            dataStorage.appendDamage(pdp)
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

    private fun findArrayIndex(data: ByteArray, p: ByteArray): Int {
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

    private fun parseUInt32le(packet: ByteArray, offset: Int = 0): Int {
        require(offset + 4 <= packet.size) { "Packet length is shorter than required" }
        return ((packet[offset].toInt() and 0xFF)) or
                ((packet[offset + 1].toInt() and 0xFF) shl 8) or
                ((packet[offset + 2].toInt() and 0xFF) shl 16) or
                ((packet[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun parsingDamage(packet: ByteArray): Boolean {
        val parsed = damagePacketParser.parseDamagePacket(packet) ?: return false
        val pdp = parsed.pdp
        val damageType = parsed.damageType
        pdp.setPayload(packet)

//        if (loopInfo.value != 0 && offset >= packet.size) return false
//
//        if (loopInfo.value != 0) {
//            for (i in 0 until loopInfo.length) {
//                var skipValueInfo = readVarInt(packet, offset)
//                if (skipValueInfo.length < 0) return false
//                pdp.addSkipData(skipValueInfo)
//                offset += skipValueInfo.length
//            }
//        }

        logger.trace("{}", toHex(packet))
        logger.trace("Type packet {}", toHex(byteArrayOf(damageType)))
        logger.trace(
            "Type packet bits {}",
            String.format("%8s", (damageType.toInt() and 0xFF).toString(2)).replace(' ', '0')
        )
        logger.trace("Varint packet: {}", toHex(packet.copyOfRange(parsed.flagsOffset, parsed.flagsOffset + parsed.flagsLength)))
        logger.debug(
            "Target: {}, attacker: {}, skill: {}, type: {}, damage: {}, damage flag: {}",
            pdp.getTargetId(),
            pdp.getActorId(),
            pdp.getSkillCode1(),
            pdp.getType(),
            pdp.getDamage(),
            pdp.getSpecials()
        )
        DebugLogWriter.debug(
            logger,
            "Target: {}, attacker: {}, skill: {}, type: {}, damage: {}, damage flag: {}",
            pdp.getTargetId(),
            pdp.getActorId(),
            pdp.getSkillCode1(),
            pdp.getType(),
            pdp.getDamage(),
            pdp.getSpecials()
        )

        // Log for skill analysis if enabled
        // SkillAnalysisLogger.logPacket(packet, pdp)

        val isAccepted = pdp.getActorId() != pdp.getTargetId()
        if (isAccepted) {
            //추후 hps 를 넣는다면 수정하기
            //혹시 나중เมื่อ เกิดเงื่อนไข self-damage กับ boss mechanic
            dataStorage.appendDamage(pdp)
        }
        return isAccepted

    }

    private fun toHex(bytes: ByteArray): String {
        //출력테스트용
        return bytes.joinToString(" ") { "%02X".format(it) }
    }

    /**
     * 🔍 DEBUG: ฟังก์ชันสำหรับดัก packet ตามเงื่อนไข
     */
    private fun logPacketDebug(packet: ByteArray) {
        // ดัก packet ทั้งหมด (แสดงแค่ 50 bytes แรก)
        DebugLogWriter.debug(logger, "📦 Packet received (size: {}): {}",
            packet.size, toHex(packet.take(50).toByteArray()))

        // ถ้าเปิด opcode filter - ดักเฉพาะ opcode ที่สนใจ
        if (ENABLE_OPCODE_FILTER && packet.size >= 4) {
            val packetLengthInfo = readVarInt(packet)
            if (packetLengthInfo.length > 0) {
                val offset = packetLengthInfo.length
                if (offset + 1 < packet.size) {
                    val opcode1 = packet[offset].toInt() and 0xFF
                    val opcode2 = packet[offset + 1].toInt() and 0xFF
                    val opcodeKey = opcode1 to opcode2

                    if (FILTER_OPCODES.contains(opcodeKey)) {
                        DebugLogWriter.info(logger, "🎯 Opcode match (0x{} 0x{}): {}",
                            "%02X".format(opcode1), "%02X".format(opcode2), toHex(packet))
                    }
                }
            }
        }

        // ถ้าเปิด string search - ค้นหาข้อความใน packet
        if (ENABLE_STRING_SEARCH) {
            searchStringInPacket(packet)
        }
    }

    /**
     * 🔍 DEBUG: ค้นหาข้อความที่ต้องการใน packet
     */
    private fun searchStringInPacket(packet: ByteArray) {
        try {
            // แปลง packet เป็น string (UTF-8)
            val packetText = String(packet, Charsets.UTF_8)

            for (searchString in SEARCH_STRINGS) {
                if (packetText.contains(searchString, ignoreCase = true)) {
                    DebugLogWriter.info(logger, "🔎 String found: \"{}\" in packet", searchString)
                    DebugLogWriter.info(logger, "   Full packet (hex): {}", toHex(packet))
                    DebugLogWriter.info(logger, "   Packet (text): {}",
                        packetText.replace(Regex("[\\x00-\\x1F\\x7F-\\x9F]"), "."))

                    // แสดงตำแหน่งที่พบข้อความ
                    val index = packetText.indexOf(searchString, ignoreCase = true)
                    if (index >= 0) {
                        DebugLogWriter.info(logger, "   Position: byte {} (0x{})",
                            index, "%02X".format(index))

                        // แสดง context รอบ ๆ ข้อความที่พบ
                        val contextStart = maxOf(0, index - 10)
                        val contextEnd = minOf(packet.size, index + searchString.length + 10)
                        DebugLogWriter.info(logger, "   Context (hex): {}",
                            toHex(packet.copyOfRange(contextStart, contextEnd)))
                    }
                }
            }
        } catch (e: Exception) {
            // ไม่ต้องทำอะไร - บาง packet อาจแปลงเป็น string ไม่ได้
        }
    }

    /**
     * 🔍 DEBUG: ดัก packet ที่มี opcode เฉพาะ (เรียกใช้แบบ manual)
     */
    private fun logIfMatchesOpcode(packet: ByteArray, targetOpcode1: Int, targetOpcode2: Int, label: String = "") {
        if (packet.size < 4) return

        val packetLengthInfo = readVarInt(packet)
        if (packetLengthInfo.length > 0) {
            val offset = packetLengthInfo.length
            if (offset + 1 < packet.size) {
                val opcode1 = packet[offset].toInt() and 0xFF
                val opcode2 = packet[offset + 1].toInt() and 0xFF

                if (opcode1 == targetOpcode1 && opcode2 == targetOpcode2) {
                    DebugLogWriter.info(logger, "🎯 {} Opcode (0x{} 0x{}) detected",
                        label, "%02X".format(opcode1), "%02X".format(opcode2))
                    DebugLogWriter.info(logger, "   Packet: {}", toHex(packet))
                }
            }
        }
    }

    private fun readVarInt(bytes: ByteArray, offset: Int = 0): VarIntOutput {
        if (!canReadVarInt(bytes, offset)) {
            return VarIntOutput(-1, -1)
        }
        //구글 Protocol Buffers 라이브러리에 이미 있나? 코드 효율성에 차이있어보이면 나중에 바꾸는게 나을듯?
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

    fun convertVarInt(value: Int): ByteArray {
        val bytes = mutableListOf<Byte>()
        var num = value

        while (num > 0x7F) {
            bytes.add(((num and 0x7F) or 0x80).toByte())
            num = num ushr 7
        }
        bytes.add(num.toByte())

        return bytes.toByteArray()
    }

    private fun computePacketSize(info: VarIntOutput): Int {
        return info.value + info.length - 4
    }

}
