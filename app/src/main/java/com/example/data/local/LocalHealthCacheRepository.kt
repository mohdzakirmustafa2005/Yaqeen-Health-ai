package com.example.data.local

import com.example.data.GroundingSource
import com.example.model.HealthCheckIn
import com.example.model.HeartMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface LocalSqlStore {
    fun observeSqlSummary(userId: String): Flow<LocalSqlSummary?>
    fun observeGroundingSourcesByMessage(userId: String): Flow<Map<String, List<GroundingSource>>>
    suspend fun syncCheckIns(userId: String, checkIns: List<HealthCheckIn>)
    suspend fun syncHeartMeasurements(userId: String, measurements: List<HeartMeasurement>)
    suspend fun saveGroundingSources(
        userId: String,
        messageId: String,
        roleName: String,
        sources: List<GroundingSource>
    )
    suspend fun clearUserCache(userId: String)
}

/**
 * Room-backed local SQLite repository (strictly device-local cache & SQL aggregates alongside Cloud Firestore).
 */
class RoomLocalHealthCacheRepository(
    private val dao: LocalHealthDao
) : LocalSqlStore {

    override fun observeSqlSummary(userId: String): Flow<LocalSqlSummary?> =
        dao.observeSqlSummary(userId)

    override fun observeGroundingSourcesByMessage(userId: String): Flow<Map<String, List<GroundingSource>>> =
        dao.observeGroundingSources(userId).map { rows ->
            rows.groupBy { it.messageId }.mapValues { (_, list) ->
                list.map { row ->
                    GroundingSource(
                        title = row.title,
                        domain = row.domain,
                        uri = row.uri,
                        searchQuery = row.searchQuery
                    )
                }
            }
        }

    override suspend fun syncCheckIns(userId: String, checkIns: List<HealthCheckIn>) {
        if (checkIns.isEmpty()) return
        val entities = checkIns.mapIndexed { index, ci ->
            LocalCheckInEntity(
                id = ci.id.ifBlank { "${userId}_${ci.dateKey}_$index" },
                userId = userId,
                dateKey = ci.dateKey,
                overallWellbeing = ci.overallWellbeing,
                sleepHours = ci.sleepHours,
                stressLevel = ci.stressLevel,
                fatigueLevel = ci.fatigueLevel,
                computedRiskLevel = ci.computedRiskLevel
            )
        }
        dao.upsertCheckIns(entities)
    }

    override suspend fun syncHeartMeasurements(userId: String, measurements: List<HeartMeasurement>) {
        if (measurements.isEmpty()) return
        val entities = measurements.mapIndexed { index, m ->
            LocalHeartMeasurementEntity(
                id = m.id.ifBlank { "${userId}_hm_${m.recordedAtIso}_$index" },
                userId = userId,
                recordedAtIso = m.recordedAtIso,
                restingHeartRateBpm = m.restingHeartRateBpm,
                systolicMmHg = m.systolicMmHg,
                diastolicMmHg = m.diastolicMmHg,
                oxygenSaturationPct = m.oxygenSaturationPct,
                sourceLabel = m.sourceLabel
            )
        }
        dao.upsertHeartMeasurements(entities)
    }

    override suspend fun saveGroundingSources(
        userId: String,
        messageId: String,
        roleName: String,
        sources: List<GroundingSource>
    ) {
        if (sources.isEmpty() || messageId.isBlank()) return
        val entities = sources.map { src ->
            LocalGroundingSourceEntity(
                userId = userId,
                messageId = messageId,
                roleName = roleName,
                title = src.title.take(180),
                domain = src.domain.take(120),
                uri = src.uri.take(500),
                searchQuery = src.searchQuery.take(200)
            )
        }
        dao.insertGroundingSources(entities)
    }

    override suspend fun clearUserCache(userId: String) {
        dao.deleteCheckInsForUser(userId)
        dao.deleteHeartMeasurementsForUser(userId)
        dao.deleteGroundingSourcesForUser(userId)
    }
}

/**
 * In-memory implementation used safely in Demo Mode or unit tests when Context is unavailable.
 */
class InMemoryLocalSqlStore : LocalSqlStore {
    private val checkIns = MutableStateFlow<Map<String, List<HealthCheckIn>>>(emptyMap())
    private val heartMeasurements = MutableStateFlow<Map<String, List<HeartMeasurement>>>(emptyMap())
    private val sources = MutableStateFlow<Map<String, Map<String, List<GroundingSource>>>>(emptyMap())

    override fun observeSqlSummary(userId: String): Flow<LocalSqlSummary?> =
        combine(checkIns, heartMeasurements) { ciMap, hmMap ->
            val items = ciMap[userId].orEmpty()
            val heartCount = hmMap[userId].orEmpty().size
            if (items.isEmpty()) {
                LocalSqlSummary(0, null, null, null, 0, heartCount)
            } else {
                LocalSqlSummary(
                    totalCount = items.size,
                    avgWellbeing = items.map { it.overallWellbeing.toDouble() }.average(),
                    avgSleep = items.map { it.sleepHours }.average(),
                    avgStress = items.map { it.stressLevel.toDouble() }.average(),
                    elevatedCount = items.count { it.computedRiskLevel == "ORANGE" || it.computedRiskLevel == "RED" },
                    heartReadingsCount = heartCount
                )
            }
        }

    override fun observeGroundingSourcesByMessage(userId: String): Flow<Map<String, List<GroundingSource>>> =
        sources.asStateFlow().map { it[userId].orEmpty() }

    override suspend fun syncCheckIns(userId: String, checkIns: List<HealthCheckIn>) {
        this.checkIns.update { it + (userId to checkIns) }
    }

    override suspend fun syncHeartMeasurements(userId: String, measurements: List<HeartMeasurement>) {
        this.heartMeasurements.update { it + (userId to measurements) }
    }

    override suspend fun saveGroundingSources(
        userId: String,
        messageId: String,
        roleName: String,
        sources: List<GroundingSource>
    ) {
        if (sources.isEmpty()) return
        this.sources.update { current ->
            val userMap = current[userId].orEmpty() + (messageId to sources)
            current + (userId to userMap)
        }
    }

    override suspend fun clearUserCache(userId: String) {
        checkIns.update { it - userId }
        heartMeasurements.update { it - userId }
        sources.update { it - userId }
    }
}
