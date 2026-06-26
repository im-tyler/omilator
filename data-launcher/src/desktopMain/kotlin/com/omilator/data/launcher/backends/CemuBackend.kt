package com.omilator.data.launcher.backends

import com.omilator.data.launcher.StandaloneBackend
import com.omilator.data.launcher.backends.MacosAppLauncher.findApp
import com.omilator.data.launcher.backends.MacosAppLauncher.launchOpenA

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

    override fun findInstallation(): String? = findApp("Cemu.app")

    override fun launch(romPath: String): Process? {
        return launchOpenA("Cemu", listOf("-g", romPath))
    }

    override fun openSettings(): Process? {
        // Launch the GUI without a ROM — user can edit game profiles there.
        return launchOpenA("Cemu", emptyList())
    }
}
