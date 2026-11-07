package com.example.domain.model

import com.example.util.AppLanguage
import com.example.util.LocalizedStrings
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Location-adaptive & multi-factor context engine for hero weather quips and nudges.
 * Selects hyper-relevant meteorological lines based on real-time city conditions,
 * seeded by location and time to avoid repetitive loops.
 */
object WeatherQuipEngine {

  private val fallbackMeteorologyFacts = listOf(
    "A single cumulonimbus cloud holds over 500,000 tons of water droplets.",
    "Petrichor is caused by raindrops trapping air bubbles that aerosolize soil oils.",
    "Lightning heats surrounding air to 30,000 K—five times hotter than the sun's surface.",
    "Every snowflake's 6-fold symmetry reflects the hexagonal crystal lattice of water ice.",
    "Rayleigh scattering scatters shorter blue wavelengths in nitrogen, creating blue skies.",
    "Barometric pressure drops before storms as rising moist air leaves lower mass near ground.",
    "Cloud iridescence occurs when uniform droplets diffract light into pastel bands.",
    "Warm foehn winds heat rapidly descending the leeward side of mountains.",
    "The longest recorded rainbow lasted 9 hours over Sheffield, England.",
    "Sahara dust travels across the Atlantic to fertilize the Amazon rainforest."
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
   * weather lines plus online meteorology fun facts.
   */
  fun buildQuipPool(
    forecast: WeatherForecast,
    selectedHourIndex: Int? = null,
    onlineFunFact: String? = null,
    seedOffset: Int = 0,
    lang: AppLanguage = AppLanguage.ENGLISH
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

    val baseHourIdx = selectedHourIndex ?: 0
    val upcomingPrecipPeak =
      forecast.hourly.drop(baseHourIdx).take(6).maxOfOrNull { it.precipProbPercent } ?: 0

    val isRainingNow =
      condition == WeatherConditionType.RAIN ||
        condition == WeatherConditionType.HEAVY_RAIN ||
        condition == WeatherConditionType.DRIZZLE ||
        condition == WeatherConditionType.THUNDERSTORM ||
        precipMm > 0.1

    // Group 1: Priority Acute / Weather-Triggered (Specific to this city right now)
    val prioritySpecific = mutableListOf<String>()

    if ((usAqi != null && usAqi >= 90) || (pm25 != null && pm25 >= 30.0)) {
      prioritySpecific.add(LocalizedStrings.quipCough(lang))
      prioritySpecific.add("Air quality: extra crunchy in ${forecast.cityName} today.")
      prioritySpecific.add("Mask up—lungs are gonna complain today.")
    }

    if ((effectiveApparentC >= 35.0 && (tempC >= 31.0 || dewPointC >= 22.0)) || tempC >= 38.0) {
      prioritySpecific.add(LocalizedStrings.quipNuclearFusion(lang))
      prioritySpecific.add("The sun is on a personal vengeance arc today.")
      prioritySpecific.add("Drink water before you turn into a raisin.")
    } else if (tempC >= 25.0 || (isDay && condition == WeatherConditionType.CLEAR_DAY)) {
      prioritySpecific.add(LocalizedStrings.quipIceCream(lang))
      if (isDay && tempC >= 28.5) {
        prioritySpecific.add("Car seatbelt buckle temperature: critical hazard.")
        prioritySpecific.add("Stepped outside for 2 seconds, character development complete.")
      }
    }

    if (isDay && uvIndex >= 5.0) {
      prioritySpecific.add("Use high quality >SPF 50 sunscreen—don't cut corners there!")
      prioritySpecific.add("Sunscreen now or tomato cosplay later, your call.")
    }

    if (isRainingNow || upcomingPrecipPeak >= 45) {
      prioritySpecific.add(LocalizedStrings.quipUmbrella(lang))
      when (condition) {
        WeatherConditionType.THUNDERSTORM -> {
          prioritySpecific.add("*Thunder Crackles* Thor is practicing drums overhead.")
          prioritySpecific.add("Sky is taking flash photography without permission.")
        }
        WeatherConditionType.HEAVY_RAIN -> {
          prioritySpecific.add("Raining cats and dogs—socks are in grave danger.")
          prioritySpecific.add("Sky forgot to turn the tap off in ${forecast.cityName}.")
        }
        else -> {
          prioritySpecific.add("Free car wash outside rn. Grab an umbrella.")
          prioritySpecific.add("Check the weather before wearing white sneakers!")
        }
      }
    }

    if (tempC <= 16.0 || isRainingNow || humidity >= 70) {
      prioritySpecific.add(LocalizedStrings.quipTea(lang))
    }

    if (tempC <= 10.0 && !isRainingNow) {
      prioritySpecific.add("Brrrrr, so cold! Certified hoodie weather.")
      if (tempC <= 2.0) {
        prioritySpecific.add("Why do we live where the air hurts our face?")
        prioritySpecific.add("Leaving bed today requires heroic bravery.")
      }
    }

    if (windKmh >= 24.0 || forecast.windGustsKmh >= 38.0) {
      prioritySpecific.add("Walking against the wind = surprise leg day.")
      prioritySpecific.add("Nature's free blow-dry. Messy hair unlocked.")
      if (forecast.windGustsKmh >= 48.0) {
        prioritySpecific.add("Umbrella flip hazard: 100%. Don't even try it.")
      }
    }

    if (todaySwingC >= 11.0 || locationTypicalSwingC >= 11.0) {
      prioritySpecific.add("Classic ${forecast.cityName} swing: ${todayMinC.roundToInt()}° to ${todayMaxC.roundToInt()}°. 3 seasons in one day.")
    }

    if (!isDay && cloudCover <= 30 && visibilityM >= 8000.0 && (usAqi == null || usAqi <= 60)) {
      prioritySpecific.add("Look up, the stars are pretty tonight.")
      prioritySpecific.add("The moon is carrying the entire sky aesthetic.")
    }

    if (tempC in 18.0..25.0 && effectiveApparentC in 17.5..25.5 && precipMm == 0.0 && windKmh <= 24.0) {
      prioritySpecific.add("Earth, as intended. Pleasantest weather.")
      prioritySpecific.add("No jacket, no sweat, no complaints. Rare W for ${forecast.cityName}.")
      prioritySpecific.add("Weather so good it feels suspicious.")
    }

    // Group 2: Meteorological Fun Facts
    val factIdx = abs(forecast.cityName.hashCode() + tempC.roundToInt() + seedOffset) % fallbackMeteorologyFacts.size
    val chosenFact = onlineFunFact?.takeIf { it.isNotBlank() } ?: fallbackMeteorologyFacts[factIdx]
    val funFactLine = "Fun fact: $chosenFact"

    // Group 3: Humorous and quirky remarks that smartly reference the weather
    val generalQuirks = listOf(
      "Why are you reading this? Go touch grass.",
      "Did you know that clouds are just really lazy fog?",
      "Weather forecast: 100% chance of things happening.",
      "Are you opening this app just to read what I wrote? Awww <3",
      LocalizedStrings.quipHello(lang),
      "Manifesting a clear sky for you. Results may vary.",
      "Current mood: mostly cloudy with a chance of snacks.",
      "Barometer: checking pressure. Me: also under pressure.",
      "If you stand still enough in this weather, you become street architecture."
    )

    // Blend smartly:
    // If we have acute conditions, rotate them by city seed so different cities have a distinct
    // starting acute observation, but ALWAYS starting with acute conditions!
    val result = LinkedHashSet<String>()
    val citySeed = abs(forecast.cityName.hashCode() + (todayMinC * 10).roundToInt() + (tempC * 5).roundToInt())

    if (prioritySpecific.isNotEmpty()) {
      val rotateOffset = citySeed % prioritySpecific.size
      val rotatedSpecific = prioritySpecific.drop(rotateOffset) + prioritySpecific.take(rotateOffset)
      
      // First 2: Genuine acute condition matches for this specific location
      rotatedSpecific.take(2).forEach { result.add(it) }
      // Then fun fact
      result.add(funFactLine)
      // Then remaining acute conditions
      rotatedSpecific.drop(2).forEach { result.add(it) }
      // Then quirky remarks
      val quirkRotate = (citySeed / 3) % generalQuirks.size
      val rotatedQuirks = generalQuirks.drop(quirkRotate) + generalQuirks.take(quirkRotate)
      rotatedQuirks.forEach { result.add(it) }
    } else {
      // Mild baseline weather: Lead with pleasant/mild quips, then fun fact, then quirks
      result.add(LocalizedStrings.quipHello(lang))
      result.add("Earth, as intended. Pleasantest weather.")
      result.add(funFactLine)
      val quirkRotate = citySeed % generalQuirks.size
      val rotatedQuirks = generalQuirks.drop(quirkRotate) + generalQuirks.take(quirkRotate)
      rotatedQuirks.forEach { result.add(it) }
    }

    return result.toList()
  }

  /**
   * Selects the active hero weather line for the current forecast, preview hour, and cycle offset.
   * At quipCycleOffset = 0, every station displays its #1 acute meteorological observation.
   * Tapping cycles sequentially through the rest of the station's tailored pool.
   */
  fun selectActiveQuip(
    forecast: WeatherForecast,
    selectedHourIndex: Int? = null,
    onlineFunFact: String? = null,
    quipCycleOffset: Int = 0,
    lang: AppLanguage = AppLanguage.ENGLISH
  ): String {
    val pool = buildQuipPool(forecast, selectedHourIndex, onlineFunFact, quipCycleOffset, lang)
    if (pool.isEmpty()) return LocalizedStrings.quipHello(lang)
    
    val index = ((quipCycleOffset % pool.size) + pool.size) % pool.size
    return pool[index]
  }
}
