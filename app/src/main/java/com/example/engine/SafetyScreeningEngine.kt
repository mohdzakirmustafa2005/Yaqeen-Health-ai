package com.example.engine

import com.example.model.EmergencyScreenResult
import com.example.model.HeartMeasurement

/**
 * Independent Safety Screening Engine executed BEFORE any AI generation.
 * Detects cardiac, neurological (stroke), respiratory, anaphylactic, or acute crisis warning signs
 * and halts normal wellness/exercise AI flows immediately.
 */
object SafetyScreeningEngine {

    private val symptomRules: List<Pair<String, List<String>>> = listOf(
        "Chest pain / pressure / squeezing / tightness" to listOf(
            "chest pain", "chest pressure", "chest tightness", "chest squeezing",
            "crushing chest", "chest discomfort", "pain in my chest", "tight chest",
            "radiating to arm", "radiating to jaw", "left arm pain", "jaw pain with chest"
        ),
        "Severe or sudden breathing difficulty" to listOf(
            "shortness of breath", "difficulty breathing", "trouble breathing",
            "can't breathe", "cannot breathe", "gasping for air", "sudden breathlessness",
            "severe breathlessness"
        ),
        "Fainting / loss of consciousness" to listOf(
            "fainting", "fainted", "passed out", "loss of consciousness", "syncope", "blacked out"
        ),
        "Stroke warning signs (weakness, numbness, speech, confusion)" to listOf(
            "sudden weakness", "one-sided weakness", "facial droop", "face drooping",
            "slurred speech", "trouble speaking", "sudden numbness", "arm weakness",
            "sudden confusion", "sudden vision loss"
        ),
        "Severe dizziness with concerning symptoms" to listOf(
            "severe dizziness", "spinning and chest", "dizzy and faint", "near fainting"
        ),
        "Severe allergic reaction signs" to listOf(
            "throat swelling", "tongue swelling", "lips swelling", "anaphylaxis",
            "severe allergic reaction", "wheezing after allergy"
        ),
        "Acute self-harm or crisis concern" to listOf(
            "self-harm", "suicidal", "want to die", "hurt myself", "end my life"
        )
    )

    private val urgentCheckboxLabels = setOf(
        "Chest pain",
        "Chest pressure or tightness",
        "Chest discomfort",
        "Shortness of breath",
        "Fainting or near-fainting",
        "Sudden weakness or numbness",
        "Confusion or slurred speech",
        "Severe dizziness",
        "Throat or facial swelling"
    )

    fun screenTextAndSymptoms(
        text: String,
        selectedSymptoms: List<String> = emptyList(),
        latestMeasurement: HeartMeasurement? = null
    ): EmergencyScreenResult {
        val lower = text.lowercase()
        val matched = linkedSetOf<String>()

        selectedSymptoms.forEach { sym ->
            if (sym in urgentCheckboxLabels || symptomRules.any { (_, kws) -> kws.any { sym.lowercase().contains(it) } }) {
                matched.add(sym)
            }
        }

        symptomRules.forEach { (label, keywords) ->
            if (keywords.any { lower.contains(it) }) {
                matched.add(label)
            }
        }

        latestMeasurement?.let { m ->
            val sys = m.systolicMmHg
            val dia = m.diastolicMmHg
            if ((sys != null && sys >= 180L) || (dia != null && dia >= 120L)) {
                if (matched.isNotEmpty() || lower.contains("headache") || lower.contains("vision") || lower.contains("chest")) {
                    matched.add("Severely elevated BP ($sys/$dia mmHg) with symptoms")
                }
            }
            val spo2 = m.oxygenSaturationPct
            if (spo2 != null && spo2 < 90L) {
                matched.add("Low oxygen saturation reading ($spo2%)")
            }
            val hr = m.restingHeartRateBpm
            if (hr != null && (hr >= 150L || hr < 38L) && matched.isNotEmpty()) {
                matched.add("Extreme heart rate reading ($hr bpm) with symptoms")
            }
        }

        if (matched.isEmpty()) {
            return EmergencyScreenResult(triggered = false)
        }

        val reason = "Detected high-priority warning indicators: ${matched.joinToString(", ")}."
        return EmergencyScreenResult(
            triggered = true,
            reason = reason,
            matchedSignals = matched.toList(),
            guidance = buildEmergencyGuidance(matched.toList())
        )
    }

    fun screenVitalThresholds(measurement: HeartMeasurement): EmergencyScreenResult {
        val signals = mutableListOf<String>()
        val sys = measurement.systolicMmHg
        val dia = measurement.diastolicMmHg
        if ((sys != null && sys >= 180L) || (dia != null && dia >= 120L)) {
            signals += "Blood Pressure ≥ 180/120 mmHg ($sys/${dia ?: "?"})"
        }
        val spo2 = measurement.oxygenSaturationPct
        if (spo2 != null && spo2 < 90L) {
            signals += "Oxygen saturation below 90% ($spo2%)"
        }
        val hr = measurement.restingHeartRateBpm
        if (hr != null && (hr >= 150L || hr < 38L)) {
            signals += "Resting Heart Rate extreme value ($hr bpm)"
        }
        measurement.symptoms.forEach { sym ->
            if (sym in urgentCheckboxLabels) signals += sym
        }
        val noteCheck = screenTextAndSymptoms(measurement.notes.orEmpty(), measurement.symptoms)
        if (noteCheck.triggered) {
            signals.addAll(noteCheck.matchedSignals)
        }
        val unique = signals.distinct()
        return if (unique.isEmpty()) {
            EmergencyScreenResult(triggered = false)
        } else {
            EmergencyScreenResult(
                triggered = true,
                reason = "Entered vitals or symptoms include urgent warning indicators: ${unique.joinToString(", ")}.",
                matchedSignals = unique,
                guidance = buildEmergencyGuidance(unique)
            )
        }
    }

    fun buildEmergencyGuidance(signals: List<String>): String = buildString {
        appendLine("URGENT MEDICAL ATTENTION ADVISED — DO NOT WAIT FOR AN AI RESPONSE")
        appendLine()
        appendLine("Possible emergency warning signs detected (${signals.joinToString(", ")}).")
        appendLine("• Call emergency services immediately: 112 (India Emergency) or your local emergency number (e.g., 911 / 999 / 108).")
        appendLine("• Have someone stay with you or go to the nearest emergency hospital right away. Do not drive yourself if feeling unwell.")
        appendLine("• Never ignore chest pain, pressure, shortness of breath, fainting, or sudden weakness—these require urgent in-person clinical evaluation, never exercise or home remedies.")
        appendLine()
        append("AI wellness guidance has been paused automatically for your safety.")
    }
}
