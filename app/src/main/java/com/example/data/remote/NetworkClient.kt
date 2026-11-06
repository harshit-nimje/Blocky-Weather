package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkClient {
  val moshi: Moshi by lazy { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }

  private val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .writeTimeout(10, TimeUnit.SECONDS)
      .build()
  }

  val forecastService: OpenMeteoForecastService by lazy {
    Retrofit.Builder()
      .baseUrl("https://api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoForecastService::class.java)
  }

  val geocodingService: OpenMeteoGeocodingService by lazy {
    Retrofit.Builder()
      .baseUrl("https://geocoding-api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoGeocodingService::class.java)
  }

  val airQualityService: OpenMeteoAirQualityService by lazy {
    Retrofit.Builder()
      .baseUrl("https://air-quality-api.open-meteo.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(OpenMeteoAirQualityService::class.java)
  }

  val wikiSummaryService: WikipediaSummaryService by lazy {
    Retrofit.Builder()
      .baseUrl("https://en.wikipedia.org/api/rest_v1/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(WikipediaSummaryService::class.java)
  }
}
