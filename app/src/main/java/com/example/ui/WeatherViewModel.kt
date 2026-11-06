package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.RefreshRateMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.repository.WeatherRepository
import com.example.domain.model.GeoSearchResult
import com.example.domain.model.SavedStation
import com.example.domain.model.WeatherForecast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.GridcastApplication
import com.example.data.local.AutoRefreshInterval
import com.example.domain.model.SevereWeatherAlert
import com.example.domain.model.SevereWeatherAlertEngine
import com.example.domain.model.WeatherQuipEngine
import com.example.util.BlockyNotificationManager

data class WeatherUiState(
  val forecast: WeatherForecast? = null,
  val settings: UserSettings = UserSettings(),
  val settingsLoaded: Boolean = false,
  val savedStations: List<SavedStation> = emptyList(),
  val isSyncing: Boolean = false,
  val isOffline: Boolean = false,
  val errorMessage: String? = null,
  val selectedHourIndex: Int? = null,
  val expandedDayIndex: Int? = null,
  val expandedMetricTags: Set<String> = emptySet(),
  val isStationSheetOpen: Boolean = false,
  val isSettingsOpen: Boolean = false,
  val searchQuery: String = "",
  val searchResults: List<GeoSearchResult> = emptyList(),
  val isSearching: Boolean = false,
  val isLocating: Boolean = false,
  val statusBanner: String? = null,
  val onlineFunFact: String? = null,
  val quipCycleOffset: Int = 0,
  val syncCompletedSeq: Int = 0,
  val activeSevereAlert: SevereWeatherAlert? = null,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WeatherViewModel(
  private val repository: WeatherRepository,
  private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

  private data class TransientUiState(
    val isSyncing: Boolean = false,
    val settingsLoaded: Boolean = false,
    val errorMessage: String? = null,
    val selectedHourIndex: Int? = null,
    val expandedDayIndex: Int? = null,
    val expandedMetricTags: Set<String> = emptySet(),
    val isStationSheetOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<GeoSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isLocating: Boolean = false,
    val statusBanner: String? = null,
    val onlineFunFact: String? = null,
    val quipCycleOffset: Int = 0,
    val syncCompletedSeq: Int = 0,
    val notificationRefreshSeq: Int = 0,
    val dismissedAlertIds: Set<String> = emptySet(),
  )

  private val transientState = MutableStateFlow(TransientUiState())
  private var searchJob: Job? = null
  private var autoRefreshJob: Job? = null
  private var lastNotifiedSevereAlertId: String? = null

  private val settingsFlow: StateFlow<UserSettings> =
    preferencesRepository.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = UserSettings(),
    )

  private val activeForecastFlow: FlowForecast =
    settingsFlow
      .map { it.activeStationId }
      .distinctUntilChanged()
      .flatMapLatest { stationId -> repository.observeCachedForecast(stationId) }

  val uiState: StateFlow<WeatherUiState> =
    combine(
        activeForecastFlow,
        settingsFlow,
        repository.observeSavedStations(),
        transientState,
      ) { forecast, settings, stations, transient ->
        val networkAvailable = repository.isNetworkAvailable()
        val offline = forecast?.isFromOfflineCache ?: !networkAvailable

        val evaluatedAlerts = SevereWeatherAlertEngine.evaluateAlerts(forecast)
        val activeAlert =
          if (settings.severeAlertOverlayEnabled) {
            evaluatedAlerts.firstOrNull { it.id !in transient.dismissedAlertIds }
          } else {
            null
          }

        WeatherUiState(
          forecast = forecast,
          settings = settings,
          settingsLoaded = transient.settingsLoaded,
          savedStations = stations,
          isSyncing = transient.isSyncing,
          isOffline = offline,
          errorMessage = transient.errorMessage,
          selectedHourIndex = transient.selectedHourIndex,
          expandedDayIndex = transient.expandedDayIndex,
          expandedMetricTags = transient.expandedMetricTags,
          isStationSheetOpen = transient.isStationSheetOpen,
          isSettingsOpen = transient.isSettingsOpen,
          searchQuery = transient.searchQuery,
          searchResults = transient.searchResults,
          isSearching = transient.isSearching,
          isLocating = transient.isLocating,
          statusBanner = transient.statusBanner,
          onlineFunFact = transient.onlineFunFact,
          quipCycleOffset = transient.quipCycleOffset,
          syncCompletedSeq = transient.syncCompletedSeq,
          activeSevereAlert = activeAlert,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeatherUiState(isSyncing = true),
      )

  init {
    viewModelScope.launch {
      preferencesRepository.settingsFlow.collect {
        if (!transientState.value.settingsLoaded) {
          transientState.update { it.copy(settingsLoaded = true) }
        }
      }
    }

    viewModelScope.launch {
      repository.ensureDefaultStationsSeeded()
      val activeId = settingsFlow.value.activeStationId
      syncWeather(activeId)
    }
    refreshOnlineFunFact()

    viewModelScope.launch {
      settingsFlow
        .map { it.autoRefreshInterval }
        .distinctUntilChanged()
        .collect { interval -> setupAutoRefreshJob(interval) }
    }

    viewModelScope.launch {
      combine(
          activeForecastFlow,
          settingsFlow,
          transientState.map { it.quipCycleOffset to it.notificationRefreshSeq }.distinctUntilChanged(),
        ) { forecast, settings, (offset, _) ->
          val ctx = GridcastApplication.instance
          if (forecast != null && settings.pinnedNotificationEnabled) {
            val quip =
              WeatherQuipEngine.buildQuipPool(forecast, seedOffset = offset).firstOrNull()
                ?: "Nice weather ngl."
            BlockyNotificationManager.updatePinnedNotification(
              context = ctx,
              forecast = forecast,
              settings = settings,
              quipText = quip,
            )
          } else if (!settings.pinnedNotificationEnabled) {
            BlockyNotificationManager.cancelPinnedNotification(ctx)
          }

          if (forecast != null && settings.severeAlertOverlayEnabled) {
            val topAlert = SevereWeatherAlertEngine.evaluateAlerts(forecast).firstOrNull()
            if (topAlert != null && topAlert.id != lastNotifiedSevereAlertId && BlockyNotificationManager.hasNotificationPermission(ctx)) {
              lastNotifiedSevereAlertId = topAlert.id
              BlockyNotificationManager.postSevereAlertNotification(
                context = ctx,
                alertTitle = topAlert.title,
                alertDesc = topAlert.description,
              )
            }
          }
        }
        .collect {}
    }
  }

  private fun setupAutoRefreshJob(interval: AutoRefreshInterval) {
    autoRefreshJob?.cancel()
    if (interval == AutoRefreshInterval.OFF) return
    autoRefreshJob = viewModelScope.launch {
      while (coroutineContext[Job]?.isActive == true) {
        delay(interval.minutes * 60 * 1000L)
        refreshCurrentStation()
      }
    }
  }

  fun refreshCurrentStation() {
    val activeId = settingsFlow.value.activeStationId
    transientState.update { it.copy(quipCycleOffset = it.quipCycleOffset + 1) }
    syncWeather(activeId, minVisualDurationMs = 680L)
    refreshOnlineFunFact()
  }

  fun cycleHeroQuip() {
    transientState.update { it.copy(quipCycleOffset = it.quipCycleOffset + 1) }
    refreshOnlineFunFact()
  }

  private fun refreshOnlineFunFact() {
    viewModelScope.launch {
      val fact = repository.fetchOnlineWeatherFunFact()
      if (!fact.isNullOrBlank()) {
        transientState.update { it.copy(onlineFunFact = fact) }
      }
    }
  }

  private fun syncWeather(stationId: String, minVisualDurationMs: Long = 0L) {
    viewModelScope.launch {
      val startedAtMs = System.currentTimeMillis()
      transientState.update {
        it.copy(isSyncing = true, errorMessage = null, statusBanner = null)
      }
      val result = repository.syncStationWeather(stationId)
      if (minVisualDurationMs > 0L) {
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        val remainingMs = minVisualDurationMs - elapsedMs
        if (remainingMs > 0L) {
          kotlinx.coroutines.delay(remainingMs)
        }
      }
      result.fold(
        onSuccess = { forecast ->
          transientState.update {
            it.copy(
              isSyncing = false,
              syncCompletedSeq = it.syncCompletedSeq + 1,
              errorMessage = null,
              statusBanner =
                if (forecast.isFromOfflineCache) {
                  "Offline — displaying cached Room snapshot"
                } else {
                  null
                },
            )
          }
        },
        onFailure = { err ->
          transientState.update {
            it.copy(
              isSyncing = false,
              errorMessage =
                err.localizedMessage ?: "Unable to reach weather grid and no offline cache exists.",
            )
          }
        },
      )
    }
  }

  fun setDarkMode(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDarkMode(enabled) }
  }

  fun toggleDarkMode() {
    setDarkMode(!settingsFlow.value.isDarkMode)
  }

  fun setUseSolarDayNightTheme(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseSolarDayNightTheme(enabled) }
  }

  fun setUseStandardHeroFont(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseStandardHeroFont(enabled) }
  }

  fun setUseFahrenheit(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseFahrenheit(enabled) }
  }

  fun toggleTemperatureUnit() {
    setUseFahrenheit(!settingsFlow.value.useFahrenheit)
  }

  fun setDynamicTheme(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setUseDynamicTheme(enabled) }
  }

  fun setShowHeroNudges(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setShowHeroNudges(enabled) }
  }

  fun setRefreshRateMode(mode: RefreshRateMode) {
    viewModelScope.launch { preferencesRepository.setRefreshRateMode(mode) }
  }

  fun setAutoRefreshInterval(interval: AutoRefreshInterval) {
    viewModelScope.launch { preferencesRepository.setAutoRefreshInterval(interval) }
  }

  fun setPinnedNotificationEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setPinnedNotificationEnabled(enabled)
      transientState.update { it.copy(notificationRefreshSeq = it.notificationRefreshSeq + 1) }
    }
  }

  fun setSevereAlertOverlayEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setSevereAlertOverlayEnabled(enabled) }
  }

  fun refreshNotificationState() {
    transientState.update { it.copy(notificationRefreshSeq = it.notificationRefreshSeq + 1) }
  }

  fun markInitialPermissionsPrompted() {
    viewModelScope.launch { preferencesRepository.markInitialPermissionsPrompted() }
  }

  fun updateAtmosphericCardOrder(newOrder: List<String>) {
    viewModelScope.launch { preferencesRepository.setAtmosphericCardOrder(newOrder) }
  }

  fun dismissSevereAlert(alertId: String) {
    transientState.update { it.copy(dismissedAlertIds = it.dismissedAlertIds + alertId) }
  }

  fun toggleEcoGridMode() {
    val nextMode =
      if (settingsFlow.value.refreshRateMode == RefreshRateMode.BATTERY_SAVER) {
        RefreshRateMode.DEFAULT
      } else {
        RefreshRateMode.BATTERY_SAVER
      }
    setRefreshRateMode(nextMode)
  }

  fun selectHour(index: Int) {
    transientState.update { state ->
      val next = if (state.selectedHourIndex == index) null else index
      state.copy(selectedHourIndex = next)
    }
  }

  fun clearSelectedHour() {
    transientState.update { it.copy(selectedHourIndex = null) }
  }

  fun toggleExpandDay(index: Int) {
    transientState.update { state ->
      val next = if (state.expandedDayIndex == index) null else index
      state.copy(expandedDayIndex = next)
    }
  }

  fun toggleExpandMetric(tag: String) {
    transientState.update { state ->
      val next =
        if (tag in state.expandedMetricTags) {
          state.expandedMetricTags - tag
        } else {
          state.expandedMetricTags + tag
        }
      state.copy(expandedMetricTags = next)
    }
  }

  fun openSettings() {
    transientState.update { it.copy(isSettingsOpen = true, isStationSheetOpen = false) }
  }

  fun closeSettings() {
    transientState.update { it.copy(isSettingsOpen = false) }
  }

  fun openStationSheet() {
    transientState.update {
      it.copy(isStationSheetOpen = true, isSettingsOpen = false, statusBanner = null)
    }
  }

  fun closeStationSheet() {
    transientState.update {
      it.copy(isStationSheetOpen = false, searchQuery = "", searchResults = emptyList())
    }
  }

  fun onSearchQueryChanged(query: String) {
    transientState.update { it.copy(searchQuery = query) }
    searchJob?.cancel()
    if (query.trim().length < 2) {
      transientState.update { it.copy(searchResults = emptyList(), isSearching = false) }
      return
    }
    searchJob =
      viewModelScope.launch {
        delay(280L)
        transientState.update { it.copy(isSearching = true) }
        val res = repository.searchLocations(query)
        res.fold(
          onSuccess = { results ->
            transientState.update { it.copy(searchResults = results, isSearching = false) }
          },
          onFailure = {
            transientState.update {
              it.copy(
                searchResults = emptyList(),
                isSearching = false,
                statusBanner = "Search offline — select from cached stations below",
              )
            }
          },
        )
      }
  }

  fun selectSavedStation(stationId: String) {
    viewModelScope.launch {
      preferencesRepository.setActiveStationId(stationId)
      transientState.update {
        it.copy(
          isStationSheetOpen = false,
          selectedHourIndex = null,
          quipCycleOffset = 0,
          searchQuery = "",
          searchResults = emptyList(),
        )
      }
      syncWeather(stationId)
      refreshOnlineFunFact()
    }
  }

  fun selectGeoSearchResult(result: GeoSearchResult) {
    viewModelScope.launch {
      val stationId = repository.saveAndSelectGeoResult(result)
      preferencesRepository.setActiveStationId(stationId)
      transientState.update {
        it.copy(
          isStationSheetOpen = false,
          selectedHourIndex = null,
          quipCycleOffset = 0,
          searchQuery = "",
          searchResults = emptyList(),
        )
      }
      syncWeather(stationId)
      refreshOnlineFunFact()
    }
  }

  fun deleteStation(stationId: String) {
    viewModelScope.launch {
      val wasActive = settingsFlow.value.activeStationId == stationId
      repository.deleteStation(stationId)
      if (wasActive) {
        preferencesRepository.setActiveStationId("station_san_francisco")
        syncWeather("station_san_francisco")
      }
    }
  }

  fun hasCoarseLocationPermission(): Boolean = repository.hasCoarseLocationPermission()

  fun useCurrentCoarseLocation() {
    viewModelScope.launch {
      transientState.update { it.copy(isLocating = true, statusBanner = null) }
      val locResult = repository.acquireCurrentLocationStation()
      locResult.fold(
        onSuccess = { stationId ->
          preferencesRepository.setActiveStationId(stationId)
          transientState.update {
            it.copy(
              isLocating = false,
              isStationSheetOpen = false,
              selectedHourIndex = null,
            )
          }
          syncWeather(stationId)
        },
        onFailure = {
          transientState.update {
            it.copy(
              isLocating = false,
              statusBanner = "Unable to acquire coarse location fix right now.",
            )
          }
        },
      )
    }
  }

  fun onLocationPermissionDenied() {
    transientState.update {
      it.copy(statusBanner = "Location permission denied. Search any city manually.")
    }
  }

  class Factory(
    private val repository: WeatherRepository,
    private val preferencesRepository: UserPreferencesRepository,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
      WeatherViewModel(repository, preferencesRepository) as T
  }
}

private typealias FlowForecast = kotlinx.coroutines.flow.Flow<WeatherForecast?>
