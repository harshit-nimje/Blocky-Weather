package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.RefreshRateMode
import com.example.domain.model.WeatherConditionType
import com.example.gl.PixelGridController
import com.example.gl.PixelGridSurfaceView
import com.example.ui.WeatherViewModel
import com.example.ui.screens.DisplayRefreshCapabilities
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StationSelectorSheet
import com.example.ui.screens.WeatherDashboardScreen
import com.example.ui.theme.GridcastTheme
import com.example.ui.theme.LocalGridcastTokens
import com.example.util.BlockyNotificationManager

class MainActivity : ComponentActivity() {
  private val weatherViewModel: WeatherViewModel by viewModels {
    val app = application as GridcastApplication
    WeatherViewModel.Factory(
      repository = app.weatherRepository,
      preferencesRepository = app.preferencesRepository,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { GridcastRoot(viewModel = weatherViewModel) }
  }
}

@Composable
private fun GridcastRoot(viewModel: WeatherViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val view = LocalView.current

  val isDarkMode = uiState.settings.isDarkMode
  val useDynamicTheme = uiState.settings.useDynamicTheme
  val useSolarDayNightTheme = uiState.settings.useSolarDayNightTheme
  val refreshRateMode = uiState.settings.refreshRateMode

  val selectedHour = uiState.selectedHourIndex?.let { uiState.forecast?.hourly?.getOrNull(it) }
  val activeCondition =
    selectedHour?.condition ?: uiState.forecast?.condition ?: WeatherConditionType.PARTLY_CLOUDY
  val activePrecipMm = selectedHour?.precipMm ?: uiState.forecast?.precipitationMm ?: 0.0
  val activePrecipProb =
    selectedHour?.precipProbPercent ?: uiState.forecast?.hourly?.firstOrNull()?.precipProbPercent ?: 0
  val activeWindKmh = selectedHour?.windSpeedKmh ?: uiState.forecast?.windSpeedKmh ?: 0.0
  val activeIsDay = selectedHour?.isDay ?: uiState.forecast?.isDay ?: true

  val effectiveDarkMode =
    if (useSolarDayNightTheme) {
      !activeIsDay
    } else {
      isDarkMode
    }

  val systemPowerSave =
    remember(context) {
      val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
      pm?.isPowerSaveMode == true
    }
  val effectiveEcoMode =
    (refreshRateMode == RefreshRateMode.BATTERY_SAVER) || systemPowerSave

  // Inspect hardware display modes for Default / Peak / Battery Saver refresh rate control
  val displayModeResolution =
    remember(context) {
      resolveDisplayRefreshModes(context)
    }

  LaunchedEffect(refreshRateMode, displayModeResolution) {
    val activity = view.context as? Activity ?: return@LaunchedEffect
    val window = activity.window ?: return@LaunchedEffect
    val params = window.attributes
    val desiredModeId =
      when (refreshRateMode) {
        RefreshRateMode.DEFAULT -> 0 // Let system adaptive VRR choose automatically
        RefreshRateMode.PEAK -> displayModeResolution.peakModeId
        RefreshRateMode.BATTERY_SAVER -> displayModeResolution.minModeId
      }
    if (params.preferredDisplayModeId != desiredModeId) {
      params.preferredDisplayModeId = desiredModeId
      window.attributes = params
    }
  }

  val targetGlFrameRateHz =
    when {
      effectiveEcoMode -> displayModeResolution.capabilities.minHz.coerceAtMost(30f)
      refreshRateMode == RefreshRateMode.PEAK -> displayModeResolution.capabilities.peakHz
      else -> 0f
    }

  val gridController = remember { PixelGridController() }
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasNotificationPermission by
    remember(context) {
      mutableStateOf(BlockyNotificationManager.hasNotificationPermission(context))
    }

  val initialPermissionsLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
      viewModel.markInitialPermissionsPrompted()
      hasNotificationPermission = BlockyNotificationManager.hasNotificationPermission(context)
      viewModel.refreshNotificationState()
      val locationGranted =
        results[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
          viewModel.hasCoarseLocationPermission()
      if (locationGranted) {
        viewModel.useCurrentCoarseLocation()
      }
    }

  LaunchedEffect(uiState.settingsLoaded, uiState.settings.hasPromptedInitialPermissions) {
    if (uiState.settingsLoaded && !uiState.settings.hasPromptedInitialPermissions) {
      val missingPermissions = buildList {
        if (!viewModel.hasCoarseLocationPermission()) {
          add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !BlockyNotificationManager.hasNotificationPermission(context)
        ) {
          add(Manifest.permission.POST_NOTIFICATIONS)
        }
      }
      if (missingPermissions.isNotEmpty()) {
        initialPermissionsLauncher.launch(missingPermissions.toTypedArray())
      } else {
        viewModel.markInitialPermissionsPrompted()
        viewModel.refreshNotificationState()
      }
    }
  }

  DisposableEffect(context, lifecycleOwner, viewModel) {
    val resumeObserver = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission = BlockyNotificationManager.hasNotificationPermission(context)
        viewModel.refreshNotificationState()
      }
    }
    lifecycleOwner.lifecycle.addObserver(resumeObserver)
    onDispose { lifecycleOwner.lifecycle.removeObserver(resumeObserver) }
  }

  var lastCompletedSyncSeq by remember { mutableIntStateOf(uiState.syncCompletedSeq) }
  LaunchedEffect(uiState.syncCompletedSeq) {
    if (uiState.syncCompletedSeq > lastCompletedSyncSeq) {
      lastCompletedSyncSeq = uiState.syncCompletedSeq
      gridController.triggerPostSyncWave()
    }
  }

  DisposableEffect(context, lifecycleOwner, gridController, effectiveEcoMode) {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val gyroSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val orientationGravitySensor =
      sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var fusedTiltSlope = 0f
    var lastGyroNanos = 0L

    val listener =
      object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
          when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
              if (lastGyroNanos != 0L) {
                val dt = ((event.timestamp - lastGyroNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                fusedTiltSlope =
                  (fusedTiltSlope - event.values[2] * dt * 0.9f).coerceIn(-1.15f, 1.15f)
                gridController.setMotionTilt(fusedTiltSlope)
              }
              lastGyroNanos = event.timestamp
            }
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ACCELEROMETER -> {
              val gx = event.values[0]
              val gy = event.values[1].coerceAtLeast(1.8f)
              val gravitySlope = (-gx / gy).coerceIn(-1.15f, 1.15f)
              fusedTiltSlope =
                (fusedTiltSlope + (gravitySlope - fusedTiltSlope) * 0.20f).coerceIn(-1.15f, 1.15f)
              gridController.setMotionTilt(fusedTiltSlope)
            }
          }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
      }

    fun registerSensors() {
      if (effectiveEcoMode || sensorManager == null) return
      lastGyroNanos = 0L
      gyroSensor?.let {
        sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
      }
      orientationGravitySensor?.let {
        sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
      }
    }

    fun unregisterSensors() {
      sensorManager?.unregisterListener(listener)
      lastGyroNanos = 0L
    }

    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> registerSensors()
        Lifecycle.Event.ON_PAUSE -> unregisterSensors()
        else -> Unit
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      registerSensors()
    }

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      unregisterSensors()
    }
  }

  LaunchedEffect(
    activeCondition,
    activePrecipMm,
    activePrecipProb,
    activeWindKmh,
    activeIsDay,
    effectiveDarkMode,
    effectiveEcoMode,
  ) {
    gridController.updateWeatherState(
      condition = activeCondition,
      isDarkMode = effectiveDarkMode,
      ecoMode = effectiveEcoMode,
      precipitationMm = activePrecipMm,
      precipProbPercent = activePrecipProb,
      windSpeedKmh = activeWindKmh,
      isDay = activeIsDay,
    )
  }

  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !effectiveDarkMode
        controller.isAppearanceLightNavigationBars = !effectiveDarkMode
      }
    }
  }

  GridcastTheme(
    darkTheme = effectiveDarkMode,
    weatherCondition = activeCondition,
    useDynamicTheme = useDynamicTheme,
  ) {
    val activeAccent = LocalGridcastTokens.current.accentPrimary
    LaunchedEffect(useDynamicTheme, activeAccent) {
      gridController.updateThemeAccent(
        enabled = useDynamicTheme,
        r = activeAccent.red,
        g = activeAccent.green,
        b = activeAccent.blue,
      )
    }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .pointerInput(gridController) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                val firstChange = event.changes.firstOrNull()
                if (firstChange != null) {
                  val pos = firstChange.position
                  when (event.type) {
                    PointerEventType.Press -> gridController.onTouchDown(pos.x, pos.y)
                    PointerEventType.Move -> gridController.onTouchMove(pos.x, pos.y)
                    PointerEventType.Release,
                    PointerEventType.Exit -> {
                      if (event.changes.none { it.pressed }) {
                        gridController.onTouchUp()
                      }
                    }
                    else -> Unit
                  }
                }
              }
            }
          }
    ) {
      // OpenGL ES 2.0 graph-paper grid & strict integer-snapped blocks
      PixelGridSurfaceView(
        controller = gridController,
        ecoMode = effectiveEcoMode,
        targetFrameRateHz = targetGlFrameRateHz,
        modifier = Modifier.fillMaxSize(),
      )

      // Clean-room blocky weather dashboard with progressive pull-to-refresh
      WeatherDashboardScreen(
        uiState = uiState,
        onOpenSettings = { viewModel.openSettings() },
        onRefresh = { viewModel.refreshCurrentStation() },
        onOpenStationSheet = { viewModel.openStationSheet() },
        onSelectHour = { idx ->
          gridController.triggerPlayfulFidget()
          viewModel.selectHour(idx)
        },
        onClearSelectedHour = { viewModel.clearSelectedHour() },
        onToggleExpandDay = { idx ->
          gridController.triggerPlayfulFidget()
          viewModel.toggleExpandDay(idx)
        },
        onToggleExpandMetric = { tag ->
          gridController.triggerPlayfulFidget()
          viewModel.toggleExpandMetric(tag)
        },
        onUpdateAtmosphericCardOrder = { newOrder ->
          viewModel.updateAtmosphericCardOrder(newOrder)
        },
        onHeroBlocksFidget = { gridController.triggerPlayfulFidget() },
        onDismissSevereAlert = { viewModel.dismissSevereAlert(it) },
        onCycleQuip = {
          gridController.triggerPlayfulFidget()
          viewModel.cycleHeroQuip()
        },
      )

      // Dedicated Blocky Settings Screen
      if (uiState.isSettingsOpen) {
        SettingsScreen(
          settings = uiState.settings,
          displayCaps = displayModeResolution.capabilities,
          hasNotificationPermission = hasNotificationPermission,
          onNotificationPermissionResult = { granted ->
            hasNotificationPermission = granted
            if (granted) {
              viewModel.refreshNotificationState()
            }
          },
          onSetDarkMode = { viewModel.setDarkMode(it) },
          onSetDynamicTheme = { viewModel.setDynamicTheme(it) },
          onSetUseSolarDayNightTheme = { viewModel.setUseSolarDayNightTheme(it) },
          onSetUseStandardHeroFont = { viewModel.setUseStandardHeroFont(it) },
          onSetUseFahrenheit = { viewModel.setUseFahrenheit(it) },
          onSetShowHeroNudges = { viewModel.setShowHeroNudges(it) },
          onSetRefreshRateMode = { mode ->
            gridController.triggerPlayfulFidget()
            viewModel.setRefreshRateMode(mode)
          },
          onSetAutoRefreshInterval = { viewModel.setAutoRefreshInterval(it) },
          onSetPinnedNotificationEnabled = { viewModel.setPinnedNotificationEnabled(it) },
          onSetSevereAlertOverlayEnabled = { viewModel.setSevereAlertOverlayEnabled(it) },
          onOpenStations = { viewModel.openStationSheet() },
          onRefreshWeather = { viewModel.refreshCurrentStation() },
          onClose = { viewModel.closeSettings() },
        )
      }

      // Station Selector, City Search, Coarse GPS & Offline Cache Manager
      if (uiState.isStationSheetOpen) {
        StationSelectorSheet(
          activeStationId = uiState.settings.activeStationId,
          savedStations = uiState.savedStations,
          searchQuery = uiState.searchQuery,
          searchResults = uiState.searchResults,
          isSearching = uiState.isSearching,
          isLocating = uiState.isLocating,
          useFahrenheit = uiState.settings.useFahrenheit,
          statusBanner = uiState.statusBanner,
          hasCoarseLocationPermission = { viewModel.hasCoarseLocationPermission() },
          onUseCoarseLocation = { viewModel.useCurrentCoarseLocation() },
          onLocationPermissionDenied = { viewModel.onLocationPermissionDenied() },
          onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
          onSelectSavedStation = { stationId ->
            gridController.triggerPlayfulFidget()
            viewModel.selectSavedStation(stationId)
          },
          onSelectGeoResult = { result ->
            gridController.triggerPlayfulFidget()
            viewModel.selectGeoSearchResult(result)
          },
          onDeleteStation = { viewModel.deleteStation(it) },
          onClose = { viewModel.closeStationSheet() },
        )
      }
    }
  }
}

private data class ResolvedDisplayModes(
  val capabilities: DisplayRefreshCapabilities,
  val minModeId: Int,
  val peakModeId: Int,
)

private fun resolveDisplayRefreshModes(context: Context): ResolvedDisplayModes {
  val display: Display? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      runCatching { context.display }.getOrNull()
    } else {
      @Suppress("DEPRECATION")
      (context as? Activity)?.windowManager?.defaultDisplay
    }

  if (display == null) {
    return ResolvedDisplayModes(
      capabilities = DisplayRefreshCapabilities(activeHz = 60f, minHz = 60f, peakHz = 60f),
      minModeId = 0,
      peakModeId = 0,
    )
  }

  val activeMode = display.mode
  val sameResModes =
    display.supportedModes.filter {
      it.physicalWidth == activeMode.physicalWidth && it.physicalHeight == activeMode.physicalHeight
    }.ifEmpty { listOf(activeMode) }

  val minMode = sameResModes.minByOrNull { it.refreshRate } ?: activeMode
  val peakMode = sameResModes.maxByOrNull { it.refreshRate } ?: activeMode

  return ResolvedDisplayModes(
    capabilities =
      DisplayRefreshCapabilities(
        activeHz = activeMode.refreshRate,
        minHz = minMode.refreshRate,
        peakHz = peakMode.refreshRate,
      ),
    minModeId = minMode.modeId,
    peakModeId = peakMode.modeId,
  )
}
