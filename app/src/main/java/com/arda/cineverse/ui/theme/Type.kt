package com.arda.cineverse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// H1 28sp/Bold, H2 22sp/SemiBold, H3 18sp/SemiBold,
// Body Large 16sp/Regular, Body Medium 14sp/Regular,
// Body Small 12sp/Regular, Caption 11sp/Regular
val CineVerseTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp),
)
