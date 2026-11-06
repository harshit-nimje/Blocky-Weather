package com.example.util

import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
  SYSTEM("SYSTEM", "System Default", "Auto-Device Locale"),
  ENGLISH("EN", "English", "English"),
  SPANISH("ES", "Spanish", "Español"),
  FRENCH("FR", "French", "Français"),
  GERMAN("DE", "German", "Deutsch"),
  HINDI("HI", "Hindi", "हिंदी"),
  JAPANESE("JA", "Japanese", "日本語"),
  CHINESE("ZH", "Chinese", "中文 (简体)");

  companion object {
    fun fromCode(code: String?): AppLanguage =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: SYSTEM

    fun resolveEffectiveLanguage(selectedCode: String?): AppLanguage {
      val selected = fromCode(selectedCode)
      if (selected != SYSTEM) return selected
      val deviceLang = Locale.getDefault().language.lowercase()
      return when (deviceLang) {
        "es" -> SPANISH
        "fr" -> FRENCH
        "de" -> GERMAN
        "hi" -> HINDI
        "ja" -> JAPANESE
        "zh" -> CHINESE
        else -> ENGLISH
      }
    }
  }
}

object LocalizedStrings {
  fun sectionHourlyTimeline(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "CRONOGRAMA 24H DE BLOQUES"
    AppLanguage.FRENCH -> "CHRONOLOGIE 24H EN BLOCS"
    AppLanguage.GERMAN -> "24H BLOCK ZEITLEISTE"
    AppLanguage.HINDI -> "24 घंटे ब्लॉक समय रेखा"
    AppLanguage.JAPANESE -> "24時間ブロックタイムライン"
    AppLanguage.CHINESE -> "24小时方块时间轴"
    else -> "24H BLOCK TIMELINE"
  }

  fun sectionAtmosphericMatrix(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "MATRIZ ATMOSFÉRICA"
    AppLanguage.FRENCH -> "MATRICE ATMOSPHÉRIQUE"
    AppLanguage.GERMAN -> "ATMOSPHÄRISCHE MATRIX"
    AppLanguage.HINDI -> "वायुमंडलीय मैट्रिक्स"
    AppLanguage.JAPANESE -> "大気マトリックス"
    AppLanguage.CHINESE -> "大气层矩阵"
    else -> "ATMOSPHERIC MATRIX"
  }

  fun sectionWeeklyReport(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "INFORME ARQUITECTÓNICO 7 DÍAS"
    AppLanguage.FRENCH -> "RAPPORT ARCHITECTURAL 7 JOURS"
    AppLanguage.GERMAN -> "7-TAGE ARCHITEKTUR BERICHT"
    AppLanguage.HINDI -> "7-दिवसीय वास्तुशिल्प रिपोर्ट"
    AppLanguage.JAPANESE -> "7日間建築レポート"
    AppLanguage.CHINESE -> "7天建筑架构报告"
    else -> "7-DAY ARCHITECTURAL REPORT"
  }

  fun metricRealFeel(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SENSACIÓN REAL"
    AppLanguage.FRENCH -> "RESSENTI RÉEL"
    AppLanguage.GERMAN -> "GEFÜHLTE TEMP"
    AppLanguage.HINDI -> "वास्तविक अनुभव"
    AppLanguage.JAPANESE -> "体感温度"
    AppLanguage.CHINESE -> "体感温度"
    else -> "REAL FEEL"
  }

  fun metricAqi(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "CALIDAD DEL AIRE"
    AppLanguage.FRENCH -> "QUALITÉ DE L'AIR"
    AppLanguage.GERMAN -> "LUFTQUALITÄT"
    AppLanguage.HINDI -> "वायु गुणवत्ता"
    AppLanguage.JAPANESE -> "空気の質"
    AppLanguage.CHINESE -> "空气质量"
    else -> "AIR QUALITY"
  }

  fun metricWind(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "VIENTO Y RÁFAGAS"
    AppLanguage.FRENCH -> "VENT ET RAFALES"
    AppLanguage.GERMAN -> "WIND & BÖEN"
    AppLanguage.HINDI -> "हवा और झोंके"
    AppLanguage.JAPANESE -> "風速・突風"
    AppLanguage.CHINESE -> "风速与阵风"
    else -> "WIND & GUSTS"
  }

  fun metricHumidity(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "HUMEDAD"
    AppLanguage.FRENCH -> "HUMIDITÉ"
    AppLanguage.GERMAN -> "FEUCHTIGKEIT"
    AppLanguage.HINDI -> "नमी"
    AppLanguage.JAPANESE -> "湿度"
    AppLanguage.CHINESE -> "湿度"
    else -> "HUMIDITY"
  }

  fun metricUv(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "RADIACIÓN UV"
    AppLanguage.FRENCH -> "RAYONNEMENT UV"
    AppLanguage.GERMAN -> "UV-STRAHLUNG"
    AppLanguage.HINDI -> "यूवी विकिरण"
    AppLanguage.JAPANESE -> "UV 紫外線"
    AppLanguage.CHINESE -> "紫外辐射"
    else -> "UV RADIATION"
  }

  fun metricPressure(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "PRESIÓN BAROMÉTRICA"
    AppLanguage.FRENCH -> "PRESSION BAROMÉTRIQUE"
    AppLanguage.GERMAN -> "LUFTDRUCK"
    AppLanguage.HINDI -> "वायुदाब"
    AppLanguage.JAPANESE -> "気圧"
    AppLanguage.CHINESE -> "气压"
    else -> "PRESSURE"
  }

  fun metricPrecipitation(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "PRECIPITACIÓN"
    AppLanguage.FRENCH -> "PRÉCIPITATION"
    AppLanguage.GERMAN -> "NIEDERSCHLAG"
    AppLanguage.HINDI -> "वर्षा"
    AppLanguage.JAPANESE -> "降水量"
    AppLanguage.CHINESE -> "降水量"
    else -> "PRECIPITATION"
  }

  fun metricSolar(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "ARCO SOLAR"
    AppLanguage.FRENCH -> "ARC SOLAIRE"
    AppLanguage.GERMAN -> "SONNENBOGEN"
    AppLanguage.HINDI -> "सौर चाप"
    AppLanguage.JAPANESE -> "太陽のアーク"
    AppLanguage.CHINESE -> "太阳运行轨迹"
    else -> "SOLAR ARC"
  }

  fun conditionLabel(conditionName: String, lang: AppLanguage): String {
    val upper = conditionName.uppercase()
    return when (lang) {
      AppLanguage.SPANISH -> when {
        "CLEAR" in upper -> "CIELO DESPEJADO"
        "CLOUDY" in upper || "PARTLY" in upper -> "PARCIALMENTE NUBLADO"
        "OVERCAST" in upper -> "CUBIERTO"
        "FOG" in upper -> "NIEBLA ESPESA"
        "DRIZZLE" in upper -> "LLOVIZNA"
        "RAIN" in upper -> "LLUVIA"
        "SNOW" in upper -> "NIEVE"
        "THUNDERSTORM" in upper -> "TORMENTA ELÉCTRICA"
        else -> upper
      }
      AppLanguage.FRENCH -> when {
        "CLEAR" in upper -> "CIEL DÉGAGÉ"
        "CLOUDY" in upper || "PARTLY" in upper -> "PARTIELLEMENT NUAGEUX"
        "OVERCAST" in upper -> "COUVERT"
        "FOG" in upper -> "BROUILLARD"
        "DRIZZLE" in upper -> "BRUINE"
        "RAIN" in upper -> "PLUIE"
        "SNOW" in upper -> "NEIGE"
        "THUNDERSTORM" in upper -> "ORAGE"
        else -> upper
      }
      AppLanguage.GERMAN -> when {
        "CLEAR" in upper -> "KLARER HIMMEL"
        "CLOUDY" in upper || "PARTLY" in upper -> "TEILWEISE BEWÖLKT"
        "OVERCAST" in upper -> "BEWÖLKT"
        "FOG" in upper -> "NEBEL"
        "DRIZZLE" in upper -> "SPRÜHREGEN"
        "RAIN" in upper -> "REGEN"
        "SNOW" in upper -> "SCHNEE"
        "THUNDERSTORM" in upper -> "GEWITTER"
        else -> upper
      }
      AppLanguage.HINDI -> when {
        "CLEAR" in upper -> "साफ आसमान"
        "CLOUDY" in upper || "PARTLY" in upper -> "आंशिक रूप से बादल"
        "OVERCAST" in upper -> "घने बादल"
        "FOG" in upper -> "कोहरा"
        "DRIZZLE" in upper -> "बूंदाबांदी"
        "RAIN" in upper -> "बारिश"
        "SNOW" in upper -> "बर्फबारी"
        "THUNDERSTORM" in upper -> "बिजली कड़कना"
        else -> upper
      }
      AppLanguage.JAPANESE -> when {
        "CLEAR" in upper -> "快晴"
        "CLOUDY" in upper || "PARTLY" in upper -> "晴れ時々曇り"
        "OVERCAST" in upper -> "本曇り"
        "FOG" in upper -> "濃霧"
        "DRIZZLE" in upper -> "小雨"
        "RAIN" in upper -> "雨"
        "SNOW" in upper -> "雪"
        "THUNDERSTORM" in upper -> "雷雨"
        else -> upper
      }
      AppLanguage.CHINESE -> when {
        "CLEAR" in upper -> "晴朗"
        "CLOUDY" in upper || "PARTLY" in upper -> "多云"
        "OVERCAST" in upper -> "阴天"
        "FOG" in upper -> "大雾"
        "DRIZZLE" in upper -> "毛毛细雨"
        "RAIN" in upper -> "降雨"
        "SNOW" in upper -> "降雪"
        "THUNDERSTORM" in upper -> "雷暴天气"
        else -> upper
      }
      else -> upper
    }
  }
}
