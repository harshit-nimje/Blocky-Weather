package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherStationDao {
  @Query("SELECT * FROM weather_stations ORDER BY sortOrder ASC, lastUpdatedMillis DESC")
  fun observeAllStations(): Flow<List<WeatherStationEntity>>

  @Query("SELECT * FROM weather_stations WHERE id = :id LIMIT 1")
  fun observeStationById(id: String): Flow<WeatherStationEntity?>

  @Query("SELECT * FROM weather_stations WHERE id = :id LIMIT 1")
  suspend fun getStationById(id: String): WeatherStationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertStation(station: WeatherStationEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertIgnoreAll(stations: List<WeatherStationEntity>)

  @Query("DELETE FROM weather_stations WHERE id = :id")
  suspend fun deleteStationById(id: String)

  @Query("SELECT COUNT(*) FROM weather_stations")
  suspend fun countStations(): Int
}
