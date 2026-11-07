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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.Color
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
import com.example.util.AppLanguage
import com.example.util.LocalizedStrings
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
  val resolvedLang = remember(settings.appLanguage) {
    AppLanguage.resolveEffectiveLanguage(settings.appLanguage)
  }
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
              text = LocalizedStrings.settingsTitle(resolvedLang),
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
            SettingsSectionLabel(title = LocalizedStrings.sectionTheme(resolvedLang))

            BlockyToggleSettingCard(
              title = LocalizedStrings.settingsDayNight(resolvedLang),
              subtitle = LocalizedStrings.settingsDayNightSub(resolvedLang),
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
              title = LocalizedStrings.settingsDarkMode(resolvedLang),
              subtitle = LocalizedStrings.settingsDarkModeSub(resolvedLang),
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
              title = LocalizedStrings.settingsDynamicTheme(resolvedLang),
              subtitle =
                if (supportsMaterialYou) {
                  LocalizedStrings.settingsDynamicThemeSub(resolvedLang)
                } else {
                  LocalizedStrings.settingsDynamicThemeFallbackSub(resolvedLang)
                },
              checked = settings.useDynamicTheme,
              onCheckedChange = onSetDynamicTheme,
              switchTestTag = "settings_dynamic_theme_switch",
            )

            BlockyToggleSettingCard(
              title = LocalizedStrings.settingsReduceAnimations(resolvedLang),
              subtitle = LocalizedStrings.settingsReduceAnimationsSub(resolvedLang),
              checked = settings.reduceAnimations,
              onCheckedChange = onSetReduceAnimations,
              switchTestTag = "settings_reduce_animations_switch",
            )
          }
        }

        // Section 2: Telemetry & Language
        BlockyEnterReveal(index = 1) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = LocalizedStrings.sectionTelemetry(resolvedLang))


            BlockyToggleSettingCard(
              title =
                if (settings.useFahrenheit) {
                  stringResource(R.string.settings_unit_title_f)
                } else {
                  stringResource(R.string.settings_unit_title_c)
                },
              subtitle = LocalizedStrings.settingsUnitDesc(resolvedLang),
              checked = settings.useFahrenheit,
              onCheckedChange = onSetUseFahrenheit,
              switchTestTag = "settings_fahrenheit_switch",
            )

            BlockyToggleSettingCard(
              title = LocalizedStrings.settingsNudges(resolvedLang),
              subtitle = LocalizedStrings.settingsNudgesSub(resolvedLang),
              checked = settings.showHeroNudges,
              onCheckedChange = onSetShowHeroNudges,
              switchTestTag = "settings_nudges_switch",
            )

            BlockyToggleSettingCard(
              title = LocalizedStrings.settingsHeroFont(resolvedLang),
              subtitle = LocalizedStrings.settingsHeroFontSub(settings.useStandardHeroFont, resolvedLang),
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
            SettingsSectionLabel(title = LocalizedStrings.sectionNotifications(resolvedLang))

            val effectivePinnedChecked = settings.pinnedNotificationEnabled && effectiveNotifGranted
            BlockyToggleSettingCard(
              title = LocalizedStrings.settingsPinnedNotif(resolvedLang),
              subtitle =
                LocalizedStrings.settingsPinnedNotifSub(effectiveNotifGranted, resolvedLang),
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
              title = LocalizedStrings.settingsMatrixOverlay(resolvedLang),
              subtitle = LocalizedStrings.settingsMatrixOverlaySub(resolvedLang),
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
                    text = LocalizedStrings.notifBlocked(resolvedLang),
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
                        text = LocalizedStrings.actionAllowPermission(resolvedLang),
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
                        text = LocalizedStrings.actionAppSettings(resolvedLang),
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
                    text = LocalizedStrings.actionGridStations(resolvedLang),
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
                    text = LocalizedStrings.actionSyncNow(resolvedLang),
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
                text = LocalizedStrings.settingsSyncTip(resolvedLang),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.inkMuted,
              )
            }
          }
        }

        // Section 6: Localization (Moved to bottom)
        BlockyEnterReveal(index = 5) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel(title = LocalizedStrings.sectionLanguage(resolvedLang))
            LanguageDropdownCard(
              currentLanguageCode = settings.appLanguage,
              onLanguageSelected = onSetAppLanguage
            )
          }
        }

        Spacer(modifier = Modifier.height(48.dp))
      }
    }
  }
}

@Composable
private fun LanguageDropdownCard(
  currentLanguageCode: String,
  onLanguageSelected: (String) -> Unit
) {
  val tokens = LocalGridcastTokens.current
  val languages = com.example.util.AppLanguage.entries
  val activeLang = com.example.util.AppLanguage.fromCode(currentLanguageCode)
  var expanded by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(tokens.surfaceElevated)
      .border(1.dp, tokens.gridBorder)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Text(
      text = LocalizedStrings.labelAppLanguage(activeLang),
      style = MaterialTheme.typography.titleMedium.copy(
        fontFamily = SilkscreenFontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
      ),
      color = tokens.inkPrimary,
    )
    
    PixelBlockContainer(
      modifier = Modifier.fillMaxWidth().testTag("language_selector_button"),
      isHighlighted = expanded,
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
      onClick = { expanded = !expanded }
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = activeLang.nativeName,
            style = MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.Bold,
              fontFamily = SilkscreenFontFamily,
              fontSize = 13.sp
            ),
            color = tokens.inkPrimary
          )
          Text(
            text = activeLang.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.inkMuted
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
          contentDescription = null,
          tint = if (expanded) tokens.accentPrimary else tokens.inkMuted
        )
      }
    }

    androidx.compose.animation.AnimatedVisibility(
      visible = expanded,
      enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
      exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(tokens.background)
          .border(1.dp, tokens.gridBorderStrong)
      ) {
        languages.forEachIndexed { idx, lang ->
          val isSelected = lang == activeLang
          if (idx > 0) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(tokens.gridBorder))
          }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onLanguageSelected(lang.code)
                expanded = false
              }
              .background(if (isSelected) tokens.accentSoftFill else Color.Transparent)
              .padding(horizontal = 16.dp, vertical = 14.dp)
              .testTag("language_option_${lang.code}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = lang.nativeName,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  ),
                  color = if (isSelected) tokens.accentPrimary else tokens.inkPrimary
                )
                Text(
                  text = lang.displayName,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) tokens.accentPrimary.copy(alpha = 0.8f) else tokens.inkMuted
                )
              }
              if (isSelected) {
                Text(
                  text = "✓",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = tokens.accentPrimary,
                )
              }
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
