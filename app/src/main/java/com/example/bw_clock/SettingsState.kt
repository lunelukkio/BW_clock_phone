package com.example.bw_clock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ClockFont(val label: String) {
    DEFAULT("デフォルト"),
    SERIF("セリフ"),
    MONOSPACE("等幅"),
    SANS_SERIF_LIGHT("細字"),
    SANS_SERIF_BOLD("太字");
}

enum class DatePosition(val label: String) {
    LEFT("左"),
    RIGHT("右");
}

enum class SettingsScope(val prefix: String) {
    APP("app_"),
    WIDGET("widget_")
}

data class ClockSettings(
    val isDarkBackground: Boolean = true,
    val showSecondHand: Boolean = true,
    val showFrame: Boolean = true,
    val brightnessPercent: Int = 100,
    val clockSizePercent: Int = 100,
    val clockFont: ClockFont = ClockFont.DEFAULT,
    val rotation: Int = 0,
    val burnInPrevention: Boolean = true,
    val showDate: Boolean = false,
    val datePosition: DatePosition = DatePosition.LEFT,
    val dateSizePercent: Int = 100,
    val clockOffsetX: Int = 0,
    val clockOffsetY: Int = 0,
    val dateOffsetX: Int = 0,
    val dateOffsetY: Int = 0,
    val majorTickScale: Int = 100,
    val minorTickScale: Int = 100,
    val numberScale: Int = 100
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clock_settings")

class ScopedKeys(scope: SettingsScope) {
    private val p = scope.prefix
    val IS_DARK_BACKGROUND = booleanPreferencesKey("${p}is_dark_background")
    val SHOW_SECOND_HAND = booleanPreferencesKey("${p}show_second_hand")
    val SHOW_FRAME = booleanPreferencesKey("${p}show_frame")
    val BRIGHTNESS_PERCENT = intPreferencesKey("${p}brightness_percent")
    val CLOCK_SIZE_PERCENT = intPreferencesKey("${p}clock_size_percent")
    val CLOCK_FONT = intPreferencesKey("${p}clock_font")
    val ROTATION = intPreferencesKey("${p}rotation")
    val BURN_IN_PREVENTION = booleanPreferencesKey("${p}burn_in_prevention")
    val SHOW_DATE = booleanPreferencesKey("${p}show_date")
    val DATE_POSITION = intPreferencesKey("${p}date_position")
    val DATE_SIZE_PERCENT = intPreferencesKey("${p}date_size_percent")
    val CLOCK_OFFSET_X = intPreferencesKey("${p}clock_offset_x")
    val CLOCK_OFFSET_Y = intPreferencesKey("${p}clock_offset_y")
    val DATE_OFFSET_X = intPreferencesKey("${p}date_offset_x")
    val DATE_OFFSET_Y = intPreferencesKey("${p}date_offset_y")
    val MAJOR_TICK_SCALE = intPreferencesKey("${p}major_tick_scale")
    val MINOR_TICK_SCALE = intPreferencesKey("${p}minor_tick_scale")
    val NUMBER_SCALE = intPreferencesKey("${p}number_scale")

    val all: List<Preferences.Key<*>> = listOf(
        IS_DARK_BACKGROUND, SHOW_SECOND_HAND, SHOW_FRAME, BRIGHTNESS_PERCENT,
        CLOCK_SIZE_PERCENT, CLOCK_FONT, ROTATION, BURN_IN_PREVENTION,
        SHOW_DATE, DATE_POSITION, DATE_SIZE_PERCENT,
        CLOCK_OFFSET_X, CLOCK_OFFSET_Y, DATE_OFFSET_X, DATE_OFFSET_Y,
        MAJOR_TICK_SCALE, MINOR_TICK_SCALE, NUMBER_SCALE
    )
}

// Legacy unprefixed keys — fallback source for the first read after upgrade,
// so existing user settings are inherited into both scopes until they diverge.
private object LegacyKeys {
    val IS_DARK_BACKGROUND = booleanPreferencesKey("is_dark_background")
    val SHOW_SECOND_HAND = booleanPreferencesKey("show_second_hand")
    val SHOW_FRAME = booleanPreferencesKey("show_frame")
    val BRIGHTNESS_PERCENT = intPreferencesKey("brightness_percent")
    val CLOCK_SIZE_PERCENT = intPreferencesKey("clock_size_percent")
    val CLOCK_FONT = intPreferencesKey("clock_font")
    val ROTATION = intPreferencesKey("rotation")
    val BURN_IN_PREVENTION = booleanPreferencesKey("burn_in_prevention")
    val SHOW_DATE = booleanPreferencesKey("show_date")
    val DATE_POSITION = intPreferencesKey("date_position")
    val DATE_SIZE_PERCENT = intPreferencesKey("date_size_percent")
    val CLOCK_OFFSET_X = intPreferencesKey("clock_offset_x")
    val CLOCK_OFFSET_Y = intPreferencesKey("clock_offset_y")
    val DATE_OFFSET_X = intPreferencesKey("date_offset_x")
    val DATE_OFFSET_Y = intPreferencesKey("date_offset_y")
    val MAJOR_TICK_SCALE = intPreferencesKey("major_tick_scale")
    val MINOR_TICK_SCALE = intPreferencesKey("minor_tick_scale")
    val NUMBER_SCALE = intPreferencesKey("number_scale")
}

class SettingsRepository(private val context: Context) {

    val appSettingsFlow: Flow<ClockSettings> = flowFor(SettingsScope.APP)
    val widgetSettingsFlow: Flow<ClockSettings> = flowFor(SettingsScope.WIDGET)

    private fun flowFor(scope: SettingsScope): Flow<ClockSettings> {
        val k = ScopedKeys(scope)
        return context.dataStore.data.map { prefs ->
            ClockSettings(
                isDarkBackground = prefs[k.IS_DARK_BACKGROUND] ?: prefs[LegacyKeys.IS_DARK_BACKGROUND] ?: true,
                showSecondHand = prefs[k.SHOW_SECOND_HAND] ?: prefs[LegacyKeys.SHOW_SECOND_HAND] ?: true,
                showFrame = prefs[k.SHOW_FRAME] ?: prefs[LegacyKeys.SHOW_FRAME] ?: true,
                brightnessPercent = prefs[k.BRIGHTNESS_PERCENT] ?: prefs[LegacyKeys.BRIGHTNESS_PERCENT] ?: 100,
                clockSizePercent = prefs[k.CLOCK_SIZE_PERCENT] ?: prefs[LegacyKeys.CLOCK_SIZE_PERCENT] ?: 100,
                clockFont = ClockFont.entries.getOrElse(
                    prefs[k.CLOCK_FONT] ?: prefs[LegacyKeys.CLOCK_FONT] ?: 0
                ) { ClockFont.DEFAULT },
                rotation = prefs[k.ROTATION] ?: prefs[LegacyKeys.ROTATION] ?: 0,
                burnInPrevention = prefs[k.BURN_IN_PREVENTION] ?: prefs[LegacyKeys.BURN_IN_PREVENTION] ?: true,
                showDate = prefs[k.SHOW_DATE] ?: prefs[LegacyKeys.SHOW_DATE] ?: false,
                datePosition = DatePosition.entries.getOrElse(
                    prefs[k.DATE_POSITION] ?: prefs[LegacyKeys.DATE_POSITION] ?: 0
                ) { DatePosition.LEFT },
                dateSizePercent = prefs[k.DATE_SIZE_PERCENT] ?: prefs[LegacyKeys.DATE_SIZE_PERCENT] ?: 100,
                clockOffsetX = prefs[k.CLOCK_OFFSET_X] ?: prefs[LegacyKeys.CLOCK_OFFSET_X] ?: 0,
                clockOffsetY = prefs[k.CLOCK_OFFSET_Y] ?: prefs[LegacyKeys.CLOCK_OFFSET_Y] ?: 0,
                dateOffsetX = prefs[k.DATE_OFFSET_X] ?: prefs[LegacyKeys.DATE_OFFSET_X] ?: 0,
                dateOffsetY = prefs[k.DATE_OFFSET_Y] ?: prefs[LegacyKeys.DATE_OFFSET_Y] ?: 0,
                majorTickScale = prefs[k.MAJOR_TICK_SCALE] ?: prefs[LegacyKeys.MAJOR_TICK_SCALE] ?: 100,
                minorTickScale = prefs[k.MINOR_TICK_SCALE] ?: prefs[LegacyKeys.MINOR_TICK_SCALE] ?: 100,
                numberScale = prefs[k.NUMBER_SCALE] ?: prefs[LegacyKeys.NUMBER_SCALE] ?: 100
            )
        }
    }

    suspend fun updateDarkBackground(scope: SettingsScope, value: Boolean) {
        context.dataStore.edit { it[ScopedKeys(scope).IS_DARK_BACKGROUND] = value }
    }

    suspend fun updateShowSecondHand(scope: SettingsScope, value: Boolean) {
        context.dataStore.edit { it[ScopedKeys(scope).SHOW_SECOND_HAND] = value }
    }

    suspend fun updateShowFrame(scope: SettingsScope, value: Boolean) {
        context.dataStore.edit { it[ScopedKeys(scope).SHOW_FRAME] = value }
    }

    suspend fun updateBrightnessPercent(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).BRIGHTNESS_PERCENT] = value.coerceIn(0, 100) }
    }

    suspend fun updateClockSizePercent(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).CLOCK_SIZE_PERCENT] = value.coerceIn(50, 200) }
    }

    suspend fun updateBurnInPrevention(scope: SettingsScope, value: Boolean) {
        context.dataStore.edit { it[ScopedKeys(scope).BURN_IN_PREVENTION] = value }
    }

    suspend fun updateRotation(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).ROTATION] = ((value % 360) + 360) % 360 }
    }

    suspend fun updateClockFont(scope: SettingsScope, value: ClockFont) {
        context.dataStore.edit { it[ScopedKeys(scope).CLOCK_FONT] = value.ordinal }
    }

    suspend fun updateShowDate(scope: SettingsScope, value: Boolean) {
        context.dataStore.edit { it[ScopedKeys(scope).SHOW_DATE] = value }
    }

    suspend fun updateDatePosition(scope: SettingsScope, value: DatePosition) {
        context.dataStore.edit { it[ScopedKeys(scope).DATE_POSITION] = value.ordinal }
    }

    suspend fun updateDateSizePercent(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).DATE_SIZE_PERCENT] = value.coerceIn(50, 300) }
    }

    suspend fun updateClockOffsetX(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).CLOCK_OFFSET_X] = value.coerceIn(-500, 500) }
    }

    suspend fun updateClockOffsetY(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).CLOCK_OFFSET_Y] = value.coerceIn(-500, 500) }
    }

    suspend fun updateDateOffsetX(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).DATE_OFFSET_X] = value.coerceIn(-500, 500) }
    }

    suspend fun updateDateOffsetY(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).DATE_OFFSET_Y] = value.coerceIn(-500, 500) }
    }

    suspend fun updateMajorTickScale(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).MAJOR_TICK_SCALE] = value.coerceIn(0, 500) }
    }

    suspend fun updateMinorTickScale(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).MINOR_TICK_SCALE] = value.coerceIn(0, 500) }
    }

    suspend fun updateNumberScale(scope: SettingsScope, value: Int) {
        context.dataStore.edit { it[ScopedKeys(scope).NUMBER_SCALE] = value.coerceIn(0, 500) }
    }

    suspend fun resetToDefaults(scope: SettingsScope) {
        val keys = ScopedKeys(scope)
        val defaults = ClockSettings()
        context.dataStore.edit { prefs ->
            prefs[keys.IS_DARK_BACKGROUND] = defaults.isDarkBackground
            prefs[keys.SHOW_SECOND_HAND] = defaults.showSecondHand
            prefs[keys.SHOW_FRAME] = defaults.showFrame
            prefs[keys.BRIGHTNESS_PERCENT] = defaults.brightnessPercent
            prefs[keys.CLOCK_SIZE_PERCENT] = defaults.clockSizePercent
            prefs[keys.CLOCK_FONT] = defaults.clockFont.ordinal
            prefs[keys.ROTATION] = defaults.rotation
            prefs[keys.BURN_IN_PREVENTION] = defaults.burnInPrevention
            prefs[keys.SHOW_DATE] = defaults.showDate
            prefs[keys.DATE_POSITION] = defaults.datePosition.ordinal
            prefs[keys.DATE_SIZE_PERCENT] = defaults.dateSizePercent
            prefs[keys.CLOCK_OFFSET_X] = defaults.clockOffsetX
            prefs[keys.CLOCK_OFFSET_Y] = defaults.clockOffsetY
            prefs[keys.DATE_OFFSET_X] = defaults.dateOffsetX
            prefs[keys.DATE_OFFSET_Y] = defaults.dateOffsetY
            prefs[keys.MAJOR_TICK_SCALE] = defaults.majorTickScale
            prefs[keys.MINOR_TICK_SCALE] = defaults.minorTickScale
            prefs[keys.NUMBER_SCALE] = defaults.numberScale
        }
    }
}
