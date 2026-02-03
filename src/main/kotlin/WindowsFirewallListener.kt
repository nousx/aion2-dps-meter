package com.tbread

import java.net.InetAddress
import java.net.ServerSocket

object WindowsFirewallListener {
    private var serverSocket: ServerSocket? = null

    fun openOnStartup() {
        if (!isWindows()) {
            return
        }

        if (serverSocket != null) {
            return
        }

        serverSocket = try {
            ServerSocket(0, 50, InetAddress.getByName("0.0.0.0")).also { socket ->
                Runtime.getRuntime().addShutdownHook(Thread { socket.close() })
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name")?.lowercase()?.contains("windows") == true
}
