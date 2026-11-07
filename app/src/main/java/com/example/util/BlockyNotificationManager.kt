package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserSettings
import com.example.domain.model.WeatherForecast
import com.example.domain.model.toDisplayTemp
import kotlin.math.roundToInt

object BlockyNotificationManager {
  private const val CHANNEL_PINNED_ID = "blocky_weather_pinned_channel"
  private const val CHANNEL_ALERTS_ID = "blocky_weather_alerts_channel"
  
  private const val NOTIFICATION_ID_PINNED = 1001
  private const val NOTIFICATION_ID_SEVERE_ALERT = 2002

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return false
      }
    }
    return true
  }

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val pinnedChannel =
        NotificationChannel(
          CHANNEL_PINNED_ID,
          "Pinned Weather Telemetry",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "Displays persistent live weather telemetry on the status bar"
          setShowBadge(false)
        }

      val alertsChannel =
        NotificationChannel(
          CHANNEL_ALERTS_ID,
          "Severe Weather Alerts",
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "Emergency notifications for extreme weather hazards"
          enableVibration(true)
          setShowBadge(true)
        }

      notificationManager.createNotificationChannel(pinnedChannel)
      notificationManager.createNotificationChannel(alertsChannel)
    }
  }

  fun updatePinnedNotification(
    context: Context,
    forecast: WeatherForecast,
    settings: UserSettings,
    quipText: String,
  ) {
    if (!settings.pinnedNotificationEnabled) {
      cancelPinnedNotification(context)
      return
    }

    if (!hasNotificationPermission(context)) {
      return
    }

    createNotificationChannels(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val currentTempStr = "${forecast.currentTempC.toDisplayTemp(settings.useFahrenheit)}°"
    val feelsStr = "${forecast.apparentTempC.toDisplayTemp(settings.useFahrenheit)}°"
    val today = forecast.daily.firstOrNull()
    val highStr = today?.let { "${it.tempMaxC.toDisplayTemp(settings.useFahrenheit)}°" } ?: "--"
    val lowStr = today?.let { "${it.tempMinC.toDisplayTemp(settings.useFahrenheit)}°" } ?: "--"

    val title = "${forecast.cityName.uppercase()} · $currentTempStr ${forecast.condition.displayTitle}"
    val shortContent = "High: $highStr · Low: $lowStr · Feels $feelsStr"
    val expandedContent =
      "Feels $feelsStr · AQI ${forecast.usAqi ?: 25} · Wind ${forecast.windSpeedKmh.roundToInt()} km/h · Hum ${forecast.humidityPercent}% · Barometer ${forecast.pressureHpa.roundToInt()} hPa"

    val notification =
      NotificationCompat.Builder(context, CHANNEL_PINNED_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(shortContent)
        .setStyle(NotificationCompat.BigTextStyle().bigText(expandedContent))
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(NOTIFICATION_ID_PINNED, notification)
  }

  fun cancelPinnedNotification(context: Context) {
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NOTIFICATION_ID_PINNED)
  }

  fun postSevereAlertNotification(
    context: Context,
    alertTitle: String,
    alertDesc: String,
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          android.Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return
      }
    }

    createNotificationChannels(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      1,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("⚠️ SEVERE WEATHER: $alertTitle")
        .setContentText(alertDesc)
        .setStyle(NotificationCompat.BigTextStyle().bigText(alertDesc))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .build()

    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(NOTIFICATION_ID_SEVERE_ALERT, notification)
  }
}
