package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class DoctorDomain(val label: String) {
    DOCTOR("AI Doctor"),
    HEART_SPECIALIST("AI Heart Specialist")
}

enum class AiDoctorMode(
    val modeId: String,
    val displayName: String,
    val subtitle: String,
    val domain: DoctorDomain,
    val defaultTaskType: AiTaskType
) {
    GENERAL_HEALTH(
        modeId = "general_health",
        displayName = "General Health",
        subtitle = "General health questions, prevention & lifestyle education",
        domain = DoctorDomain.DOCTOR,
        defaultTaskType = AiTaskType.GENERAL_HEALTH_ASSISTANT
    ),
    HEART_SPECIALIST(
        modeId = "heart_specialist",
        displayName = "Heart Specialist",
        subtitle = "Cardiovascular education, vitals & ECG report guidance",
        domain = DoctorDomain.HEART_SPECIALIST,
        defaultTaskType = AiTaskType.HEART_HEALTH_ASSISTANT
    ),
    SYMPTOM_CHECKER(
        modeId = "symptom_checker",
        displayName = "Symptom Checker",
        subtitle = "Duration, severity, warning signs & safe next steps",
        domain = DoctorDomain.DOCTOR,
        defaultTaskType = AiTaskType.HEALTH_SYMPTOM_INFORMATION
    ),
    REPORT_EXPLAINER(
        modeId = "report_explainer",
        displayName = "Report Explainer",
        subtitle = "Medical terms, units, reference ranges & doctor questions",
        domain = DoctorDomain.DOCTOR,
        defaultTaskType = AiTaskType.HEALTH_REPORT_EXPLANATION
    ),
    WELLNESS(
        modeId = "wellness",
        displayName = "Wellness",
        subtitle = "Sleep, fatigue, hydration, activity & healthy routines",
        domain = DoctorDomain.DOCTOR,
        defaultTaskType = AiTaskType.HEALTH_WELLNESS_GUIDANCE
    ),
    MENTAL_WELLNESS(
        modeId = "mental_wellness",
        displayName = "Mental Wellness",
        subtitle = "Stress, burnout recovery, emotional wellbeing & grounding",
        domain = DoctorDomain.DOCTOR,
        defaultTaskType = AiTaskType.HEALTH_WELLNESS_GUIDANCE
    ),
    HEALTH_NAVIGATOR(
        modeId = "health_navigator",
        displayName = "Health Navigator",
        subtitle = "Which specialist to consult, urgency & appointment prep",
        domain = DoctorDomain.DOCTOR,
        defaultTaskType = AiTaskType.APPOINTMENT_PREPARATION
    );

    companion object {
        fun fromRaw(raw: String?): AiDoctorMode =
            entries.firstOrNull { it.name == raw || it.modeId == raw } ?: GENERAL_HEALTH
    }
}

enum class AiTaskType(val label: String) {
    GENERAL_HEALTH_ASSISTANT("General Health Assistant"),
    HEALTH_SYMPTOM_INFORMATION("Health Symptom Information"),
    HEALTH_REPORT_EXPLANATION("Health Report Explanation"),
    HEALTH_WELLNESS_GUIDANCE("Health Wellness Guidance"),
    HEART_HEALTH_ASSISTANT("Heart Health Assistant"),
    CARDIOVASCULAR_EDUCATION("Cardiovascular Education"),
    HEART_MEASUREMENT_EXPLANATION("Heart Measurement Explanation"),
    ECG_REPORT_EXPLANATION("ECG Report Explanation"),
    HEALTH_TREND_ANALYSIS("Health Trend Analysis"),
    PERSONALIZED_WELLNESS_PLAN("Personalized Wellness Plan"),
    APPOINTMENT_PREPARATION("Appointment Preparation");

    companion object {
        fun fromRaw(raw: String?): AiTaskType =
            entries.firstOrNull { it.name == raw } ?: GENERAL_HEALTH_ASSISTANT
    }
}

data class MedicalAttentionAdvice(
    val recommended: Boolean = false,
    val urgency: String = "routine",
    val reason: String = ""
)

data class EmergencyScreenResult(
    val triggered: Boolean = false,
    val reason: String = "",
    val matchedSignals: List<String> = emptyList(),
    val guidance: String = ""
)

data class StructuredAiResponse(
    val mode: String = "general_health",
    val summary: String = "",
    val answer: String = "",
    val observations: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    val doctorType: String? = null,
    val medicalAttention: MedicalAttentionAdvice = MedicalAttentionAdvice(),
    val emergencyScreen: EmergencyScreenResult = EmergencyScreenResult(),
    val disclaimer: String = DOCTOR_MODE_DISCLAIMER,
    val providerUsed: String = "Firebase AI Logic (Gemini)",
    val fallbackUsed: Boolean = false
) {
    companion object {
        const val DOCTOR_MODE_DISCLAIMER =
            "AI Doctor Mode provides health information and support. It is not a licensed doctor and does not replace professional medical care."
        const val HEART_MODE_DISCLAIMER =
            "Yaqeen AI Cardiology Assistant provides cardiovascular health education only. It is not a cardiologist and does not diagnose heart conditions."
    }
}

@Keep
@IgnoreExtraProperties
data class DoctorConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val domain: String = DoctorDomain.DOCTOR.name,
    val mode: String = AiDoctorMode.GENERAL_HEALTH.name,
    val title: String = "Health Consultation",
    val lastPreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "domain" to if (domain == DoctorDomain.HEART_SPECIALIST.name) DoctorDomain.HEART_SPECIALIST.name else DoctorDomain.DOCTOR.name,
        "mode" to AiDoctorMode.fromRaw(mode).name,
        "title" to title.trim().ifBlank { "Health Consultation" }.take(120),
        "lastPreview" to lastPreview.trim().ifBlank { "New conversation" }.take(240),
        "messageCount" to messageCount.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DoctorAiMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val sender: String = "USER",
    val mode: String = AiDoctorMode.GENERAL_HEALTH.name,
    val taskType: String = AiTaskType.GENERAL_HEALTH_ASSISTANT.name,
    val text: String = "",
    val structuredJson: String? = null,
    val providerUsed: String = "Firebase AI Logic",
    val feedback: Long = 0L,
    val emergencyTriggered: Boolean = false,
    val fallbackUsed: Boolean = false,
    val language: String = "EN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "conversationId" to conversationId.trim().ifBlank { "default" }.take(128),
            "sender" to if (sender in listOf("USER", "ASSISTANT", "SYSTEM_EMERGENCY")) sender else "ASSISTANT",
            "mode" to AiDoctorMode.fromRaw(mode).name,
            "taskType" to AiTaskType.fromRaw(taskType).name,
            "text" to text.trim().ifBlank { "..." }.take(6000),
            "structuredJson" to structuredJson?.trim()?.take(12000)?.takeIf { it.isNotEmpty() },
            "providerUsed" to providerUsed.trim().ifBlank { "Firebase AI Logic" }.take(80),
            "feedback" to feedback.coerceIn(-1L, 1L),
            "emergencyTriggered" to emergencyTriggered,
            "fallbackUsed" to fallbackUsed,
            "language" to if (language in listOf("EN", "HI", "TE", "UR")) language else "EN",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class HeartHealthProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ageYears: Long? = null,
    val knownConditions: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val smokingStatus: String = "NEVER",
    val familyHeartHistory: Boolean = false,
    val baselineActivityLevel: String = "MODERATE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "ageYears" to ageYears?.coerceIn(14L, 120L),
            "knownConditions" to knownConditions.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(15),
            "medications" to medications.map { it.trim().take(80) }.filter { it.isNotBlank() }.take(20),
            "allergies" to allergies.map { it.trim().take(80) }.filter { it.isNotBlank() }.take(20),
            "smokingStatus" to if (smokingStatus in listOf("NEVER", "FORMER", "CURRENT", "PREFER_NOT_TO_SAY")) smokingStatus else "NEVER",
            "familyHeartHistory" to familyHeartHistory,
            "baselineActivityLevel" to if (baselineActivityLevel in listOf("SEDENTARY", "LIGHT", "MODERATE", "VIGOROUS")) baselineActivityLevel else "MODERATE",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class HeartMeasurement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val recordedAtIso: String = "",
    val restingHeartRateBpm: Long? = null,
    val systolicMmHg: Long? = null,
    val diastolicMmHg: Long? = null,
    val oxygenSaturationPct: Long? = null,
    val weightKg: Double? = null,
    val bloodGlucoseMgDl: Double? = null,
    val totalCholesterolMgDl: Double? = null,
    val ldlCholesterolMgDl: Double? = null,
    val hdlCholesterolMgDl: Double? = null,
    val symptoms: List<String> = emptyList(),
    val activityLevel: String = "RESTING",
    val notes: String? = null,
    val sourceLabel: String = "USER_ENTERED",
    val emergencyFlagged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "recordedAtIso" to recordedAtIso.trim().ifBlank { "2026-11-19 09:00" }.take(32),
            "restingHeartRateBpm" to restingHeartRateBpm?.coerceIn(25L, 250L),
            "systolicMmHg" to systolicMmHg?.coerceIn(50L, 280L),
            "diastolicMmHg" to diastolicMmHg?.coerceIn(30L, 180L),
            "oxygenSaturationPct" to oxygenSaturationPct?.coerceIn(50L, 100L),
            "weightKg" to weightKg?.coerceIn(20.0, 350.0),
            "bloodGlucoseMgDl" to bloodGlucoseMgDl?.coerceIn(20.0, 800.0),
            "totalCholesterolMgDl" to totalCholesterolMgDl?.coerceIn(50.0, 600.0),
            "ldlCholesterolMgDl" to ldlCholesterolMgDl?.coerceIn(20.0, 500.0),
            "hdlCholesterolMgDl" to hdlCholesterolMgDl?.coerceIn(10.0, 200.0),
            "symptoms" to symptoms.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(15),
            "activityLevel" to if (activityLevel in listOf("RESTING", "LIGHT_ACTIVITY", "POST_EXERCISE", "MODERATE", "VIGOROUS")) activityLevel else "RESTING",
            "notes" to notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "sourceLabel" to if (sourceLabel == "DEMO_FICTIONAL") "DEMO_FICTIONAL" else "USER_ENTERED",
            "emergencyFlagged" to emergencyFlagged,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class BloodPressureRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val measurementId: String? = null,
    val recordedAtIso: String = "",
    val systolicMmHg: Long = 120L,
    val diastolicMmHg: Long = 80L,
    val pulseBpm: Long? = null,
    val postureOrContext: String = "SITTING_REST",
    val notes: String? = null,
    val sourceLabel: String = "USER_ENTERED",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "measurementId" to measurementId?.trim()?.take(128)?.takeIf { it.isNotEmpty() },
            "recordedAtIso" to recordedAtIso.trim().ifBlank { "2026-11-19 09:00" }.take(32),
            "systolicMmHg" to systolicMmHg.coerceIn(50L, 280L),
            "diastolicMmHg" to diastolicMmHg.coerceIn(30L, 180L),
            "pulseBpm" to pulseBpm?.coerceIn(25L, 250L),
            "postureOrContext" to if (postureOrContext in listOf("SITTING_REST", "STANDING", "MORNING", "EVENING", "POST_ACTIVITY")) postureOrContext else "SITTING_REST",
            "notes" to notes?.trim()?.take(400)?.takeIf { it.isNotEmpty() },
            "sourceLabel" to if (sourceLabel == "DEMO_FICTIONAL") "DEMO_FICTIONAL" else "USER_ENTERED",
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class HeartSymptomReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symptoms: List<String> = emptyList(),
    val severityScore: Long = 3L,
    val durationLabel: String = "",
    val associatedSymptoms: List<String> = emptyList(),
    val notes: String? = null,
    val emergencyTriggered: Boolean = false,
    val screeningReason: String = "",
    val aiSummary: String = "",
    val urgencyLevel: String = "routine",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "symptoms" to symptoms.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(15),
            "severityScore" to severityScore.coerceIn(1L, 10L),
            "durationLabel" to durationLabel.trim().ifBlank { "Not specified" }.take(80),
            "associatedSymptoms" to associatedSymptoms.map { it.trim().take(60) }.filter { it.isNotBlank() }.take(15),
            "notes" to notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
            "emergencyTriggered" to emergencyTriggered,
            "screeningReason" to screeningReason.trim().ifBlank { "Routine screening completed." }.take(400),
            "aiSummary" to aiSummary.trim().ifBlank { "Review non-diagnostic guidance." }.take(2000),
            "urgencyLevel" to if (urgencyLevel in listOf("routine", "prompt", "urgent", "emergency")) urgencyLevel else "routine",
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class HealthReportUpload(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val reportType: String = "GENERAL_LAB",
    val fileName: String? = null,
    val extractedOrEnteredText: String = "",
    val summary: String = "",
    val outOfRangeFindings: List<String> = emptyList(),
    val questionsForDoctor: List<String> = emptyList(),
    val emergencyTriggered: Boolean = false,
    val providerUsed: String = "Firebase AI Logic",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "title" to title.trim().ifBlank { "Health Report" }.take(120),
            "reportType" to if (reportType in listOf("GENERAL_LAB", "ECG_REPORT", "CARDIAC_LAB", "BLOOD_PRESSURE_LOG", "OTHER")) reportType else "GENERAL_LAB",
            "fileName" to fileName?.trim()?.take(120)?.takeIf { it.isNotEmpty() },
            "extractedOrEnteredText" to extractedOrEnteredText.trim().ifBlank { "Image / Lab upload" }.take(5000),
            "summary" to summary.trim().ifBlank { "Report reviewed." }.take(2000),
            "outOfRangeFindings" to outOfRangeFindings.map { it.trim().take(180) }.filter { it.isNotBlank() }.take(15),
            "questionsForDoctor" to questionsForDoctor.map { it.trim().take(200) }.filter { it.isNotBlank() }.take(10),
            "emergencyTriggered" to emergencyTriggered,
            "providerUsed" to providerUsed.trim().ifBlank { "Firebase AI Logic" }.take(80),
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class AiHealthInsightRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val domain: String = "DOCTOR",
    val taskType: String = AiTaskType.GENERAL_HEALTH_ASSISTANT.name,
    val title: String = "",
    val summary: String = "",
    val observations: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val recommendedDoctorType: String? = null,
    val urgency: String = "routine",
    val providerUsed: String = "Firebase AI Logic",
    val fallbackUsed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "domain" to if (domain in listOf("DOCTOR", "HEART_SPECIALIST", "REPORT_EXPLAINER", "APPOINTMENT_PREP")) domain else "DOCTOR",
            "taskType" to AiTaskType.fromRaw(taskType).name,
            "title" to title.trim().ifBlank { "AI Wellness Insight" }.take(140),
            "summary" to summary.trim().ifBlank { "Non-diagnostic summary." }.take(2000),
            "observations" to observations.map { it.trim().take(220) }.filter { it.isNotBlank() }.take(10),
            "recommendations" to recommendations.map { it.trim().take(220) }.filter { it.isNotBlank() }.take(10),
            "recommendedDoctorType" to recommendedDoctorType?.trim()?.take(80)?.takeIf { it.isNotEmpty() },
            "urgency" to if (urgency in listOf("routine", "prompt", "urgent", "emergency")) urgency else "routine",
            "providerUsed" to providerUsed.trim().ifBlank { "Firebase AI Logic" }.take(80),
            "fallbackUsed" to fallbackUsed,
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class MedicalSafetyEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceModule: String = "HEART_SCREENING",
    val matchedSignals: List<String> = emptyList(),
    val reason: String = "",
    val actionTaken: String = "BLOCKED_WELLNESS_FLOW_SHOWED_EMERGENCY_GUIDANCE",
    val emergencyRegionHint: String = "INDIA_112_OR_LOCAL_EMS",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "sourceModule" to sourceModule.trim().ifBlank { "HEART_SCREENING" }.take(60),
        "matchedSignals" to matchedSignals.map { it.trim().take(80) }.filter { it.isNotBlank() }.take(15),
        "reason" to reason.trim().ifBlank { "Emergency symptom indicator detected." }.take(400),
        "actionTaken" to actionTaken.trim().ifBlank { "BLOCKED_WELLNESS_FLOW_SHOWED_EMERGENCY_GUIDANCE" }.take(120),
        "emergencyRegionHint" to emergencyRegionHint.trim().ifBlank { "INDIA_112_OR_LOCAL_EMS" }.take(60),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class UserConsentRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val consentAnalytics: Boolean = true,
    val consentAiGuidance: Boolean = true,
    val consentHeartAiContext: Boolean = true,
    val consentReportAnalysis: Boolean = true,
    val consentCoordinatorOutreach: Boolean = false,
    val policyNote: String = "User updated consent settings",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "consentAnalytics" to consentAnalytics,
        "consentAiGuidance" to consentAiGuidance,
        "consentHeartAiContext" to consentHeartAiContext,
        "consentReportAnalysis" to consentReportAnalysis,
        "consentCoordinatorOutreach" to consentCoordinatorOutreach,
        "policyNote" to policyNote.trim().ifBlank { "Consent updated" }.take(240),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

data class HeartRiskFactorTopic(
    val id: String,
    val title: String,
    val summary: String,
    val preventiveFocus: String,
    val discussWithClinician: String
)
