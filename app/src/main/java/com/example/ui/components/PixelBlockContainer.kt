package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalGridcastTokens

/**
 * Clean, flat, sharp-cornered (0dp) architectural block container aligned to the grid system.
 * Uses crisp discrete state snaps rather than fluid animations.
 */
@Composable
fun PixelBlockContainer(
  modifier: Modifier = Modifier,
  isHighlighted: Boolean = false,
  isExpanded: Boolean = false,
  fillOverride: Color? = null,
  chamferStep: Dp = 0.dp,
  contentPadding: PaddingValues = PaddingValues(14.dp),
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val tokens = LocalGridcastTokens.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val active = isHighlighted || isExpanded || isPressed
  val borderColor =
    when {
      isPressed -> tokens.gridBorderStrong
      active -> tokens.accentPrimary
      else -> tokens.gridBorder
    }

  val backgroundColor =
    fillOverride
      ?: when {
        isPressed -> tokens.surfaceElevated
        isHighlighted -> tokens.accentSoftFill
        else -> tokens.surfaceTranslucent
      }

  val clickableModifier =
    if (onClick != null) {
      Modifier.minimumInteractiveComponentSize()
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
    } else {
      Modifier
    }

  Box(
    modifier =
      modifier
        .background(backgroundColor)
        .border(width = if (active) 1.5.dp else 1.dp, color = borderColor)
        .then(clickableModifier)
        .padding(contentPadding),
    content = content,
  )
}
