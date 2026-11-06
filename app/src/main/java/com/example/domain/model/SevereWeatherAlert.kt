package com.example.domain.model

import java.util.Locale

enum class AlertSeverity(val label: String, val levelCode: Int) {
  ADVISORY("ADVISORY", 1),
  WARNING("WARNING", 2),
  CRITICAL("EMERGENCY", 3)
}

data class SevereWeatherAlert(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val severity: AlertSeverity,
  val metricTag: String,
)

object SevereWeatherAlertEngine {
  fun evaluateAlerts(forecast: WeatherForecast?): List<SevereWeatherAlert> {
    if (forecast == null) return emptyList()
    val alerts = mutableListOf<SevereWeatherAlert>()

    // 1. Thunderstorm / Crackling Lightning
    if (forecast.condition == WeatherConditionType.THUNDERSTORM) {
      alerts.add(
        SevereWeatherAlert(
          id = "thunderstorm_${forecast.locationId}",
          title = "THUNDERSTORM & LIGHTNING HAZARD",
          category = "ELECTRICAL STORM",
          description = "*Thunder Crackles* Convective electrical storm active. Avoid tall trees and open areas!",
          severity = AlertSeverity.CRITICAL,
          metricTag = "metric_precip",
        )
      )
    }

    // 2. Heavy Rain & Flood Risk
    if (forecast.condition == WeatherConditionType.HEAVY_RAIN || forecast.precipitationMm > 12.0) {
      alerts.add(
        SevereWeatherAlert(
          id = "heavy_rain_${forecast.locationId}",
          title = "HEAVY DOWNPOUR & FLOOD RISK",
          category = "PRECIPITATION",
          description = "Rain gonna fill up to your ankles today. Harvest some rain water dude!",
          severity = AlertSeverity.WARNING,
          metricTag = "metric_precip",
        )
      )
    }

    // 3. High Wind / Gale Force
    if (forecast.windGustsKmh >= 52.0 || forecast.windSpeedKmh >= 42.0) {
      alerts.add(
        SevereWeatherAlert(
          id = "high_wind_${forecast.locationId}",
          title = "GALE FORCE WIND GUSTS",
          category = "WIND SPEEDS",
          description = "Peak gusts reaching ${forecast.windGustsKmh.toInt()} km/h. Secure loose outdoor objects!",
          severity = AlertSeverity.WARNING,
          metricTag = "metric_wind",
        )
      )
    }

    // 4. Extreme UV Radiation
    if (forecast.uvIndex >= 8.0) {
      alerts.add(
        SevereWeatherAlert(
          id = "extreme_uv_${forecast.locationId}",
          title = "EXTREME UV RADIATION HAZARD",
          category = "SOLAR RADIATION",
          description = "UV Index at ${String.format(Locale.US, "%.1f", forecast.uvIndex)}. Skin burn alert — use high quality >SPF 50 sunscreen!",
          severity = AlertSeverity.WARNING,
          metricTag = "metric_uv",
        )
      )
    }

    // 5. Hazardous Air Quality (AQI)
    val aqi = forecast.usAqi ?: 0
    if (aqi >= 150) {
      alerts.add(
        SevereWeatherAlert(
          id = "hazardous_aqi_${forecast.locationId}",
          title = "HAZARDOUS AIR QUALITY (AQI $aqi)",
          category = "POLLUTION",
          description = "Honey, where is my mask? Lungs are gonna complain today!",
          severity = if (aqi >= 200) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
          metricTag = "metric_aqi",
        )
      )
    }

    // 6. Extreme Heatwave
    if (forecast.currentTempC >= 36.0 || forecast.apparentTempC >= 38.0) {
      alerts.add(
        SevereWeatherAlert(
          id = "extreme_heat_${forecast.locationId}",
          title = "EXTREME HEATWAVE WARNING",
          category = "TEMPERATURE",
          description = "Basically a nuclear fusion outside (${forecast.currentTempC.toInt()}°C). Sweating buckets — stay inside with AC!",
          severity = AlertSeverity.CRITICAL,
          metricTag = "metric_real_feel",
        )
      )
    }

    // 7. Freezing Blizzard / Severe Chill
    if (forecast.currentTempC <= -3.0) {
      alerts.add(
        SevereWeatherAlert(
          id = "freezing_chill_${forecast.locationId}",
          title = "FREEZING CHILL & ICE ALERT",
          category = "TEMPERATURE",
          description = "brrrrr, so coldddd (${forecast.currentTempC.toInt()}°C). Stay wrapped up in blankies!",
          severity = AlertSeverity.WARNING,
          metricTag = "metric_real_feel",
        )
      )
    }

    return alerts
  }
}
