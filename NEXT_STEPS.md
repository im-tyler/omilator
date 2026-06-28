# Next Steps — Priority Roadmap

## Phase 6A: iOS Audio (HIGH PRIORITY)
**Goal:** Actual sound playback on iOS

AVAudioEngine is initialized and connected, but PCM buffer scheduling is deferred.
Need to create proper AVAudioPCMBuffer objects from the ShortArray samples and
schedule them via playerNode.scheduleBuffer().

Challenge: AVAudioPCMBuffer requires platform.AVFAudio interop which has complex
Kotlin/Native type mapping. The buffer's int16ChannelData needs proper pointer
handling.

Files to modify:
- core-audio/src/iosMain/.../IosAudioOutput.kt — implement write() with PCM buffer

## Phase 6B: iOS Touch Input Polish (HIGH PRIORITY)
**Goal:** Responsive touch controls

Current state: buttonStates map works, staticCFunction reads from it.
Issues:
- Multi-touch not fully reliable (pointer ID tracking needs verification)
- No visual feedback on press (scale animation exists but may not render)
- D-pad center deadzone needs tuning
- Need real ROMs to test actual gameplay (Pokemon Yellow intro)

Files to modify:
- ui-shared/src/iosMain/.../IosPlayerScreen.kt — ControllerBar composable

## Phase 6C: iOS Core Auto-Download (MEDIUM)
**Goal:** Download + convert + sign cores automatically on iOS

Mirror the desktop CoreDownloader but:
1. Download from apple/ios-arm64 buildbot (not osx/arm64)
2. Run vtool conversion (platform → iossim)
3. Run codesign (ad-hoc)
4. Save to Documents/cores/

Challenge: vtool and codesign are macOS CLI tools. On a real iPhone, this can't
work. On simulator it could work via ProcessBuilder if the simulator process
can access host macOS tools.

For real device: cores must be pre-converted and bundled.

## Phase 7: Metal HW Render (GAME CHANGER)
**Goal:** Unlock PSP, GameCube, Wii on iOS via libretro

Implement RETRO_HW_CONTEXT_METAL in the NativeCoreController bridge:

1. Create MTLDevice via platform.Metal.MTLCreateSystemDefaultDevice()
2. Create CAMetalLayer via platform.QuartzCore
3. Create MTLTexture (render target for the core)
4. Handle SET_HW_RENDER (cmd 14):
   - Read retro_hw_render_callback struct
   - Fill get_current_framebuffer (returns texture ID)
   - Fill get_proc_address (Metal function lookup)
   - Call core's context_reset
5. Per frame: core renders to texture → present drawable → display in Compose

This is ~500 LOC of platform.Metal.* + platform.QuartzCore.* interop.
PPSSPP libretro core already supports Metal on iOS — it just needs the frontend
to provide the Metal device.

Files to create:
- core-libretro/src/iosMain/.../MetalHwRender.kt
- Modify NativeCoreController.kt handleEnvironment() for SET_HW_RENDER

## Phase 8: Static Core Bundling (APP STORE)
**Goal:** No dlopen — cores compiled into app binary

For App Store distribution, cores must be statically linked:
1. Compile each core as a .a static library for iosArm64
2. Link into the UiShared.framework via cinterop or direct linking
3. Call retro_* functions directly (no dlsym)

This is what Delta does. It's the only path to App Store.

## Phase 9: Persistent Library on iOS (MEDIUM)
**Goal:** Survive app restarts with scanned games

Currently: LibraryViewModel rescans Documents on launch via CoroutineScope.
Issue: ViewModels survive player/library switches (fixed), but app restart
recreates everything.

Solution: Persist scannedDirectories to a settings file in Documents.
Load on startup. Same pattern as desktop SettingsStore.

## Phase 10: Desktop Polish
- Windows/Linux testing (HW render should work there — no Cocoa restriction)
- DMG distribution for macOS
- Code signing for distribution outside developer machine
- Cover art via TheGamesDB API (free key from thegamesdb.net)

## Key Technical References
- nanoarch (~1k LOC C): _reference/nanoarch/nanoarch.c — minimal libretro frontend
- Ludo (Go): _reference/ludo/ — complete frontend, study video.go/audio.go/core.go
- libretro.h: core-libretro/src/nativeInterop/cinterop/libretro.h
- libretro_vulkan.h: download from RetroArch repo for Vulkan HW render spec

## Cover Art Sources That Work
- libretro-thumbnails CDN: has non-Nintendo games (GitHub raw URLs)
- ScreenScraper API: needs dev credentials (free at screenscraper.fr)
- TheGamesDB API: needs API key (free at thegamesdb.net)
- Local files: ROM-name.png alongside ROM (works everywhere)
- Delta bundles its own database (not applicable for us)
