package com.omilator.data.launcher.backends

import com.omilator.data.launcher.StandaloneBackend
import com.omilator.data.launcher.backends.MacosAppLauncher
import java.io.File

/**
 * Cemu (Wii U).
 * Cemu 2.0+ has native macOS arm64 support.
 *
 * Flag: -g <rom> launches a game directly.
 * Cemu's game profile editor is reachable by launching the GUI without a ROM.
 */
internal class CemuBackend : StandaloneBackend {
    override val systemId = "wii_u"
    override val displayName = "Cemu"

    override fun findInstallation(): String? {
        // brew --cask installs as CEmu.app (capital E); official builds may be Cemu.app
        return MacosAppLauncher.findApp("CEmu.app")
            ?: MacosAppLauncher.findApp("Cemu.app")
    }

    override fun launch(romPath: String): Process? {
        val app = findInstallation() ?: return null
        val appName = File(app).nameWithoutExtension
        // Cemu -g launches a game directly.
        return MacosAppLauncher.launchOpenA(appName, listOf("-g", romPath))
    }

    override fun openSettings(): Process? {
        // Launch the GUI without a ROM.
        val app = findInstallation() ?: return null
        val appName = File(app).nameWithoutExtension
        return MacosAppLauncher.launchOpenA(appName, emptyList())
    }
}
