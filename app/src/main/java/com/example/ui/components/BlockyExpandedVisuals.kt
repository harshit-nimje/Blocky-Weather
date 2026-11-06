package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.HourlyPoint
import com.example.domain.model.toCompassCardinal
import com.example.domain.model.toDisplayTemp
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily
import com.example.ui.theme.ensureMinContrastAgainst
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

internal fun accessibleStatusTextInk(color: Color, isDark: Boolean): Color {
  val referenceBg = if (isDark) Color(0xFF0A0B0F) else Color(0xFFF3F1EA)
  return ensureMinContrastAgainst(
    foreground = color,
    background = referenceBg,
    isDarkCanvas = isDark,
    minRatio = 4.6f,
  )
}

/**
 * Balanced multi-frame glide easing that traverses every intermediate grid cell smoothly
 * without front-loading 95% of motion into the first 100ms.
 */
private val BlockyGlideEase = CubicBezierEasing(0.22f, 0.04f, 0.20f, 1.0f)

/**
 * Upward Sunrise-to-Sunset arch curve rendered on a strict 1:1 square pixel grid,
 * with equal-size 2x2 square blocks for both the yellow Sun and blue-grey Moon, each
 * surrounded by a clear 1-block blank aura separating it from the arch line.
 */
@Composable
fun BlockySolarBowlVisual(
  solarProgress: Float,
  isDay: Boolean,
  sunriseLabel: String,
  sunsetLabel: String,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(solarProgress, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 1080, easing = BlockyGlideEase),
    )
  }

  val pulseTransition = rememberInfiniteTransition(label = "solar_pulse")
  val livePulse by
    pulseTransition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "solar_glow",
    )

  val interactionSource = remember { MutableInteractionSource() }
  val sunGold = Color(0xFFF59E0B)
  val sunBright = Color(0xFFFACC15)
  val moonBlueGrey = Color(0xFF7FA6D6)
  val moonInnerGrey = Color(0xFFA5BFCF)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("visual_solar_bowl")
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        ),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(76.dp)) {
      val rows = 12
      val cellPitch = size.height / rows
      val cols = (size.width / cellPitch).toInt().coerceIn(24, 48)
      val gridWidth = cols * cellPitch
      val originX = (size.width - gridWidth) * 0.5f
      val blockPx = cellPitch * 0.84f
      val pad = (cellPitch - blockPx) * 0.5f

      fun drawSquareCell(c: Int, r: Int, color: Color, spanCells: Float = 1f) {
        if (c in 0 until cols && r in 0 until rows) {
          val side = (cellPitch * spanCells - pad * 2f).coerceAtLeast(3f)
          drawRect(
            color = color,
            topLeft = Offset(originX + c * cellPitch + pad, r * cellPitch + pad),
            size = Size(side, side),
          )
        }
      }

      val horizonRow = rows - 3f
      val peakLift = 7.5f
      fun archRowForX(xNorm: Float): Float {
        val centered = 2f * xNorm - 1f
        val archHeightFactor = 1f - centered * centered
        return horizonRow - archHeightFactor * peakLift
      }

      val p = anim.value
      val clampedProgress = solarProgress.coerceIn(0.04f, 0.96f)
      val sunTarget =
        if (isDay) clampedProgress
        else if (clampedProgress < 0.5f) 0.86f else 0.14f
      val moonTarget =
        if (!isDay) clampedProgress
        else if (clampedProgress < 0.5f) {
          (0.64f + clampedProgress * 0.50f).coerceIn(0.64f, 0.88f)
        } else {
          (0.12f + (clampedProgress - 0.5f) * 0.50f).coerceIn(0.12f, 0.36f)
        }

      val currentSunNorm = (sunTarget * p).coerceIn(0f, 1f)
      val currentMoonNorm = (moonTarget * p).coerceIn(0f, 1f)

      val sunCenterCol = (1f + currentSunNorm * (cols - 3)).roundToInt().coerceIn(2, cols - 3)
      val sunCenterRow = archRowForX(currentSunNorm).roundToInt().coerceIn(2, rows - 3)
      val sunLeftCol = (sunCenterCol - 1).coerceIn(1, cols - 3)
      val sunTopRow = (sunCenterRow - 1).coerceIn(1, rows - 3)

      val moonCenterCol = (1f + currentMoonNorm * (cols - 3)).roundToInt().coerceIn(2, cols - 3)
      val moonCenterRow = archRowForX(currentMoonNorm).roundToInt().coerceIn(2, rows - 3)
      val moonLeftCol = (moonCenterCol - 1).coerceIn(1, cols - 3)
      val moonTopRow = (moonCenterRow - 1).coerceIn(1, rows - 3)

      fun isInBlankAura(c: Int, r: Int): Boolean {
        val nearSun = c in (sunLeftCol - 1)..(sunLeftCol + 2) && r in (sunTopRow - 1)..(sunTopRow + 2)
        val nearMoon =
          c in (moonLeftCol - 1)..(moonLeftCol + 2) && r in (moonTopRow - 1)..(moonTopRow + 2)
        return nearSun || nearMoon
      }

      // 0. Horizon baseline squares sweep in left-to-right during early frames
      val horizonR = horizonRow.roundToInt()
      val horizonSweep = (p * 2.1f).coerceIn(0f, 1f)
      val maxHorizonCol = (horizonSweep * (cols - 1)).roundToInt()
      for (c in 1..maxHorizonCol step 2) {
        if (!isInBlankAura(c, horizonR)) {
          drawSquareCell(c, horizonR, tokens.gridBorder.copy(alpha = 0.55f))
        }
      }

      // 1. Stepped Upward Arch Track builds block-by-block across columns
      val activePhaseNorm = if (isDay) currentSunNorm else currentMoonNorm
      val archRevealNorm = (p * 1.35f).coerceIn(0f, 1f)
      for (c in 1 until cols - 1) {
        val xNorm = (c - 1).toFloat() / (cols - 3).coerceAtLeast(1)
        if (xNorm > archRevealNorm) continue
        val rArch = archRowForX(xNorm).roundToInt()
        if (!isInBlankAura(c, rArch)) {
          val isLit = xNorm <= activePhaseNorm
          val trackColor =
            when {
              isLit && !isDay ->
                moonBlueGrey.copy(alpha = (if (c % 2 == 0) 0.90f else 0.72f) * livePulse)
              isLit && c % 2 == 0 -> sunGold
              isLit -> sunBright.copy(alpha = 0.85f * livePulse)
              else -> tokens.gridBorder
            }
          drawSquareCell(c, rArch, trackColor)
        }
      }

      if (!isInBlankAura(1, horizonR)) {
        drawSquareCell(1, horizonR, sunGold)
      }
      if (!isInBlankAura(cols - 2, horizonR) && archRevealNorm >= 0.9f) {
        drawSquareCell(cols - 2, horizonR, moonBlueGrey)
      }

      // 2. Clean 2x2 Blue-Grey Square Block for MOON (equal size to Sun, framed by 1-cell blank aura)
      drawSquareCell(moonLeftCol, moonTopRow, moonBlueGrey, spanCells = 2f)
      drawSquareCell(moonLeftCol, moonTopRow, moonInnerGrey, spanCells = 1f)

      // 3. Clean 2x2 Yellow Square Block for SUN (framed by 1-cell blank aura)
      drawSquareCell(sunLeftCol, sunTopRow, sunBright, spanCells = 2f)
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "☀ $sunriseLabel",
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = SilkscreenFontFamily,
            fontSize = 10.sp,
          ),
        color = accessibleStatusTextInk(sunGold, tokens.isDark),
        maxLines = 1,
        softWrap = false,
      )
      Text(
        text = if (isDay) "DAY ${(solarProgress * 100).roundToInt()}%" else "NIGHT ARCH",
        style = MaterialTheme.typography.labelSmall,
        color = tokens.inkMuted,
        maxLines = 1,
        softWrap = false,
      )
      Text(
        text = "$sunsetLabel ☾",
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = SilkscreenFontFamily,
            fontSize = 10.sp,
          ),
        color = accessibleStatusTextInk(moonBlueGrey, tokens.isDark),
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

/**
 * Color-coded blocky UV radiation meter (Low Green -> Moderate Yellow -> High Orange ->
 * Very High Red -> Extreme Violet) with an animated blocky pointer needle.
 */
@Composable
fun BlockyUvMeterVisual(
  uvIndex: Double,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(uvIndex, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 960, easing = BlockyGlideEase),
    )
  }

  val interactionSource = remember { MutableInteractionSource() }
  val p = anim.value
  val barRevealSweep = (p * 1.45f).coerceIn(0f, 1f)
  val needleSweep = ((p - 0.12f) / 0.88f).coerceIn(0f, 1f)
  val normalizedUv = (uvIndex / 12.0).toFloat().coerceIn(0f, 1f) * needleSweep

  val uvStatusText =
    when {
      uvIndex < 3.0 -> "SAFE · NO PROTECTION NEEDED"
      uvIndex < 6.0 -> "MODERATE · WEAR SPF 30+"
      uvIndex < 8.0 -> "HIGH RISK · SHADE & SPF 50"
      uvIndex < 11.0 -> "VERY HIGH · MINIMIZE MIDDAY SUN"
      else -> "EXTREME HAZARD · STAY INDOORS"
    }

  val uvStatusColor =
    when {
      uvIndex < 3.0 -> Color(0xFF22C55E)
      uvIndex < 6.0 -> Color(0xFFEAB308)
      uvIndex < 8.0 -> Color(0xFFF97316)
      uvIndex < 11.0 -> Color(0xFFEF4444)
      else -> Color(0xFFA855F7)
    }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("visual_uv_meter")
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        ),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(42.dp)) {
      val cols = 16
      val pitchX = size.width / cols
      val blockSize = minOf(pitchX * 0.84f, 11.dp.toPx())
      val padX = (pitchX - blockSize) * 0.5f

      fun bandColorForCol(c: Int): Color =
        when {
          c <= 2 -> Color(0xFF22C55E)
          c <= 6 -> Color(0xFFEAB308)
          c <= 10 -> Color(0xFFF97316)
          c <= 13 -> Color(0xFFEF4444)
          else -> Color(0xFFA855F7)
        }

      // Draw color-coded blocky meter strip illuminating column-by-column
      val barTopY = size.height - blockSize * 2f - 4.dp.toPx()
      for (c in 0 until cols) {
        val bandColor = bandColorForCol(c)
        val colNorm = c.toFloat() / (cols - 1)
        val colPop = ((barRevealSweep - colNorm * 0.72f) / 0.28f).coerceIn(0f, 1f)
        if (colPop <= 0.02f) continue

        val isActive = colNorm <= (uvIndex / 12.0).toFloat()
        val cellColor =
          if (isActive) bandColor.copy(alpha = colPop)
          else bandColor.copy(alpha = 0.28f * colPop)
        val curSide = blockSize * (0.45f + 0.55f * colPop)
        val sideOffset = (blockSize - curSide) * 0.5f

        drawRect(
          color = cellColor,
          topLeft = Offset(c * pitchX + padX + sideOffset, barTopY + sideOffset),
          size = Size(curSide, curSide),
        )
        drawRect(
          color = cellColor.copy(alpha = cellColor.alpha * 0.85f),
          topLeft =
            Offset(
              c * pitchX + padX + sideOffset,
              barTopY + blockSize + 2.dp.toPx() + sideOffset,
            ),
          size = Size(curSide, curSide),
        )
      }

      // Animated Blocky Needle / Pointer sweeping across columns to the exact UV column
      val targetCol = (normalizedUv * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
      val needleCenterX = targetCol * pitchX + pitchX * 0.5f
      val needlePx = minOf(blockSize * 0.55f, 7.dp.toPx())
      val dropOffset = (1f - needleSweep) * 6.dp.toPx()
      val tipY = barTopY - needlePx - 3.dp.toPx() - dropOffset

      drawRect(
        color = uvStatusColor,
        topLeft = Offset(needleCenterX - needlePx * 0.5f, tipY),
        size = Size(needlePx, needlePx),
      )
      for (dx in -1..1) {
        drawRect(
          color = uvStatusColor,
          topLeft =
            Offset(
              needleCenterX + dx * needlePx - needlePx * 0.5f,
              tipY - needlePx - 1.dp.toPx(),
            ),
          size = Size(needlePx, needlePx),
        )
      }
      for (step in 2..3) {
        drawRect(
          color = tokens.inkPrimary,
          topLeft =
            Offset(
              needleCenterX - needlePx * 0.5f,
              tipY - step * (needlePx + 1.dp.toPx()),
            ),
          size = Size(needlePx, needlePx),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "0 SAFE",
        style = MaterialTheme.typography.labelSmall,
        color = accessibleStatusTextInk(Color(0xFF22C55E), tokens.isDark),
        maxLines = 1,
        softWrap = false,
      )
      Text(
        text = "3 MOD",
        style = MaterialTheme.typography.labelSmall,
        color = accessibleStatusTextInk(Color(0xFFEAB308), tokens.isDark),
        maxLines = 1,
        softWrap = false,
      )
      Text(
        text = "7 HIGH",
        style = MaterialTheme.typography.labelSmall,
        color = accessibleStatusTextInk(Color(0xFFF97316), tokens.isDark),
        maxLines = 1,
        softWrap = false,
      )
      Text(
        text = "11+ EXT",
        style = MaterialTheme.typography.labelSmall,
        color = accessibleStatusTextInk(Color(0xFFA855F7), tokens.isDark),
        maxLines = 1,
        softWrap = false,
      )
    }

    Text(
      text = uvStatusText,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontFamily = SilkscreenFontFamily,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
        ),
      color = accessibleStatusTextInk(uvStatusColor, tokens.isDark),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Compact single-row color-coded UV radiation bar + pointer block for compact day/hour drawers.
 */
@Composable
fun BlockyCompactUvStrip(
  uvIndex: Double,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  val anim = remember { Animatable(0f) }
  LaunchedEffect(uvIndex) {
    anim.snapTo(0f)
    delay(100L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 820, easing = BlockyGlideEase),
    )
  }

  val p = anim.value
  val normalizedUv = (uvIndex / 12.0).toFloat().coerceIn(0f, 1f) * p
  val (uvBadge, uvColor) =
    when {
      uvIndex < 3.0 -> "UV ${uvIndex.roundToInt()} LOW" to Color(0xFF22C55E)
      uvIndex < 6.0 -> "UV ${uvIndex.roundToInt()} MOD" to Color(0xFFEAB308)
      uvIndex < 8.0 -> "UV ${uvIndex.roundToInt()} HIGH" to Color(0xFFF97316)
      uvIndex < 11.0 -> "UV ${uvIndex.roundToInt()} V.HIGH" to Color(0xFFEF4444)
      else -> "UV ${uvIndex.roundToInt()} EXT" to Color(0xFFA855F7)
    }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Canvas(modifier = Modifier.weight(1f).height(20.dp)) {
      val cols = 12
      val pitchX = size.width / cols
      val blockPx = minOf(pitchX * 0.80f, 9.dp.toPx())
      val padX = (pitchX - blockPx) * 0.5f
      val barY = size.height - blockPx - 1.dp.toPx()

      for (c in 0 until cols) {
        val colNorm = c.toFloat() / (cols - 1)
        val cellAlpha = ((p * 1.4f - colNorm * 0.7f) / 0.3f).coerceIn(0f, 1f)
        if (cellAlpha <= 0.02f) continue
        val bandColor =
          when {
            c <= 2 -> Color(0xFF22C55E)
            c <= 5 -> Color(0xFFEAB308)
            c <= 8 -> Color(0xFFF97316)
            c <= 10 -> Color(0xFFEF4444)
            else -> Color(0xFFA855F7)
          }
        val active = colNorm <= (uvIndex / 12.0).toFloat()
        drawRect(
          color = if (active) bandColor.copy(alpha = cellAlpha) else bandColor.copy(alpha = 0.25f * cellAlpha),
          topLeft = Offset(c * pitchX + padX, barY),
          size = Size(blockPx, blockPx),
        )
      }

      val targetCol = (normalizedUv * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
      drawRect(
        color = uvColor,
        topLeft = Offset(targetCol * pitchX + padX, barY - blockPx - 2.dp.toPx()),
        size = Size(blockPx, blockPx),
      )
    }

    Text(
      text = uvBadge,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontFamily = SilkscreenFontFamily,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
        ),
      color = accessibleStatusTextInk(uvColor, tokens.isDark),
      maxLines = 1,
      softWrap = false,
    )
  }
}

/**
 * Spatial pixel-art wind compass (N / E / S / W) with a bold blocky arrow pointing spatially
 * in the exact meteorological wind direction plus animated gust bars.
 */
@Composable
fun BlockyWindCompassVisual(
  windSpeedKmh: Double,
  windGustsKmh: Double,
  windDirectionDeg: Int,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(windDirectionDeg, windSpeedKmh, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 1020, easing = BlockyGlideEase),
    )
  }

  val interactionSource = remember { MutableInteractionSource() }
  val windCyan = Color(0xFF38BDF8)
  val gustAmber = Color(0xFFF59E0B)
  val p = anim.value

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("visual_wind_compass")
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Canvas(modifier = Modifier.size(68.dp)) {
      val grid = 15
      val center = (grid - 1) / 2f
      val pitch = size.width / grid
      val px = pitch * 0.84f
      val pad = (pitch - px) * 0.5f

      fun cell(c: Int, r: Int, color: Color) {
        if (c in 0 until grid && r in 0 until grid) {
          drawRect(
            color = color,
            topLeft = Offset(c * pitch + pad, r * pitch + pad),
            size = Size(px, px),
          )
        }
      }

      // Outer stepped compass ring builds clockwise around 360°
      val ringSweep = (p * 1.45f).coerceIn(0f, 1f)
      for (r in 0 until grid) {
        for (c in 0 until grid) {
          val dx = c - center
          val dy = r - center
          val dist = kotlin.math.sqrt(dx * dx + dy * dy)
          if (dist in 5.8f..6.7f) {
            val angleNorm =
              (((kotlin.math.atan2(dy.toDouble(), dx.toDouble()) + PI / 2.0 + 2 * PI) % (2 * PI)) /
                  (2 * PI))
                .toFloat()
            if (angleNorm <= ringSweep) {
              cell(c, r, tokens.gridBorder)
            }
          }
        }
      }

      // Cardinal anchors N / E / S / W
      cell(7, 0, tokens.accentPrimary); cell(7, 1, tokens.accentPrimary)
      if (ringSweep >= 0.25f) {
        cell(14, 7, tokens.inkPrimary); cell(13, 7, tokens.inkMuted)
      }
      if (ringSweep >= 0.50f) {
        cell(7, 14, tokens.inkPrimary); cell(7, 13, tokens.inkMuted)
      }
      if (ringSweep >= 0.75f) {
        cell(0, 7, tokens.inkPrimary); cell(1, 7, tokens.inkMuted)
      }

      // Sweep bold blocky arrow across 120° arc into exact meteorological wind direction
      val arrowSweep = ((p - 0.10f) / 0.90f).coerceIn(0f, 1f)
      val animatedDeg = windDirectionDeg.toFloat() - (1f - arrowSweep) * 120f
      val rad = animatedDeg * (PI / 180.0)
      val sinA = sin(rad).toFloat()
      val cosA = cos(rad).toFloat()

      val arrowLocalCoords =
        listOf(
          0f to -4f,
          -1f to -3f, 0f to -3f, 1f to -3f,
          -2f to -2f, 0f to -2f, 2f to -2f,
          0f to -1f,
          0f to 0f,
          0f to 1f,
          0f to 2f,
          -1f to 3f, 0f to 3f, 1f to 3f,
        )

      arrowLocalCoords.forEachIndexed { idx, (lx, ly) ->
        val rx = lx * cosA - ly * sinA
        val ry = lx * sinA + ly * cosA
        val gc = (center + rx).roundToInt()
        val gr = (center + ry).roundToInt()
        val isHead = idx < 7
        cell(gc, gr, if (isHead) tokens.accentPrimary else tokens.inkPrimary)
      }
      cell(7, 7, windCyan)
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "DIR ${windDirectionDeg}° ${windDirectionDeg.toCompassCardinal()}",
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = SilkscreenFontFamily,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
            ),
          color = tokens.accentPrimary,
          maxLines = 1,
          softWrap = false,
        )
        Text(
          text = "N↑ E→ S↓ W←",
          style = MaterialTheme.typography.labelSmall,
          color = tokens.inkMuted,
          maxLines = 1,
          softWrap = false,
        )
      }

      Text(
        text = "SPEED ${windSpeedKmh.roundToInt()} KM/H · GUST ${windGustsKmh.roundToInt()}",
        style = MaterialTheme.typography.labelSmall,
        color = tokens.inkSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      PixelSegmentMeter(
        fraction = ((windSpeedKmh / 60.0).toFloat().coerceIn(0.08f, 1f)) * p,
        segments = 10,
        activeColor = windCyan,
        modifier = Modifier.fillMaxWidth().height(6.dp),
      )
      val gustSweep = ((p - 0.14f) / 0.86f).coerceIn(0f, 1f)
      PixelSegmentMeter(
        fraction = ((windGustsKmh / 80.0).toFloat().coerceIn(0.10f, 1f)) * gustSweep,
        segments = 10,
        activeColor = gustAmber,
        modifier = Modifier.fillMaxWidth().height(5.dp),
      )
    }
  }
}

/**
 * Pixel moisture chamber / humidity gauge filling upward block-by-block with rising vapor pixels.
 */
@Composable
fun BlockyHumidityGaugeVisual(
  humidityPercent: Int,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(humidityPercent, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 920, easing = BlockyGlideEase),
    )
  }

  val interactionSource = remember { MutableInteractionSource() }
  val aqua = Color(0xFF0EA5E9)
  val aquaBright = Color(0xFF38BDF8)
  val p = anim.value
  val targetRatio = (humidityPercent / 100f).coerceIn(0f, 1f) * p

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Canvas(modifier = Modifier.weight(1f).height(26.dp)) {
      val cols = 20
      val rows = 2
      val pitchX = size.width / cols
      val pitchY = size.height / rows
      val px = minOf(pitchX, pitchY) * 0.84f
      val litCols = (targetRatio * cols).roundToInt()

      for (c in 0 until cols) {
        val colReveal = ((p * 1.6f - (c.toFloat() / cols) * 0.7f) / 0.3f).coerceIn(0f, 1f)
        if (colReveal <= 0.02f) continue
        for (r in 0 until rows) {
          // Bottom row leads top row slightly for a staggered liquid fill
          val rowColLimit = if (r == 1) litCols else (litCols - 1).coerceAtLeast(0)
          val isLit = c < rowColLimit
          val color =
            when {
              !isLit -> tokens.gridBorder.copy(alpha = colReveal)
              c % 3 == 0 -> aquaBright
              else -> aqua
            }
          drawRect(
            color = color,
            topLeft = Offset(c * pitchX + (pitchX - px) * 0.5f, r * pitchY + (pitchY - px) * 0.5f),
            size = Size(px, px),
          )
        }
      }
    }
    Text(
      text = "$humidityPercent% SAT",
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontFamily = SilkscreenFontFamily,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
        ),
      color = accessibleStatusTextInk(aquaBright, tokens.isDark),
    )
  }
}

/**
 * Stepped pixel barometer scale (975..1035 hPa) with animated blocky needle indicating
 * low/normal/high pressure system stability.
 */
@Composable
fun BlockyBarometerVisual(
  pressureHpa: Double,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(pressureHpa, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 940, easing = BlockyGlideEase),
    )
  }

  val interactionSource = remember { MutableInteractionSource() }
  val p = anim.value
  val barSweep = (p * 1.5f).coerceIn(0f, 1f)
  val needleSweep = ((p - 0.14f) / 0.86f).coerceIn(0f, 1f)
  val ratio = ((pressureHpa - 975.0) / 60.0).toFloat().coerceIn(0f, 1f) * needleSweep
  val barColor = if (pressureHpa >= 1013.0) Color(0xFF4ADE80) else Color(0xFFF59E0B)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        ),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
      val cols = 19
      val pitchX = size.width / cols
      val px = minOf(pitchX * 0.82f, 11.dp.toPx())
      val padX = (pitchX - px) * 0.5f
      val barY = size.height - px - 2.dp.toPx()

      val centerIdx = cols / 2
      for (c in 0 until cols) {
        val distNorm = abs(c - centerIdx).toFloat() / centerIdx.coerceAtLeast(1)
        if (distNorm > barSweep) continue
        val isCenterRef = c == centerIdx
        val color =
          when {
            isCenterRef -> tokens.inkPrimary
            c < centerIdx -> Color(0xFFF59E0B).copy(alpha = 0.50f)
            else -> Color(0xFF4ADE80).copy(alpha = 0.50f)
          }
        drawRect(
          color = color,
          topLeft = Offset(c * pitchX + padX, barY),
          size = Size(px, px),
        )
      }

      val needleCol = (ratio * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
      val nx = needleCol * pitchX + padX
      drawRect(color = barColor, topLeft = Offset(nx, barY - px - 2.dp.toPx()), size = Size(px, px))
      drawRect(
        color = tokens.inkPrimary,
        topLeft = Offset(nx, barY - 2f * (px + 2.dp.toPx())),
        size = Size(px, px),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = "975 LOW", style = MaterialTheme.typography.labelSmall, color = tokens.inkMuted)
      Text(
        text = "1013 NORMAL",
        style = MaterialTheme.typography.labelSmall,
        color = tokens.inkSecondary,
      )
      Text(text = "1035 HIGH", style = MaterialTheme.typography.labelSmall, color = tokens.inkMuted)
    }
  }
}

/**
 * Stepped 12-hour pixel rain probability waveform for the Precipitation card.
 */
@Composable
fun BlockyRainMatrixVisual(
  hourly: List<HourlyPoint>,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(hourly, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 960, easing = BlockyGlideEase),
    )
  }

  val interactionSource = remember { MutableInteractionSource() }
  val slice = remember(hourly) { hourly.take(12) }
  if (slice.isEmpty()) return

  val rainColor = Color(0xFF38BDF8)

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(34.dp)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        )
  ) {
    val cols = slice.size
    val maxRows = 4
    val pitchX = size.width / cols
    val pitchY = size.height / maxRows
    val px = minOf(pitchX * 0.78f, pitchY * 0.84f)
    val padX = (pitchX - px) * 0.5f
    val padY = (pitchY - px) * 0.5f
    val p = anim.value

    slice.forEachIndexed { c, pt ->
      // Staggered column wave so each hour rises one after another across ~1s
      val colWave = ((p * 1.45f - c * 0.04f) / 1.0f).coerceIn(0f, 1f)
      val targetRows =
        ((pt.precipProbPercent / 100f) * maxRows)
          .roundToInt()
          .coerceAtLeast(if (pt.precipProbPercent > 0) 1 else 0)
      val activeRows = (targetRows * colWave).roundToInt()

      for (r in 0 until maxRows) {
        val fromBottom = (maxRows - 1) - r
        val isLit = fromBottom < activeRows
        drawRect(
          color = if (isLit) rainColor else tokens.gridBorder.copy(alpha = 0.6f),
          topLeft = Offset(c * pitchX + padX, r * pitchY + padY),
          size = Size(px, px),
        )
      }
    }
  }
}

/**
 * Pixel Real Feel vs Actual Temperature thermal differential meter.
 */
@Composable
fun BlockyRealFeelVisual(
  actualTempC: Double,
  apparentTempC: Double,
  dewPointC: Double,
  useFahrenheit: Boolean,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(actualTempC, apparentTempC, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 940, easing = BlockyGlideEase),
    )
  }

  val interactionSource = remember { MutableInteractionSource() }
  val diffC = apparentTempC - actualTempC
  val diffLabel =
    when {
      abs(diffC) < 0.5 -> "MATCHES ACTUAL"
      diffC > 0 -> "+${String.format(Locale.US, "%.1f", diffC)}° MUGGY HEAT"
      else -> "${String.format(Locale.US, "%.1f", diffC)}° WIND CHILL"
    }

  val diffColor =
    when {
      diffC > 1.5 -> Color(0xFFF7C6A8)
      diffC < -1.5 -> Color(0xFF38BDF8)
      else -> Color(0xFF4ADE80)
    }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        ),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
      val cols = 16
      val pitchX = size.width / cols
      val px = minOf(pitchX * 0.82f, 10.dp.toPx())
      val padX = (pitchX - px) * 0.5f

      val minT = minOf(actualTempC, apparentTempC) - 4.0
      val maxT = maxOf(actualTempC, apparentTempC) + 4.0
      val span = (maxT - minT).coerceAtLeast(8.0)

      val actCol = (((actualTempC - minT) / span) * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
      val appCol = (((apparentTempC - minT) / span) * (cols - 1)).roundToInt().coerceIn(0, cols - 1)

      val p = anim.value
      val scaleReveal = (p * 1.55f).coerceIn(0f, 1f)
      val travelSweep = ((p - 0.18f) / 0.82f).coerceIn(0f, 1f)
      val animatedAppCol =
        (actCol + (appCol - actCol) * travelSweep).roundToInt().coerceIn(0, cols - 1)

      val startCol = minOf(actCol, animatedAppCol)
      val endCol = maxOf(actCol, animatedAppCol)

      for (c in 0 until cols) {
        val colNorm = c.toFloat() / (cols - 1)
        if (colNorm > scaleReveal) continue
        val inRange = c in startCol..endCol
        val color =
          when {
            c == animatedAppCol && travelSweep > 0.05f -> diffColor
            c == actCol -> tokens.inkPrimary
            inRange -> diffColor.copy(alpha = 0.55f)
            else -> tokens.gridBorder.copy(alpha = 0.40f)
          }
        drawRect(
          color = color,
          topLeft = Offset(c * pitchX + padX, size.height - px - 2.dp.toPx()),
          size = Size(px, px),
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "ACTUAL ${actualTempC.toDisplayTemp(useFahrenheit)}°",
        style = MaterialTheme.typography.labelSmall,
        color = tokens.inkMuted,
      )
      Text(
        text = diffLabel,
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = SilkscreenFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
          ),
        color = accessibleStatusTextInk(diffColor, tokens.isDark),
      )
      Text(
        text = "FEELS ${apparentTempC.toDisplayTemp(useFahrenheit)}°",
        style = MaterialTheme.typography.labelSmall,
        color = tokens.inkPrimary,
      )
    }
  }
}

/**
 * 5-Stage color-coded Air Quality (AQI) blocky spectrum meter with animated pointer needle.
 */
@Composable
fun BlockyAqiMeterVisual(
  usAqi: Int,
  pm25: Double?,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  var replayTrigger by remember { mutableIntStateOf(0) }
  val anim = remember { Animatable(0f) }

  LaunchedEffect(usAqi, replayTrigger) {
    anim.snapTo(0f)
    if (replayTrigger == 0) delay(120L)
    anim.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 980, easing = BlockyGlideEase),
    )
  }

  val interactionSource = remember { MutableInteractionSource() }
  val p = anim.value
  val barSweep = (p * 1.45f).coerceIn(0f, 1f)
  val needleSweep = ((p - 0.14f) / 0.86f).coerceIn(0f, 1f)
  val normAqi = (usAqi / 200.0).toFloat().coerceIn(0f, 1f) * needleSweep

  val (statusLabel, statusColor) =
    when {
      usAqi <= 50 -> "GOOD · CLEAN AIR" to Color(0xFF22C55E)
      usAqi <= 100 -> "MODERATE · OK" to Color(0xFFEAB308)
      usAqi <= 150 -> "UNHEALTHY SENSITIVE" to Color(0xFFF97316)
      usAqi <= 200 -> "UNHEALTHY · MASK UP" to Color(0xFFEF4444)
      else -> "HAZARDOUS · STAY IN" to Color(0xFFA855F7)
    }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = { replayTrigger++ },
        ),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
      val cols = 15
      val pitchX = size.width / cols
      val px = minOf(pitchX * 0.82f, 10.dp.toPx())
      val padX = (pitchX - px) * 0.5f
      val barY = size.height - px - 2.dp.toPx()

      for (c in 0 until cols) {
        val colNorm = c.toFloat() / (cols - 1)
        val colReveal = ((barSweep - colNorm * 0.7f) / 0.3f).coerceIn(0f, 1f)
        if (colReveal <= 0.02f) continue

        val colAqi = colNorm * 200.0
        val bandColor =
          when {
            colAqi <= 50 -> Color(0xFF22C55E)
            colAqi <= 100 -> Color(0xFFEAB308)
            colAqi <= 150 -> Color(0xFFF97316)
            colAqi <= 200 -> Color(0xFFEF4444)
            else -> Color(0xFFA855F7)
          }
        val active = colAqi <= usAqi
        drawRect(
          color = if (active) bandColor.copy(alpha = colReveal) else bandColor.copy(alpha = 0.28f * colReveal),
          topLeft = Offset(c * pitchX + padX, barY),
          size = Size(px, px),
        )
      }

      val targetCol = (normAqi * (cols - 1)).roundToInt().coerceIn(0, cols - 1)
      val nx = targetCol * pitchX + padX
      drawRect(
        color = statusColor,
        topLeft = Offset(nx, barY - px - 2.dp.toPx()),
        size = Size(px, px),
      )
      drawRect(
        color = tokens.inkPrimary,
        topLeft = Offset(nx, barY - 2f * (px + 2.dp.toPx())),
        size = Size(px, px),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "0 GOOD",
        style = MaterialTheme.typography.labelSmall,
        color = accessibleStatusTextInk(Color(0xFF22C55E), tokens.isDark),
      )
      Text(
        text = "100 MOD",
        style = MaterialTheme.typography.labelSmall,
        color = accessibleStatusTextInk(Color(0xFFEAB308), tokens.isDark),
      )
      Text(
        text = "200+ BAD",
        style = MaterialTheme.typography.labelSmall,
        color = accessibleStatusTextInk(Color(0xFFA855F7), tokens.isDark),
      )
    }

    Text(
      text = statusLabel + (pm25?.let { " · PM2.5 ${String.format(Locale.US, "%.1f", it)} µg/m³" } ?: ""),
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontFamily = SilkscreenFontFamily,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
        ),
      color = accessibleStatusTextInk(statusColor, tokens.isDark),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

