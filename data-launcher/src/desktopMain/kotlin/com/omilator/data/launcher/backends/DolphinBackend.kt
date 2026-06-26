package com.omilator.data.launcher.backends

import com.omilator.data.launcher.StandaloneBackend
import com.omilator.data.launcher.backends.MacosAppLauncher.findApp
import com.omilator.data.launcher.backends.MacosAppLauncher.launchOpenA

/**
 * Dolphin (GameCube + Wii).
 *   -b            batch mode (skip GUI)
 *   -e <rom>      execute the game immediately
 *   -c            open configuration dialog before launch (used by openSettings)
 */
internal class DolphinBackend : StandaloneBackend {
    override val systemId = "gamecube_wii"  // matches both GameCube and Wii
    override val displayName = "Dolphin"

    override fun findInstallation(): String? = findApp("Dolphin.app")

    override fun launch(romPath: String): Process? {
        // Batch mode + immediate execute; goes straight into the game.
        return launchOpenA("Dolphin", listOf("-b", "-e", romPath))
    }

    override fun openSettings(): Process? {
        // -c opens Dolphin's main config dialog without launching a game.
        return launchOpenA("Dolphin", listOf("-c"))
    }
}
