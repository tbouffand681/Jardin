package com.jardin.semis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jardin.semis.data.model.*
import com.jardin.semis.data.repository.SemisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class SemisApplication : Application() {
    val database by lazy { com.jardin.semis.data.db.SemisDatabase.getDatabase(this) }
    val repository by lazy {
        SemisRepository(database.plantDao(), database.sowingDao(), database.naturalEventDao(), this)
    }
    override fun onCreate() { super.onCreate() }
}

class SemisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SemisApplication).repository

    init {
        viewModelScope.launch(Dispatchers.IO) { repository.populateDefaultPlants() }
    }

    val allPlants = repository.getAllPlants()
    val allCategories = repository.getAllCategories()
    val allSowingsWithPlant = repository.getAllSowingsWithPlant()
    val activeSowings = repository.getActiveSowings()
    val allNaturalEvents = repository.getAllNaturalEvents()
    val naturalEventCategories = repository.getNaturalEventCategories()

    private val _weatherData = MutableLiveData<WeatherData?>()
    val weatherData: LiveData<WeatherData?> = _weatherData
    private val _weatherLoading = MutableLiveData(false)
    val weatherLoading: LiveData<Boolean> = _weatherLoading
    private val _weatherError = MutableLiveData<String?>()
    val weatherError: LiveData<String?> = _weatherError
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    // ─── Plantes ──────────────────────────────────────────────────────────────

    /** Retourne true si succès — le dialog peut dismiss sur true */
    fun addPlant(plant: Plant, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        try {
            repository.insertPlant(plant)
            _message.postValue("${plant.emoji} ${plant.name} ajouté")
            onDone(true)
        } catch (e: Exception) {
            _message.postValue("Erreur : ${e.message}")
            onDone(false)
        }
    }

    fun updatePlant(plant: Plant, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        try {
            repository.updatePlant(plant)
            _message.postValue("Plante modifiée")
            onDone(true)
        } catch (e: Exception) {
            _message.postValue("Erreur : ${e.message}")
            onDone(false)
        }
    }

    fun deletePlant(plant: Plant, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.deletePlant(plant)
        _message.postValue("${plant.name} supprimé")
        onDone()
    }

    fun searchPlants(query: String) = repository.searchPlants(query)
    fun getPlantsByCategory(cat: String) = repository.getPlantsByCategory(cat)

    // ─── Semis ────────────────────────────────────────────────────────────────

    fun addSowing(plantId: Long, sowingDate: LocalDate, location: String, quantity: Int, notes: String,
                  onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
        try {
            repository.addSowing(plantId, sowingDate, location, quantity, notes)
            _message.postValue("✅ Semis enregistré !")
            onDone(true)
        } catch (e: Exception) {
            _message.postValue("❌ Erreur : ${e.message}")
            onDone(false)
        }
    }

    fun updateSowingStatus(id: Long, status: SowingStatus) = viewModelScope.launch {
        repository.updateSowingStatus(id, status)
        _message.postValue(when (status) {
            SowingStatus.GERMINATED    -> "Levée enregistrée 🌱"
            SowingStatus.TRANSPLANTED  -> "Repiquage enregistré 🪴"
            SowingStatus.GROWING       -> "En croissance 🌿"
            SowingStatus.HARVESTED     -> "Récolte enregistrée 🎉"
            SowingStatus.FAILED        -> "Semis marqué comme échoué"
            else -> "Statut mis à jour"
        })
    }

    fun deleteSowingById(id: Long) = viewModelScope.launch {
        repository.deleteSowingById(id)
        _message.postValue("Semis supprimé")
    }

    fun deleteSowing(sowing: Sowing) = viewModelScope.launch {
        repository.deleteSowing(sowing)
        _message.postValue("Semis supprimé")
    }

    fun getSowingsBetweenDates(start: LocalDate, end: LocalDate) =
        repository.getSowingsBetweenDates(start, end)

    // ─── Journal naturel ──────────────────────────────────────────────────────

    fun addNaturalEvent(event: NaturalEvent, onDone: () -> Unit = {}) = viewModelScope.launch {
        repository.addNaturalEvent(event)
        _message.postValue("${event.emoji} Observation enregistrée")
        onDone()
    }

    fun deleteNaturalEvent(event: NaturalEvent) = viewModelScope.launch {
        repository.deleteNaturalEvent(event)
        _message.postValue("Observation supprimée")
    }

    // ─── Météo Open-Meteo (sans clé API) ──────────────────────────────────────

    fun fetchWeatherByCity(city: String) = viewModelScope.launch {
        _weatherLoading.postValue(true)
        _weatherError.postValue(null)
        repository.getWeatherByCity(city).fold(
            onSuccess = { _weatherData.postValue(it); _weatherLoading.postValue(false) },
            onFailure = { e ->
                _weatherError.postValue("Ville introuvable. Vérifiez l'orthographe (ex: Lyon, Bordeaux).")
                _weatherLoading.postValue(false)
            }
        )
    }

    fun clearMessage() { _message.value = null }
}
