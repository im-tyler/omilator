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
        return MacosAppLauncher.findApp("PPSSPPSDL.app")
            ?: MacosAppLauncher.findApp("PPSSPP.app")
    }

    override fun launch(romPath: String): Process? {
        val app = findInstallation() ?: return null
        val appName = File(app).name.removeSuffix(".app")
        return MacosAppLauncher.launchOpenA(appName, listOf(romPath))
    }

    /** PPSSPP has no settings-only flag — launch GUI without ROM. */
    override fun openSettingsGuiOnly(): Process? {
        val app = findInstallation() ?: return null
        val appName = File(app).name.removeSuffix(".app")
        // Launch with no args — PPSSPP opens its main menu
        return MacosAppLauncher.launchOpenA(appName, emptyList())
    }
}
