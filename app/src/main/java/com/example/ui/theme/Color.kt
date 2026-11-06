package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.domain.model.WeatherConditionType

// Clean-Room Graph Paper Light Palette (matching reference architectural grid)
val CleanRoomLightBg = Color(0xFFFAF9F5)
val CleanRoomLightSurface = Color(0xF5FAF9F5)
val CleanRoomLightSurfaceElevated = Color(0xFFF3F1EA)
val CleanRoomLightBorder = Color(0xFFE3E0D6)
val CleanRoomLightBorderStrong = Color(0xFF141413)
val CleanRoomLightInkPrimary = Color(0xFF141413)
val CleanRoomLightInkSecondary = Color(0xFF454440)
val CleanRoomLightInkMuted = Color(0xFF5A5853)

// True AMOLED Dark Graph Paper Palette (#000000 pure black canvas)
val NightLabDarkBg = Color(0xFF000000)
val NightLabDarkSurface = Color(0xEB050507)
val NightLabDarkSurfaceElevated = Color(0xFF0A0B0F)
val NightLabDarkBorder = Color(0xFF1A1D24)
val NightLabDarkBorderStrong = Color(0xFFF5F5F0)
val NightLabDarkInkPrimary = Color(0xFFFAFAFA)
val NightLabDarkInkSecondary = Color(0xFFB4B8C2)
val NightLabDarkInkMuted = Color(0xFF949AA8)

// Reference Architectural Pastel Block Swatches (Light) & Muted Lab Swatches (Dark)
val BlockLavenderLight = Color(0xFFC6BDF7)
val BlockButterLight = Color(0xFFF6E29B)
val BlockSageLight = Color(0xFFC2DBC0)
val BlockSkyLight = Color(0xFFB7D5F7)
val BlockPeachLight = Color(0xFFF7C6A8)

val BlockLavenderDark = Color(0xFF3E356E)
val BlockButterDark = Color(0xFF5C4E22)
val BlockSageDark = Color(0xFF284734)
val BlockSkyDark = Color(0xFF214263)
val BlockPeachDark = Color(0xFF5E3424)

data class WeatherAccentPalette(
  val heroBlockFill: Color,
  val heroBlockInk: Color,
  val primary: Color,
  val secondary: Color,
  val softFill: Color,
)

fun weatherAccentFor(condition: WeatherConditionType, isDark: Boolean): WeatherAccentPalette =
  when (condition) {
    WeatherConditionType.CLEAR_DAY ->
      if (isDark) {
        WeatherAccentPalette(
          heroBlockFill = BlockButterDark,
          heroBlockInk = Color(0xFFFDF6DC),
          primary = Color(0xFFE5B84B),
          secondary = Color(0xFF9E8CF2),
          softFill = Color(0x28E5B84B),
        )
      } else {
        WeatherAccentPalette(
          heroBlockFill = BlockButterLight,
          heroBlockInk = Color(0xFF141413),
          primary = Color(0xFFD99B1E),
          secondary = Color(0xFF7E6CE0),
          softFill = Color(0x45F6E29B),
        )
      }

    WeatherConditionType.CLEAR_NIGHT,
    WeatherConditionType.PARTLY_CLOUDY ->
      if (isDark) {
        WeatherAccentPalette(
          heroBlockFill = BlockLavenderDark,
          heroBlockInk = Color(0xFFF2F0FF),
          primary = Color(0xFF9E8CF2),
          secondary = Color(0xFF7CC0F5),
          softFill = Color(0x2E9E8CF2),
        )
      } else {
        WeatherAccentPalette(
          heroBlockFill = BlockLavenderLight,
          heroBlockInk = Color(0xFF141413),
          primary = Color(0xFF6B57D6),
          secondary = Color(0xFF4A90D9),
          softFill = Color(0x55C6BDF7),
        )
      }

    WeatherConditionType.OVERCAST,
    WeatherConditionType.FOG ->
      if (isDark) {
        WeatherAccentPalette(
          heroBlockFill = BlockSageDark,
          heroBlockInk = Color(0xFFE6F4EA),
          primary = Color(0xFF78B88E),
          secondary = Color(0xFFE5B84B),
          softFill = Color(0x2E78B88E),
        )
      } else {
        WeatherAccentPalette(
          heroBlockFill = BlockSageLight,
          heroBlockInk = Color(0xFF141413),
          primary = Color(0xFF4A8C62),
          secondary = Color(0xFFD99B1E),
          softFill = Color(0x55C2DBC0),
        )
      }

    WeatherConditionType.DRIZZLE,
    WeatherConditionType.RAIN,
    WeatherConditionType.HEAVY_RAIN,
    WeatherConditionType.SNOW ->
      if (isDark) {
        WeatherAccentPalette(
          heroBlockFill = BlockSkyDark,
          heroBlockInk = Color(0xFFE5F2FF),
          primary = Color(0xFF68B5F5),
          secondary = Color(0xFF9E8CF2),
          softFill = Color(0x2E68B5F5),
        )
      } else {
        WeatherAccentPalette(
          heroBlockFill = BlockSkyLight,
          heroBlockInk = Color(0xFF141413),
          primary = Color(0xFF2B78C4),
          secondary = Color(0xFF6B57D6),
          softFill = Color(0x55B7D5F7),
        )
      }

    WeatherConditionType.THUNDERSTORM ->
      if (isDark) {
        WeatherAccentPalette(
          heroBlockFill = BlockPeachDark,
          heroBlockInk = Color(0xFFFFEFE8),
          primary = Color(0xFFE88A61),
          secondary = Color(0xFFE5B84B),
          softFill = Color(0x2EE88A61),
        )
      } else {
        WeatherAccentPalette(
          heroBlockFill = BlockPeachLight,
          heroBlockInk = Color(0xFF141413),
          primary = Color(0xFFD16034),
          secondary = Color(0xFFD99B1E),
          softFill = Color(0x55F7C6A8),
        )
      }
  }
