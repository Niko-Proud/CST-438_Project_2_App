package com.example.recallapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Text styles for the app. */
val LabTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
