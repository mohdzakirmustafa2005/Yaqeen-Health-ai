package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AiAssistantRole
import com.example.data.GroundingSource
import com.example.data.local.LocalHealthDatabase
import com.example.data.local.RoomLocalHealthCacheRepository
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.AiChatMessage
import com.example.model.AiSender
import com.example.model.HealthCheckIn
import com.example.model.RiskLevel
import com.example.model.UserRole
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthAndLandingScreen
import com.example.ui.theme.YaqeenHealthTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YaqeenUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `landing screen displays Google Sign-In and Demo Mode role selection`() {
        var selectedDemoRole: UserRole? = null

        composeRule.setContent {
            YaqeenHealthTheme {
                AuthAndLandingScreen(
                    language = AppLanguage.EN,
                    strings = AppStrings.get(AppLanguage.EN),
                    onLanguageChange = {},
                    onAuthSuccess = {},
                    onEnterDemoMode = { selectedDemoRole = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("demo_role_coordinator").performScrollTo().performClick()
        composeRule.onNodeWithTag("enter_demo_mode_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(UserRole.COORDINATOR, selectedDemoRole)
        composeRule.onNodeWithText("Yaqeen Health").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ai assistant renders roles, search grounding citations, and sends message`() {
        var sentPrompt: String? = null
        var selectedRole by androidx.compose.runtime.mutableStateOf(AiAssistantRole.WELLNESS_GUIDE)
        var searchEnabled by androidx.compose.runtime.mutableStateOf(false)

        val msgs = listOf(
            AiChatMessage(id = "u1", userId = "u", sender = AiSender.USER.name, text = "How much sleep helps burnout?"),
            AiChatMessage(id = "a1", userId = "u", sender = AiSender.ASSISTANT.name, text = "Aim for 7-9 hours consistently.")
        )
        val citations = mapOf(
            "a1" to listOf(
                GroundingSource(
                    title = "CDC Sleep Hygiene Basics",
                    domain = "cdc.gov",
                    uri = "https://www.cdc.gov/sleep",
                    searchQuery = "CDC sleep hygiene guidelines"
                )
            )
        )

        composeRule.setContent {
            YaqeenHealthTheme {
                AiAssistantScreen(
                    strings = AppStrings.get(AppLanguage.EN),
                    messages = msgs,
                    streamingReply = null,
                    isBusy = false,
                    selectedRole = selectedRole,
                    googleSearchEnabled = searchEnabled,
                    groundingSourcesByMessage = citations,
                    onSelectRole = { selectedRole = it },
                    onToggleGoogleSearch = { searchEnabled = it },
                    onSendMessage = { sentPrompt = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("cdc.gov: CDC Sleep Hygiene Basics").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_google_search_toggle").performClick()
        assertTrue(searchEnabled)

        composeRule.onNodeWithTag("ai_role_public_health_search").performScrollTo().performClick()
        assertEquals(AiAssistantRole.PUBLIC_HEALTH_SEARCH, selectedRole)

        composeRule.onNodeWithTag("ai_chat_input").performTextInput("What are WHO hydration tips?")
        composeRule.onNodeWithTag("ai_chat_send_button").performClick()
        assertEquals("What are WHO hydration tips?", sentPrompt)
    }

    @Test
    fun `room local sqlite database caches check-ins, computes sql summary, and stores citations`() = runTest {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocalHealthDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val repo = RoomLocalHealthCacheRepository(db.localHealthDao())
            repo.syncCheckIns(
                "user_sql",
                listOf(
                    HealthCheckIn(id = "c1", userId = "user_sql", dateKey = "2026-11-18", overallWellbeing = 8L, sleepHours = 7.5, stressLevel = 3L, fatigueLevel = 3L, computedRiskLevel = RiskLevel.GREEN.name),
                    HealthCheckIn(id = "c2", userId = "user_sql", dateKey = "2026-11-19", overallWellbeing = 4L, sleepHours = 5.5, stressLevel = 8L, fatigueLevel = 8L, computedRiskLevel = RiskLevel.ORANGE.name)
                )
            )
            val summary = repo.observeSqlSummary("user_sql").first()
            assertEquals(2, summary?.totalCount)
            assertEquals(6.0, summary?.avgWellbeing ?: 0.0, 0.01)
            assertEquals(6.5, summary?.avgSleep ?: 0.0, 0.01)
            assertEquals(1, summary?.elevatedCount)

            repo.saveGroundingSources(
                userId = "user_sql",
                messageId = "msg_9",
                roleName = "Health Search",
                sources = listOf(GroundingSource("NIOSH Heat Stress", "cdc.gov", "https://www.cdc.gov/niosh", "heat stress"))
            )
            val storedCitations = repo.observeGroundingSourcesByMessage("user_sql").first()
            assertEquals(1, storedCitations["msg_9"]?.size)

            repo.syncHeartMeasurements(
                "user_sql",
                listOf(
                    com.example.model.HeartMeasurement(
                        id = "hm1",
                        userId = "user_sql",
                        recordedAtIso = "2026-11-19 08:00",
                        restingHeartRateBpm = 72L,
                        systolicMmHg = 120L,
                        diastolicMmHg = 78L,
                        oxygenSaturationPct = 98L
                    )
                )
            )
            val summaryWithHeart = repo.observeSqlSummary("user_sql").first()
            assertEquals(1, summaryWithHeart?.heartReadingsCount)

            repo.clearUserCache("user_sql")
            val cleared = repo.observeSqlSummary("user_sql").first()
            assertEquals(0, cleared?.totalCount)
            assertEquals(0, cleared?.heartReadingsCount)
        } finally {
            db.close()
        }
    }

    @Test
    fun `heart symptom checker renders emergency 112 banner immediately when chest pain selected`() {
        composeRule.setContent {
            YaqeenHealthTheme {
                com.example.ui.screens.HeartSymptomCheckerScreen(
                    isBusy = false,
                    emergencyBanner = null,
                    lastResult = null,
                    symptomReports = emptyList(),
                    onSubmitCheck = { _, _, _, _, _ -> },
                    onDismissEmergency = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("symptom_checkbox_chest_pain").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("emergency_safety_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("EMERGENCY WARNING — CALL 112 OR LOCAL EMS").assertIsDisplayed()
    }

    @Test
    fun `report explainer demo ecg loader populates text and triggers analysis callback`() {
        var analyzedTitle: String? = null
        var analyzedText: String? = null

        composeRule.setContent {
            YaqeenHealthTheme {
                com.example.ui.screens.ReportExplainerScreen(
                    isBusy = false,
                    lastResult = null,
                    uploads = emptyList(),
                    onAnalyzeReport = { title, _, _, text, _ ->
                        analyzedTitle = title
                        analyzedText = text
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("load_demo_ecg_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("analyze_report_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("Fictional Demo ECG Report", analyzedTitle)
        assertTrue(analyzedText?.contains("Rate: 76 bpm") == true)
    }

    @Test
    fun `structured response parser extracts markdown json, validates fields, and clamps urgency`() {
        val raw = """
            ```json
            {
              "mode": "heart_specialist",
              "summary": "Resting BP overview.",
              "answer": "Always measure resting BP after 5 minutes sitting quietly.",
              "observations": ["BP 132/84 mmHg logged"],
              "recommendations": ["Keep a 7-day BP log"],
              "follow_up_questions": ["Was caffeine consumed beforehand?"],
              "doctor_type": "Cardiologist",
              "medical_attention": {
                "recommended": true,
                "urgency": "INVALID_LEVEL",
                "reason": "Follow up"
              },
              "emergency_screen": {
                "triggered": false,
                "reason": ""
              },
              "disclaimer": "Educational guidance only."
            }
            ```
        """.trimIndent()
        val parsed = com.example.data.StructuredAiResponseParser.parseAndValidate(
            rawJson = raw,
            fallbackMode = "general_health",
            fallbackDisclaimer = "Default disclaimer",
            providerName = "TestGemini"
        )
        assertEquals("heart_specialist", parsed?.mode)
        assertEquals("routine", parsed?.medicalAttention?.urgency)
        assertEquals("Cardiologist", parsed?.doctorType)

        val emptyInvalid = com.example.data.StructuredAiResponseParser.parseAndValidate(
            rawJson = """{"summary":"","answer":""}""",
            fallbackMode = "general_health",
            fallbackDisclaimer = "Default",
            providerName = "Test"
        )
        assertEquals(null, emptyInvalid)
    }

    @Test
    fun `doctor chat screen renders mandatory disclaimer and sends question`() {
        var sentText: String? = null

        composeRule.setContent {
            YaqeenHealthTheme {
                com.example.ui.screens.DoctorChatScreen(
                    selectedMode = com.example.model.AiDoctorMode.GENERAL_HEALTH,
                    messages = emptyList(),
                    isBusy = false,
                    errorMessage = null,
                    emergencyBanner = null,
                    googleSearchEnabled = false,
                    groundingSourcesByMessage = emptyMap(),
                    onSelectMode = {},
                    onToggleGoogleSearch = {},
                    onSendMessage = { sentText = it },
                    onNewChat = {},
                    onRetry = {},
                    onFeedback = { _, _ -> },
                    onDismissEmergency = {},
                    onOpenAppointmentPrep = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(com.example.model.StructuredAiResponse.DOCTOR_MODE_DISCLAIMER).assertIsDisplayed()
        composeRule.onNodeWithTag("doctor_chat_input").performTextInput("How does hydration affect fatigue?")
        composeRule.onNodeWithTag("doctor_chat_send_button").performClick()
        assertEquals("How does hydration affect fatigue?", sentText)
    }
}
