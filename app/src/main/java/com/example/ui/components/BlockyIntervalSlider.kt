package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AutoRefreshInterval
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily
import kotlin.math.roundToInt

/**
 * Computes the snapped discrete step index in `0 until totalSteps` for a horizontal pointer coordinate.
 */
internal fun snapSliderOffsetToStep(
  offsetX: Float,
  totalWidthPx: Float,
  thumbSizePx: Float,
  totalSteps: Int,
): Int {
  if (totalSteps <= 1) return 0
  val margin = thumbSizePx * 0.5f
  val span = (totalWidthPx - thumbSizePx).coerceAtLeast(1f)
  val stepWidth = span / (totalSteps - 1).toFloat()
  return ((offsetX - margin) / stepWidth).roundToInt().coerceIn(0, totalSteps - 1)
}

@Composable
fun BlockyIntervalSliderCard(
  selectedInterval: AutoRefreshInterval,
  onIntervalChanged: (AutoRefreshInterval) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  val intervals = remember { AutoRefreshInterval.entries }
  val currentIndex = intervals.indexOf(selectedInterval).coerceIn(0, intervals.size - 1)
  val haptic = LocalHapticFeedback.current

  var snappedStep by remember(currentIndex) { mutableIntStateOf(currentIndex) }
  val displayedInterval = intervals[snappedStep.coerceIn(0, intervals.size - 1)]

  PixelBlockContainer(
    modifier = modifier.fillMaxWidth().testTag("auto_refresh_slider_card"),
    isHighlighted = displayedInterval != AutoRefreshInterval.OFF,
    contentPadding = PaddingValues(16.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
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
              if (displayedInterval == AutoRefreshInterval.OFF) {
                "Disabled · Manual pull-to-refresh only"
              } else {
                "Periodically fetch live telemetry in background"
              },
            style = MaterialTheme.typography.bodySmall,
            color = tokens.inkSecondary,
          )
        }

        // High-Contrast Active Interval Badge
        Box(
          modifier =
            Modifier.border(
                width = 1.5.dp,
                color =
                  if (displayedInterval != AutoRefreshInterval.OFF) tokens.accentPrimary
                  else tokens.gridBorder,
              )
              .background(
                if (displayedInterval != AutoRefreshInterval.OFF) tokens.background
                else tokens.surfaceElevated
              )
              .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Text(
            text = displayedInterval.label,
            style =
              MaterialTheme.typography.labelSmall.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              ),
            color =
              if (displayedInterval != AutoRefreshInterval.OFF) tokens.accentPrimary
              else tokens.inkMuted,
          )
        }
      }

      // Strictly Snapping Blocky Track + Thumb (jumps exclusively between discrete stop points)
      BlockyStickySliderTrack(
        snappedStep = snappedStep,
        totalSteps = intervals.size,
        onStepSnapped = { nextIdx ->
          val bounded = nextIdx.coerceIn(0, intervals.size - 1)
          if (bounded != snappedStep) {
            snappedStep = bounded
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onIntervalChanged(intervals[bounded])
          }
        },
      )

      // Discrete Step Labels Row aligned to stops
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        intervals.forEachIndexed { idx, interval ->
          val isSelected = idx == snappedStep
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
                .minimumInteractiveComponentSize()
                .testTag("auto_sync_step_$idx")
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null,
                ) {
                  if (idx != snappedStep) {
                    snappedStep = idx
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIntervalChanged(intervals[idx])
                  }
                }
                .padding(vertical = 4.dp),
            contentAlignment =
              when (idx) {
                0 -> Alignment.CenterStart
                intervals.size - 1 -> Alignment.CenterEnd
                else -> Alignment.Center
              },
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
              textAlign =
                when (idx) {
                  0 -> TextAlign.Start
                  intervals.size - 1 -> TextAlign.End
                  else -> TextAlign.Center
                },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BlockyStickySliderTrack(
  snappedStep: Int,
  totalSteps: Int,
  onStepSnapped: (Int) -> Unit,
) {
  val tokens = LocalGridcastTokens.current
  val currentSnapped by rememberUpdatedState(snappedStep)
  var rawDragStep by remember { mutableFloatStateOf(snappedStep.toFloat()) }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(44.dp)
        .testTag("auto_sync_slider_track")
        .pointerInput(totalSteps) {
          detectTapGestures { offset ->
            val thumbPx = 22.dp.toPx()
            val clickedStep =
              snapSliderOffsetToStep(
                offsetX = offset.x,
                totalWidthPx = size.width.toFloat(),
                thumbSizePx = thumbPx,
                totalSteps = totalSteps,
              )
            rawDragStep = clickedStep.toFloat()
            onStepSnapped(clickedStep)
          }
        }
        .pointerInput(totalSteps) {
          detectHorizontalDragGestures(
            onDragStart = { startOffset ->
              val thumbPx = 22.dp.toPx()
              val margin = thumbPx * 0.5f
              val span = (size.width.toFloat() - thumbPx).coerceAtLeast(1f)
              val stepWidth = span / (totalSteps - 1).coerceAtLeast(1).toFloat()
              val startRaw = ((startOffset.x - margin) / stepWidth).coerceIn(0f, (totalSteps - 1).toFloat())
              rawDragStep = startRaw
              val nextStep = startRaw.roundToInt().coerceIn(0, totalSteps - 1)
              onStepSnapped(nextStep)
            },
            onHorizontalDrag = { change, dragAmount ->
              change.consume()
              val thumbPx = 22.dp.toPx()
              val span = (size.width.toFloat() - thumbPx).coerceAtLeast(1f)
              val stepWidth = span / (totalSteps - 1).coerceAtLeast(1).toFloat()
              rawDragStep = (rawDragStep + dragAmount / stepWidth).coerceIn(0f, (totalSteps - 1).toFloat())
              val nextStep = rawDragStep.roundToInt().coerceIn(0, totalSteps - 1)
              onStepSnapped(nextStep)
            },
            onDragEnd = {
              rawDragStep = currentSnapped.toFloat()
            },
            onDragCancel = {
              rawDragStep = currentSnapped.toFloat()
            },
          )
        },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
      val thumbSizePx = 22.dp.toPx()
      val margin = thumbSizePx * 0.5f
      val numGaps = (totalSteps - 1).coerceAtLeast(1)
      val usableSpan = (size.width - thumbSizePx).coerceAtLeast(1f)
      val stepWidth = usableSpan / numGaps.toFloat()
      val trackHeightPx = 8.dp.toPx()
      val trackTopY = (size.height - trackHeightPx) / 2f

      fun stopCenterX(step: Int): Float = margin + step * stepWidth

      // Track Background Frame
      drawRect(
        color = tokens.gridBorder,
        topLeft = Offset(margin, trackTopY),
        size = Size(usableSpan, trackHeightPx),
      )

      // Filled Active Track Progress snapped strictly to current option point
      val activeCenterX = stopCenterX(snappedStep)
      drawRect(
        color = tokens.accentPrimary,
        topLeft = Offset(margin, trackTopY),
        size = Size((activeCenterX - margin).coerceAtLeast(0f), trackHeightPx),
      )

      // Discrete Option Tick Stops
      val tickSizePx = 12.dp.toPx()
      val tickTopY = (size.height - tickSizePx) / 2f
      for (i in 0 until totalSteps) {
        val cx = stopCenterX(i)
        val isPassed = i <= snappedStep
        drawRect(
          color = if (isPassed) tokens.accentPrimary else tokens.gridBorder,
          topLeft = Offset(cx - tickSizePx * 0.5f, tickTopY),
          size = Size(tickSizePx, tickSizePx),
        )
      }

      // Solid Square Pixel Thumb locked directly on snapped stop center
      val thumbTopY = (size.height - thumbSizePx) / 2f
      val leftPos = activeCenterX - thumbSizePx * 0.5f

      drawRect(
        color = tokens.inkPrimary,
        topLeft = Offset(leftPos, thumbTopY),
        size = Size(thumbSizePx, thumbSizePx),
      )
      val borderThickness = 3.dp.toPx()
      drawRect(
        color = tokens.accentPrimary,
        topLeft = Offset(leftPos + borderThickness, thumbTopY + borderThickness),
        size = Size(thumbSizePx - borderThickness * 2, thumbSizePx - borderThickness * 2),
      )
    }
  }
}
