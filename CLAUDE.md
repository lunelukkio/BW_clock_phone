# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BW_clock_phone is an Android phone clock application ported from the BW_clock TV version. The app displays a black-and-white analog clock in portrait fullscreen, intended for always-on display (e.g., repurposing an old phone as a desk/bedside clock).

Settings are opened by tapping the clock; ◀/▶ buttons in the settings panel change each value, and tapping the dimmed background dismisses it.

## Target Platform

- Android phone (portrait)
- Minimum SDK: 21, Target SDK: 36
- `applicationId`: `com.example.bw_clock_phone`
- Jetpack Compose + Material3 (no Leanback/TV dependencies)

## Build

```
./gradlew assembleRelease
```

Release APK and baseline profiles are checked into `app/release/`.

## Architecture

- `MainActivity.kt` — sets fullscreen/keep-screen-on, hosts `ClockScreen` + optional `SettingsScreen`, applies `settings.rotation` via `graphicsLayer`, toggles settings via `clickable`.
- `ClockScreen.kt` — Canvas-based analog clock. Supports frame / tick marks / numbers / second hand / date, burn-in shift, brightness dim overlay, and per-element XY offsets (`clockOffsetX/Y`, `dateOffsetX/Y`, percent of screen).
- `SettingsScreen.kt` — scrollable settings panel on the right edge, dismissed by tapping the scrim.
- `SettingsState.kt` — `ClockSettings` data class + `SettingsRepository` backed by DataStore Preferences.
- `ui/theme/` — `ClockColors` (B&W) and Material3 color scheme.

## Notes

- The TV-version key handling (`onKeyEvent`, `FocusRequester`, `focusable`) was removed in the phone port. Do not reintroduce Leanback or `androidx.tv.*` dependencies.
- `AndroidManifest.xml` is locked to `screenOrientation="portrait"`; in-app rotation uses Compose `graphicsLayer` instead of activity rotation.
- Date layout branches on portrait vs. landscape within the Canvas (`ClockScreen.kt:229`), since phone screens are typically portrait but users can rotate the clock 90°/270°.
