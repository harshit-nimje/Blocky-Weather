package com.example.domain.model

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Location-adaptive & multi-factor context engine for hero weather quips and nudges.
 * Combines psychrometric dew point / apparent heat stress (never misfiring on cool-humid air),
 * each city's 7-day baseline & daily temperature swing tendency, wind/visibility/UV/AQI telemetry,
 * and live online meteorology fun facts.
 */
object WeatherQuipEngine {

  private val fallbackMeteorologyFacts =
    listOf(
      "A single cumulonimbus thunderstorm cloud can hold over 500,000 tons of water droplets.",
      "Petrichor is the earthy scent caused when raindrops trap tiny air bubbles that aerosolize soil oils.",
      "Lightning bolts heat surrounding air to roughly 30,000 K—about five times hotter than the sun's surface.",
      "Every snowflake's six-fold symmetry comes directly from the hexagonal crystal lattice of water ice.",
      "Sunlight scatters shorter blue wavelengths more strongly in nitrogen and oxygen—the Rayleigh scattering behind blue skies.",
      "Air pressure drops before storms because warm, moist air rises and leaves lower mass near the surface.",
      "Cloud iridescence occurs when uniform water droplets diffract sunlight into pastel bands along cloud edges.",
      "Warm foehn winds form when moist air cools rising up a mountain, sheds rain, and warms rapidly descending the leeward side.",
    )

  /**
   * Computes Australian BoM apparent temperature (°C) incorporating temperature, vapor pressure, and wind speed.
   */
  internal fun computeApparentTempC(
    tempC: Double,
    humidityPercent: Int,
    windSpeedKmh: Double,
  ): Double {
    val rh = humidityPercent.coerceIn(5, 100)
    val vaporPressureHpa =
      (rh / 100.0) * 6.105 * exp((17.27 * tempC) / (237.7 + tempC))
    val windMs = (windSpeedKmh / 3.6).coerceAtLeast(0.0)
    return tempC + 0.33 * vaporPressureHpa - 0.70 * windMs - 4.00
  }

  /**
   * Estimates dew point (°C) via Magnus formula.
   */
  internal fun estimateDewPointC(tempC: Double, humidityPercent: Int): Double {
    val rh = humidityPercent.coerceIn(5, 100) / 100.0
    val gamma = ((17.27 * tempC) / (237.7 + tempC)) + kotlin.math.ln(rh)
    return (237.7 * gamma) / (17.27 - gamma)
  }

  /**
   * Returns an ordered, deduplicated list of smart contextual, location-adaptive, and relatable
   * weather lines plus online meteorology fun facts and random greetings.
   */
  fun buildQuipPool(
    forecast: WeatherForecast,
    selectedHourIndex: Int? = null,
    onlineFunFact: String? = null,
    seedOffset: Int = 0,
  ): List<String> {
    val selectedHour = selectedHourIndex?.let { forecast.hourly.getOrNull(it) }
    val condition = selectedHour?.condition ?: forecast.condition
    val tempC = selectedHour?.tempC ?: forecast.currentTempC
    val humidity = forecast.humidityPercent
    val uvIndex = selectedHour?.uvIndex ?: forecast.uvIndex
    val windKmh = selectedHour?.windSpeedKmh ?: forecast.windSpeedKmh
    val precipMm = selectedHour?.precipMm ?: forecast.precipitationMm
    val isDay = selectedHour?.isDay ?: forecast.isDay
    val cloudCover = forecast.cloudCoverPercent
    val visibilityM = forecast.visibilityMeters
    val usAqi = forecast.usAqi
    val pm25 = forecast.pm25

    val dewPointC =
      if (selectedHour != null) estimateDewPointC(tempC, humidity)
      else forecast.dewPointC

    val effectiveApparentC =
      if (selectedHour != null) {
        computeApparentTempC(tempC, humidity, windKmh)
      } else {
        // Blend server apparent temperature with psychrometric apparent temperature for safety
        maxOf(forecast.apparentTempC, computeApparentTempC(tempC, humidity, windKmh))
      }

    // Location multi-day tendency & diurnal swing analytics
    val todayDaily = forecast.daily.firstOrNull()
    val todayMinC = todayDaily?.tempMinC ?: tempC
    val todayMaxC = todayDaily?.tempMaxC ?: tempC
    val todaySwingC = (todayMaxC - todayMinC).coerceAtLeast(0.0)

    val locationTypicalSwingC =
      if (forecast.daily.isNotEmpty()) {
        forecast.daily.map { (it.tempMaxC - it.tempMinC).coerceAtLeast(0.0) }.average()
      } else {
        todaySwingC
      }

    val weekMeanTempC =
      if (forecast.daily.isNotEmpty()) {
        forecast.daily.map { (it.tempMaxC + it.tempMinC) * 0.5 }.average()
      } else {
        tempC
      }
    val tempAnomalyC = tempC - weekMeanTempC
    val tempPositionInDay =
      ((tempC - todayMinC) / todaySwingC.coerceAtLeast(1.0)).coerceIn(0.0, 1.0)

    val baseHourIdx = selectedHourIndex ?: 0
    val upcomingPrecipPeak =
      forecast.hourly.drop(baseHourIdx).take(6).maxOfOrNull { it.precipProbPercent } ?: 0
    val next6hTempDeltaC =
      (forecast.hourly.getOrNull(baseHourIdx + 6)?.tempC ?: tempC) - tempC

    val activeTimeIso = selectedHour?.timeIso ?: forecast.hourly.firstOrNull()?.timeIso
    val localHour = activeTimeIso?.substringAfter('T', "")?.take(2)?.toIntOrNull()
    val isAfternoon = localHour != null && localHour in 12..18
    val isNightOrLateEvening = !isDay || (localHour != null && (localHour >= 20 || localHour <= 5))

    val contextual = LinkedHashSet<String>()

    // 1. Acute / High-Signal Health & Weather Alerts
    if ((usAqi != null && usAqi >= 90) || (pm25 != null && pm25 >= 30.0)) {
      contextual.add("*coughs intensely* help me...")
      contextual.add("Honey, where is my mask?")
      contextual.add("Lungs are gonna complain today.")
    }
    if ((usAqi != null && usAqi >= 70) || (humidity >= 70 && tempC >= 22.0)) {
      contextual.add("Bit stuffy here.")
    }
    if ((effectiveApparentC >= 36.0 && (tempC >= 31.0 || dewPointC >= 22.0)) || tempC >= 38.0) {
      contextual.add("Basically a nuclear fusion outside. stay in.")
    }
    if (isDay && uvIndex >= 5.0) {
      contextual.add("use high quality >SPF 50 sunscreen, dont save money there!")
      if (uvIndex >= 6.0 && cloudCover <= 65) {
        contextual.add("skin burn alert!")
      }
    }

    // 2. Clear Starry Night & Clean Air
    if (!isDay && cloudCover <= 30 && visibilityM >= 8000.0 && (usAqi == null || usAqi <= 60)) {
      contextual.add("Look up, the stars are pretty")
    }
    if ((usAqi == null || usAqi <= 45) && cloudCover <= 40) {
      contextual.add("Breathe in. Breathe out")
    }

    // 3. Hot / Sunny / Ice Cream / High Dew-Point Muggy / AC
    if (tempC >= 24.0 || (isDay && condition == WeatherConditionType.CLEAR_DAY)) {
      contextual.add("I crave icecream")
    }
    if (isDay && tempC >= 28.5 && cloudCover <= 50 && precipMm == 0.0) {
      contextual.add("Dont go out barefeet today.")
    }
    if (tempC >= 29.5 || effectiveApparentC >= 33.5) {
      contextual.add("Cooked.")
    }
    // Foolproof psychrometric sweat check: high humidity at 14°C is cool mist, NOT sweating buckets
    if (tempC >= 25.5 && (dewPointC >= 19.0 || (humidity >= 68 && effectiveApparentC >= 28.0))) {
      contextual.add("sweating buckets huh?")
    }
    if (
      (isAfternoon && tempC >= 26.5 && (dewPointC >= 17.5 || humidity >= 58)) ||
        (isNightOrLateEvening && tempC >= 23.0 && (dewPointC >= 17.0 || humidity >= 60)) ||
        (tempC >= 28.0 && humidity >= 55) ||
        effectiveApparentC >= 30.0
    ) {
      contextual.add("Sleep with AC on.")
    }

    val isRainingNow =
      condition == WeatherConditionType.RAIN ||
        condition == WeatherConditionType.HEAVY_RAIN ||
        condition == WeatherConditionType.DRIZZLE ||
        condition == WeatherConditionType.THUNDERSTORM ||
        precipMm > 0.1

    // 4. Cool / Mild Damp Air & Cosy Tea Weather
    if (tempC in 8.0..19.5 && humidity >= 78 && precipMm <= 0.1) {
      if (condition == WeatherConditionType.FOG || visibilityM <= 4000.0) {
        contextual.add("Can barely see two blocks ahead. Fog claimed the whole grid.")
      } else {
        contextual.add("Chilly and damp at ${tempC.roundToInt()}°—hoodie air, zero sweat.")
      }
    }
    if (tempC <= 16.0 || isRainingNow || humidity >= 70) {
      contextual.add("A cup of tea wont hurt nobody.")
      contextual.add("wrapped up in blankies.")
    }

    // 5. Rainy / Stormy / Heavy Rain / Thunderstorm
    if (isRainingNow || upcomingPrecipPeak >= 45) {
      contextual.add("carry umbrellas, you are welcome")
      when (condition) {
        WeatherConditionType.HEAVY_RAIN -> {
          contextual.add("Better not get splashed today. Its raining... Cats and dogs?")
          contextual.add("rain gonna fill up to your ankles today.")
          contextual.add("Harvest some rain water dude.")
        }
        WeatherConditionType.THUNDERSTORM -> {
          contextual.add("*Thunder Crackles*")
          contextual.add("Electric sky out there. Let the pixel thunder roll from indoors.")
        }
        WeatherConditionType.DRIZZLE ->
          contextual.add("Light drizzle. Just wet enough to ruin clean sneakers.")
        else -> {
          contextual.add("Better not get splashed today. Grab an umbrella.")
          if (precipMm >= 2.0 || upcomingPrecipPeak >= 70) {
            contextual.add("Harvest some rain water dude.")
          }
        }
      }
    }

    // 6. Location-Adaptive Daily Range, Diurnal Swing & Anomaly Tendencies
    if (todaySwingC >= 12.0 || locationTypicalSwingC >= 12.0) {
      if (tempPositionInDay <= 0.40 && tempC <= 16.5 && todayMaxC >= 20.5) {
        contextual.add(
          "Freezing start, warm afternoon—classic ${forecast.cityName} ${todaySwingC.roundToInt()}° swing. Layer up."
        )
      } else if (next6hTempDeltaC <= -5.5) {
        contextual.add(
          "Enjoy the warmth now—${forecast.cityName} drops ${abs(next6hTempDeltaC.roundToInt())}° fast after sundown."
        )
      } else if (todaySwingC >= 12.0) {
        contextual.add(
          "${forecast.cityName} woke up at ${todayMinC.roundToInt()}° and hits ${todayMaxC.roundToInt()}° today. Respect the range."
        )
      }
    } else if (forecast.daily.size >= 3 && locationTypicalSwingC in 0.5..5.5 && !isRainingNow) {
      contextual.add(
        "Locked around ${todayMinC.roundToInt()}°–${todayMaxC.roundToInt()}°. ${forecast.cityName} picked one temperature and committed."
      )
    }

    if (forecast.daily.size >= 3) {
      if (tempAnomalyC <= -4.5) {
        contextual.add("Plot twist: noticeably colder than ${forecast.cityName} usually runs this week.")
      } else if (tempAnomalyC >= 4.5 && tempC >= 20.0) {
        contextual.add("Unusually warm for ${forecast.cityName} today. Sky switched teams.")
      }
    }

    // 7. Cloudy / Overcast / Gloomy Skies
    if (
      condition == WeatherConditionType.OVERCAST ||
        condition == WeatherConditionType.FOG ||
        isRainingNow ||
        cloudCover >= 75
    ) {
      contextual.add("Bit sad today.")
      contextual.add("Bro, see the stars today. ps. You can't.")
    } else if (condition == WeatherConditionType.PARTLY_CLOUDY || cloudCover >= 45) {
      contextual.add("Bro, see the stars today. ps. You can't.")
    }

    // 8. Breezy & Comfortable Stroll
    if (
      tempC in 16.0..26.5 &&
        effectiveApparentC in 16.0..27.0 &&
        windKmh in 10.0..32.0 &&
        forecast.windGustsKmh < 45.0 &&
        precipMm == 0.0 &&
        (usAqi == null || usAqi <= 85)
    ) {
      contextual.add("bro take a stroll, breeze is goated.")
    }

    // 9. Ideal Goldilocks Weather
    if (
      tempC in 18.5..25.5 &&
        effectiveApparentC in 18.0..26.0 &&
        dewPointC <= 18.0 &&
        precipMm == 0.0 &&
        windKmh <= 26.0 &&
        (usAqi == null || usAqi <= 75)
    ) {
      contextual.add("pleasantest weather.")
      contextual.add("Earth, as intended")
      if (isDay && condition == WeatherConditionType.CLEAR_DAY) {
        contextual.add("Bit sunny today. Nice weather ngl.")
      } else {
        contextual.add("Nice weather ngl.")
      }
    }

    // 10. Cold Weather
    if (tempC <= 12.0 && !isRainingNow) {
      contextual.add("brrrrr, so coldddd.")
      if (tempC <= 1.0) {
        contextual.add("Biting cold out there. Breath looks like pixel smoke.")
      } else {
        contextual.add("Crisp ${tempC.roundToInt()}° hoodie weather. Keep hands in pockets.")
      }
    }

    // 11. Clear Clean-Air Secondary Line
    if (cloudCover <= 25 && visibilityM >= 8000.0 && (usAqi == null || usAqi <= 50)) {
      contextual.add("Look up, the stars are pretty")
    }

    // Ultimate fallback if no bucket triggered
    if (contextual.isEmpty()) {
      contextual.add("Earth, as intended")
      contextual.add("pleasantest weather.")
    }

    // Build real online / meteorology fun fact
    val factIdx =
      abs(forecast.cityName.hashCode() + tempC.roundToInt() + seedOffset) %
        fallbackMeteorologyFacts.size
    val chosenFact = onlineFunFact?.takeIf { it.isNotBlank() } ?: fallbackMeteorologyFacts[factIdx]
    val funFactLine = "fun fact: $chosenFact"

    // Weave contextual lines cleanly with live fun fact & friendly greeting
    val result = mutableListOf<String>()
    val ctxList = contextual.toList()
    ctxList.forEachIndexed { idx, line ->
      result.add(line)
      if (idx == 0) {
        result.add(funFactLine)
      } else if (idx == 1) {
        result.add("Are you opening this app to read what i wrote? awww <3")
      } else if (idx == 2) {
        result.add("Hello :)")
      }
    }
    if (funFactLine !in result) result.add(funFactLine)
    if ("Are you opening this app to read what i wrote? awww <3" !in result) {
      result.add("Are you opening this app to read what i wrote? awww <3")
    }
    if ("Hello :)" !in result) result.add("Hello :)")

    return result
  }

  /**
   * Selects the active hero weather line for the current forecast, preview hour, and cycle offset.
   */
  fun selectActiveQuip(
    forecast: WeatherForecast,
    selectedHourIndex: Int? = null,
    onlineFunFact: String? = null,
    quipCycleOffset: Int = 0,
  ): String {
    val pool = buildQuipPool(forecast, selectedHourIndex, onlineFunFact, quipCycleOffset)
    if (pool.isEmpty()) return "Hello :)"
    val index = ((quipCycleOffset % pool.size) + pool.size) % pool.size
    return pool[index]
  }
}
