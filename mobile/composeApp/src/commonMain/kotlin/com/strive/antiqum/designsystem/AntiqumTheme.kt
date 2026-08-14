package com.strive.antiqum.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeMode {
    System,
    Light,
    Dark
}

object AntiqumColors {
    val Forest = Color(0xFF21483F)
    val ForestDark = Color(0xFF8EB3A5)
    val Ivory = Color(0xFFF6F2EA)
    val Porcelain = Color(0xFFFFFCF7)
    val Charcoal = Color(0xFF292724)
    val Stone = Color(0xFFE7E0D5)
    val Muted = Color(0xFF77716A)
    val Bronze = Color(0xFF9A6A43)
    val Night = Color(0xFF1F211F)
    val NightSurface = Color(0xFF2A2D2A)
    val NightBorder = Color(0xFF424640)
}

private val LightColors = lightColorScheme(
    primary = AntiqumColors.Forest,
    onPrimary = AntiqumColors.Porcelain,
    secondary = AntiqumColors.Bronze,
    onSecondary = AntiqumColors.Porcelain,
    background = AntiqumColors.Ivory,
    onBackground = AntiqumColors.Charcoal,
    surface = AntiqumColors.Porcelain,
    onSurface = AntiqumColors.Charcoal,
    surfaceVariant = AntiqumColors.Stone,
    onSurfaceVariant = AntiqumColors.Muted,
    outline = AntiqumColors.Stone,
    error = Color(0xFF9B3D35)
)

private val DarkColors = darkColorScheme(
    primary = AntiqumColors.ForestDark,
    onPrimary = AntiqumColors.Night,
    secondary = Color(0xFFC19A70),
    background = AntiqumColors.Night,
    onBackground = AntiqumColors.Ivory,
    surface = AntiqumColors.NightSurface,
    onSurface = AntiqumColors.Ivory,
    surfaceVariant = AntiqumColors.NightBorder,
    onSurfaceVariant = Color(0xFFA8AAA0),
    outline = AntiqumColors.NightBorder,
    error = Color(0xFFFFB4AB)
)

private val AntiqumTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.2.sp
    )
)

@Composable
fun AntiqumTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AntiqumTypography
    ) {
        Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
