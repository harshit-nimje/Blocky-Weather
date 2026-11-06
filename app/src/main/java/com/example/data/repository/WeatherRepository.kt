package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.example.data.local.WeatherStationDao
import com.example.data.local.WeatherStationEntity
import com.example.data.remote.NetworkClient
import com.example.data.remote.OpenMeteoAirQualityResponse
import com.example.data.remote.OpenMeteoAirQualityService
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.data.remote.OpenMeteoForecastService
import com.example.data.remote.OpenMeteoGeocodingService
import com.example.data.remote.WikipediaSummaryService
import com.example.domain.model.DailyPoint
import com.example.domain.model.GeoSearchResult
import com.example.domain.model.HourlyPoint
import com.example.domain.model.SavedStation
import com.example.domain.model.WeatherConditionType
import com.example.domain.model.WeatherForecast
import com.squareup.moshi.Moshi
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class WeatherRepository(
  private val context: Context,
  private val stationDao: WeatherStationDao,
  private val forecastService: OpenMeteoForecastService = NetworkClient.forecastService,
  private val geocodingService: OpenMeteoGeocodingService = NetworkClient.geocodingService,
  private val airQualityService: OpenMeteoAirQualityService = NetworkClient.airQualityService,
  private val wikiSummaryService: WikipediaSummaryService = NetworkClient.wikiSummaryService,
  private val moshi: Moshi = NetworkClient.moshi,
) {
  private val forecastAdapter = moshi.adapter(WeatherForecast::class.java)

  suspend fun ensureDefaultStationsSeeded() {
    withContext(Dispatchers.IO) {
      if (stationDao.countStations() == 0) {
        val defaults =
          listOf(
            WeatherStationEntity(
              id = "station_san_francisco",
              name = "San Francisco",
              admin1 = "California",
              country = "United States",
              latitude = 37.7749,
              longitude = -122.4194,
              cachedForecastJson = null,
              lastTempC = null,
              lastConditionName = null,
              lastUpdatedMillis = null,
              isPinned = true,
              sortOrder = 0,
            ),
            WeatherStationEntity(
              id = "station_tokyo",
              name = "Tokyo",
              admin1 = "Tokyo",
              country = "Japan",
              latitude = 35.6762,
              longitude = 139.6503,
              cachedForecastJson = null,
              lastTempC = null,
              lastConditionName = null,
              lastUpdatedMillis = null,
              isPinned = true,
              sortOrder = 1,
            ),
            WeatherStationEntity(
              id = "station_zurich",
              name = "Zurich",
              admin1 = "Zurich",
              country = "Switzerland",
              latitude = 47.3769,
              longitude = 8.5417,
              cachedForecastJson = null,
              lastTempC = null,
              lastConditionName = null,
              lastUpdatedMillis = null,
              isPinned = true,
              sortOrder = 2,
            ),
            WeatherStationEntity(
              id = "station_reykjavik",
              name = "Reykjavik",
              admin1 = "Capital Region",
              country = "Iceland",
              latitude = 64.1466,
              longitude = -21.9426,
              cachedForecastJson = null,
              lastTempC = null,
              lastConditionName = null,
              lastUpdatedMillis = null,
              isPinned = true,
              sortOrder = 3,
            ),
          )
        stationDao.insertIgnoreAll(defaults)
      }
    }
  }

  fun observeSavedStations(): Flow<List<SavedStation>> =
    stationDao.observeAllStations().map { entities ->
      entities.map { entity ->
        SavedStation(
          id = entity.id,
          name = entity.name,
          admin1 = entity.admin1,
          country = entity.country,
          latitude = entity.latitude,
          longitude = entity.longitude,
          lastTempC = entity.lastTempC,
          lastCondition =
            entity.lastConditionName?.let { name ->
              runCatching { WeatherConditionType.valueOf(name) }.getOrNull()
            },
          lastUpdatedMillis = entity.lastUpdatedMillis,
          isPinned = entity.isPinned,
          hasOfflineCache = entity.cachedForecastJson != null,
        )
      }
    }

  fun observeCachedForecast(stationId: String): Flow<WeatherForecast?> =
    stationDao.observeStationById(stationId).map { entity ->
      entity?.cachedForecastJson?.let { json ->
        runCatching { forecastAdapter.fromJson(json) }
          .getOrNull()
          ?.copy(isFromOfflineCache = !isNetworkAvailable())
      }
    }

  fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  suspend fun syncStationWeather(stationId: String): Result<WeatherForecast> =
    withContext(Dispatchers.IO) {
      ensureDefaultStationsSeeded()
      val station =
        stationDao.getStationById(stationId)
          ?: stationDao.getStationById("station_san_francisco")
          ?: return@withContext Result.failure(IllegalStateException("No station available"))

      val cachedForecast =
        station.cachedForecastJson?.let { json ->
          runCatching { forecastAdapter.fromJson(json) }.getOrNull()
        }

      try {
        val (response, airQuality) =
          coroutineScope {
            val forecastDeferred = async {
              forecastService.getForecast(
                latitude = station.latitude,
                longitude = station.longitude,
              )
            }
            val airDeferred = async {
              runCatching {
                  airQualityService.getAirQuality(
                    latitude = station.latitude,
                    longitude = station.longitude,
                  )
                }
                .getOrNull()
            }
            forecastDeferred.await() to airDeferred.await()
          }
        val mapped = mapResponseToForecast(station, response, airQuality)
        val serialized = forecastAdapter.toJson(mapped)
        stationDao.upsertStation(
          station.copy(
            cachedForecastJson = serialized,
            lastTempC = mapped.currentTempC,
            lastConditionName = mapped.condition.name,
            lastUpdatedMillis = mapped.fetchedAtMillis,
          )
        )
        Result.success(mapped.copy(isFromOfflineCache = false))
      } catch (e: Exception) {
        if (cachedForecast != null) {
          Result.success(cachedForecast.copy(isFromOfflineCache = true))
        } else {
          Result.failure(e)
        }
      }
    }

  suspend fun fetchOnlineWeatherFunFact(): String? =
    withContext(Dispatchers.IO) {
      val topics =
        listOf(
          "Petrichor",
          "Cumulonimbus",
          "Rayleigh_scattering",
          "Ball_lightning",
          "Cloud_iridescence",
          "Aurora",
          "Trade_winds",
          "Snowflake",
          "Foehn_wind",
          "Atmospheric_pressure",
        )
      val topic = topics.random()
      runCatching {
          val summary =
            withTimeoutOrNull(4000L) { wikiSummaryService.getSummary(topic) } ?: return@runCatching null
          val raw = summary.extract.orEmpty().trim()
          if (raw.isEmpty()) return@runCatching null
          val firstSentence = raw.substringBefore(". ").trim().trimEnd('.') + "."
          if (firstSentence.length in 18..170) {
            firstSentence
          } else {
            firstSentence.take(150).trimEnd() + "..."
          }
        }
        .getOrNull()
    }

  suspend fun searchLocations(query: String): Result<List<GeoSearchResult>> =
    withContext(Dispatchers.IO) {
      val trimmed = query.trim()
      if (trimmed.length < 2) return@withContext Result.success(emptyList())
      try {
        val resp = geocodingService.searchLocations(query = trimmed)
        val list =
          resp.results.orEmpty().map { dto ->
            GeoSearchResult(
              id = dto.id,
              name = dto.name,
              admin1 = dto.admin1,
              country = dto.country.orEmpty(),
              latitude = dto.latitude,
              longitude = dto.longitude,
              timezone = dto.timezone,
            )
          }
        Result.success(list)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  suspend fun saveAndSelectGeoResult(result: GeoSearchResult): String =
    withContext(Dispatchers.IO) {
      val stationId = "station_geo_${result.id}"
      val existing = stationDao.getStationById(stationId)
      if (existing == null) {
        stationDao.upsertStation(
          WeatherStationEntity(
            id = stationId,
            name = result.name,
            admin1 = result.admin1,
            country = result.country,
            latitude = result.latitude,
            longitude = result.longitude,
            cachedForecastJson = null,
            lastTempC = null,
            lastConditionName = null,
            lastUpdatedMillis = null,
            isPinned = false,
            sortOrder = 10,
          )
        )
      }
      stationId
    }

  suspend fun deleteStation(stationId: String) {
    withContext(Dispatchers.IO) { stationDao.deleteStationById(stationId) }
  }

  fun hasCoarseLocationPermission(): Boolean {
    val coarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val fine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return coarse || fine
  }

  @SuppressLint("MissingPermission")
  suspend fun acquireCurrentLocationStation(): Result<String> =
    withContext(Dispatchers.IO) {
      if (!hasCoarseLocationPermission()) {
        return@withContext Result.failure(SecurityException("Coarse location permission required"))
      }
      val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
          ?: return@withContext Result.failure(IllegalStateException("LocationManager unavailable"))

      val location =
        getBestLocation(locationManager)
          ?: return@withContext Result.failure(IllegalStateException("Location unavailable"))

      val (cityName, admin1, country) = reverseGeocodeCity(location.latitude, location.longitude)
      val stationId = "station_device_gps"
      val existing = stationDao.getStationById(stationId)
      stationDao.upsertStation(
        WeatherStationEntity(
          id = stationId,
          name = cityName,
          admin1 = admin1,
          country = country,
          latitude = location.latitude,
          longitude = location.longitude,
          cachedForecastJson = existing?.cachedForecastJson,
          lastTempC = existing?.lastTempC,
          lastConditionName = existing?.lastConditionName,
          lastUpdatedMillis = existing?.lastUpdatedMillis,
          isPinned = true,
          sortOrder = -1,
        )
      )
      Result.success(stationId)
    }

  @SuppressLint("MissingPermission")
  private suspend fun getBestLocation(lm: LocationManager): Location? {
    val providers =
      listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
      )
    val lastKnown =
      providers
        .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }

    if (
      lastKnown != null &&
        System.currentTimeMillis() - lastKnown.time < 15 * 60 * 1000L
    ) {
      return lastKnown
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val activeProvider =
        providers.firstOrNull { p -> runCatching { lm.isProviderEnabled(p) }.getOrDefault(false) }
      if (activeProvider != null) {
        val fresh =
          withTimeoutOrNull(4500L) {
            suspendCancellableCoroutine<Location?> { cont ->
              val signal = CancellationSignal()
              cont.invokeOnCancellation { signal.cancel() }
              runCatching {
                  lm.getCurrentLocation(
                    activeProvider,
                    signal,
                    ContextCompat.getMainExecutor(context),
                  ) { loc ->
                    if (cont.isActive) cont.resume(loc)
                  }
                }
                .onFailure { if (cont.isActive) cont.resume(null) }
            }
          }
        if (fresh != null) return fresh
      }
    }
    return lastKnown
  }

  @Suppress("DEPRECATION")
  private fun reverseGeocodeCity(lat: Double, lon: Double): Triple<String, String?, String> {
    try {
      if (Geocoder.isPresent()) {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        val first = addresses?.firstOrNull()
        if (first != null) {
          val city =
            first.locality
              ?: first.subAdminArea
              ?: first.adminArea
              ?: "Local Grid"
          val admin = first.adminArea
          val country = first.countryName ?: "Coordinates"
          return Triple(city, admin, country)
        }
      }
    } catch (_: Exception) {
      // Fallback to coordinate label when offline or geocoder unavailable
    }
    val latStr = "${((lat * 100).roundToInt() / 100.0)}°"
    val lonStr = "${((lon * 100).roundToInt() / 100.0)}°"
    return Triple("Local Grid", "$latStr, $lonStr", "GPS Telemetry")
  }

  private fun mapResponseToForecast(
    station: WeatherStationEntity,
    resp: OpenMeteoForecastResponse,
    airQuality: OpenMeteoAirQualityResponse? = null,
  ): WeatherForecast {
    val current = resp.current
    val hourlyDto = resp.hourly
    val dailyDto = resp.daily

    val isDayNow = (current?.isDay ?: 1) == 1
    val currentCode = current?.weatherCode ?: 0
    val currentCondition = WeatherConditionType.fromWmoCode(currentCode, isDayNow)

    val currentHourPrefix = current?.time?.take(13)
    val startIdx =
      if (hourlyDto != null && currentHourPrefix != null) {
        hourlyDto.time.indexOfFirst { it.startsWith(currentHourPrefix) }.coerceAtLeast(0)
      } else {
        0
      }

    val currentUv = hourlyDto?.uvIndex?.getOrNull(startIdx) ?: 0.0
    val currentVisibility = hourlyDto?.visibility?.getOrNull(startIdx) ?: 10000.0
    val currentDewPoint = hourlyDto?.dewPoint2m?.getOrNull(startIdx) ?: 10.0

    val hourlyPoints =
      if (hourlyDto != null) {
        val endIdx = (startIdx + 24).coerceAtMost(hourlyDto.time.size)
        (startIdx until endIdx).mapIndexed { offset, idx ->
          val rawTime = hourlyDto.time[idx]
          val hourStr = rawTime.substringAfter('T', "00:00").take(5)
          val hIsDay = (hourlyDto.isDay.getOrNull(idx) ?: 1) == 1
          val hCode = hourlyDto.weatherCode.getOrNull(idx) ?: currentCode
          HourlyPoint(
            timeIso = rawTime,
            hourLabel = if (offset == 0) "NOW" else hourStr,
            tempC = hourlyDto.temperature2m.getOrNull(idx) ?: (current?.temperature2m ?: 20.0),
            precipProbPercent = hourlyDto.precipitationProbability.getOrNull(idx) ?: 0,
            precipMm = hourlyDto.precipitation.getOrNull(idx) ?: 0.0,
            windSpeedKmh = hourlyDto.windSpeed10m.getOrNull(idx) ?: 0.0,
            uvIndex = hourlyDto.uvIndex.getOrNull(idx) ?: 0.0,
            condition = WeatherConditionType.fromWmoCode(hCode, hIsDay),
            isDay = hIsDay,
          )
        }
      } else {
        emptyList()
      }

    val inDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val outDayFormat = SimpleDateFormat("EEE", Locale.US)
    val outDateShort = SimpleDateFormat("MMM dd", Locale.US)

    val dailyPoints =
      if (dailyDto != null) {
        dailyDto.time.indices.map { idx ->
          val dateStr = dailyDto.time[idx]
          val parsedDate = runCatching { inDateFormat.parse(dateStr) }.getOrNull()
          val dayLabel =
            if (idx == 0) {
              "TODAY"
            } else {
              parsedDate?.let { outDayFormat.format(it).uppercase(Locale.US) } ?: dateStr
            }
          val dateShort =
            parsedDate?.let { outDateShort.format(it).uppercase(Locale.US) } ?: dateStr
          val dCode = dailyDto.weatherCode.getOrNull(idx) ?: 0
          DailyPoint(
            dateIso = dateStr,
            dayLabel = dayLabel,
            dateShort = dateShort,
            tempMinC = dailyDto.temperature2mMin.getOrNull(idx) ?: 12.0,
            tempMaxC = dailyDto.temperature2mMax.getOrNull(idx) ?: 22.0,
            apparentMinC = dailyDto.apparentTemperatureMin.getOrNull(idx) ?: 11.0,
            apparentMaxC = dailyDto.apparentTemperatureMax.getOrNull(idx) ?: 22.0,
            precipProbMaxPercent = dailyDto.precipitationProbabilityMax.getOrNull(idx) ?: 0,
            precipSumMm = dailyDto.precipitationSum.getOrNull(idx) ?: 0.0,
            windMaxKmh = dailyDto.windSpeed10mMax.getOrNull(idx) ?: 0.0,
            uvMax = dailyDto.uvIndexMax.getOrNull(idx) ?: 0.0,
            sunriseIso = dailyDto.sunrise.getOrNull(idx).orEmpty(),
            sunsetIso = dailyDto.sunset.getOrNull(idx).orEmpty(),
            condition = WeatherConditionType.fromWmoCode(dCode, true),
          )
        }
      } else {
        emptyList()
      }

    val regionCountry =
      listOfNotNull(station.admin1?.takeIf { it.isNotBlank() && it != station.name }, station.country)
        .joinToString(" · ")
        .ifBlank { "GRID TELEMETRY" }

    return WeatherForecast(
      locationId = station.id,
      cityName = station.name,
      regionCountry = regionCountry,
      latitude = resp.latitude,
      longitude = resp.longitude,
      timezone = resp.timezone ?: "UTC",
      currentTempC = current?.temperature2m ?: 20.0,
      apparentTempC = current?.apparentTemperature ?: (current?.temperature2m ?: 20.0),
      humidityPercent = current?.relativeHumidity2m ?: 50,
      dewPointC = currentDewPoint,
      pressureHpa = current?.surfacePressure ?: 1013.0,
      cloudCoverPercent = current?.cloudCover ?: 0,
      windSpeedKmh = current?.windSpeed10m ?: 0.0,
      windDirectionDeg = current?.windDirection10m ?: 0,
      windGustsKmh = current?.windGusts10m ?: 0.0,
      precipitationMm = current?.precipitation ?: 0.0,
      uvIndex = currentUv,
      visibilityMeters = currentVisibility,
      isDay = isDayNow,
      condition = currentCondition,
      conditionLabel = currentCondition.displayTitle,
      hourly = hourlyPoints,
      daily = dailyPoints,
      fetchedAtMillis = System.currentTimeMillis(),
      isFromOfflineCache = false,
      usAqi = airQuality?.current?.usAqi,
      pm25 = airQuality?.current?.pm25,
    )
  }
}
