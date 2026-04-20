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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val datePositionLabel = if (isVertical) {
        if (activeSettings.datePosition == DatePosition.LEFT) "上" else "下"
    } else {
        activeSettings.datePosition.label
    }

    val items = remember(activeSettings, isVertical, selectedScope) {
        buildSettingsItems(selectedScope, activeSettings, datePositionLabel, repository, coroutineScope)
    }

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
                text = "設定",
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
                TabButton("本体", selectedScope == SettingsScope.APP) {
                    selectedScope = SettingsScope.APP
                }
                TabButton("ウィジェット", selectedScope == SettingsScope.WIDGET) {
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
                                text = "◀",
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
                                text = "▶",
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

private fun buildSettingsItems(
    scope: SettingsScope,
    s: ClockSettings,
    datePositionLabel: String,
    repo: SettingsRepository,
    cs: CoroutineScope
): List<SettingsItem> {
    val items = mutableListOf<SettingsItem>()

    if (scope == SettingsScope.APP) {
        items += SettingsItem(
            label = "明るさ",
            valueText = "${s.brightnessPercent}%",
            onLeft = { cs.launch { repo.updateBrightnessPercent(scope, s.brightnessPercent - 10) } },
            onRight = { cs.launch { repo.updateBrightnessPercent(scope, s.brightnessPercent + 10) } }
        )
    }

    items += SettingsItem(
        label = "テーマ",
        valueText = if (s.isDarkBackground) "黒背景" else "白背景",
        onLeft = { cs.launch { repo.updateDarkBackground(scope, !s.isDarkBackground) } },
        onRight = { cs.launch { repo.updateDarkBackground(scope, !s.isDarkBackground) } }
    )

    if (scope == SettingsScope.APP) {
        items += SettingsItem(
            label = "秒針",
            valueText = if (s.showSecondHand) "ON" else "OFF",
            onLeft = { cs.launch { repo.updateShowSecondHand(scope, !s.showSecondHand) } },
            onRight = { cs.launch { repo.updateShowSecondHand(scope, !s.showSecondHand) } }
        )
    }

    items += SettingsItem(
        label = "数字大きさ",
        valueText = "${s.numberScale}%",
        onLeft = { cs.launch { repo.updateNumberScale(scope, s.numberScale - 25) } },
        onRight = { cs.launch { repo.updateNumberScale(scope, s.numberScale + 25) } }
    )
    items += SettingsItem(
        label = "5分ドット",
        valueText = "${s.majorTickScale}%",
        onLeft = { cs.launch { repo.updateMajorTickScale(scope, s.majorTickScale - 25) } },
        onRight = { cs.launch { repo.updateMajorTickScale(scope, s.majorTickScale + 25) } }
    )
    items += SettingsItem(
        label = "1分ドット",
        valueText = "${s.minorTickScale}%",
        onLeft = { cs.launch { repo.updateMinorTickScale(scope, s.minorTickScale - 25) } },
        onRight = { cs.launch { repo.updateMinorTickScale(scope, s.minorTickScale + 25) } }
    )
    items += SettingsItem(
        label = "外枠",
        valueText = if (s.showFrame) "ON" else "OFF",
        onLeft = { cs.launch { repo.updateShowFrame(scope, !s.showFrame) } },
        onRight = { cs.launch { repo.updateShowFrame(scope, !s.showFrame) } }
    )
    items += SettingsItem(
        label = "フォント",
        valueText = s.clockFont.label,
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
        label = "サイズ",
        valueText = "${s.clockSizePercent}%",
        onLeft = { cs.launch { repo.updateClockSizePercent(scope, s.clockSizePercent - 10) } },
        onRight = { cs.launch { repo.updateClockSizePercent(scope, s.clockSizePercent + 10) } }
    )
    items += SettingsItem(
        label = "時計 横位置",
        valueText = "${s.clockOffsetX}%",
        onLeft = { cs.launch { repo.updateClockOffsetX(scope, s.clockOffsetX - 5) } },
        onRight = { cs.launch { repo.updateClockOffsetX(scope, s.clockOffsetX + 5) } }
    )
    items += SettingsItem(
        label = "時計 縦位置",
        valueText = "${s.clockOffsetY}%",
        onLeft = { cs.launch { repo.updateClockOffsetY(scope, s.clockOffsetY - 5) } },
        onRight = { cs.launch { repo.updateClockOffsetY(scope, s.clockOffsetY + 5) } }
    )
    items += SettingsItem(
        label = "日付",
        valueText = if (s.showDate) "ON" else "OFF",
        onLeft = { cs.launch { repo.updateShowDate(scope, !s.showDate) } },
        onRight = { cs.launch { repo.updateShowDate(scope, !s.showDate) } }
    )
    items += SettingsItem(
        label = "日付位置",
        valueText = datePositionLabel,
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
        label = "日付サイズ",
        valueText = "${s.dateSizePercent}%",
        onLeft = { cs.launch { repo.updateDateSizePercent(scope, s.dateSizePercent - 10) } },
        onRight = { cs.launch { repo.updateDateSizePercent(scope, s.dateSizePercent + 10) } }
    )
    items += SettingsItem(
        label = "日付 横位置",
        valueText = "${s.dateOffsetX}%",
        onLeft = { cs.launch { repo.updateDateOffsetX(scope, s.dateOffsetX - 5) } },
        onRight = { cs.launch { repo.updateDateOffsetX(scope, s.dateOffsetX + 5) } }
    )
    items += SettingsItem(
        label = "日付 縦位置",
        valueText = "${s.dateOffsetY}%",
        onLeft = { cs.launch { repo.updateDateOffsetY(scope, s.dateOffsetY - 5) } },
        onRight = { cs.launch { repo.updateDateOffsetY(scope, s.dateOffsetY + 5) } }
    )

    if (scope == SettingsScope.APP) {
        items += SettingsItem(
            label = "焼付防止",
            valueText = if (s.burnInPrevention) "ON" else "OFF",
            onLeft = { cs.launch { repo.updateBurnInPrevention(scope, !s.burnInPrevention) } },
            onRight = { cs.launch { repo.updateBurnInPrevention(scope, !s.burnInPrevention) } }
        )
        items += SettingsItem(
            label = "回転",
            valueText = "${s.rotation}°",
            onLeft = { cs.launch { repo.updateRotation(scope, s.rotation - 90) } },
            onRight = { cs.launch { repo.updateRotation(scope, s.rotation + 90) } }
        )
    }

    items += SettingsItem(
        label = "リセット",
        valueText = "初期状態に戻す",
        onLeft = { cs.launch { repo.resetToDefaults(scope) } },
        onRight = { cs.launch { repo.resetToDefaults(scope) } }
    )

    return items
}
