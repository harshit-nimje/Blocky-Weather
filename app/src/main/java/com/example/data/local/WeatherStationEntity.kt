package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_stations")
data class WeatherStationEntity(
  @PrimaryKey val id: String,
  val name: String,
  val admin1: String?,
  val country: String,
  val latitude: Double,
  val longitude: Double,
  val cachedForecastJson: String?,
  val lastTempC: Double?,
  val lastConditionName: String?,
  val lastUpdatedMillis: Long?,
  val isPinned: Boolean,
  val sortOrder: Int,
)
