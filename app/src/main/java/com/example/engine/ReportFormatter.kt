package com.example.engine

import com.example.model.BloodPressureRecord
import com.example.model.HealthCheckIn
import com.example.model.HealthReportUpload
import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.HeartSymptomReport
import com.example.model.MedicalSafetyEvent
import com.example.model.RiskAlert
import com.example.model.UserProfile
import com.example.model.WellnessRecommendation
import org.json.JSONArray
import org.json.JSONObject

object ReportFormatter {
    fun buildPersonalWellnessReport(
        profile: UserProfile?,
        checkIns: List<HealthCheckIn>,
        alerts: List<RiskAlert>,
        recommendations: List<WellnessRecommendation>,
        periodDays: Int,
        isDemo: Boolean
    ): String {
        val scoped = checkIns.take(periodDays)
        val avgWellbeing = scoped.map { it.overallWellbeing }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgSleep = scoped.map { it.sleepHours }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgStress = scoped.map { it.stressLevel }.average().takeIf { !it.isNaN() } ?: 0.0
        val avgFatigue = scoped.map { it.fatigueLevel }.average().takeIf { !it.isNaN() } ?: 0.0

        return buildString {
            appendLine("YAQEEN ALLAH WORKFORCE HEALTH AI")
            appendLine("Personal Wellness & Early-Warning Summary")
            if (isDemo) appendLine("[FICTIONAL DEMO DATA — FOR EVALUATION ONLY]")
            appendLine("==========================================")
            appendLine("Member: ${profile?.displayName ?: "Team Member"} (${profile?.department ?: "Operations"})")
            appendLine("Period: Last ${scoped.size} Check-Ins (Window: $periodDays days)")
            appendLine()
            appendLine("SUMMARY AVERAGES")
            appendLine("- Overall Wellbeing: ${String.format("%.1f", avgWellbeing)} / 10")
            appendLine("- Sleep Duration:    ${String.format("%.1f", avgSleep)} hrs/night")
            appendLine("- Stress Level:      ${String.format("%.1f", avgStress)} / 10")
            appendLine("- Fatigue Level:     ${String.format("%.1f", avgFatigue)} / 10")
            appendLine()
            appendLine("CHECK-IN HISTORY")
            if (scoped.isEmpty()) {
                appendLine("- No check-ins recorded in selected window.")
            } else {
                scoped.forEach { ci ->
                    appendLine("- ${ci.dateKey}: Wellbeing ${ci.overallWellbeing}/10 | Sleep ${ci.sleepHours}h | Stress ${ci.stressLevel}/10 | Risk ${ci.computedRiskLevel}")
                    if (ci.symptoms.isNotEmpty()) {
                        appendLine("  Symptoms: ${ci.symptoms.joinToString(", ")}")
                    }
                }
            }
            appendLine()
            appendLine("EARLY WARNING ALERTS (${alerts.size})")
            if (alerts.isEmpty()) {
                appendLine("- None recorded.")
            } else {
                alerts.take(5).forEach { alert ->
                    appendLine("- [${alert.riskLevel} · ${alert.status}] ${alert.title} (${alert.dateKey})")
                }
            }
            appendLine()
            appendLine("PREVENTIVE RECOMMENDATIONS")
            recommendations.take(6).forEach { rec ->
                val mark = if (rec.completed) "[Done]" else "[Open]"
                appendLine("- $mark ${rec.category}: ${rec.title} — ${rec.description}")
            }
            appendLine()
            appendLine("IMPORTANT DISCLAIMER")
            appendLine("This report provides non-diagnostic wellness and prevention information only. It does not constitute medical diagnosis or treatment. Consult a qualified healthcare professional for medical advice.")
        }
    }

    fun buildHeartHealthReport(
        profile: UserProfile?,
        heartProfile: HeartHealthProfile?,
        measurements: List<HeartMeasurement>,
        bpRecords: List<BloodPressureRecord>,
        symptomReports: List<HeartSymptomReport>,
        safetyEvents: List<MedicalSafetyEvent>,
        isDemo: Boolean
    ): String = buildString {
        appendLine("YAQEEN AI CARDIOLOGY ASSISTANT")
        appendLine("Personal Cardiovascular & Vital Log Summary")
        if (isDemo) appendLine("[FICTIONAL DEMO DATA — FOR EVALUATION ONLY]")
        appendLine("==========================================")
        appendLine("Member: ${profile?.displayName ?: "Team Member"} (${profile?.department ?: "Operations"})")
        heartProfile?.let { hp ->
            if (hp.ageYears != null) appendLine("Age (User-Provided): ${hp.ageYears} yrs")
            if (hp.knownConditions.isNotEmpty()) appendLine("Known Conditions: ${hp.knownConditions.joinToString(", ")}")
            if (hp.medications.isNotEmpty()) appendLine("Medications: ${hp.medications.joinToString(", ")}")
            if (hp.allergies.isNotEmpty()) appendLine("Allergies: ${hp.allergies.joinToString(", ")}")
            appendLine("Smoking Status: ${hp.smokingStatus} | Family Heart History: ${hp.familyHeartHistory}")
        }
        appendLine()
        appendLine("USER-ENTERED HEART & METABOLIC MEASUREMENTS (${measurements.size})")
        if (measurements.isEmpty()) {
            appendLine("- No heart measurements recorded yet.")
        } else {
            measurements.take(15).forEach { m ->
                val parts = mutableListOf<String>()
                m.restingHeartRateBpm?.let { parts += "HR ${it} bpm" }
                if (m.systolicMmHg != null && m.diastolicMmHg != null) {
                    parts += "BP ${m.systolicMmHg}/${m.diastolicMmHg} mmHg"
                }
                m.oxygenSaturationPct?.let { parts += "SpO2 ${it}%" }
                m.weightKg?.let { parts += "Weight ${it} kg" }
                m.bloodGlucoseMgDl?.let { parts += "Glucose ${it} mg/dL" }
                m.totalCholesterolMgDl?.let { parts += "Total Chol ${it} mg/dL" }
                m.ldlCholesterolMgDl?.let { parts += "LDL ${it} mg/dL" }
                m.hdlCholesterolMgDl?.let { parts += "HDL ${it} mg/dL" }
                appendLine("- [${m.sourceLabel}] ${m.recordedAtIso} (${m.activityLevel}): ${parts.joinToString(" | ")}")
                if (m.symptoms.isNotEmpty()) appendLine("  Symptoms: ${m.symptoms.joinToString(", ")}")
                if (!m.notes.isNullOrBlank()) appendLine("  Notes: ${m.notes}")
            }
        }
        appendLine()
        appendLine("BLOOD PRESSURE LOG (${bpRecords.size})")
        if (bpRecords.isEmpty()) {
            appendLine("- No standalone BP records.")
        } else {
            bpRecords.take(10).forEach { bp ->
                appendLine("- [${bp.sourceLabel}] ${bp.recordedAtIso}: ${bp.systolicMmHg}/${bp.diastolicMmHg} mmHg (${bp.postureOrContext})")
            }
        }
        appendLine()
        appendLine("SYMPTOM SCREENING & SAFETY LOGS")
        if (symptomReports.isEmpty() && safetyEvents.isEmpty()) {
            appendLine("- No symptom checks or emergency flags recorded.")
        } else {
            symptomReports.take(5).forEach { s ->
                appendLine("- [${s.urgencyLevel.uppercase()}] Symptoms: ${s.symptoms.joinToString(", ")} | Severity: ${s.severityScore}/10 | Duration: ${s.durationLabel}")
            }
            safetyEvents.take(5).forEach { ev ->
                appendLine("- [SAFETY EVENT - ${ev.sourceModule}] ${ev.reason} (${ev.actionTaken})")
            }
        }
        appendLine()
        appendLine("IMPORTANT DISCLAIMER")
        appendLine("All vitals listed above reflect user-entered records (or fictional demo records where labeled). Yaqeen AI Cardiology Assistant provides cardiovascular health education only, is not a cardiologist, and does not diagnose heart conditions.")
    }

    fun buildExportJson(
        profile: UserProfile?,
        checkIns: List<HealthCheckIn>,
        alerts: List<RiskAlert>,
        heartMeasurements: List<HeartMeasurement> = emptyList(),
        bpRecords: List<BloodPressureRecord> = emptyList(),
        reportUploads: List<HealthReportUpload> = emptyList(),
        safetyEvents: List<MedicalSafetyEvent> = emptyList(),
        isDemo: Boolean = false
    ): String {
        val root = JSONObject()
        root.put("platform", "Yaqeen Health Workforce AI")
        root.put("isDemoData", isDemo)
        root.put("displayName", profile?.displayName ?: "")
        root.put("department", profile?.department ?: "")
        root.put("language", profile?.language ?: "EN")
        val ciArr = JSONArray()
        checkIns.forEach { ci ->
            ciArr.put(
                JSONObject().apply {
                    put("dateKey", ci.dateKey)
                    put("overallWellbeing", ci.overallWellbeing)
                    put("sleepQuality", ci.sleepQuality)
                    put("sleepHours", ci.sleepHours)
                    put("stressLevel", ci.stressLevel)
                    put("energyLevel", ci.energyLevel)
                    put("fatigueLevel", ci.fatigueLevel)
                    put("mood", ci.mood)
                    put("riskLevel", ci.computedRiskLevel)
                }
            )
        }
        root.put("checkIns", ciArr)
        val alertArr = JSONArray()
        alerts.forEach { a ->
            alertArr.put(
                JSONObject().apply {
                    put("dateKey", a.dateKey)
                    put("riskLevel", a.riskLevel)
                    put("title", a.title)
                    put("status", a.status)
                }
            )
        }
        root.put("alerts", alertArr)

        val heartArr = JSONArray()
        heartMeasurements.forEach { m ->
            heartArr.put(
                JSONObject().apply {
                    put("recordedAtIso", m.recordedAtIso)
                    put("sourceLabel", m.sourceLabel)
                    m.restingHeartRateBpm?.let { put("restingHeartRateBpm", it) }
                    m.systolicMmHg?.let { put("systolicMmHg", it) }
                    m.diastolicMmHg?.let { put("diastolicMmHg", it) }
                    m.oxygenSaturationPct?.let { put("oxygenSaturationPct", it) }
                    m.weightKg?.let { put("weightKg", it) }
                    m.bloodGlucoseMgDl?.let { put("bloodGlucoseMgDl", it) }
                    m.totalCholesterolMgDl?.let { put("totalCholesterolMgDl", it) }
                    put("activityLevel", m.activityLevel)
                    put("emergencyFlagged", m.emergencyFlagged)
                }
            )
        }
        root.put("heartMeasurements", heartArr)

        val bpArr = JSONArray()
        bpRecords.forEach { bp ->
            bpArr.put(
                JSONObject().apply {
                    put("recordedAtIso", bp.recordedAtIso)
                    put("systolicMmHg", bp.systolicMmHg)
                    put("diastolicMmHg", bp.diastolicMmHg)
                    put("postureOrContext", bp.postureOrContext)
                    put("sourceLabel", bp.sourceLabel)
                }
            )
        }
        root.put("bloodPressureRecords", bpArr)

        val uploadArr = JSONArray()
        reportUploads.forEach { r ->
            uploadArr.put(
                JSONObject().apply {
                    put("title", r.title)
                    put("reportType", r.reportType)
                    put("summary", r.summary)
                    put("outOfRangeFindings", JSONArray(r.outOfRangeFindings))
                }
            )
        }
        root.put("healthReportUploads", uploadArr)

        val safetyArr = JSONArray()
        safetyEvents.forEach { ev ->
            safetyArr.put(
                JSONObject().apply {
                    put("sourceModule", ev.sourceModule)
                    put("reason", ev.reason)
                    put("actionTaken", ev.actionTaken)
                }
            )
        }
        root.put("medicalSafetyEvents", safetyArr)
        return root.toString(2)
    }
}
