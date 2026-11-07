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

  fun metricVisibility(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "VISIBILIDAD"
    AppLanguage.FRENCH -> "VISIBILITÉ"
    AppLanguage.GERMAN -> "SICHTWEITE"
    AppLanguage.HINDI -> "दृश्यता"
    AppLanguage.JAPANESE -> "視程"
    AppLanguage.CHINESE -> "能见度"
    else -> "VISIBILITY"
  }

  fun metricMoonPhase(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "FASE LUNAR"
    AppLanguage.FRENCH -> "PHASE LUNAIRE"
    AppLanguage.GERMAN -> "MONDPHASE"
    AppLanguage.HINDI -> "चंद्र चरण"
    AppLanguage.JAPANESE -> "月相"
    AppLanguage.CHINESE -> "月相"
    else -> "MOON PHASE"
  }

  fun settingsTitle(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "AJUSTES ARQUITECTÓNICOS"
    AppLanguage.FRENCH -> "PARAMÈTRES ARCHITECTURAUX"
    AppLanguage.GERMAN -> "ARCHITEKTUR EINSTELLUNGEN"
    AppLanguage.HINDI -> "वास्तुशिल्प सेटिंग्स"
    AppLanguage.JAPANESE -> "建築設定"
    AppLanguage.CHINESE -> "建筑架构设置"
    else -> "ARCHITECTURAL SETTINGS"
  }

  fun sectionTheme(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "APARIENCIA Y TEMA"
    AppLanguage.FRENCH -> "APPARENCE ET THÈME"
    AppLanguage.GERMAN -> "ERSCHEINUNGSBILD & THEMA"
    AppLanguage.HINDI -> "उपस्थिति और थीम"
    AppLanguage.JAPANESE -> "外観とテーマ"
    AppLanguage.CHINESE -> "外观与主题"
    else -> "APPEARANCE & THEME"
  }

  fun sectionLanguage(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "IDIOMA Y LOCALIZACIÓN"
    AppLanguage.FRENCH -> "LANGUE ET LOCALISATION"
    AppLanguage.GERMAN -> "SPRACHE & LOKALISIERUNG"
    AppLanguage.HINDI -> "भाषा और स्थानीयकरण"
    AppLanguage.JAPANESE -> "言語とローカリゼーション"
    AppLanguage.CHINESE -> "语言与本地化"
    else -> "LANGUAGE & LOCALIZATION"
  }

  fun sectionTelemetry(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "TELEMETRÍA Y SINCRONIZACIÓN"
    AppLanguage.FRENCH -> "TÉLÉMÉTRIE ET SYNC"
    AppLanguage.GERMAN -> "TELEMETRIE & SYNC"
    AppLanguage.HINDI -> "टेलीमेट्री और सिंक"
    AppLanguage.JAPANESE -> "テレメトリと同期"
    AppLanguage.CHINESE -> "遥测与同步"
    else -> "TELEMETRY & SYNC"
  }

  fun sectionNotifications(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "NOTIFICACIONES Y ALERTAS"
    AppLanguage.FRENCH -> "NOTIFICATIONS ET ALERTES"
    AppLanguage.GERMAN -> "BENACHRICHTIGUNGEN & ALARME"
    AppLanguage.HINDI -> "सूचनाएं और अलर्ट"
    AppLanguage.JAPANESE -> "通知とアラート"
    AppLanguage.CHINESE -> "通知与提醒"
    else -> "NOTIFICATIONS & ALERTS"
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

  fun actionSyncNow(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SINCRONIZAR AHORA"
    AppLanguage.FRENCH -> "SYNCHRONISER MAINTENANT"
    AppLanguage.GERMAN -> "JETZT SYNCHRONISIEREN"
    AppLanguage.HINDI -> "अभी सिंक करें"
    AppLanguage.JAPANESE -> "今すぐ同期"
    AppLanguage.CHINESE -> "立即同步"
    else -> "SYNC NOW"
  }

  fun actionGridStations(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "ESTACIONES DE RED"
    AppLanguage.FRENCH -> "STATIONS DU RÉSEAU"
    AppLanguage.GERMAN -> "GRID-STATIONEN"
    AppLanguage.HINDI -> "ग्रिड स्टेशन"
    AppLanguage.JAPANESE -> "グリッドステーション"
    AppLanguage.CHINESE -> "网格站点"
    else -> "GRID STATIONS"
  }

  fun labelAppLanguage(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "IDIOMA DE LA APP"
    AppLanguage.FRENCH -> "LANGUE DE L'APP"
    AppLanguage.GERMAN -> "APP-SPRACHE"
    AppLanguage.HINDI -> "ऐप की भाषा"
    AppLanguage.JAPANESE -> "アプリの言語"
    AppLanguage.CHINESE -> "应用语言"
    else -> "APP LANGUAGE"
  }

  fun settingsDayNight(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "TEMA DÍA / NOCHE AUTO"
    AppLanguage.FRENCH -> "THÈME JOUR / NUIT AUTO"
    AppLanguage.GERMAN -> "AUTO TAG- / NACHT-THEMA"
    AppLanguage.HINDI -> "ऑटो दिन / रात थीम"
    AppLanguage.JAPANESE -> "自動昼夜テーマ"
    AppLanguage.CHINESE -> "自动昼夜主题"
    else -> "AUTO DAY / NIGHT THEME"
  }

  fun settingsDarkMode(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "MODO OSCURO"
    AppLanguage.FRENCH -> "MODE SOMBRE"
    AppLanguage.GERMAN -> "DUNKLER MODUS"
    AppLanguage.HINDI -> "डार्क मोड"
    AppLanguage.JAPANESE -> "ダークモード"
    AppLanguage.CHINESE -> "深色模式"
    else -> "DARK MODE"
  }

  fun settingsDynamicTheme(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "TEMA DINÁMICO"
    AppLanguage.FRENCH -> "THÈME DYNAMIQUE"
    AppLanguage.GERMAN -> "DYNAMISCHES THEMA"
    AppLanguage.HINDI -> "गतिशील थीम"
    AppLanguage.JAPANESE -> "ダイナミックテーマ"
    AppLanguage.CHINESE -> "动态主题"
    else -> "DYNAMIC THEME"
  }

  fun settingsReduceAnimations(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "REDUCIR ANIMACIONES"
    AppLanguage.FRENCH -> "RÉDUIRE LES ANIMATIONS"
    AppLanguage.GERMAN -> "ANIMATIONEN REDUZIEREN"
    AppLanguage.HINDI -> "एनीमेशन कम करें"
    AppLanguage.JAPANESE -> "アニメーションを減らす"
    AppLanguage.CHINESE -> "减弱动画"
    else -> "REDUCE ANIMATIONS"
  }

  fun settingsHeroFont(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "FUENTE DE TEMPERATURA"
    AppLanguage.FRENCH -> "POLICE DE TEMPÉRATURE"
    AppLanguage.GERMAN -> "TEMPERATUR-SCHRIFT"
    AppLanguage.HINDI -> "तापमान फ़ॉन्ट"
    AppLanguage.JAPANESE -> "気温のフォント"
    AppLanguage.CHINESE -> "温度字体"
    else -> "MAIN TEMPERATURE FONT"
  }

  fun settingsNudges(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "COMENTARIOS DIVERTIDOS"
    AppLanguage.FRENCH -> "COMMENTAIRES AMUSANTS"
    AppLanguage.GERMAN -> "LUSTIGE KOMMENTARE"
    AppLanguage.HINDI -> "मज़ेदार टिप्पणियाँ"
    AppLanguage.JAPANESE -> "楽しいコメント"
    AppLanguage.CHINESE -> "有趣评论"
    else -> "HERO NUDGES"
  }

  fun settingsPinnedNotif(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "NOTIFICACIÓN FIJA"
    AppLanguage.FRENCH -> "NOTIFICATION ÉPINGLÉE"
    AppLanguage.GERMAN -> "ANGEHEFTETE BENACHRICHTIGUNG"
    AppLanguage.HINDI -> "पिन की गई अधिसूचना"
    AppLanguage.JAPANESE -> "固定された通知"
    AppLanguage.CHINESE -> "常驻通知"
    else -> "PINNED NOTIFICATION"
  }

  fun settingsMatrixOverlay(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SUPERPOSICIÓN DE MATRIZ"
    AppLanguage.FRENCH -> "SUPERPOSITION DE MATRICE"
    AppLanguage.GERMAN -> "MATRIX-OVERLAY"
    AppLanguage.HINDI -> "मैट्रिक्स ओवरले"
    AppLanguage.JAPANESE -> "マトリックスオーバーレイ"
    AppLanguage.CHINESE -> "矩阵叠加"
    else -> "SEVERE MATRIX OVERLAY"
  }

  fun labelSummary(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "RESUMEN DEL DÍA"
    AppLanguage.FRENCH -> "RÉSUMÉ DU JOUR"
    AppLanguage.GERMAN -> "TAGESZUSAMMENFASSUNG"
    AppLanguage.HINDI -> "दिन का सारांश"
    AppLanguage.JAPANESE -> "一日のまとめ"
    AppLanguage.CHINESE -> "今日摘要"
    else -> "DAY SUMMARY"
  }

  fun labelTemperatureSwing(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "OSCILACIÓN TÉRMICA"
    AppLanguage.FRENCH -> "VARIATION THERMIQUE"
    AppLanguage.GERMAN -> "TEMPERATURSCHWANKUNG"
    AppLanguage.HINDI -> "तापमान में बदलाव"
    AppLanguage.JAPANESE -> "気温の変化"
    AppLanguage.CHINESE -> "温度变化"
    else -> "TEMPERATURE SWING"
  }

  fun labelWind(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "VIENTO"
    AppLanguage.FRENCH -> "VENT"
    AppLanguage.GERMAN -> "WIND"
    AppLanguage.HINDI -> "हवा"
    AppLanguage.JAPANESE -> "風"
    AppLanguage.CHINESE -> "风"
    else -> "WIND"
  }

  fun labelPrecipitation(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "PRECIPITACIÓN"
    AppLanguage.FRENCH -> "PRÉCIPITATION"
    AppLanguage.GERMAN -> "NIEDERSCHLAG"
    AppLanguage.HINDI -> "降水量"
    AppLanguage.JAPANESE -> "降水量"
    AppLanguage.CHINESE -> "降水量"
    else -> "PRECIPITATION"
  }

  fun labelHigh(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "MÁX"
    AppLanguage.FRENCH -> "MAX"
    AppLanguage.GERMAN -> "HOCH"
    AppLanguage.HINDI -> "उच्च"
    AppLanguage.JAPANESE -> "最高"
    AppLanguage.CHINESE -> "最高"
    else -> "HIGH"
  }

  fun labelLow(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "MÍN"
    AppLanguage.FRENCH -> "MIN"
    AppLanguage.GERMAN -> "TIEF"
    AppLanguage.HINDI -> "निम्न"
    AppLanguage.JAPANESE -> "最低"
    AppLanguage.CHINESE -> "最低"
    else -> "LOW"
  }

  fun labelAqi(lang: AppLanguage): String = when (lang) {
    else -> "AQI"
  }

  fun labelSunrise(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SALIDA SOL"
    AppLanguage.FRENCH -> "LEVER"
    AppLanguage.GERMAN -> "SONNENAUFGANG"
    AppLanguage.HINDI -> "सूर्योदय"
    AppLanguage.JAPANESE -> "日の出"
    AppLanguage.CHINESE -> "日出"
    else -> "SUNRISE"
  }

  fun labelSunset(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "PUESTA SOL"
    AppLanguage.FRENCH -> "COUCHER"
    AppLanguage.GERMAN -> "SONNENUNTERGANG"
    AppLanguage.HINDI -> "सूर्यास्त"
    AppLanguage.JAPANESE -> "日の入り"
    AppLanguage.CHINESE -> "日落"
    else -> "SUNSET"
  }

  fun labelUvPeak(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "PICO UV"
    AppLanguage.FRENCH -> "PIC UV"
    AppLanguage.GERMAN -> "UV SPITZE"
    AppLanguage.HINDI -> "यूवी पीक"
    AppLanguage.JAPANESE -> "UVピーク"
    AppLanguage.CHINESE -> "UV峰值"
    else -> "UV PEAK"
  }

  fun statusOffline(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "DESCONECTADO"
    AppLanguage.FRENCH -> "HORS LIGNE"
    AppLanguage.GERMAN -> "OFFLINE"
    AppLanguage.HINDI -> "ऑफलाइन"
    AppLanguage.JAPANESE -> "オフライン"
    AppLanguage.CHINESE -> "离线"
    else -> "OFFLINE"
  }

  fun actionPullToSync(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "TIRAR PARA SINCRONIZAR"
    AppLanguage.FRENCH -> "TIRER POUR SYNC"
    AppLanguage.GERMAN -> "ZUM SYNC ZIEHEN"
    AppLanguage.HINDI -> "सिंक करने के लिए खींचें"
    AppLanguage.JAPANESE -> "引いて同期"
    AppLanguage.CHINESE -> "下拉同步"
    else -> "PULL TO SYNC"
  }

  fun statusSyncing(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SINCRONIZANDO..."
    AppLanguage.FRENCH -> "SYNCHRONISATION..."
    AppLanguage.GERMAN -> "SYNCHRONISIERE..."
    AppLanguage.HINDI -> "सिंक हो रहा है..."
    AppLanguage.JAPANESE -> "同期中..."
    AppLanguage.CHINESE -> "同步中..."
    else -> "SYNCING..."
  }

  fun labelFeels(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SENSACIÓN"
    AppLanguage.FRENCH -> "RESSENTI"
    AppLanguage.GERMAN -> "GEFÜHLT"
    AppLanguage.HINDI -> "महसूस"
    AppLanguage.JAPANESE -> "体感"
    AppLanguage.CHINESE -> "体感"
    else -> "FEELS"
  }

  fun labelHumidityShort(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "HUM"
    AppLanguage.FRENCH -> "HUM"
    AppLanguage.GERMAN -> "FEUCHT"
    AppLanguage.HINDI -> "नमी"
    AppLanguage.JAPANESE -> "湿度"
    AppLanguage.CHINESE -> "湿度"
    else -> "HUM"
  }

  fun actionReleaseToSync(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SOLTAR PARA SINCRONIZAR [↓]"
    AppLanguage.FRENCH -> "RELACHER POUR SYNC [↓]"
    AppLanguage.GERMAN -> "LOSLASSEN ZUM SYNC [↓]"
    AppLanguage.HINDI -> "सिंक करने के लिए छोड़ें [↓]"
    AppLanguage.JAPANESE -> "放して同期 [↓]"
    AppLanguage.CHINESE -> "松开同步 [↓]"
    else -> "RELEASE TO SYNC [↓]"
  }

  fun labelPreviewMode(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "VISTA PREVIA"
    AppLanguage.FRENCH -> "APERÇU"
    AppLanguage.GERMAN -> "VORSCHAU"
    AppLanguage.HINDI -> "पूर्वावलोकन"
    AppLanguage.JAPANESE -> "プレビュー"
    AppLanguage.CHINESE -> "预览"
    else -> "PREVIEW"
  }

  fun labelResetToLive(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "TOCA PARA VOLVER AL VIVO"
    AppLanguage.FRENCH -> "TOUCHER POUR LE DIRECT"
    AppLanguage.GERMAN -> "TIPPEN FÜR ECHTZEIT"
    AppLanguage.HINDI -> "लाइव पर वापस जाने के लिए टैप करें"
    AppLanguage.JAPANESE -> "タップでライブに戻る"
    AppLanguage.CHINESE -> "点击回到实时"
    else -> "TAP TO RESET TO LIVE"
  }
  fun settingsDayNightSub(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Tema claro de día · Tema oscuro de noche"
    AppLanguage.FRENCH -> "Thème clair le jour · Thème sombre la nuit"
    AppLanguage.GERMAN -> "Helles Thema am Tag · Dunkles Thema in der Nacht"
    AppLanguage.HINDI -> "दिन में लाइट मोड · रात में डार्क मोड"
    AppLanguage.JAPANESE -> "昼間はライトモード · 夜間はダークモード"
    AppLanguage.CHINESE -> "日间亮色模式 · 夜间深色模式"
    else -> "Light mode in daytime · Dark mode at night"
  }

  fun settingsDarkModeSub(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Forzar tema oscuro AMOLED siempre"
    AppLanguage.FRENCH -> "Forcer le thème sombre AMOLED toujours"
    AppLanguage.GERMAN -> "Immer dunkles AMOLED-Thema erzwingen"
    AppLanguage.HINDI -> "हमेशा डार्क अमोलेड थीम लागू करें"
    AppLanguage.JAPANESE -> "常にAMOLEDダークテーマを強制する"
    AppLanguage.CHINESE -> "始终强制 AMOLED 深色主题"
    else -> "Force AMOLED dark theme always"
  }

  fun settingsDynamicThemeSub(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Colores dinámicos del sistema Material You"
    AppLanguage.FRENCH -> "Couleurs système dynamiques Material You"
    AppLanguage.GERMAN -> "Dynamische Systemfarben (Material You)"
    AppLanguage.HINDI -> "सिस्टम से लिए गए गतिशील रंग (Material You)"
    AppLanguage.JAPANESE -> "Material You ダイナミックシステムカラー"
    AppLanguage.CHINESE -> "Material You 动态系统颜色"
    else -> "Dynamic system colors (Material You)"
  }

  fun settingsDynamicThemeFallbackSub(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Requiere Android 12+"
    AppLanguage.FRENCH -> "Nécessite Android 12+"
    AppLanguage.GERMAN -> "Erfordert Android 12+"
    AppLanguage.HINDI -> "Android 12+ की आवश्यकता है"
    AppLanguage.JAPANESE -> "Android 12以上が必要"
    AppLanguage.CHINESE -> "需要 Android 12+"
    else -> "Requires Android 12+"
  }

  fun settingsReduceAnimationsSub(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Desactiva efectos de fondo y lluvia; mantiene la cuadrícula limpia"
    AppLanguage.FRENCH -> "Désactive les effets de fond et la pluie ; garde la grille propre"
    AppLanguage.GERMAN -> "Deaktiviert Hintergrundeffekte und Regen; bewahrt sauberes Raster"
    AppLanguage.HINDI -> "बैकग्राउंड प्रभाव और बारिश बंद करें; साफ ग्रिड रखें"
    AppLanguage.JAPANESE -> "背景エフェクトと雨を無効化し、クリーンなグリッドを維持します"
    AppLanguage.CHINESE -> "禁用背景特效与下雨动画；保留整洁的网格"
    else -> "Disables background particle effects & weather rain; retains clean grid"
  }

  fun settingsUnitDesc(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Cambiar entre Fahrenheit y Celsius"
    AppLanguage.FRENCH -> "Basculer entre Fahrenheit et Celsius"
    AppLanguage.GERMAN -> "Zwischen Fahrenheit und Celsius wechseln"
    AppLanguage.HINDI -> "फारेनहाइट और सेल्सियस के बीच स्विच करें"
    AppLanguage.JAPANESE -> "華氏と摂氏を切り替える"
    AppLanguage.CHINESE -> "切换华氏度和摄氏度"
    else -> "Switch between Fahrenheit and Celsius"
  }

  fun settingsNudgesSub(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Muestra consejos y datos meteorológicos adaptativos"
    AppLanguage.FRENCH -> "Affiche des conseils et faits météo adaptatifs"
    AppLanguage.GERMAN -> "Zeigt adaptive Wettertipps und Fakten an"
    AppLanguage.HINDI -> "अनुकूली मौसम युक्तियाँ और तथ्य दिखाएं"
    AppLanguage.JAPANESE -> "適応型の気象ヒントと事実を表示します"
    AppLanguage.CHINESE -> "显示自适应天气提示与实况事实"
    else -> "Show adaptive weather tips and meteorology facts"
  }

  fun settingsHeroFontSub(standard: Boolean, lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> if (standard) "Fuente de tipografía negrita estándar" else "Dígitos de matriz de píxeles exclusivos"
    AppLanguage.FRENCH -> if (standard) "Police typographique grasse standard" else "Chiffres exclusifs en matrice de pixels"
    AppLanguage.GERMAN -> if (standard) "Standard-Fettschrift" else "Exklusive Pixel-Matrix-Ziffern"
    AppLanguage.HINDI -> if (standard) "मानक बोल्ड फ़ॉन्ट" else "विशिष्ट पिक्सेल मैट्रिक्स अंक"
    AppLanguage.JAPANESE -> if (standard) "標準的な太字フォント" else "独自のピクセルマトリックス数字"
    AppLanguage.CHINESE -> if (standard) "标准粗体字形" else "独特的像素矩阵数字"
    else -> if (standard) "Standard bold typography font" else "Signature blocky pixel matrix digits"
  }

  fun settingsPinnedNotifSub(granted: Boolean, lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> if (!granted) "Permiso requerido para la barra de estado" else "Muestra temperatura y comentario en la barra de estado"
    AppLanguage.FRENCH -> if (!granted) "Permission requise pour la barre d'état" else "Affiche la température et commentaire dans la barre d'état"
    AppLanguage.GERMAN -> if (!granted) "Berechtigung für Statusleiste erforderlich" else "Zeigt Temperatur & Kommentar in der Statusleiste an"
    AppLanguage.HINDI -> if (!granted) "स्टेटस बार के लिए अनुमति आवश्यक है" else "स्टेटस बार में तापमान और टिप्पणी प्रदर्शित करें"
    AppLanguage.JAPANESE -> if (!granted) "ステータスバーの許可が必要です" else "ステータスバーに気温とコメントを表示します"
    AppLanguage.CHINESE -> if (!granted) "状态栏常驻需要权限" else "在状态栏显示实时温度与点评"
    else -> if (!granted) "Permission required for status bar card" else "Display persistent live temperature & quip on status bar"
  }

  fun settingsMatrixOverlaySub(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Superposición de alto contraste en alertas graves (Viento, UV, Tormentas)"
    AppLanguage.FRENCH -> "Superposition à haut contraste lors d'alertes graves (Vent, UV, Orages)"
    AppLanguage.GERMAN -> "Hochkontrast-Overlay bei schweren Alarmen (Wind, UV, Stürme)"
    AppLanguage.HINDI -> "गंभीर अलर्ट (हवा, यूवी, तूफान) पर उच्च कंट्रास्ट ओवरले दिखाएं"
    AppLanguage.JAPANESE -> "重大なアラート（風、UV、嵐）時に高コントラストのオーバーレイを表示"
    AppLanguage.CHINESE -> "在严重警报（强风、紫外线、雷暴）时显示高对比度叠加层"
    else -> "High-contrast overlay on severe alerts (Wind, UV, Pollutants, Storms)"
  }

  fun settingsSyncTip(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "CONSEJO: DESLIZA HACIA ABAJO EN LA CUADRÍCULA PARA SINCRONIZAR."
    AppLanguage.FRENCH -> "ASTUCE : GLISSEZ VERS LE BAS SUR LA GRILLE POUR SYNCHRONISER."
    AppLanguage.GERMAN -> "TIPP: AUF DEM RASTER NACH UNTEN ZIEHEN ZUM SYNCHRONISIEREN."
    AppLanguage.HINDI -> "टिप: सिंक करने के लिए ग्रिड पर नीचे खींचें।"
    AppLanguage.JAPANESE -> "ヒント：グリッド上で下にスワイプして同期します。"
    AppLanguage.CHINESE -> "提示：在天气网格上向下滑动以触发进度同步。"
    else -> "TIP: PULL DOWN ANYWHERE ON THE WEATHER GRID TO TRIGGER PROGRESSIVE SYNC."
  }

  fun notifBlocked(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "PERMISO DE NOTIFICACIÓN BLOQUEADO"
    AppLanguage.FRENCH -> "PERMISSION DE NOTIFICATION BLOQUÉE"
    AppLanguage.GERMAN -> "BENACHRICHTIGUNGSBERECHTIGUNG BLOCKIERT"
    AppLanguage.HINDI -> "अधिसूचना अनुमति अवरुद्ध है"
    AppLanguage.JAPANESE -> "通知の許可がブロックされています"
    AppLanguage.CHINESE -> "通知权限被拦截或未授予"
    else -> "NOTIFICATION PERMISSION BLOCKED"
  }

  fun actionAllowPermission(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "PERMITIR ACCESO"
    AppLanguage.FRENCH -> "AUTORISER L'ACCÈS"
    AppLanguage.GERMAN -> "ZUGRIFF ERLAUBEN"
    AppLanguage.HINDI -> "अनुमति दें"
    AppLanguage.JAPANESE -> "許可する"
    AppLanguage.CHINESE -> "授予权限"
    else -> "ALLOW PERMISSION"
  }

  fun actionAppSettings(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "AJUSTES APP"
    AppLanguage.FRENCH -> "PARAMÈTRES APP"
    AppLanguage.GERMAN -> "APP-EINSTELLUNGEN"
    AppLanguage.HINDI -> "ऐप सेटिंग्स"
    AppLanguage.JAPANESE -> "アプリ設定"
    AppLanguage.CHINESE -> "应用设置"
    else -> "APP SETTINGS"
  }

  fun quipCough(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "*tose intensamente* ayúdame..."
    AppLanguage.FRENCH -> "*tousse intensément* aide-moi..."
    AppLanguage.GERMAN -> "*hustet heftig* hilf mir..."
    AppLanguage.HINDI -> "*तेजी से खांसते हुए* मेरी मदद करो..."
    AppLanguage.JAPANESE -> "*激しく咳き込む* 助けて..."
    AppLanguage.CHINESE -> "*剧烈咳嗽* 救救我..."
    else -> "*coughs intensely* help me..."
  }

  fun quipNuclearFusion(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Básicamente una fusión nuclear afuera. Quédate adentro."
    AppLanguage.FRENCH -> "C'est pratiquement la fusion nucléaire dehors. Reste à l'intérieur."
    AppLanguage.GERMAN -> "Draußen herrscht praktisch Kernfusion. Bleib drin."
    AppLanguage.HINDI -> "बाहर व्यावहारिक रूप से परमाणु संलयन हो रहा है। अंदर रहें।"
    AppLanguage.JAPANESE -> "外は実質的に核融合状態です。室内にいましょう。"
    AppLanguage.CHINESE -> "外面基本上是在核聚变，待在室内吧。"
    else -> "Basically a nuclear fusion outside. stay in."
  }

  fun quipIceCream(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Se me antoja un helado"
    AppLanguage.FRENCH -> "J'ai envie d'une glace"
    AppLanguage.GERMAN -> "Ich habe Lust auf Eis"
    AppLanguage.HINDI -> "मुझे आइसक्रीम चाहिए"
    AppLanguage.JAPANESE -> "アイスが食べたい"
    AppLanguage.CHINESE -> "我渴望冰淇淋"
    else -> "I crave icecream"
  }

  fun quipTea(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Una taza de té no le haría mal a nadie."
    AppLanguage.FRENCH -> "Une tasse de thé ne ferait de mal à personne."
    AppLanguage.GERMAN -> "Eine Tasse Tee hat noch niemandem geschadet."
    AppLanguage.HINDI -> "एक कप चाय किसी को नुकसान नहीं पहुँचाएगी।"
    AppLanguage.JAPANESE -> "一杯の紅茶は誰にも害を与えません。"
    AppLanguage.CHINESE -> "喝杯茶不会有坏处的。"
    else -> "A cup of tea wont hurt nobody."
  }

  fun quipUmbrella(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Lleva paraguas, de nada"
    AppLanguage.FRENCH -> "Prends un parapluie, de rien"
    AppLanguage.GERMAN -> "Nimm einen Regenschirm mit, gern geschehen"
    AppLanguage.HINDI -> "छाता साथ रखें, आपका स्वागत है"
    AppLanguage.JAPANESE -> "傘を持って行って、どういたしまして"
    AppLanguage.CHINESE -> "记得带伞，不客气"
    else -> "carry umbrellas, you are welcome"
  }

  fun factClouds(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Una sola nube cumulonimbus puede contener más de 500,000 toneladas de agua."
    AppLanguage.FRENCH -> "Un seul cumulonimbus peut contenir plus de 500 000 tonnes d'eau."
    AppLanguage.GERMAN -> "Eine einzige Cumulonimbus-Wolke kann über 500.000 Tonnen Wasser enthalten."
    AppLanguage.HINDI -> "एक अकेले क्यूम्यलोनिम्बस बादल में 500,000 टन से अधिक पानी हो सकता है।"
    AppLanguage.JAPANESE -> "たった一つの積乱雲には50万トン以上の水分が含まれることがあります。"
    AppLanguage.CHINESE -> "一朵积雨云可以包含超过 50 万吨的水滴。"
    else -> "A single cumulonimbus thunderstorm cloud can hold over 500,000 tons of water droplets."
  }

  fun quipHello(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "Hola :)"
    AppLanguage.FRENCH -> "Bonjour :)"
    AppLanguage.GERMAN -> "Hallo :)"
    AppLanguage.HINDI -> "नमस्ते :)"
    AppLanguage.JAPANESE -> "こんにちは :)"
    AppLanguage.CHINESE -> "你好 :)"
    else -> "Hello :)"
  }

  fun actionShare(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "COMPARTIR ↗"
    AppLanguage.FRENCH -> "PARTAGER ↗"
    AppLanguage.GERMAN -> "TEILEN ↗"
    AppLanguage.HINDI -> "साझा करें ↗"
    AppLanguage.JAPANESE -> "共有 ↗"
    AppLanguage.CHINESE -> "分享 ↗"
    else -> "SHARE ↗"
  }

  fun actionSettings(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "AJUSTES"
    AppLanguage.FRENCH -> "PARAMÈTRES"
    AppLanguage.GERMAN -> "EINSTELLUNGEN"
    AppLanguage.HINDI -> "सेटिंग्स"
    AppLanguage.JAPANESE -> "設定"
    AppLanguage.CHINESE -> "设置"
    else -> "SETTINGS"
  }

  fun shareTemplate(
    city: String,
    temp: String,
    feels: String,
    high: String,
    low: String,
    cond: String,
    wind: String,
    aqi: String,
    uv: String,
    lang: AppLanguage
  ): String {
    val header = "BLOCKY WEATHER · ${city.uppercase()}"
    return when (lang) {
      AppLanguage.SPANISH -> "$header\nTemp: $temp (Siente $feels) · Máx: $high / Mín: $low\nCondición: $cond · Viento: $wind\nAQI: $aqi · UV: $uv"
      AppLanguage.FRENCH -> "$header\nTemp: $temp (Ressenti $feels) · Max: $high / Min: $low\nCondition: $cond · Vent: $wind\nAQI: $aqi · UV: $uv"
      AppLanguage.GERMAN -> "$header\nTemp: $temp (Gefühlt $feels) · Hoch: $high / Tief: $low\nZustand: $cond · Wind: $wind\nAQI: $aqi · UV: $uv"
      AppLanguage.HINDI -> "$header\nतापमान: $temp (महसूस $feels) · उच्च: $high / निम्न: $low\nस्थिति: $cond · हवा: $wind\nAQI: $aqi · UV: $uv"
      AppLanguage.JAPANESE -> "$header\n気温: $temp (体感 $feels) · 最高: $high / 最低: $low\n状態: $cond · 風: $wind\nAQI: $aqi · UV: $uv"
      AppLanguage.CHINESE -> "$header\n温度: $temp (体感 $feels) · 最高: $high / 最低: $low\n天气: $cond · 风速: $wind\nAQI: $aqi · UV: $uv"
      else -> "$header\nTemp: $temp (Feels $feels) · High: $high / Low: $low\nCondition: $cond · Wind: $wind\nAQI: $aqi · UV: $uv"
    }
  }

  fun actionAddGrid(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "+ AÑADIR RED"
    AppLanguage.FRENCH -> "+ AJOUTER AU RÉSEAU"
    AppLanguage.GERMAN -> "+ GRID HINZUFÜGEN"
    AppLanguage.HINDI -> "+ ग्रिड जोड़ें"
    AppLanguage.JAPANESE -> "+ グリッドに追加"
    AppLanguage.CHINESE -> "+ 添加至网格"
    else -> "+ ADD GRID"
  }

  fun statusCached(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "EN CACHÉ"
    AppLanguage.FRENCH -> "EN CACHE"
    AppLanguage.GERMAN -> "GESPEICHERT"
    AppLanguage.HINDI -> "कैश्ड"
    AppLanguage.JAPANESE -> "キャッシュ済み"
    AppLanguage.CHINESE -> "已缓存"
    else -> "CACHED"
  }

  fun timeJustNow(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "AHORA MISMO"
    AppLanguage.FRENCH -> "À L'INSTANT"
    AppLanguage.GERMAN -> "GERADE EBEN"
    AppLanguage.HINDI -> "अभी-अभी"
    AppLanguage.JAPANESE -> "今すぐ"
    AppLanguage.CHINESE -> "刚刚"
    else -> "JUST NOW"
  }

  fun timeMinsAgo(mins: Long, lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "HACE ${mins}M"
    AppLanguage.FRENCH -> "IL Y A ${mins}M"
    AppLanguage.GERMAN -> "VOR ${mins} MIN."
    AppLanguage.HINDI -> "${mins} मिनट पहले"
    AppLanguage.JAPANESE -> "${mins}分前"
    AppLanguage.CHINESE -> "${mins} 分钟前"
    else -> "${mins}M AGO"
  }

  fun timeHoursAgo(hours: Long, lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "HACE ${hours}H"
    AppLanguage.FRENCH -> "IL Y A ${hours}H"
    AppLanguage.GERMAN -> "VOR ${hours} STD."
    AppLanguage.HINDI -> "${hours} घंटे पहले"
    AppLanguage.JAPANESE -> "${hours}時間前"
    AppLanguage.CHINESE -> "${hours} 小时前"
    else -> "${hours}H AGO"
  }

  fun moonPhaseName(phaseKey: String, lang: AppLanguage): String {
    val upper = phaseKey.uppercase()
    return when (lang) {
      AppLanguage.SPANISH -> when {
        "NEW" in upper -> "LUNA NUEVA"
        "WAXING CRESCENT" in upper -> "CRECIENTE"
        "FIRST QUARTER" in upper -> "CUARTO CRECIENTE"
        "WAXING GIBBOUS" in upper -> "GIBOSA CRECIENTE"
        "FULL" in upper -> "LUNA LLENA"
        "WANING GIBBOUS" in upper -> "GIBOSA MENGUANTE"
        "LAST QUARTER" in upper -> "CUARTO MENGUANTE"
        "WANING CRESCENT" in upper -> "MENGUANTE"
        else -> upper
      }
      AppLanguage.FRENCH -> when {
        "NEW" in upper -> "NOUVELLE LUNE"
        "WAXING CRESCENT" in upper -> "PREMIER CROISSANT"
        "FIRST QUARTER" in upper -> "PREMIER QUARTIER"
        "WAXING GIBBOUS" in upper -> "GIBBEUSE CROISSANTE"
        "FULL" in upper -> "PLEINE LUNE"
        "WANING GIBBOUS" in upper -> "GIBBEUSE DÉCROISSANTE"
        "LAST QUARTER" in upper -> "DERNIER QUARTIER"
        "WANING CRESCENT" in upper -> "DERNIER CROISSANT"
        else -> upper
      }
      AppLanguage.GERMAN -> when {
        "NEW" in upper -> "NEUMOND"
        "WAXING CRESCENT" in upper -> "ZUNEHMENDE SICHEL"
        "FIRST QUARTER" in upper -> "ERSTES VIERTEL"
        "WAXING GIBBOUS" in upper -> "ZUNEHMENDER MOND"
        "FULL" in upper -> "VOLLMOND"
        "WANING GIBBOUS" in upper -> "ABNEHMENDER MOND"
        "LAST QUARTER" in upper -> "LETZTES VIERTEL"
        "WANING CRESCENT" in upper -> "ABNEHMENDE SICHEL"
        else -> upper
      }
      AppLanguage.HINDI -> when {
        "NEW" in upper -> "अमावस्या"
        "WAXING CRESCENT" in upper -> "शुक्ल पक्ष अर्धचंद्र"
        "FIRST QUARTER" in upper -> "प्रथम चरण"
        "WAXING GIBBOUS" in upper -> "शुक्ल पक्ष कुबड़ा"
        "FULL" in upper -> "पूर्णिमा"
        "WANING GIBBOUS" in upper -> "कृष्ण पक्ष कुबड़ा"
        "LAST QUARTER" in upper -> "अंतिम चरण"
        "WANING CRESCENT" in upper -> "कृष्ण पक्ष अर्धचंद्र"
        else -> upper
      }
      AppLanguage.JAPANESE -> when {
        "NEW" in upper -> "新月"
        "WAXING CRESCENT" in upper -> "三日月"
        "FIRST QUARTER" in upper -> "上弦の月"
        "WAXING GIBBOUS" in upper -> "十三夜月"
        "FULL" in upper -> "満月"
        "WANING GIBBOUS" in upper -> "十六夜月"
        "LAST QUARTER" in upper -> "下弦の月"
        "WANING CRESCENT" in upper -> "有明月"
        else -> upper
      }
      AppLanguage.CHINESE -> when {
        "NEW" in upper -> "新月"
        "WAXING CRESCENT" in upper -> "娥眉月"
        "FIRST QUARTER" in upper -> "上弦月"
        "WAXING GIBBOUS" in upper -> "盈凸月"
        "FULL" in upper -> "满月"
        "WANING GIBBOUS" in upper -> "亏凸月"
        "LAST QUARTER" in upper -> "下弦月"
        "WANING CRESCENT" in upper -> "残月"
        else -> upper
      }
      else -> upper
    }
  }

  fun dragHintHold(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "MANTÉN Y ARRASTRA"
    AppLanguage.FRENCH -> "MAINTENIR ET GLISSER"
    AppLanguage.GERMAN -> "HALTEN & ZIEHEN"
    AppLanguage.HINDI -> "दबाकर खींचें"
    AppLanguage.JAPANESE -> "長押ししてドラッグ"
    AppLanguage.CHINESE -> "按住并拖动"
    else -> "HOLD & DRAG"
  }

  fun dragHintDrop(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "SOLTAR EN CASILLA"
    AppLanguage.FRENCH -> "DÉPOSER DANS L'EMPLACEMENT"
    AppLanguage.GERMAN -> "IN SLOT ABLEGEN"
    AppLanguage.HINDI -> "स्लॉट में छोड़ें"
    AppLanguage.JAPANESE -> "スロットにドロップ"
    AppLanguage.CHINESE -> "放入插槽"
    else -> "DROP IN SLOT"
  }

  fun labelStability(lang: AppLanguage): String = when (lang) {
    AppLanguage.SPANISH -> "ESTABILIDAD"
    AppLanguage.FRENCH -> "STABILITÉ"
    AppLanguage.GERMAN -> "STABILITÄT"
    AppLanguage.HINDI -> "स्थिरता"
    AppLanguage.JAPANESE -> "安定度"
    AppLanguage.CHINESE -> "稳定性"
    else -> "STABILITY"
  }
}
