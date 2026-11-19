package com.example.engine

import com.example.model.HealthCheckIn
import com.example.model.MoodOption
import com.example.model.RecCategory
import com.example.model.RecFrequency
import com.example.model.RiskAlert
import com.example.model.RiskLevel
import com.example.model.WellnessRecommendation

data class RiskEvaluationResult(
    val riskLevel: RiskLevel,
    val burnoutScore: Int, // 0..100
    val triggerFactors: List<String>,
    val title: String,
    val explanation: String,
    val recommendedNextStep: String,
    val suggestedRecommendations: List<WellnessRecommendation>
)

/**
 * Explainable, transparent, non-diagnostic Early Health Warning Engine.
 * Evaluates both single check-ins and longitudinal check-in trajectories.
 */
object EarlyWarningEngine {

    private val urgentKeywords = listOf(
        "chest pain", "chest tightness", "shortness of breath", "trouble breathing",
        "fainting", "passed out", "severe dizziness", "numbness", "slurred speech",
        "self-harm", "suicidal", "crushing pain"
    )

    private val urgentSymptoms = setOf(
        "Chest discomfort", "Shortness of breath", "Severe dizziness"
    )

    fun evaluate(
        latest: HealthCheckIn,
        history: List<HealthCheckIn>,
        userId: String
    ): RiskEvaluationResult {
        val recent = (listOf(latest) + history).take(7)
        val triggers = mutableListOf<String>()
        var points = 0

        val notesLower = latest.notes.orEmpty().lowercase()
        val hasUrgentSymptom = latest.symptoms.any { it in urgentSymptoms } ||
            urgentKeywords.any { notesLower.contains(it) }

        if (hasUrgentSymptom) {
            triggers += "Urgent symptom or warning sign reported"
            points += 10
        }

        // 1. Sleep deprivation / poor sleep pattern
        if (latest.sleepHours < 5.5 || latest.sleepQuality <= 3) {
            triggers += "Short or poor quality sleep (${String.format("%.1f", latest.sleepHours)}h)"
            points += 3
        } else if (latest.sleepHours < 6.5 || latest.sleepQuality <= 5) {
            triggers += "Below-target sleep recovery (${String.format("%.1f", latest.sleepHours)}h)"
            points += 1
        }

        val multiDayPoorSleep = recent.count { it.sleepHours < 6.0 || it.sleepQuality <= 4 }
        if (multiDayPoorSleep >= 3) {
            triggers += "Repeated sleep deficit ($multiDayPoorSleep of last ${recent.size} check-ins)"
            points += 2
        }

        // 2. Stress & work-related burnout indicators
        if (latest.stressLevel >= 8 || latest.workStress >= 8) {
            triggers += "High stress load (${latest.stressLevel}/10; work ${latest.workStress}/10)"
            points += 3
        } else if (latest.stressLevel >= 6 || latest.workStress >= 6) {
            triggers += "Elevated stress (${latest.stressLevel}/10)"
            points += 1
        }

        val multiDayHighStress = recent.count { it.stressLevel >= 7 || it.workStress >= 7 }
        if (multiDayHighStress >= 3) {
            triggers += "Sustained stress streak ($multiDayHighStress recent check-ins)"
            points += 2
        }

        // 3. Fatigue & low energy cluster
        if (latest.fatigueLevel >= 7 && latest.energyLevel <= 4) {
            triggers += "High fatigue (${latest.fatigueLevel}/10) with low energy (${latest.energyLevel}/10)"
            points += 3
        } else if (latest.fatigueLevel >= 7 || latest.energyLevel <= 4) {
            triggers += "Noticeable fatigue or low energy"
            points += 1
        }

        // 4. Longitudinal declining wellness trend (compare latest vs prior check-ins)
        if (history.size >= 2) {
            val prev1 = history[0]
            val prev2 = history[1]
            if (latest.overallWellbeing < prev1.overallWellbeing && prev1.overallWellbeing < prev2.overallWellbeing) {
                triggers += "Declining wellbeing trend (${prev2.overallWellbeing} -> ${prev1.overallWellbeing} -> ${latest.overallWellbeing})"
                points += 2
            }
        }

        // 5. Persistent physical symptoms & mood
        if (latest.symptoms.size >= 3) {
            triggers += "${latest.symptoms.size} concurrent physical symptoms"
            points += 2
        } else if (latest.symptoms.isNotEmpty()) {
            triggers += "Reported symptoms: ${latest.symptoms.take(2).joinToString(", ")}"
            points += 1
        }

        val moodEnum = MoodOption.fromRaw(latest.mood)
        if (moodEnum == MoodOption.OVERWHELMED || moodEnum == MoodOption.LOW) {
            triggers += "Emotional load indicator (${moodEnum.displayName})"
            points += 1
        }

        // 6. Lifestyle hydration / activity protective signals
        if (latest.hydrationLiters < 1.2) {
            triggers += "Low hydration (${String.format("%.1f", latest.hydrationLiters)}L)"
            points += 1
        }

        val riskLevel = when {
            hasUrgentSymptom || points >= 9 -> RiskLevel.RED
            points >= 6 -> RiskLevel.ORANGE
            points >= 3 -> RiskLevel.YELLOW
            else -> RiskLevel.GREEN
        }

        val burnoutScore = computeBurnoutIndex(latest, recent)

        val (title, explanation, nextStep) = when (riskLevel) {
            RiskLevel.RED -> Triple(
                "Urgent Wellness Warning Signs Detected",
                "Your check-in includes high-priority warning indicators (${triggers.take(3).joinToString("; ")}). This platform does not diagnose conditions, so urgent symptoms should be evaluated promptly by a clinician.",
                "If you are experiencing severe chest pain, breathing difficulty, or fainting, seek emergency medical care immediately. Otherwise contact a healthcare professional today."
            )
            RiskLevel.ORANGE -> Triple(
                "Elevated Strain & Burnout Pattern",
                "Combined indicators across sleep, stress, or fatigue (${triggers.take(3).joinToString("; ")}) suggest growing strain that benefits from proactive follow-up.",
                "Prioritize rest recovery today, lighten non-urgent workload where possible, and consider scheduling a check-in with a healthcare professional or health coordinator."
            )
            RiskLevel.YELLOW -> Triple(
                "Mild Wellness Drift — Monitor Closely",
                "Early signals (${triggers.take(2).joinToString("; ")}) show mild strain compared to optimal balance.",
                "Follow today's hydration, micro-break, and sleep wind-down plan, and complete tomorrow's check-in to watch the trend."
            )
            RiskLevel.GREEN -> Triple(
                "Balanced & Stable Wellness Pattern",
                "Your sleep, stress, and energy indicators look steady without concerning multi-day warning trends.",
                "Maintain your current hydration, movement, and sleep consistency habits."
            )
        }

        val recs = buildPersonalizedRecommendations(latest, riskLevel, userId)

        return RiskEvaluationResult(
            riskLevel = riskLevel,
            burnoutScore = burnoutScore,
            triggerFactors = if (triggers.isEmpty()) listOf("Balanced sleep, stress & energy scores") else triggers,
            title = title,
            explanation = explanation,
            recommendedNextStep = nextStep,
            suggestedRecommendations = recs
        )
    }

    fun computeBurnoutIndex(latest: HealthCheckIn, recent: List<HealthCheckIn>): Int {
        val avgStress = recent.map { it.stressLevel.toDouble() }.average().takeIf { !it.isNaN() } ?: latest.stressLevel.toDouble()
        val avgWorkStress = recent.map { it.workStress.toDouble() }.average().takeIf { !it.isNaN() } ?: latest.workStress.toDouble()
        val avgFatigue = recent.map { it.fatigueLevel.toDouble() }.average().takeIf { !it.isNaN() } ?: latest.fatigueLevel.toDouble()
        val avgSleepDeficit = (8.0 - (recent.map { it.sleepHours }.average().takeIf { !it.isNaN() } ?: latest.sleepHours)).coerceIn(0.0, 4.0) * 2.5
        val raw = ((avgStress * 0.30 + avgWorkStress * 0.25 + avgFatigue * 0.25 + avgSleepDeficit * 0.20) * 10.0)
        return raw.toInt().coerceIn(5, 98)
    }

    fun buildAlertFromEvaluation(
        eval: RiskEvaluationResult,
        dateKey: String,
        userId: String
    ): RiskAlert? {
        if (eval.riskLevel == RiskLevel.GREEN) return null
        return RiskAlert(
            userId = userId,
            riskLevel = eval.riskLevel.name,
            title = eval.title,
            explanation = eval.explanation,
            recommendedNextStep = eval.recommendedNextStep,
            triggerFactors = eval.triggerFactors,
            dateKey = dateKey
        )
    }

    fun buildPersonalizedRecommendations(
        latest: HealthCheckIn,
        riskLevel: RiskLevel,
        userId: String
    ): List<WellnessRecommendation> {
        val list = mutableListOf<WellnessRecommendation>()

        if (latest.sleepHours < 7.0 || latest.sleepQuality <= 6) {
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.SLEEP.name,
                title = "45-Minute Digital Wind-Down",
                description = "Dim bright screens 45 minutes before bed and keep a consistent sleep window tonight.",
                frequency = RecFrequency.DAILY.name,
                source = "ENGINE"
            )
        }
        if (latest.stressLevel >= 6 || latest.workStress >= 6) {
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.STRESS.name,
                title = "4-Minute Box Breathing Reset",
                description = "Use the Mental Wellness breathing timer (4s inhale, 4s hold, 4s exhale) between work blocks.",
                frequency = RecFrequency.DAILY.name,
                source = "ENGINE"
            )
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.BREAKS.name,
                title = "90-Minute Micro-Break Cadence",
                description = "Step away from tasks for 5 minutes every 90 minutes to lower cognitive fatigue.",
                frequency = RecFrequency.DAILY.name,
                source = "ENGINE"
            )
        }
        if (latest.hydrationLiters < 2.0) {
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.HYDRATION.name,
                title = "Afternoon Hydration Anchor",
                description = "Keep a 500 ml water bottle at your workstation and finish two refills before 5 PM.",
                frequency = RecFrequency.DAILY.name,
                source = "ENGINE"
            )
        }
        if (latest.activityMinutes < 25) {
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.ACTIVITY.name,
                title = "15-Minute Brisk Recovery Walk",
                description = "Add a gentle 15-minute daylight walk to support circulation, mood, and sleep depth.",
                frequency = RecFrequency.DAILY.name,
                source = "ENGINE"
            )
        }
        if (riskLevel == RiskLevel.ORANGE || riskLevel == RiskLevel.RED) {
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.MEDICAL.name,
                title = "Consult a Healthcare Professional",
                description = "Share your exported wellness summary with a qualified clinician if symptoms or exhaustion persist.",
                frequency = RecFrequency.WEEKLY.name,
                source = "ENGINE"
            )
        }
        if (list.isEmpty()) {
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.MENTAL.name,
                title = "Evening Gratitude & Disconnect",
                description = "Write one positive takeaway from today and silence work notifications after hours.",
                frequency = RecFrequency.DAILY.name,
                source = "ENGINE"
            )
        }
        return list.take(5)
    }
}
