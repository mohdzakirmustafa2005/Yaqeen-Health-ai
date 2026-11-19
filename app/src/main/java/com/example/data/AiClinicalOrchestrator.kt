package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.BuildConfig
import com.example.engine.AiRateLimiter
import com.example.engine.PiiSanitizer
import com.example.engine.SafetyScreeningEngine
import com.example.localization.AppLanguage
import com.example.model.AiDoctorMode
import com.example.model.AiTaskType
import com.example.model.DoctorAiMessage
import com.example.model.EmergencyScreenResult
import com.example.model.HealthCheckIn
import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.MedicalAttentionAdvice
import com.example.model.StructuredAiResponse
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AiProviderHealth(
    val name: String,
    val roleLabel: String,
    val configured: Boolean,
    val detail: String
)

data class ClinicalAiInvocationResult(
    val response: StructuredAiResponse,
    val rawJson: String,
    val groundingSources: List<GroundingSource> = emptyList(),
    val rateLimited: Boolean = false
)

/**
 * Central AI Orchestrator supporting:
 * 1) Pre-AI Safety Screening (halts wellness flow immediately on emergency signs)
 * 2) PII Sanitization & Privacy Minimization
 * 3) Sliding-Window Rate Limiting
 * 4) Primary: Google Gemini via Firebase AI Logic (Structured Schema + Multimodal + Search Grounding)
 * 5) Backup: Groq API (OpenAI-compatible REST via BuildConfig.GROQ_API_KEY)
 * 6) Optional: OpenRouter API (via BuildConfig.OPENROUTER_API_KEY)
 * 7) Fallback: Rule-Based Clinical Guidance (always returns validated StructuredAiResponse)
 */
class AiClinicalOrchestrator(
    private val rateLimiter: AiRateLimiter = AiRateLimiter()
) {

    fun getProviderStatuses(): List<AiProviderHealth> {
        val groqConfigured = isRealSecret(BuildConfig.GROQ_API_KEY, "MY_GROQ_API_KEY_DEFAULT_VALUE")
        val openRouterConfigured = isRealSecret(BuildConfig.OPENROUTER_API_KEY, "MY_OPENROUTER_API_KEY_DEFAULT_VALUE")
        return listOf(
            AiProviderHealth(
                name = "Google Gemini (Firebase AI Logic)",
                roleLabel = "PRIMARY",
                configured = true,
                detail = "Active via Firebase App Check (gemini-3.8-flash / gemini-3.5-flash-lite)"
            ),
            AiProviderHealth(
                name = "Groq API",
                roleLabel = "BACKUP",
                configured = groqConfigured,
                detail = if (groqConfigured) "Configured via Secrets panel" else "Add GROQ_API_KEY in Secrets panel to enable backup"
            ),
            AiProviderHealth(
                name = "OpenRouter API",
                roleLabel = "OPTIONAL",
                configured = openRouterConfigured,
                detail = if (openRouterConfigured) "Configured via Secrets panel" else "Add OPENROUTER_API_KEY in Secrets panel if needed"
            ),
            AiProviderHealth(
                name = "Rule-Based Clinical Safety Engine",
                roleLabel = "FALLBACK",
                configured = true,
                detail = "Always-on deterministic guidance & emergency screening"
            )
        )
    }

    private fun isRealSecret(value: String, placeholder: String): Boolean =
        value.isNotBlank() && value != placeholder

    private val structuredSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "mode" to Schema.string(description = "Mode identifier e.g. general_health, heart_specialist, symptom_checker, report_explainer, wellness, mental_wellness, health_navigator"),
                "summary" to Schema.string(description = "Concise 1-2 sentence educational summary"),
                "answer" to Schema.string(description = "Clear, empathetic, non-diagnostic plain-language explanation"),
                "observations" to Schema.array(items = Schema.string(), description = "Key observations grounded strictly in user-provided info"),
                "recommendations" to Schema.array(items = Schema.string(), description = "Safe preventive wellness or preparation steps"),
                "follow_up_questions" to Schema.array(items = Schema.string(), description = "2-3 clarifying follow-up questions for the user"),
                "doctor_type" to Schema.string(description = "Appropriate clinician type e.g. Primary Care Physician, Cardiologist", nullable = true),
                "medical_attention" to Schema.obj(
                    properties = mapOf(
                        "recommended" to Schema.boolean(),
                        "urgency" to Schema.enumeration(values = listOf("routine", "prompt", "urgent", "emergency")),
                        "reason" to Schema.string()
                    )
                ),
                "emergency_screen" to Schema.obj(
                    properties = mapOf(
                        "triggered" to Schema.boolean(),
                        "reason" to Schema.string()
                    )
                ),
                "disclaimer" to Schema.string(description = "Clear non-diagnostic disclaimer")
            ),
            optionalProperties = listOf("doctor_type")
        )
    }

    private val modelCache = mutableMapOf<Pair<String, Boolean>, GenerativeModel>()

    @Synchronized
    private fun getGeminiStructuredModel(
        promptType: SystemPromptType,
        modelName: String,
        useGoogleSearch: Boolean
    ): GenerativeModel {
        val key = "${promptType.name}_$modelName" to useGoogleSearch
        return modelCache.getOrPut(key) {
            if (useGoogleSearch) {
                Firebase.ai.generativeModel(
                    modelName = modelName,
                    tools = listOf(Tool.googleSearch()),
                    systemInstruction = content { text(systemPromptFor(promptType) + "\nRespond strictly in the JSON contract requested in the prompt.") }
                )
            } else {
                Firebase.ai.generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = structuredSchema
                    },
                    systemInstruction = content { text(systemPromptFor(promptType)) }
                )
            }
        }
    }

    suspend fun runClinicalTask(
        userPrompt: String,
        mode: AiDoctorMode,
        taskType: AiTaskType,
        language: AppLanguage,
        conversationHistory: List<DoctorAiMessage>,
        profile: UserProfile?,
        heartProfile: HeartHealthProfile?,
        recentCheckIns: List<HealthCheckIn>,
        recentHeartMeasurements: List<HeartMeasurement>,
        reportBitmap: Bitmap? = null,
        selectedSymptoms: List<String> = emptyList(),
        useGoogleSearch: Boolean = false
    ): ClinicalAiInvocationResult {
        // 1. Independent Pre-AI Safety Screening (MUST run before any LLM call)
        val preScreen = SafetyScreeningEngine.screenTextAndSymptoms(
            text = userPrompt,
            selectedSymptoms = selectedSymptoms,
            latestMeasurement = recentHeartMeasurements.firstOrNull()
        )
        if (preScreen.triggered) {
            val emergencyResponse = buildEmergencyStructuredResponse(mode, preScreen)
            return ClinicalAiInvocationResult(
                response = emergencyResponse,
                rawJson = StructuredAiResponseParser.toJson(emergencyResponse)
            )
        }

        // 2. Rate limiting check
        val rateOutcome = rateLimiter.tryAcquire()
        if (!rateOutcome.allowed) {
            val limited = StructuredAiResponse(
                mode = mode.modeId,
                summary = rateOutcome.reason ?: "Rate limit active.",
                answer = "${rateOutcome.reason} For urgent symptoms, seek immediate medical care rather than waiting.",
                recommendations = listOf("Wait briefly before retrying or review your recent health logs."),
                medicalAttention = MedicalAttentionAdvice(false, "routine", "Rate limit cooldown"),
                disclaimer = disclaimerFor(mode),
                providerUsed = "Rate Limiter Guard",
                fallbackUsed = true
            )
            return ClinicalAiInvocationResult(
                response = limited,
                rawJson = StructuredAiResponseParser.toJson(limited),
                rateLimited = true
            )
        }

        // 3. PII Minimization & Consent-Aware Context Assembly
        val sanitizedPrompt = PiiSanitizer.sanitizeFreeText(userPrompt, profile?.displayName)
        val minimizedContext = PiiSanitizer.buildMinimizedClinicalContext(
            consentAi = profile?.consentAiGuidance ?: true,
            consentHeartAi = profile?.consentAiGuidance ?: true,
            profile = profile,
            heartProfile = heartProfile,
            recentCheckIns = recentCheckIns,
            recentHeartMeasurements = recentHeartMeasurements
        )

        val promptType = when (mode) {
            AiDoctorMode.HEART_SPECIALIST -> SystemPromptType.HEART_SPECIALIST_AI
            AiDoctorMode.REPORT_EXPLAINER -> SystemPromptType.REPORT_EXPLAINER
            else -> if (taskType == AiTaskType.ECG_REPORT_EXPLANATION) SystemPromptType.REPORT_EXPLAINER else SystemPromptType.GENERAL_AI_DOCTOR
        }

        val fullUserMessage = buildStructuredUserPrompt(
            sanitizedPrompt = sanitizedPrompt,
            mode = mode,
            taskType = taskType,
            language = language,
            minimizedContext = minimizedContext
        )

        // 4. Primary: Firebase AI Logic (Gemini)
        val modelName = if (mode == AiDoctorMode.WELLNESS && !useGoogleSearch && reportBitmap == null) {
            "gemini-3.5-flash-lite"
        } else {
            "gemini-3.8-flash"
        }

        val primaryResult = tryGeminiPrimary(
            promptType = promptType,
            modelName = modelName,
            fullUserMessage = fullUserMessage,
            history = conversationHistory,
            mode = mode,
            reportBitmap = reportBitmap,
            useGoogleSearch = useGoogleSearch && reportBitmap == null
        )
        if (primaryResult != null) return primaryResult

        // 5. Backup: Groq API (only when configured)
        if (isRealSecret(BuildConfig.GROQ_API_KEY, "MY_GROQ_API_KEY_DEFAULT_VALUE") && reportBitmap == null) {
            val groqResult = tryOpenAiCompatibleProvider(
                endpoint = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = BuildConfig.GROQ_API_KEY,
                modelId = "llama-3.3-70b-versatile",
                providerLabel = "Groq API (Backup)",
                systemPrompt = systemPromptFor(promptType),
                userMessage = fullUserMessage,
                mode = mode
            )
            if (groqResult != null) return groqResult
        }

        // 6. Optional: OpenRouter API (only when configured)
        if (isRealSecret(BuildConfig.OPENROUTER_API_KEY, "MY_OPENROUTER_API_KEY_DEFAULT_VALUE") && reportBitmap == null) {
            val orResult = tryOpenAiCompatibleProvider(
                endpoint = "https://openrouter.ai/api/v1/chat/completions",
                apiKey = BuildConfig.OPENROUTER_API_KEY,
                modelId = "google/gemini-2.0-flash-001",
                providerLabel = "OpenRouter API (Optional)",
                systemPrompt = systemPromptFor(promptType),
                userMessage = fullUserMessage,
                mode = mode
            )
            if (orResult != null) return orResult
        }

        // 7. Guaranteed Safe Rule-Based Clinical Fallback
        val fallback = buildDeterministicClinicalFallback(
            prompt = sanitizedPrompt,
            mode = mode,
            taskType = taskType,
            language = language,
            recentHeart = recentHeartMeasurements,
            recentCheckIns = recentCheckIns
        )
        return ClinicalAiInvocationResult(
            response = fallback,
            rawJson = StructuredAiResponseParser.toJson(fallback)
        )
    }

    private suspend fun tryGeminiPrimary(
        promptType: SystemPromptType,
        modelName: String,
        fullUserMessage: String,
        history: List<DoctorAiMessage>,
        mode: AiDoctorMode,
        reportBitmap: Bitmap?,
        useGoogleSearch: Boolean
    ): ClinicalAiInvocationResult? {
        return try {
            val model = getGeminiStructuredModel(promptType, modelName, useGoogleSearch)
            val sdkHistory: List<Content> = history
                .filter { it.text.isNotBlank() && it.sender != "SYSTEM_EMERGENCY" }
                .takeLast(8)
                .map { msg ->
                    val role = if (msg.sender == "USER") "user" else "model"
                    content(role) { text(msg.text.take(1500)) }
                }

            val response = withTimeout(60_000L) {
                if (reportBitmap != null) {
                    model.generateContent(
                        content {
                            image(reportBitmap)
                            text(fullUserMessage)
                        }
                    )
                } else {
                    val chat = model.startChat(history = sdkHistory)
                    chat.sendMessage(fullUserMessage)
                }
            }

            val raw = response.text.orEmpty()
            val sources = mutableListOf<GroundingSource>()
            response.candidates.firstOrNull()?.groundingMetadata?.let { meta ->
                val q = meta.webSearchQueries.firstOrNull().orEmpty()
                meta.groundingChunks.forEach { chunk ->
                    val web = chunk.web ?: return@forEach
                    val uri = web.uri?.trim().orEmpty()
                    if (uri.startsWith("http")) {
                        sources += GroundingSource(
                            title = web.title?.trim().orEmpty().ifBlank { "Verified Source" },
                            domain = web.domain?.trim().orEmpty().ifBlank { "web" },
                            uri = uri,
                            searchQuery = q
                        )
                    }
                }
            }

            val parsed = StructuredAiResponseParser.parseAndValidate(
                rawJson = raw,
                fallbackMode = mode.modeId,
                fallbackDisclaimer = disclaimerFor(mode),
                providerName = "Google Gemini ($modelName)"
            )
            if (parsed != null) {
                return ClinicalAiInvocationResult(
                    response = parsed,
                    rawJson = StructuredAiResponseParser.toJson(parsed),
                    groundingSources = sources.distinctBy { it.uri }
                )
            }

            // One controlled correction retry if JSON validation failed
            val repairResponse = withTimeout(30_000L) {
                model.generateContent(
                    "Convert your previous response into STRICT valid JSON matching the exact schema keys " +
                        "(mode, summary, answer, observations, recommendations, follow_up_questions, doctor_type, medical_attention, emergency_screen, disclaimer). " +
                        "Original text:\n${raw.take(2500)}"
                )
            }
            val repaired = StructuredAiResponseParser.parseAndValidate(
                rawJson = repairResponse.text.orEmpty(),
                fallbackMode = mode.modeId,
                fallbackDisclaimer = disclaimerFor(mode),
                providerName = "Google Gemini ($modelName)"
            )
            if (repaired != null) {
                ClinicalAiInvocationResult(
                    response = repaired,
                    rawJson = StructuredAiResponseParser.toJson(repaired),
                    groundingSources = sources.distinctBy { it.uri }
                )
            } else null
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.e(TAG, "Gemini AI call timed out after 60s", e)
                null
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI primary failed, checking backup providers", e)
            null
        }
    }

    private suspend fun tryOpenAiCompatibleProvider(
        endpoint: String,
        apiKey: String,
        modelId: String,
        providerLabel: String,
        systemPrompt: String,
        userMessage: String,
        mode: AiDoctorMode
    ): ClinicalAiInvocationResult? = withContext(Dispatchers.IO) {
        try {
            withTimeout(25_000L) {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                val payload = JSONObject().apply {
                    put("model", modelId)
                    put("temperature", 0.3)
                    put("response_format", JSONObject().put("type", "json_object"))
                    put(
                        "messages",
                        JSONArray().apply {
                            put(JSONObject().put("role", "system").put("content", systemPrompt))
                            put(JSONObject().put("role", "user").put("content", userMessage))
                        }
                    )
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
                val code = conn.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "$providerLabel HTTP $code")
                    return@withTimeout null
                }
                val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
                val contentStr = JSONObject(body)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val parsed = StructuredAiResponseParser.parseAndValidate(
                    rawJson = contentStr,
                    fallbackMode = mode.modeId,
                    fallbackDisclaimer = disclaimerFor(mode),
                    providerName = providerLabel
                )
                parsed?.let {
                    ClinicalAiInvocationResult(
                        response = it,
                        rawJson = StructuredAiResponseParser.toJson(it)
                    )
                }
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.w(TAG, "$providerLabel timed out", e)
                null
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.w(TAG, "$providerLabel failed: ${e.localizedMessage}")
            null
        }
    }

    private fun buildStructuredUserPrompt(
        sanitizedPrompt: String,
        mode: AiDoctorMode,
        taskType: AiTaskType,
        language: AppLanguage,
        minimizedContext: String
    ): String = """
        Reply strictly in ${language.englishName}.
        Mode: ${mode.modeId} (${mode.displayName})
        Task Type: ${taskType.name}
        Non-identifying user wellness context (use ONLY what is relevant, NEVER invent missing values):
        $minimizedContext

        User Request / Question:
        $sanitizedPrompt

        Return valid JSON matching this exact structure:
        {
          "mode": "${mode.modeId}",
          "summary": "1-2 sentence takeaway",
          "answer": "clear non-diagnostic explanation",
          "observations": ["observation grounded in supplied inputs"],
          "recommendations": ["practical preventive step or question to discuss"],
          "follow_up_questions": ["helpful clarifying question"],
          "doctor_type": "e.g. Primary Care Physician or Cardiologist, or null",
          "medical_attention": {
            "recommended": true/false,
            "urgency": "routine|prompt|urgent|emergency",
            "reason": "why"
          },
          "emergency_screen": {
            "triggered": false,
            "reason": ""
          },
          "disclaimer": "${disclaimerFor(mode)}"
        }
    """.trimIndent()

    fun buildEmergencyStructuredResponse(
        mode: AiDoctorMode,
        screen: EmergencyScreenResult
    ): StructuredAiResponse = StructuredAiResponse(
        mode = mode.modeId,
        summary = "EMERGENCY WARNING: Seek immediate medical attention right away.",
        answer = screen.guidance,
        observations = screen.matchedSignals,
        recommendations = listOf(
            "Call 112 (India Emergency) or your local emergency medical services immediately.",
            "Do not wait for an AI response or attempt strenuous activity or home remedies."
        ),
        followUpQuestions = emptyList(),
        doctorType = "Emergency Medical Services / Emergency Department",
        medicalAttention = MedicalAttentionAdvice(
            recommended = true,
            urgency = "emergency",
            reason = screen.reason
        ),
        emergencyScreen = screen,
        disclaimer = disclaimerFor(mode),
        providerUsed = "Independent Safety Screening Engine",
        fallbackUsed = false
    )

    fun buildDeterministicClinicalFallback(
        prompt: String,
        mode: AiDoctorMode,
        taskType: AiTaskType,
        language: AppLanguage,
        recentHeart: List<HeartMeasurement>,
        recentCheckIns: List<HealthCheckIn>
    ): StructuredAiResponse {
        val latestHeart = recentHeart.firstOrNull()
        val latestCheckIn = recentCheckIns.firstOrNull()

        val obs = mutableListOf<String>()
        latestHeart?.let { m ->
            m.restingHeartRateBpm?.let { obs += "User-entered resting HR: $it bpm (${m.recordedAtIso})" }
            if (m.systolicMmHg != null && m.diastolicMmHg != null) {
                obs += "User-entered BP: ${m.systolicMmHg}/${m.diastolicMmHg} mmHg (${m.recordedAtIso})"
            }
            m.oxygenSaturationPct?.let { obs += "User-entered SpO2: $it%" }
        }
        latestCheckIn?.let { ci ->
            obs += "Recent check-in (${ci.dateKey}): wellbeing ${ci.overallWellbeing}/10, sleep ${ci.sleepHours}h, stress ${ci.stressLevel}/10"
        }

        val (summary, answer, recs, docType, urgency) = when (mode) {
            AiDoctorMode.HEART_SPECIALIST -> FiveTuple(
                "Cardiovascular educational guidance based on your entered records (Rule-Based Fallback).",
                "I can help explain user-entered heart rate, blood pressure, cholesterol, or ECG report concepts, though live cloud AI is temporarily unreachable. Always review persistent palpitations, abnormal readings, or cardiac reports with a qualified physician or cardiologist.",
                listOf(
                    "Log resting BP & heart rate after 5 minutes of quiet sitting for consistent trends.",
                    "Avoid strenuous exertion if you notice chest discomfort, faintness, or breathlessness.",
                    "Use Appointment Prep to list your medications and questions for your cardiologist."
                ),
                "Cardiologist or Primary Care Physician",
                if ((latestHeart?.systolicMmHg ?: 0) >= 140) "prompt" else "routine"
            )
            AiDoctorMode.REPORT_EXPLAINER -> FiveTuple(
                "Safe lab/ECG terminology helper (Rule-Based Fallback).",
                "When reviewing any health or ECG report, preserve exact units (such as mg/dL, bpm, ms, mmol/L) and compare values strictly against the lab's printed reference range. Never interpret a single number in isolation without clinician context.",
                listOf(
                    "Highlight any measurement marked High/Low relative to the report's printed reference interval.",
                    "Confirm whether fasting or medication timing could affect the result.",
                    "Bring the original report to your doctor for formal medical interpretation."
                ),
                "Primary Care Physician or Ordering Specialist",
                "routine"
            )
            AiDoctorMode.SYMPTOM_CHECKER -> FiveTuple(
                "Structured symptom safety check (Rule-Based Fallback).",
                "Understanding how long a symptom has lasted, how intense it feels (1–10), and what other symptoms accompany it helps clinicians decide whether routine, prompt, or urgent care is needed.",
                listOf(
                    "Note exact onset time, triggers, and relieving factors.",
                    "Seek emergency help (112 in India or local EMS) if chest pain, breathlessness, fainting, or sudden weakness occur."
                ),
                "Primary Care Physician",
                "prompt"
            )
            AiDoctorMode.MENTAL_WELLNESS -> FiveTuple(
                "Supportive emotional & burnout recovery guidance (Rule-Based Fallback).",
                "Sustained stress and sleep debt impact both mental clarity and physical recovery. Gentle grounding routines and boundary-setting help, while persistent overwhelm benefits from professional counseling.",
                listOf(
                    "Try a 4-minute 4-4-4-4 box breathing reset in the Mental Wellness tool.",
                    "Step away from screens for 5 minutes every 90 minutes during shifts."
                ),
                "Counselor, Psychologist, or Primary Care Physician",
                "routine"
            )
            AiDoctorMode.HEALTH_NAVIGATOR -> FiveTuple(
                "Care navigation & appointment prep (Rule-Based Fallback).",
                "Choosing the right clinician saves time: Primary Care handles broad symptoms and preventive labs, Cardiology evaluates blood pressure, palpitations, and ECG findings, and Endocrinology supports blood glucose or thyroid concerns.",
                listOf(
                    "Write down your top 3 questions and current medication list before your visit.",
                    "Bring your exported Yaqeen Health trend summary."
                ),
                "Primary Care Physician / Cardiologist",
                "routine"
            )
            else -> FiveTuple(
                "Non-diagnostic preventive wellness overview (Rule-Based Fallback).",
                "Focus on consistent sleep (7–8 hours), hydration, moderate daily movement when feeling well, and early clinical follow-up if new or worsening symptoms appear.",
                listOf(
                    "Complete your daily check-in to keep trend trajectories updated.",
                    "Discuss persistent symptoms or medications with a qualified clinician."
                ),
                "Primary Care Physician",
                "routine"
            )
        }

        val langSuffix = if (language != AppLanguage.EN) " (${language.nativeLabel})" else ""
        return StructuredAiResponse(
            mode = mode.modeId,
            summary = summary + langSuffix,
            answer = answer,
            observations = obs.ifEmpty { listOf("No prior vital measurements supplied for this query.") },
            recommendations = recs,
            followUpQuestions = listOf(
                "How long have you noticed this concern?",
                "Are you experiencing any warning signs like shortness of breath or dizziness?"
            ),
            doctorType = docType,
            medicalAttention = MedicalAttentionAdvice(
                recommended = urgency != "routine",
                urgency = urgency,
                reason = "Guidance for task ${taskType.label}"
            ),
            emergencyScreen = EmergencyScreenResult(triggered = false),
            disclaimer = disclaimerFor(mode),
            providerUsed = "Rule-Based Clinical Fallback",
            fallbackUsed = true
        )
    }

    private data class FiveTuple(
        val summary: String,
        val answer: String,
        val recs: List<String>,
        val docType: String?,
        val urgency: String
    )

    private enum class SystemPromptType {
        GENERAL_AI_DOCTOR,
        HEART_SPECIALIST_AI,
        REPORT_EXPLAINER
    }

    private fun systemPromptFor(type: SystemPromptType): String = when (type) {
        SystemPromptType.GENERAL_AI_DOCTOR -> """
            You are Yaqeen Health's General AI Doctor Mode — an empathetic, clear, plain-language healthcare information assistant.
            MANDATORY SAFETY CONTRACT:
            1) NEVER claim to be a licensed doctor or claim you physically examined the user.
            2) NEVER diagnose diseases with certainty, prescribe medications, recommend dosage changes, or tell users to stop prescribed medicines.
            3) Ask relevant clarifying follow-up questions (duration, severity, associated symptoms).
            4) Identify emergency warning signs immediately and never falsely reassure users about serious symptoms.
            5) Recommend appropriate healthcare professionals (e.g., Primary Care, Cardiologist, Endocrinologist) and routine/prompt/urgent care levels.
            6) Never invent or assume missing clinical measurements.
        """.trimIndent()

        SystemPromptType.HEART_SPECIALIST_AI -> """
            You are YAQEEN AI CARDIOLOGY ASSISTANT (AI Heart Specialist Mode).
            MANDATORY CARDIOLOGY SAFETY CONTRACT:
            1) Specialize in cardiovascular health education: heart rate, blood pressure, palpitations, cholesterol, diabetes-heart connection, smoking cessation, preventive nutrition/activity, and cautious explanation of supplied ECG or cardiac lab reports.
            2) NEVER diagnose heart conditions or calculate an official clinical CVD diagnosis score. Distinguish user-entered readings from educational guidance.
            3) NEVER dismiss chest pain, chest pressure, shortness of breath, fainting, or jaw/arm radiation as gas, anxiety, or acidity without medical evaluation.
            4) Recognize emergency symptoms immediately and urge immediate emergency care (112 in India or local emergency services), telling users not to wait or substitute exercise/breathing/home remedies.
            5) Recommend appropriate cardiologist or physician follow-up and help prepare appointment questions.
        """.trimIndent()

        SystemPromptType.REPORT_EXPLAINER -> """
            You are Yaqeen Health's Report Explainer Mode.
            MANDATORY REPORT EXPLANATION RULES:
            1) Explain medical terminology, lab markers, and ECG terms in clear, simple language.
            2) Use ONLY the actual text, image values, and units supplied by the user. Preserve units strictly.
            3) Respect printed reference ranges when present, highlight out-of-range values clearly, and NEVER invent or guess missing values.
            4) Never diagnose from reports alone; always list thoughtful questions the user can ask their ordering doctor.
        """.trimIndent()
    }

    private fun disclaimerFor(mode: AiDoctorMode): String =
        if (mode == AiDoctorMode.HEART_SPECIALIST) StructuredAiResponse.HEART_MODE_DISCLAIMER
        else StructuredAiResponse.DOCTOR_MODE_DISCLAIMER

    companion object {
        private const val TAG = "AiClinicalOrchestrator"
    }
}

object StructuredAiResponseParser {

    fun parseAndValidate(
        rawJson: String,
        fallbackMode: String,
        fallbackDisclaimer: String,
        providerName: String
    ): StructuredAiResponse? {
        val cleaned = extractJsonObject(rawJson) ?: return null
        return try {
            val obj = JSONObject(cleaned)
            val summary = obj.optString("summary", "").trim()
            val answer = obj.optString("answer", "").trim()
            if (summary.isBlank() && answer.isBlank()) return null

            val mode = obj.optString("mode", fallbackMode).ifBlank { fallbackMode }
            val observations = obj.optJSONArray("observations").toStringList()
            val recommendations = obj.optJSONArray("recommendations").toStringList()
            val followUps = obj.optJSONArray("follow_up_questions").toStringList()
            val doctorType = obj.optString("doctor_type", "").trim().takeIf {
                it.isNotEmpty() && !it.equals("null", ignoreCase = true)
            }

            val medObj = obj.optJSONObject("medical_attention")
            val urgencyRaw = medObj?.optString("urgency", "routine")?.lowercase().orEmpty()
            val urgency = if (urgencyRaw in listOf("routine", "prompt", "urgent", "emergency")) urgencyRaw else "routine"
            val medicalAttention = MedicalAttentionAdvice(
                recommended = medObj?.optBoolean("recommended", urgency != "routine") ?: (urgency != "routine"),
                urgency = urgency,
                reason = medObj?.optString("reason", "").orEmpty().trim()
            )

            val emObj = obj.optJSONObject("emergency_screen")
            val emTriggered = emObj?.optBoolean("triggered", false) ?: false
            val emReason = emObj?.optString("reason", "").orEmpty().trim()

            val disclaimer = obj.optString("disclaimer", "").trim().ifBlank { fallbackDisclaimer }

            StructuredAiResponse(
                mode = mode,
                summary = summary.ifBlank { answer.take(140) },
                answer = answer.ifBlank { summary },
                observations = observations,
                recommendations = recommendations,
                followUpQuestions = followUps,
                doctorType = doctorType,
                medicalAttention = medicalAttention,
                emergencyScreen = EmergencyScreenResult(
                    triggered = emTriggered,
                    reason = emReason
                ),
                disclaimer = disclaimer,
                providerUsed = providerName,
                fallbackUsed = false
            )
        } catch (_: Exception) {
            null
        }
    }

    fun toJson(resp: StructuredAiResponse): String = JSONObject().apply {
        put("mode", resp.mode)
        put("summary", resp.summary)
        put("answer", resp.answer)
        put("observations", JSONArray(resp.observations))
        put("recommendations", JSONArray(resp.recommendations))
        put("follow_up_questions", JSONArray(resp.followUpQuestions))
        put("doctor_type", resp.doctorType ?: JSONObject.NULL)
        put(
            "medical_attention",
            JSONObject().apply {
                put("recommended", resp.medicalAttention.recommended)
                put("urgency", resp.medicalAttention.urgency)
                put("reason", resp.medicalAttention.reason)
            }
        )
        put(
            "emergency_screen",
            JSONObject().apply {
                put("triggered", resp.emergencyScreen.triggered)
                put("reason", resp.emergencyScreen.reason)
            }
        )
        put("disclaimer", resp.disclaimer)
        put("provider_used", resp.providerUsed)
        put("fallback_used", resp.fallbackUsed)
    }.toString()

    private fun extractJsonObject(raw: String): String? {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        return trimmed.substring(start, end + 1)
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .mapNotNull { idx -> optString(idx, "").trim().takeIf { it.isNotEmpty() } }
            .take(10)
    }
}
