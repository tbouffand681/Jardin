package com.jardin.semis.data.db

import com.jardin.semis.data.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object WeatherApiClient {

    suspend fun getWeatherByCity(city: String): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // Étape 1 : géocodage via Nominatim
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val geoUrl = "https://nominatim.openstreetmap.org/search?q=$encodedCity&format=json&limit=1&accept-language=fr"
            val geoJson = fetchJson(geoUrl, userAgent = "AlmanachDuJardin/1.0")
            val geoArray = JSONArray(geoJson)
            if (geoArray.length() == 0) return@withContext Result.failure(Exception("Ville non trouvée"))
            val geoResult = geoArray.getJSONObject(0)
            val lat = geoResult.getDouble("lat")
            val lon = geoResult.getDouble("lon")
            val cityName = geoResult.optString("display_name", city).split(",").first().trim()

            // Étape 2 : Open-Meteo — 5 jours pour le cumul ET₀
            val weatherUrl = buildString {
                append("https://api.open-meteo.com/v1/forecast?")
                append("latitude=$lat&longitude=$lon")
                append("&current=temperature_2m,relative_humidity_2m,apparent_temperature")
                append(",precipitation,wind_speed_10m,weather_code")
                append("&daily=temperature_2m_max,temperature_2m_min,precipitation_sum")
                append(",et0_fao_evapotranspiration,wind_speed_10m_max")
                append("&timezone=auto&forecast_days=5")  // 5 jours pour les cumuls
            }
            val weatherJson = fetchJson(weatherUrl)
            val root = JSONObject(weatherJson)
            val current = root.getJSONObject("current")
            val daily = root.getJSONObject("daily")

            val temp = current.getDouble("temperature_2m")
            val humidity = current.getInt("relative_humidity_2m")
            val feelsLike = current.getDouble("apparent_temperature")
            val wind = current.getDouble("wind_speed_10m")
            val precipitation = current.optDouble("precipitation", 0.0)
            val weatherCode = current.getInt("weather_code")

            val et0Arr = daily.getJSONArray("et0_fao_evapotranspiration")
            val tempMaxArr = daily.getJSONArray("temperature_2m_max")
            val tempMinArr = daily.getJSONArray("temperature_2m_min")
            val precipArr = daily.getJSONArray("precipitation_sum")

            val tempMax = tempMaxArr.getDouble(0)
            val tempMin = tempMinArr.getDouble(0)
            val et0Today = et0Arr.optDouble(0, 0.0)  // mm/jour = L/m²

            // Cumuls ET₀ sur 2 jours (aujourd'hui + demain) et 5 jours
            var et0Sum2 = 0.0
            var et0Sum5 = 0.0
            for (i in 0 until minOf(et0Arr.length(), 5)) {
                val v = et0Arr.optDouble(i, 0.0)
                if (i < 2) et0Sum2 += v
                et0Sum5 += v
            }

            // Cumul précipitations 5 jours
            var precipSum5 = 0.0
            for (i in 0 until minOf(precipArr.length(), 5)) precipSum5 += precipArr.optDouble(i, 0.0)

            Result.success(WeatherData(
                cityName = cityName,
                temperature = temp,
                feelsLike = feelsLike,
                humidity = humidity,
                description = weatherCodeToDescription(weatherCode),
                icon = weatherCodeToEmoji(weatherCode),
                windSpeed = wind,
                tempMin = tempMin,
                tempMax = tempMax,
                precipitation = precipitation,
                evapotranspiration = et0Today,
                et0Cumul2days = et0Sum2,
                et0Cumul5days = et0Sum5,
                precipCumul5days = precipSum5
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchJson(url: String, userAgent: String = "AlmanachDuJardin/1.0 Android"): String {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.setRequestProperty("User-Agent", userAgent)
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        return connection.inputStream.bufferedReader().readText()
    }

    private fun weatherCodeToDescription(code: Int) = when (code) {
        0 -> "Ciel dégagé"; 1, 2, 3 -> "Partiellement nuageux"
        45, 48 -> "Brouillard"; 51, 53, 55 -> "Bruine"
        61, 63, 65 -> "Pluie"; 71, 73, 75 -> "Neige"
        80, 81, 82 -> "Averses"; 85, 86 -> "Averses de neige"
        95 -> "Orage"; 96, 99 -> "Orage avec grêle"
        else -> "Conditions variables"
    }

    private fun weatherCodeToEmoji(code: Int) = when (code) {
        0 -> "☀️"; 1, 2 -> "🌤️"; 3 -> "☁️"
        45, 48 -> "🌫️"; 51, 53, 55, 61, 63, 65 -> "🌧️"
        71, 73, 75, 85, 86 -> "❄️"; 80, 81, 82 -> "🌦️"
        95, 96, 99 -> "⛈️"; else -> "🌡️"
    }
}
