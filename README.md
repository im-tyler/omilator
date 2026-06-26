# Omilator

A cross-platform libretro frontend, built with Kotlin Multiplatform + Compose Multiplatform.
Targets: macOS, Windows, Linux desktop + iOS, Android mobile.

## Architecture

Strict multi-module separation. UI is disposable per-platform; libretro glue is shared and stable.

```
core-libretro/    libretro C API bridge (JNI on JVM, cinterop on iOS/Native)
core-render/      platform video surfaces (MTKView / SurfaceView / GL)
core-audio/       platform audio output (AVAudioEngine / Oboe / cubeb)
core-input/       gamepad/keyboard -> libretro input state
data-library/     ROM scanner, metadata, system detection
data-settings/    per-game + global config (kotlinx.serialization)
data-saves/       save states, SRAM, battery saves
ui-shared/        ~80% shared Compose UI: game grid, player view, settings
app-desktop/      JVM entry point, Compose Desktop window
app-android/      Android Activity hosting Compose
iosApp/           Xcode project hosting Compose (TODO)
```

## Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Compose Multiplatform 1.7.0 (Material 3) |
| Build | Gradle 8.10.2 (wrapper bundled) |
| Async | kotlinx.coroutines 1.9.0 |
| Serialization | kotlinx.serialization 1.7.3 |
| Logging | Kermit 2.0.4 |
| Cores | libretro (vendored `libretro.h`, cinterop on iOS) |

## Build

Requires JDK 21 on PATH (or `org.gradle.java.home` set in `~/.gradle/gradle.properties`).

```sh
# Desktop (Mac/Windows/Linux) — runs in window
./gradlew :app-desktop:run

# Desktop distribution (.app / .exe / .deb)
./gradlew :app-desktop:packageDistributionForCurrentOS

# iOS framework (requires Xcode)
./gradlew :core-libretro:assembleCoreLibretroDebugFrameworkForIosSimulatorArm64

# Android (requires Android SDK at ANDROID_HOME)
./gradlew :app-android:assembleDebug
```

## Status

**Phase 0 — scaffold (complete).** Multi-module KMP project builds clean on desktop + iOS. All libretro interfaces defined in `commonMain`. Platform implementations are stubs returning sane defaults.

Next phases (see plan):
- Phase 1 — libretro bridge on desktop (load mGBA core, render first frame)
- Phase 2 — desktop playable (video + audio + keyboard input wired)
- Phase 3 — UI polish (real library grid, player view)
- Phase 4 — mobile bridge (iOS cinterop, Android JNI)
- Phase 5 — save states, per-game overrides, box art, sync

## Reference

`_reference/` (gitignored) holds study-only clones of:
- `libretro/nanoarch` — minimal C frontend, ~1k LOC, used as translation reference
- `libretro/ludo` — complete Go frontend by the libretro team, architecture reference
