package com.tbread.packet.parser

import com.tbread.DataStorage
import com.tbread.logging.DebugLogWriter
import org.slf4j.LoggerFactory

class NameResolver(private val dataStorage: DataStorage) {
    private val logger = LoggerFactory.getLogger(NameResolver::class.java)

    data class VarIntOutput(val value: Int, val length: Int)

    fun parseNickname(buffer: ByteArray): Boolean {
        val packetLengthInfo = readVarInt(buffer)
        if (packetLengthInfo.length < 0) return false
        val offset = packetLengthInfo.length
        if (offset + 1 >= buffer.size) return false
        if (buffer[offset] != 0x04.toByte()) return false
        if (buffer[offset + 1] != 0x8d.toByte()) return false

        val searchStart = packetLengthInfo.length + 2
        val searchEnd = minOf(buffer.size - 2, searchStart + 24)
        for (candidateOffset in searchStart..searchEnd) {
            if (!canReadVarInt(buffer, candidateOffset)) continue
            val playerInfo = readVarInt(buffer, candidateOffset)
            if (playerInfo.length <= 0) continue
            val nameLengthOffset = candidateOffset + playerInfo.length
            if (nameLengthOffset >= buffer.size) continue
            val nicknameLength = buffer[nameLengthOffset].toInt() and 0xff
            if (nicknameLength == 0 || nicknameLength > 72) continue
            val nameEnd = nameLengthOffset + 1 + nicknameLength
            if (nameEnd > buffer.size) continue
            val np = buffer.copyOfRange(nameLengthOffset + 1, nameEnd)
            val possibleName = String(np, Charsets.UTF_8)
            val sanitizedName = sanitizeNickname(possibleName) ?: continue
            logger.info("Nickname found (0x04 0x8D): {} (UID: {})", sanitizedName, playerInfo.value)
            DebugLogWriter.info(logger, "Nickname found (0x04 0x8D): {} (UID: {})", sanitizedName, playerInfo.value)
            dataStorage.appendNickname(playerInfo.value, sanitizedName)
            return true
        }
        return false
    }

    fun parseEntityNameBindingRules(packet: ByteArray): Boolean {
        var i = 0
        var lastAnchor: ActorAnchor? = null
        val namedActors = mutableSetOf<Int>()
        while (i < packet.size) {
            if (packet[i] == 0x36.toByte()) {
                if (i + 1 < packet.size) {
                    val actorInfo = readVarInt(packet, i + 1)
                    lastAnchor = if (actorInfo.length > 0 && actorInfo.value >= 1000) {
                        ActorAnchor(actorInfo.value, i, i + 1 + actorInfo.length)
                    } else {
                        null
                    }
                }
                i++
                continue
            }

            if (packet[i] == 0x07.toByte()) {
                val nameInfo = readAsciiName(packet, i)
                if (nameInfo != null && lastAnchor != null && lastAnchor.actorId !in namedActors) {
                    val distance = i - lastAnchor.endIndex
                    if (distance >= 0) {
                        val canBind = registerAsciiNickname(
                            packet,
                            lastAnchor.actorId,
                            nameInfo.first,
                            nameInfo.second
                        )
                        if (canBind) {
                            namedActors.add(lastAnchor.actorId)
                            lastAnchor = null
                            return true
                        }
                    }
                }
            }
            i++
        }
        return false
    }

    fun parseLootAttributionActorName(packet: ByteArray): Boolean {
        val candidates = mutableListOf<ActorNameCandidate>()
        var idx = 0
        while (idx + 2 < packet.size) {
            val marker = packet[idx].toInt() and 0xff
            val markerNext = packet[idx + 1].toInt() and 0xff
            val isMarker = marker == 0xF8 && markerNext == 0x03
            if (isMarker) {
                val actorOffset = idx - 2
                if (actorOffset < 0 || !canReadVarInt(packet, actorOffset)) {
                    idx++
                    continue
                }
                val actorInfo = readVarInt(packet, actorOffset)
                if (actorInfo.length != 2 || actorOffset + actorInfo.length != idx) {
                    idx++
                    continue
                }
                if (actorInfo.value !in 100..99999 || actorInfo.value == 0) {
                    idx++
                    continue
                }
                val lengthIdx = idx + 2
                if (lengthIdx >= packet.size) {
                    idx++
                    continue
                }
                val nameLength = packet[lengthIdx].toInt() and 0xff
                if (nameLength !in 3..16) {
                    idx++
                    continue
                }
                val nameStart = lengthIdx + 1
                val nameEnd = nameStart + nameLength
                if (nameEnd > packet.size) {
                    idx++
                    continue
                }
                val nameBytes = packet.copyOfRange(nameStart, nameEnd)
                val possibleName = decodeUtf8Strict(nameBytes)
                if (possibleName == null) {
                    idx++
                    continue
                }
                val sanitizedName = sanitizeNickname(possibleName)
                if (sanitizedName == null) {
                    idx = nameEnd
                    continue
                }
                candidates.add(ActorNameCandidate(actorInfo.value, sanitizedName, nameBytes))
                idx = skipGuildName(packet, nameEnd)
                continue
            }
            idx++
        }

        if (candidates.isEmpty()) return false
        val allowPrepopulate = candidates.size > 1
        var foundAny = false
        for (candidate in candidates) {
            if (!allowPrepopulate && !actorAppearsInCombat(candidate.actorId)) {
                dataStorage.cachePendingNickname(candidate.actorId, candidate.name)
                continue
            }
            if (dataStorage.getNickname()[candidate.actorId] != null) continue
            logger.info(
                "Loot attribution actor name found {} -> {} (hex={})",
                candidate.actorId,
                candidate.name,
                toHex(candidate.nameBytes)
            )
            DebugLogWriter.info(
                logger,
                "Loot attribution actor name found {} -> {} (hex={})",
                candidate.actorId,
                candidate.name,
                toHex(candidate.nameBytes)
            )
            dataStorage.appendNickname(candidate.actorId, candidate.name)
            foundAny = true
        }
        return foundAny
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

    private fun decodeUtf8Strict(bytes: ByteArray): String? {
        return try {
            val decoded = String(bytes, Charsets.UTF_8)
            if (decoded.contains('\uFFFD')) null else decoded
        } catch (e: Exception) {
            null
        }
    }

    private fun skipGuildName(packet: ByteArray, startIndex: Int): Int {
        if (startIndex >= packet.size) return startIndex
        val length = packet[startIndex].toInt() and 0xff
        if (length !in 1..32) return startIndex
        val nameStart = startIndex + 1
        val nameEnd = nameStart + length
        if (nameEnd > packet.size) return startIndex
        val nameBytes = packet.copyOfRange(nameStart, nameEnd)
        val possibleName = decodeUtf8Strict(nameBytes) ?: return startIndex
        return nameEnd
    }

    private fun actorExists(actorId: Int): Boolean {
        return dataStorage.getNickname().containsKey(actorId) ||
                dataStorage.getActorData().containsKey(actorId) ||
                dataStorage.getBossModeData().containsKey(actorId) ||
                dataStorage.getSummonData().containsKey(actorId)
    }

    private fun actorAppearsInCombat(actorId: Int): Boolean {
        return dataStorage.getActorData().containsKey(actorId)
    }

    private data class ActorNameCandidate(
        val actorId: Int,
        val name: String,
        val nameBytes: ByteArray
    )

    private data class ActorAnchor(val actorId: Int, val startIndex: Int, val endIndex: Int)

    private fun readAsciiName(packet: ByteArray, anchorIndex: Int): Pair<Int, Int>? {
        val lengthIndex = anchorIndex + 1
        if (lengthIndex >= packet.size) return null
        val nameLength = packet[lengthIndex].toInt() and 0xff
        if (nameLength !in 1..16) return null
        val nameStart = lengthIndex + 1
        val nameEnd = nameStart + nameLength
        if (nameEnd > packet.size) return null
        return nameStart to nameLength
    }

    private fun registerAsciiNickname(
        packet: ByteArray,
        actorId: Int,
        nameStart: Int,
        nameLength: Int
    ): Boolean {
        if (dataStorage.getNickname()[actorId] != null) return false
        if (nameLength <= 0 || nameLength > 16) return false
        val nameEnd = nameStart + nameLength
        if (nameStart < 0 || nameEnd > packet.size) return false
        val possibleNameBytes = packet.copyOfRange(nameStart, nameEnd)
        val possibleName = decodeUtf8Strict(possibleNameBytes) ?: return false
        val sanitizedName = sanitizeNickname(possibleName) ?: return false
        if (!actorExists(actorId)) {
            dataStorage.cachePendingNickname(actorId, sanitizedName)
            return true
        }
        val existingNickname = dataStorage.getNickname()[actorId]
        if (existingNickname != sanitizedName) {
            logger.info(
                "Actor name binding found {} -> {} (hex={})",
                actorId,
                sanitizedName,
                toHex(possibleNameBytes)
            )
            DebugLogWriter.info(
                logger,
                "Actor name binding found {} -> {} (hex={})",
                actorId,
                sanitizedName,
                toHex(possibleNameBytes)
            )
        }
        dataStorage.appendNickname(actorId, sanitizedName)
        return true
    }

    private fun sanitizeNickname(nickname: String): String? {
        val sanitizedNickname = nickname.substringBefore('\u0000').trim()
        if (sanitizedNickname.isEmpty()) return null
        if (sanitizedNickname.contains('\uFFFD')) return null
        val nicknameBuilder = StringBuilder()
        var onlyNumbers = true
        var hasHan = false
        for (ch in sanitizedNickname) {
            if (!Character.isLetterOrDigit(ch)) {
                if (nicknameBuilder.isEmpty()) return null
                break
            }
            if (ch == '\uFFFD') {
                if (nicknameBuilder.isEmpty()) return null
                break
            }
            if (Character.isISOControl(ch)) {
                if (nicknameBuilder.isEmpty()) return null
                break
            }
            nicknameBuilder.append(ch)
            if (Character.isLetter(ch)) onlyNumbers = false
            if (Character.UnicodeScript.of(ch.code) == Character.UnicodeScript.HAN) {
                hasHan = true
            }
        }
        val trimmedNickname = nicknameBuilder.toString()
        if (trimmedNickname.isEmpty()) return null
        val hasDigit = trimmedNickname.any { it.isDigit() }
        if (onlyNumbers || (trimmedNickname.firstOrNull()?.isDigit() == true)) return null
        if (trimmedNickname.length == 1) {
            return if (hasHan) trimmedNickname else null
        }
        val hasLetter = trimmedNickname.any { it.isLetter() }
        if (trimmedNickname.length == 2) {
            return if (hasLetter && hasDigit) trimmedNickname else null
        }
        return if (hasLetter) trimmedNickname else null
    }

    private fun toHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
}
