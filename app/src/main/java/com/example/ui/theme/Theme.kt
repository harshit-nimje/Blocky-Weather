package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.WeatherConditionType

@Immutable
data class GridcastDesignTokens(
  val isDark: Boolean,
  val isDynamicThemeActive: Boolean,
  val background: Color,
  val surfaceTranslucent: Color,
  val surfaceElevated: Color,
  val gridBorder: Color,
  val gridBorderStrong: Color,
  val inkPrimary: Color,
  val inkSecondary: Color,
  val inkMuted: Color,
  val heroBlockFill: Color,
  val heroBlockInk: Color,
  val accentPrimary: Color,
  val accentSecondary: Color,
  val accentSoftFill: Color,
  val blockSwatches: List<Color>,
)

val LocalGridcastTokens =
  staticCompositionLocalOf {
    GridcastDesignTokens(
      isDark = false,
      isDynamicThemeActive = false,
      background = CleanRoomLightBg,
      surfaceTranslucent = CleanRoomLightSurface,
      surfaceElevated = CleanRoomLightSurfaceElevated,
      gridBorder = CleanRoomLightBorder,
      gridBorderStrong = CleanRoomLightBorderStrong,
      inkPrimary = CleanRoomLightInkPrimary,
      inkSecondary = CleanRoomLightInkSecondary,
      inkMuted = CleanRoomLightInkMuted,
      heroBlockFill = BlockLavenderLight,
      heroBlockInk = CleanRoomLightInkPrimary,
      accentPrimary = Color(0xFF6B57D6),
      accentSecondary = Color(0xFF4A90D9),
      accentSoftFill = Color(0x55C6BDF7),
      blockSwatches =
        listOf(
          BlockSageLight,
          BlockButterLight,
          BlockPeachLight,
          BlockLavenderLight,
          BlockSkyLight,
        ),
    )
  }

@Composable
fun GridcastTheme(
  darkTheme: Boolean,
  weatherCondition: WeatherConditionType,
  useDynamicTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val weatherAccent = weatherAccentFor(weatherCondition, darkTheme)

  val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val dynamicScheme =
    if (useDynamicTheme && supportsDynamicColor) {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
      null
    }

  val accentPrimary = dynamicScheme?.primary ?: weatherAccent.primary
  val accentSecondary = dynamicScheme?.tertiary ?: weatherAccent.secondary
  val heroBlockFill = dynamicScheme?.primaryContainer ?: weatherAccent.heroBlockFill
  val heroBlockInk = dynamicScheme?.onPrimaryContainer ?: weatherAccent.heroBlockInk
  val accentSoftFill =
    dynamicScheme?.primary?.copy(alpha = if (darkTheme) 0.22f else 0.16f) ?: weatherAccent.softFill

  // Preserve Clean-Room paper light & True AMOLED #000000 dark canvases while subtly blending
  // Material You wallpaper surface tint into light mode
  val bg =
    if (darkTheme) {
      NightLabDarkBg
    } else if (dynamicScheme != null) {
      lerp(CleanRoomLightBg, dynamicScheme.background, 0.22f)
    } else {
      CleanRoomLightBg
    }

  val surfaceTranslucent =
    if (darkTheme) {
      NightLabDarkSurface
    } else if (dynamicScheme != null) {
      lerp(CleanRoomLightSurface, dynamicScheme.surface, 0.25f)
    } else {
      CleanRoomLightSurface
    }

  val surfaceElevated =
    if (darkTheme) {
      dynamicScheme?.let { lerp(NightLabDarkSurfaceElevated, it.surfaceVariant, 0.22f) }
        ?: NightLabDarkSurfaceElevated
    } else {
      dynamicScheme?.let { lerp(CleanRoomLightSurfaceElevated, it.surfaceVariant, 0.28f) }
        ?: CleanRoomLightSurfaceElevated
    }

  val gridBorder =
    if (darkTheme) {
      dynamicScheme?.let { lerp(NightLabDarkBorder, it.outlineVariant, 0.28f) }
        ?: NightLabDarkBorder
    } else {
      dynamicScheme?.let { lerp(CleanRoomLightBorder, it.outlineVariant, 0.30f) }
        ?: CleanRoomLightBorder
    }

  val gridBorderStrong = if (darkTheme) NightLabDarkBorderStrong else CleanRoomLightBorderStrong
  val inkPrimary = if (darkTheme) NightLabDarkInkPrimary else CleanRoomLightInkPrimary
  val inkSecondary = if (darkTheme) NightLabDarkInkSecondary else CleanRoomLightInkSecondary
  val inkMuted = if (darkTheme) NightLabDarkInkMuted else CleanRoomLightInkMuted

  val baseSwatches =
    if (darkTheme) {
      listOf(
        BlockSageDark,
        BlockButterDark,
        BlockPeachDark,
        BlockLavenderDark,
        BlockSkyDark,
      )
    } else {
      listOf(
        BlockSageLight,
        BlockButterLight,
        BlockPeachLight,
        BlockLavenderLight,
        BlockSkyLight,
      )
    }

  val swatches =
    if (dynamicScheme != null) {
      listOf(
        dynamicScheme.primaryContainer,
        dynamicScheme.secondaryContainer,
        dynamicScheme.tertiaryContainer,
        baseSwatches[3],
        baseSwatches[4],
      )
    } else {
      baseSwatches
    }

  val tokens =
    GridcastDesignTokens(
      isDark = darkTheme,
      isDynamicThemeActive = dynamicScheme != null,
      background = bg,
      surfaceTranslucent = surfaceTranslucent,
      surfaceElevated = surfaceElevated,
      gridBorder = gridBorder,
      gridBorderStrong = gridBorderStrong,
      inkPrimary = inkPrimary,
      inkSecondary = inkSecondary,
      inkMuted = inkMuted,
      heroBlockFill = heroBlockFill,
      heroBlockInk = heroBlockInk,
      accentPrimary = accentPrimary,
      accentSecondary = accentSecondary,
      accentSoftFill = accentSoftFill,
      blockSwatches = swatches,
    )

  val colorScheme =
    if (darkTheme) {
      darkColorScheme(
        primary = accentPrimary,
        onPrimary = dynamicScheme?.onPrimary ?: inkPrimary,
        primaryContainer = heroBlockFill,
        onPrimaryContainer = heroBlockInk,
        secondary = accentSecondary,
        background = bg,
        surface = surfaceTranslucent,
        surfaceVariant = surfaceElevated,
        onBackground = inkPrimary,
        onSurface = inkPrimary,
        onSurfaceVariant = inkSecondary,
        outline = gridBorder,
      )
    } else {
      lightColorScheme(
        primary = accentPrimary,
        onPrimary = dynamicScheme?.onPrimary ?: Color.White,
        primaryContainer = heroBlockFill,
        onPrimaryContainer = heroBlockInk,
        secondary = accentSecondary,
        background = bg,
        surface = surfaceTranslucent,
        surfaceVariant = surfaceElevated,
        onBackground = inkPrimary,
        onSurface = inkPrimary,
        onSurfaceVariant = inkSecondary,
        outline = gridBorder,
      )
    }

  CompositionLocalProvider(LocalGridcastTokens provides tokens) {
    MaterialTheme(colorScheme = colorScheme, typography = GridcastTypography, content = content)
  }
}
