package com.example.recallapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LabColors = lightColorScheme(
    primary = Lab.Teal, onPrimary = Color.White,
    secondary = Lab.Accent, onSecondary = Color.White,
    background = Lab.Bg, onBackground = Lab.Ink,
    surface = Lab.Surface, onSurface = Lab.Ink,
    onSurfaceVariant = Lab.Muted, outlineVariant = Lab.Line,
    error = Lab.Hazard,
)

private val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

/** App theme. */
@Composable
fun RecallTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LabColors, typography = LabTypography, shapes = LabShapes, content = content)
}
