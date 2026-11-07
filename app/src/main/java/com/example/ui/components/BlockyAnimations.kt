package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlinx.coroutines.delay

private val BlockyArchitecturalEasing = CubicBezierEasing(0.16f, 0.92f, 0.24f, 1.0f)

/**
 * Staggered blocky-architectural on-screen enter transition.
 * Blends a sleek cubic settle with quantized grid-step translation and scale so cards snap
 * crisply into their grid slots while remaining high-FPS and elegant.
 */
@Composable
fun BlockyEnterReveal(
  index: Int,
  modifier: Modifier = Modifier,
  reduceAnimations: Boolean = false,
  content: @Composable () -> Unit,
) {
  if (reduceAnimations) {
    Box(modifier = modifier) {
      content()
    }
    return
  }

  val progress = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    val staggerMs = (index.coerceIn(0, 12) * 34L)
    if (staggerMs > 0L) delay(staggerMs)
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 280, easing = BlockyArchitecturalEasing),
    )
  }

  val p = progress.value
  // Blend smooth progression with 8 discrete architectural grid micro-steps
  val quantized = (floor(p * 8f) / 8f) * 0.34f + p * 0.66f

  Box(
    modifier =
      modifier.graphicsLayer {
        alpha = p
        translationY = (1f - quantized) * 32f
        scaleX = 0.955f + 0.045f * quantized
        scaleY = 0.955f + 0.045f * quantized
      }
  ) {
    content()
  }
}

/**
 * Staggered word-by-word architectural entry for hero quips & status headlines.
 * Each word snaps upward onto its baseline in a fluidly-blocky sequence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockyWordStaggerText(
  text: String,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  baseDelayMs: Long = 80L,
  perWordStaggerMs: Long = 38L,
  reduceAnimations: Boolean = false,
) {
  val words = remember(text) { text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() } }

  key(text) {
    FlowRow(
      modifier = modifier,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      words.forEachIndexed { idx, word ->
        if (reduceAnimations) {
          Text(
            text = word,
            style = style,
            color = color,
          )
        } else {
          val wordProgress = remember(idx) { Animatable(0f) }
          LaunchedEffect(idx) {
            val waitMs = baseDelayMs + idx * perWordStaggerMs
            if (waitMs > 0L) delay(waitMs)
            wordProgress.animateTo(
              targetValue = 1f,
              animationSpec = tween(durationMillis = 220, easing = BlockyArchitecturalEasing),
            )
          }
          val wp = wordProgress.value
          val stepSnap = (floor(wp * 5f) / 5f) * 0.42f + wp * 0.58f

          Text(
            text = word,
            style = style,
            color = color,
            modifier =
              Modifier.graphicsLayer {
                alpha = wp
                translationY = (1f - stepSnap) * 16f
                translationX = (1f - stepSnap) * 6f
              },
          )
        }
      }
    }
  }
}

/**
 * Auto-scrolling marquee text for handling long strings without clipping.
 */
@Composable
fun BlockyMarqueeText(
  text: String,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  delayMillis: Int = 2000
) {
  val scrollState = androidx.compose.foundation.rememberScrollState()
  var shouldAnimate by remember { mutableStateOf(true) }

  LaunchedEffect(text, shouldAnimate) {
    if (shouldAnimate) {
      while (true) {
        delay(delayMillis.toLong())
        scrollState.animateScrollTo(
          scrollState.maxValue,
          animationSpec = tween(durationMillis = scrollState.maxValue * 15, easing = LinearEasing)
        )
        delay(delayMillis.toLong())
        scrollState.scrollTo(0)
      }
    }
  }

  androidx.compose.foundation.layout.Row(
    modifier = modifier
      .horizontalScroll(scrollState, enabled = false)
  ) {
    Text(
      text = text,
      style = style,
      color = color,
      maxLines = 1,
      overflow = TextOverflow.Visible,
      modifier = Modifier.onSizeChanged { 
        shouldAnimate = scrollState.maxValue > 0
      }
    )
  }
}
