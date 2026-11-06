package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.WeatherConditionType
import kotlin.math.max
import kotlin.math.min

internal fun wcagContrastRatio(foreground: Color, background: Color): Float {
  val l1 = foreground.luminance()
  val l2 = background.luminance()
  val lighter = max(l1, l2)
  val darker = min(l1, l2)
  return (lighter + 0.05f) / (darker + 0.05f)
}

internal fun ensureMinContrastAgainst(
  foreground: Color,
  background: Color,
  isDarkCanvas: Boolean,
  minRatio: Float = 4.8f,
): Color {
  if (wcagContrastRatio(foreground, background) >= minRatio) return foreground
  val targetAnchor = if (isDarkCanvas) Color(0xFFFAFAFA) else Color(0xFF121211)
  var step = 0.05f
  while (step <= 1.0f) {
    val candidate = lerp(foreground, targetAnchor, step)
    if (wcagContrastRatio(candidate, background) >= minRatio) {
      return candidate
    }
    step += 0.05f
  }
  return targetAnchor
}

internal fun ensureMinContrastAgainstAll(
  foreground: Color,
  backgrounds: List<Color>,
  isDarkCanvas: Boolean,
  minRatio: Float = 4.8f,
): Color {
  var current = foreground
  for (bg in backgrounds) {
    current = ensureMinContrastAgainst(current, bg, isDarkCanvas, minRatio)
  }
  return current
}

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
      accentPrimary = Color(0xFF5542B8),
      accentSecondary = Color(0xFF2F6DB3),
      accentSoftFill = Color(0x36C6BDF7),
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

  val tokens =
    enforceDynamicTokenContrast(
      scheme = dynamicScheme,
      darkTheme = darkTheme,
      weatherAccent = weatherAccent,
    )
  val bg = tokens.background
  val surfaceTranslucent = tokens.surfaceTranslucent
  val surfaceElevated = tokens.surfaceElevated
  val gridBorder = tokens.gridBorder
  val inkPrimary = tokens.inkPrimary
  val inkSecondary = tokens.inkSecondary
  val heroBlockFill = tokens.heroBlockFill
  val heroBlockInk = tokens.heroBlockInk
  val accentPrimary = tokens.accentPrimary
  val accentSecondary = tokens.accentSecondary

  val onPrimaryResolved =
    ensureMinContrastAgainst(
      foreground = dynamicScheme?.onPrimary ?: if (darkTheme) Color(0xFF0A0B0F) else Color.White,
      background = accentPrimary,
      isDarkCanvas = accentPrimary.luminance() < 0.35f,
      minRatio = 4.8f,
    )

  val colorScheme =
    if (darkTheme) {
      darkColorScheme(
        primary = accentPrimary,
        onPrimary = onPrimaryResolved,
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
        onPrimary = onPrimaryResolved,
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

internal fun enforceDynamicTokenContrast(
  scheme: ColorScheme?,
  darkTheme: Boolean,
  weatherAccent: WeatherAccentPalette = weatherAccentFor(WeatherConditionType.CLEAR_DAY, darkTheme),
): GridcastDesignTokens {
  val bg =
    if (darkTheme) {
      NightLabDarkBg
    } else if (scheme != null) {
      lerp(CleanRoomLightBg, scheme.background, 0.18f)
    } else {
      CleanRoomLightBg
    }

  val surfaceTranslucent =
    if (darkTheme) {
      NightLabDarkSurface
    } else if (scheme != null) {
      lerp(CleanRoomLightSurface, scheme.surface, 0.20f)
    } else {
      CleanRoomLightSurface
    }

  val surfaceElevated =
    if (darkTheme) {
      scheme?.let { lerp(NightLabDarkSurfaceElevated, it.surfaceVariant, 0.18f) }
        ?: NightLabDarkSurfaceElevated
    } else {
      scheme?.let { lerp(CleanRoomLightSurfaceElevated, it.surfaceVariant, 0.22f) }
        ?: CleanRoomLightSurfaceElevated
    }

  val gridBorder =
    if (darkTheme) {
      scheme?.let { lerp(NightLabDarkBorder, it.outlineVariant, 0.26f) }
        ?: NightLabDarkBorder
    } else {
      scheme?.let { lerp(CleanRoomLightBorder, it.outlineVariant, 0.28f) }
        ?: CleanRoomLightBorder
    }

  val gridBorderStrong = if (darkTheme) NightLabDarkBorderStrong else CleanRoomLightBorderStrong

  val rawAccentPrimary = scheme?.primary ?: weatherAccent.primary
  val rawAccentSecondary = scheme?.tertiary ?: weatherAccent.secondary

  val accentSoftFill =
    if (scheme != null) {
      rawAccentPrimary.copy(alpha = if (darkTheme) 0.16f else 0.11f)
    } else {
      weatherAccent.softFill
    }

  val compositedActiveBg = accentSoftFill.compositeOver(bg)
  val compositedElevatedBg = accentSoftFill.compositeOver(surfaceElevated)
  val surfaceList = listOf(bg, surfaceElevated, compositedActiveBg, compositedElevatedBg)

  val accentPrimary =
    ensureMinContrastAgainstAll(
      foreground = rawAccentPrimary,
      backgrounds = surfaceList,
      isDarkCanvas = darkTheme,
      minRatio = 4.8f,
    )
  val accentSecondary =
    ensureMinContrastAgainstAll(
      foreground = rawAccentSecondary,
      backgrounds = listOf(bg, surfaceElevated, compositedActiveBg),
      isDarkCanvas = darkTheme,
      minRatio = 4.5f,
    )

  val rawInkPrimary = if (darkTheme) NightLabDarkInkPrimary else CleanRoomLightInkPrimary
  val rawInkSecondary = if (darkTheme) NightLabDarkInkSecondary else CleanRoomLightInkSecondary
  val rawInkMuted = if (darkTheme) NightLabDarkInkMuted else CleanRoomLightInkMuted

  val inkPrimary = ensureMinContrastAgainstAll(rawInkPrimary, surfaceList, darkTheme, 9.5f)
  val inkSecondary = ensureMinContrastAgainstAll(rawInkSecondary, surfaceList, darkTheme, 5.5f)
  val inkMuted = ensureMinContrastAgainstAll(rawInkMuted, surfaceList, darkTheme, 4.6f)

  val heroBlockFill = scheme?.primaryContainer ?: weatherAccent.heroBlockFill
  val rawHeroBlockInk = scheme?.onPrimaryContainer ?: weatherAccent.heroBlockInk
  val heroBlockIsDark = heroBlockFill.luminance() < 0.38f
  val candidateHeroInk =
    if (wcagContrastRatio(rawHeroBlockInk, heroBlockFill) >= 5.5f) {
      rawHeroBlockInk
    } else {
      if (heroBlockIsDark) NightLabDarkInkPrimary else CleanRoomLightInkPrimary
    }
  val heroBlockInk =
    ensureMinContrastAgainst(
      foreground = candidateHeroInk,
      background = heroBlockFill,
      isDarkCanvas = heroBlockIsDark,
      minRatio = 5.5f,
    )

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
    if (scheme != null) {
      listOf(
        scheme.primaryContainer,
        scheme.secondaryContainer,
        scheme.tertiaryContainer,
        baseSwatches[3],
        baseSwatches[4],
      )
    } else {
      baseSwatches
    }

  return GridcastDesignTokens(
    isDark = darkTheme,
    isDynamicThemeActive = scheme != null,
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
}
