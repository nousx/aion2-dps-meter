package com.tbread.hotkey

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import com.tbread.packet.PropertyHandler
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

@Serializable
data class HotkeyConfig(
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val key: String = ""
)

object GlobalHotkeyManager {
    private val logger = LoggerFactory.getLogger(GlobalHotkeyManager::class.java)
    private val running = AtomicBoolean(false)
    private var messageThread: Thread? = null

    // Hotkey IDs
    private const val HOTKEY_TOGGLE = 1
    private const val HOTKEY_RESET = 2

    // Modifier keys
    private const val MOD_CONTROL = 0x0002
    private const val MOD_SHIFT = 0x0004
    private const val MOD_ALT = 0x0001

    // Callbacks
    private var onToggle: (() -> Unit)? = null
    private var onReset: (() -> Unit)? = null

    // Current hotkey configs
    private var toggleConfig = HotkeyConfig(ctrl = true, shift = false, alt = false, key = "T")
    private var resetConfig = HotkeyConfig(ctrl = true, shift = false, alt = false, key = "R")

    private val keyMap = mapOf(
        "A" to 0x41, "B" to 0x42, "C" to 0x43, "D" to 0x44, "E" to 0x45,
        "F" to 0x46, "G" to 0x47, "H" to 0x48, "I" to 0x49, "J" to 0x4A,
        "K" to 0x4B, "L" to 0x4C, "M" to 0x4D, "N" to 0x4E, "O" to 0x4F,
        "P" to 0x50, "Q" to 0x51, "R" to 0x52, "S" to 0x53, "T" to 0x54,
        "U" to 0x55, "V" to 0x56, "W" to 0x57, "X" to 0x58, "Y" to 0x59, "Z" to 0x5A,
        "F1" to 0x70, "F2" to 0x71, "F3" to 0x72, "F4" to 0x73,
        "F5" to 0x74, "F6" to 0x75, "F7" to 0x76, "F8" to 0x77,
        "F9" to 0x78, "F10" to 0x79, "F11" to 0x7A, "F12" to 0x7B,
        "0" to 0x30, "1" to 0x31, "2" to 0x32, "3" to 0x33, "4" to 0x34,
        "5" to 0x35, "6" to 0x36, "7" to 0x37, "8" to 0x38, "9" to 0x39
    )

    fun start(onToggleCallback: () -> Unit, onResetCallback: () -> Unit) {
        if (running.get()) {
            logger.warn("GlobalHotkeyManager already running")
            return
        }

        if (!com.sun.jna.Platform.isWindows()) {
            logger.warn("GlobalHotkeyManager only works on Windows")
            return
        }

        // Load hotkeys from settings
        loadHotkeys()

        onToggle = onToggleCallback
        onReset = onResetCallback
        running.set(true)

        messageThread = Thread({
            try {
                val user32 = User32.INSTANCE

                // Register hotkeys
                registerHotkeyFromConfig(user32, HOTKEY_TOGGLE, toggleConfig, "toggle")
                registerHotkeyFromConfig(user32, HOTKEY_RESET, resetConfig, "reset")

                // Message loop
                val msg = WinUser.MSG()
                while (running.get() && user32.GetMessage(msg, null, 0, 0) != 0) {
                    if (msg.message == WinUser.WM_HOTKEY) {
                        val hotkeyId = msg.wParam.toInt()

                        javafx.application.Platform.runLater {
                            when (hotkeyId) {
                                HOTKEY_TOGGLE -> {
                                    logger.info("Toggle hotkey pressed")
                                    onToggle?.invoke()
                                }
                                HOTKEY_RESET -> {
                                    logger.info("Reset hotkey pressed")
                                    onReset?.invoke()
                                }
                            }
                        }
                    }

                    user32.TranslateMessage(msg)
                    user32.DispatchMessage(msg)
                }

                // Cleanup
                user32.UnregisterHotKey(null, HOTKEY_TOGGLE)
                user32.UnregisterHotKey(null, HOTKEY_RESET)
                logger.info("Global hotkeys unregistered")

            } catch (e: Exception) {
                logger.error("Error in hotkey message loop", e)
            }
        }, "GlobalHotkeyThread").apply {
            isDaemon = true
            start()
        }
    }

    private fun registerHotkeyFromConfig(user32: User32, id: Int, config: HotkeyConfig, name: String) {
        val modifiers = buildModifiers(config)
        val vk = keyMap[config.key.uppercase()] ?: return

        if (user32.RegisterHotKey(null, id, modifiers, vk)) {
            logger.info("Registered $name hotkey: ${formatHotkey(config)}")
        } else {
            logger.error("Failed to register $name hotkey: ${formatHotkey(config)}")
        }
    }

    private fun buildModifiers(config: HotkeyConfig): Int {
        var mods = 0
        if (config.ctrl) mods = mods or MOD_CONTROL
        if (config.shift) mods = mods or MOD_SHIFT
        if (config.alt) mods = mods or MOD_ALT
        return mods
    }

    private fun formatHotkey(config: HotkeyConfig): String {
        val parts = mutableListOf<String>()
        if (config.ctrl) parts.add("Ctrl")
        if (config.shift) parts.add("Shift")
        if (config.alt) parts.add("Alt")
        if (config.key.isNotEmpty()) parts.add(config.key)
        return parts.joinToString("+")
    }

    fun updateHotkeys(toggleHotkey: HotkeyConfig?, resetHotkey: HotkeyConfig?) {
        if (!running.get()) return

        // Save to settings
        toggleHotkey?.let {
            toggleConfig = it
            PropertyHandler.setProperty("hotkey.toggle", Json.encodeToString(HotkeyConfig.serializer(), it))
        }
        resetHotkey?.let {
            resetConfig = it
            PropertyHandler.setProperty("hotkey.reset", Json.encodeToString(HotkeyConfig.serializer(), it))
        }

        // Restart to apply changes
        restart()
    }

    private fun loadHotkeys() {
        try {
            PropertyHandler.getProperty("hotkey.toggle")?.let {
                toggleConfig = Json.decodeFromString(HotkeyConfig.serializer(), it)
            }
            PropertyHandler.getProperty("hotkey.reset")?.let {
                resetConfig = Json.decodeFromString(HotkeyConfig.serializer(), it)
            }
            logger.info("Loaded hotkeys from settings")
        } catch (e: Exception) {
            logger.warn("Failed to load hotkeys, using defaults", e)
        }
    }

    private fun restart() {
        val toggleCb = onToggle
        val resetCb = onReset

        stop()
        Thread.sleep(100) // Give time for cleanup

        if (toggleCb != null && resetCb != null) {
            start(toggleCb, resetCb)
        }
    }

    fun stop() {
        if (!running.get()) return

        running.set(false)
        messageThread?.interrupt()
        messageThread = null

        onToggle = null
        onReset = null
    }
}
