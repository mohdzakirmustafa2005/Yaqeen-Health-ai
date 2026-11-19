package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String, val subtitle: String) {
    INDIVIDUAL("Individual", "Personal health check-ins, AI guidance & prevention"),
    COORDINATOR("Health Coordinator", "Permitted follow-ups, alerts & wellness outreach"),
    ORG_ADMIN("Organization Admin", "Privacy-preserving workforce & department analytics"),
    PLATFORM_ADMIN("Platform Admin", "Role management, AI telemetry & audit governance");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: INDIVIDUAL
    }
}

enum class RiskLevel(val label: String, val severityRank: Int) {
    GREEN("Stable", 0),
    YELLOW("Monitor", 1),
    ORANGE("Follow-Up Recommended", 2),
    RED("Urgent Attention", 3);

    companion object {
        fun fromRaw(raw: String?): RiskLevel =
            entries.firstOrNull { it.name == raw } ?: GREEN
    }
}

enum class AlertStatus {
    ACTIVE, ACKNOWLEDGED, DISMISSED;

    companion object {
        fun fromRaw(raw: String?): AlertStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class MoodOption(val displayName: String) {
    CALM("Calm"),
    OPTIMISTIC("Optimistic"),
    TIRED("Tired"),
    ANXIOUS("Anxious"),
    OVERWHELMED("Overwhelmed"),
    LOW("Low");

    companion object {
        fun fromRaw(raw: String?): MoodOption =
            entries.firstOrNull { it.name == raw } ?: CALM
    }
}

enum class RecCategory(val displayName: String) {
    SLEEP("Sleep"),
    HYDRATION("Hydration"),
    ACTIVITY("Activity"),
    PHYSICAL_ACTIVITY("Physical Activity"),
    STRESS("Stress Relief"),
    NUTRITION("Nutrition"),
    BREAKS("Work Breaks"),
    MENTAL("Mental Wellness"),
    CARDIOVASCULAR("Heart Wellness"),
    HEALTH_FOLLOW_UP("Health Follow-Up"),
    MEDICAL("Medical Follow-Up");

    companion object {
        fun fromRaw(raw: String?): RecCategory =
            entries.firstOrNull { it.name == raw } ?: STRESS
    }
}

enum class RecFrequency { DAILY, WEEKLY }

enum class NotifCategory { REMINDER, ALERT, RECOMMENDATION, SUMMARY, FOLLOW_UP }

enum class AiSender { USER, ASSISTANT }

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Team Member",
    val department: String = "Operations",
    val organizationId: String = "crescent-workforce",
    val role: String = UserRole.INDIVIDUAL.name,
    val language: String = "EN",
    val consentAnalytics: Boolean = true,
    val consentAiGuidance: Boolean = true,
    val consentCoordinatorOutreach: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderHour: Long = 9L,
    val reminderMinute: Long = 0L,
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "displayName" to displayName.trim().ifBlank { "Team Member" }.take(80),
        "department" to department.trim().ifBlank { "Operations" }.take(60),
        "organizationId" to organizationId.trim().ifBlank { "crescent-workforce" }.take(64),
        "role" to UserRole.INDIVIDUAL.name,
        "language" to language,
        "consentAnalytics" to consentAnalytics,
        "consentAiGuidance" to consentAiGuidance,
        "consentCoordinatorOutreach" to consentCoordinatorOutreach,
        "remindersEnabled" to remindersEnabled,
        "reminderHour" to reminderHour.coerceIn(0L, 23L),
        "reminderMinute" to reminderMinute.coerceIn(0L, 59L),
        "onboardingCompleted" to onboardingCompleted,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class HealthCheckIn(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val overallWellbeing: Long = 7L,
    val sleepQuality: Long = 7L,
    val sleepHours: Double = 7.0,
    val stressLevel: Long = 4L,
    val energyLevel: Long = 7L,
    val fatigueLevel: Long = 4L,
    val mood: String = MoodOption.CALM.name,
    val symptoms: List<String> = emptyList(),
    val workStress: Long = 4L,
    val hydrationLiters: Double = 2.0,
    val activityMinutes: Long = 30L,
    val notes: String? = null,
    val computedRiskLevel: String = RiskLevel.GREEN.name,
    val aiSummary: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val cleanNotes = notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
        val cleanAi = aiSummary?.trim()?.take(1000)?.takeIf { it.isNotEmpty() }
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "dateKey" to dateKey.take(20),
            "overallWellbeing" to overallWellbeing.coerceIn(1L, 10L),
            "sleepQuality" to sleepQuality.coerceIn(1L, 10L),
            "sleepHours" to sleepHours.coerceIn(0.0, 24.0),
            "stressLevel" to stressLevel.coerceIn(1L, 10L),
            "energyLevel" to energyLevel.coerceIn(1L, 10L),
            "fatigueLevel" to fatigueLevel.coerceIn(1L, 10L),
            "mood" to MoodOption.fromRaw(mood).name,
            "symptoms" to symptoms.take(15),
            "workStress" to workStress.coerceIn(1L, 10L),
            "hydrationLiters" to hydrationLiters.coerceIn(0.0, 10.0),
            "activityMinutes" to activityMinutes.coerceIn(0L, 600L),
            "notes" to cleanNotes,
            "computedRiskLevel" to RiskLevel.fromRaw(computedRiskLevel).name,
            "aiSummary" to cleanAi,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class RiskAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val riskLevel: String = RiskLevel.YELLOW.name,
    val title: String = "",
    val explanation: String = "",
    val recommendedNextStep: String = "",
    val triggerFactors: List<String> = emptyList(),
    val dateKey: String = "",
    val status: String = AlertStatus.ACTIVE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "riskLevel" to RiskLevel.fromRaw(riskLevel).name,
        "title" to title.trim().ifBlank { "Wellness Notice" }.take(120),
        "explanation" to explanation.trim().ifBlank { "Review recent check-in trends." }.take(600),
        "recommendedNextStep" to recommendedNextStep.trim().ifBlank { "Take rest and monitor." }.take(400),
        "triggerFactors" to triggerFactors.take(10),
        "dateKey" to dateKey.take(20),
        "status" to AlertStatus.ACTIVE.name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class WellnessRecommendation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = RecCategory.STRESS.name,
    val title: String = "",
    val description: String = "",
    val frequency: String = RecFrequency.DAILY.name,
    val completed: Boolean = false,
    val source: String = "ENGINE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "category" to RecCategory.fromRaw(category).name,
        "title" to title.trim().ifBlank { "Daily Wellness Habit" }.take(120),
        "description" to description.trim().ifBlank { "Practice preventive recovery." }.take(500),
        "frequency" to if (frequency == RecFrequency.WEEKLY.name) RecFrequency.WEEKLY.name else RecFrequency.DAILY.name,
        "completed" to completed,
        "source" to if (source == "AI") "AI" else "ENGINE",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AiChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sender: String = AiSender.ASSISTANT.name,
    val text: String = "",
    val language: String = "EN",
    val fallbackUsed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "sender" to if (sender == AiSender.USER.name) AiSender.USER.name else AiSender.ASSISTANT.name,
        "text" to text.trim().ifBlank { "..." }.take(4000),
        "language" to language,
        "fallbackUsed" to fallbackUsed,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class SmartNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = NotifCategory.REMINDER.name,
    val title: String = "",
    val body: String = "",
    val readStatus: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "category" to category,
        "title" to title.trim().ifBlank { "Yaqeen Health Notice" }.take(120),
        "body" to body.trim().ifBlank { "Check your wellness summary." }.take(400),
        "readStatus" to readStatus,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "",
    val details: String = "",
    val actorRole: String = UserRole.INDIVIDUAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "action" to action.trim().ifBlank { "EVENT" }.take(80),
        "details" to details.trim().ifBlank { "Recorded event" }.take(300),
        "actorRole" to actorRole.take(30),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

// Privacy-Preserving Organization Aggregate (Enforces minimum group-size threshold k >= 5)
data class DepartmentAggregate(
    val departmentName: String,
    val memberCount: Int,
    val participationPercent: Int,
    val avgWellbeing: Float,
    val avgStress: Float,
    val avgSleepHours: Float,
    val avgFatigue: Float,
    val highRiskSharePercent: Int
) {
    val isSuppressedForPrivacy: Boolean get() = memberCount < MIN_GROUP_SIZE

    companion object {
        const val MIN_GROUP_SIZE = 5
    }
}

data class WorkforceAnalyticsSummary(
    val organizationName: String,
    val totalConsentedMembers: Int,
    val participationRatePercent: Int,
    val avgWellbeingScore: Float,
    val greenCount: Int,
    val yellowCount: Int,
    val orangeCount: Int,
    val redCount: Int,
    val departments: List<DepartmentAggregate>,
    val isDemoData: Boolean = true
)

data class CoordinatorOutreachCase(
    val caseId: String,
    val anonymizedAlias: String,
    val department: String,
    val consentGranted: Boolean,
    val riskLevel: RiskLevel,
    val triggerSummary: String,
    val lastCheckInDate: String,
    val outreachNote: String,
    val status: String
)
