# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BW_clock_phone is an Android phone clock application ported from the BW_clock TV version. The app displays a black-and-white analog clock in portrait fullscreen, intended for always-on display (e.g., repurposing an old phone as a desk/bedside clock). It also provides a home-screen widget version of the same clock.

Settings are opened by double-tapping the clock; ◀/▶ buttons change each value, tabs switch between 本体 (app) and ウィジェット (widget) scopes, and tapping the dimmed background dismisses the panel.

## Target Platform

- Android phone (portrait) + home-screen widget
- Minimum SDK: 21, Target SDK: 36
- `applicationId`: `com.example.bw_clock_phone`
- Jetpack Compose + Material3 + Jetpack Glance (no Leanback/TV dependencies)

## Build

```
./gradlew assembleRelease
```

Release APK and baseline profiles are checked into `app/release/`.

## Architecture

- `MainActivity.kt` — fullscreen host. Collects `appSettingsFlow` and `widgetSettingsFlow`; applies `appSettings.rotation` via `graphicsLayer`; opens settings on double-tap; observes `widgetSettingsFlow` and pushes immediate `GlanceAppWidget.update()` so the home-screen widget reflects changes without waiting for the next minute tick.
- `ClockScreen.kt` — thin Compose wrapper that hosts a `Canvas` and delegates to `drawClock()` with the app's foreground/background colors, burn-in offset, and brightness dim.
- `ClockRenderer.kt` — `drawClock(canvas, width, height, density, settings, …)` is a pure `android.graphics.Canvas` drawing function shared by the app and the widget. The `radiusPadding` parameter controls outer margin (0.1 for app, 0.0 for widget so it fills the bitmap).
- `ClockWidget.kt` — `GlanceAppWidget` with `SizeMode.Exact`. Renders a **square** `Bitmap` sized to the shorter widget side (prevents clipping on non-square cells), then shows it via `Image(ImageProvider(bitmap))`. Forces `showSecondHand=false`, `burnInPrevention=false`, `brightnessPercent=100`, `rotation=0` regardless of stored widget settings.
- `ClockWidgetReceiver.kt` — `GlanceAppWidgetReceiver` that schedules a minute-aligned `AlarmManager.setRepeating` alarm via a custom `MINUTE_TICK` action (manifest-declared `ACTION_TIME_TICK` no longer works on modern Android).
- `SettingsScreen.kt` — tabbed settings panel (本体 / ウィジェット). Each tab shows only the items relevant to its scope.
- `SettingsState.kt` — `ClockSettings` data class + `SettingsRepository`. Settings are persisted in a single DataStore under two namespaces (`app_*` and `widget_*`). Legacy unprefixed keys are read as a fallback for pre-1.1 installs.
- `ui/theme/` — `ClockColors` (B&W) and Material3 color scheme.

## Settings scopes

Same `ClockSettings` data class is used for both scopes, but the widget ignores a few fields at render time:

| Setting | App | Widget |
|---|:---:|:---:|
| brightness, second hand, burn-in prevention, rotation | ✓ | ignored |
| theme, font, size%, frame, date*, tick scales, number scale, offsets | ✓ | ✓ |

`resetToDefaults(scope)` writes explicit defaults into the scope keys (rather than removing them) so the legacy fallback cannot re-surface old pre-1.1 values.

## Notes

- The TV-version key handling (`onKeyEvent`, `FocusRequester`, `focusable`) was removed in the phone port. Do not reintroduce Leanback or `androidx.tv.*` dependencies.
- `AndroidManifest.xml` is locked to `screenOrientation="portrait"`; in-app rotation uses Compose `graphicsLayer` instead of activity rotation.
- Tick dot size is controlled by `majorTickScale` / `minorTickScale` (0–500%), and number size by `numberScale` (0–500%). 0% means hidden. There is no separate ON/OFF toggle for ticks or numbers.
- The widget cannot update more often than once per minute, so the app's second-hand cannot be mirrored there.
