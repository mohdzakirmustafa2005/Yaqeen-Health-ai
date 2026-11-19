package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_check_in_cache")
data class LocalCheckInEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateKey: String,
    val overallWellbeing: Long,
    val sleepHours: Double,
    val stressLevel: Long,
    val fatigueLevel: Long,
    val computedRiskLevel: String,
    val syncedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_ai_grounding_sources")
data class LocalGroundingSourceEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val userId: String,
    val messageId: String,
    val roleName: String,
    val title: String,
    val domain: String,
    val uri: String,
    val searchQuery: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_heart_measurement_cache")
data class LocalHeartMeasurementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val recordedAtIso: String,
    val restingHeartRateBpm: Long?,
    val systolicMmHg: Long?,
    val diastolicMmHg: Long?,
    val oxygenSaturationPct: Long?,
    val sourceLabel: String,
    val syncedAtMillis: Long = System.currentTimeMillis()
)

data class LocalSqlSummary(
    val totalCount: Int,
    val avgWellbeing: Double?,
    val avgSleep: Double?,
    val avgStress: Double?,
    val elevatedCount: Int,
    val heartReadingsCount: Int = 0
)

@Dao
interface LocalHealthDao {
    @Query("SELECT * FROM local_check_in_cache WHERE userId = :userId ORDER BY dateKey DESC, syncedAtMillis DESC LIMIT 60")
    fun observeCachedCheckIns(userId: String): Flow<List<LocalCheckInEntity>>

    @Query(
        """
        SELECT 
            COUNT(*) AS totalCount,
            AVG(overallWellbeing) AS avgWellbeing,
            AVG(sleepHours) AS avgSleep,
            AVG(stressLevel) AS avgStress,
            SUM(CASE WHEN computedRiskLevel IN ('ORANGE', 'RED') THEN 1 ELSE 0 END) AS elevatedCount,
            (SELECT COUNT(*) FROM local_heart_measurement_cache WHERE userId = :userId) AS heartReadingsCount
        FROM local_check_in_cache
        WHERE userId = :userId
        """
    )
    fun observeSqlSummary(userId: String): Flow<LocalSqlSummary?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckIns(items: List<LocalCheckInEntity>)

    @Query("DELETE FROM local_check_in_cache WHERE userId = :userId")
    suspend fun deleteCheckInsForUser(userId: String)

    @Query("SELECT * FROM local_ai_grounding_sources WHERE userId = :userId ORDER BY createdAtMillis ASC")
    fun observeGroundingSources(userId: String): Flow<List<LocalGroundingSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroundingSources(sources: List<LocalGroundingSourceEntity>)

    @Query("DELETE FROM local_ai_grounding_sources WHERE userId = :userId")
    suspend fun deleteGroundingSourcesForUser(userId: String)

    @Query("SELECT * FROM local_heart_measurement_cache WHERE userId = :userId ORDER BY recordedAtIso DESC LIMIT 60")
    fun observeHeartMeasurements(userId: String): Flow<List<LocalHeartMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHeartMeasurements(items: List<LocalHeartMeasurementEntity>)

    @Query("DELETE FROM local_heart_measurement_cache WHERE userId = :userId")
    suspend fun deleteHeartMeasurementsForUser(userId: String)
}

@Database(
    entities = [
        LocalCheckInEntity::class,
        LocalGroundingSourceEntity::class,
        LocalHeartMeasurementEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LocalHealthDatabase : RoomDatabase() {
    abstract fun localHealthDao(): LocalHealthDao

    companion object {
        @Volatile
        private var INSTANCE: LocalHealthDatabase? = null

        fun getInstance(context: Context): LocalHealthDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalHealthDatabase::class.java,
                    "yaqeen_health_local.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
