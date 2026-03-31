package com.jardin.semis.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Événement naturel observé au jardin.
 * Ex : floraison, retour d'un oiseau, observation d'un animal...
 */
@Entity(tableName = "natural_events")
data class NaturalEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Date d'observation (ISO-8601 : "2024-03-15") */
    val eventDate: String,

    /** Titre court (ex: "Première hirondelle") */
    val title: String,

    /** Catégorie : Flore, Faune, Météo, Autre */
    val category: String = "Faune",

    /** Emoji représentatif */
    val emoji: String = "🌿",

    /** Description détaillée */
    val description: String = "",

    /** Lieu d'observation */
    val location: String = ""
)
