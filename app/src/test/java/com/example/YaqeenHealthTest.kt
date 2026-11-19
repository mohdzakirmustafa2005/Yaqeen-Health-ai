package com.example

import com.example.data.AiAssistantRole
import com.example.data.DemoHealthRepository
import com.example.data.FirebaseAiHealthService
import com.example.data.RuleBasedFallbackAdvisor
import com.example.engine.DemoDataFactory
import com.example.engine.EarlyWarningEngine
import com.example.engine.ReportFormatter
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.AiChatMessage
import com.example.model.AiSender
import com.example.model.AlertStatus
import com.example.model.HealthCheckIn
import com.example.model.MoodOption
import com.example.model.RiskLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YaqeenHealthTest {

    @Test
    fun `early warning engine classifies stable check-in as GREEN`() {
        val latest = HealthCheckIn(
            dateKey = "2026-11-19",
            overallWellbeing = 8L,
            sleepQuality = 8L,
            sleepHours = 7.5,
            stressLevel = 3L,
            energyLevel = 8L,
            fatigueLevel = 3L,
            mood = MoodOption.CALM.name,
            workStress = 3L,
            hydrationLiters = 2.4,
            activityMinutes = 40L
        )
        val eval = EarlyWarningEngine.evaluate(latest, emptyList(), "u1")
        assertEquals(RiskLevel.GREEN, eval.riskLevel)
        assertTrue(eval.suggestedRecommendations.isNotEmpty())
    }

    @Test
    fun `early warning engine detects multi-day burnout & declining trend as ORANGE`() {
        val h1 = HealthCheckIn(overallWellbeing = 7L, sleepHours = 5.5, stressLevel = 8L, workStress = 8L)
        val h2 = HealthCheckIn(overallWellbeing = 8L, sleepHours = 5.8, stressLevel = 7L, workStress = 7L)
        val latest = HealthCheckIn(
            dateKey = "2026-11-19",
            overallWellbeing = 5L,
            sleepQuality = 4L,
            sleepHours = 5.0,
            stressLevel = 8L,
            energyLevel = 3L,
            fatigueLevel = 8L,
            mood = MoodOption.OVERWHELMED.name,
            workStress = 8L,
            hydrationLiters = 1.0,
            activityMinutes = 10L
        )
        val eval = EarlyWarningEngine.evaluate(latest, listOf(h1, h2), "u1")
        assertEquals(RiskLevel.RED, eval.riskLevel)
        assertTrue(eval.triggerFactors.any { it.contains("Declining wellbeing trend") })
    }

    @Test
    fun `urgent symptom triggers RED alert with emergency guidance`() {
        val latest = HealthCheckIn(
            dateKey = "2026-11-19",
            overallWellbeing = 4L,
            sleepHours = 7.0,
            stressLevel = 5L,
            symptoms = listOf("Chest discomfort")
        )
        val eval = EarlyWarningEngine.evaluate(latest, emptyList(), "u1")
        assertEquals(RiskLevel.RED, eval.riskLevel)
        assertTrue(eval.recommendedNextStep.contains("emergency", ignoreCase = true))
    }

    @Test
    fun `workforce analytics enforces minimum group size privacy suppression`() {
        val summary = DemoDataFactory.demoWorkforceAnalytics()
        val suppressed = summary.departments.first { it.departmentName == "Executive Support" }
        val visible = summary.departments.first { it.departmentName == "Field Operations" }
        assertTrue(suppressed.isSuppressedForPrivacy)
        assertFalse(visible.isSuppressedForPrivacy)
    }

    @Test
    fun `personal wellness report includes non-diagnostic disclaimer and metrics`() {
        val report = ReportFormatter.buildPersonalWellnessReport(
            profile = DemoDataFactory.demoProfile(),
            checkIns = DemoDataFactory.demoCheckIns(),
            alerts = DemoDataFactory.demoAlerts(),
            recommendations = DemoDataFactory.demoRecommendations(),
            periodDays = 7,
            isDemo = true
        )
        assertTrue(report.contains("not constitute medical diagnosis", ignoreCase = true))
        assertTrue(report.contains("FICTIONAL DEMO DATA"))
        assertTrue(report.contains("SUMMARY AVERAGES"))
    }

    @Test
    fun `multilingual strings support all 4 languages and RTL for Urdu`() {
        assertTrue(AppLanguage.UR.isRtl)
        assertFalse(AppLanguage.EN.isRtl)
        assertFalse(AppLanguage.HI.isRtl)
        assertFalse(AppLanguage.TE.isRtl)
        AppLanguage.entries.forEach { lang ->
            val s = AppStrings.get(lang)
            assertTrue(s.appTitle.isNotBlank())
            assertTrue(s.nonDiagnosticBanner.isNotBlank())
        }
    }

    @Test
    fun `fallback advisor responds safely to emergency keywords`() {
        val reply = RuleBasedFallbackAdvisor.reply("I feel chest pain and shortness of breath", emptyList(), AppLanguage.EN)
        assertTrue(reply.contains("emergency", ignoreCase = true))
        assertTrue(reply.contains("does not replace professional medical care", ignoreCase = true))
    }

    @Test
    fun `multi-turn chat history builder maps roles and respects turn window`() {
        val msgs = (1..16).map { idx ->
            AiChatMessage(
                id = "m$idx",
                userId = "u1",
                sender = if (idx % 2 == 1) AiSender.USER.name else AiSender.ASSISTANT.name,
                text = "Turn $idx"
            )
        }
        val history = FirebaseAiHealthService.buildChatHistory(msgs, maxTurns = 12)
        assertEquals(12, history.size)
        assertEquals("user", history.first().role)
        assertEquals("model", history.last().role)
    }

    @Test
    fun `ai assistant roles configure supported models and search grounding default`() {
        assertTrue(AiAssistantRole.PUBLIC_HEALTH_SEARCH.defaultSearchEnabled)
        assertFalse(AiAssistantRole.WELLNESS_GUIDE.defaultSearchEnabled)
        assertEquals("gemini-3.5-flash-lite", AiAssistantRole.QUICK_SLEEP_COACH.modelName)
        assertEquals("gemini-3.8-flash", AiAssistantRole.WELLNESS_GUIDE.modelName)
    }

    @Test
    fun `demo repository persists check-in, recommendation toggle, and alert status`() = runTest {
        val repo = DemoHealthRepository()
        val initialCount = repo.observeCheckIns(DemoDataFactory.DEMO_UID).first().size
        val res = repo.saveCheckIn(
            DemoDataFactory.DEMO_UID,
            HealthCheckIn(dateKey = "2026-11-19", overallWellbeing = 9L, computedRiskLevel = RiskLevel.GREEN.name)
        )
        assertTrue(res.isSuccess)
        val afterCount = repo.observeCheckIns(DemoDataFactory.DEMO_UID).first().size
        assertEquals(initialCount + 1, afterCount)

        repo.toggleRecommendation(DemoDataFactory.DEMO_UID, "demo_rec_1", true)
        val rec = repo.observeRecommendations(DemoDataFactory.DEMO_UID).first().first { it.id == "demo_rec_1" }
        assertTrue(rec.completed)

        repo.updateAlertStatus(DemoDataFactory.DEMO_UID, "demo_alert_1", AlertStatus.ACKNOWLEDGED)
        val alert = repo.observeAlerts(DemoDataFactory.DEMO_UID).first().first { it.id == "demo_alert_1" }
        assertEquals(AlertStatus.ACKNOWLEDGED.name, alert.status)
    }

    @Test
    fun `safety screening triggers emergency 112 guidance for chest pain, stroke signs, and severe vitals`() {
        val chestScreen = com.example.engine.SafetyScreeningEngine.screenTextAndSymptoms(
            text = "I have crushing chest pain radiating to arm for 15 minutes",
            selectedSymptoms = emptyList()
        )
        assertTrue(chestScreen.triggered)
        assertTrue(chestScreen.guidance.contains("112"))
        assertTrue(chestScreen.guidance.contains("paused automatically", ignoreCase = true))

        val strokeScreen = com.example.engine.SafetyScreeningEngine.screenTextAndSymptoms(
            text = "Sudden facial droop and slurred speech",
            selectedSymptoms = emptyList()
        )
        assertTrue(strokeScreen.triggered)
        assertTrue(strokeScreen.matchedSignals.any { it.contains("Stroke", ignoreCase = true) })

        val vitalScreen = com.example.engine.SafetyScreeningEngine.screenVitalThresholds(
            com.example.model.HeartMeasurement(
                systolicMmHg = 188L,
                diastolicMmHg = 124L,
                oxygenSaturationPct = 88L
            )
        )
        assertTrue(vitalScreen.triggered)
        assertTrue(vitalScreen.guidance.contains("112"))
    }

    @Test
    fun `pii sanitizer strips direct identifiers and respects granular ai consent`() {
        val cleaned = com.example.engine.PiiSanitizer.sanitizeFreeText(
            raw = "My name is Amina Khan, email amina.khan@corp.org, phone +91 98765 43210, EMP-4421 at 120 Oak Street.",
            userDisplayName = "Amina Khan"
        )
        assertFalse(cleaned.contains("Amina", ignoreCase = true))
        assertFalse(cleaned.contains("amina.khan@corp.org"))
        assertFalse(cleaned.contains("98765 43210"))
        assertFalse(cleaned.contains("EMP-4421"))
        assertFalse(cleaned.contains("120 Oak Street"))

        val optedOut = com.example.engine.PiiSanitizer.buildMinimizedClinicalContext(
            consentAi = false,
            consentHeartAi = false,
            profile = DemoDataFactory.demoProfile(),
            heartProfile = DemoDataFactory.demoHeartProfile(),
            recentCheckIns = DemoDataFactory.demoCheckIns(),
            recentHeartMeasurements = DemoDataFactory.demoHeartMeasurements()
        )
        assertTrue(optedOut.contains("opted out", ignoreCase = true))

        val heartExcluded = com.example.engine.PiiSanitizer.buildMinimizedClinicalContext(
            consentAi = true,
            consentHeartAi = false,
            profile = DemoDataFactory.demoProfile(),
            heartProfile = DemoDataFactory.demoHeartProfile(),
            recentCheckIns = DemoDataFactory.demoCheckIns(),
            recentHeartMeasurements = DemoDataFactory.demoHeartMeasurements()
        )
        assertFalse(heartExcluded.contains("HR="))
        assertFalse(heartExcluded.contains("Amina"))
    }

    @Test
    fun `rate limiter enforces rapid cooldown and sliding window cap`() {
        val limiter = com.example.engine.AiRateLimiter(maxRequestsPerWindow = 3, windowMillis = 60_000L, minIntervalMillis = 1000L)
        assertTrue(limiter.tryAcquire(1_000L).allowed)
        assertFalse(limiter.tryAcquire(1_500L).allowed)
        assertTrue(limiter.tryAcquire(2_500L).allowed)
        assertTrue(limiter.tryAcquire(4_000L).allowed)
        assertFalse(limiter.tryAcquire(6_000L).allowed)
    }

    @Test
    fun `heart risk education covers 10 factors and blocks exercise suggestions when urgent symptoms exist`() {
        assertEquals(10, com.example.engine.HeartRiskEducationEngine.educationalRiskFactors.size)

        val urgentRecs = com.example.engine.HeartRiskEducationEngine.buildSafeHeartSuggestions(
            userId = "u1",
            latestMeasurement = DemoDataFactory.demoHeartMeasurements().first(),
            profile = DemoDataFactory.demoHeartProfile(),
            hasUrgentSymptoms = true
        )
        assertEquals(1, urgentRecs.size)
        assertEquals("HEALTH_FOLLOW_UP", urgentRecs.first().category)

        val safeRecs = com.example.engine.HeartRiskEducationEngine.buildSafeHeartSuggestions(
            userId = "u1",
            latestMeasurement = DemoDataFactory.demoHeartMeasurements().first(),
            profile = DemoDataFactory.demoHeartProfile(),
            hasUrgentSymptoms = false
        )
        assertEquals(5, safeRecs.size)
        assertTrue(safeRecs.any { it.category == "PHYSICAL_ACTIVITY" })
    }

    @Test
    fun `clinical orchestrator deterministic fallback produces valid non-diagnostic responses`() {
        val orch = com.example.data.AiClinicalOrchestrator()
        val docFallback = orch.buildDeterministicClinicalFallback(
            prompt = "How can I prepare for my appointment?",
            mode = com.example.model.AiDoctorMode.HEALTH_NAVIGATOR,
            taskType = com.example.model.AiTaskType.APPOINTMENT_PREPARATION,
            language = AppLanguage.EN,
            recentHeart = DemoDataFactory.demoHeartMeasurements(),
            recentCheckIns = DemoDataFactory.demoCheckIns()
        )
        assertTrue(docFallback.fallbackUsed)
        assertTrue(docFallback.summary.isNotBlank())
        assertTrue(docFallback.disclaimer.contains("not a licensed doctor", ignoreCase = true))

        val heartFallback = orch.buildDeterministicClinicalFallback(
            prompt = "Explain home BP readings",
            mode = com.example.model.AiDoctorMode.HEART_SPECIALIST,
            taskType = com.example.model.AiTaskType.HEART_HEALTH_ASSISTANT,
            language = AppLanguage.EN,
            recentHeart = DemoDataFactory.demoHeartMeasurements(),
            recentCheckIns = DemoDataFactory.demoCheckIns()
        )
        assertEquals("heart_specialist", heartFallback.mode)
        assertTrue(heartFallback.disclaimer.contains("Cardiology", ignoreCase = true))
    }

    @Test
    fun `heart health report formatter and demo clinical repository persist new clinical entities`() = runTest {
        val heartReport = ReportFormatter.buildHeartHealthReport(
            profile = DemoDataFactory.demoProfile(),
            heartProfile = DemoDataFactory.demoHeartProfile(),
            measurements = DemoDataFactory.demoHeartMeasurements(),
            bpRecords = DemoDataFactory.demoBloodPressureRecords(),
            symptomReports = DemoDataFactory.demoHeartSymptomReports(),
            safetyEvents = DemoDataFactory.demoSafetyEvents(),
            isDemo = true
        )
        assertTrue(heartReport.contains("YAQEEN AI CARDIOLOGY ASSISTANT"))
        assertTrue(heartReport.contains("FICTIONAL DEMO DATA"))
        assertTrue(heartReport.contains("does not diagnose heart conditions", ignoreCase = true))

        val repo = DemoHealthRepository()
        val uid = DemoDataFactory.DEMO_UID
        val convId = repo.createOrUpdateConversation(
            uid,
            com.example.model.DoctorConversation(
                userId = uid,
                domain = com.example.model.DoctorDomain.DOCTOR.name,
                mode = com.example.model.AiDoctorMode.GENERAL_HEALTH.modeId,
                title = "Sleep check"
            )
        ).getOrThrow()
        repo.saveDoctorMessage(
            uid,
            com.example.model.DoctorAiMessage(
                conversationId = convId,
                userId = uid,
                sender = "USER",
                text = "Test question"
            )
        )
        val msgs = repo.observeDoctorMessages(uid).first().filter { it.conversationId == convId }
        assertEquals(1, msgs.size)

        val beforeMeas = repo.observeHeartMeasurements(uid).first().size
        repo.saveHeartMeasurement(uid, com.example.model.HeartMeasurement(restingHeartRateBpm = 70L, systolicMmHg = 118L, diastolicMmHg = 76L))
        assertEquals(beforeMeas + 1, repo.observeHeartMeasurements(uid).first().size)

        val beforeUploads = repo.observeHealthReportUploads(uid).first().size
        repo.saveHealthReportUpload(uid, com.example.model.HealthReportUpload(title = "CBC Check", summary = "All markers within range"))
        assertEquals(beforeUploads + 1, repo.observeHealthReportUploads(uid).first().size)
    }
}
