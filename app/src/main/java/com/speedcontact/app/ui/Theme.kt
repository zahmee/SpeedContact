package com.speedcontact.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BrandBlue = Color(0xFF2F6FDE)
private val BrandBlueLight = Color(0xFFAEC6FF)

@Composable
fun SpeedContactTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = BrandBlueLight)
        else -> lightColorScheme(primary = BrandBlue)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** Keeps phone numbers readable, with a leading + in place, inside right-to-left text. */
fun String.asLeftToRight(): String = "\u2066$this\u2069"
