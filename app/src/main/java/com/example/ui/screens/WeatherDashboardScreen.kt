package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.DailyPoint
import com.example.domain.model.HourlyPoint
import com.example.domain.model.WeatherConditionType
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherQuipEngine
import com.example.domain.model.toCompassCardinal
import com.example.domain.model.toDisplayTemp
import com.example.ui.WeatherUiState
import com.example.ui.components.BlockyAqiMeterVisual
import com.example.ui.components.BlockyBarometerVisual
import com.example.ui.components.BlockyCompactUvStrip
import com.example.ui.components.BlockyEnterReveal
import com.example.ui.components.BlockyHumidityGaugeVisual
import com.example.ui.components.BlockyMetricIcon
import com.example.ui.components.BlockyRainMatrixVisual
import com.example.ui.components.BlockyRealFeelVisual
import com.example.ui.components.BlockySmallIcon
import com.example.ui.components.BlockySolarBowlVisual
import com.example.ui.components.BlockyUvMeterVisual
import com.example.ui.components.BlockyWeatherIcon
import com.example.ui.components.BlockyWindCompassVisual
import com.example.ui.components.BlockyWordStaggerText
import com.example.ui.components.PixelBlockContainer
import com.example.ui.components.PixelMatrixTemperature
import com.example.ui.components.PixelThermalRangeBar
import com.example.ui.theme.LocalGridcastTokens
import com.example.ui.theme.SilkscreenFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

import com.example.ui.components.BlockySevereAlertOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
  uiState: WeatherUiState,
  onOpenSettings: () -> Unit,
  onRefresh: () -> Unit,
  onOpenStationSheet: () -> Unit,
  onSelectHour: (Int) -> Unit,
  onClearSelectedHour: () -> Unit,
  onToggleExpandDay: (Int) -> Unit,
  onToggleExpandMetric: (String) -> Unit,
  onHeroBlocksFidget: () -> Unit,
  onDismissSevereAlert: (String) -> Unit = {},
  onCycleQuip: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  val density = LocalDensity.current
  val forecast = uiState.forecast
  val useFahrenheit = uiState.settings.useFahrenheit
  val ecoMode = uiState.settings.ecoGridMode
  val showHeroNudges = uiState.settings.showHeroNudges

  val isPullRefreshing = uiState.isSyncing && forecast != null
  val pullToRefreshState = rememberPullToRefreshState()
  val pullFraction = pullToRefreshState.distanceFraction
  val maxContentPullShiftPx = remember(density) { with(density) { 54.dp.toPx() } }
  val contentShiftPx =
    if (isPullRefreshing) {
      maxContentPullShiftPx * 0.85f
    } else {
      pullFraction.coerceIn(0f, 1.35f) * maxContentPullShiftPx
    }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    val isExpandedLayout = maxWidth >= 700.dp
    val isMediumGrid = maxWidth >= 540.dp

    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Minimalist Top Clean-Room Header Bar (Location Selector + Settings Page Button)
      BlockyEnterReveal(index = 0) {
        CleanRoomTopBar(
          cityName = forecast?.cityName ?: "BLOCKY WEATHER",
          isOffline = uiState.isOffline,
          fetchedAtMillis = forecast?.fetchedAtMillis,
          isSyncing = uiState.isSyncing,
          onOpenStations = onOpenStationSheet,
          onOpenSettings = onOpenSettings,
          modifier = Modifier.widthIn(max = 1120.dp).fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        )
      }

      // Severe Weather Alert Matrix Notification Overlay Component
      BlockySevereAlertOverlay(
        alert = uiState.activeSevereAlert,
        onInspectMetric = onToggleExpandMetric,
        onDismissAlert = onDismissSevereAlert,
      )

      if (forecast == null) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
        ) {
          PixelBlockContainer(
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
            isHighlighted = true,
            contentPadding = PaddingValues(24.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              if (uiState.isSyncing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(28.dp),
                  color = tokens.accentPrimary,
                  strokeWidth = 2.5.dp,
                )
                Text(
                  text = stringResource(R.string.status_syncing),
                  style = MaterialTheme.typography.titleMedium,
                  color = tokens.inkPrimary,
                )
              } else {
                BlockyWeatherIcon(
                  condition = WeatherConditionType.PARTLY_CLOUDY,
                  size = 40.dp,
                  animated = false,
                )
                Text(
                  text = uiState.errorMessage ?: stringResource(R.string.error_offline_no_cache),
                  style = MaterialTheme.typography.bodyMedium,
                  color = tokens.inkSecondary,
                  textAlign = TextAlign.Center,
                )
                PixelBlockContainer(
                  modifier = Modifier.testTag("retry_sync_button"),
                  isHighlighted = true,
                  contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                  onClick = onRefresh,
                ) {
                  Text(
                    text = stringResource(R.string.action_retry_sync),
                    style = MaterialTheme.typography.labelLarge,
                    color = tokens.inkPrimary,
                  )
                }
              }
            }
          }
        }
      } else {
        PullToRefreshBox(
          isRefreshing = isPullRefreshing,
          onRefresh = onRefresh,
          state = pullToRefreshState,
          modifier = Modifier.fillMaxSize(),
          indicator = {
            BlockyPullRefreshBanner(
              pullFraction = pullFraction,
              isSyncing = isPullRefreshing,
              ecoMode = ecoMode,
              modifier =
                Modifier.align(Alignment.TopCenter)
                  .widthIn(max = 580.dp)
                  .fillMaxWidth()
                  .padding(top = 4.dp),
            )
          },
        ) {
          if (isExpandedLayout) {
            // Two-Pane Tablet / Foldable Clean-Room Layout
            Row(
              modifier =
                Modifier.widthIn(max = 1120.dp)
                  .fillMaxSize()
                  .graphicsLayer { translationY = contentShiftPx },
              horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
              Column(
                modifier =
                  Modifier.weight(0.45f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
              ) {
                BlockyEnterReveal(index = 0) {
                  HeroArchitecturalSection(
                    forecast = forecast,
                    selectedHourIndex = uiState.selectedHourIndex,
                    useFahrenheit = useFahrenheit,
                    ecoMode = ecoMode,
                    showHeroNudges = showHeroNudges,
                    useStandardHeroFont = uiState.settings.useStandardHeroFont,
                    hasActiveSevereAlert = uiState.activeSevereAlert != null,
                    onlineFunFact = uiState.onlineFunFact,
                    quipCycleOffset = uiState.quipCycleOffset,
                    onClearSelectedHour = onClearSelectedHour,
                    onHeroTapped = onHeroBlocksFidget,
                    onCycleQuip = onCycleQuip,
                  )
                }
                BlockyEnterReveal(index = 1) {
                  HourlyBlockTimelineSection(
                    hourly = forecast.hourly,
                    selectedHourIndex = uiState.selectedHourIndex,
                    useFahrenheit = useFahrenheit,
                    onSelectHour = onSelectHour,
                    onClearSelectedHour = onClearSelectedHour,
                  )
                }
              }

              Column(
                modifier =
                  Modifier.weight(0.55f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
              ) {
                BlockyEnterReveal(index = 2) {
                  AtmosphericMatrixSection(
                    forecast = forecast,
                    useFahrenheit = useFahrenheit,
                    columns = 3,
                    expandedMetricTags = uiState.expandedMetricTags,
                    onToggleExpandMetric = onToggleExpandMetric,
                  )
                }
                BlockyEnterReveal(index = 4) {
                  WeeklyArchitecturalReportSection(
                    daily = forecast.daily,
                    expandedDayIndex = uiState.expandedDayIndex,
                    useFahrenheit = useFahrenheit,
                    onToggleExpandDay = onToggleExpandDay,
                  )
                }
              }
            }
          } else {
            // Mobile Portrait Clean-Room Layout with stationary PullToRefreshBox + smooth downward content shift
            Box(
              modifier =
                Modifier.fillMaxSize()
                  .graphicsLayer { translationY = contentShiftPx },
              contentAlignment = Alignment.TopCenter,
            ) {
              Column(
                modifier =
                  Modifier.widthIn(max = 580.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.Start,
              ) {
                HeroArchitecturalSection(
                  forecast = forecast,
                  selectedHourIndex = uiState.selectedHourIndex,
                  useFahrenheit = useFahrenheit,
                  ecoMode = ecoMode,
                  showHeroNudges = showHeroNudges,
                  useStandardHeroFont = uiState.settings.useStandardHeroFont,
                  hasActiveSevereAlert = uiState.activeSevereAlert != null,
                  onlineFunFact = uiState.onlineFunFact,
                  quipCycleOffset = uiState.quipCycleOffset,
                  onClearSelectedHour = onClearSelectedHour,
                  onHeroTapped = onHeroBlocksFidget,
                  onCycleQuip = onCycleQuip,
                )

                Spacer(modifier = Modifier.height(14.dp))

                BlockyEnterReveal(index = 2) {
                  HourlyBlockTimelineSection(
                    hourly = forecast.hourly,
                    selectedHourIndex = uiState.selectedHourIndex,
                    useFahrenheit = useFahrenheit,
                    onSelectHour = onSelectHour,
                    onClearSelectedHour = onClearSelectedHour,
                  )
                }

                Spacer(modifier = Modifier.height(22.dp))

                BlockyEnterReveal(index = 2) {
                  AtmosphericMatrixSection(
                    forecast = forecast,
                    useFahrenheit = useFahrenheit,
                    columns = if (isMediumGrid) 3 else 2,
                    expandedMetricTags = uiState.expandedMetricTags,
                    onToggleExpandMetric = onToggleExpandMetric,
                  )
                }

                Spacer(modifier = Modifier.height(22.dp))

                BlockyEnterReveal(index = 3) {
                  WeeklyArchitecturalReportSection(
                    daily = forecast.daily,
                    expandedDayIndex = uiState.expandedDayIndex,
                    useFahrenheit = useFahrenheit,
                    onToggleExpandDay = onToggleExpandDay,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CleanRoomTopBar(
  cityName: String,
  isOffline: Boolean,
  fetchedAtMillis: Long?,
  isSyncing: Boolean,
  onOpenStations: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  val settingsDesc = stringResource(R.string.cd_open_settings)

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PixelBlockContainer(
      modifier = Modifier.weight(1f).testTag("station_selector_button"),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
      onClick = onOpenStations,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.size(8.dp)
                .background(
                  when {
                    isSyncing -> tokens.accentPrimary
                    isOffline -> Color(0xFFD99B1E)
                    else -> Color(0xFF4A8C62)
                  }
                )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = cityName.uppercase(Locale.US),
            style = MaterialTheme.typography.titleMedium,
            color = tokens.inkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (isOffline && fetchedAtMillis != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "· OFFLINE (${formatTimeShort(fetchedAtMillis)})",
              style = MaterialTheme.typography.labelSmall,
              color = tokens.inkMuted,
            )
          }
        }
        Icon(
          imageVector = Icons.Filled.KeyboardArrowDown,
          contentDescription = stringResource(R.string.cd_location_selector),
          tint = tokens.inkSecondary,
          modifier = Modifier.size(18.dp),
        )
      }
    }

    // Settings button replacing top-bar refresh button (pull-down handles refresh)
    PixelBlockContainer(
      modifier =
        Modifier.testTag("settings_button")
          .semantics { contentDescription = settingsDesc },
      contentPadding = PaddingValues(horizontal = 13.dp, vertical = 12.dp),
      onClick = onOpenSettings,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        BlockySmallIcon(
          kind = BlockyMetricIcon.SETTINGS,
          tint = tokens.inkPrimary,
          size = 18.dp,
        )
        Text(
          text = "SETTINGS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = tokens.inkPrimary,
        )
      }
    }
  }
}

/**
 * Progressive blocky pull-down indicator that illuminates 24 pixel blocks across 12 columns
 * proportionally to pull distance, then animates a stepped wave while telemetry syncs.
 */
@Composable
private fun BlockyPullRefreshBanner(
  pullFraction: Float,
  isSyncing: Boolean,
  ecoMode: Boolean,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalGridcastTokens.current
  val effectiveFraction = if (isSyncing) 1f else pullFraction
  if (!isSyncing && effectiveFraction <= 0.02f) return

  val infiniteTransition = rememberInfiniteTransition(label = "sync_marquee")
  val syncWavePhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 12f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 720, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "sync_wave_phase",
    )

  val revealAlpha = if (isSyncing) 1f else (effectiveFraction * 2.4f).coerceIn(0f, 1f)
  val isReadyToRelease = !isSyncing && effectiveFraction >= 1.0f

  Box(
    modifier =
      modifier
        .graphicsLayer { alpha = revealAlpha }
        .height(44.dp)
        .border(
          width = if (isReadyToRelease || isSyncing) 1.5.dp else 1.dp,
          color = if (isReadyToRelease || isSyncing) tokens.accentPrimary else tokens.gridBorder,
        )
        .background(if (isReadyToRelease) tokens.accentSoftFill else tokens.surfaceElevated)
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .testTag("pull_refresh_banner"),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // 12-column x 2-row progressive pixel block matrix
      Canvas(modifier = Modifier.width(120.dp).height(20.dp)) {
        val cols = 12
        val rows = 2
        val pitchX = size.width / cols
        val pitchY = size.height / rows
        val basePx = minOf(pitchX * 0.78f, pitchY * 0.80f)

        for (c in 0 until cols) {
          val colProgress =
            if (isSyncing) {
              if (ecoMode) {
                if (c % 2 == 0) 1f else 0.35f
              } else {
                val dist = kotlin.math.abs(((c - syncWavePhase + 12f) % 12f) - 2f)
                (1f - (dist / 3.2f)).coerceIn(0.18f, 1f)
              }
            } else {
              ((effectiveFraction - c / cols.toFloat()) * cols).coerceIn(0f, 1f)
            }

          val isLit = isSyncing || colProgress > 0.05f
          val scale = if (isLit) (0.45f + 0.55f * colProgress) else 0.45f
          val px = basePx * scale
          val hopY = if (isLit) (1f - colProgress) * 3.dp.toPx() else 0f
          val blockColor =
            when {
              isSyncing && colProgress > 0.75f -> tokens.accentPrimary
              isSyncing -> tokens.blockSwatches[c % tokens.blockSwatches.size].copy(alpha = 0.55f)
              colProgress >= 1f ->
                if (isReadyToRelease) tokens.accentPrimary
                else tokens.blockSwatches[c % tokens.blockSwatches.size]
              isLit -> tokens.accentPrimary.copy(alpha = 0.55f)
              else -> tokens.gridBorder.copy(alpha = 0.45f)
            }

          for (r in 0 until rows) {
            val left = c * pitchX + (pitchX - px) * 0.5f
            val top = (r * pitchY + (pitchY - px) * 0.5f - hopY).coerceIn(0f, size.height - px)
            drawRect(
              color = blockColor,
              topLeft = Offset(left, top),
              size = Size(px, px),
            )
          }
        }
      }

      Text(
        text =
          when {
            isSyncing -> "SYNCING TELEMETRY..."
            isReadyToRelease -> "RELEASE TO SYNC [↓]"
            else -> "PULL TO SYNC · ${(effectiveFraction * 100).roundToInt().coerceIn(0, 99)}%"
          },
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = SilkscreenFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
          ),
        color =
          if (isReadyToRelease || isSyncing) tokens.accentPrimary else tokens.inkSecondary,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

@Composable
private fun HeroArchitecturalSection(
  forecast: WeatherForecast,
  selectedHourIndex: Int?,
  useFahrenheit: Boolean,
  ecoMode: Boolean,
  showHeroNudges: Boolean,
  useStandardHeroFont: Boolean = false,
  hasActiveSevereAlert: Boolean = false,
  onlineFunFact: String?,
  quipCycleOffset: Int,
  onClearSelectedHour: () -> Unit,
  onHeroTapped: () -> Unit,
  onCycleQuip: () -> Unit,
) {
  val tokens = LocalGridcastTokens.current
  val selectedHour = selectedHourIndex?.let { forecast.hourly.getOrNull(it) }

  val displayTempC = selectedHour?.tempC ?: forecast.currentTempC
  val displayTempInt = displayTempC.toDisplayTemp(useFahrenheit)
  val displayCondition = selectedHour?.condition ?: forecast.condition

  val todayDaily = forecast.daily.firstOrNull()
  val todayHighC = todayDaily?.tempMaxC ?: forecast.currentTempC
  val todayLowC = todayDaily?.tempMinC ?: forecast.currentTempC
  val highTemp = todayHighC.toDisplayTemp(useFahrenheit)
  val lowTemp = todayLowC.toDisplayTemp(useFahrenheit)
  val feelsTemp = forecast.apparentTempC.toDisplayTemp(useFahrenheit)
  val unitLabel = if (useFahrenheit) "F" else "C"

  val displayConditionLabel =
    selectedHour?.condition?.displayTitle ?: forecast.conditionLabel

  val weatherQuip =
    remember(forecast, selectedHourIndex, onlineFunFact, quipCycleOffset) {
      WeatherQuipEngine.selectActiveQuip(
        forecast = forecast,
        selectedHourIndex = selectedHourIndex,
        onlineFunFact = onlineFunFact,
        quipCycleOffset = quipCycleOffset,
      )
    }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start,
  ) {
    // Centralized weather type icon + condition title above hero temperature (location is in selector above)
    BlockyEnterReveal(index = 1) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Box(
          modifier =
            Modifier.border(1.dp, tokens.gridBorder)
              .background(tokens.surfaceElevated)
              .padding(horizontal = 12.dp, vertical = 7.dp),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            BlockyWeatherIcon(
              condition = displayCondition,
              size = 22.dp,
              animated = !ecoMode,
            )
            Text(
              text =
                if (selectedHour != null) {
                  "${selectedHour.hourLabel} · $displayConditionLabel"
                } else {
                  displayConditionLabel
                },
              style =
                MaterialTheme.typography.labelLarge.copy(
                  fontFamily = SilkscreenFontFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                ),
              color = tokens.inkPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }

    // Huge standalone pixelated weather degrees with left-hugging High/Low arrows & yellow-filled blocky thermal line
    BlockyEnterReveal(index = 2) {
      PixelMatrixTemperature(
        temperature = displayTempInt,
        currentTempC = displayTempC,
        todayHigh = highTemp,
        todayHighC = todayHighC,
        todayLow = lowTemp,
        todayLowC = todayLowC,
        unitSymbol = unitLabel,
        ecoMode = ecoMode,
        useStandardHeroFont = useStandardHeroFont,
        onHeroTapped = onHeroTapped,
      )
    }

    // Spacious bottom padding below the main hero temperature before the word-staggered weather quip
    Spacer(modifier = Modifier.height(22.dp))

    // Word-by-word staggered blocky funny weather quip + compact telemetry hugged above the 24H timeline
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .background(tokens.background.copy(alpha = 0.88f))
          .padding(vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (showHeroNudges && !hasActiveSevereAlert) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .testTag("hero_weather_quip")
              .clickable(onClick = onCycleQuip)
              .padding(vertical = 2.dp),
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          BlockyWordStaggerText(
            text = weatherQuip,
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontFamily = SilkscreenFontFamily,
                fontSize = 12.sp,
                lineHeight = 18.sp,
              ),
            color = tokens.inkPrimary,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
          )
          Text(
            text = "[↻]",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.inkMuted,
          )
        }
      }

      val aqiSegment = forecast.usAqi?.let { " · AQI $it" }.orEmpty()
      Text(
        text =
          if (selectedHour != null) {
            "PREVIEW ${selectedHour.hourLabel} · TAP TO RESET TO LIVE"
          } else {
            "FEELS $feelsTemp° · WIND ${forecast.windSpeedKmh.roundToInt()} KM/H ${forecast.windDirectionDeg.toCompassCardinal()} · HUM ${forecast.humidityPercent}%$aqiSegment"
          },
        style = MaterialTheme.typography.labelSmall,
        color = tokens.inkMuted,
        modifier =
          if (selectedHour != null) Modifier.clickable(onClick = onClearSelectedHour)
          else Modifier,
      )
    }
  }
}

@Composable
private fun HourlyBlockTimelineSection(
  hourly: List<HourlyPoint>,
  selectedHourIndex: Int?,
  useFahrenheit: Boolean,
  onSelectHour: (Int) -> Unit,
  onClearSelectedHour: () -> Unit,
) {
  val tokens = LocalGridcastTokens.current
  if (hourly.isEmpty()) return

  val inspectedHour = selectedHourIndex?.let { hourly.getOrNull(it) }

  Column(modifier = Modifier.fillMaxWidth().testTag("hourly_forecast_strip")) {
    SectionHeaderLabel(title = stringResource(R.string.section_hourly_timeline))
    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      itemsIndexed(hourly) { idx, point ->
        val isSelected = selectedHourIndex == idx || (selectedHourIndex == null && idx == 0)
        val swatch = tokens.blockSwatches[idx % tokens.blockSwatches.size]

        BlockyEnterReveal(index = idx.coerceAtMost(6)) {
          PixelBlockContainer(
            modifier = Modifier.width(74.dp).testTag("hour_card_$idx"),
            isHighlighted = isSelected,
            fillOverride = if (isSelected) tokens.heroBlockFill else tokens.surfaceTranslucent,
            contentPadding = PaddingValues(0.dp),
            onClick = { onSelectHour(idx) },
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(swatch))

              Column(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = point.hourLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) tokens.heroBlockInk else tokens.inkMuted,
                )

                BlockyWeatherIcon(
                  condition = point.condition,
                  size = 24.dp,
                  animated = false,
                )

                Text(
                  text = "${point.tempC.toDisplayTemp(useFahrenheit)}°",
                  style = MaterialTheme.typography.titleMedium,
                  color = if (isSelected) tokens.heroBlockInk else tokens.inkPrimary,
                )

                Text(
                  text = "${point.precipProbPercent}%",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isSelected) tokens.heroBlockInk.copy(alpha = 0.78f) else tokens.inkMuted,
                )
              }
            }
          }
        }
      }
    }

    // Expandable Detailed Menu when any hour card is tapped
    AnimatedVisibility(
      visible = inspectedHour != null,
      enter =
        expandVertically(animationSpec = tween(540, easing = FastOutSlowInEasing)) +
          slideInVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)) { -it / 6 } +
          fadeIn(tween(400, delayMillis = 60)),
      exit =
        shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
          fadeOut(tween(150)),
    ) {
      if (inspectedHour != null) {
        PixelBlockContainer(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("hourly_detail_panel"),
          isExpanded = true,
          fillOverride = tokens.surfaceElevated,
          contentPadding = PaddingValues(12.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                BlockyWeatherIcon(
                  condition = inspectedHour.condition,
                  size = 24.dp,
                  animated = true,
                )
                Text(
                  text = "${inspectedHour.hourLabel} · ${inspectedHour.condition.displayTitle}",
                  style = MaterialTheme.typography.labelLarge,
                  color = tokens.inkPrimary,
                )
              }
              Text(
                text = "[× CLOSE]",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.accentPrimary,
                modifier = Modifier.clickable(onClick = onClearSelectedHour),
              )
            }
            BlockyCompactUvStrip(uvIndex = inspectedHour.uvIndex)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(modifier = Modifier.weight(1f)) {
                DayDetailStat(
                  label = "TEMP",
                  value = "${inspectedHour.tempC.toDisplayTemp(useFahrenheit)}°",
                )
              }
              Box(modifier = Modifier.weight(1f)) {
                DayDetailStat(
                  label = "PRECIP",
                  value =
                    "${inspectedHour.precipProbPercent}% · ${String.format(Locale.US, "%.1f", inspectedHour.precipMm)}mm",
                )
              }
              Box(modifier = Modifier.weight(1f)) {
                DayDetailStat(
                  label = "WIND",
                  value = "${inspectedHour.windSpeedKmh.roundToInt()} km/h",
                )
              }
              Box(modifier = Modifier.weight(1f)) {
                DayDetailStat(
                  label = "UV INDEX",
                  value = "UV ${inspectedHour.uvIndex.roundToInt()}",
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
private fun AtmosphericMatrixSection(
  forecast: WeatherForecast,
  useFahrenheit: Boolean,
  columns: Int,
  expandedMetricTags: Set<String>,
  onToggleExpandMetric: (String) -> Unit,
) {
  val tokens = LocalGridcastTokens.current
  val next24PrecipPeak = forecast.hourly.maxOfOrNull { it.precipProbPercent } ?: 0
  val todayDaily = forecast.daily.firstOrNull()
  val sunriseShort = todayDaily?.sunriseIso?.substringAfter('T', "06:30")?.take(5) ?: "06:30"
  val sunsetShort = todayDaily?.sunsetIso?.substringAfter('T', "18:00")?.take(5) ?: "18:00"
  val solarProgress = computeSolarProgress(todayDaily?.sunriseIso, todayDaily?.sunsetIso)

  val cards =
    listOf(
      MetricCardData(
        tag = "metric_real_feel",
        icon = BlockyMetricIcon.REAL_FEEL,
        title = stringResource(R.string.metric_real_feel),
        primaryValue = "${forecast.apparentTempC.toDisplayTemp(useFahrenheit)}°",
        secondaryDetail = "Actual ${forecast.currentTempC.toDisplayTemp(useFahrenheit)}° · Dew ${forecast.dewPointC.toDisplayTemp(useFahrenheit)}°",
        ratio = ((forecast.apparentTempC + 10.0) / 50.0).toFloat().coerceIn(0.1f, 1f),
        expandedVisual = {
          BlockyRealFeelVisual(
            actualTempC = forecast.currentTempC,
            apparentTempC = forecast.apparentTempC,
            dewPointC = forecast.dewPointC,
            useFahrenheit = useFahrenheit,
          )
        },
        expandedFacts =
          listOf(
            "ACTUAL" to "${forecast.currentTempC.toDisplayTemp(useFahrenheit)}°",
            "DEW POINT" to "${forecast.dewPointC.toDisplayTemp(useFahrenheit)}°",
            "HEAT STRESS" to humidityComfortLabel(forecast.humidityPercent),
          ),
      ),
      MetricCardData(
        tag = "metric_aqi",
        icon = BlockyMetricIcon.AQI,
        title = stringResource(R.string.metric_aqi),
        primaryValue = "AQI ${forecast.usAqi ?: 25}",
        secondaryDetail =
          "${aqiStatusLabel(forecast.usAqi ?: 25)} · PM2.5 ${forecast.pm25?.let { String.format(Locale.US, "%.1f", it) } ?: "8.0"}",
        ratio = ((forecast.usAqi ?: 25) / 200.0).toFloat().coerceIn(0.08f, 1f),
        expandedVisual = {
          BlockyAqiMeterVisual(
            usAqi = forecast.usAqi ?: 25,
            pm25 = forecast.pm25,
          )
        },
        expandedFacts =
          listOf(
            "US AQI" to "${forecast.usAqi ?: 25}",
            "PM2.5" to (forecast.pm25?.let { String.format(Locale.US, "%.1f µg/m³", it) } ?: "8.0 µg/m³"),
            "AIR STATUS" to aqiStatusLabel(forecast.usAqi ?: 25),
          ),
      ),
      MetricCardData(
        tag = "metric_wind",
        icon = BlockyMetricIcon.WIND,
        title = stringResource(R.string.metric_wind),
        primaryValue = "${forecast.windSpeedKmh.roundToInt()} km/h",
        secondaryDetail =
          "${forecast.windDirectionDeg.toCompassCardinal()} · Gust ${forecast.windGustsKmh.roundToInt()}",
        ratio = (forecast.windSpeedKmh / 60.0).toFloat().coerceIn(0.08f, 1f),
        expandedVisual = {
          BlockyWindCompassVisual(
            windSpeedKmh = forecast.windSpeedKmh,
            windGustsKmh = forecast.windGustsKmh,
            windDirectionDeg = forecast.windDirectionDeg,
          )
        },
        expandedFacts =
          listOf(
            "BEARING" to "${forecast.windDirectionDeg}° ${forecast.windDirectionDeg.toCompassCardinal()}",
            "PEAK GUST" to "${forecast.windGustsKmh.roundToInt()} km/h",
            "STATUS" to windBeaufortLabel(forecast.windSpeedKmh),
          ),
      ),
      MetricCardData(
        tag = "metric_humidity",
        icon = BlockyMetricIcon.HUMIDITY,
        title = stringResource(R.string.metric_humidity),
        primaryValue = "${forecast.humidityPercent}%",
        secondaryDetail = "Dew ${forecast.dewPointC.toDisplayTemp(useFahrenheit)}°",
        ratio = (forecast.humidityPercent / 100f).coerceIn(0.06f, 1f),
        expandedVisual = {
          BlockyHumidityGaugeVisual(humidityPercent = forecast.humidityPercent)
        },
        expandedFacts =
          listOf(
            "DEW POINT" to "${forecast.dewPointC.toDisplayTemp(useFahrenheit)}°",
            "FEELS LIKE" to "${forecast.apparentTempC.toDisplayTemp(useFahrenheit)}°",
            "AIR FEEL" to humidityComfortLabel(forecast.humidityPercent),
          ),
      ),
      MetricCardData(
        tag = "metric_uv",
        icon = BlockyMetricIcon.UV,
        title = stringResource(R.string.metric_uv),
        primaryValue = "UV ${forecast.uvIndex.roundToInt()}",
        secondaryDetail = "${uvCategoryLabel(forecast.uvIndex)} · Cloud ${forecast.cloudCoverPercent}%",
        ratio = (forecast.uvIndex / 11.0).toFloat().coerceIn(0.08f, 1f),
        expandedVisual = {
          BlockyUvMeterVisual(uvIndex = forecast.uvIndex)
        },
        expandedFacts =
          listOf(
            "EXACT UV" to String.format(Locale.US, "%.1f", forecast.uvIndex),
            "CLOUD COVER" to "${forecast.cloudCoverPercent}%",
            "ADVICE" to if (forecast.uvIndex >= 3.0) "SPF 30+ outdoors" else "Minimal UV risk",
          ),
      ),
      MetricCardData(
        tag = "metric_pressure",
        icon = BlockyMetricIcon.PRESSURE,
        title = stringResource(R.string.metric_pressure),
        primaryValue = "${forecast.pressureHpa.roundToInt()} hPa",
        secondaryDetail = "Vis ${(forecast.visibilityMeters / 1000.0).roundToInt()} km",
        ratio = ((forecast.pressureHpa - 975.0) / 65.0).toFloat().coerceIn(0.1f, 1f),
        expandedVisual = {
          BlockyBarometerVisual(pressureHpa = forecast.pressureHpa)
        },
        expandedFacts =
          listOf(
            "SURFACE" to String.format(Locale.US, "%.1f hPa", forecast.pressureHpa),
            "VISIBILITY" to String.format(Locale.US, "%.1f km", forecast.visibilityMeters / 1000.0),
            "SYSTEM" to if (forecast.pressureHpa >= 1013.0) "High · Stable" else "Low · Unsettled",
          ),
      ),
      MetricCardData(
        tag = "metric_precipitation",
        icon = BlockyMetricIcon.PRECIPITATION,
        title = stringResource(R.string.metric_precipitation),
        primaryValue = String.format(Locale.US, "%.1f mm", forecast.precipitationMm),
        secondaryDetail = "24h peak $next24PrecipPeak%",
        ratio = (next24PrecipPeak / 100f).coerceIn(0.06f, 1f),
        expandedVisual = {
          BlockyRainMatrixVisual(hourly = forecast.hourly)
        },
        expandedFacts =
          listOf(
            "CURRENT" to String.format(Locale.US, "%.1f mm/h", forecast.precipitationMm),
            "24H PEAK" to "$next24PrecipPeak%",
            "DAY TOTAL" to String.format(Locale.US, "%.1f mm", todayDaily?.precipSumMm ?: 0.0),
          ),
      ),
      MetricCardData(
        tag = "metric_solar",
        icon = BlockyMetricIcon.SOLAR,
        title = stringResource(R.string.metric_daylight),
        primaryValue = "$sunriseShort–$sunsetShort",
        secondaryDetail = "${(solarProgress * 100).roundToInt()}% elapsed",
        ratio = solarProgress.coerceIn(0.08f, 1f),
        expandedVisual = {
          BlockySolarBowlVisual(
            solarProgress = solarProgress,
            isDay = forecast.isDay,
            sunriseLabel = sunriseShort,
            sunsetLabel = sunsetShort,
          )
        },
        expandedFacts =
          listOf(
            "SUNRISE" to sunriseShort,
            "SUNSET" to sunsetShort,
            "DAYLIGHT" to "${(solarProgress * 100).roundToInt()}% complete",
          ),
      ),
    )

  Column(modifier = Modifier.fillMaxWidth()) {
    SectionHeaderLabel(title = stringResource(R.string.section_atmospheric_matrix))
    Spacer(modifier = Modifier.height(10.dp))

    val rows = cards.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      rows.forEachIndexed { rIdx, rowCards ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          rowCards.forEachIndexed { cIdx, card ->
            val flatIndex = rIdx * columns + cIdx
            val swatch = tokens.blockSwatches[flatIndex % tokens.blockSwatches.size]
            val isExpanded = card.tag in expandedMetricTags

            BlockyEnterReveal(
              index = flatIndex + 2,
              modifier = Modifier.weight(1f),
            ) {
              PixelBlockContainer(
                modifier = Modifier.fillMaxWidth().testTag(card.tag),
                isExpanded = isExpanded,
                fillOverride = if (isExpanded) tokens.surfaceElevated else tokens.surfaceTranslucent,
                contentPadding = PaddingValues(13.dp),
                onClick = { onToggleExpandMetric(card.tag) },
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      modifier = Modifier.weight(1f),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      BlockySmallIcon(
                        kind = card.icon,
                        tint = tokens.inkSecondary,
                        size = 15.dp,
                      )
                      Text(
                        text = card.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                      Text(
                        text = if (isExpanded) "−" else "+",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpanded) tokens.accentPrimary else tokens.inkMuted,
                      )
                      Box(modifier = Modifier.size(10.dp).background(swatch))
                    }
                  }

                  Text(
                    text = card.primaryValue,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )

                  Text(
                    text = card.secondaryDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
          repeat(columns - rowCards.size) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }

        // Full-width compact architectural detail drawer below each row when any card in that row is expanded
        val expandedInRow = rowCards.filter { it.tag in expandedMetricTags }
        AnimatedVisibility(
          visible = expandedInRow.isNotEmpty(),
          enter =
            expandVertically(animationSpec = tween(560, easing = FastOutSlowInEasing)) +
              slideInVertically(animationSpec = tween(520, easing = FastOutSlowInEasing)) {
                -it / 6
              } +
              fadeIn(tween(420, delayMillis = 60)),
          exit =
            shrinkVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
              fadeOut(tween(150)),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            expandedInRow.forEach { activeCard ->
              PixelBlockContainer(
                modifier = Modifier.fillMaxWidth().testTag("${activeCard.tag}_expanded"),
                isExpanded = true,
                fillOverride = tokens.surfaceElevated,
                contentPadding = PaddingValues(12.dp),
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      BlockySmallIcon(
                        kind = activeCard.icon,
                        tint = tokens.accentPrimary,
                        size = 15.dp,
                      )
                      Text(
                        text = "${activeCard.title} · ${activeCard.primaryValue}",
                        style =
                          MaterialTheme.typography.labelMedium.copy(
                            fontFamily = SilkscreenFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                          ),
                        color = tokens.inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Text(
                      text = "[× CLOSE]",
                      style = MaterialTheme.typography.labelSmall,
                      color = tokens.accentPrimary,
                      modifier = Modifier.clickable { onToggleExpandMetric(activeCard.tag) },
                    )
                  }

                  Box(
                    modifier =
                      Modifier.fillMaxWidth()
                        .border(1.dp, tokens.gridBorder)
                        .background(tokens.background)
                        .padding(10.dp)
                  ) {
                    activeCard.expandedVisual()
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    activeCard.expandedFacts.forEach { (k, v) ->
                      Box(modifier = Modifier.weight(1f)) {
                        DayDetailStat(label = k, value = v)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WeeklyArchitecturalReportSection(
  daily: List<DailyPoint>,
  expandedDayIndex: Int?,
  useFahrenheit: Boolean,
  onToggleExpandDay: (Int) -> Unit,
) {
  val tokens = LocalGridcastTokens.current
  if (daily.isEmpty()) return

  val weekMinC = daily.minOf { it.tempMinC }
  val weekMaxC = daily.maxOf { it.tempMaxC }

  Column(modifier = Modifier.fillMaxWidth().testTag("daily_forecast_list")) {
    SectionHeaderLabel(title = stringResource(R.string.section_weekly_report))
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      daily.forEachIndexed { idx, day ->
        val isExpanded = expandedDayIndex == idx
        val swatch = tokens.blockSwatches[idx % tokens.blockSwatches.size]
        val daySunrise = day.sunriseIso.substringAfter('T', "06:30").take(5)
        val daySunset = day.sunsetIso.substringAfter('T', "18:00").take(5)
        val daySolarProgress = computeSolarProgress(day.sunriseIso, day.sunsetIso)

        BlockyEnterReveal(index = idx + 3) {
          PixelBlockContainer(
            modifier = Modifier.fillMaxWidth().testTag("day_row_$idx"),
            isExpanded = isExpanded,
            fillOverride = if (isExpanded) tokens.surfaceElevated else tokens.surfaceTranslucent,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            onClick = { onToggleExpandDay(idx) },
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.width(86.dp),
                ) {
                  Box(modifier = Modifier.size(10.dp).background(swatch))
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = day.dayLabel,
                      style = MaterialTheme.typography.titleMedium,
                      color = tokens.inkPrimary,
                    )
                    Text(
                      text = day.dateShort,
                      style = MaterialTheme.typography.labelSmall,
                      color = tokens.inkMuted,
                    )
                  }
                }

                BlockyWeatherIcon(
                  condition = day.condition,
                  size = 22.dp,
                  animated = false,
                )

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = "${day.tempMinC.toDisplayTemp(useFahrenheit)}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.inkMuted,
                  )
                  PixelThermalRangeBar(
                    dayMinC = day.tempMinC,
                    dayMaxC = day.tempMaxC,
                    weekMinC = weekMinC,
                    weekMaxC = weekMaxC,
                    isExpanded = isExpanded,
                    modifier = Modifier.width(64.dp).height(9.dp),
                  )
                  Text(
                    text = "${day.tempMaxC.toDisplayTemp(useFahrenheit)}°",
                    style = MaterialTheme.typography.titleMedium,
                    color = tokens.inkPrimary,
                  )
                  Icon(
                    imageVector =
                      if (isExpanded) Icons.Filled.KeyboardArrowUp
                      else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = tokens.inkMuted,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }

              AnimatedVisibility(
                visible = isExpanded,
                enter =
                  expandVertically(animationSpec = tween(540, easing = FastOutSlowInEasing)) +
                    slideInVertically(animationSpec = tween(500, easing = FastOutSlowInEasing)) {
                      -it / 6
                    } +
                    fadeIn(tween(400, delayMillis = 60)),
                exit =
                  shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    fadeOut(tween(140)),
              ) {
                Column(
                  modifier =
                    Modifier.fillMaxWidth()
                      .padding(top = 10.dp)
                      .border(1.dp, tokens.gridBorder)
                      .background(tokens.background)
                      .padding(10.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  BlockySolarBowlVisual(
                    solarProgress = daySolarProgress,
                    isDay = true,
                    sunriseLabel = daySunrise,
                    sunsetLabel = daySunset,
                  )
                  BlockyCompactUvStrip(uvIndex = day.uvMax)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Box(modifier = Modifier.weight(1f)) {
                      DayDetailStat(
                        label = "FEELS RANGE",
                        value =
                          "${day.apparentMinC.toDisplayTemp(useFahrenheit)}°..${day.apparentMaxC.toDisplayTemp(useFahrenheit)}°",
                      )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                      DayDetailStat(
                        label = "PRECIP",
                        value =
                          "${day.precipProbMaxPercent}% · ${String.format(Locale.US, "%.1f", day.precipSumMm)}mm",
                      )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                      DayDetailStat(
                        label = "MAX WIND",
                        value = "${day.windMaxKmh.roundToInt()} km/h",
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DayDetailStat(label: String, value: String) {
  val tokens = LocalGridcastTokens.current
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = tokens.inkMuted,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Bold,
      color = tokens.inkPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SectionHeaderLabel(title: String) {
  val tokens = LocalGridcastTokens.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(modifier = Modifier.size(10.dp).background(tokens.inkPrimary))
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = tokens.inkPrimary,
    )
  }
}

private data class MetricCardData(
  val tag: String,
  val icon: BlockyMetricIcon,
  val title: String,
  val primaryValue: String,
  val secondaryDetail: String,
  val ratio: Float,
  val expandedVisual: @Composable () -> Unit,
  val expandedFacts: List<Pair<String, String>>,
)

private fun windBeaufortLabel(kmh: Double): String =
  when {
    kmh < 6.0 -> "Calm Air"
    kmh < 20.0 -> "Light Breeze"
    kmh < 39.0 -> "Fresh Breeze"
    kmh < 62.0 -> "Strong Wind"
    else -> "Gale Force"
  }

private fun humidityComfortLabel(humidity: Int): String =
  when {
    humidity < 30 -> "Dry Air"
    humidity <= 65 -> "Comfortable"
    humidity <= 80 -> "Humid"
    else -> "Very Muggy"
  }

private fun uvCategoryLabel(uv: Double): String =
  when {
    uv < 3.0 -> "Low"
    uv < 6.0 -> "Moderate"
    uv < 8.0 -> "High"
    uv < 11.0 -> "Very High"
    else -> "Extreme"
  }

private fun aqiStatusLabel(aqi: Int): String =
  when {
    aqi <= 50 -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Unhealthy for Sensitive Groups"
    aqi <= 200 -> "Unhealthy"
    aqi <= 300 -> "Very Unhealthy"
    else -> "Hazardous"
  }

private fun formatTimeShort(millis: Long): String {
  val fmt = SimpleDateFormat("HH:mm", Locale.US)
  return fmt.format(Date(millis))
}

private fun computeSolarProgress(sunriseIso: String?, sunsetIso: String?): Float {
  if (sunriseIso.isNullOrBlank() || sunsetIso.isNullOrBlank()) return 0.5f
  val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
  val rise = runCatching { fmt.parse(sunriseIso.take(16))?.time }.getOrNull() ?: return 0.5f
  val set = runCatching { fmt.parse(sunsetIso.take(16))?.time }.getOrNull() ?: return 0.5f
  val now = System.currentTimeMillis()
  if (set <= rise) return 0.5f
  return ((now - rise).toFloat() / (set - rise).toFloat()).coerceIn(0f, 1f)
}
