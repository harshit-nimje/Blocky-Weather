package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily

/**
 * Custom restyled Android toggle switch strictly adhering to 0dp architectural pixel geometry:
 * crisp outer rectangular track + sharp square thumb block that snaps across on toggle.
 */
@Composable
fun BlockySwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val tokens = LocalGridcastTokens.current
  val interactionSource = remember { MutableInteractionSource() }

  val thumbOffsetX by
    animateDpAsState(
      targetValue = if (checked) 28.dp else 4.dp,
      animationSpec =
        spring(
          stiffness = Spring.StiffnessMediumLow,
          dampingRatio = 0.80f,
        ),
      label = "blocky_switch_offset",
    )

  val trackFill by
    animateColorAsState(
      targetValue = if (checked) tokens.accentSoftFill else tokens.surfaceElevated,
      animationSpec = tween(durationMillis = 160),
      label = "blocky_switch_track",
    )

  val trackBorder by
    animateColorAsState(
      targetValue = if (checked) tokens.accentPrimary else tokens.gridBorderStrong,
      animationSpec = tween(durationMillis = 160),
      label = "blocky_switch_border",
    )

  val thumbColor by
    animateColorAsState(
      targetValue = if (checked) tokens.accentPrimary else tokens.inkMuted,
      animationSpec = tween(durationMillis = 160),
      label = "blocky_switch_thumb",
    )

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .toggleable(
          value = checked,
          enabled = enabled,
          role = Role.Switch,
          interactionSource = interactionSource,
          indication = null,
          onValueChange = onCheckedChange,
        ),
    contentAlignment = Alignment.Center,
  ) {
    // 52dp x 28dp sharp 0dp blocky switch track
    Box(
      modifier =
        Modifier.width(52.dp)
          .height(28.dp)
          .border(width = if (checked) 2.dp else 1.5.dp, color = trackBorder)
          .background(trackFill),
      contentAlignment = Alignment.CenterStart,
    ) {
      // Subtle pixel notch markers inside the track
      Box(
        modifier =
          Modifier.offset(x = if (checked) 9.dp else 39.dp)
            .size(4.dp)
            .background(trackBorder.copy(alpha = 0.45f))
      )

      // 20dp x 20dp sharp square blocky thumb
      Box(
        modifier =
          Modifier.offset(x = thumbOffsetX)
            .size(20.dp)
            .border(1.dp, tokens.inkPrimary)
            .background(thumbColor),
        contentAlignment = Alignment.Center,
      ) {
        // Inner 4dp pixel core for authentic arcade/clean-room tactility
        Box(
          modifier =
            Modifier.size(4.dp)
              .background(if (checked) tokens.background else tokens.surfaceElevated)
        )
      }
    }
  }
}

/**
 * Clean-room architectural setting item with a pixel badge, uppercase title, concise description,
 * and a right-aligned blocky Android switch.
 */
@Composable
fun BlockyToggleSettingCard(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  switchTestTag: String? = null,
) {
  val tokens = LocalGridcastTokens.current
  PixelBlockContainer(
    modifier = modifier.fillMaxWidth(),
    isHighlighted = checked,
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    onClick = { onCheckedChange(!checked) },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.weight(1f).padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(8.dp)
                .background(if (checked) tokens.accentPrimary else tokens.gridBorderStrong)
          )
          Text(
            text = title,
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = tokens.inkPrimary,
          )
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = tokens.inkSecondary,
        )
      }

      BlockySwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier,
      )
    }
  }
}

/**
 * Sharp 0dp blocky multi-option selector option card (used for Screen Hz Optimization & Units).
 */
@Composable
fun BlockyRadioOptionCard(
  title: String,
  subtitle: String,
  badgeText: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .border(
          width = if (selected) 2.dp else 1.dp,
          color = if (selected) tokens.accentPrimary else tokens.gridBorder,
        )
        .background(if (selected) tokens.accentSoftFill else tokens.surfaceTranslucent)
        .selectable(
          selected = selected,
          role = Role.RadioButton,
          onClick = onClick,
        )
        .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f).padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Sharp 0dp square radio indicator block
        Box(
          modifier =
            Modifier.size(18.dp)
              .border(1.5.dp, if (selected) tokens.accentPrimary else tokens.gridBorderStrong)
              .background(if (selected) tokens.accentPrimary else tokens.background),
          contentAlignment = Alignment.Center,
        ) {
          if (selected) {
            Box(modifier = Modifier.size(6.dp).background(tokens.background))
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = title,
            style =
              MaterialTheme.typography.titleMedium.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = tokens.inkPrimary,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = tokens.inkSecondary,
          )
        }
      }

      Text(
        text = badgeText,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (selected) tokens.accentPrimary else tokens.inkMuted,
        modifier =
          Modifier.border(
              1.dp,
              if (selected) tokens.accentPrimary else tokens.gridBorder,
            )
            .background(tokens.background)
            .padding(horizontal = 7.dp, vertical = 3.dp),
      )
    }
  }
}
