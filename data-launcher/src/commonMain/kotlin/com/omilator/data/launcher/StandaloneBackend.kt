package com.omilator.data.launcher

/**
 * Backend for launching a ROM via a standalone emulator app
 * (PPSSPP, Dolphin, RPCS3, Cemu, xemu, etc.) rather than via libretro.
 *
 * Used for systems where libretro cores don't exist (PS3, Wii U, Xbox)
 * or where the libretro path is blocked on macOS (PPSSPP/Dolphin HW render).
 *
 * Implementations are platform-specific — macOS uses NSApp via `open -a`,
 * Linux uses flatpak/binary paths, Windows uses .exe paths.
 */
interface StandaloneBackend {

    /** Stable identifier (matches GameSystem name in lowercase). */
    val systemId: String

    /** Human-readable name shown in UI ("PPSSPP", "Dolphin", etc.). */
    val displayName: String

    /**
     * Returns the executable path if the standalone app is installed on
     * this system, null otherwise. Used to enable/disable UI affordances.
     */
    fun findInstallation(): String?

    /** True if [findInstallation] returns a non-null path. */
    fun isInstalled(): Boolean = findInstallation() != null

    /**
     * Launch the ROM in the standalone. The standalone's window appears
     * separately from Omilator's window — macOS prevents cross-process
     * window embedding (see process isolation in WindowServer).
     *
     * @param romPath Absolute path to the ROM file.
     * @return The launched [Process], or null if launch failed.
     */
    fun launch(romPath: String): Process?

    /**
     * Open the standalone's settings/config UI without launching a game.
     * Default implementation returns null (not supported).
     *
     * Each emulator handles this differently:
     *   - Dolphin: `-c` flag opens config dialog
     *   - Cemu: game profile editor exists
     *   - PPSSPP / RPCS3 / xemu: must launch GUI, no clean settings flag
     *
     * Implementations should override if a clean config-mode launch exists.
     */
    fun openSettings(): Process? = null

    /**
     * Launches the standalone GUI WITHOUT a ROM — used as fallback when
     * [openSettings] returns null. Lets the user access the emulator's
     * own settings menu without launching the game.
     */
    fun openSettingsGuiOnly(): Process? = null
}

/**
 * Registry of all known standalone backends on this platform.
 * Call [availableBackends] to get only the installed ones.
 */
expect class StandaloneRegistry() {
    val all: List<StandaloneBackend>
    fun available(): List<StandaloneBackend>
    fun forSystem(systemId: String): StandaloneBackend?
}
