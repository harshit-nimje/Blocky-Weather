package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
  val latitude: Double,
  val longitude: Double,
  val timezone: String? = null,
  val current: CurrentWeatherDto? = null,
  val hourly: HourlyWeatherDto? = null,
  val daily: DailyWeatherDto? = null,
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
  val time: String? = null,
  @Json(name = "temperature_2m") val temperature2m: Double? = null,
  @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
  @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
  @Json(name = "is_day") val isDay: Int? = null,
  val precipitation: Double? = null,
  @Json(name = "weather_code") val weatherCode: Int? = null,
  @Json(name = "cloud_cover") val cloudCover: Int? = null,
  @Json(name = "surface_pressure") val surfacePressure: Double? = null,
  @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
  @Json(name = "wind_direction_10m") val windDirection10m: Int? = null,
  @Json(name = "wind_gusts_10m") val windGusts10m: Double? = null,
)

@JsonClass(generateAdapter = true)
data class HourlyWeatherDto(
  val time: List<String> = emptyList(),
  @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
  @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int?> = emptyList(),
  @Json(name = "dew_point_2m") val dewPoint2m: List<Double?> = emptyList(),
  @Json(name = "apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
  @Json(name = "precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
  val precipitation: List<Double?> = emptyList(),
  @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
  val visibility: List<Double?> = emptyList(),
  @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
  @Json(name = "uv_index") val uvIndex: List<Double?> = emptyList(),
  @Json(name = "is_day") val isDay: List<Int?> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DailyWeatherDto(
  val time: List<String> = emptyList(),
  @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
  @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?> = emptyList(),
  @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?> = emptyList(),
  @Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double?> = emptyList(),
  @Json(name = "apparent_temperature_min") val apparentTemperatureMin: List<Double?> = emptyList(),
  val sunrise: List<String?> = emptyList(),
  val sunset: List<String?> = emptyList(),
  @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
  @Json(name = "precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
  @Json(name = "precipitation_probability_max")
  val precipitationProbabilityMax: List<Int?> = emptyList(),
  @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double?> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GeocodingSearchResponse(val results: List<GeocodingPlaceDto>? = null)

@JsonClass(generateAdapter = true)
data class GeocodingPlaceDto(
  val id: Long,
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val country: String? = null,
  val admin1: String? = null,
  val timezone: String? = null,
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityResponse(
  val current: CurrentAirQualityDto? = null,
)

@JsonClass(generateAdapter = true)
data class CurrentAirQualityDto(
  @Json(name = "us_aqi") val usAqi: Int? = null,
  @Json(name = "pm2_5") val pm25: Double? = null,
  val pm10: Double? = null,
)

@JsonClass(generateAdapter = true)
data class WikipediaSummaryResponse(
  val title: String? = null,
  val extract: String? = null,
)

interface OpenMeteoForecastService {
  @GET("v1/forecast")
  suspend fun getForecast(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current")
    current: String =
      "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,cloud_cover,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m",
    @Query("hourly")
    hourly: String =
      "temperature_2m,relative_humidity_2m,dew_point_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,visibility,wind_speed_10m,uv_index,is_day",
    @Query("daily")
    daily: String =
      "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,uv_index_max,precipitation_sum,precipitation_probability_max,wind_speed_10m_max",
    @Query("timezone") timezone: String = "auto",
    @Query("forecast_days") forecastDays: Int = 7,
  ): OpenMeteoForecastResponse
}

interface OpenMeteoGeocodingService {
  @GET("v1/search")
  suspend fun searchLocations(
    @Query("name") query: String,
    @Query("count") count: Int = 12,
    @Query("language") language: String = "en",
    @Query("format") format: String = "json",
  ): GeocodingSearchResponse
}

interface OpenMeteoAirQualityService {
  @GET("v1/air-quality")
  suspend fun getAirQuality(
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("current") current: String = "us_aqi,pm2_5,pm10",
    @Query("timezone") timezone: String = "auto",
  ): OpenMeteoAirQualityResponse
}

interface WikipediaSummaryService {
  @GET("page/summary/{title}")
  suspend fun getSummary(
    @retrofit2.http.Path("title") title: String,
  ): WikipediaSummaryResponse
}
