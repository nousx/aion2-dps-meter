package com.tbread.webview

import com.tbread.DpsCalculator
import com.tbread.entity.DpsData
import com.tbread.hotkey.GlobalHotkeyManager
import com.tbread.logging.DebugLogWriter
import com.tbread.logging.PacketLogger
import com.tbread.logging.SkillAnalysisLogger
import com.tbread.logging.UnifiedLogger
import com.tbread.packet.CombatPortDetector
import com.tbread.packet.LocalPlayer
import com.tbread.packet.PropertyHandler
import com.tbread.windows.WindowTitleDetector
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.application.HostServices
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.util.Duration
import javafx.application.Platform
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import netscape.javascript.JSObject
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class BrowserApp(private val dpsCalculator: DpsCalculator) : Application() {

    companion object {
        @Volatile
        var isRecordingHotkey = false
    }

    private val logger = LoggerFactory.getLogger(BrowserApp::class.java)

    @Serializable
    data class ConnectionInfo(
        val ip: String?,
        val port: Int?,
        val locked: Boolean,
        val characterName: String?,
        val device: String?
    )

    class JSBridge(
        private val stage: Stage,
        private val dpsCalculator: DpsCalculator,
        private val hostServices: HostServices,
    ) {
        fun moveWindow(x: Double, y: Double) {
            stage.x = x
            stage.y = y
        }

        fun resetDps(){
            dpsCalculator.resetDataStorage()
        }

        fun resetAutoDetection() {
            CombatPortDetector.reset()
        }

        fun setCharacterName(name: String?) {
            val trimmed = name?.trim().orEmpty()
            LocalPlayer.characterName = if (trimmed.isBlank()) null else trimmed
        }

        fun setTargetSelection(mode: String?) {
            dpsCalculator.setTargetSelectionModeById(mode)
        }

        fun getConnectionInfo(): String {
            val ip = PropertyHandler.getProperty("server.ip")
            val lockedPort = CombatPortDetector.currentPort()
            val lockedDevice = CombatPortDetector.currentDevice()
            val storedDevice = PropertyHandler.getProperty("server.device")
            val fallbackPort = PropertyHandler.getProperty("server.port")?.toIntOrNull()
            val info = ConnectionInfo(
                ip = ip,
                port = lockedPort ?: fallbackPort,
                locked = lockedPort != null,
                characterName = LocalPlayer.characterName,
                device = lockedDevice ?: storedDevice
            )
            return Json.encodeToString(info)
        }

        fun getAion2WindowTitle(): String? {
            return WindowTitleDetector.findAion2WindowTitle()
        }

        fun openBrowser(url: String) {
            try {
                hostServices.showDocument(url)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun readResource(path: String): String? {
            val normalized = if (path.startsWith("/")) path else "/$path"
            return try {
                javaClass.getResourceAsStream(normalized)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
        }

        fun getSetting(key: String): String? {
            return PropertyHandler.getProperty(key)
        }

        fun setSetting(key: String, value: String) {
            PropertyHandler.setProperty(key, value)
        }

        fun setDebugLoggingEnabled(enabled: Boolean) {
            DebugLogWriter.setEnabled(enabled)
            PropertyHandler.setProperty(DebugLogWriter.SETTING_KEY, enabled.toString())
        }

        fun setPacketLoggingEnabled(enabled: Boolean) {
            if (enabled) {
                PacketLogger.enable()
            } else {
                PacketLogger.disable()
            }
            PropertyHandler.setProperty("packetLoggingEnabled", enabled.toString())
        }

        fun isPacketLoggingEnabled(): Boolean {
            return PacketLogger.isEnabled()
        }

        fun setSkillAnalysisEnabled(enabled: Boolean, skillId: Int = 13350000) {
            if (enabled) {
                SkillAnalysisLogger.enable(skillId)
            } else {
                SkillAnalysisLogger.disable()
            }
            PropertyHandler.setProperty("skillAnalysisEnabled", enabled.toString())
            PropertyHandler.setProperty("skillAnalysisTargetId", skillId.toString())
        }

        fun isSkillAnalysisEnabled(): Boolean {
            return SkillAnalysisLogger.isEnabled()
        }

        fun getSkillAnalysisTargetId(): Int {
            return SkillAnalysisLogger.getTargetSkillId()
        }

        fun startHotkeyRecording() {
            isRecordingHotkey = true
        }

        fun stopHotkeyRecording() {
            isRecordingHotkey = false
        }

        fun updateHotkey(action: String, ctrl: Boolean, shift: Boolean, alt: Boolean, key: String) {
            val config = com.tbread.hotkey.HotkeyConfig(ctrl, shift, alt, key)
            when (action) {
                "toggle" -> GlobalHotkeyManager.updateHotkeys(toggleHotkey = config, resetHotkey = null)
                "reset" -> GlobalHotkeyManager.updateHotkeys(toggleHotkey = null, resetHotkey = config)
            }
        }

        fun isRunningViaGradle(): Boolean {
            val gradleAppName = System.getProperty("org.gradle.appname")
            val javaCommand = System.getProperty("sun.java.command").orEmpty()
            return gradleAppName != null || javaCommand.contains("org.gradle", ignoreCase = true)
        }

        fun exitApp() {
          println("exitApp called - starting cleanup")
          try {
              GlobalHotkeyManager.stop()
              println("GlobalHotkeyManager stopped")
          } catch (e: Exception) {
              println("Error stopping GlobalHotkeyManager: ${e.message}")
              e.printStackTrace()
          }
          try {
              com.tbread.logging.UnifiedLogger.shutdown()
              println("UnifiedLogger shutdown")
          } catch (e: Exception) {
              println("Error shutting down UnifiedLogger: ${e.message}")
              e.printStackTrace()
          }
          println("Calling exitProcess(0)")
          System.err.flush()
          System.out.flush()
          exitProcess(0)
        }
    }

    @Volatile
    private var dpsData: DpsData = dpsCalculator.getDps()

    private val debugMode = false

    private val version = "1.0.0"


    override fun start(stage: Stage) {
        DebugLogWriter.loadFromSettings()

        // Auto-enable skill analysis logging for skill ID 13350000
        // SkillAnalysisLogger.enable(13350000)
        // logger.info("Skill analysis logger enabled for skill ID 13350000")

        stage.setOnCloseRequest {
            try {
                GlobalHotkeyManager.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                com.tbread.logging.UnifiedLogger.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            exitProcess(0)
        }
        val webView = WebView()
        val engine = webView.engine
        engine.load(javaClass.getResource("/index.html")?.toExternalForm())

        val bridge = JSBridge(stage, dpsCalculator, hostServices)
        engine.loadWorker.stateProperty().addListener { _, _, newState ->
            if (newState == Worker.State.SUCCEEDED) {
                val window = engine.executeScript("window") as JSObject
                window.setMember("javaBridge", bridge)
                window.setMember("dpsData", this)
            }
        }

        // Store engine reference for hotkey callbacks
        val webEngine = engine


        val scene = Scene(webView, 1600.0, 1000.0)
        scene.fill = Color.TRANSPARENT

        // Intercept ALL key events at Scene level when recording hotkey
        // This runs BEFORE WebView consumes Ctrl+key combinations
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED) { event ->
            if (isRecordingHotkey) {
                val keyName = event.code.name // e.g. KEY_L, KEY_R, F1 etc.
                val ctrlKey = event.isControlDown
                val shiftKey = event.isShiftDown
                val altKey = event.isAltDown

                Platform.runLater {
                    try {
                        webEngine.executeScript(
                            "hotkeyManager.onNativeKey('$keyName', $ctrlKey, $shiftKey, $altKey)"
                        )
                    } catch (e: Exception) {
                        logger.error("Failed to forward key event to JS", e)
                    }
                }

                event.consume() // Block WebView from processing
            }
        }

        try {
            val pageField = engine.javaClass.getDeclaredField("page")
            pageField.isAccessible = true
            val page = pageField.get(engine)

            val setBgMethod = page.javaClass.getMethod("setBackgroundColor", Int::class.javaPrimitiveType)
            setBgMethod.isAccessible = true
            setBgMethod.invoke(page, 0)
        } catch (e: Exception) {
            logger.error("Failed to set webview background via reflection", e)
        }

        stage.initStyle(StageStyle.TRANSPARENT)
        stage.scene = scene
        stage.isAlwaysOnTop = true
        stage.title = "Aion2 Dps Overlay"

        stage.show()

        // Start global hotkey manager
        GlobalHotkeyManager.start(
            onToggleCallback = {
                // Toggle window visibility
                Platform.runLater {
                    stage.isIconified = !stage.isIconified
                }
            },
            onResetCallback = {
                // Reset DPS calculator and refresh UI
                Platform.runLater {
                    try {
                        // Call JS resetAll function
                        webEngine.executeScript("if (window.dpsApp && window.dpsApp.resetAll) { window.dpsApp.resetAll({ callBackend: true }); }")
                        logger.info("DPS reset via global hotkey")
                    } catch (e: Exception) {
                        logger.error("Failed to reset via hotkey", e)
                    }
                }
            }
        )
        logger.info("Global hotkeys enabled")

        // เร็วขึ้นเป็น 100ms สำหรับ real-time updates
        Timeline(KeyFrame(Duration.millis(100.0), {
            dpsData = dpsCalculator.getDps()
        })).apply {
            cycleCount = Timeline.INDEFINITE
            play()
        }
    }

    fun getDpsData(): String {
        return Json.encodeToString(dpsData)
    }

    fun isDebuggingMode(): Boolean {
        return debugMode
    }

    fun getBattleDetail(uid:Int):String{
        val json = Json.encodeToString(dpsData.map[uid]?.analyzedData)

        // Debug: Print first 500 chars of JSON to check if specialtySlots is included
        // println("[getBattleDetail] JSON (first 500 chars): ${json.take(500)}")

        return json
    }

    fun getVersion():String{
        return version
    }

}
