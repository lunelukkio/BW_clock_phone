package com.example.bw_clock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ClockColors(
    val background: Color,
    val foreground: Color,
    val secondHand: Color = SecondHandRed
)

val LocalClockColors = staticCompositionLocalOf {
    ClockColors(background = ClockBlack, foreground = ClockWhite)
}

@Composable
fun BW_clockTheme(
    isDarkBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    val clockColors = if (isDarkBackground) {
        ClockColors(background = ClockBlack, foreground = ClockWhite)
    } else {
        ClockColors(background = ClockWhite, foreground = ClockBlack)
    }

    val colorScheme = if (isDarkBackground) {
        darkColorScheme(
            primary = ClockWhite,
            secondary = ClockWhite,
            tertiary = ClockWhite,
            background = ClockBlack,
            surface = ClockBlack,
            onPrimary = ClockBlack,
            onSecondary = ClockBlack,
            onBackground = ClockWhite,
            onSurface = ClockWhite
        )
    } else {
        lightColorScheme(
            primary = ClockBlack,
            secondary = ClockBlack,
            tertiary = ClockBlack,
            background = ClockWhite,
            surface = ClockWhite,
            onPrimary = ClockWhite,
            onSecondary = ClockWhite,
            onBackground = ClockBlack,
            onSurface = ClockBlack
        )
    }

    CompositionLocalProvider(LocalClockColors provides clockColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
