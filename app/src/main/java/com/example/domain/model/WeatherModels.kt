package com.example.domain.model

import com.squareup.moshi.JsonClass

enum class WeatherConditionType(val shaderMode: Int, val displayTitle: String) {
  CLEAR_DAY(0, "CLEAR SKY"),
  CLEAR_NIGHT(0, "CLEAR NIGHT"),
  PARTLY_CLOUDY(1, "PARTLY CLOUDY"),
  OVERCAST(1, "OVERCAST"),
  FOG(5, "FOG & MIST"),
  DRIZZLE(2, "LIGHT DRIZZLE"),
  RAIN(2, "RAIN SHOWERS"),
  HEAVY_RAIN(2, "HEAVY RAIN"),
  SNOW(4, "SNOWFALL"),
  THUNDERSTORM(3, "THUNDERSTORM");

  companion object {
    fun fromWmoCode(code: Int, isDay: Boolean): WeatherConditionType =
      when (code) {
        0, 1 -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
        2 -> PARTLY_CLOUDY
        3 -> OVERCAST
        45, 48 -> FOG
        51, 53, 55, 56, 57 -> DRIZZLE
        61, 63, 80, 81 -> RAIN
        65, 66, 67, 82 -> HEAVY_RAIN
        71, 73, 75, 77, 85, 86 -> SNOW
        95, 96, 99 -> THUNDERSTORM
        else -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
      }
  }
}

@JsonClass(generateAdapter = true)
data class HourlyPoint(
  val timeIso: String,
  val hourLabel: String,
  val tempC: Double,
  val precipProbPercent: Int,
  val precipMm: Double,
  val windSpeedKmh: Double,
  val uvIndex: Double,
  val condition: WeatherConditionType,
  val isDay: Boolean,
)

@JsonClass(generateAdapter = true)
data class DailyPoint(
  val dateIso: String,
  val dayLabel: String,
  val dateShort: String,
  val tempMinC: Double,
  val tempMaxC: Double,
  val apparentMinC: Double,
  val apparentMaxC: Double,
  val precipProbMaxPercent: Int,
  val precipSumMm: Double,
  val windMaxKmh: Double,
  val uvMax: Double,
  val sunriseIso: String,
  val sunsetIso: String,
  val condition: WeatherConditionType,
)

@JsonClass(generateAdapter = true)
data class WeatherForecast(
  val locationId: String,
  val cityName: String,
  val regionCountry: String,
  val latitude: Double,
  val longitude: Double,
  val timezone: String,
  val currentTempC: Double,
  val apparentTempC: Double,
  val humidityPercent: Int,
  val dewPointC: Double,
  val pressureHpa: Double,
  val cloudCoverPercent: Int,
  val windSpeedKmh: Double,
  val windDirectionDeg: Int,
  val windGustsKmh: Double,
  val precipitationMm: Double,
  val uvIndex: Double,
  val visibilityMeters: Double,
  val isDay: Boolean,
  val condition: WeatherConditionType,
  val conditionLabel: String,
  val hourly: List<HourlyPoint>,
  val daily: List<DailyPoint>,
  val fetchedAtMillis: Long,
  val isFromOfflineCache: Boolean = false,
  val usAqi: Int? = null,
  val pm25: Double? = null,
)

data class SavedStation(
  val id: String,
  val name: String,
  val admin1: String?,
  val country: String,
  val latitude: Double,
  val longitude: Double,
  val lastTempC: Double?,
  val lastCondition: WeatherConditionType?,
  val lastUpdatedMillis: Long?,
  val isPinned: Boolean,
  val hasOfflineCache: Boolean,
)

data class GeoSearchResult(
  val id: Long,
  val name: String,
  val admin1: String?,
  val country: String,
  val latitude: Double,
  val longitude: Double,
  val timezone: String?,
)

fun Double.toDisplayTemp(useFahrenheit: Boolean): Int =
  if (useFahrenheit) {
    kotlin.math.round(this * 9.0 / 5.0 + 32.0).toInt()
  } else {
    kotlin.math.round(this).toInt()
  }

fun Int.toCompassCardinal(): String {
  val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
  val idx = (((this % 360) + 360) % 360 + 22) / 45 % 8
  return dirs[idx]
}
