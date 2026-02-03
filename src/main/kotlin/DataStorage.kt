package com.tbread

import com.tbread.entity.ParsedDamagePacket
import com.tbread.logging.DebugLogWriter
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

class DataStorage {
    private val logger = LoggerFactory.getLogger(DataStorage::class.java)
    private val byTargetStorage = ConcurrentHashMap<Int, ConcurrentSkipListSet<ParsedDamagePacket>>()
    private val byActorStorage = ConcurrentHashMap<Int, ConcurrentSkipListSet<ParsedDamagePacket>>()
    private val nicknameStorage = ConcurrentHashMap<Int, String>()
    private val pendingNicknameStorage = ConcurrentHashMap<Int, String>()
    private val summonStorage = ConcurrentHashMap<Int, Int>()
    private val skillCodeData = ConcurrentHashMap<Int, String>()
    private val mobCodeData = ConcurrentHashMap<Int, String>()
    private val mobStorage = ConcurrentHashMap<Int, Int>()
    private val currentTarget = AtomicInteger(0)

    @Synchronized
    fun appendDamage(pdp: ParsedDamagePacket) {
        byActorStorage.getOrPut(pdp.getActorId()) { ConcurrentSkipListSet(compareBy<ParsedDamagePacket> { it.getTimeStamp() }.thenBy { it.getUuid() }) }
            .add(pdp)
        byTargetStorage.getOrPut(pdp.getTargetId()) { ConcurrentSkipListSet(compareBy<ParsedDamagePacket> { it.getTimeStamp() }.thenBy { it.getUuid() }) }
            .add(pdp)
        applyPendingNickname(pdp.getActorId())
    }

    fun setCurrentTarget(targetId: Int) {
        currentTarget.set(targetId)
    }

    fun getCurrentTarget(): Int {
        return currentTarget.get()
    }

    fun appendMobCode(code: Int, name: String) {
        //이건나중에 파일이나 서버에서 불러오는걸로
        mobCodeData[code] = name
    }

    fun appendMob(mid: Int, code: Int) {
        mobStorage[mid] = code
    }

    fun appendSummon(summoner: Int, summon: Int) {
        summonStorage[summon] = summoner
    }

    fun appendNickname(uid: Int, nickname: String) {
        val nicknameBytes = nickname.toByteArray(Charsets.UTF_8).size
        val codepoints = nickname.map { it.code }.joinToString(",")

        logger.debug("appendNickname: uid={}, nickname=\"{}\", bytes={}, codepoints={}",
            uid, nickname, nicknameBytes, codepoints)

        if (nicknameStorage[uid] != null && nicknameStorage[uid].equals(nickname)) return
        if (nicknameStorage[uid] != null &&
            nickname.toByteArray(Charsets.UTF_8).size == 2 &&
            nickname.toByteArray(Charsets.UTF_8).size < nicknameStorage[uid]!!.toByteArray(Charsets.UTF_8).size
        ) {
            logger.debug("Nickname registration skipped {} -x> {} (existing bytes={}, new bytes={})",
                nicknameStorage[uid], nickname,
                nicknameStorage[uid]!!.toByteArray(Charsets.UTF_8).size,
                nicknameBytes)
            DebugLogWriter.debug(
                logger,
                "Nickname registration skipped {} -x> {} (existing bytes={}, new bytes={})",
                nicknameStorage[uid],
                nickname,
                nicknameStorage[uid]!!.toByteArray(Charsets.UTF_8).size,
                nicknameBytes
            )
            return
        }
        logger.debug("Nickname registered {} -> {} (bytes={})", nicknameStorage[uid], nickname, nicknameBytes)
        DebugLogWriter.debug(logger, "Nickname registered {} -> {} (bytes={})", nicknameStorage[uid], nickname, nicknameBytes)
        nicknameStorage[uid] = nickname
    }

    fun cachePendingNickname(uid: Int, nickname: String) {
        if (nicknameStorage[uid] != null) return
        pendingNicknameStorage[uid] = nickname
        logger.debug("Cached pending nickname {} -> {}", uid, nickname)
        DebugLogWriter.debug(logger, "Cached pending nickname {} -> {}", uid, nickname)
    }

    private fun applyPendingNickname(uid: Int) {
        if (nicknameStorage[uid] != null) return
        val pending = pendingNicknameStorage.remove(uid) ?: return
        logger.debug("Applied pending nickname {} -> {}", uid, pending)
        DebugLogWriter.debug(logger, "Applied pending nickname {} -> {}", uid, pending)
        appendNickname(uid, pending)
    }

    @Synchronized
    fun flushDamageStorage() {
        byActorStorage.clear()
        byTargetStorage.clear()
        summonStorage.clear()
        logger.info("Damage packets reset")
    }

    private fun flushNicknameStorage() {
        nicknameStorage.clear()
    }

    fun getSkillName(skillCode: Int): String {
        return skillCodeData[skillCode] ?: skillCode.toString()
    }

    fun getActorData(): ConcurrentHashMap<Int, ConcurrentSkipListSet<ParsedDamagePacket>> {
        return byActorStorage
    }

    fun getBossModeData(): ConcurrentHashMap<Int, ConcurrentSkipListSet<ParsedDamagePacket>> {
        return byTargetStorage
    }

    fun getNickname(): ConcurrentHashMap<Int, String> {
        return nicknameStorage
    }

    fun getSummonData(): ConcurrentHashMap<Int, Int> {
        return summonStorage
    }

    fun getMobCodeData(): ConcurrentHashMap<Int, String> {
        return mobCodeData
    }

    fun getMobData(): ConcurrentHashMap<Int, Int> {
        return mobStorage
    }
}
