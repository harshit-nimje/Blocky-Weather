package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AlertSeverity
import com.example.domain.model.SevereWeatherAlert
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily

/**
 * High-contrast matrix typography severe weather alert overlay component.
 * Displays real-time severe hazards with emergency matrix styling, flashing blocky hazard borders,
 * and quick action buttons whenever new telemetry triggers severe alerts.
 */
@Composable
fun BlockySevereAlertOverlay(
  alert: SevereWeatherAlert?,
  onInspectMetric: (String) -> Unit,
  onDismissAlert: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current

  val infiniteTransition = rememberInfiniteTransition(label = "hazard_pulse")
  val pulseAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.70f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "hazard_pulse_alpha",
    )

  AnimatedVisibility(
    visible = alert != null,
    enter =
      expandVertically(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
        slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { -it } +
        fadeIn(tween(300)),
    exit =
      shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
        slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } +
        fadeOut(tween(200)),
    modifier = modifier,
  ) {
    if (alert != null) {
      val alertColor =
        when (alert.severity) {
          AlertSeverity.CRITICAL -> Color(0xFFEF4444)
          AlertSeverity.WARNING -> Color(0xFFF97316)
          AlertSeverity.ADVISORY -> Color(0xFFEAB308)
        }

      Box(
        modifier =
          Modifier.widthIn(max = 620.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("severe_weather_alert_overlay")
      ) {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .border(
                width = 2.dp,
                color = alertColor.copy(alpha = pulseAlpha),
              )
              .background(tokens.surfaceElevated)
              .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Top Matrix Badge & Cause Category Stack
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(10.dp)
                    .background(alertColor.copy(alpha = pulseAlpha))
              )
              Text(
                text = "⚡ MATRIX HAZARD ALERT · ${alert.severity.label}",
                style =
                  MaterialTheme.typography.labelMedium.copy(
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                color = alertColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Box(
              modifier =
                Modifier.border(1.dp, alertColor.copy(alpha = 0.60f))
                  .background(alertColor.copy(alpha = 0.15f))
                  .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text(
                text = "HAZARD CAUSE: ${alert.category}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = SilkscreenFontFamily,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                ),
                color = alertColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          // Main High-Contrast Title & Description
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = alert.title,
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontFamily = SilkscreenFontFamily,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                ),
              color = tokens.inkPrimary,
            )

            Text(
              text = alert.description,
              style =
                MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 12.sp,
                  lineHeight = 17.sp,
                ),
              color = tokens.inkSecondary,
            )
          }

          // Emergency Safety Action Checklist
          val safetyTips = getEmergencySafetyChecklist(alert)
          if (safetyTips.isNotEmpty()) {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(tokens.surfaceTranslucent)
                  .border(1.dp, alertColor.copy(alpha = 0.35f))
                  .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "🛡 ACTIONABLE SAFETY CHECKLIST",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                color = alertColor,
              )
              safetyTips.forEach { tip ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(6.dp)
                        .background(alertColor)
                  )
                  Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = tokens.inkPrimary,
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          // Action Row: Inspect Related Matrix Card & Dismiss Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PixelBlockContainer(
              modifier = Modifier.testTag("alert_inspect_card_button"),
              isHighlighted = true,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              onClick = {
                onInspectMetric(alert.metricTag)
                onDismissAlert(alert.id)
              },
            ) {
              Text(
                text = "[VIEW CARD ↑]",
                style =
                  MaterialTheme.typography.labelSmall.copy(
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                color = tokens.accentPrimary,
              )
            }

            Text(
              text = "[ACKNOWLEDGE ×]",
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = SilkscreenFontFamily,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                ),
              color = alertColor,
              modifier =
                Modifier.clickable { onDismissAlert(alert.id) }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("alert_acknowledge_button"),
            )
          }
        }
      }
    }
  }
}

internal fun getEmergencySafetyChecklist(alert: SevereWeatherAlert): List<String> {
  val titleUpper = alert.title.uppercase()
  val descUpper = alert.description.uppercase()
  return when {
    "WIND" in titleUpper || "GALE" in titleUpper || "WIND" in descUpper ->
      listOf(
        "Secure outdoor items, furniture & patio gear",
        "Avoid standing near tall trees or powerlines",
        "Maintain firm grip on steering wheel under crosswinds",
      )
    "UV" in titleUpper || "RADIATION" in titleUpper || "HEAT" in titleUpper ->
      listOf(
        "Apply broad-spectrum >SPF 50 sunscreen liberally",
        "Stay hydrated with cold water & electrolyte drinks",
        "Seek shade and avoid direct midday sun exposure",
      )
    "AIR" in titleUpper || "AQI" in titleUpper || "POLLUTION" in titleUpper || "SMOG" in titleUpper ->
      listOf(
        "Wear a well-fitted N95 mask outdoors",
        "Run indoor HEPA air purifier on medium-high",
        "Keep windows sealed and avoid outdoor exercise",
      )
    "STORM" in titleUpper || "LIGHTNING" in titleUpper || "THUNDER" in titleUpper ->
      listOf(
        "Stay indoors away from glass windows and doors",
        "Unplug sensitive high-voltage electronics",
        "Avoid open fields, hilltops, and metal fences",
      )
    "SNOW" in titleUpper || "ICE" in titleUpper || "FREEZE" in titleUpper ->
      listOf(
        "Watch for hidden black ice on stairs & walkways",
        "Keep thermal layers, gloves & emergency torch ready",
        "Increase vehicle braking distance significantly",
      )
    else ->
      listOf(
        "Monitor live telemetry updates regularly",
        "Follow local civil protection authority guidance",
        "Keep phone charged and emergency numbers saved",
      )
  }
}
