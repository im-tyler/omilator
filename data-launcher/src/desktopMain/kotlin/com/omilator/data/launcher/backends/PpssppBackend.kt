package com.omilator.data.launcher.backends

import com.omilator.data.launcher.StandaloneBackend
import com.omilator.data.launcher.backends.MacosAppLauncher
import java.io.File

/**
 * PPSSPP (PSP) — `open -a PPSSPP <rom>` launches in foreground.
 * PPSSPP has no separate settings launch mode; user presses Esc in-game
 * to access its menu.
 */
internal class PpssppBackend : StandaloneBackend {
    override val systemId = "psp"
    override val displayName = "PPSSPP"

    override fun findInstallation(): String? {
        // brew --cask installs as PPSSPPSDL.app; the .org download is PPSSPP.app.
        return MacosAppLauncher.findApp("PPSSPPSDL.app")
            ?: MacosAppLauncher.findApp("PPSSPP.app")
    }

    override fun launch(romPath: String): Process? {
        val app = findInstallation() ?: return null
        val appName = File(app).name  // PPSSPPSDL.app or PPSSPP.app
        // PPSSPP treats the first positional argument as a ROM to launch.
        return MacosAppLauncher.launchOpenA(appName.removeSuffix(".app"), listOf(romPath))
    }
}
