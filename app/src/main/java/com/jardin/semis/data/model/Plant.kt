package com.jardin.semis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Représente une plante/légume dans la bibliothèque.
 */
@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Nom commun (ex: "Tomate") */
    val name: String,

    /** Nom latin */
    val latinName: String = "",

    /** Catégorie (légume, aromate, fleur...) */
    val category: String = "Légume",

    /** Emoji ou icône représentative */
    val emoji: String = "🌱",

    /** Mois de semis conseillés (liste séparée par virgules, ex: "2,3,4") */
    val sowingMonths: String = "",

    /** Durée d'occupation du sol en jours */
    val occupationDays: Int = 90,

    /** Profondeur de semis en cm */
    val sowingDepthCm: Float = 1f,

    /** Espacement entre plants en cm */
    val spacingCm: Int = 30,

    /** Exposition (plein soleil, mi-ombre, ombre) */
    val sunExposure: String = "Plein soleil",

    /** Besoin en eau (faible, moyen, élevé) */
    val waterNeeds: String = "Moyen",

    /** Notes et conseils de culture */
    val notes: String = "",

    /** Durée de germination en jours */
    val germinationDays: Int = 10,

    /** Température de germination min (°C) */
    val germinationTempMin: Int = 15,

    /** Température de germination max (°C) */
    val germinationTempMax: Int = 25,

    /** Plante prédéfinie (ne pas supprimer) */
    val isDefault: Boolean = false,

    /** URL d'une image illustrative */
    val imageUrl: String = ""
)
