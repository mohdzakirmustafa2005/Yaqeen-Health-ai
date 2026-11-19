package com.example.engine

import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.HealthCheckIn
import com.example.model.UserProfile
import java.util.Locale

/**
 * Strips unnecessary direct identifiers before sending minimal health context
 * to external AI providers, and enforces client-side sliding-window rate limiting.
 */
object PiiSanitizer {

    private val emailRegex = Regex("[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val phoneRegex = Regex("(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{3,5}\\)?[\\s.-]?)?\\d{3,5}[\\s.-]?\\d{4}")
    private val employeeIdRegex = Regex("\\b(?:EMP|ID|EID|BADGE)[\\s:#-]*[A-Z0-9-]{3,12}\\b", RegexOption.IGNORE_CASE)
    private val addressRegex = Regex("\\b\\d{1,5}\\s+[A-Za-z0-9.\\s]{2,25}\\s+(?:Street|St|Avenue|Ave|Road|Rd|Lane|Ln|Blvd|Nagar|Colony|Sector)\\b", RegexOption.IGNORE_CASE)

    fun sanitizeFreeText(raw: String, userDisplayName: String? = null): String {
        var cleaned = raw
        if (!userDisplayName.isNullOrBlank() && userDisplayName.length >= 3) {
            cleaned = cleaned.replace(userDisplayName, "[Member]", ignoreCase = true)
            userDisplayName.split(" ").filter { it.length >= 3 }.forEach { token ->
                cleaned = cleaned.replace(Regex("\\b${Regex.escape(token)}\\b", RegexOption.IGNORE_CASE), "[Member]")
            }
        }
        cleaned = emailRegex.replace(cleaned, "[email-redacted]")
        cleaned = phoneRegex.replace(cleaned, "[phone-redacted]")
        cleaned = employeeIdRegex.replace(cleaned, "[id-redacted]")
        cleaned = addressRegex.replace(cleaned, "[address-redacted]")
        return cleaned.trim()
    }

    fun buildMinimizedClinicalContext(
        consentAi: Boolean,
        consentHeartAi: Boolean,
        profile: UserProfile?,
        heartProfile: HeartHealthProfile?,
        recentCheckIns: List<HealthCheckIn>,
        recentHeartMeasurements: List<HeartMeasurement>,
        includeRecentCheckIns: Boolean = true,
        includeHeartMetrics: Boolean = true
    ): String {
        if (!consentAi) return "User opted out of personalized AI context."

        val sb = StringBuilder()
        if (heartProfile?.ageYears != null) {
            sb.appendLine("Age (user-provided): ${heartProfile.ageYears} years")
        }
        if (consentHeartAi && heartProfile != null) {
            if (heartProfile.knownConditions.isNotEmpty()) {
                sb.appendLine("Declared conditions: ${heartProfile.knownConditions.joinToString(", ")}")
            }
            if (heartProfile.medications.isNotEmpty()) {
                sb.appendLine("User-entered medications: ${heartProfile.medications.joinToString(", ")}")
            }
            if (heartProfile.allergies.isNotEmpty()) {
                sb.appendLine("User-entered allergies: ${heartProfile.allergies.joinToString(", ")}")
            }
            sb.appendLine("Smoking: ${heartProfile.smokingStatus}, Family heart history: ${heartProfile.familyHeartHistory}, Baseline activity: ${heartProfile.baselineActivityLevel}")
        }
        if (includeRecentCheckIns && recentCheckIns.isNotEmpty()) {
            sb.appendLine("Recent daily check-ins (de-identified):")
            recentCheckIns.take(4).forEach { ci ->
                sb.appendLine("- ${ci.dateKey}: wellbeing=${ci.overallWellbeing}/10, sleep=${String.format(Locale.US, "%.1f", ci.sleepHours)}h, stress=${ci.stressLevel}/10, fatigue=${ci.fatigueLevel}/10, risk=${ci.computedRiskLevel}")
            }
        }
        if (consentHeartAi && includeHeartMetrics && recentHeartMeasurements.isNotEmpty()) {
            sb.appendLine("Recent user-entered heart & metabolic readings:")
            recentHeartMeasurements.take(5).forEach { m ->
                val parts = mutableListOf<String>()
                m.restingHeartRateBpm?.let { parts += "HR=${it}bpm" }
                if (m.systolicMmHg != null && m.diastolicMmHg != null) parts += "BP=${m.systolicMmHg}/${m.diastolicMmHg}mmHg"
                m.oxygenSaturationPct?.let { parts += "SpO2=${it}%" }
                m.bloodGlucoseMgDl?.let { parts += "Glucose=${it}mg/dL" }
                m.totalCholesterolMgDl?.let { parts += "Chol=${it}mg/dL" }
                m.weightKg?.let { parts += "Weight=${it}kg" }
                if (m.symptoms.isNotEmpty()) parts += "Symptoms=[${m.symptoms.joinToString()}]"
                sb.appendLine("- ${m.recordedAtIso} (${m.activityLevel}): ${parts.joinToString(", ")}")
            }
        }
        // Notice: displayName, department, organizationId, phone, email, and UID are intentionally omitted.
        val result = sb.toString().trim()
        return if (result.isBlank()) "No prior readings supplied; use only the user's current question." else result
    }
}

class AiRateLimiter(
    private val maxRequestsPerWindow: Int = 20,
    private val windowMillis: Long = 10 * 60 * 1000L,
    private val minIntervalMillis: Long = 1500L
) {
    private val timestamps = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(nowMillis: Long = System.currentTimeMillis()): RateLimitOutcome {
        while (timestamps.isNotEmpty() && nowMillis - timestamps.first() > windowMillis) {
            timestamps.removeFirst()
        }
        val last = timestamps.lastOrNull()
        if (last != null && nowMillis - last < minIntervalMillis) {
            return RateLimitOutcome(allowed = false, reason = "Please wait a moment before sending another AI request.")
        }
        if (timestamps.size >= maxRequestsPerWindow) {
            return RateLimitOutcome(allowed = false, reason = "AI safety rate limit reached (20 requests per 10 minutes). Please try again shortly.")
        }
        timestamps.addLast(nowMillis)
        return RateLimitOutcome(allowed = true)
    }
}

data class RateLimitOutcome(
    val allowed: Boolean,
    val reason: String? = null
)
