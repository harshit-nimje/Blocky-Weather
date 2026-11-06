package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WeatherStationEntity::class], version = 1, exportSchema = false)
abstract class GridcastDatabase : RoomDatabase() {
  abstract fun weatherStationDao(): WeatherStationDao

  companion object {
    @Volatile private var instance: GridcastDatabase? = null

    fun getInstance(context: Context): GridcastDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                GridcastDatabase::class.java,
                "gridcast_weather_cache.db",
              )
              .build()
              .also { instance = it }
        }
  }
}
