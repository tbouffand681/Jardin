package com.jardin.semis.data.model

data class WeatherData(
    val cityName: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val description: String,
    val icon: String,          // emoji météo
    val windSpeed: Double,
    val tempMin: Double,
    val tempMax: Double,
    val precipitation: Double = 0.0,          // mm
    val evapotranspiration: Double = 0.0      // mm/jour (ET₀ FAO-56)
) {
    /** Conseil de semis adapté à la météo */
    fun sowingAdvice(): String = when {
        temperature < 3  -> "❄️ Gel possible — aucun semis en pleine terre. Protégez vos cultures."
        temperature < 8  -> "🥶 Trop froid pour la plupart des semis. Réservez la serre."
        temperature in 8.0..12.0 -> "🌡️ Frais. Semis d'espèces rustiques (épinard, mâche, radis)."
        temperature in 12.0..17.0 -> "✅ Bonnes conditions pour légumes printaniers (carotte, laitue, poireau)."
        temperature in 17.0..27.0 -> "🌞 Conditions idéales pour la plupart des semis."
        temperature > 32 -> "🔥 Trop chaud — semez tôt le matin et arrosez abondamment."
        else -> "✅ Conditions favorables."
    }

    /** Conseil d'arrosage basé sur l'évapotranspiration */
    fun irrigationAdvice(): String = when {
        precipitation >= evapotranspiration -> "💧 Pluie suffisante aujourd'hui — arrosage non nécessaire."
        evapotranspiration < 2.0 -> "💧 Évapotranspiration faible — arrosage léger si sol sec."
        evapotranspiration < 4.0 -> "💧💧 Arrosez modérément (environ ${String.format("%.1f", evapotranspiration)} mm conseillés)."
        else -> "💧💧💧 Forte évaporation — arrosage important nécessaire (${String.format("%.1f", evapotranspiration)} mm)."
    }
}
