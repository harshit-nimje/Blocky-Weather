package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.R
import com.example.data.local.AutoRefreshInterval
import com.example.data.local.RefreshRateMode
import com.example.data.local.UserSettings
import com.example.ui.components.BlockyEnterReveal
import com.example.ui.components.BlockyIntervalSliderCard
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
  hasNotificationPermission: Boolean = true,
  onNotificationPermissionResult: (Boolean) -> Unit = {},
  onSetDarkMode: (Boolean) -> Unit,
  onSetDynamicTheme: (Boolean) -> Unit,
  onSetUseSolarDayNightTheme: (Boolean) -> Unit = {},
  onSetUseStandardHeroFont: (Boolean) -> Unit = {},
  onSetReduceAnimations: (Boolean) -> Unit = {},
  onSetAppLanguage: (String) -> Unit = {},
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
  val context = LocalContext.current
  val activity = context as? Activity
  val supportsMaterialYou = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val requiresRuntimeNotifPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
  val effectiveNotifGranted = !requiresRuntimeNotifPermission || hasNotificationPermission

  var wasNotifRationaleBeforeLaunch by remember { mutableStateOf(false) }
  var showNotifSettingsFallback by remember { mutableStateOf(false) }
  var pendingNotifToggleTarget by remember { mutableStateOf<String?>(null) }

  val notifPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      onNotificationPermissionResult(granted)
      if (granted) {
        showNotifSettingsFallback = false
        if (pendingNotifToggleTarget == "severe") {
          onSetSevereAlertOverlayEnabled(true)
        } else {
          onSetPinnedNotificationEnabled(true)
        }
      } else {
        val nowRationale =
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
              activity,
              Manifest.permission.POST_NOTIFICATIONS,
            )
        if (!nowRationale) {
          showNotifSettingsFallback = true
        }
        if (!wasNotifRationaleBeforeLaunch && !nowRationale) {
          openNotificationSettings(context)
        }
      }
      pendingNotifToggleTarget = null
    }

  fun requestNotificationPermission(target: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
      pendingNotifToggleTarget = target
      wasNotifRationaleBeforeLaunch =
        activity != null &&
          ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
          )
      notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

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
              subtitle = "Light mode in daytime · Dark mode at night",
              checked = settings.useSolarDayNightTheme,
              onCheckedChange = { enabled ->
                if (enabled) {
                  onSetUseSolarDayNightTheme(true)
                  onSetDarkMode(false)
                } else {
                  onSetUseSolarDayNightTheme(false)
                }
              },
              switchTestTag = "settings_solar_day_night_switch",
            )

            BlockyToggleSettingCard(
              title = "DARK MODE",
              subtitle = "Force AMOLED dark theme always",
              checked = settings.isDarkMode && !settings.useSolarDayNightTheme,
              onCheckedChange = { enabled ->
                if (enabled) {
                  onSetDarkMode(true)
                  onSetUseSolarDayNightTheme(false)
                } else {
                  onSetDarkMode(false)
                }
              },
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

            BlockyToggleSettingCard(
              title = "REDUCE ANIMATIONS (PLAIN GRID)",
              subtitle = "Disables background particle effects, weather rain & motion; retains clean graph paper grid",
              checked = settings.reduceAnimations,
              onCheckedChange = onSetReduceAnimations,
              switchTestTag = "settings_reduce_animations_switch",
            )
          }
        }

        // Section 2: Telemetry & Language
        BlockyEnterReveal(index = 1) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = "LANGUAGE & TELEMETRY")

            // Language Selector Card
            val activeLang = com.example.util.AppLanguage.fromCode(settings.appLanguage)
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .background(tokens.surfaceElevated)
                  .border(1.dp, tokens.gridBorder)
                  .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "APP LANGUAGE",
                style =
                  MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SilkscreenFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                color = tokens.inkPrimary,
              )
              Text(
                text = "Smartly adapts to device language, or pick manual locale",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = tokens.inkSecondary,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                com.example.util.AppLanguage.entries.forEach { lang ->
                  val isSelected = activeLang == lang
                  PixelBlockContainer(
                    isHighlighted = isSelected,
                    fillOverride = if (isSelected) tokens.accentSoftFill else tokens.surfaceTranslucent,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    onClick = { onSetAppLanguage(lang.code) },
                  ) {
                    Text(
                      text = lang.code,
                      style =
                        MaterialTheme.typography.labelSmall.copy(
                          fontFamily = SilkscreenFontFamily,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                        ),
                      color = if (isSelected) tokens.accentPrimary else tokens.inkPrimary,
                    )
                  }
                }
              }
            }

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

            BlockyToggleSettingCard(
              title = "MAIN TEMPERATURE FONT",
              subtitle = if (settings.useStandardHeroFont) "Standard bold typography font" else "Signature blocky pixel matrix digits",
              checked = settings.useStandardHeroFont,
              onCheckedChange = onSetUseStandardHeroFont,
              switchTestTag = "settings_standard_hero_font_switch",
            )
          }
        }

        // Section 3: Automatic Background Telemetry Sync
        BlockyEnterReveal(index = 2) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BlockyIntervalSliderCard(
              selectedInterval = settings.autoRefreshInterval,
              onIntervalChanged = onSetAutoRefreshInterval,
            )
          }
        }

        // Section 4: System Notifications & Emergency Alerts
        BlockyEnterReveal(index = 3) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = "NOTIFICATIONS & MATRIX ALERTS")

            val effectivePinnedChecked = settings.pinnedNotificationEnabled && effectiveNotifGranted
            BlockyToggleSettingCard(
              title = "PINNED STATUS BAR NOTIFICATION",
              subtitle =
                if (!effectiveNotifGranted) {
                  "Permission required — tap switch to allow persistent status bar card"
                } else {
                  "Display persistent live temperature & quip on device status bar"
                },
              checked = effectivePinnedChecked,
              onCheckedChange = { enabled ->
                if (enabled) {
                  onSetPinnedNotificationEnabled(true)
                  if (!effectiveNotifGranted) {
                    requestNotificationPermission("pinned")
                  }
                } else {
                  onSetPinnedNotificationEnabled(false)
                }
              },
              switchTestTag = "settings_pinned_notif_switch",
            )

            BlockyToggleSettingCard(
              title = "SEVERE WEATHER MATRIX OVERLAY",
              subtitle = "Show emergency high-contrast overlay & alerts on gale wind, UV, pollution & storms",
              checked = settings.severeAlertOverlayEnabled,
              onCheckedChange = { enabled ->
                onSetSevereAlertOverlayEnabled(enabled)
                if (enabled && !effectiveNotifGranted) {
                  requestNotificationPermission("severe")
                }
              },
              switchTestTag = "settings_severe_overlay_switch",
            )

            if (!effectiveNotifGranted && (settings.pinnedNotificationEnabled || showNotifSettingsFallback)) {
              PixelBlockContainer(
                modifier = Modifier.fillMaxWidth(),
                isHighlighted = true,
                fillOverride = tokens.accentSoftFill,
                contentPadding = PaddingValues(12.dp),
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(
                    text = "NOTIFICATION PERMISSION BLOCKED OR NOT GRANTED YET",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.inkPrimary,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    PixelBlockContainer(
                      modifier =
                        Modifier.weight(1f)
                          .minimumInteractiveComponentSize()
                          .testTag("settings_grant_notif_permission_button"),
                      isHighlighted = true,
                      fillOverride = tokens.surfaceElevated,
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                      onClick = { requestNotificationPermission("pinned") },
                    ) {
                      Text(
                        text = "ALLOW PERMISSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.accentPrimary,
                      )
                    }
                    PixelBlockContainer(
                      modifier =
                        Modifier.weight(1f)
                          .minimumInteractiveComponentSize()
                          .testTag("settings_open_notif_settings_button"),
                      fillOverride = tokens.surfaceElevated,
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                      onClick = { openNotificationSettings(context) },
                    ) {
                      Text(
                        text = "APP SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.inkPrimary,
                      )
                    }
                  }
                }
              }
            }
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

private fun openNotificationSettings(context: Context) {
  val intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
      }
    }
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  runCatching { context.startActivity(intent) }
}
