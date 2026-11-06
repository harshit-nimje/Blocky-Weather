package com.example

import android.app.Application
import com.example.data.local.GridcastDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.WeatherRepository

class GridcastApplication : Application() {
  companion object {
    lateinit var instance: GridcastApplication
      private set
  }

  override fun onCreate() {
    super.onCreate()
    instance = this
  }

  val database: GridcastDatabase by lazy { GridcastDatabase.getInstance(this) }

  val preferencesRepository: UserPreferencesRepository by lazy {
    UserPreferencesRepository(this)
  }

  val weatherRepository: WeatherRepository by lazy {
    WeatherRepository(
      context = applicationContext,
      stationDao = database.weatherStationDao(),
    )
  }
}
