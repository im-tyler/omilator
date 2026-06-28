# Next Steps — Priority Roadmap

# Next Steps — Priority Roadmap

## DONE in v0.6 (2026-06-27) — Android parity + Linux/Win CI smoke

### Phase 11: Android parity (code complete)
- **`MobilePlayerScreen` extracted to `ui-shared/commonMain`** — shared
  between iOS and Android. Touch controls + frame rendering + multi-touch
  handling all live in one file now.
- **`AndroidAudioOutput`** via `AudioTrack` (USAGE_GAME, MODE_STREAM,
  PERFORMANCE_MODE_LOW_LATENCY). AudioTrack.write blocks naturally —
  no need for the iOS MAX_PENDING backpressure cap.
- **`AndroidCoreDownloader`** — direct `.so` download from buildbot's
  `android/arm64-v8a` and `android/x86_64` paths. No vtool/codesign.
- **`setup-cores-android.sh`** — bundles cores as `lib<core>_libretro.so`
  into `app-android/src/androidMain/jniLibs/<abi>/`. Auto-picked-up by
  Gradle, loaded at runtime via `System.loadLibrary`.
- **`MainActivity` fully wired** — SAF directory picker
  (`ActivityResultContracts.OpenDocumentTree`), SettingsViewModel
  persistence, onDownloadCores callback, player launch by ROM extension,
  bundled-or-downloaded core path resolution.
- **`createImageBitmapFromArgb` expect/actual** — Skia path on iOS/desktop,
  android.graphics.Bitmap path on Android (Skia isn't directly accessible
  on Android's Compose backend).
- **APK builds** — `app-android-debug.apk` (~17MB).
- **Runtime test blocked**: Android emulator on macOS is flaky in
  headless mode (qemu crashes during snapshot save without an active
  GUI session). Requires manual device test or Android Studio.

### Phase 12: Win/Linux CI smoke (workflow updated)
- `.github/workflows/build.yml` now runs an actual app-launch smoke test
  on Linux (via `xvfb-run`) and Windows (via Start-Process + 10s liveness
  check). Catches runtime regressions, not just compile errors.
- Linux step installs X11 + GLFW native deps before build.
- macOS still ships via `release-dmg.sh` (ProGuard-free DMG path).

---

## DONE in v0.5 (2026-06-27)

### Phase 7: Vulkan HW Render — scaffolding complete
- **MoltenVK vendored**: `./setup-moltenvk.sh` downloads MoltenVK.xcframework
  + Vulkan headers (~80MB) from KhronosGroup release.
- **libretro.h updated**: `RETRO_HW_CONTEXT_VULKAN = 6` + D3D enums.
- **Vulkan cinterop**: `core-libretro/src/nativeInterop/cinterop/vulkan.def`
  brings in Vk* types.
- **SET_HW_RENDER handler** in `NativeCoreController.handleSetHwRender()`:
  accepts cmd 14, fills get_proc_address (dlsym MoltenVK) +
  get_current_framebuffer (placeholder).
- **VulkanHwRender.kt** scaffold: dlopens MoltenVK, exposes getProcAddress.
- **MoltenVK linked** in iosApp/project.yml with build-phase codesign (the
  iOS Simulator rejects MoltenVK at dyld without re-signing, even with
  CODE_SIGNING_ALLOWED=NO).
- **HW-render cores** added to IosCoreDownloader (ppsspp/dolphin/flycast).
- **What's left**: actual VkInstance + VkSurfaceKHR (CAMetalLayer-backed)
  + VkSwapchainKHR + VkQueuePresent. ~300-500 LOC of careful threading
  + Vulkan interop. See VulkanHwRender.kt TODOs.

### Phase 8: Bundled-Core Distribution — developer-signing path
- **`./setup-cores.sh --sim|--device`** downloads + converts + signs cores
  into `iosApp/Frameworks/`.
- **project.yml postBuildScript** copies them into the .app's Frameworks/
  and re-signs each.
- **NativeCoreController.loadCore** tries Documents/cores/ first, falls
  back to `<Bundle>/Frameworks/` for bundled copies.
- **App Store path still requires static linking** — build cores as .a
  files from source (multi-day effort, see Phase 8.4 below).

### Phase 10: CI matrix
- **`.github/workflows/build.yml`** — Windows/Linux/macOS desktop builds
  + macOS DMG artifact + iOS framework link verification + libretro FFM
  smoke test.
- Linux step installs X11 / GLFW native deps.
- The release DMG path uses `./app-desktop/release-dmg.sh` (skips ProGuard).

---

## DONE in v0.4 (2026-06-27)

- **Phase 6A: iOS Audio** — FIXED AND VERIFIED END-TO-END. AVAudioSession.playback
  activated before AVAudioEngine.start, samples scheduled via
  AVAudioPCMBuffer.scheduleBuffer with Float32 non-interleaved + 8-buffer
  backpressure cap. The silent bug was a missing audio session, not K/N
  interop as originally suspected. Verified via deep-link test:
  `[Audio] Started: 32768.0Hz 2ch -> HW 44100.0Hz` and first buffer scheduled.
- **Phase 6B: Touch input polish** — FIXED. Per-pointer tracking via
  awaitEachGesture (replaces tryAwaitRelease which broke multi-touch).
  D-pad is now one touch surface with offset-based direction + 10dp deadzone.
  graphicsLayer + animateFloatAsState for smooth 60ms press feedback.
- **Phase 6C: Core auto-download** — DONE (simulator-only). IosCoreDownloader
  runs curl + unzip + vtool + codesign via popen. Wired into Settings UI.
  PSP/GC/Wii cores omitted (need Vulkan HW render first). Fixed URL/name
  bugs (`beetle_psx_hw` → `mednafen_psx_hw`, inconsistent `_ios` suffix).
- **Phase 9: Persistent library** — DONE. IosSettingsPersistence writes
  `<Documents>/settings.json` via POSIX fopen/fread/fwrite. Plumbs through
  existing SettingsStore/JSON encoder.

### Test infrastructure added (2026-06-27)
- `omilator://play/<url-encoded-absolute-path>` URL scheme for headless testing.
  Use: `xcrun simctl openurl booted omilator://play/$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1],safe=''))" "<abs path>")`
- `logI(tag, message)` Kotlin helper that routes through NSLog (visible in
  `xcrun simctl log show`). Plain `println()` isn't reliably captured.
- Polling-based deep-link pickup (every 100ms via LaunchedEffect) because
  StateFlow + collectAsState wasn't triggering recomposition from Swift's
  onOpenURL callback.

---

## Phase 7: Vulkan HW Render via MoltenVK — IMPLEMENTATION REMAINING
**Goal:** Unlock PSP, GameCube, Wii, Dreamcast, N64-HW on iOS

v0.5 scaffolding is in place (see DONE above). Remaining work to actually
render PSP/GameCube frames:

### Phase 7.6 — VkInstance + VkSurface + VkSwapchain plumbing (THE BIG PIECE)
File: `core-libretro/src/iosMain/.../VulkanHwRender.kt`

**Critical finding (2026-06-27) from real PSP test:** The iOS Simulator's
Metal proxy does NOT support MoltenVK for non-trivial use cases. Tested
with `MotorStorm - Arctic Edge.iso` (830MB PSP game) + bundled ppsspp core:
ppsspp's `retro_init` fires many env queries (cmd 11/27/35/74 etc.),
proceeds to set up Vulkan via MoltenVK, then the process is silently
killed when MoltenVK tries `com.apple.metal.simulator.Omilator` XPC
lookup. Simulator log shows:
```
failed lookup: name = com.apple.metal.simulator.Omilator, ... error = 3: No such process
```
mGBA + Pokemon Yellow still works (software render, no MoltenVK), proving
the regression isn't from our changes. Conclusion: **HW render via
MoltenVK requires a real iPhone**. The simulator's Metal implementation
is sufficient for basic CAMetalLayer drawing but not for Vulkan's complex
compute + render pipelines.

So 7.6 development + testing must happen on-device. The plumbing (MoltenVK
linked, vulkan.h cinterop, SET_HW_RENDER handler, VulkanHwRender scaffold)
is in place — only the actual VkInstance/VkSurface/VkSwapchain calls
remain, but iterating requires hardware.

1. **`prepare()`** currently just dlopens MoltenVK. Extend to:
   - Create `MTLCreateSystemDefaultDevice()` (already in code, commented out)
   - Create `CAMetalLayer` and assign its `.device` + `.pixelFormat = .bgra8Unorm`
   - Hold a strong reference to both for app lifetime.
2. **New `createVulkanInstance()`**:
   - Use `vkCreateInstance` (loaded via dlsym from MoltenVK) with
     `VK_EXT_metal_surface` + `VK_KHR_portability_enumeration` enabled
   - Pass `VkInstanceCreateInfo.pApplicationInfo` with engine = "Omilator"
3. **`createVulkanSurface(CAMetalLayer)`**:
   - Use `vkCreateMetalSurfaceEXT(instance, &metalCreateInfo, NULL, &surface)`
   - This is the bridge between Vulkan and the CAMetalLayer.
4. **`createSwapchain()`**:
   - Pick a `VkPhysicalDevice` that supports graphics + present to our surface
   - Create `VkDevice` with one graphics queue + one present queue
   - Create `VkSwapchainKHR` with `VK_FORMAT_B8G8R8A8_UNORM` + `VK_COLOR_SPACE_SRGB_NONLINEAR_KHR`
   - Acquire the swapchain's `VkImage[]` handles
5. **`currentFramebuffer()`** — return the currently-acquired `VkImage` handle
   (currently returns 0; this is what `get_current_framebuffer` feeds back
   to the core).
6. **`presentFrame()`** — `vkQueuePresentKHR(presentQueue, &presentInfo)`
   (currently a no-op).

### 7.7 — Compose UI integration
The `CAMetalLayer` needs to be inserted into the UIKit hierarchy so the
rendered frames appear on screen. Options:
- `UIViewRepresentable` that returns a `UIView` whose `.layer` is the
  `CAMetalLayer`. Mount it inside `IosPlayerScreen` above (or instead of)
  the current `Canvas`-based software renderer.
- Or use `CAMetalLayer.delegates` to call back into Compose when a new
  drawable is ready, then trigger recomposition.

### 7.8 — Threading
Vulkan command submission must happen on the same thread that called
`retro_run` (the @ThreadLocal NativeCoreController). The CAMetalLayer
must be owned by the main thread. Add a `dispatch_async(main, ...)` hop
around layer.present(drawable).

### Estimated effort
~300-500 LOC. Plus iterative testing with ppsspp + a real PSP ISO
(multi-day effort because each Vulkan call has many failure modes).

## Phase 8: Static Core Bundling (APP STORE)
**Goal:** No dlopen — cores compiled into app binary

For App Store distribution, cores must be statically linked:
1. Compile each core as a .a static library for iosArm64
2. Link into the UiShared.framework via cinterop or direct linking
3. Call retro_* functions directly (no dlsym)

This is what Delta does. It's the only path to App Store.

### Phase 8.4 — App Store path documentation
The developer-signing path (Phase 8.1-8.3) covers Personal / Ad Hoc /
TestFlight. App Store submission requires Apple's review process and
statically-linked cores — that's a separate multi-day build effort:

1. **Build cores from source** — clone each libretro core repo, build for
   `iphoneos-arm64` with `make -f Makefile.libretro platform=ios` static
   target. Output: `libmgba_libretro_ios.a` etc.
2. **Bundle into the app** as static libraries. Use cinterop to expose
   the `retro_*` symbols and call them directly (no dlopen).
3. **Refactor `NativeCoreController.loadCore`** to dispatch by core name
   to inline static entry points instead of dlopen.
4. **Submit for review** — Apple has been historically inconsistent about
   emulator apps; the litmus test is whether the app ships any ROMs or
   "looks like" it's encouraging piracy. We ship no ROMs (user-supplied),
   so the path is open but slow.

Alternative: ship on AltStore / SideStore which sidesteps the App Store.

## Phase 9: Persistent Library on iOS (DONE in v0.4)
Removed — see DONE section above.

## Phase 10: Desktop Polish (partially done in v0.4.2)
- ✅ **DMG distribution** — `./app-desktop/release-dmg.sh` produces
  `Omilator-1.0.0.dmg` (113MB). Uses debug build (no ProGuard) + hdiutil
  because Compose Desktop's release ProGuard config chokes on LWJGL's
  spurious `javax.annotation.Nullable` references.
- ✅ **TheGamesDB API key persistence** — `SettingsViewModel` now writes
  through to `SettingsStore`. Theme + API key + libraryDirectories all
  survive restarts on both desktop and iOS.
- Windows/Linux testing — HW render should work there (no Cocoa
  restriction). Needs a CI matrix or manual VM test.
- macOS code signing — requires Apple Developer ID ($99/yr). For now the
  unsigned DMG works on the developer machine; Gatekeeper blocks it
  elsewhere without `xattr -cr`.

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
