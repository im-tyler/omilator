package com.omilator.data.launcher.backends

import java.io.File

/**
 * Common macOS app-launch helpers. Standalones ship as .app bundles;
 * the canonical launch path is `open -a AppName --args ...` which goes
 * through LaunchServices and properly activates the new window.
 */
internal object MacosAppLauncher {

    /**
     * Search common install locations for an .app bundle by name.
     * Returns the bundle path (e.g. "/Applications/PPSSPP.app") if found.
     */
    fun findApp(appName: String): String? {
        val candidates = listOf(
            "/Applications/$appName",
            "${System.getProperty("user.home")}/Applications/$appName",
        )
        return candidates.firstOrNull { File(it).isDirectory }
    }

    /**
     * Launch the app via `open -a`, passing [args] to the app via --args.
     * Returns the spawned [Process] (the `open` command itself — short-lived),
     * or null if launch failed.
     */
    fun launchOpenA(appBundleName: String, args: List<String>): Process? {
        val cmd = mutableListOf("open", "-a", appBundleName)
        if (args.isNotEmpty()) {
            cmd += "--args"
            cmd += args
        }
        return runCatching {
            ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
        }.getOrNull()
    }

    /**
     * Launch the binary inside the .app bundle directly (bypasses LaunchServices).
     * Some emulators (RPCS3, xemu) handle this better than `open -a --args`.
     * [binaryRelativePath] is e.g. "Contents/MacOS/RPCS3".
     */
    fun launchBinary(appPath: String, binaryRelativePath: String, args: List<String>): Process? {
        val binary = File(appPath, binaryRelativePath)
        if (!binary.canExecute()) return null
        val cmd = mutableListOf(binary.absolutePath) + args
        return runCatching {
            ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .directory(File(appPath))  // .app dir contains the bundled frameworks
                .start()
        }.getOrNull()
    }
}
