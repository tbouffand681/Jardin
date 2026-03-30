package com.jardin.semis.data.db

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jardin.semis.data.model.Plant
import com.jardin.semis.data.model.Sowing
import com.jardin.semis.data.model.SowingStatus
import com.jardin.semis.data.model.NaturalEvent
import kotlinx.coroutines.flow.Flow

// ─── DAO Plantes ─────────────────────────────────────────────────────────────

@Dao
interface PlantDao {

    @Query("SELECT * FROM plants ORDER BY name ASC")
    fun getAllPlants(): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE category = :category ORDER BY name ASC")
    fun getPlantsByCategory(category: String): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getPlantById(id: Long): Plant?

    @Query("SELECT * FROM plants WHERE name LIKE '%' || :query || '%' OR latinName LIKE '%' || :query || '%'")
    fun searchPlants(query: String): Flow<List<Plant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant): Long

    /** N'insère que si le nom n'existe pas encore (évite les doublons au démarrage) */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlants(plants: List<Plant>)

    @Update
    suspend fun updatePlant(plant: Plant)

    @Delete
    suspend fun deletePlant(plant: Plant)

    @Query("SELECT DISTINCT category FROM plants ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM plants WHERE isDefault = 1")
    suspend fun countDefaultPlants(): Int
}

// ─── DAO Semis ────────────────────────────────────────────────────────────────

@Dao
interface SowingDao {

    @Query("""
        SELECT s.id, s.plantId, p.name as plantName, p.emoji as plantEmoji,
               s.sowingDate, s.expectedHarvestDate, s.soilFreeDate,
               s.location, s.quantity, s.status, s.notes,
               s.germinationDate, s.firstHarvestDate, s.reminderSet
        FROM sowings s
        INNER JOIN plants p ON s.plantId = p.id
        ORDER BY s.sowingDate DESC
    """)
    fun getAllSowingsWithPlant(): Flow<List<SowingWithPlant>>

    @Query("SELECT * FROM sowings WHERE id = :id")
    suspend fun getSowingById(id: Long): Sowing?

    @Query("SELECT * FROM sowings WHERE sowingDate BETWEEN :startDate AND :endDate ORDER BY sowingDate ASC")
    fun getSowingsBetweenDates(startDate: String, endDate: String): Flow<List<Sowing>>

    @Query("SELECT * FROM sowings WHERE status != 'HARVESTED' AND status != 'FAILED' ORDER BY sowingDate ASC")
    fun getActiveSowings(): Flow<List<Sowing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSowing(sowing: Sowing): Long

    @Update
    suspend fun updateSowing(sowing: Sowing)

    @Delete
    suspend fun deleteSowing(sowing: Sowing)

    @Query("UPDATE sowings SET status = :status WHERE id = :id")
    suspend fun updateSowingStatus(id: Long, status: SowingStatus)
}

// ─── DAO Journal naturel ──────────────────────────────────────────────────────

@Dao
interface NaturalEventDao {

    @Query("SELECT * FROM natural_events ORDER BY eventDate DESC")
    fun getAllEvents(): Flow<List<NaturalEvent>>

    @Query("SELECT * FROM natural_events WHERE category = :category ORDER BY eventDate DESC")
    fun getEventsByCategory(category: String): Flow<List<NaturalEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: NaturalEvent): Long

    @Update
    suspend fun updateEvent(event: NaturalEvent)

    @Delete
    suspend fun deleteEvent(event: NaturalEvent)

    @Query("SELECT DISTINCT category FROM natural_events ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
}

// ─── Data class jointure semis ────────────────────────────────────────────────

data class SowingWithPlant(
    val id: Long,
    val plantId: Long,
    val plantName: String,
    val plantEmoji: String,
    val sowingDate: String,
    val expectedHarvestDate: String,
    val soilFreeDate: String,
    val location: String,
    val quantity: Int,
    val status: SowingStatus,
    val notes: String,
    val germinationDate: String?,
    val firstHarvestDate: String?,
    val reminderSet: Boolean
)

// ─── Convertisseurs ───────────────────────────────────────────────────────────

class Converters {
    @TypeConverter
    fun fromSowingStatus(status: SowingStatus): String = status.name

    @TypeConverter
    fun toSowingStatus(value: String): SowingStatus = SowingStatus.valueOf(value)
}

// ─── Base de données ──────────────────────────────────────────────────────────

@Database(
    entities = [Plant::class, Sowing::class, NaturalEvent::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SemisDatabase : RoomDatabase() {

    abstract fun plantDao(): PlantDao
    abstract fun sowingDao(): SowingDao
    abstract fun naturalEventDao(): NaturalEventDao

    companion object {
        @Volatile
        private var INSTANCE: SemisDatabase? = null

        fun getDatabase(context: Context): SemisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SemisDatabase::class.java,
                    "semis_database"
                )
                    .fallbackToDestructiveMigration() // recrée la DB si version change
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
