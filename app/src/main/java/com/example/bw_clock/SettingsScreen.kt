package com.example.bw_clock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class SettingsItem(
    val label: String,
    val valueText: String,
    val onLeft: () -> Unit,
    val onRight: () -> Unit
)

@Composable
fun SettingsScreen(
    appSettings: ClockSettings,
    widgetSettings: ClockSettings,
    repository: SettingsRepository,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit
) {
    var selectedScope by remember { mutableStateOf(SettingsScope.APP) }
    val activeSettings = if (selectedScope == SettingsScope.APP) appSettings else widgetSettings

    val isVertical = selectedScope == SettingsScope.APP &&
        (appSettings.rotation == 90 || appSettings.rotation == 270)

    val items = buildSettingsItems(selectedScope, activeSettings, isVertical, repository, coroutineScope)

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .heightIn(max = 560.dp)
                .background(Color.DarkGray.copy(alpha = 0.9f))
                .clickable { /* prevent dismiss when clicking panel */ }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(stringResource(R.string.tab_app), selectedScope == SettingsScope.APP) {
                    selectedScope = SettingsScope.APP
                }
                TabButton(stringResource(R.string.tab_widget), selectedScope == SettingsScope.WIDGET) {
                    selectedScope = SettingsScope.WIDGET
                }
            }

            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\u25C0",
                                color = Color.Yellow,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { item.onLeft() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            Text(
                                text = item.valueText,
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "\u25B6",
                                color = Color.Yellow,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { item.onRight() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent
    val fg = if (selected) Color.White else Color.LightGray
    Text(
        text = label,
        color = fg,
        fontSize = 14.sp,
        modifier = Modifier
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun buildSettingsItems(
    scope: SettingsScope,
    s: ClockSettings,
    isVertical: Boolean,
    repo: SettingsRepository,
    cs: CoroutineScope
): List<SettingsItem> {
    val on = stringResource(R.string.value_on)
    val off = stringResource(R.string.value_off)

    val datePositionText = when {
        isVertical && s.datePosition == DatePosition.LEFT -> stringResource(R.string.date_pos_top)
        isVertical -> stringResource(R.string.date_pos_bottom)
        s.datePosition == DatePosition.LEFT -> stringResource(R.string.date_pos_left)
        else -> stringResource(R.string.date_pos_right)
    }
    val fontText = s.clockFont.localizedLabel()
    val handTipText = s.handTipStyle.localizedLabel()
    val themeText = if (s.isDarkBackground) stringResource(R.string.theme_dark) else stringResource(R.string.theme_light)

    val labelBrightness = stringResource(R.string.label_brightness)
    val labelTheme = stringResource(R.string.label_theme)
    val labelSecondHand = stringResource(R.string.label_second_hand)
    val labelFrame = stringResource(R.string.label_frame)
    val labelNumberScale = stringResource(R.string.label_number_scale)
    val labelMajorTick = stringResource(R.string.label_major_tick)
    val labelMinorTick = stringResource(R.string.label_minor_tick)
    val labelFont = stringResource(R.string.label_font)
    val labelSize = stringResource(R.string.label_size)
    val labelClockOffsetX = stringResource(R.string.label_clock_offset_x)
    val labelClockOffsetY = stringResource(R.string.label_clock_offset_y)
    val labelDate = stringResource(R.string.label_date)
    val labelDatePosition = stringResource(R.string.label_date_position)
    val labelDateSize = stringResource(R.string.label_date_size)
    val labelDateOffsetX = stringResource(R.string.label_date_offset_x)
    val labelDateOffsetY = stringResource(R.string.label_date_offset_y)
    val labelHandTip = stringResource(R.string.label_hand_tip)
    val labelHandThickness = stringResource(R.string.label_hand_thickness)
    val labelBurnIn = stringResource(R.string.label_burn_in)
    val labelRotation = stringResource(R.string.label_rotation)
    val labelLanguage = stringResource(R.string.label_language)
    val languageJa = stringResource(R.string.language_ja)
    val languageEn = stringResource(R.string.language_en)
    val labelReset = stringResource(R.string.label_reset)
    val resetAction = stringResource(R.string.reset_action)

    val items = mutableListOf<SettingsItem>()

    if (scope == SettingsScope.APP) {
        items += SettingsItem(
            label = labelBrightness,
            valueText = "${s.brightnessPercent}%",
            onLeft = { cs.launch { repo.updateBrightnessPercent(scope, s.brightnessPercent - 10) } },
            onRight = { cs.launch { repo.updateBrightnessPercent(scope, s.brightnessPercent + 10) } }
        )
    }

    items += SettingsItem(
        label = labelTheme,
        valueText = themeText,
        onLeft = { cs.launch { repo.updateDarkBackground(scope, !s.isDarkBackground) } },
        onRight = { cs.launch { repo.updateDarkBackground(scope, !s.isDarkBackground) } }
    )

    if (scope == SettingsScope.APP) {
        items += SettingsItem(
            label = labelSecondHand,
            valueText = if (s.showSecondHand) on else off,
            onLeft = { cs.launch { repo.updateShowSecondHand(scope, !s.showSecondHand) } },
            onRight = { cs.launch { repo.updateShowSecondHand(scope, !s.showSecondHand) } }
        )
    }

    items += SettingsItem(
        label = labelFrame,
        valueText = if (s.showFrame) on else off,
        onLeft = { cs.launch { repo.updateShowFrame(scope, !s.showFrame) } },
        onRight = { cs.launch { repo.updateShowFrame(scope, !s.showFrame) } }
    )
    items += SettingsItem(
        label = labelNumberScale,
        valueText = "${s.numberScale}%",
        onLeft = { cs.launch { repo.updateNumberScale(scope, s.numberScale - 25) } },
        onRight = { cs.launch { repo.updateNumberScale(scope, s.numberScale + 25) } }
    )
    items += SettingsItem(
        label = labelMajorTick,
        valueText = "${s.majorTickScale}%",
        onLeft = { cs.launch { repo.updateMajorTickScale(scope, s.majorTickScale - 25) } },
        onRight = { cs.launch { repo.updateMajorTickScale(scope, s.majorTickScale + 25) } }
    )
    items += SettingsItem(
        label = labelMinorTick,
        valueText = "${s.minorTickScale}%",
        onLeft = { cs.launch { repo.updateMinorTickScale(scope, s.minorTickScale - 25) } },
        onRight = { cs.launch { repo.updateMinorTickScale(scope, s.minorTickScale + 25) } }
    )
    items += SettingsItem(
        label = labelFont,
        valueText = fontText,
        onLeft = {
            val fonts = ClockFont.entries
            val prev = fonts[(s.clockFont.ordinal - 1 + fonts.size) % fonts.size]
            cs.launch { repo.updateClockFont(scope, prev) }
        },
        onRight = {
            val fonts = ClockFont.entries
            val next = fonts[(s.clockFont.ordinal + 1) % fonts.size]
            cs.launch { repo.updateClockFont(scope, next) }
        }
    )
    items += SettingsItem(
        label = labelHandTip,
        valueText = handTipText,
        onLeft = {
            val styles = HandTipStyle.entries
            val prev = styles[(s.handTipStyle.ordinal - 1 + styles.size) % styles.size]
            cs.launch { repo.updateHandTipStyle(scope, prev) }
        },
        onRight = {
            val styles = HandTipStyle.entries
            val next = styles[(s.handTipStyle.ordinal + 1) % styles.size]
            cs.launch { repo.updateHandTipStyle(scope, next) }
        }
    )
    items += SettingsItem(
        label = labelHandThickness,
        valueText = "${s.handThicknessScale}%",
        onLeft = { cs.launch { repo.updateHandThicknessScale(scope, s.handThicknessScale - 5) } },
        onRight = { cs.launch { repo.updateHandThicknessScale(scope, s.handThicknessScale + 5) } }
    )
    items += SettingsItem(
        label = labelSize,
        valueText = "${s.clockSizePercent}%",
        onLeft = { cs.launch { repo.updateClockSizePercent(scope, s.clockSizePercent - 10) } },
        onRight = { cs.launch { repo.updateClockSizePercent(scope, s.clockSizePercent + 10) } }
    )
    items += SettingsItem(
        label = labelClockOffsetX,
        valueText = "${s.clockOffsetX}%",
        onLeft = { cs.launch { repo.updateClockOffsetX(scope, s.clockOffsetX - 5) } },
        onRight = { cs.launch { repo.updateClockOffsetX(scope, s.clockOffsetX + 5) } }
    )
    items += SettingsItem(
        label = labelClockOffsetY,
        valueText = "${s.clockOffsetY}%",
        onLeft = { cs.launch { repo.updateClockOffsetY(scope, s.clockOffsetY - 5) } },
        onRight = { cs.launch { repo.updateClockOffsetY(scope, s.clockOffsetY + 5) } }
    )
    items += SettingsItem(
        label = labelDate,
        valueText = if (s.showDate) on else off,
        onLeft = { cs.launch { repo.updateShowDate(scope, !s.showDate) } },
        onRight = { cs.launch { repo.updateShowDate(scope, !s.showDate) } }
    )
    items += SettingsItem(
        label = labelDatePosition,
        valueText = datePositionText,
        onLeft = {
            val positions = DatePosition.entries
            val prev = positions[(s.datePosition.ordinal - 1 + positions.size) % positions.size]
            cs.launch { repo.updateDatePosition(scope, prev) }
        },
        onRight = {
            val positions = DatePosition.entries
            val next = positions[(s.datePosition.ordinal + 1) % positions.size]
            cs.launch { repo.updateDatePosition(scope, next) }
        }
    )
    items += SettingsItem(
        label = labelDateSize,
        valueText = "${s.dateSizePercent}%",
        onLeft = { cs.launch { repo.updateDateSizePercent(scope, s.dateSizePercent - 10) } },
        onRight = { cs.launch { repo.updateDateSizePercent(scope, s.dateSizePercent + 10) } }
    )
    items += SettingsItem(
        label = labelDateOffsetX,
        valueText = "${s.dateOffsetX}%",
        onLeft = { cs.launch { repo.updateDateOffsetX(scope, s.dateOffsetX - 5) } },
        onRight = { cs.launch { repo.updateDateOffsetX(scope, s.dateOffsetX + 5) } }
    )
    items += SettingsItem(
        label = labelDateOffsetY,
        valueText = "${s.dateOffsetY}%",
        onLeft = { cs.launch { repo.updateDateOffsetY(scope, s.dateOffsetY - 5) } },
        onRight = { cs.launch { repo.updateDateOffsetY(scope, s.dateOffsetY + 5) } }
    )

    if (scope == SettingsScope.APP) {
        items += SettingsItem(
            label = labelBurnIn,
            valueText = if (s.burnInPrevention) on else off,
            onLeft = { cs.launch { repo.updateBurnInPrevention(scope, !s.burnInPrevention) } },
            onRight = { cs.launch { repo.updateBurnInPrevention(scope, !s.burnInPrevention) } }
        )
        items += SettingsItem(
            label = labelRotation,
            valueText = "${s.rotation}°",
            onLeft = { cs.launch { repo.updateRotation(scope, s.rotation - 90) } },
            onRight = { cs.launch { repo.updateRotation(scope, s.rotation + 90) } }
        )
    }

    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val langIsEn = currentLang.startsWith("en")
    val langValueText = if (langIsEn) languageEn else languageJa
    items += SettingsItem(
        label = labelLanguage,
        valueText = langValueText,
        onLeft = { toggleLanguage(langIsEn) },
        onRight = { toggleLanguage(langIsEn) }
    )

    items += SettingsItem(
        label = labelReset,
        valueText = resetAction,
        onLeft = { cs.launch { repo.resetToDefaults(scope) } },
        onRight = { cs.launch { repo.resetToDefaults(scope) } }
    )

    return items
}

private fun toggleLanguage(currentlyEnglish: Boolean) {
    val target = if (currentlyEnglish) "ja" else "en"
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(target))
}

@Composable
private fun ClockFont.localizedLabel(): String = when (this) {
    ClockFont.DEFAULT -> stringResource(R.string.font_default)
    ClockFont.SERIF -> stringResource(R.string.font_serif)
    ClockFont.MONOSPACE -> stringResource(R.string.font_monospace)
    ClockFont.SANS_SERIF_LIGHT -> stringResource(R.string.font_light)
    ClockFont.SANS_SERIF_BOLD -> stringResource(R.string.font_bold)
}

@Composable
private fun HandTipStyle.localizedLabel(): String = when (this) {
    HandTipStyle.ROUNDED -> stringResource(R.string.hand_tip_rounded)
    HandTipStyle.SQUARED -> stringResource(R.string.hand_tip_squared)
    HandTipStyle.TAPERED -> stringResource(R.string.hand_tip_tapered)
}
