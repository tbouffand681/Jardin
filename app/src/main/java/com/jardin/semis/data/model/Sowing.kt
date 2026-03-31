package com.jardin.semis.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Représente un semis effectué par l'utilisateur.
 */
@Entity(
    tableName = "sowings",
    foreignKeys = [ForeignKey(
        entity = Plant::class,
        parentColumns = ["id"],
        childColumns = ["plantId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("plantId")]
)
data class Sowing(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Référence à la plante */
    val plantId: Long,

    /** Date de semis (stockée en ISO-8601 : "2024-03-15") */
    val sowingDate: String,

    /** Date de récolte prévue (calculée automatiquement) */
    val expectedHarvestDate: String,

    /** Date de fin d'occupation du sol */
    val soilFreeDate: String,

    /** Emplacement dans le jardin (ex: "Carré A", "Serre") */
    val location: String = "",

    /** Quantité semée (graines, plants...) */
    val quantity: Int = 1,

    /** Statut : SOWED, GERMINATED, GROWING, HARVESTED, FAILED */
    val status: SowingStatus = SowingStatus.SOWED,

    /** Notes de l'utilisateur */
    val notes: String = "",

    /** Date de levée constatée */
    val germinationDate: String? = null,

    /** Date de première récolte effective */
    val firstHarvestDate: String? = null,

    /** Notification de rappel programmée */
    val reminderSet: Boolean = false
)

enum class SowingStatus {
    SOWED,      // Semé
    GERMINATED, // Levée observée
    GROWING,    // En croissance
    HARVESTED,  // Récolté
    FAILED      // Échec
}
