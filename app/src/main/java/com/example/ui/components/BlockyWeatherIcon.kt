package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.WeatherConditionType
import com.example.ui.theme.LocalGridcastTokens
import kotlinx.coroutines.delay

enum class BlockyMetricIcon {
  REAL_FEEL,
  AQI,
  WIND,
  HUMIDITY,
  UV,
  PRESSURE,
  PRECIPITATION,
  SOLAR,
  LOCATION,
  SUN_THEME,
  MOON_THEME,
  ECO_BOLT,
  SETTINGS,
}

@Composable
fun BlockyWeatherIcon(
  condition: WeatherConditionType,
  modifier: Modifier = Modifier,
  size: Dp = 28.dp,
  animated: Boolean = true,
) {
  val tokens = LocalGridcastTokens.current
  var stepTick by remember { mutableIntStateOf(0) }
  LaunchedEffect(animated) {
    if (animated) {
      while (true) {
        delay(150L)
        stepTick = (stepTick + 1) % 8
      }
    } else {
      stepTick = 0
    }
  }
  val phase = stepTick / 8f

  Canvas(modifier = modifier.size(size)) {
    val grid = 12
    val pitch = this.size.width / grid
    val px = pitch * 0.86f
    val pad = (pitch - px) * 0.5f

    fun block(c: Int, r: Int, color: Color) {
      if (c in 0 until grid && r in 0 until grid) {
        drawRect(
          color = color,
          topLeft = Offset(c * pitch + pad, r * pitch + pad),
          size = Size(px, px),
        )
      }
    }

    val sunGold = Color(0xFFF59E0B)
    val sunBright = Color(0xFFFBBF24)
    val cloudMain = if (tokens.isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val cloudLight = if (tokens.isDark) Color(0xFFF8FAFC) else Color(0xFF64748B)
    val rainCyan = Color(0xFF0EA5E9)
    val rainDeep = Color(0xFF0284C7)
    val boltYellow = Color(0xFFFACC15)
    val snowIce = if (tokens.isDark) Color(0xFFE0F2FE) else Color(0xFF0284C7)
    val moonIndigo = Color(0xFF818CF8)

    when (condition) {
      WeatherConditionType.CLEAR_DAY -> {
        // 4x4 stepped sun core
        for (r in 4..7) {
          for (c in 4..7) {
            block(c, r, if ((c + r + (stepTick / 4)) % 2 == 0) sunGold else sunBright)
          }
        }
        val rayStep = if (stepTick % 8 < 4) 0 else 1
        // Cardinal rays
        block(5, 2 - rayStep, sunGold); block(6, 2 - rayStep, sunGold)
        block(5, 9 + rayStep, sunGold); block(6, 9 + rayStep, sunGold)
        block(2 - rayStep, 5, sunGold); block(2 - rayStep, 6, sunGold)
        block(9 + rayStep, 5, sunGold); block(9 + rayStep, 6, sunGold)
        // Diagonal rays alternating cleanly with cardinal rays
        val diag = 1 - rayStep
        block(2 + diag, 2 + diag, sunBright)
        block(9 - diag, 2 + diag, sunBright)
        block(2 + diag, 9 - diag, sunBright)
        block(9 - diag, 9 - diag, sunBright)
      }

      WeatherConditionType.CLEAR_NIGHT -> {
        val crescent =
          listOf(
            "....####....",
            "...#####....",
            "..####......",
            "..###.......",
            "..###.......",
            "..####......",
            "...#####....",
            "....####....",
          )
        crescent.forEachIndexed { idx, row ->
          row.forEachIndexed { c, ch ->
            if (ch == '#') block(c, idx + 2, moonIndigo)
          }
        }
        // Twinkling pixel stars
        if (stepTick % 8 < 6) block(9, 2, sunBright)
        if (stepTick % 8 in 2..7) block(10, 6, rainCyan)
        if (stepTick % 8 != 4) block(8, 9, moonIndigo)
      }

      WeatherConditionType.PARTLY_CLOUDY -> {
        // Sun peeking top-right with pulsing pixel ray
        for (r in 2..4) for (c in 7..9) block(c, r, sunGold)
        val pRay = if (stepTick % 8 < 4) 0 else 1
        block(8, 1 - pRay, sunBright)
        block(10 + pRay, 3, sunBright)
        // Stepped cloud front
        drawPixelCloud(startRow = 5, shiftX = if (stepTick % 8 < 4) 0 else 1, cloudMain, cloudLight, ::block)
      }

      WeatherConditionType.OVERCAST -> {
        drawPixelCloud(startRow = 3, shiftX = 0, cloudLight, cloudMain, ::block)
        drawPixelCloud(startRow = 6, shiftX = if (stepTick % 8 < 4) 1 else 0, cloudMain, cloudLight, ::block)
      }

      WeatherConditionType.DRIZZLE,
      WeatherConditionType.RAIN,
      WeatherConditionType.HEAVY_RAIN -> {
        drawPixelCloud(startRow = 2, shiftX = 0, cloudMain, cloudLight, ::block)
        val dropShift = stepTick % 4
        val cols =
          if (condition == WeatherConditionType.HEAVY_RAIN) listOf(2, 4, 6, 8, 9)
          else listOf(3, 6, 8)
        cols.forEachIndexed { idx, c ->
          val r = 7 + ((dropShift + idx) % 4)
          block(c, r, if (idx % 2 == 0) rainCyan else rainDeep)
          if (condition == WeatherConditionType.HEAVY_RAIN) {
            block(c, (r + 2).coerceAtMost(11), rainDeep)
          }
        }
      }

      WeatherConditionType.THUNDERSTORM -> {
        drawPixelCloud(startRow = 1, shiftX = 0, cloudMain, cloudLight, ::block)
        val flashOn = stepTick % 4 != 2
        val boltColor = if (flashOn) boltYellow else rainCyan
        // Stepped lightning bolt
        block(6, 6, boltColor)
        block(5, 7, boltColor); block(6, 7, boltColor)
        block(4, 8, boltColor); block(5, 8, boltColor); block(6, 8, boltColor)
        block(5, 9, boltColor); block(6, 9, boltColor)
        block(4, 10, boltColor)
        block(3, 11, boltColor)
      }

      WeatherConditionType.SNOW -> {
        drawPixelCloud(startRow = 1, shiftX = 0, cloudMain, cloudLight, ::block)
        val step = stepTick % 5
        val flakes = listOf(2 to 7, 5 to 8, 8 to 7, 4 to 10, 9 to 10)
        flakes.forEachIndexed { idx, (c, baseR) ->
          val r = 6 + ((baseR - 6 + step + idx) % 5)
          val sway = if ((step + idx) % 2 == 0) 0 else 1
          block((c + sway).coerceAtMost(10), r, snowIce)
        }
      }

      WeatherConditionType.FOG -> {
        val shift = if (stepTick % 8 < 4) 0 else 1
        for (c in (1 + shift)..(9 + shift)) block(c, 3, cloudLight)
        for (c in (2 - shift)..(10 - shift)) block(c, 5, cloudMain)
        for (c in (1 + shift)..(8 + shift)) block(c, 7, cloudLight)
        for (c in (3 - shift)..(10 - shift)) block(c, 9, cloudMain)
      }
    }
  }
}

private inline fun drawPixelCloud(
  startRow: Int,
  shiftX: Int,
  primary: Color,
  highlight: Color,
  block: (Int, Int, Color) -> Unit,
) {
  val rows =
    listOf(
      "...####...",
      "..######..",
      ".########.",
      "##########",
    )
  rows.forEachIndexed { rIdx, str ->
    str.forEachIndexed { cIdx, ch ->
      if (ch == '#') {
        block(cIdx + 1 + shiftX, startRow + rIdx, if (rIdx == 0) highlight else primary)
      }
    }
  }
}

@Composable
fun BlockySmallIcon(
  kind: BlockyMetricIcon,
  tint: Color,
  modifier: Modifier = Modifier,
  size: Dp = 18.dp,
) {
  Canvas(modifier = modifier.size(size)) {
    val grid = 9
    val pitch = this.size.width / grid
    val px = pitch * 0.85f
    val pad = (pitch - px) * 0.5f

    fun pxBlock(c: Int, r: Int, alpha: Float = 1f) {
      if (c in 0 until grid && r in 0 until grid) {
        drawRect(
          color = tint.copy(alpha = alpha),
          topLeft = Offset(c * pitch + pad, r * pitch + pad),
          size = Size(px, px),
        )
      }
    }

    when (kind) {
      BlockyMetricIcon.REAL_FEEL -> {
        // Stepped pixel thermometer icon
        pxBlock(4, 0); pxBlock(4, 1)
        pxBlock(3, 2); pxBlock(5, 2)
        pxBlock(3, 3); pxBlock(4, 3); pxBlock(5, 3)
        pxBlock(3, 4); pxBlock(4, 4); pxBlock(5, 4)
        for (r in 5..7) for (c in 2..6) pxBlock(c, r)
        pxBlock(4, 8)
      }
      BlockyMetricIcon.AQI -> {
        // Stepped pixel air filter / lungs mask
        for (c in 1..7) pxBlock(c, 2)
        for (c in 2..6) { pxBlock(c, 3); pxBlock(c, 4) }
        pxBlock(3, 5); pxBlock(4, 5); pxBlock(5, 5)
        pxBlock(4, 6); pxBlock(4, 7)
        pxBlock(0, 1, 0.6f); pxBlock(8, 1, 0.6f)
      }
      BlockyMetricIcon.WIND -> {
        for (c in 1..6) pxBlock(c, 2)
        pxBlock(7, 1)
        for (c in 0..7) pxBlock(c, 4)
        for (c in 2..6) pxBlock(c, 6)
        pxBlock(6, 7)
      }
      BlockyMetricIcon.HUMIDITY -> {
        pxBlock(4, 1)
        pxBlock(3, 2); pxBlock(4, 2); pxBlock(5, 2)
        pxBlock(2, 3); pxBlock(3, 3); pxBlock(4, 3); pxBlock(5, 3); pxBlock(6, 3)
        for (r in 4..6) for (c in 2..6) pxBlock(c, r)
        for (c in 3..5) pxBlock(c, 7)
      }
      BlockyMetricIcon.UV -> {
        for (r in 3..5) for (c in 3..5) pxBlock(c, r)
        pxBlock(4, 1); pxBlock(4, 7); pxBlock(1, 4); pxBlock(7, 4)
        pxBlock(2, 2); pxBlock(6, 2); pxBlock(2, 6); pxBlock(6, 6)
      }
      BlockyMetricIcon.PRESSURE -> {
        for (c in 2..6) { pxBlock(c, 1); pxBlock(c, 7) }
        for (r in 2..6) { pxBlock(1, r); pxBlock(7, r) }
        pxBlock(4, 4); pxBlock(5, 3); pxBlock(6, 2)
      }
      BlockyMetricIcon.PRECIPITATION -> {
        for (c in 2..6) pxBlock(c, 2)
        for (c in 1..7) pxBlock(c, 3)
        pxBlock(2, 5); pxBlock(4, 6); pxBlock(6, 5); pxBlock(3, 7); pxBlock(5, 7)
      }
      BlockyMetricIcon.SOLAR -> {
        for (c in 0..8) pxBlock(c, 6, 0.5f)
        pxBlock(2, 5); pxBlock(3, 4); pxBlock(4, 3); pxBlock(5, 4); pxBlock(6, 5)
        pxBlock(4, 1)
      }
      BlockyMetricIcon.LOCATION -> {
        for (c in 3..5) pxBlock(c, 1)
        pxBlock(2, 2); pxBlock(6, 2)
        pxBlock(2, 3); pxBlock(4, 3); pxBlock(6, 3)
        pxBlock(2, 4); pxBlock(6, 4)
        pxBlock(3, 5); pxBlock(5, 5)
        pxBlock(4, 6); pxBlock(4, 7)
      }
      BlockyMetricIcon.SUN_THEME -> {
        for (r in 3..5) for (c in 3..5) pxBlock(c, r)
        pxBlock(4, 1); pxBlock(4, 7); pxBlock(1, 4); pxBlock(7, 4)
        pxBlock(2, 2); pxBlock(6, 2); pxBlock(2, 6); pxBlock(6, 6)
      }
      BlockyMetricIcon.MOON_THEME -> {
        for (c in 3..5) pxBlock(c, 1)
        for (c in 2..4) pxBlock(c, 2)
        for (c in 2..3) pxBlock(c, 3)
        for (c in 2..3) pxBlock(c, 4)
        for (c in 2..4) pxBlock(c, 5)
        for (c in 3..5) pxBlock(c, 6)
        pxBlock(6, 2, 0.7f); pxBlock(7, 4, 0.7f)
      }
      BlockyMetricIcon.ECO_BOLT -> {
        pxBlock(5, 1); pxBlock(4, 2); pxBlock(3, 3)
        for (c in 2..6) pxBlock(c, 4)
        pxBlock(5, 5); pxBlock(4, 6); pxBlock(3, 7)
      }
      BlockyMetricIcon.SETTINGS -> {
        // 9x9 stepped pixel gear icon
        // Cardinal gear teeth
        pxBlock(4, 0)
        pxBlock(4, 8)
        pxBlock(0, 4)
        pxBlock(8, 4)
        // Diagonal gear teeth
        pxBlock(1, 1); pxBlock(7, 1)
        pxBlock(1, 7); pxBlock(7, 7)
        // Outer gear ring (rows 2..6, cols 2..6 minus center 4,4)
        for (r in 2..6) {
          for (c in 2..6) {
            if (!(c == 4 && r == 4)) {
              pxBlock(c, r)
            }
          }
        }
      }
    }
  }
}
