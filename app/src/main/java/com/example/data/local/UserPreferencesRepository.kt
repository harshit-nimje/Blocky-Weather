package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gridcastDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "gridcast_preferences")

enum class RefreshRateMode(val storageValue: String) {
  DEFAULT("default"),
  PEAK("peak"),
  BATTERY_SAVER("battery_saver");

  companion object {
    fun fromStorageValue(raw: String?): RefreshRateMode =
      entries.firstOrNull { it.storageValue == raw } ?: DEFAULT
  }
}

enum class AutoRefreshInterval(val minutes: Int, val label: String) {
  OFF(0, "OFF"),
  MIN_15(15, "15 MINS"),
  MIN_30(30, "30 MINS"),
  HOUR_1(60, "1 HOUR"),
  HOUR_3(180, "3 HOURS");

  companion object {
    fun fromMinutes(min: Int?): AutoRefreshInterval =
      entries.firstOrNull { it.minutes == min } ?: MIN_30
  }
}

val DEFAULT_ATMOSPHERIC_CARD_ORDER: List<String> =
  listOf(
    "metric_real_feel",
    "metric_aqi",
    "metric_wind",
    "metric_humidity",
    "metric_uv",
    "metric_pressure",
    "metric_precipitation",
    "metric_solar",
  )

internal fun normalizeAtmosphericCardOrder(order: List<String>): List<String> {
  val valid = order.filter { it in DEFAULT_ATMOSPHERIC_CARD_ORDER }.distinct()
  return valid + DEFAULT_ATMOSPHERIC_CARD_ORDER.filterNot { it in valid }
}

internal fun reorderAtmosphericCardsOnHover(
  currentTags: List<String>,
  movingTag: String,
  targetIndex: Int,
): List<String> {
  val fromIdx = currentTags.indexOf(movingTag)
  if (fromIdx == -1 || currentTags.isEmpty()) return currentTags
  val clampedTarget = targetIndex.coerceIn(0, currentTags.lastIndex)
  if (clampedTarget == fromIdx) return currentTags
  val updated = currentTags.toMutableList()
  updated.removeAt(fromIdx)
  updated.add(clampedTarget, movingTag)
  return updated
}

data class UserSettings(
  val isDarkMode: Boolean = false,
  val useFahrenheit: Boolean = false,
  val showHeroNudges: Boolean = true,
  val useDynamicTheme: Boolean = true,
  val useSolarDayNightTheme: Boolean = false,
  val useStandardHeroFont: Boolean = false,
  val reduceAnimations: Boolean = false,
  val appLanguage: String = "SYSTEM",
  val refreshRateMode: RefreshRateMode = RefreshRateMode.DEFAULT,
  val activeStationId: String = "station_san_francisco",
  val autoRefreshInterval: AutoRefreshInterval = AutoRefreshInterval.MIN_30,
  val pinnedNotificationEnabled: Boolean = true,
  val severeAlertOverlayEnabled: Boolean = true,
  val hasPromptedInitialPermissions: Boolean = false,
  val atmosphericCardOrder: List<String> = DEFAULT_ATMOSPHERIC_CARD_ORDER,
) {
  val ecoGridMode: Boolean
    get() = refreshRateMode == RefreshRateMode.BATTERY_SAVER || reduceAnimations
}

class UserPreferencesRepository(private val context: Context) {
  private object Keys {
    val DARK_MODE = booleanPreferencesKey("is_dark_mode")
    val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
    val SHOW_HERO_NUDGES = booleanPreferencesKey("show_hero_nudges")
    val USE_DYNAMIC_THEME = booleanPreferencesKey("use_dynamic_theme")
    val USE_SOLAR_DAY_NIGHT_THEME = booleanPreferencesKey("use_solar_day_night_theme")
    val USE_STANDARD_HERO_FONT = booleanPreferencesKey("use_standard_hero_font")
    val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
    val APP_LANGUAGE = stringPreferencesKey("app_language")
    val REFRESH_RATE_MODE = stringPreferencesKey("refresh_rate_mode")
    val ECO_GRID_MODE = booleanPreferencesKey("eco_grid_mode")
    val ACTIVE_STATION_ID = stringPreferencesKey("active_station_id")
    val AUTO_REFRESH_MINUTES = androidx.datastore.preferences.core.intPreferencesKey("auto_refresh_minutes")
    val PINNED_NOTIF_ENABLED = booleanPreferencesKey("pinned_notif_enabled")
    val SEVERE_ALERT_OVERLAY_ENABLED = booleanPreferencesKey("severe_alert_overlay_enabled")
    val HAS_PROMPTED_INITIAL_PERMISSIONS = booleanPreferencesKey("has_prompted_initial_permissions")
    val ATMOSPHERIC_CARD_ORDER = stringPreferencesKey("atmospheric_card_order")
  }

  val settingsFlow: Flow<UserSettings> =
    context.gridcastDataStore.data.map { prefs ->
      val storedHzRaw = prefs[Keys.REFRESH_RATE_MODE]
      val legacyEco = prefs[Keys.ECO_GRID_MODE] ?: false
      val resolvedHzMode =
        if (storedHzRaw != null) {
          RefreshRateMode.fromStorageValue(storedHzRaw)
        } else if (legacyEco) {
          RefreshRateMode.BATTERY_SAVER
        } else {
          RefreshRateMode.DEFAULT
        }

      val rawOrder = prefs[Keys.ATMOSPHERIC_CARD_ORDER]
      val resolvedOrder =
        if (rawOrder.isNullOrBlank()) {
          DEFAULT_ATMOSPHERIC_CARD_ORDER
        } else {
          normalizeAtmosphericCardOrder(rawOrder.split(','))
        }

      UserSettings(
        isDarkMode = prefs[Keys.DARK_MODE] ?: false,
        useFahrenheit = prefs[Keys.USE_FAHRENHEIT] ?: false,
        showHeroNudges = prefs[Keys.SHOW_HERO_NUDGES] ?: true,
        useDynamicTheme = prefs[Keys.USE_DYNAMIC_THEME] ?: true,
        useSolarDayNightTheme = prefs[Keys.USE_SOLAR_DAY_NIGHT_THEME] ?: false,
        useStandardHeroFont = prefs[Keys.USE_STANDARD_HERO_FONT] ?: false,
        reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false,
        appLanguage = prefs[Keys.APP_LANGUAGE] ?: "SYSTEM",
        refreshRateMode = resolvedHzMode,
        activeStationId = prefs[Keys.ACTIVE_STATION_ID] ?: "station_san_francisco",
        autoRefreshInterval = AutoRefreshInterval.fromMinutes(prefs[Keys.AUTO_REFRESH_MINUTES]),
        pinnedNotificationEnabled = prefs[Keys.PINNED_NOTIF_ENABLED] ?: true,
        severeAlertOverlayEnabled = prefs[Keys.SEVERE_ALERT_OVERLAY_ENABLED] ?: true,
        hasPromptedInitialPermissions = prefs[Keys.HAS_PROMPTED_INITIAL_PERMISSIONS] ?: false,
        atmosphericCardOrder = resolvedOrder,
      )
    }

  suspend fun setDarkMode(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.DARK_MODE] = enabled }
  }

  suspend fun setUseFahrenheit(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.USE_FAHRENHEIT] = enabled }
  }

  suspend fun setShowHeroNudges(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.SHOW_HERO_NUDGES] = enabled }
  }

  suspend fun setUseDynamicTheme(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.USE_DYNAMIC_THEME] = enabled }
  }

  suspend fun setUseSolarDayNightTheme(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.USE_SOLAR_DAY_NIGHT_THEME] = enabled }
  }

  suspend fun setUseStandardHeroFont(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.USE_STANDARD_HERO_FONT] = enabled }
  }

  suspend fun setReduceAnimations(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.REDUCE_ANIMATIONS] = enabled }
  }

  suspend fun setAppLanguage(languageCode: String) {
    context.gridcastDataStore.edit { it[Keys.APP_LANGUAGE] = languageCode }
  }

  suspend fun setRefreshRateMode(mode: RefreshRateMode) {
    context.gridcastDataStore.edit { prefs ->
      prefs[Keys.REFRESH_RATE_MODE] = mode.storageValue
      prefs[Keys.ECO_GRID_MODE] = (mode == RefreshRateMode.BATTERY_SAVER)
    }
  }

  suspend fun setEcoGridMode(enabled: Boolean) {
    setRefreshRateMode(if (enabled) RefreshRateMode.BATTERY_SAVER else RefreshRateMode.DEFAULT)
  }

  suspend fun setActiveStationId(stationId: String) {
    context.gridcastDataStore.edit { it[Keys.ACTIVE_STATION_ID] = stationId }
  }

  suspend fun setAutoRefreshInterval(interval: AutoRefreshInterval) {
    context.gridcastDataStore.edit { it[Keys.AUTO_REFRESH_MINUTES] = interval.minutes }
  }

  suspend fun setPinnedNotificationEnabled(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.PINNED_NOTIF_ENABLED] = enabled }
  }

  suspend fun setSevereAlertOverlayEnabled(enabled: Boolean) {
    context.gridcastDataStore.edit { it[Keys.SEVERE_ALERT_OVERLAY_ENABLED] = enabled }
  }

  suspend fun markInitialPermissionsPrompted() {
    context.gridcastDataStore.edit { it[Keys.HAS_PROMPTED_INITIAL_PERMISSIONS] = true }
  }

  suspend fun setAtmosphericCardOrder(order: List<String>) {
    val normalized = normalizeAtmosphericCardOrder(order)
    context.gridcastDataStore.edit {
      it[Keys.ATMOSPHERIC_CARD_ORDER] = normalized.joinToString(",")
    }
  }
}
