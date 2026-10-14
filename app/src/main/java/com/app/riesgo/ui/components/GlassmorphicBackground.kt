package com.app.riesgo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.app.riesgo.ui.theme.DarkBg
import com.app.riesgo.ui.theme.DarkSurface
import com.app.riesgo.ui.theme.LightBg
import com.app.riesgo.ui.theme.LightSurfaceVariant

@Composable
fun GlassmorphicBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgBrush = if (isDark) {
        Brush.verticalGradient(listOf(DarkBg, DarkSurface))
    } else {
        Brush.verticalGradient(listOf(LightBg, LightSurfaceVariant))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush),
        content = content
    )
}
