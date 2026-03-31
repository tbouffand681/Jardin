package com.jardin.semis.data.model

/**
 * Données météo actuelles retournées par OpenWeatherMap.
 */
data class WeatherData(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val description: String,
    val icon: String,
    val windSpeed: Double,
    val tempMin: Double,
    val tempMax: Double
) {
    /** Recommandation pour les semis selon la météo */
    fun sowingAdvice(): String = when {
        temperature < 5 -> "❄️ Trop froid pour semer en pleine terre. Privilégiez la serre."
        temperature in 5.0..10.0 -> "🌡️ Frais. Semis sous abri conseillé."
        temperature in 10.0..15.0 -> "✅ Conditions acceptables pour semis d'espèces rustiques."
        temperature in 15.0..25.0 -> "🌞 Conditions idéales pour la plupart des semis."
        temperature > 30 -> "🔥 Trop chaud. Semez le matin tôt et arrosez abondamment."
        else -> "✅ Conditions favorables."
    }
}

/**
 * Réponse brute de l'API OpenWeatherMap.
 */
data class WeatherApiResponse(
    val name: String,
    val main: MainData,
    val weather: List<WeatherDescription>,
    val wind: WindData
)

data class MainData(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val humidity: Int
)

data class WeatherDescription(
    val description: String,
    val icon: String
)

data class WindData(
    val speed: Double
)
