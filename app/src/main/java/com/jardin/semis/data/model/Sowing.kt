package com.jardin.semis.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val sowingDate: String,
    val expectedHarvestDate: String,
    val soilFreeDate: String,
    val location: String = "",
    val quantity: Int = 1,
    val status: SowingStatus = SowingStatus.SOWED,
    val notes: String = "",
    val germinationDate: String? = null,
    val firstHarvestDate: String? = null,
    val reminderSet: Boolean = false
)

enum class SowingStatus {
    SOWED,        // 🌰 Semé
    GERMINATED,   // 🌱 Levée
    TRANSPLANTED, // 🪴 Repiqué  ← NOUVEAU
    GROWING,      // 🌿 Croissance
    HARVESTED,    // ✅ Récolté
    FAILED        // ❌ Échec
}
