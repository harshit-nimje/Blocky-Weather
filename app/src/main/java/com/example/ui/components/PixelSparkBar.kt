package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalGridcastTokens
import kotlin.math.roundToInt

/**
 * Discrete square-block meter snapped to integer steps without fluid interpolation.
 */
@Composable
fun PixelSegmentMeter(
  fraction: Float,
  segments: Int = 8,
  activeColor: Color? = null,
  modifier: Modifier = Modifier.fillMaxWidth().height(8.dp),
) {
  val tokens = LocalGridcastTokens.current
  val fillTint = activeColor ?: tokens.accentPrimary

  Canvas(modifier = modifier) {
    val gap = 3.dp.toPx()
    val blockW = ((size.width - gap * (segments - 1)) / segments).coerceAtLeast(2f)
    val blockH = size.height
    val litCount = (fraction.coerceIn(0f, 1f) * segments).roundToInt()

    for (i in 0 until segments) {
      val x = i * (blockW + gap)
      val isLit = i < litCount
      drawRect(
        color = if (isLit) fillTint else tokens.gridBorder,
        topLeft = Offset(x, 0f),
        size = Size(blockW, blockH),
      )
    }
  }
}

/**
 * Discrete square-cell thermal range bar for the 7-day forecast rows.
 */
@Composable
fun PixelThermalRangeBar(
  dayMinC: Double,
  dayMaxC: Double,
  weekMinC: Double,
  weekMaxC: Double,
  isExpanded: Boolean,
  modifier: Modifier = Modifier.width(84.dp).height(10.dp),
) {
  val tokens = LocalGridcastTokens.current
  val span = (weekMaxC - weekMinC).coerceAtLeast(1.0)
  val startFrac = ((dayMinC - weekMinC) / span).toFloat().coerceIn(0f, 1f)
  val endFrac = ((dayMaxC - weekMinC) / span).toFloat().coerceIn(startFrac, 1f)

  Canvas(modifier = modifier) {
    val cols = 10
    val gap = 2.dp.toPx()
    val blockW = ((size.width - gap * (cols - 1)) / cols).coerceAtLeast(2f)
    val blockH = size.height

    for (c in 0 until cols) {
      val pos = (c + 0.5f) / cols
      val inRange = pos >= startFrac - 0.05f && pos <= endFrac + 0.05f
      val color =
        if (!inRange) {
          tokens.gridBorder
        } else {
          if (isExpanded) tokens.accentPrimary else tokens.inkPrimary
        }
      drawRect(
        color = color,
        topLeft = Offset(c * (blockW + gap), 0f),
        size = Size(blockW, blockH),
      )
    }
  }
}
