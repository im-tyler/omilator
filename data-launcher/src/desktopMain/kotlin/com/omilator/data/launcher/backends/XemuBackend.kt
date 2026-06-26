package com.omilator.data.launcher.backends

import com.omilator.data.launcher.StandaloneBackend
import com.omilator.data.launcher.backends.MacosAppLauncher.findApp
import com.omilator.data.launcher.backends.MacosAppLauncher.launchBinary

/**
 * xemu (Original Xbox).
 * xemu uses QEMU-style flags. Launching the binary directly works reliably.
 *
 * Flags:
 *   -full-screen     fullscreen mode
 *   -cdrom <rom>     load the ISO as a CD-ROM
 *   -dvd_path <rom>  alternate flag name in some builds
 */
internal class XemuBackend : StandaloneBackend {
    override val systemId = "xbox"
    override val displayName = "xemu"

    override fun findInstallation(): String? = findApp("xemu.app")

    override fun launch(romPath: String): Process? {
        val app = findInstallation() ?: return null
        return launchBinary(app, "Contents/MacOS/xemu", listOf("-full-screen", "-cdrom", romPath))
    }
}
