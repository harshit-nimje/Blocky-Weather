package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AutoRefreshInterval
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily
import kotlin.math.roundToInt

@Composable
fun BlockyIntervalSliderCard(
  selectedInterval: AutoRefreshInterval,
  onIntervalChanged: (AutoRefreshInterval) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  val intervals = remember { AutoRefreshInterval.entries }
  val currentIndex = intervals.indexOf(selectedInterval).coerceIn(0, intervals.size - 1)

  PixelBlockContainer(
    modifier = modifier.fillMaxWidth().testTag("auto_refresh_slider_card"),
    isHighlighted = selectedInterval != AutoRefreshInterval.OFF,
    contentPadding = PaddingValues(16.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Top Status Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "AUTO TELEMETRY REFRESH",
            style =
              MaterialTheme.typography.labelLarge.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = tokens.inkPrimary,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text =
              if (selectedInterval == AutoRefreshInterval.OFF) {
                "Disabled · Manual pull-to-refresh only"
              } else {
                "Periodically fetch live telemetry in background"
              },
            style = MaterialTheme.typography.bodySmall,
            color = tokens.inkSecondary,
          )
        }

        // Active Badge
        Box(
          modifier =
            Modifier.border(
                width = 1.dp,
                color =
                  if (selectedInterval != AutoRefreshInterval.OFF) tokens.accentPrimary
                  else tokens.gridBorder,
              )
              .background(
                if (selectedInterval != AutoRefreshInterval.OFF) tokens.accentSoftFill
                else tokens.surfaceElevated
              )
              .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            text = selectedInterval.label,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              ),
            color =
              if (selectedInterval != AutoRefreshInterval.OFF) tokens.accentPrimary
              else tokens.inkMuted,
          )
        }
      }

      // Compact Sticky Blocky Track + Thumb
      BlockyStickySliderTrack(
        selectedIndex = currentIndex,
        totalSteps = intervals.size,
        onStepSelected = { idx ->
          onIntervalChanged(intervals[idx.coerceIn(0, intervals.size - 1)])
        },
      )

      // Discrete Step Labels Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        intervals.forEachIndexed { idx, interval ->
          val isSelected = idx == currentIndex
          val shortLabel =
            when (interval) {
              AutoRefreshInterval.OFF -> "OFF"
              AutoRefreshInterval.MIN_15 -> "15M"
              AutoRefreshInterval.MIN_30 -> "30M"
              AutoRefreshInterval.HOUR_1 -> "1H"
              AutoRefreshInterval.HOUR_3 -> "3H"
            }

          Box(
            modifier =
              Modifier.weight(1f)
                .pointerInput(idx) {
                  detectTapGestures {
                    onIntervalChanged(intervals[idx])
                  }
                }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = shortLabel,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = SilkscreenFontFamily,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                ),
              color = if (isSelected) tokens.accentPrimary else tokens.inkMuted,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BlockyStickySliderTrack(
  selectedIndex: Int,
  totalSteps: Int,
  onStepSelected: (Int) -> Unit,
) {
  val tokens = LocalGridcastTokens.current
  val animatedStep by
    animateFloatAsState(
      targetValue = selectedIndex.toFloat(),
      animationSpec = spring(stiffness = 600f, dampingRatio = 0.72f),
      label = "sticky_slider_step",
    )

  var isDragging by remember { mutableStateOf(false) }
  var dragPositionStep by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(36.dp)
        .pointerInput(totalSteps) {
          detectTapGestures { offset ->
            val stepWidth = size.width / (totalSteps - 1).toFloat()
            val clickedStep = (offset.x / stepWidth).roundToInt().coerceIn(0, totalSteps - 1)
            dragPositionStep = clickedStep.toFloat()
            onStepSelected(clickedStep)
          }
        }
        .pointerInput(totalSteps) {
          detectTransformGestures { _, pan, _, _ ->
            isDragging = true
            val stepWidth = size.width / (totalSteps - 1).toFloat()
            dragPositionStep = (dragPositionStep + pan.x / stepWidth).coerceIn(0f, (totalSteps - 1).toFloat())
            val closestStep = dragPositionStep.roundToInt().coerceIn(0, totalSteps - 1)
            onStepSelected(closestStep)
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
      val numGaps = totalSteps - 1
      val stepWidth = size.width / numGaps.toFloat()
      val trackHeightPx = 8.dp.toPx()
      val trackTopY = (size.height - trackHeightPx) / 2f

      // Track Background Frame
      drawRect(
        color = tokens.gridBorder,
        topLeft = Offset(0f, trackTopY),
        size = Size(size.width, trackHeightPx),
      )

      // Filled Active Track Progress
      val activeIndex = if (isDragging) dragPositionStep else animatedStep
      val fillWidth = activeIndex * stepWidth
      drawRect(
        color = tokens.accentPrimary,
        topLeft = Offset(0f, trackTopY),
        size = Size(fillWidth, trackHeightPx),
      )

      // Discrete Tick Stops
      for (i in 0 until totalSteps) {
        val tickX = i * stepWidth
        val isPassed = i <= selectedIndex
        val tickSizePx = 12.dp.toPx()
        val tickTopY = (size.height - tickSizePx) / 2f

        drawRect(
          color = if (isPassed) tokens.accentPrimary else tokens.gridBorder,
          topLeft = Offset((tickX - tickSizePx / 2f).coerceIn(0f, size.width - tickSizePx), tickTopY),
          size = Size(tickSizePx, tickSizePx),
        )
      }

      // Solid Sticky Pixel Thumb
      val thumbX = activeIndex * stepWidth
      val thumbSizePx = 20.dp.toPx()
      val thumbTopY = (size.height - thumbSizePx) / 2f
      val leftPos = (thumbX - thumbSizePx / 2f).coerceIn(0f, size.width - thumbSizePx)

      // Outer Thumb Border
      drawRect(
        color = tokens.inkPrimary,
        topLeft = Offset(leftPos, thumbTopY),
        size = Size(thumbSizePx, thumbSizePx),
      )
      // Inner Thumb Core
      val borderThickness = 3.dp.toPx()
      drawRect(
        color = tokens.accentPrimary,
        topLeft = Offset(leftPos + borderThickness, thumbTopY + borderThickness),
        size = Size(thumbSizePx - borderThickness * 2, thumbSizePx - borderThickness * 2),
      )
    }
  }
}
