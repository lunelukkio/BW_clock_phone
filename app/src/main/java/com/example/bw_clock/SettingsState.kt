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
import kotlin.math.roundToInt

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

data class ClockSettings(
    val isDarkBackground: Boolean = true,
    val showSecondHand: Boolean = true,
    val showNumbers: Boolean = true,
    val showTickMarks: Boolean = true,
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
    val dateOffsetY: Int = 0
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clock_settings")

object SettingsKeys {
    val IS_DARK_BACKGROUND = booleanPreferencesKey("is_dark_background")
    val SHOW_SECOND_HAND = booleanPreferencesKey("show_second_hand")
    val SHOW_NUMBERS = booleanPreferencesKey("show_numbers")
    val SHOW_TICK_MARKS = booleanPreferencesKey("show_tick_marks")
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
}

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<ClockSettings> = context.dataStore.data.map { prefs ->
        ClockSettings(
            isDarkBackground = prefs[SettingsKeys.IS_DARK_BACKGROUND] ?: true,
            showSecondHand = prefs[SettingsKeys.SHOW_SECOND_HAND] ?: true,
            showNumbers = prefs[SettingsKeys.SHOW_NUMBERS] ?: true,
            showTickMarks = prefs[SettingsKeys.SHOW_TICK_MARKS] ?: true,
            showFrame = prefs[SettingsKeys.SHOW_FRAME] ?: true,
            brightnessPercent = prefs[SettingsKeys.BRIGHTNESS_PERCENT] ?: 100,
            clockSizePercent = prefs[SettingsKeys.CLOCK_SIZE_PERCENT] ?: 100,
            clockFont = ClockFont.entries.getOrElse(prefs[SettingsKeys.CLOCK_FONT] ?: 0) { ClockFont.DEFAULT },
            rotation = prefs[SettingsKeys.ROTATION] ?: 0,
            burnInPrevention = prefs[SettingsKeys.BURN_IN_PREVENTION] ?: true,
            showDate = prefs[SettingsKeys.SHOW_DATE] ?: false,
            datePosition = DatePosition.entries.getOrElse(prefs[SettingsKeys.DATE_POSITION] ?: 0) { DatePosition.LEFT },
            dateSizePercent = prefs[SettingsKeys.DATE_SIZE_PERCENT] ?: 100,
            clockOffsetX = prefs[SettingsKeys.CLOCK_OFFSET_X] ?: 0,
            clockOffsetY = prefs[SettingsKeys.CLOCK_OFFSET_Y] ?: 0,
            dateOffsetX = prefs[SettingsKeys.DATE_OFFSET_X] ?: 0,
            dateOffsetY = prefs[SettingsKeys.DATE_OFFSET_Y] ?: 0
        )
    }

    suspend fun updateDarkBackground(value: Boolean) {
        context.dataStore.edit { it[SettingsKeys.IS_DARK_BACKGROUND] = value }
    }

    suspend fun updateShowSecondHand(value: Boolean) {
        context.dataStore.edit { it[SettingsKeys.SHOW_SECOND_HAND] = value }
    }

    suspend fun updateShowNumbers(value: Boolean) {
        context.dataStore.edit { it[SettingsKeys.SHOW_NUMBERS] = value }
    }

    suspend fun updateShowTickMarks(value: Boolean) {
        context.dataStore.edit { it[SettingsKeys.SHOW_TICK_MARKS] = value }
    }

    suspend fun updateShowFrame(value: Boolean) {
        context.dataStore.edit { it[SettingsKeys.SHOW_FRAME] = value }
    }

    suspend fun updateBrightnessPercent(value: Int) {
        context.dataStore.edit { it[SettingsKeys.BRIGHTNESS_PERCENT] = value.coerceIn(0, 100) }
    }

    suspend fun updateClockSizePercent(value: Int) {
        context.dataStore.edit { it[SettingsKeys.CLOCK_SIZE_PERCENT] = value.coerceIn(50, 200) }
    }

    suspend fun updateBurnInPrevention(value: Boolean) {
        context.dataStore.edit { it[SettingsKeys.BURN_IN_PREVENTION] = value }
    }

    suspend fun updateRotation(value: Int) {
        context.dataStore.edit { it[SettingsKeys.ROTATION] = ((value % 360) + 360) % 360 }
    }

    suspend fun updateClockFont(value: ClockFont) {
        context.dataStore.edit { it[SettingsKeys.CLOCK_FONT] = value.ordinal }
    }

    suspend fun updateShowDate(value: Boolean) {
        context.dataStore.edit { it[SettingsKeys.SHOW_DATE] = value }
    }

    suspend fun updateDatePosition(value: DatePosition) {
        context.dataStore.edit { it[SettingsKeys.DATE_POSITION] = value.ordinal }
    }

    suspend fun updateDateSizePercent(value: Int) {
        context.dataStore.edit { it[SettingsKeys.DATE_SIZE_PERCENT] = value.coerceIn(50, 300) }
    }

    suspend fun updateClockOffsetX(value: Int) {
        context.dataStore.edit { it[SettingsKeys.CLOCK_OFFSET_X] = value.coerceIn(-500, 500) }
    }

    suspend fun updateClockOffsetY(value: Int) {
        context.dataStore.edit { it[SettingsKeys.CLOCK_OFFSET_Y] = value.coerceIn(-500, 500) }
    }

    suspend fun updateDateOffsetX(value: Int) {
        context.dataStore.edit { it[SettingsKeys.DATE_OFFSET_X] = value.coerceIn(-500, 500) }
    }

    suspend fun updateDateOffsetY(value: Int) {
        context.dataStore.edit { it[SettingsKeys.DATE_OFFSET_Y] = value.coerceIn(-500, 500) }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
