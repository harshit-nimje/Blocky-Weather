package com.example

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.example.data.local.DEFAULT_ATMOSPHERIC_CARD_ORDER
import com.example.data.local.normalizeAtmosphericCardOrder
import com.example.data.local.reorderAtmosphericCardsOnHover
import com.example.data.remote.NetworkClient
import com.example.data.remote.OpenMeteoAirQualityResponse
import com.example.data.remote.OpenMeteoForecastResponse
import com.example.domain.model.DailyPoint
import com.example.domain.model.HourlyPoint
import com.example.domain.model.WeatherConditionType
import com.example.domain.model.WeatherForecast
import com.example.domain.model.WeatherQuipEngine
import com.example.domain.model.toCompassCardinal
import com.example.domain.model.toDisplayTemp
import com.example.gl.PixelGridController
import com.example.ui.components.accessibleStatusTextInk
import com.example.ui.components.snapSliderOffsetToStep
import com.example.ui.theme.enforceDynamicTokenContrast
import com.example.ui.theme.wcagContrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun wmoWeatherCodeMapping_coversKeyConditions() {
    assertEquals(WeatherConditionType.CLEAR_DAY, WeatherConditionType.fromWmoCode(0, true))
    assertEquals(WeatherConditionType.CLEAR_NIGHT, WeatherConditionType.fromWmoCode(0, false))
    assertEquals(WeatherConditionType.PARTLY_CLOUDY, WeatherConditionType.fromWmoCode(2, true))
    assertEquals(WeatherConditionType.OVERCAST, WeatherConditionType.fromWmoCode(3, true))
    assertEquals(WeatherConditionType.FOG, WeatherConditionType.fromWmoCode(45, true))
    assertEquals(WeatherConditionType.DRIZZLE, WeatherConditionType.fromWmoCode(53, true))
    assertEquals(WeatherConditionType.RAIN, WeatherConditionType.fromWmoCode(61, true))
    assertEquals(WeatherConditionType.HEAVY_RAIN, WeatherConditionType.fromWmoCode(65, true))
    assertEquals(WeatherConditionType.SNOW, WeatherConditionType.fromWmoCode(73, true))
    assertEquals(WeatherConditionType.THUNDERSTORM, WeatherConditionType.fromWmoCode(95, true))
  }

  @Test
  fun temperatureAndCompassConversions_areAccurate() {
    assertEquals(68, 20.0.toDisplayTemp(useFahrenheit = true))
    assertEquals(32, 0.0.toDisplayTemp(useFahrenheit = true))
    assertEquals(20, 20.4.toDisplayTemp(useFahrenheit = false))
    assertEquals("N", 0.toCompassCardinal())
    assertEquals("E", 90.toCompassCardinal())
    assertEquals("SW", 225.toCompassCardinal())
    assertEquals("NW", 315.toCompassCardinal())

    // Verify daily thermal range yellow fill ratio
    val lowC = 10.0
    val highC = 22.0
    val midRatio = ((16.0 - lowC) / (highC - lowC)).toFloat().coerceIn(0f, 1f)
    val highRatio = ((22.0 - lowC) / (highC - lowC)).toFloat().coerceIn(0f, 1f)
    val lowRatio = ((10.0 - lowC) / (highC - lowC)).toFloat().coerceIn(0f, 1f)
    assertEquals(0.5f, midRatio, 0.001f)
    assertEquals(1.0f, highRatio, 0.001f)
    assertEquals(0.0f, lowRatio, 0.001f)
  }

  @Test
  fun openMeteoJsonAndOfflineCacheRoundTrip_succeeds() {
    val sampleJson =
      """
      {
        "latitude": 37.7749,
        "longitude": -122.4194,
        "timezone": "America/Los_Angeles",
        "current": {
          "time": "2026-11-05T08:00",
          "temperature_2m": 19.2,
          "relative_humidity_2m": 62,
          "apparent_temperature": 18.8,
          "is_day": 1,
          "precipitation": 0.0,
          "weather_code": 2,
          "cloud_cover": 25,
          "surface_pressure": 1016.4,
          "wind_speed_10m": 14.0,
          "wind_direction_10m": 280,
          "wind_gusts_10m": 21.5
        }
      }
      """
        .trimIndent()

    val respAdapter = NetworkClient.moshi.adapter(OpenMeteoForecastResponse::class.java)
    val parsed = respAdapter.fromJson(sampleJson)
    assertNotNull(parsed)
    assertEquals(19.2, parsed!!.current!!.temperature2m!!, 0.01)
    assertEquals(2, parsed.current!!.weatherCode)

    val forecast =
      WeatherForecast(
        locationId = "station_san_francisco",
        cityName = "San Francisco",
        regionCountry = "California · United States",
        latitude = 37.7749,
        longitude = -122.4194,
        timezone = "America/Los_Angeles",
        currentTempC = 19.2,
        apparentTempC = 18.8,
        humidityPercent = 62,
        dewPointC = 11.4,
        pressureHpa = 1016.4,
        cloudCoverPercent = 25,
        windSpeedKmh = 14.0,
        windDirectionDeg = 280,
        windGustsKmh = 21.5,
        precipitationMm = 0.0,
        uvIndex = 4.2,
        visibilityMeters = 16000.0,
        isDay = true,
        condition = WeatherConditionType.PARTLY_CLOUDY,
        conditionLabel = "PARTLY CLOUDY",
        hourly =
          listOf(
            HourlyPoint(
              timeIso = "2026-11-05T08:00",
              hourLabel = "NOW",
              tempC = 19.2,
              precipProbPercent = 10,
              precipMm = 0.0,
              windSpeedKmh = 14.0,
              uvIndex = 4.2,
              condition = WeatherConditionType.PARTLY_CLOUDY,
              isDay = true,
            )
          ),
        daily =
          listOf(
            DailyPoint(
              dateIso = "2026-11-05",
              dayLabel = "TODAY",
              dateShort = "NOV 05",
              tempMinC = 13.0,
              tempMaxC = 22.5,
              apparentMinC = 12.5,
              apparentMaxC = 22.0,
              precipProbMaxPercent = 10,
              precipSumMm = 0.0,
              windMaxKmh = 21.5,
              uvMax = 5.5,
              sunriseIso = "2026-11-05T06:40",
              sunsetIso = "2026-11-05T17:10",
              condition = WeatherConditionType.PARTLY_CLOUDY,
            )
          ),
        fetchedAtMillis = 1762358400000L,
        isFromOfflineCache = true,
      )

    val cacheAdapter = NetworkClient.moshi.adapter(WeatherForecast::class.java)
    val encoded = cacheAdapter.toJson(forecast)
    val decoded = cacheAdapter.fromJson(encoded)
    assertNotNull(decoded)
    assertEquals("San Francisco", decoded!!.cityName)
    assertEquals(19.2, decoded.currentTempC, 0.01)
    assertEquals(1, decoded.hourly.size)
    assertEquals(1, decoded.daily.size)
  }

  @Test
  fun pixelGridController_advancesDiscreteTicksAndFidgets() {
    val controller = PixelGridController()
    val initialTick = controller.getTick()
    val initialFidget = controller.getFidgetCount()

    controller.updateWeatherState(
      condition = WeatherConditionType.DRIZZLE,
      isDarkMode = true,
      ecoMode = false,
      precipitationMm = 0.6,
    )
    val drizzleIntensity = controller.getPrecipitationIntensity()

    controller.updateWeatherState(
      condition = WeatherConditionType.RAIN,
      isDarkMode = true,
      ecoMode = false,
      precipitationMm = 3.5,
    )
    val rainIntensity = controller.getPrecipitationIntensity()

    controller.updateWeatherState(
      condition = WeatherConditionType.HEAVY_RAIN,
      isDarkMode = true,
      ecoMode = false,
      precipitationMm = 12.0,
    )
    val heavyIntensity = controller.getPrecipitationIntensity()

    assertTrue(drizzleIntensity in 0.20f..0.42f)
    assertTrue(rainIntensity > drizzleIntensity)
    assertTrue(heavyIntensity > rainIntensity)

    controller.setMotionTilt(0.64f)
    assertEquals(0.64f, controller.rainTiltSlope, 0.001f)
    controller.setMotionTilt(-3.5f)
    assertEquals(-1.15f, controller.rainTiltSlope, 0.001f)

    controller.advanceDiscreteTick()
    controller.triggerPlayfulFidget()

    assertTrue(controller.getTick() > initialTick)
    assertTrue(controller.getFidgetCount() > initialFidget)
    assertTrue(controller.isDarkMode())
    assertEquals(WeatherConditionType.HEAVY_RAIN, controller.getCondition())
  }

  @Test
  fun solarUpwardArchCurve_startsAtHorizonPeaksAtNoonAndReturnsToHorizon() {
    val horizonRow = 8.0f
    val peakLift = 6.5f
    fun archRowForX(xNorm: Float): Float {
      val centered = 2f * xNorm - 1f
      val archHeightFactor = 1f - centered * centered
      return horizonRow - archHeightFactor * peakLift
    }
    assertEquals(horizonRow, archRowForX(0f), 0.001f)
    assertEquals(horizonRow - peakLift, archRowForX(0.5f), 0.001f)
    assertEquals(horizonRow, archRowForX(1f), 0.001f)

    // Verify 1-cell blank aura bounds around 2x2 Sun and Moon never include occupied or border cells
    val sunLeftCol = 10
    val sunTopRow = 3
    val moonLeftCol = 24
    val moonTopRow = 6
    fun isInBlankAura(c: Int, r: Int): Boolean {
      val nearSun = c in (sunLeftCol - 1)..(sunLeftCol + 2) && r in (sunTopRow - 1)..(sunTopRow + 2)
      val nearMoon =
        c in (moonLeftCol - 1)..(moonLeftCol + 2) && r in (moonTopRow - 1)..(moonTopRow + 2)
      return nearSun || nearMoon
    }
    assertTrue(isInBlankAura(sunLeftCol - 1, sunTopRow))
    assertTrue(isInBlankAura(moonLeftCol + 2, moonTopRow + 2))
    assertTrue(!isInBlankAura(sunLeftCol - 2, sunTopRow))
    assertTrue(!isInBlankAura(moonLeftCol - 2, moonTopRow))
  }

  @Test
  fun pixelGridController_debouncesRapidTouchMovesAndQueuesCleanBursts() {
    val controller = PixelGridController()
    controller.onTouchDown(100f, 200f)
    assertTrue(controller.touchActive)

    // High-frequency tiny jitters under 26px must NOT spawn redundant bursts
    controller.onTouchMove(104f, 203f)
    controller.onTouchMove(109f, 207f)
    controller.onTouchMove(115f, 210f)

    val bursts = mutableListOf<com.example.gl.TouchGridBurst>()
    controller.drainTouchBursts(bursts)
    assertEquals(1, bursts.size)
    assertEquals(5, bursts.first().blockCount)

    // Dragging across >= 52px spawns a crisp new trail burst
    controller.onTouchMove(170f, 200f)
    bursts.clear()
    controller.drainTouchBursts(bursts)
    assertEquals(1, bursts.size)
    assertEquals(3, bursts.first().blockCount)

    controller.onTouchUp()
    assertTrue(!controller.touchActive)

    // Verify RefreshRateMode storage round-trip
    assertEquals(
      com.example.data.local.RefreshRateMode.PEAK,
      com.example.data.local.RefreshRateMode.fromStorageValue("peak"),
    )
    assertEquals(
      com.example.data.local.RefreshRateMode.BATTERY_SAVER,
      com.example.data.local.RefreshRateMode.fromStorageValue("battery_saver"),
    )
    assertEquals(
      com.example.data.local.RefreshRateMode.DEFAULT,
      com.example.data.local.RefreshRateMode.fromStorageValue(null),
    )
  }

  private fun buildTestForecast(
    tempC: Double = 21.0,
    apparentTempC: Double = 21.0,
    dewPointC: Double = 11.0,
    humidity: Int = 50,
    cloudCover: Int = 15,
    windSpeedKmh: Double = 10.0,
    precipMm: Double = 0.0,
    uvIndex: Double = 3.0,
    isDay: Boolean = true,
    condition: WeatherConditionType = WeatherConditionType.CLEAR_DAY,
    usAqi: Int? = 25,
    hourIso: String = "2026-11-05T14:00",
    upcomingPrecipProb: Int = 0,
    daily: List<DailyPoint> = emptyList(),
  ): WeatherForecast =
    WeatherForecast(
      locationId = "station_test",
      cityName = "TestGrid",
      regionCountry = "Testland",
      latitude = 37.0,
      longitude = -122.0,
      timezone = "UTC",
      currentTempC = tempC,
      apparentTempC = apparentTempC,
      humidityPercent = humidity,
      dewPointC = dewPointC,
      pressureHpa = 1015.0,
      cloudCoverPercent = cloudCover,
      windSpeedKmh = windSpeedKmh,
      windDirectionDeg = 270,
      windGustsKmh = windSpeedKmh + 6.0,
      precipitationMm = precipMm,
      uvIndex = uvIndex,
      visibilityMeters = 15000.0,
      isDay = isDay,
      condition = condition,
      conditionLabel = condition.displayTitle,
      hourly =
        listOf(
          HourlyPoint(
            timeIso = hourIso,
            hourLabel = "NOW",
            tempC = tempC,
            precipProbPercent = upcomingPrecipProb,
            precipMm = precipMm,
            windSpeedKmh = windSpeedKmh,
            uvIndex = uvIndex,
            condition = condition,
            isDay = isDay,
          )
        ),
      daily = daily,
      fetchedAtMillis = 1762358400000L,
      isFromOfflineCache = false,
      usAqi = usAqi,
      pm25 = usAqi?.let { it * 0.4 },
    )

  @Test
  fun weatherQuipEngine_matchesAllContextualWeatherScenariosAndAirQuality() {
    // 1. Heavy pollution
    val polluted = buildTestForecast(usAqi = 165)
    assertEquals(
      "*coughs intensely* help me...",
      WeatherQuipEngine.selectActiveQuip(polluted),
    )

    // 2. Hot & humid nuclear fusion (high temp + high dew point)
    val nuclearHeat =
      buildTestForecast(
        tempC = 34.0,
        apparentTempC = 39.0,
        dewPointC = 24.5,
        humidity = 76,
      )
    val heatPool = WeatherQuipEngine.buildQuipPool(nuclearHeat)
    assertTrue("Basically a nuclear fusion outside. stay in." in heatPool)
    assertTrue("Dont go out barefeet today." in heatPool)
    assertTrue("sweating buckets huh?" in heatPool)
    assertTrue("Sleep with AC on." in heatPool)
    assertTrue("Cooked." in heatPool)

    // 2b. Cool & humid (14C with 86% RH): must NOT say "sweating buckets huh?" or warm AC!
    val coolHumid =
      buildTestForecast(
        tempC = 14.0,
        apparentTempC = 13.0,
        dewPointC = 11.7,
        humidity = 86,
        condition = WeatherConditionType.PARTLY_CLOUDY,
      )
    val coolHumidPool = WeatherQuipEngine.buildQuipPool(coolHumid)
    assertTrue("sweating buckets huh?" !in coolHumidPool)
    assertTrue("Sleep with AC on." !in coolHumidPool)
    assertTrue("Basically a nuclear fusion outside. stay in." !in coolHumidPool)
    assertTrue(coolHumidPool.any { "Chilly and damp at 14°" in it })

    // 2c. Wide daily temperature range location tendency (7C low -> 23C high = 16C swing)
    val wideSwingForecast =
      buildTestForecast(
        tempC = 10.0,
        apparentTempC = 9.5,
        dewPointC = 5.0,
        humidity = 55,
        daily =
          listOf(
            DailyPoint(
              dateIso = "2026-11-05",
              dayLabel = "TODAY",
              dateShort = "NOV 05",
              tempMinC = 7.0,
              tempMaxC = 23.0,
              apparentMinC = 6.0,
              apparentMaxC = 23.0,
              precipProbMaxPercent = 0,
              precipSumMm = 0.0,
              windMaxKmh = 14.0,
              uvMax = 5.0,
              sunriseIso = "2026-11-05T06:40",
              sunsetIso = "2026-11-05T17:10",
              condition = WeatherConditionType.CLEAR_DAY,
            )
          ),
      )
    val swingPool = WeatherQuipEngine.buildQuipPool(wideSwingForecast)
    assertTrue(swingPool.any { "16° swing" in it })

    // 3. High UV skin burn alert
    val highUv = buildTestForecast(uvIndex = 8.5, isDay = true)
    assertTrue("skin burn alert!" in WeatherQuipEngine.buildQuipPool(highUv))

    // 4. Cloudy / overcast / gloomy
    val overcast =
      buildTestForecast(cloudCover = 88, condition = WeatherConditionType.OVERCAST)
    val overcastPool = WeatherQuipEngine.buildQuipPool(overcast)
    assertTrue("Bro, see the stars today. ps. You can't." in overcastPool)
    assertTrue("Bit sad today." in overcastPool)

    // 5. Sunny / Rainy showers
    val rainy =
      buildTestForecast(
        condition = WeatherConditionType.RAIN,
        precipMm = 2.4,
        upcomingPrecipProb = 80,
      )
    assertTrue("carry umbrellas, you are welcome" in WeatherQuipEngine.buildQuipPool(rainy))

    // 6. Breezy & comfortable stroll
    val breezy = buildTestForecast(tempC = 21.0, humidity = 52, windSpeedKmh = 20.0)
    val breezyPool = WeatherQuipEngine.buildQuipPool(breezy)
    assertTrue("bro take a stroll, breeze is goated." in breezyPool)
    assertTrue("pleasantest weather." in breezyPool)
    assertTrue("Earth, as intended" in breezyPool)

    // 7. Verify new user requested nudges
    assertTrue("Honey, where is my mask?" in WeatherQuipEngine.buildQuipPool(polluted))
    assertTrue("Lungs are gonna complain today." in WeatherQuipEngine.buildQuipPool(polluted))
    assertTrue("use high quality >SPF 50 sunscreen, dont save money there!" in WeatherQuipEngine.buildQuipPool(highUv))
    assertTrue("A cup of tea wont hurt nobody." in WeatherQuipEngine.buildQuipPool(coolHumid))
    assertTrue("wrapped up in blankies." in WeatherQuipEngine.buildQuipPool(coolHumid))
    assertTrue("brrrrr, so coldddd." in WeatherQuipEngine.buildQuipPool(buildTestForecast(tempC = 5.0)))
    assertTrue("I crave icecream" in WeatherQuipEngine.buildQuipPool(buildTestForecast(tempC = 26.0)))

    // 7. Clear sky, low pollution night
    val starryNight =
      buildTestForecast(
        isDay = false,
        cloudCover = 5,
        usAqi = 18,
        condition = WeatherConditionType.CLEAR_NIGHT,
      )
    assertEquals(
      "Look up, the stars are pretty",
      WeatherQuipEngine.selectActiveQuip(starryNight),
    )

    // 8. Random Hello :) and live online fun fact present in pool & reachable via cycling
    val poolWithFact =
      WeatherQuipEngine.buildQuipPool(
        forecast = breezy,
        onlineFunFact = "Petrichor comes from plant oils released during rainfall.",
      )
    assertTrue("Hello :)" in poolWithFact)
    assertTrue(
      "fun fact: Petrichor comes from plant oils released during rainfall." in poolWithFact
    )

    // Verify OpenMeteoAirQualityResponse JSON parsing
    val aqJson =
      """
      {
        "latitude": 28.6,
        "longitude": 77.2,
        "current": {
          "time": "2026-11-05T14:00",
          "us_aqi": 154,
          "pm2_5": 61.3,
          "pm10": 112.0
        }
      }
      """
        .trimIndent()
    val aqParsed =
      NetworkClient.moshi.adapter(OpenMeteoAirQualityResponse::class.java).fromJson(aqJson)
    assertNotNull(aqParsed)
    assertEquals(154, aqParsed!!.current!!.usAqi)

    // 9. New smart contextual nudges
    val thunderForecast = buildTestForecast(condition = WeatherConditionType.THUNDERSTORM, precipMm = 8.0)
    val thunderPool = WeatherQuipEngine.buildQuipPool(thunderForecast)
    assertTrue("sky is taking flash photography without permission." in thunderPool)
    assertTrue("thor is aggressively practicing drums overhead." in thunderPool)

    val foggyForecast = buildTestForecast(condition = WeatherConditionType.FOG, tempC = 15.0)
    assertTrue(
      "render distance set to 2 chunks." in WeatherQuipEngine.buildQuipPool(foggyForecast)
    )
    assertTrue(
      "sunscreen now or tomato cosplay later, your call." in WeatherQuipEngine.buildQuipPool(highUv)
    )
    assertTrue(
      "drink water before you turn into a raisin bro." in WeatherQuipEngine.buildQuipPool(nuclearHeat)
    )

    val windyForecast = buildTestForecast(windSpeedKmh = 40.0)
    assertTrue(
      "umbrella flip hazard: 100%. dont even try it." in WeatherQuipEngine.buildQuipPool(windyForecast)
    )
  }

  @Test
  fun discreteSliderSnapping_snapsStrictlyToNearestStep() {
    assertEquals(0, snapSliderOffsetToStep(offsetX = -20f, totalWidthPx = 420f, thumbSizePx = 20f, totalSteps = 5))
    assertEquals(0, snapSliderOffsetToStep(offsetX = 40f, totalWidthPx = 420f, thumbSizePx = 20f, totalSteps = 5))
    assertEquals(1, snapSliderOffsetToStep(offsetX = 110f, totalWidthPx = 420f, thumbSizePx = 20f, totalSteps = 5))
    assertEquals(2, snapSliderOffsetToStep(offsetX = 210f, totalWidthPx = 420f, thumbSizePx = 20f, totalSteps = 5))
    assertEquals(3, snapSliderOffsetToStep(offsetX = 310f, totalWidthPx = 420f, thumbSizePx = 20f, totalSteps = 5))
    assertEquals(4, snapSliderOffsetToStep(offsetX = 405f, totalWidthPx = 420f, thumbSizePx = 20f, totalSteps = 5))
    assertEquals(4, snapSliderOffsetToStep(offsetX = 500f, totalWidthPx = 420f, thumbSizePx = 20f, totalSteps = 5))
  }

  @Test
  fun atmosphericCardOrderNormalization_preservesCustomOrderAndDeduplicates() {
    val custom = listOf("metric_solar", "metric_aqi", "metric_solar", "unknown_tag")
    val normalized = normalizeAtmosphericCardOrder(custom)
    assertEquals(DEFAULT_ATMOSPHERIC_CARD_ORDER.size, normalized.size)
    assertEquals("metric_solar", normalized[0])
    assertEquals("metric_aqi", normalized[1])
    assertTrue(DEFAULT_ATMOSPHERIC_CARD_ORDER.all { it in normalized })

    // Verify neighboring cards shift smoothly to make space when dragging forward or backward
    val movedForward =
      reorderAtmosphericCardsOnHover(
        currentTags = DEFAULT_ATMOSPHERIC_CARD_ORDER,
        movingTag = "metric_real_feel",
        targetIndex = 3,
      )
    assertEquals(
      listOf(
        "metric_aqi",
        "metric_wind",
        "metric_humidity",
        "metric_real_feel",
        "metric_uv",
        "metric_pressure",
        "metric_precipitation",
        "metric_solar",
        "metric_visibility",
        "metric_moon_phase",
      ),
      movedForward,
    )

    val movedBackward =
      reorderAtmosphericCardsOnHover(
        currentTags = movedForward,
        movingTag = "metric_uv",
        targetIndex = 1,
      )
    assertEquals(
      listOf(
        "metric_aqi",
        "metric_uv",
        "metric_wind",
        "metric_humidity",
        "metric_real_feel",
        "metric_pressure",
        "metric_precipitation",
        "metric_solar",
        "metric_visibility",
        "metric_moon_phase",
      ),
      movedBackward,
    )
  }

  @Test
  fun dynamicThemeContrast_enforcesAccessibleContrastOnTrickyPastelAndDarkAccents() {
    val paleLightScheme =
      lightColorScheme(
        primary = Color(0xFFD9E650),
        secondary = Color(0xFFC5D86D),
        tertiary = Color(0xFFB5C7FF),
        background = Color(0xFFF6F6F2),
        surface = Color(0xFFEEEEEA),
        onSurface = Color(0xFF1A1B20),
        onSurfaceVariant = Color(0xFF7A7D86),
        outline = Color(0xFF5D6068),
      )

    val fixedLight = enforceDynamicTokenContrast(scheme = paleLightScheme, darkTheme = false)
    val softFillLight = fixedLight.accentSoftFill.compositeOver(fixedLight.surfaceTranslucent)

    assertTrue(wcagContrastRatio(fixedLight.accentPrimary, fixedLight.background) >= 4.7f)
    assertTrue(wcagContrastRatio(fixedLight.accentPrimary, fixedLight.surfaceElevated) >= 4.7f)
    assertTrue(wcagContrastRatio(fixedLight.accentPrimary, softFillLight) >= 4.7f)
    assertTrue(wcagContrastRatio(fixedLight.inkPrimary, softFillLight) >= 4.5f)
    assertTrue(wcagContrastRatio(fixedLight.inkMuted, fixedLight.surfaceElevated) >= 4.5f)
    assertTrue(wcagContrastRatio(fixedLight.heroBlockInk, fixedLight.heroBlockFill) >= 5.0f)

    // Verify status text ink helper also preserves light-mode contrast
    val darkenedLightStatus = accessibleStatusTextInk(Color(0xFF6CD68E), isDark = false)
    assertTrue(wcagContrastRatio(darkenedLightStatus, fixedLight.background) >= 4.5f)

    val murkyDarkScheme =
      darkColorScheme(
        primary = Color(0xFF1F2B6C),
        secondary = Color(0xFF28357A),
        tertiary = Color(0xFF2B2250),
        background = Color(0xFF000000),
        surface = Color(0xFF0C0F14),
        onSurface = Color(0xFFE0E2EC),
        onSurfaceVariant = Color(0xFF4B505E),
        outline = Color(0xFF6E7585),
      )

    val fixedDark = enforceDynamicTokenContrast(scheme = murkyDarkScheme, darkTheme = true)
    val softFillDark = fixedDark.accentSoftFill.compositeOver(fixedDark.surfaceTranslucent)

    assertTrue(wcagContrastRatio(fixedDark.accentPrimary, Color(0xFF000000)) >= 4.7f)
    assertTrue(wcagContrastRatio(fixedDark.accentPrimary, fixedDark.surfaceElevated) >= 4.7f)
    assertTrue(wcagContrastRatio(fixedDark.accentPrimary, softFillDark) >= 4.7f)
    assertTrue(wcagContrastRatio(fixedDark.inkPrimary, softFillDark) >= 4.5f)
    assertTrue(wcagContrastRatio(fixedDark.inkMuted, fixedDark.surfaceElevated) >= 4.5f)
    assertTrue(wcagContrastRatio(fixedDark.heroBlockInk, fixedDark.heroBlockFill) >= 5.0f)
  }

  @Test
  fun pixelGridController_postSyncWaveIncrementsCounter() {
    val controller = PixelGridController()
    assertEquals(0, controller.getPostSyncWaveCount())
    controller.triggerPostSyncWave()
    assertEquals(1, controller.getPostSyncWaveCount())
  }

  @Test
  fun pixelGridController_reduceAnimationsFlag_isUpdatedAndRetained() {
    val controller = PixelGridController()
    assertTrue(!controller.isReduceAnimations())

    controller.updateWeatherState(
      condition = WeatherConditionType.CLEAR_DAY,
      isDarkMode = false,
      ecoMode = false,
      reduceAnimations = true,
    )
    assertTrue(controller.isReduceAnimations())
  }

  @Test
  fun appLanguageLocalization_resolvesAutoDeviceAndSupportedLocales() {
    assertEquals(com.example.util.AppLanguage.SYSTEM, com.example.util.AppLanguage.fromCode("SYSTEM"))
    assertEquals(com.example.util.AppLanguage.SPANISH, com.example.util.AppLanguage.fromCode("ES"))
    assertEquals(com.example.util.AppLanguage.JAPANESE, com.example.util.AppLanguage.fromCode("JA"))

    assertEquals(
      "MATRIZ ATMOSFÉRICA",
      com.example.util.LocalizedStrings.sectionAtmosphericMatrix(com.example.util.AppLanguage.SPANISH),
    )
    assertEquals(
      "SENSACIÓN REAL",
      com.example.util.LocalizedStrings.metricRealFeel(com.example.util.AppLanguage.SPANISH),
    )
    assertEquals(
      "雨",
      com.example.util.LocalizedStrings.conditionLabel("RAIN", com.example.util.AppLanguage.JAPANESE),
    )
  }

  @Test
  fun emergencySafetyChecklist_generatesActionableTipsPerHazard() {
    val galeAlert =
      com.example.domain.model.SevereWeatherAlert(
        id = "alert_wind",
        title = "GALE WIND WARNING",
        description = "High wind gusts reaching 75 km/h",
        category = "WIND",
        metricTag = "metric_wind",
        severity = com.example.domain.model.AlertSeverity.WARNING,
      )
    val tips = com.example.ui.components.getEmergencySafetyChecklist(galeAlert)
    assertEquals(3, tips.size)
    assertTrue(tips.any { "outdoor items" in it.lowercase() })

    val uvAlert =
      com.example.domain.model.SevereWeatherAlert(
        id = "alert_uv",
        title = "EXTREME UV RADIATION",
        description = "UV index 11+",
        category = "UV",
        metricTag = "metric_uv",
        severity = com.example.domain.model.AlertSeverity.CRITICAL,
      )
    val uvTips = com.example.ui.components.getEmergencySafetyChecklist(uvAlert)
    assertTrue(uvTips.any { "sunscreen" in it.lowercase() })
  }
}
