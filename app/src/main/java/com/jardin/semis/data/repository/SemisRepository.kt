package com.jardin.semis.data.repository

import android.content.Context
import com.jardin.semis.BuildConfig
import com.jardin.semis.data.db.*
import com.jardin.semis.data.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SemisRepository(
    private val plantDao: PlantDao,
    private val sowingDao: SowingDao,
    private val naturalEventDao: NaturalEventDao,
    private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ─── Plantes ──────────────────────────────────────────────────────────────

    fun getAllPlants(): Flow<List<Plant>> = plantDao.getAllPlants()
    fun getPlantsByCategory(category: String): Flow<List<Plant>> = plantDao.getPlantsByCategory(category)
    fun searchPlants(query: String): Flow<List<Plant>> = plantDao.searchPlants(query)
    fun getAllCategories(): Flow<List<String>> = plantDao.getAllCategories()
    suspend fun getPlantById(id: Long): Plant? = plantDao.getPlantById(id)
    suspend fun insertPlant(plant: Plant): Long = plantDao.insertPlant(plant)
    suspend fun updatePlant(plant: Plant) = plantDao.updatePlant(plant)
    suspend fun deletePlant(plant: Plant) = plantDao.deletePlant(plant)

    // ─── Semis ────────────────────────────────────────────────────────────────

    fun getAllSowingsWithPlant(): Flow<List<SowingWithPlant>> = sowingDao.getAllSowingsWithPlant()
    fun getActiveSowings(): Flow<List<Sowing>> = sowingDao.getActiveSowings()
    fun getSowingsBetweenDates(start: LocalDate, end: LocalDate): Flow<List<Sowing>> =
        sowingDao.getSowingsBetweenDates(start.format(dateFormatter), end.format(dateFormatter))

    suspend fun addSowing(
        plantId: Long,
        sowingDate: LocalDate,
        location: String,
        quantity: Int,
        notes: String
    ): Long {
        val plant = plantDao.getPlantById(plantId)
            ?: throw IllegalArgumentException("Plante introuvable (id=$plantId)")

        val harvestDate = sowingDate.plusDays(plant.occupationDays.toLong())
        val soilFreeDate = harvestDate.plusDays(7)

        val sowing = Sowing(
            plantId = plantId,
            sowingDate = sowingDate.format(dateFormatter),
            expectedHarvestDate = harvestDate.format(dateFormatter),
            soilFreeDate = soilFreeDate.format(dateFormatter),
            location = location,
            quantity = quantity,
            notes = notes
        )
        return sowingDao.insertSowing(sowing)
    }

    suspend fun updateSowingStatus(id: Long, status: SowingStatus) =
        sowingDao.updateSowingStatus(id, status)

    suspend fun deleteSowing(sowing: Sowing) = sowingDao.deleteSowing(sowing)

    // ─── Journal naturel ──────────────────────────────────────────────────────

    fun getAllNaturalEvents(): Flow<List<NaturalEvent>> = naturalEventDao.getAllEvents()
    fun getNaturalEventsByCategory(cat: String): Flow<List<NaturalEvent>> = naturalEventDao.getEventsByCategory(cat)
    fun getNaturalEventCategories(): Flow<List<String>> = naturalEventDao.getAllCategories()

    suspend fun addNaturalEvent(event: NaturalEvent): Long = naturalEventDao.insertEvent(event)
    suspend fun updateNaturalEvent(event: NaturalEvent) = naturalEventDao.updateEvent(event)
    suspend fun deleteNaturalEvent(event: NaturalEvent) = naturalEventDao.deleteEvent(event)

    // ─── Météo ────────────────────────────────────────────────────────────────

    suspend fun getWeatherByCity(city: String): Result<WeatherData> {
        return try {
            val response = WeatherApiClient.service.getCurrentWeatherByCity(
                city = city,
                apiKey = BuildConfig.WEATHER_API_KEY
            )
            Result.success(response.toWeatherData())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.jardin.semis.data.model.WeatherApiResponse.toWeatherData() = WeatherData(
        cityName = name,
        temperature = main.temp,
        feelsLike = main.feels_like,
        humidity = main.humidity,
        description = weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "",
        icon = weather.firstOrNull()?.icon ?: "",
        windSpeed = wind.speed,
        tempMin = main.temp_min,
        tempMax = main.temp_max
    )

    // ─── Initialisation des plantes par défaut (une seule fois) ──────────────

    suspend fun populateDefaultPlants() {
        // Vérifie si les plantes par défaut existent déjà → évite les doublons
        if (plantDao.countDefaultPlants() > 0) return

        val defaultPlants = listOf(
            Plant(name = "Tomate", latinName = "Solanum lycopersicum", category = "Légume fruit",
                emoji = "🍅", sowingMonths = "2,3,4", occupationDays = 150,
                sowingDepthCm = 0.5f, spacingCm = 60, sunExposure = "Plein soleil",
                waterNeeds = "Élevé", germinationDays = 7, germinationTempMin = 18,
                germinationTempMax = 25, isDefault = true,
                notes = "Semer sous abri chauffé. Repiquer après les Saints de Glace."),
            Plant(name = "Courgette", latinName = "Cucurbita pepo", category = "Légume fruit",
                emoji = "🥒", sowingMonths = "4,5", occupationDays = 120,
                sowingDepthCm = 2f, spacingCm = 80, sunExposure = "Plein soleil",
                waterNeeds = "Élevé", germinationDays = 5, germinationTempMin = 18,
                germinationTempMax = 28, isDefault = true,
                notes = "Croissance rapide. Récolter régulièrement."),
            Plant(name = "Carotte", latinName = "Daucus carota", category = "Légume racine",
                emoji = "🥕", sowingMonths = "3,4,5,6,7", occupationDays = 90,
                sowingDepthCm = 1f, spacingCm = 5, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 14, germinationTempMin = 8,
                germinationTempMax = 20, isDefault = true,
                notes = "Ameublir le sol en profondeur. Éclaircir à 5 cm."),
            Plant(name = "Laitue", latinName = "Lactuca sativa", category = "Salade",
                emoji = "🥬", sowingMonths = "2,3,4,5,6,7,8", occupationDays = 60,
                sowingDepthCm = 0.5f, spacingCm = 25, sunExposure = "Mi-ombre",
                waterNeeds = "Moyen", germinationDays = 5, germinationTempMin = 10,
                germinationTempMax = 20, isDefault = true,
                notes = "Éviter la chaleur excessive."),
            Plant(name = "Haricot vert", latinName = "Phaseolus vulgaris", category = "Légumineuse",
                emoji = "🫘", sowingMonths = "5,6,7", occupationDays = 75,
                sowingDepthCm = 3f, spacingCm = 15, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 8, germinationTempMin = 15,
                germinationTempMax = 25, isDefault = true,
                notes = "Ne pas semer avant que le sol soit à 15°C minimum."),
            Plant(name = "Basilic", latinName = "Ocimum basilicum", category = "Aromate",
                emoji = "🌿", sowingMonths = "3,4,5", occupationDays = 120,
                sowingDepthCm = 0.3f, spacingCm = 20, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 8, germinationTempMin = 18,
                germinationTempMax = 25, isDefault = true,
                notes = "Sensible au froid. Pincer les fleurs."),
            Plant(name = "Radis", latinName = "Raphanus sativus", category = "Légume racine",
                emoji = "🔴", sowingMonths = "2,3,4,5,8,9,10", occupationDays = 30,
                sowingDepthCm = 1f, spacingCm = 5, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 3, germinationTempMin = 8,
                germinationTempMax = 18, isDefault = true,
                notes = "Culture rapide. Idéal en intercalaire."),
            Plant(name = "Poireau", latinName = "Allium porrum", category = "Légume",
                emoji = "🧅", sowingMonths = "2,3,4", occupationDays = 180,
                sowingDepthCm = 1f, spacingCm = 15, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 12, germinationTempMin = 10,
                germinationTempMax = 18, isDefault = true,
                notes = "Repiquer quand les plants ont la taille d'un crayon."),
            Plant(name = "Concombre", latinName = "Cucumis sativus", category = "Légume fruit",
                emoji = "🥒", sowingMonths = "4,5", occupationDays = 100,
                sowingDepthCm = 2f, spacingCm = 50, sunExposure = "Plein soleil",
                waterNeeds = "Élevé", germinationDays = 5, germinationTempMin = 20,
                germinationTempMax = 28, isDefault = true,
                notes = "Nécessite chaleur et humidité. Palissage recommandé."),
            Plant(name = "Épinard", latinName = "Spinacia oleracea", category = "Légume feuille",
                emoji = "🌿", sowingMonths = "2,3,9,10", occupationDays = 60,
                sowingDepthCm = 2f, spacingCm = 10, sunExposure = "Mi-ombre",
                waterNeeds = "Moyen", germinationDays = 10, germinationTempMin = 5,
                germinationTempMax = 18, isDefault = true,
                notes = "Monte en graine à la chaleur."),
            Plant(name = "Persil", latinName = "Petroselinum crispum", category = "Aromate",
                emoji = "🌿", sowingMonths = "3,4,5,8,9", occupationDays = 365,
                sowingDepthCm = 0.5f, spacingCm = 15, sunExposure = "Mi-ombre",
                waterNeeds = "Moyen", germinationDays = 21, germinationTempMin = 10,
                germinationTempMax = 20, isDefault = true,
                notes = "Tremper les graines 24h avant semis."),
            Plant(name = "Poivron", latinName = "Capsicum annuum", category = "Légume fruit",
                emoji = "🫑", sowingMonths = "2,3", occupationDays = 150,
                sowingDepthCm = 0.5f, spacingCm = 50, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 14, germinationTempMin = 20,
                germinationTempMax = 28, isDefault = true,
                notes = "Démarrage sous abri chauffé nécessaire."),
            Plant(name = "Betterave", latinName = "Beta vulgaris", category = "Légume racine",
                emoji = "🟣", sowingMonths = "4,5,6", occupationDays = 90,
                sowingDepthCm = 2f, spacingCm = 10, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 10, germinationTempMin = 10,
                germinationTempMax = 20, isDefault = true,
                notes = "Éclaircir à 10 cm. Feuilles comestibles."),
            Plant(name = "Potiron", latinName = "Cucurbita maxima", category = "Légume fruit",
                emoji = "🎃", sowingMonths = "4,5", occupationDays = 150,
                sowingDepthCm = 2f, spacingCm = 150, sunExposure = "Plein soleil",
                waterNeeds = "Moyen", germinationDays = 7, germinationTempMin = 18,
                germinationTempMax = 25, isDefault = true,
                notes = "Plante très volumineuse."),
            Plant(name = "Ciboulette", latinName = "Allium schoenoprasum", category = "Aromate",
                emoji = "🌱", sowingMonths = "3,4,5", occupationDays = 365,
                sowingDepthCm = 0.5f, spacingCm = 20, sunExposure = "Plein soleil",
                waterNeeds = "Faible", germinationDays = 14, germinationTempMin = 10,
                germinationTempMax = 20, isDefault = true,
                notes = "Vivace. Diviser les touffes tous les 2-3 ans.")
        )
        plantDao.insertPlants(defaultPlants)
    }
}
