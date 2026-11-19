package com.example.data

import android.content.Context
import com.example.R
import com.example.engine.DemoDataFactory
import com.example.model.AiChatMessage
import com.example.model.AiDoctorMode
import com.example.model.AiHealthInsightRecord
import com.example.model.AlertStatus
import com.example.model.AuditLogEntry
import com.example.model.BloodPressureRecord
import com.example.model.DoctorAiMessage
import com.example.model.DoctorConversation
import com.example.model.HealthCheckIn
import com.example.model.HealthReportUpload
import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.HeartSymptomReport
import com.example.model.MedicalSafetyEvent
import com.example.model.RiskAlert
import com.example.model.SmartNotification
import com.example.model.UserConsentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.WellnessRecommendation
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import java.util.UUID

interface HealthRepository {
    fun observeProfile(userId: String): Flow<UserProfile?>
    fun observeCheckIns(userId: String): Flow<List<HealthCheckIn>>
    fun observeAlerts(userId: String): Flow<List<RiskAlert>>
    fun observeRecommendations(userId: String): Flow<List<WellnessRecommendation>>
    fun observeAiMessages(userId: String): Flow<List<AiChatMessage>>
    fun observeNotifications(userId: String): Flow<List<SmartNotification>>
    fun observeAuditLogs(userId: String): Flow<List<AuditLogEntry>>

    suspend fun ensureUserProfile(userId: String, defaultName: String): Result<Unit>
    suspend fun updateProfilePreferences(
        userId: String,
        displayName: String,
        department: String,
        language: String,
        consentAnalytics: Boolean,
        consentAiGuidance: Boolean,
        consentCoordinatorOutreach: Boolean,
        remindersEnabled: Boolean,
        reminderHour: Long,
        reminderMinute: Long,
        onboardingCompleted: Boolean
    ): Result<Unit>

    suspend fun saveCheckIn(userId: String, checkIn: HealthCheckIn): Result<String>
    suspend fun updateCheckInAiSummary(userId: String, checkInId: String, aiSummary: String): Result<Unit>
    suspend fun saveAlert(userId: String, alert: RiskAlert): Result<Unit>
    suspend fun updateAlertStatus(userId: String, alertId: String, status: AlertStatus): Result<Unit>
    suspend fun saveRecommendations(userId: String, recs: List<WellnessRecommendation>): Result<Unit>
    suspend fun toggleRecommendation(userId: String, recId: String, completed: Boolean): Result<Unit>
    suspend fun saveAiMessage(userId: String, message: AiChatMessage): Result<String>
    suspend fun saveNotification(userId: String, notif: SmartNotification): Result<Unit>
    suspend fun markNotificationRead(userId: String, notifId: String): Result<Unit>
    suspend fun appendAuditLog(userId: String, action: String, details: String, actorRole: String): Result<Unit>
    suspend fun deleteAccountData(userId: String): Result<Unit>

    // --- AI Doctor & AI Heart Specialist Clinical Collections ---
    fun observeDoctorConversations(userId: String): Flow<List<DoctorConversation>>
    fun observeDoctorMessages(userId: String): Flow<List<DoctorAiMessage>>
    fun observeHeartProfile(userId: String): Flow<HeartHealthProfile?>
    fun observeHeartMeasurements(userId: String): Flow<List<HeartMeasurement>>
    fun observeBloodPressureRecords(userId: String): Flow<List<BloodPressureRecord>>
    fun observeHeartSymptomReports(userId: String): Flow<List<HeartSymptomReport>>
    fun observeHealthReportUploads(userId: String): Flow<List<HealthReportUpload>>
    fun observeAiInsights(userId: String): Flow<List<AiHealthInsightRecord>>
    fun observeSafetyEvents(userId: String): Flow<List<MedicalSafetyEvent>>
    fun observeConsentRecords(userId: String): Flow<List<UserConsentRecord>>

    suspend fun createOrUpdateConversation(userId: String, conversation: DoctorConversation): Result<String>
    suspend fun deleteConversationAndMessages(userId: String, conversationId: String): Result<Unit>
    suspend fun saveDoctorMessage(userId: String, message: DoctorAiMessage): Result<String>
    suspend fun updateDoctorMessageFeedback(userId: String, messageId: String, feedback: Long): Result<Unit>
    suspend fun saveHeartProfile(userId: String, profile: HeartHealthProfile): Result<Unit>
    suspend fun saveHeartMeasurement(userId: String, measurement: HeartMeasurement): Result<String>
    suspend fun saveBloodPressureRecord(userId: String, record: BloodPressureRecord): Result<String>
    suspend fun saveHeartSymptomReport(userId: String, report: HeartSymptomReport): Result<String>
    suspend fun saveHealthReportUpload(userId: String, upload: HealthReportUpload): Result<String>
    suspend fun saveAiInsight(userId: String, insight: AiHealthInsightRecord): Result<String>
    suspend fun saveSafetyEvent(userId: String, event: MedicalSafetyEvent): Result<Unit>
    suspend fun saveConsentRecord(userId: String, record: UserConsentRecord): Result<Unit>
}

class FirestoreHealthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : HealthRepository {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = FirebaseAuth.getInstance()
    )

    private fun requireUserId(expectedUid: String): String {
        val current = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(current == expectedUid) { "Authenticated user mismatch; refusing cross-user access." }
        return current
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    override fun observeProfile(userId: String): Flow<UserProfile?> =
        userDoc(userId).snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error -> logFlowError(error, OperationType.GET, "users/$userId", userId); throw error }

    override fun observeCheckIns(userId: String): Flow<List<HealthCheckIn>> =
        userDoc(userId).collection("checkIns")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(HealthCheckIn::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/checkIns", userId); throw error }

    override fun observeAlerts(userId: String): Flow<List<RiskAlert>> =
        userDoc(userId).collection("alerts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snap -> snap.toObjects(RiskAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/alerts", userId); throw error }

    override fun observeRecommendations(userId: String): Flow<List<WellnessRecommendation>> =
        userDoc(userId).collection("recommendations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snap -> snap.toObjects(WellnessRecommendation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/recommendations", userId); throw error }

    override fun observeAiMessages(userId: String): Flow<List<AiChatMessage>> =
        userDoc(userId).collection("aiMessages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(60)
            .snapshots()
            .map { snap -> snap.toObjects(AiChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/aiMessages", userId); throw error }

    override fun observeNotifications(userId: String): Flow<List<SmartNotification>> =
        userDoc(userId).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(SmartNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/notifications", userId); throw error }

    override fun observeAuditLogs(userId: String): Flow<List<AuditLogEntry>> =
        userDoc(userId).collection("auditLogs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(AuditLogEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/auditLogs", userId); throw error }

    override suspend fun ensureUserProfile(userId: String, defaultName: String): Result<Unit> =
        safeWrite(OperationType.CREATE, "users/$userId") {
            val uid = requireUserId(userId)
            val snap = userDoc(uid).get().await()
            if (!snap.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = defaultName.ifBlank { "Team Member" },
                    onboardingCompleted = false
                )
                userDoc(uid).set(profile.toCreateMap(uid)).await()
            }
        }

    override suspend fun updateProfilePreferences(
        userId: String,
        displayName: String,
        department: String,
        language: String,
        consentAnalytics: Boolean,
        consentAiGuidance: Boolean,
        consentCoordinatorOutreach: Boolean,
        remindersEnabled: Boolean,
        reminderHour: Long,
        reminderMinute: Long,
        onboardingCompleted: Boolean
    ): Result<Unit> = safeWrite(OperationType.UPDATE, "users/$userId") {
        val uid = requireUserId(userId)
        userDoc(uid).update(
            mapOf(
                "displayName" to displayName.trim().ifBlank { "Team Member" }.take(80),
                "department" to department.trim().ifBlank { "Operations" }.take(60),
                "language" to language,
                "consentAnalytics" to consentAnalytics,
                "consentAiGuidance" to consentAiGuidance,
                "consentCoordinatorOutreach" to consentCoordinatorOutreach,
                "remindersEnabled" to remindersEnabled,
                "reminderHour" to reminderHour.coerceIn(0L, 23L),
                "reminderMinute" to reminderMinute.coerceIn(0L, 59L),
                "onboardingCompleted" to onboardingCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    override suspend fun saveCheckIn(userId: String, checkIn: HealthCheckIn): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/checkIns") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("checkIns").document()
            docRef.set(checkIn.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun updateCheckInAiSummary(userId: String, checkInId: String, aiSummary: String): Result<Unit> =
        safeWrite(OperationType.UPDATE, "users/$userId/checkIns/$checkInId") {
            val uid = requireUserId(userId)
            require(checkInId.isNotBlank())
            userDoc(uid).collection("checkIns").document(checkInId).update(
                mapOf(
                    "aiSummary" to aiSummary.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    override suspend fun saveAlert(userId: String, alert: RiskAlert): Result<Unit> =
        safeWrite(OperationType.CREATE, "users/$userId/alerts") {
            val uid = requireUserId(userId)
            userDoc(uid).collection("alerts").document().set(alert.toCreateMap(uid)).await()
        }

    override suspend fun updateAlertStatus(userId: String, alertId: String, status: AlertStatus): Result<Unit> =
        safeWrite(OperationType.UPDATE, "users/$userId/alerts/$alertId") {
            val uid = requireUserId(userId)
            require(alertId.isNotBlank())
            userDoc(uid).collection("alerts").document(alertId).update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    override suspend fun saveRecommendations(userId: String, recs: List<WellnessRecommendation>): Result<Unit> =
        safeWrite(OperationType.CREATE, "users/$userId/recommendations") {
            val uid = requireUserId(userId)
            val col = userDoc(uid).collection("recommendations")
            recs.forEach { rec ->
                col.document().set(rec.toCreateMap(uid)).await()
            }
        }

    override suspend fun toggleRecommendation(userId: String, recId: String, completed: Boolean): Result<Unit> =
        safeWrite(OperationType.UPDATE, "users/$userId/recommendations/$recId") {
            val uid = requireUserId(userId)
            require(recId.isNotBlank())
            userDoc(uid).collection("recommendations").document(recId).update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    override suspend fun saveAiMessage(userId: String, message: AiChatMessage): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/aiMessages") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("aiMessages").document()
            docRef.set(message.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun saveNotification(userId: String, notif: SmartNotification): Result<Unit> =
        safeWrite(OperationType.CREATE, "users/$userId/notifications") {
            val uid = requireUserId(userId)
            userDoc(uid).collection("notifications").document().set(notif.toCreateMap(uid)).await()
        }

    override suspend fun markNotificationRead(userId: String, notifId: String): Result<Unit> =
        safeWrite(OperationType.UPDATE, "users/$userId/notifications/$notifId") {
            val uid = requireUserId(userId)
            require(notifId.isNotBlank())
            userDoc(uid).collection("notifications").document(notifId).update(
                mapOf(
                    "readStatus" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    override suspend fun appendAuditLog(
        userId: String,
        action: String,
        details: String,
        actorRole: String
    ): Result<Unit> = safeWrite(OperationType.CREATE, "users/$userId/auditLogs") {
        val uid = requireUserId(userId)
        val entry = AuditLogEntry(userId = uid, action = action, details = details, actorRole = actorRole)
        userDoc(uid).collection("auditLogs").document().set(entry.toCreateMap(uid)).await()
    }

    override suspend fun deleteAccountData(userId: String): Result<Unit> =
        safeWrite(OperationType.DELETE, "users/$userId") {
            val uid = requireUserId(userId)
            val deletableSubCollections = listOf(
                "checkIns", "alerts", "recommendations", "aiMessages", "notifications",
                "doctorConversations", "doctorMessages", "heartProfiles", "heartMeasurements",
                "bloodPressureRecords", "heartSymptomReports", "healthReportUploads", "aiInsights"
            )
            for (sub in deletableSubCollections) {
                val docs = userDoc(uid).collection(sub).get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            userDoc(uid).delete().await()
        }

    // --- AI Doctor & Heart Specialist Observations ---
    override fun observeDoctorConversations(userId: String): Flow<List<DoctorConversation>> =
        userDoc(userId).collection("doctorConversations")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(40)
            .snapshots()
            .map { snap -> snap.toObjects(DoctorConversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/doctorConversations", userId); throw error }

    override fun observeDoctorMessages(userId: String): Flow<List<DoctorAiMessage>> =
        userDoc(userId).collection("doctorMessages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(250)
            .snapshots()
            .map { snap -> snap.toObjects(DoctorAiMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/doctorMessages", userId); throw error }

    override fun observeHeartProfile(userId: String): Flow<HeartHealthProfile?> =
        userDoc(userId).collection("heartProfiles").document("primary")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(HeartHealthProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error -> logFlowError(error, OperationType.GET, "users/$userId/heartProfiles/primary", userId); throw error }

    override fun observeHeartMeasurements(userId: String): Flow<List<HeartMeasurement>> =
        userDoc(userId).collection("heartMeasurements")
            .orderBy("recordedAtIso", Query.Direction.DESCENDING)
            .limit(60)
            .snapshots()
            .map { snap -> snap.toObjects(HeartMeasurement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/heartMeasurements", userId); throw error }

    override fun observeBloodPressureRecords(userId: String): Flow<List<BloodPressureRecord>> =
        userDoc(userId).collection("bloodPressureRecords")
            .orderBy("recordedAtIso", Query.Direction.DESCENDING)
            .limit(60)
            .snapshots()
            .map { snap -> snap.toObjects(BloodPressureRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/bloodPressureRecords", userId); throw error }

    override fun observeHeartSymptomReports(userId: String): Flow<List<HeartSymptomReport>> =
        userDoc(userId).collection("heartSymptomReports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(HeartSymptomReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/heartSymptomReports", userId); throw error }

    override fun observeHealthReportUploads(userId: String): Flow<List<HealthReportUpload>> =
        userDoc(userId).collection("healthReportUploads")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(HealthReportUpload::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/healthReportUploads", userId); throw error }

    override fun observeAiInsights(userId: String): Flow<List<AiHealthInsightRecord>> =
        userDoc(userId).collection("aiInsights")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(AiHealthInsightRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/aiInsights", userId); throw error }

    override fun observeSafetyEvents(userId: String): Flow<List<MedicalSafetyEvent>> =
        userDoc(userId).collection("medicalSafetyEvents")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snap -> snap.toObjects(MedicalSafetyEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/medicalSafetyEvents", userId); throw error }

    override fun observeConsentRecords(userId: String): Flow<List<UserConsentRecord>> =
        userDoc(userId).collection("consentRecords")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snap -> snap.toObjects(UserConsentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users/$userId/consentRecords", userId); throw error }

    override suspend fun createOrUpdateConversation(userId: String, conversation: DoctorConversation): Result<String> =
        safeWriteValue(OperationType.WRITE, "users/$userId/doctorConversations") {
            val uid = requireUserId(userId)
            val col = userDoc(uid).collection("doctorConversations")
            val docRef = if (conversation.id.isNotBlank()) col.document(conversation.id) else col.document()
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(conversation.toCreateMap(uid)).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to conversation.title.trim().ifBlank { "Health Consultation" }.take(120),
                        "mode" to AiDoctorMode.fromRaw(conversation.mode).name,
                        "lastPreview" to conversation.lastPreview.trim().ifBlank { "Updated conversation" }.take(240),
                        "messageCount" to conversation.messageCount.coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            docRef.id
        }

    override suspend fun deleteConversationAndMessages(userId: String, conversationId: String): Result<Unit> =
        safeWrite(OperationType.DELETE, "users/$userId/doctorConversations/$conversationId") {
            val uid = requireUserId(userId)
            require(conversationId.isNotBlank())
            val msgs = userDoc(uid).collection("doctorMessages")
                .whereEqualTo("conversationId", conversationId)
                .get()
                .await()
            for (doc in msgs.documents) {
                doc.reference.delete().await()
            }
            userDoc(uid).collection("doctorConversations").document(conversationId).delete().await()
        }

    override suspend fun saveDoctorMessage(userId: String, message: DoctorAiMessage): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/doctorMessages") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("doctorMessages").document()
            docRef.set(message.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun updateDoctorMessageFeedback(userId: String, messageId: String, feedback: Long): Result<Unit> =
        safeWrite(OperationType.UPDATE, "users/$userId/doctorMessages/$messageId") {
            val uid = requireUserId(userId)
            require(messageId.isNotBlank())
            userDoc(uid).collection("doctorMessages").document(messageId).update(
                mapOf(
                    "feedback" to feedback.coerceIn(-1L, 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

    override suspend fun saveHeartProfile(userId: String, profile: HeartHealthProfile): Result<Unit> =
        safeWrite(OperationType.WRITE, "users/$userId/heartProfiles/primary") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("heartProfiles").document("primary")
            val snap = docRef.get().await()
            val payload = profile.toCreateMap(uid)
            if (!snap.exists()) {
                docRef.set(payload).await()
            } else {
                val updateMap = payload.toMutableMap().apply {
                    remove("userId")
                    remove("createdAt")
                }
                docRef.update(updateMap).await()
            }
        }

    override suspend fun saveHeartMeasurement(userId: String, measurement: HeartMeasurement): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/heartMeasurements") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("heartMeasurements").document()
            docRef.set(measurement.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun saveBloodPressureRecord(userId: String, record: BloodPressureRecord): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/bloodPressureRecords") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("bloodPressureRecords").document()
            docRef.set(record.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun saveHeartSymptomReport(userId: String, report: HeartSymptomReport): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/heartSymptomReports") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("heartSymptomReports").document()
            docRef.set(report.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun saveHealthReportUpload(userId: String, upload: HealthReportUpload): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/healthReportUploads") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("healthReportUploads").document()
            docRef.set(upload.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun saveAiInsight(userId: String, insight: AiHealthInsightRecord): Result<String> =
        safeWriteValue(OperationType.CREATE, "users/$userId/aiInsights") {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("aiInsights").document()
            docRef.set(insight.toCreateMap(uid)).await()
            docRef.id
        }

    override suspend fun saveSafetyEvent(userId: String, event: MedicalSafetyEvent): Result<Unit> =
        safeWrite(OperationType.CREATE, "users/$userId/medicalSafetyEvents") {
            val uid = requireUserId(userId)
            userDoc(uid).collection("medicalSafetyEvents").document().set(event.toCreateMap(uid)).await()
        }

    override suspend fun saveConsentRecord(userId: String, record: UserConsentRecord): Result<Unit> =
        safeWrite(OperationType.CREATE, "users/$userId/consentRecords") {
            val uid = requireUserId(userId)
            userDoc(uid).collection("consentRecords").document().set(record.toCreateMap(uid)).await()
        }

    private fun logFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        if (auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(firestoreError ?: error, op, path)
        }
    }

    private suspend inline fun safeWrite(
        op: OperationType,
        path: String,
        crossinline block: suspend () -> Unit
    ): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, op, path)
        Result.failure(e)
    }

    private suspend inline fun <T> safeWriteValue(
        op: OperationType,
        path: String,
        crossinline block: suspend () -> T
    ): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, op, path)
        Result.failure(e)
    }
}

/**
 * In-memory reactive repository for Judge Demo Mode (fictional sample data).
 */
class DemoHealthRepository(initialRole: UserRole = UserRole.INDIVIDUAL) : HealthRepository {
    private val profileFlow = MutableStateFlow<UserProfile?>(DemoDataFactory.demoProfile(initialRole))
    private val checkInsFlow = MutableStateFlow(DemoDataFactory.demoCheckIns())
    private val alertsFlow = MutableStateFlow(DemoDataFactory.demoAlerts())
    private val recsFlow = MutableStateFlow(DemoDataFactory.demoRecommendations())
    private val messagesFlow = MutableStateFlow(DemoDataFactory.demoAiMessages())
    private val notificationsFlow = MutableStateFlow(DemoDataFactory.demoNotifications())
    private val auditLogsFlow = MutableStateFlow(DemoDataFactory.demoAuditLogs())
    private val conversationsFlow = MutableStateFlow(DemoDataFactory.demoDoctorConversations())
    private val doctorMessagesFlow = MutableStateFlow(DemoDataFactory.demoDoctorMessages())
    private val heartProfileFlow = MutableStateFlow<HeartHealthProfile?>(DemoDataFactory.demoHeartProfile())
    private val heartMeasurementsFlow = MutableStateFlow(DemoDataFactory.demoHeartMeasurements())
    private val bloodPressureFlow = MutableStateFlow(DemoDataFactory.demoBloodPressureRecords())
    private val heartSymptomReportsFlow = MutableStateFlow(DemoDataFactory.demoHeartSymptomReports())
    private val healthReportUploadsFlow = MutableStateFlow(DemoDataFactory.demoHealthReportUploads())
    private val aiInsightsFlow = MutableStateFlow(DemoDataFactory.demoAiInsights())
    private val safetyEventsFlow = MutableStateFlow(DemoDataFactory.demoSafetyEvents())
    private val consentRecordsFlow = MutableStateFlow(DemoDataFactory.demoConsentRecords())

    fun setDemoRole(role: UserRole) {
        profileFlow.update { it?.copy(role = role.name) }
    }

    override fun observeProfile(userId: String): Flow<UserProfile?> = profileFlow.asStateFlow()
    override fun observeCheckIns(userId: String): Flow<List<HealthCheckIn>> = checkInsFlow.asStateFlow()
    override fun observeAlerts(userId: String): Flow<List<RiskAlert>> = alertsFlow.asStateFlow()
    override fun observeRecommendations(userId: String): Flow<List<WellnessRecommendation>> = recsFlow.asStateFlow()
    override fun observeAiMessages(userId: String): Flow<List<AiChatMessage>> = messagesFlow.asStateFlow()
    override fun observeNotifications(userId: String): Flow<List<SmartNotification>> = notificationsFlow.asStateFlow()
    override fun observeAuditLogs(userId: String): Flow<List<AuditLogEntry>> = auditLogsFlow.asStateFlow()
    override fun observeDoctorConversations(userId: String): Flow<List<DoctorConversation>> = conversationsFlow.asStateFlow()
    override fun observeDoctorMessages(userId: String): Flow<List<DoctorAiMessage>> = doctorMessagesFlow.asStateFlow()
    override fun observeHeartProfile(userId: String): Flow<HeartHealthProfile?> = heartProfileFlow.asStateFlow()
    override fun observeHeartMeasurements(userId: String): Flow<List<HeartMeasurement>> = heartMeasurementsFlow.asStateFlow()
    override fun observeBloodPressureRecords(userId: String): Flow<List<BloodPressureRecord>> = bloodPressureFlow.asStateFlow()
    override fun observeHeartSymptomReports(userId: String): Flow<List<HeartSymptomReport>> = heartSymptomReportsFlow.asStateFlow()
    override fun observeHealthReportUploads(userId: String): Flow<List<HealthReportUpload>> = healthReportUploadsFlow.asStateFlow()
    override fun observeAiInsights(userId: String): Flow<List<AiHealthInsightRecord>> = aiInsightsFlow.asStateFlow()
    override fun observeSafetyEvents(userId: String): Flow<List<MedicalSafetyEvent>> = safetyEventsFlow.asStateFlow()
    override fun observeConsentRecords(userId: String): Flow<List<UserConsentRecord>> = consentRecordsFlow.asStateFlow()

    override suspend fun ensureUserProfile(userId: String, defaultName: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateProfilePreferences(
        userId: String,
        displayName: String,
        department: String,
        language: String,
        consentAnalytics: Boolean,
        consentAiGuidance: Boolean,
        consentCoordinatorOutreach: Boolean,
        remindersEnabled: Boolean,
        reminderHour: Long,
        reminderMinute: Long,
        onboardingCompleted: Boolean
    ): Result<Unit> {
        profileFlow.update { current ->
            (current ?: DemoDataFactory.demoProfile()).copy(
                displayName = displayName,
                department = department,
                language = language,
                consentAnalytics = consentAnalytics,
                consentAiGuidance = consentAiGuidance,
                consentCoordinatorOutreach = consentCoordinatorOutreach,
                remindersEnabled = remindersEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                onboardingCompleted = onboardingCompleted
            )
        }
        return Result.success(Unit)
    }

    override suspend fun saveCheckIn(userId: String, checkIn: HealthCheckIn): Result<String> {
        val newId = "demo_ci_${UUID.randomUUID().toString().take(8)}"
        val saved = checkIn.copy(id = newId, userId = userId, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
        checkInsFlow.update { listOf(saved) + it }
        return Result.success(newId)
    }

    override suspend fun updateCheckInAiSummary(userId: String, checkInId: String, aiSummary: String): Result<Unit> {
        checkInsFlow.update { list -> list.map { if (it.id == checkInId) it.copy(aiSummary = aiSummary) else it } }
        return Result.success(Unit)
    }

    override suspend fun saveAlert(userId: String, alert: RiskAlert): Result<Unit> {
        val saved = alert.copy(id = "demo_alert_${UUID.randomUUID().toString().take(8)}", createdAt = Timestamp.now())
        alertsFlow.update { listOf(saved) + it }
        return Result.success(Unit)
    }

    override suspend fun updateAlertStatus(userId: String, alertId: String, status: AlertStatus): Result<Unit> {
        alertsFlow.update { list -> list.map { if (it.id == alertId) it.copy(status = status.name) else it } }
        return Result.success(Unit)
    }

    override suspend fun saveRecommendations(userId: String, recs: List<WellnessRecommendation>): Result<Unit> {
        val stamped = recs.map { it.copy(id = "demo_rec_${UUID.randomUUID().toString().take(8)}", createdAt = Timestamp.now()) }
        recsFlow.update { stamped + it }
        return Result.success(Unit)
    }

    override suspend fun toggleRecommendation(userId: String, recId: String, completed: Boolean): Result<Unit> {
        recsFlow.update { list -> list.map { if (it.id == recId) it.copy(completed = completed) else it } }
        return Result.success(Unit)
    }

    override suspend fun saveAiMessage(userId: String, message: AiChatMessage): Result<String> {
        val id = "demo_msg_${UUID.randomUUID().toString().take(8)}"
        val saved = message.copy(id = id, createdAt = Timestamp.now())
        messagesFlow.update { it + saved }
        return Result.success(id)
    }

    override suspend fun saveNotification(userId: String, notif: SmartNotification): Result<Unit> {
        val saved = notif.copy(id = "demo_notif_${UUID.randomUUID().toString().take(8)}", createdAt = Timestamp.now())
        notificationsFlow.update { listOf(saved) + it }
        return Result.success(Unit)
    }

    override suspend fun markNotificationRead(userId: String, notifId: String): Result<Unit> {
        notificationsFlow.update { list -> list.map { if (it.id == notifId) it.copy(readStatus = true) else it } }
        return Result.success(Unit)
    }

    override suspend fun appendAuditLog(userId: String, action: String, details: String, actorRole: String): Result<Unit> {
        val saved = AuditLogEntry(
            id = "demo_log_${UUID.randomUUID().toString().take(8)}",
            userId = userId,
            action = action,
            details = details,
            actorRole = actorRole,
            createdAt = Timestamp.now()
        )
        auditLogsFlow.update { listOf(saved) + it }
        return Result.success(Unit)
    }

    override suspend fun createOrUpdateConversation(userId: String, conversation: DoctorConversation): Result<String> {
        val convId = conversation.id.ifBlank { "demo_conv_${UUID.randomUUID().toString().take(8)}" }
        val stamped = conversation.copy(
            id = convId,
            userId = userId,
            createdAt = conversation.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        conversationsFlow.update { list ->
            val exists = list.any { it.id == convId }
            if (exists) {
                listOf(stamped) + list.filterNot { it.id == convId }
            } else {
                listOf(stamped) + list
            }
        }
        return Result.success(convId)
    }

    override suspend fun deleteConversationAndMessages(userId: String, conversationId: String): Result<Unit> {
        conversationsFlow.update { list -> list.filterNot { it.id == conversationId } }
        doctorMessagesFlow.update { list -> list.filterNot { it.conversationId == conversationId } }
        return Result.success(Unit)
    }

    override suspend fun saveDoctorMessage(userId: String, message: DoctorAiMessage): Result<String> {
        val id = "demo_dmsg_${UUID.randomUUID().toString().take(8)}"
        val saved = message.copy(id = id, userId = userId, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
        doctorMessagesFlow.update { it + saved }
        return Result.success(id)
    }

    override suspend fun updateDoctorMessageFeedback(userId: String, messageId: String, feedback: Long): Result<Unit> {
        doctorMessagesFlow.update { list -> list.map { if (it.id == messageId) it.copy(feedback = feedback) else it } }
        return Result.success(Unit)
    }

    override suspend fun saveHeartProfile(userId: String, profile: HeartHealthProfile): Result<Unit> {
        heartProfileFlow.value = profile.copy(id = "primary", userId = userId, updatedAt = Timestamp.now())
        return Result.success(Unit)
    }

    override suspend fun saveHeartMeasurement(userId: String, measurement: HeartMeasurement): Result<String> {
        val id = "demo_hm_${UUID.randomUUID().toString().take(8)}"
        val saved = measurement.copy(id = id, userId = userId, createdAt = Timestamp.now(), updatedAt = Timestamp.now())
        heartMeasurementsFlow.update { listOf(saved) + it }
        return Result.success(id)
    }

    override suspend fun saveBloodPressureRecord(userId: String, record: BloodPressureRecord): Result<String> {
        val id = "demo_bp_${UUID.randomUUID().toString().take(8)}"
        val saved = record.copy(id = id, userId = userId, createdAt = Timestamp.now())
        bloodPressureFlow.update { listOf(saved) + it }
        return Result.success(id)
    }

    override suspend fun saveHeartSymptomReport(userId: String, report: HeartSymptomReport): Result<String> {
        val id = "demo_hsym_${UUID.randomUUID().toString().take(8)}"
        val saved = report.copy(id = id, userId = userId, createdAt = Timestamp.now())
        heartSymptomReportsFlow.update { listOf(saved) + it }
        return Result.success(id)
    }

    override suspend fun saveHealthReportUpload(userId: String, upload: HealthReportUpload): Result<String> {
        val id = "demo_rep_${UUID.randomUUID().toString().take(8)}"
        val saved = upload.copy(id = id, userId = userId, createdAt = Timestamp.now())
        healthReportUploadsFlow.update { listOf(saved) + it }
        return Result.success(id)
    }

    override suspend fun saveAiInsight(userId: String, insight: AiHealthInsightRecord): Result<String> {
        val id = "demo_ins_${UUID.randomUUID().toString().take(8)}"
        val saved = insight.copy(id = id, userId = userId, createdAt = Timestamp.now())
        aiInsightsFlow.update { listOf(saved) + it }
        return Result.success(id)
    }

    override suspend fun saveSafetyEvent(userId: String, event: MedicalSafetyEvent): Result<Unit> {
        val saved = event.copy(id = "demo_saf_${UUID.randomUUID().toString().take(8)}", userId = userId, createdAt = Timestamp.now())
        safetyEventsFlow.update { listOf(saved) + it }
        return Result.success(Unit)
    }

    override suspend fun saveConsentRecord(userId: String, record: UserConsentRecord): Result<Unit> {
        val saved = record.copy(id = "demo_con_${UUID.randomUUID().toString().take(8)}", userId = userId, createdAt = Timestamp.now())
        consentRecordsFlow.update { listOf(saved) + it }
        return Result.success(Unit)
    }

    override suspend fun deleteAccountData(userId: String): Result<Unit> {
        checkInsFlow.value = emptyList()
        alertsFlow.value = emptyList()
        recsFlow.value = emptyList()
        messagesFlow.value = emptyList()
        notificationsFlow.value = emptyList()
        conversationsFlow.value = emptyList()
        doctorMessagesFlow.value = emptyList()
        heartProfileFlow.value = null
        heartMeasurementsFlow.value = emptyList()
        bloodPressureFlow.value = emptyList()
        heartSymptomReportsFlow.value = emptyList()
        healthReportUploadsFlow.value = emptyList()
        aiInsightsFlow.value = emptyList()
        return Result.success(Unit)
    }
}
