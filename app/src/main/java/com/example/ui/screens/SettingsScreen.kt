package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.AutoRefreshInterval
import com.example.data.local.RefreshRateMode
import com.example.data.local.UserSettings
import com.example.ui.components.BlockyEnterReveal
import com.example.ui.components.BlockyMetricIcon
import com.example.ui.components.BlockyRadioOptionCard
import com.example.ui.components.BlockySmallIcon
import com.example.ui.components.BlockyToggleSettingCard
import com.example.ui.components.PixelBlockContainer
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily
import kotlin.math.roundToInt

data class DisplayRefreshCapabilities(
  val activeHz: Float = 60f,
  val minHz: Float = 60f,
  val peakHz: Float = 60f,
)

@Composable
fun SettingsScreen(
  settings: UserSettings,
  displayCaps: DisplayRefreshCapabilities,
  onSetDarkMode: (Boolean) -> Unit,
  onSetDynamicTheme: (Boolean) -> Unit,
  onSetUseSolarDayNightTheme: (Boolean) -> Unit = {},
  onSetUseFahrenheit: (Boolean) -> Unit,
  onSetShowHeroNudges: (Boolean) -> Unit,
  onSetRefreshRateMode: (RefreshRateMode) -> Unit,
  onSetAutoRefreshInterval: (AutoRefreshInterval) -> Unit,
  onSetPinnedNotificationEnabled: (Boolean) -> Unit,
  onSetSevereAlertOverlayEnabled: (Boolean) -> Unit,
  onOpenStations: () -> Unit,
  onRefreshWeather: () -> Unit,
  onClose: () -> Unit,
) {
  BackHandler(enabled = true) { onClose() }

  val tokens = LocalGridcastTokens.current
  val supportsMaterialYou = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(tokens.background.copy(alpha = 0.96f))
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 620.dp).fillMaxSize(),
    ) {
      // Top Architectural Settings Header
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          BlockySmallIcon(
            kind = BlockyMetricIcon.SETTINGS,
            tint = tokens.accentPrimary,
            size = 20.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = stringResource(R.string.settings_screen_title),
              style =
                MaterialTheme.typography.titleLarge.copy(
                  fontFamily = SilkscreenFontFamily,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                ),
              color = tokens.inkPrimary,
            )
            Text(
              text =
                "DISPLAY ${displayCaps.activeHz.roundToInt()} HZ · RANGE ${displayCaps.minHz.roundToInt()}–${displayCaps.peakHz.roundToInt()} HZ",
              style = MaterialTheme.typography.labelSmall,
              color = tokens.inkMuted,
            )
          }
        }

        IconButton(
          onClick = onClose,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("close_settings_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.cd_close_settings),
            tint = tokens.inkPrimary,
          )
        }
      }

      Column(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        // Section 1: Appearance & Device Theme
        BlockyEnterReveal(index = 0) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = stringResource(R.string.settings_section_theme))

            BlockyToggleSettingCard(
              title = "AUTO DAY / NIGHT THEME",
              subtitle = "Light mode during daytime · Dark mode at night",
              checked = settings.useSolarDayNightTheme,
              onCheckedChange = onSetUseSolarDayNightTheme,
              switchTestTag = "settings_solar_day_night_switch",
            )

            BlockyToggleSettingCard(
              title = stringResource(R.string.settings_dark_mode_title),
              subtitle = stringResource(R.string.settings_dark_mode_desc),
              checked = settings.isDarkMode,
              onCheckedChange = onSetDarkMode,
              switchTestTag = "settings_dark_mode_switch",
            )

            BlockyToggleSettingCard(
              title = stringResource(R.string.settings_dynamic_theme_title),
              subtitle =
                if (supportsMaterialYou) {
                  stringResource(R.string.settings_dynamic_theme_desc)
                } else {
                  stringResource(R.string.settings_dynamic_theme_fallback_desc)
                },
              checked = settings.useDynamicTheme,
              onCheckedChange = onSetDynamicTheme,
              switchTestTag = "settings_dynamic_theme_switch",
            )
          }
        }

        // Section 2: Telemetry & Hero Nudges
        BlockyEnterReveal(index = 1) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = stringResource(R.string.settings_section_telemetry))

            BlockyToggleSettingCard(
              title =
                if (settings.useFahrenheit) {
                  stringResource(R.string.settings_unit_title_f)
                } else {
                  stringResource(R.string.settings_unit_title_c)
                },
              subtitle = stringResource(R.string.settings_unit_desc),
              checked = settings.useFahrenheit,
              onCheckedChange = onSetUseFahrenheit,
              switchTestTag = "settings_fahrenheit_switch",
            )

            BlockyToggleSettingCard(
              title = stringResource(R.string.settings_nudges_title),
              subtitle = stringResource(R.string.settings_nudges_desc),
              checked = settings.showHeroNudges,
              onCheckedChange = onSetShowHeroNudges,
              switchTestTag = "settings_nudges_switch",
            )
          }
        }

        // Section 3: Automatic Background Telemetry Sync
        BlockyEnterReveal(index = 2) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = "AUTOMATIC TELEMETRY REFRESH")

            AutoRefreshInterval.entries.forEach { interval ->
              BlockyRadioOptionCard(
                title = "AUTO REFRESH: ${interval.label}",
                subtitle =
                  if (interval == AutoRefreshInterval.OFF) "Manual pull-down & sync actions only"
                  else "Periodically fetch live weather telemetry every ${interval.label.lowercase()}",
                badgeText = interval.label,
                selected = settings.autoRefreshInterval == interval,
                onClick = { onSetAutoRefreshInterval(interval) },
                modifier = Modifier.testTag("auto_refresh_${interval.name}"),
              )
            }
          }
        }

        // Section 4: System Notifications & Emergency Alerts
        BlockyEnterReveal(index = 3) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = "NOTIFICATIONS & MATRIX ALERTS")

            BlockyToggleSettingCard(
              title = "PINNED STATUS BAR NOTIFICATION",
              subtitle = "Display persistent live temperature & quip on device status bar",
              checked = settings.pinnedNotificationEnabled,
              onCheckedChange = onSetPinnedNotificationEnabled,
              switchTestTag = "settings_pinned_notif_switch",
            )

            BlockyToggleSettingCard(
              title = "SEVERE WEATHER MATRIX OVERLAY",
              subtitle = "Show emergency high-contrast overlay on gale wind, UV, pollution & severe storms",
              checked = settings.severeAlertOverlayEnabled,
              onCheckedChange = onSetSevereAlertOverlayEnabled,
              switchTestTag = "settings_severe_overlay_switch",
            )
          }
        }

        // Section 5: Screen Hz Optimization
        BlockyEnterReveal(index = 4) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = stringResource(R.string.settings_section_refresh_rate))

            val minHzLabel = "${displayCaps.minHz.roundToInt()} HZ MIN"
            val peakHzLabel = "${displayCaps.peakHz.roundToInt()} HZ MAX"

            BlockyRadioOptionCard(
              title = stringResource(R.string.hz_mode_default_title),
              subtitle = stringResource(R.string.hz_mode_default_desc),
              badgeText = "AUTO",
              selected = settings.refreshRateMode == RefreshRateMode.DEFAULT,
              onClick = { onSetRefreshRateMode(RefreshRateMode.DEFAULT) },
              modifier = Modifier.testTag("hz_mode_default"),
            )

            BlockyRadioOptionCard(
              title = stringResource(R.string.hz_mode_peak_title),
              subtitle = stringResource(R.string.hz_mode_peak_desc),
              badgeText = peakHzLabel,
              selected = settings.refreshRateMode == RefreshRateMode.PEAK,
              onClick = { onSetRefreshRateMode(RefreshRateMode.PEAK) },
              modifier = Modifier.testTag("hz_mode_peak"),
            )

            BlockyRadioOptionCard(
              title = stringResource(R.string.hz_mode_battery_title),
              subtitle = stringResource(R.string.hz_mode_battery_desc),
              badgeText = "$minHzLabel · ECO",
              selected = settings.refreshRateMode == RefreshRateMode.BATTERY_SAVER,
              onClick = { onSetRefreshRateMode(RefreshRateMode.BATTERY_SAVER) },
              modifier = Modifier.testTag("hz_mode_battery"),
            )
          }
        }

        // Section 4: Grid Stations & Telemetry Sync Actions
        BlockyEnterReveal(index = 3) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = stringResource(R.string.settings_section_grid_actions))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              PixelBlockContainer(
                modifier = Modifier.weight(1f).testTag("settings_open_stations_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                onClick = {
                  onClose()
                  onOpenStations()
                },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  BlockySmallIcon(
                    kind = BlockyMetricIcon.LOCATION,
                    tint = tokens.accentPrimary,
                    size = 16.dp,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "GRID STATIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.inkPrimary,
                  )
                }
              }

              PixelBlockContainer(
                modifier = Modifier.weight(1f).testTag("settings_sync_now_button"),
                isHighlighted = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                onClick = {
                  onClose()
                  onRefreshWeather()
                },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = tokens.accentPrimary,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "SYNC NOW",
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.inkPrimary,
                  )
                }
              }
            }

            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .border(1.dp, tokens.gridBorder)
                  .background(tokens.surfaceTranslucent)
                  .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
              Text(
                text = "TIP: PULL DOWN ANYWHERE ON THE WEATHER GRID TO TRIGGER PROGRESSIVE BLOCK SYNC.",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.inkMuted,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionLabel(title: String) {
  val tokens = LocalGridcastTokens.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(modifier = Modifier.size(9.dp).background(tokens.accentPrimary))
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = tokens.inkPrimary,
    )
  }
}
