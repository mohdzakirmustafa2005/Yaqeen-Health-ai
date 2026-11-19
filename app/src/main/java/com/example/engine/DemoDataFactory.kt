package com.example.engine

import com.example.model.AiChatMessage
import com.example.model.AiDoctorMode
import com.example.model.AiHealthInsightRecord
import com.example.model.AiSender
import com.example.model.AiTaskType
import com.example.model.AlertStatus
import com.example.model.AuditLogEntry
import com.example.model.BloodPressureRecord
import com.example.model.CoordinatorOutreachCase
import com.example.model.DepartmentAggregate
import com.example.model.DoctorAiMessage
import com.example.model.DoctorConversation
import com.example.model.DoctorDomain
import com.example.model.HealthCheckIn
import com.example.model.HealthReportUpload
import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.HeartSymptomReport
import com.example.model.MedicalSafetyEvent
import com.example.model.MoodOption
import com.example.model.NotifCategory
import com.example.model.RecCategory
import com.example.model.RecFrequency
import com.example.model.RiskAlert
import com.example.model.RiskLevel
import com.example.model.SmartNotification
import com.example.model.UserConsentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.WellnessRecommendation
import com.example.model.WorkforceAnalyticsSummary
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Provides clearly labeled FICTIONAL sample data for Judge Demo Mode
 * and privacy-preserving workforce demo analytics.
 */
object DemoDataFactory {
    const val DEMO_UID = "demo_judge_user"

    fun demoProfile(role: UserRole = UserRole.INDIVIDUAL): UserProfile = UserProfile(
        id = DEMO_UID,
        userId = DEMO_UID,
        displayName = "Zara Siddiqui (Demo)",
        department = "Field Operations",
        organizationId = "crescent-health-demo",
        role = role.name,
        language = "EN",
        consentAnalytics = true,
        consentAiGuidance = true,
        consentCoordinatorOutreach = true,
        remindersEnabled = true,
        reminderHour = 9L,
        reminderMinute = 0L,
        onboardingCompleted = true
    )

    fun demoCheckIns(): List<HealthCheckIn> {
        val nowMs = System.currentTimeMillis()
        fun tsDaysAgo(days: Int) = Timestamp(Date(nowMs - days * 86_400_000L))

        return listOf(
            HealthCheckIn(
                id = "demo_ci_7",
                userId = DEMO_UID,
                dateKey = "2026-11-19",
                overallWellbeing = 6L,
                sleepQuality = 5L,
                sleepHours = 5.8,
                stressLevel = 7L,
                energyLevel = 5L,
                fatigueLevel = 7L,
                mood = MoodOption.TIRED.name,
                symptoms = listOf("Eye strain", "Shoulder tension"),
                workStress = 8L,
                hydrationLiters = 1.5,
                activityMinutes = 20L,
                notes = "Long sprint shift; felt afternoon dip after 3 PM.",
                computedRiskLevel = RiskLevel.ORANGE.name,
                aiSummary = "Fictional Demo Insight: Rising work stress (8/10) paired with <6h sleep is driving mild burnout strain. Prioritize screen wind-down and a 15-minute recovery walk.",
                createdAt = tsDaysAgo(0),
                updatedAt = tsDaysAgo(0)
            ),
            HealthCheckIn(
                id = "demo_ci_6",
                userId = DEMO_UID,
                dateKey = "2026-11-18",
                overallWellbeing = 6L,
                sleepQuality = 5L,
                sleepHours = 6.0,
                stressLevel = 7L,
                energyLevel = 6L,
                fatigueLevel = 6L,
                mood = MoodOption.ANXIOUS.name,
                symptoms = listOf("Mild headache"),
                workStress = 7L,
                hydrationLiters = 1.7,
                activityMinutes = 25L,
                notes = "Back-to-back coordination calls.",
                computedRiskLevel = RiskLevel.YELLOW.name,
                createdAt = tsDaysAgo(1),
                updatedAt = tsDaysAgo(1)
            ),
            HealthCheckIn(
                id = "demo_ci_5",
                userId = DEMO_UID,
                dateKey = "2026-11-17",
                overallWellbeing = 7L,
                sleepQuality = 6L,
                sleepHours = 6.4,
                stressLevel = 6L,
                energyLevel = 6L,
                fatigueLevel = 5L,
                mood = MoodOption.CALM.name,
                symptoms = emptyList(),
                workStress = 6L,
                hydrationLiters = 2.0,
                activityMinutes = 30L,
                computedRiskLevel = RiskLevel.YELLOW.name,
                createdAt = tsDaysAgo(2),
                updatedAt = tsDaysAgo(2)
            ),
            HealthCheckIn(
                id = "demo_ci_4",
                userId = DEMO_UID,
                dateKey = "2026-11-16",
                overallWellbeing = 8L,
                sleepQuality = 7L,
                sleepHours = 7.2,
                stressLevel = 4L,
                energyLevel = 8L,
                fatigueLevel = 3L,
                mood = MoodOption.OPTIMISTIC.name,
                symptoms = emptyList(),
                workStress = 4L,
                hydrationLiters = 2.3,
                activityMinutes = 40L,
                computedRiskLevel = RiskLevel.GREEN.name,
                createdAt = tsDaysAgo(3),
                updatedAt = tsDaysAgo(3)
            ),
            HealthCheckIn(
                id = "demo_ci_3",
                userId = DEMO_UID,
                dateKey = "2026-11-15",
                overallWellbeing = 8L,
                sleepQuality = 8L,
                sleepHours = 7.6,
                stressLevel = 3L,
                energyLevel = 8L,
                fatigueLevel = 3L,
                mood = MoodOption.CALM.name,
                symptoms = emptyList(),
                workStress = 3L,
                hydrationLiters = 2.4,
                activityMinutes = 45L,
                computedRiskLevel = RiskLevel.GREEN.name,
                createdAt = tsDaysAgo(4),
                updatedAt = tsDaysAgo(4)
            ),
            HealthCheckIn(
                id = "demo_ci_2",
                userId = DEMO_UID,
                dateKey = "2026-11-14",
                overallWellbeing = 7L,
                sleepQuality = 7L,
                sleepHours = 7.0,
                stressLevel = 5L,
                energyLevel = 7L,
                fatigueLevel = 4L,
                mood = MoodOption.CALM.name,
                symptoms = emptyList(),
                workStress = 5L,
                hydrationLiters = 2.1,
                activityMinutes = 35L,
                computedRiskLevel = RiskLevel.GREEN.name,
                createdAt = tsDaysAgo(5),
                updatedAt = tsDaysAgo(5)
            ),
            HealthCheckIn(
                id = "demo_ci_1",
                userId = DEMO_UID,
                dateKey = "2026-11-13",
                overallWellbeing = 7L,
                sleepQuality = 6L,
                sleepHours = 6.8,
                stressLevel = 5L,
                energyLevel = 7L,
                fatigueLevel = 5L,
                mood = MoodOption.OPTIMISTIC.name,
                symptoms = emptyList(),
                workStress = 5L,
                hydrationLiters = 1.9,
                activityMinutes = 25L,
                computedRiskLevel = RiskLevel.GREEN.name,
                createdAt = tsDaysAgo(6),
                updatedAt = tsDaysAgo(6)
            )
        )
    }

    fun demoAlerts(): List<RiskAlert> = listOf(
        RiskAlert(
            id = "demo_alert_1",
            userId = DEMO_UID,
            riskLevel = RiskLevel.ORANGE.name,
            title = "Rising Burnout & Sleep Deficit Pattern (Fictional Demo)",
            explanation = "Sleep dropped below 6 hours while work stress stayed >= 7/10 across consecutive check-ins.",
            recommendedNextStep = "Take scheduled micro-breaks, wind down screens 45m early, and consult a wellness coordinator if exhaustion continues.",
            triggerFactors = listOf(
                "Short sleep (5.8h)",
                "Work stress 8/10",
                "High fatigue (7/10)",
                "Declining 3-day wellbeing trend"
            ),
            dateKey = "2026-11-19",
            status = AlertStatus.ACTIVE.name
        ),
        RiskAlert(
            id = "demo_alert_2",
            userId = DEMO_UID,
            riskLevel = RiskLevel.YELLOW.name,
            title = "Mild Hydration & Tension Drift (Fictional Demo)",
            explanation = "Hydration dipped to 1.7L with mild tension headache reported.",
            recommendedNextStep = "Keep water nearby and stretch shoulders every 90 minutes.",
            triggerFactors = listOf("Hydration 1.7L", "Mild headache"),
            dateKey = "2026-11-18",
            status = AlertStatus.ACKNOWLEDGED.name
        )
    )

    fun demoRecommendations(): List<WellnessRecommendation> = listOf(
        WellnessRecommendation(
            id = "demo_rec_1",
            userId = DEMO_UID,
            category = RecCategory.SLEEP.name,
            title = "Consistent 10:30 PM Wind-Down",
            description = "Dim devices 45 minutes before sleep to rebuild deep recovery past 7 hours.",
            frequency = RecFrequency.DAILY.name,
            completed = false,
            source = "ENGINE"
        ),
        WellnessRecommendation(
            id = "demo_rec_2",
            userId = DEMO_UID,
            category = RecCategory.STRESS.name,
            title = "4-Minute Box Breathing Between Shifts",
            description = "Use the guided breathing timer to lower acute work tension.",
            frequency = RecFrequency.DAILY.name,
            completed = true,
            source = "AI"
        ),
        WellnessRecommendation(
            id = "demo_rec_3",
            userId = DEMO_UID,
            category = RecCategory.HYDRATION.name,
            title = "2.2L Daily Hydration Target",
            description = "Drink one glass of water at check-in and one after lunch.",
            frequency = RecFrequency.DAILY.name,
            completed = false,
            source = "ENGINE"
        ),
        WellnessRecommendation(
            id = "demo_rec_4",
            userId = DEMO_UID,
            category = RecCategory.BREAKS.name,
            title = "Posture & Eye-Strain 20-20-20 Reset",
            description = "Look 20 feet away for 20 seconds every 20 minutes to ease shoulder and eye strain.",
            frequency = RecFrequency.DAILY.name,
            completed = false,
            source = "AI"
        ),
        WellnessRecommendation(
            id = "demo_rec_5",
            userId = DEMO_UID,
            category = RecCategory.MEDICAL.name,
            title = "Optional Nurse Coordinator Check-In",
            description = "If fatigue persists past 5 days, schedule a preventive wellness consultation.",
            frequency = RecFrequency.WEEKLY.name,
            completed = false,
            source = "ENGINE"
        )
    )

    fun demoAiMessages(): List<AiChatMessage> = listOf(
        AiChatMessage(
            id = "demo_msg_1",
            userId = DEMO_UID,
            sender = AiSender.ASSISTANT.name,
            text = "Welcome to Yaqeen Health Demo Mode (fictional sample data). Based on your 7-day log, sleep dipped from 7.6h to 5.8h while work stress rose to 8/10. Ask me about sleep recovery, ergonomic breaks, or when to seek clinical care. (Non-diagnostic wellness guidance only.)",
            language = "EN",
            fallbackUsed = false
        )
    )

    fun demoNotifications(): List<SmartNotification> = listOf(
        SmartNotification(
            id = "demo_notif_1",
            userId = DEMO_UID,
            category = NotifCategory.ALERT.name,
            title = "Early Warning: Orange Strain Pattern",
            body = "Work stress & sleep deficit triggered a preventive follow-up suggestion.",
            readStatus = false
        ),
        SmartNotification(
            id = "demo_notif_2",
            userId = DEMO_UID,
            category = NotifCategory.REMINDER.name,
            title = "Daily Check-In Reminder",
            body = "Your 9:00 AM daily wellness check-in keeps longitudinal trends accurate.",
            readStatus = false
        ),
        SmartNotification(
            id = "demo_notif_3",
            userId = DEMO_UID,
            category = NotifCategory.SUMMARY.name,
            title = "Weekly Wellness Summary Ready",
            body = "View and export your 7-day trend report in Trends & Reports.",
            readStatus = true
        )
    )

    fun demoAuditLogs(): List<AuditLogEntry> = listOf(
        AuditLogEntry(
            id = "demo_log_1",
            userId = DEMO_UID,
            action = "CHECK_IN_SUBMITTED",
            details = "Recorded check-in 2026-11-19 (Risk: ORANGE).",
            actorRole = UserRole.INDIVIDUAL.name
        ),
        AuditLogEntry(
            id = "demo_log_2",
            userId = DEMO_UID,
            action = "PRIVACY_AGGREGATION_GUARD",
            details = "Suppressed Executive Support dept (n=3 < min threshold 5) from org dashboard.",
            actorRole = UserRole.ORG_ADMIN.name
        ),
        AuditLogEntry(
            id = "demo_log_3",
            userId = DEMO_UID,
            action = "CONSENT_VERIFIED",
            details = "User opted into anonymized aggregate analytics & AI guidance.",
            actorRole = UserRole.INDIVIDUAL.name
        )
    )

    fun demoWorkforceAnalytics(): WorkforceAnalyticsSummary = WorkforceAnalyticsSummary(
        organizationName = "Crescent Health & Tech Workforce (Fictional Demo)",
        totalConsentedMembers = 64,
        participationRatePercent = 84,
        avgWellbeingScore = 7.2f,
        greenCount = 38,
        yellowCount = 16,
        orangeCount = 8,
        redCount = 2,
        departments = listOf(
            DepartmentAggregate("Field Operations", 24, 88, 6.6f, 6.4f, 6.2f, 6.1f, 21),
            DepartmentAggregate("Engineering & Product", 19, 82, 7.3f, 5.6f, 6.8f, 5.1f, 11),
            DepartmentAggregate("Care & Support", 18, 86, 7.8f, 4.7f, 7.2f, 4.3f, 6),
            // Intentionally < MIN_GROUP_SIZE (5) to demonstrate privacy re-identification suppression!
            DepartmentAggregate("Executive Support", 3, 100, 0f, 0f, 0f, 0f, 0)
        ),
        isDemoData = true
    )

    fun demoCoordinatorCases(): List<CoordinatorOutreachCase> = listOf(
        CoordinatorOutreachCase(
            caseId = "CASE-2041",
            anonymizedAlias = "Member #A48 (Consented)",
            department = "Field Operations",
            consentGranted = true,
            riskLevel = RiskLevel.ORANGE,
            triggerSummary = "3-day sleep < 6h + high work stress (8/10)",
            lastCheckInDate = "2026-11-19",
            outreachNote = "Shared ergonomic break schedule & invited to optional wellness call.",
            status = "Follow-Up Scheduled"
        ),
        CoordinatorOutreachCase(
            caseId = "CASE-2038",
            anonymizedAlias = "Member #C19 (Consented)",
            department = "Engineering & Product",
            consentGranted = true,
            riskLevel = RiskLevel.RED,
            triggerSummary = "Reported persistent dizziness + severe fatigue",
            lastCheckInDate = "2026-11-19",
            outreachNote = "Recommended same-day primary care clinical evaluation.",
            status = "Urgent Outreach Sent"
        ),
        CoordinatorOutreachCase(
            caseId = "CASE-2029",
            anonymizedAlias = "Identity Protected (No Outreach Consent)",
            department = "Care & Support",
            consentGranted = false,
            riskLevel = RiskLevel.YELLOW,
            triggerSummary = "Aggregate-only alert; direct contact withheld per privacy consent.",
            lastCheckInDate = "2026-11-18",
            outreachNote = "Automated in-app self-care guidance delivered privately.",
            status = "Privacy Protected"
        )
    )

    const val sampleDemoEcgText = """FICTIONAL DEMO ECG REPORT (FOR EVALUATION ONLY)
Rate: 76 bpm | PR Interval: 162 ms (Normal: 120–200 ms) | QRS Duration: 88 ms (Normal: <120 ms)
QT/QTc: 398 / 422 ms | Rhythm: Normal sinus rhythm
Note: Mild non-specific T-wave flattening noted in lead III; clinical correlation by physician advised if symptoms exist."""

    const val sampleDemoLipidText = """FICTIONAL DEMO CARDIAC & METABOLIC LAB PANEL (FOR EVALUATION ONLY)
Total Cholesterol: 214 mg/dL (Desirable: <200 mg/dL) [High]
LDL Cholesterol: 138 mg/dL (Optimal: <100 mg/dL) [Borderline High]
HDL Cholesterol: 44 mg/dL (Reference: >=40 mg/dL) [Normal]
Triglycerides: 162 mg/dL (Normal: <150 mg/dL) [Mildly Elevated]
Fasting Blood Glucose: 106 mg/dL (Normal: 70–99 mg/dL) [Elevated Fasting]"""

    fun demoDoctorConversations(): List<DoctorConversation> {
        val nowMs = System.currentTimeMillis()
        fun tsHoursAgo(h: Long) = Timestamp(Date(nowMs - h * 3_600_000L))
        return listOf(
            DoctorConversation(
                id = "demo_conv_doc_1",
                userId = DEMO_UID,
                domain = DoctorDomain.DOCTOR.name,
                mode = AiDoctorMode.GENERAL_HEALTH.name,
                title = "Afternoon Fatigue & Hydration Questions (Demo)",
                lastPreview = "Reviewing sleep recovery, hydration targets, and when to check basic labs.",
                messageCount = 2L,
                createdAt = tsHoursAgo(12),
                updatedAt = tsHoursAgo(12)
            ),
            DoctorConversation(
                id = "demo_conv_heart_1",
                userId = DEMO_UID,
                domain = DoctorDomain.HEART_SPECIALIST.name,
                mode = AiDoctorMode.HEART_SPECIALIST.name,
                title = "Home Blood Pressure Tracking Guide (Demo)",
                lastPreview = "Explaining cuff posture, resting pulse, and questions for your physician.",
                messageCount = 2L,
                createdAt = tsHoursAgo(5),
                updatedAt = tsHoursAgo(5)
            )
        )
    }

    fun demoDoctorMessages(): List<DoctorAiMessage> = listOf(
        DoctorAiMessage(
            id = "demo_dmsg_1",
            userId = DEMO_UID,
            conversationId = "demo_conv_doc_1",
            sender = "USER",
            mode = AiDoctorMode.GENERAL_HEALTH.name,
            taskType = AiTaskType.GENERAL_HEALTH_ASSISTANT.name,
            text = "I feel tired around 3 PM after short nights of sleep. What lifestyle questions should I look at?",
            providerUsed = "User"
        ),
        DoctorAiMessage(
            id = "demo_dmsg_2",
            userId = DEMO_UID,
            conversationId = "demo_conv_doc_1",
            sender = "ASSISTANT",
            mode = AiDoctorMode.GENERAL_HEALTH.name,
            taskType = AiTaskType.GENERAL_HEALTH_ASSISTANT.name,
            text = "Fictional Demo Guidance: Afternoon fatigue often coincides with <6.5h sleep, dehydration, or high cognitive workload. Consider keeping 7–8h sleep consistency, 2L+ daily fluids, and brief daylight breaks. If fatigue persists for weeks, a primary care physician can evaluate routine labs such as iron, B12, or thyroid function.",
            providerUsed = "Demo Clinical Sample"
        ),
        DoctorAiMessage(
            id = "demo_hmsg_1",
            userId = DEMO_UID,
            conversationId = "demo_conv_heart_1",
            sender = "USER",
            mode = AiDoctorMode.HEART_SPECIALIST.name,
            taskType = AiTaskType.HEART_HEALTH_ASSISTANT.name,
            text = "My demo evening blood pressure read 132/84 mmHg after a busy shift. How should I track this accurately?",
            providerUsed = "User"
        ),
        DoctorAiMessage(
            id = "demo_hmsg_2",
            userId = DEMO_UID,
            conversationId = "demo_conv_heart_1",
            sender = "ASSISTANT",
            mode = AiDoctorMode.HEART_SPECIALIST.name,
            taskType = AiTaskType.HEART_HEALTH_ASSISTANT.name,
            text = "Fictional Demo Cardiology Info: Single readings after work stress can run higher than resting baseline. Rest quietly for 5 minutes with back supported, feet flat, and arm at heart level before taking two readings 1 minute apart. Share your 7-day dated log with a primary care physician or cardiologist.",
            providerUsed = "Demo Cardiology Sample"
        )
    )

    fun demoHeartProfile(): HeartHealthProfile = HeartHealthProfile(
        id = "primary",
        userId = DEMO_UID,
        ageYears = 34L,
        knownConditions = listOf("Occasional elevated evening BP (Demo)"),
        medications = listOf("Vitamin D3 supplement (Demo)"),
        allergies = listOf("None reported"),
        smokingStatus = "NEVER",
        familyHeartHistory = true,
        baselineActivityLevel = "MODERATE"
    )

    fun demoHeartMeasurements(): List<HeartMeasurement> = listOf(
        HeartMeasurement(
            id = "demo_hm_4",
            userId = DEMO_UID,
            recordedAtIso = "2026-11-19 08:30",
            restingHeartRateBpm = 74L,
            systolicMmHg = 128L,
            diastolicMmHg = 82L,
            oxygenSaturationPct = 98L,
            weightKg = 71.4,
            bloodGlucoseMgDl = 104.0,
            totalCholesterolMgDl = 208.0,
            ldlCholesterolMgDl = 132.0,
            hdlCholesterolMgDl = 46.0,
            symptoms = emptyList(),
            activityLevel = "RESTING",
            notes = "Fictional demo morning resting reading before coffee.",
            sourceLabel = "DEMO_FICTIONAL"
        ),
        HeartMeasurement(
            id = "demo_hm_3",
            userId = DEMO_UID,
            recordedAtIso = "2026-11-18 19:15",
            restingHeartRateBpm = 79L,
            systolicMmHg = 132L,
            diastolicMmHg = 84L,
            oxygenSaturationPct = 98L,
            symptoms = listOf("Mild fatigue"),
            activityLevel = "RESTING",
            notes = "Fictional demo evening post-shift log.",
            sourceLabel = "DEMO_FICTIONAL"
        ),
        HeartMeasurement(
            id = "demo_hm_2",
            userId = DEMO_UID,
            recordedAtIso = "2026-11-17 08:20",
            restingHeartRateBpm = 71L,
            systolicMmHg = 124L,
            diastolicMmHg = 79L,
            oxygenSaturationPct = 99L,
            activityLevel = "RESTING",
            sourceLabel = "DEMO_FICTIONAL"
        ),
        HeartMeasurement(
            id = "demo_hm_1",
            userId = DEMO_UID,
            recordedAtIso = "2026-11-16 08:15",
            restingHeartRateBpm = 70L,
            systolicMmHg = 121L,
            diastolicMmHg = 78L,
            oxygenSaturationPct = 99L,
            activityLevel = "RESTING",
            sourceLabel = "DEMO_FICTIONAL"
        )
    )

    fun demoBloodPressureRecords(): List<BloodPressureRecord> = listOf(
        BloodPressureRecord(
            id = "demo_bp_3",
            userId = DEMO_UID,
            measurementId = "demo_hm_4",
            recordedAtIso = "2026-11-19 08:30",
            systolicMmHg = 128L,
            diastolicMmHg = 82L,
            pulseBpm = 74L,
            postureOrContext = "MORNING",
            notes = "Fictional demo baseline",
            sourceLabel = "DEMO_FICTIONAL"
        ),
        BloodPressureRecord(
            id = "demo_bp_2",
            userId = DEMO_UID,
            measurementId = "demo_hm_3",
            recordedAtIso = "2026-11-18 19:15",
            systolicMmHg = 132L,
            diastolicMmHg = 84L,
            pulseBpm = 79L,
            postureOrContext = "EVENING",
            sourceLabel = "DEMO_FICTIONAL"
        ),
        BloodPressureRecord(
            id = "demo_bp_1",
            userId = DEMO_UID,
            measurementId = "demo_hm_2",
            recordedAtIso = "2026-11-17 08:20",
            systolicMmHg = 124L,
            diastolicMmHg = 79L,
            pulseBpm = 71L,
            postureOrContext = "MORNING",
            sourceLabel = "DEMO_FICTIONAL"
        )
    )

    fun demoHeartSymptomReports(): List<HeartSymptomReport> = listOf(
        HeartSymptomReport(
            id = "demo_hsym_1",
            userId = DEMO_UID,
            symptoms = listOf("Mild fatigue"),
            severityScore = 3L,
            durationLabel = "2 days afternoon only",
            associatedSymptoms = listOf("Poor sleep (<6h)"),
            emergencyTriggered = false,
            screeningReason = "No chest pain, breathlessness, fainting, or neurological red flags reported.",
            aiSummary = "Fictional Demo Check: Mild fatigue correlates with recent short sleep; continue rest monitoring and seek medical care if red flags emerge.",
            urgencyLevel = "routine"
        )
    )

    fun demoHealthReportUploads(): List<HealthReportUpload> = listOf(
        HealthReportUpload(
            id = "demo_rep_1",
            userId = DEMO_UID,
            title = "Lipid & Fasting Glucose Panel (Fictional Demo)",
            reportType = "CARDIAC_LAB",
            fileName = "demo_lipid_panel.txt",
            extractedOrEnteredText = sampleDemoLipidText,
            summary = "Fictional Demo Explanation: Total cholesterol (214 mg/dL), LDL (138 mg/dL), triglycerides (162 mg/dL), and fasting glucose (106 mg/dL) sit modestly above standard reference targets while HDL (44 mg/dL) is within range.",
            outOfRangeFindings = listOf(
                "Total Cholesterol: 214 mg/dL (Desirable <200 mg/dL)",
                "LDL Cholesterol: 138 mg/dL (Optimal <100 mg/dL)",
                "Fasting Glucose: 106 mg/dL (Normal 70–99 mg/dL)"
            ),
            questionsForDoctor = listOf(
                "Would dietary adjustments and repeat fasting labs in 3 months be appropriate?",
                "How does my family heart history factor into my target LDL goal?"
            ),
            emergencyTriggered = false,
            providerUsed = "Demo Sample Report"
        )
    )

    fun demoAiInsights(): List<AiHealthInsightRecord> = listOf(
        AiHealthInsightRecord(
            id = "demo_ins_1",
            userId = DEMO_UID,
            domain = "HEART_SPECIALIST",
            taskType = AiTaskType.HEALTH_TREND_ANALYSIS.name,
            title = "BP & Sleep Correlation Observation (Demo)",
            summary = "Fictional demo evening BP (132/84 mmHg) coincided with 5.8h sleep and work stress (8/10).",
            observations = listOf("Evening BP slightly higher than morning baseline (124–128 mmHg systolic)."),
            recommendations = listOf("Log resting BP after 5 minutes of quiet sitting and prioritize 7h+ sleep."),
            recommendedDoctorType = "Primary Care Physician",
            urgency = "routine",
            providerUsed = "Demo Sample Insight",
            fallbackUsed = false
        )
    )

    fun demoSafetyEvents(): List<MedicalSafetyEvent> = emptyList()

    fun demoConsentRecords(): List<UserConsentRecord> = listOf(
        UserConsentRecord(
            id = "demo_consent_1",
            userId = DEMO_UID,
            consentAnalytics = true,
            consentAiGuidance = true,
            consentHeartAiContext = true,
            consentReportAnalysis = true,
            consentCoordinatorOutreach = true,
            policyNote = "Demo Mode initial consent snapshot"
        )
    )
}
