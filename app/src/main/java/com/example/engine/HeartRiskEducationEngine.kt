package com.example.engine

import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.HeartRiskFactorTopic
import com.example.model.RecCategory
import com.example.model.RecFrequency
import com.example.model.WellnessRecommendation

/**
 * Provides cardiovascular risk-factor education, descriptive measurement summaries
 * (strictly non-diagnostic; never computes fake clinical risk scores), and safe heart suggestions.
 */
object HeartRiskEducationEngine {

    val educationalRiskFactors: List<HeartRiskFactorTopic> = listOf(
        HeartRiskFactorTopic(
            id = "rf_bp",
            title = "High Blood Pressure (Hypertension)",
            summary = "Sustained elevated arterial pressure puts extra strain on blood vessel walls and the heart muscle over time.",
            preventiveFocus = "Monitor home BP at rest with proper cuff posture, limit excess sodium, manage stress, and stay hydrated.",
            discussWithClinician = "Bring a 7-day dated systolic/diastolic log to review target ranges appropriate for your health."
        ),
        HeartRiskFactorTopic(
            id = "rf_diabetes",
            title = "Blood Sugar & Diabetes Awareness",
            summary = "Elevated blood glucose over time can affect blood vessels and nerves that support cardiovascular function.",
            preventiveFocus = "Pair balanced fiber-rich meals with regular post-meal walking and consistent sleep schedules.",
            discussWithClinician = "Ask about periodic HbA1c, fasting glucose, and combined cardiometabolic prevention."
        ),
        HeartRiskFactorTopic(
            id = "rf_chol",
            title = "Blood Cholesterol & Lipids",
            summary = "High LDL or triglycerides and low HDL levels are associated with plaque buildup in arteries over years.",
            preventiveFocus = "Favor unsaturated fats (olive oil, nuts, seeds, fish), whole grains, vegetables, and regular aerobic movement.",
            discussWithClinician = "Review complete lipid panel values with units and reference ranges during checkups."
        ),
        HeartRiskFactorTopic(
            id = "rf_smoking",
            title = "Tobacco & Smoking Exposure",
            summary = "Tobacco smoke damages arterial lining, reduces oxygen delivery, and raises blood pressure and heart rate.",
            preventiveFocus = "Every smoke-free week measurably improves vascular recovery and resting heart workload.",
            discussWithClinician = "Ask a doctor about evidence-backed cessation support, counseling, or nicotine replacement options."
        ),
        HeartRiskFactorTopic(
            id = "rf_family",
            title = "Family Cardiovascular History",
            summary = "Early heart disease in close biological relatives can increase baseline risk, making preventive screening valuable.",
            preventiveFocus = "Keep routine wellness records organized and track modifiable lifestyle factors consistently.",
            discussWithClinician = "Share any known family cardiac history so your doctor can tailor preventive screening cadence."
        ),
        HeartRiskFactorTopic(
            id = "rf_inactivity",
            title = "Physical Inactivity & Prolonged Sitting",
            summary = "Long sedentary work shifts reduce vascular flexibility, insulin sensitivity, and cardiorespiratory fitness.",
            preventiveFocus = "Break up sitting every 60–90 minutes and build gradual moderate activity as tolerated (avoid if urgent symptoms exist).",
            discussWithClinician = "Confirm safe exercise intensity before starting vigorous training if you have cardiac symptoms or conditions."
        ),
        HeartRiskFactorTopic(
            id = "rf_weight",
            title = "Metabolic & Excess Weight Strain",
            summary = "Carrying excess visceral weight often correlates with higher blood pressure, cholesterol, and sleep apnea risk.",
            preventiveFocus = "Focus on gradual, sustainable meal balance and routine daily steps rather than extreme crash diets.",
            discussWithClinician = "Discuss individualized metabolic nutrition and safe weight management goals."
        ),
        HeartRiskFactorTopic(
            id = "rf_sleep",
            title = "Poor Sleep & Recovery Deficit",
            summary = "Chronic short sleep elevates stress hormones, night-time blood pressure, and inflammation markers.",
            preventiveFocus = "Aim for regular sleep/wake windows, morning daylight, and dark screen-free wind-down periods.",
            discussWithClinician = "Mention persistent daytime fatigue, loud snoring, or waking breathless to a healthcare provider."
        ),
        HeartRiskFactorTopic(
            id = "rf_stress",
            title = "Chronic Workplace Stress",
            summary = "Prolonged occupational stress keeps sympathetic tone elevated, contributing to palpitations and high BP readings.",
            preventiveFocus = "Practice paced breathing breaks, clear shift transitions, and emotional recovery check-ins.",
            discussWithClinician = "Seek professional care if stress triggers chest pain, persistent palpitations, or panic symptoms."
        ),
        HeartRiskFactorTopic(
            id = "rf_age",
            title = "Age-Related Vascular Changes",
            summary = "Arteries naturally stiffen with age, making periodic BP, glucose, and lipid monitoring increasingly helpful.",
            preventiveFocus = "Maintain steady preventive habits and bring longitudinal trend summaries to annual exams.",
            discussWithClinician = "Discuss validated clinical risk screening (such as Framingham/ASCVD tools applied by a clinician) during checkups."
        )
    )

    fun buildSafeHeartSuggestions(
        userId: String,
        latestMeasurement: HeartMeasurement?,
        profile: HeartHealthProfile?,
        hasUrgentSymptoms: Boolean
    ): List<WellnessRecommendation> {
        val list = mutableListOf<WellnessRecommendation>()

        if (hasUrgentSymptoms) {
            list += WellnessRecommendation(
                userId = userId,
                category = RecCategory.HEALTH_FOLLOW_UP.name,
                title = "Seek Prompt Clinical Cardiovascular Evaluation",
                description = "Because concerning symptoms are present, prioritize medical evaluation and pause strenuous physical exertion until cleared by a clinician.",
                frequency = RecFrequency.DAILY.name,
                source = "ENGINE"
            )
            return list
        }

        // Physical Activity (only when no urgent symptoms are active)
        list += WellnessRecommendation(
            userId = userId,
            category = RecCategory.PHYSICAL_ACTIVITY.name,
            title = "Gradual Zone-2 Daily Movement",
            description = "Aim for 20–30 minutes of comfortable, conversational-pace walking today, respecting your energy and joint comfort.",
            frequency = RecFrequency.DAILY.name,
            source = "ENGINE"
        )

        // Nutrition
        list += WellnessRecommendation(
            userId = userId,
            category = RecCategory.NUTRITION.name,
            title = "Heart-Balanced Meal & Sodium Awareness",
            description = "Include leafy greens, whole grains, legumes, and potassium-rich produce while moderating salty packaged snacks—no extreme diets.",
            frequency = RecFrequency.DAILY.name,
            source = "ENGINE"
        )

        // Sleep
        list += WellnessRecommendation(
            userId = userId,
            category = RecCategory.SLEEP.name,
            title = "Consistent 7–8h Cardiac Recovery Sleep",
            description = "Keep a steady bedtime and dim screens 45 minutes prior to support healthy nocturnal blood pressure dipping.",
            frequency = RecFrequency.DAILY.name,
            source = "ENGINE"
        )

        // Stress
        list += WellnessRecommendation(
            userId = userId,
            category = RecCategory.STRESS.name,
            title = "Paced Breathing & Shift Micro-Breaks",
            description = "Take a 4-minute calm breathing pause between demanding tasks; contact a support counselor if stress remains persistent.",
            frequency = RecFrequency.DAILY.name,
            source = "ENGINE"
        )

        // Health Follow-up
        val followUpDetail = when {
            latestMeasurement?.systolicMmHg != null && latestMeasurement.systolicMmHg >= 130 ->
                "Your entered BP reading (${latestMeasurement.systolicMmHg}/${latestMeasurement.diastolicMmHg ?: "--"} mmHg) is worth tracking at rest and sharing with a primary care doctor or cardiologist."
            profile?.familyHeartHistory == true || !profile?.knownConditions.isNullOrEmpty() ->
                "Prepare questions and a 7-day measurement log for your next scheduled preventive cardiology or primary care visit."
            else ->
                "Use Medical Appointment Prep to organize your resting HR, BP log, medications, and questions before your routine checkup."
        }
        list += WellnessRecommendation(
            userId = userId,
            category = RecCategory.HEALTH_FOLLOW_UP.name,
            title = "Clinician Checkup & Trend Review",
            description = followUpDetail,
            frequency = RecFrequency.WEEKLY.name,
            source = "ENGINE"
        )

        return list
    }
}
