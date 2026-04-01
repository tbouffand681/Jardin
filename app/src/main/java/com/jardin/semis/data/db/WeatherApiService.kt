package com.jardin.semis.data.db

import com.jardin.semis.data.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Service météo utilisant :
 * 1. Nominatim (OpenStreetMap) pour géocoder la ville → lat/lon
 * 2. Open-Meteo pour les données météo agricoles (GRATUIT, sans clé API)
 */
object WeatherApiClient {

    suspend fun getWeatherByCity(city: String): Result<WeatherData> = withContext(Dispatchers.IO) {
        try {
            // Étape 1 : géocodage de la ville
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val geoUrl = "https://nominatim.openstreetmap.org/search?q=$encodedCity&format=json&limit=1&accept-language=fr"
            val geoJson = fetchJson(geoUrl, userAgent = "AlmanachDuJardin/1.0")

            val geoArray = JSONArray(geoJson)
            if (geoArray.length() == 0) return@withContext Result.failure(Exception("Ville non trouvée"))

            val geoResult = geoArray.getJSONObject(0)
            val lat = geoResult.getDouble("lat")
            val lon = geoResult.getDouble("lon")
            val cityName = geoResult.optString("display_name", city).split(",").first().trim()

            // Étape 2 : météo Open-Meteo avec variables agricoles
            val weatherUrl = buildString {
                append("https://api.open-meteo.com/v1/forecast?")
                append("latitude=$lat&longitude=$lon")
                append("&current=temperature_2m,relative_humidity_2m,apparent_temperature")
                append(",precipitation,wind_speed_10m,weather_code")
                append("&daily=temperature_2m_max,temperature_2m_min,precipitation_sum")
                append(",et0_fao_evapotranspiration,wind_speed_10m_max")
                append("&timezone=auto&forecast_days=1")
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

            val dailyMaxArr = daily.getJSONArray("temperature_2m_max")
            val dailyMinArr = daily.getJSONArray("temperature_2m_min")
            val et0Arr = daily.getJSONArray("et0_fao_evapotranspiration")
            val tempMax = dailyMaxArr.getDouble(0)
            val tempMin = dailyMinArr.getDouble(0)
            val et0 = et0Arr.optDouble(0, 0.0)  // Évapotranspiration (mm/jour)

            val description = weatherCodeToDescription(weatherCode)
            val icon = weatherCodeToEmoji(weatherCode)

            Result.success(WeatherData(
                cityName = cityName,
                temperature = temp,
                feelsLike = feelsLike,
                humidity = humidity,
                description = description,
                icon = icon,
                windSpeed = wind,
                tempMin = tempMin,
                tempMax = tempMax,
                precipitation = precipitation,
                evapotranspiration = et0
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

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0 -> "Ciel dégagé"
        1, 2, 3 -> "Partiellement nuageux"
        45, 48 -> "Brouillard"
        51, 53, 55 -> "Bruine"
        61, 63, 65 -> "Pluie"
        71, 73, 75 -> "Neige"
        80, 81, 82 -> "Averses"
        85, 86 -> "Averses de neige"
        95 -> "Orage"
        96, 99 -> "Orage avec grêle"
        else -> "Conditions variables"
    }

    private fun weatherCodeToEmoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "🌤️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 61, 63, 65 -> "🌧️"
        71, 73, 75, 85, 86 -> "❄️"
        80, 81, 82 -> "🌦️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
}
