package com.omilator.data.launcher.backends

import com.omilator.data.launcher.StandaloneBackend
import com.omilator.data.launcher.backends.MacosAppLauncher.findApp
import com.omilator.data.launcher.backends.MacosAppLauncher.launchBinary

/**
 * RPCS3 (PlayStation 3).
 * RPCS3 doesn't accept --args via `open -a` cleanly for headless mode;
 * launching the binary directly is more reliable.
 *
 * Flag: --no-gui skips the GUI and boots the game directly.
 */
internal class Rpcs3Backend : StandaloneBackend {
    override val systemId = "ps3"
    override val displayName = "RPCS3"

    override fun findInstallation(): String? = findApp("RPCS3.app")

    override fun launch(romPath: String): Process? {
        val app = findInstallation() ?: return null
        return launchBinary(app, "Contents/MacOS/RPCS3", listOf("--no-gui", romPath))
    }
}
