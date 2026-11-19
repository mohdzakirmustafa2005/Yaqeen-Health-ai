package com.example.data

import android.util.Log
import com.example.engine.EarlyWarningEngine
import com.example.localization.AppLanguage
import com.example.model.AiChatMessage
import com.example.model.AiSender
import com.example.model.HealthCheckIn
import com.example.model.RecCategory
import com.example.model.RecFrequency
import com.example.model.RiskLevel
import com.example.model.WellnessRecommendation
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

enum class AiAssistantRole(
    val displayName: String,
    val subtitle: String,
    val modelName: String,
    val defaultSearchEnabled: Boolean,
    val systemDirective: String
) {
    WELLNESS_GUIDE(
        displayName = "Wellness Guide",
        subtitle = "Check-in trends & daily prevention",
        modelName = "gemini-3.8-flash",
        defaultSearchEnabled = false,
        systemDirective = "Focus on holistic workforce wellness, daily check-in pattern reflection, hydration, movement, and safe non-diagnostic preventive next steps."
    ),
    BURNOUT_COACH(
        displayName = "Burnout & Stress",
        subtitle = "Workload recovery & calm routines",
        modelName = "gemini-3.8-flash",
        defaultSearchEnabled = false,
        systemDirective = "Focus on occupational stress, burnout prevention, pacing work breaks, boundary setting, and nervous-system grounding techniques such as 4-4-4-4 box breathing."
    ),
    QUICK_SLEEP_COACH(
        displayName = "Quick Sleep Coach",
        subtitle = "Fast, concise sleep & energy tips",
        modelName = "gemini-3.5-flash-lite",
        defaultSearchEnabled = false,
        systemDirective = "Keep answers crisp, practical, and action-focused (3–5 bullet points) covering circadian consistency, caffeine cutoffs, wind-down cues, and daytime energy recovery."
    ),
    PUBLIC_HEALTH_SEARCH(
        displayName = "Health Search",
        subtitle = "Live Google Search grounded guidelines",
        modelName = "gemini-3.8-flash",
        defaultSearchEnabled = true,
        systemDirective = "Use Google Search grounding to retrieve up-to-date public health, occupational wellness, WHO, CDC, or NIOSH guidance and summarize findings clearly without diagnosing."
    )
}

data class GroundingSource(
    val title: String,
    val domain: String,
    val uri: String,
    val searchQuery: String = ""
)

data class AiCheckInInsight(
    val summary: String,
    val recommendations: List<WellnessRecommendation>,
    val fallbackUsed: Boolean,
    val errorNotice: String? = null
)

sealed interface AiStreamChunk {
    data class Partial(val accumulatedText: String) : AiStreamChunk
    data class Completed(
        val finalText: String,
        val fallbackUsed: Boolean,
        val errorNotice: String? = null,
        val groundingSources: List<GroundingSource> = emptyList(),
        val searchQueries: List<String> = emptyList()
    ) : AiStreamChunk
}

/**
 * AI-provider abstraction layer supporting multi-turn chat, role-based instructions,
 * structured JSON insights, and Google Search grounding via Firebase AI Logic.
 */
interface AiHealthService {
    suspend fun analyzeCheckIn(
        latest: HealthCheckIn,
        history: List<HealthCheckIn>,
        riskLevel: RiskLevel,
        language: AppLanguage,
        userId: String
    ): AiCheckInInsight

    fun streamAssistantReply(
        userPrompt: String,
        history: List<AiChatMessage>,
        recentCheckIns: List<HealthCheckIn>,
        language: AppLanguage,
        role: AiAssistantRole = AiAssistantRole.WELLNESS_GUIDE,
        useGoogleSearch: Boolean = false
    ): Flow<AiStreamChunk>
}

class FirebaseAiHealthService : AiHealthService {

    private val modelCache = mutableMapOf<Pair<AiAssistantRole, Boolean>, GenerativeModel>()

    @Synchronized
    private fun getOrCreateChatModel(role: AiAssistantRole, useGoogleSearch: Boolean): GenerativeModel {
        val key = role to useGoogleSearch
        return modelCache.getOrPut(key) {
            val toolsList = if (useGoogleSearch) listOf(Tool.googleSearch()) else emptyList()
            Firebase.ai.generativeModel(
                modelName = role.modelName,
                tools = toolsList.ifEmpty { null },
                systemInstruction = content {
                    text(
                        "You are Yaqeen Health's non-diagnostic AI Workforce Wellness & Prevention Guide (${role.displayName}). " +
                            "${role.systemDirective} " +
                            "Safety Rules: 1) NEVER diagnose medical conditions or claim clinical certainty. " +
                            "2) NEVER prescribe medications or tell users to stop prescribed medications. " +
                            "3) Recognize emergency symptoms (chest pain, breathing difficulty, fainting, acute crisis) and urge immediate emergency medical care. " +
                            "4) Maintain multi-turn context coherently across the user's conversation history. " +
                            "5) End with a brief reminder that this information does not replace professional medical care."
                    )
                }
            )
        }
    }

    private val structuredSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Brief, empathetic non-diagnostic summary of the check-in and trend"),
                "recommendations" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "category" to Schema.enumeration(
                                values = RecCategory.entries.map { it.name },
                                description = "Wellness category"
                            ),
                            "title" to Schema.string(description = "Short action title"),
                            "description" to Schema.string(description = "Practical non-diagnostic step"),
                            "frequency" to Schema.enumeration(
                                values = listOf("DAILY", "WEEKLY"),
                                description = "Cadence"
                            )
                        )
                    ),
                    description = "2 to 3 personalized preventive actions"
                )
            )
        )
    }

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredSchema
            }
        )
    }

    override suspend fun analyzeCheckIn(
        latest: HealthCheckIn,
        history: List<HealthCheckIn>,
        riskLevel: RiskLevel,
        language: AppLanguage,
        userId: String
    ): AiCheckInInsight {
        val trendSnippet = (listOf(latest) + history).take(5).joinToString("\n") { ci ->
            "- ${ci.dateKey}: wellbeing=${ci.overallWellbeing}/10, sleep=${ci.sleepHours}h (q=${ci.sleepQuality}/10), stress=${ci.stressLevel}/10, fatigue=${ci.fatigueLevel}/10, symptoms=${ci.symptoms.joinToString()}"
        }
        val prompt = """
            Respond in ${language.englishName}.
            Analyze this worker's daily wellness check-in (non-diagnostic prevention support only, no diagnosis, no prescriptions).
            Computed Early Warning Level: ${riskLevel.name}.
            Recent check-ins:
            $trendSnippet
            Notes: ${latest.notes ?: "None"}
            Provide a 2-3 sentence non-diagnostic trend summary and 2-3 safe, practical preventive recommendations.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty AI JSON response")
            val root = JSONObject(jsonText)
            val summary = root.getString("summary")
            val arr = root.getJSONArray("recommendations")
            val recs = (0 until arr.length()).map { idx ->
                val obj = arr.getJSONObject(idx)
                WellnessRecommendation(
                    userId = userId,
                    category = RecCategory.fromRaw(obj.optString("category")).name,
                    title = obj.optString("title", "Wellness Habit").take(120),
                    description = obj.optString("description", "Focus on rest and hydration.").take(500),
                    frequency = if (obj.optString("frequency") == "WEEKLY") RecFrequency.WEEKLY.name else RecFrequency.DAILY.name,
                    completed = false,
                    source = "AI"
                )
            }
            AiCheckInInsight(
                summary = summary,
                recommendations = recs,
                fallbackUsed = false
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI check-in analysis timed out", e)
            buildFallbackInsight(latest, history, riskLevel, language, userId, "AI analysis timed out; showing rule-based safety guidance.")
        } catch (e: Exception) {
            Log.e(TAG, "AI check-in analysis failed, using safe fallback", e)
            buildFallbackInsight(latest, history, riskLevel, language, userId, e.localizedMessage ?: "AI service unavailable; showing rule-based safety guidance.")
        }
    }

    override fun streamAssistantReply(
        userPrompt: String,
        history: List<AiChatMessage>,
        recentCheckIns: List<HealthCheckIn>,
        language: AppLanguage,
        role: AiAssistantRole,
        useGoogleSearch: Boolean
    ): Flow<AiStreamChunk> = flow {
        val searchActive = useGoogleSearch || role.defaultSearchEnabled
        val model = getOrCreateChatModel(role, searchActive)
        val sdkHistory = buildChatHistory(history)

        val contextLines = recentCheckIns.take(4).joinToString("\n") { ci ->
            "${ci.dateKey}: wellbeing ${ci.overallWellbeing}/10, sleep ${ci.sleepHours}h, stress ${ci.stressLevel}/10, fatigue ${ci.fatigueLevel}/10, risk ${ci.computedRiskLevel}"
        }.ifBlank { "No recent check-ins recorded yet." }

        val fullPrompt = """
            Preferred language: ${language.englishName}. Reply strictly in ${language.englishName}.
            Active specialist mode: ${role.displayName}.
            Recent non-diagnostic check-in context:
            $contextLines

            User message: $userPrompt
        """.trimIndent()

        val accumulated = StringBuilder()
        val collectedSources = linkedMapOf<String, GroundingSource>()
        val collectedQueries = linkedSetOf<String>()

        withTimeout(60_000L) {
            val chat = model.startChat(history = sdkHistory)
            chat.sendMessageStream(fullPrompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(AiStreamChunk.Partial(accumulated.toString()))
                }
                extractGrounding(chunk, collectedQueries, collectedSources)
            }
        }

        val final = accumulated.toString().trim()
        if (final.isNotEmpty()) {
            emit(
                AiStreamChunk.Completed(
                    finalText = final,
                    fallbackUsed = false,
                    groundingSources = collectedSources.values.toList(),
                    searchQueries = collectedQueries.toList()
                )
            )
        } else {
            emit(
                AiStreamChunk.Completed(
                    finalText = RuleBasedFallbackAdvisor.reply(userPrompt, recentCheckIns, language, role),
                    fallbackUsed = true,
                    errorNotice = "Model returned an empty response; showing rule-based wellness guidance."
                )
            )
        }
    }.catch { error ->
        if (error is CancellationException && error !is TimeoutCancellationException) {
            throw error
        }
        Log.e(TAG, "Streaming AI reply failed, falling back safely", error)
        emit(
            AiStreamChunk.Completed(
                finalText = RuleBasedFallbackAdvisor.reply(userPrompt, recentCheckIns, language, role),
                fallbackUsed = true,
                errorNotice = error.localizedMessage ?: "Gemini AI unreachable; using safe offline guidance."
            )
        )
    }

    private fun extractGrounding(
        response: GenerateContentResponse,
        queriesOut: MutableSet<String>,
        sourcesOut: MutableMap<String, GroundingSource>
    ) {
        val metadata = response.candidates.firstOrNull()?.groundingMetadata ?: return
        metadata.webSearchQueries.forEach { query ->
            if (query.isNotBlank()) queriesOut.add(query.trim())
        }
        val queryLabel = queriesOut.firstOrNull().orEmpty()
        metadata.groundingChunks.forEach { chunk ->
            val web = chunk.web ?: return@forEach
            val uri = web.uri?.trim().orEmpty()
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                val title = web.title?.trim().orEmpty().ifBlank { web.domain?.trim().orEmpty().ifBlank { "Verified Source" } }
                val domain = web.domain?.trim().orEmpty().ifBlank {
                    uri.removePrefix("https://").removePrefix("http://").substringBefore("/")
                }
                sourcesOut[uri] = GroundingSource(
                    title = title,
                    domain = domain,
                    uri = uri,
                    searchQuery = queryLabel
                )
            }
        }
    }

    private fun buildFallbackInsight(
        latest: HealthCheckIn,
        history: List<HealthCheckIn>,
        riskLevel: RiskLevel,
        language: AppLanguage,
        userId: String,
        notice: String
    ): AiCheckInInsight {
        val eval = EarlyWarningEngine.evaluate(latest, history, userId)
        val prefix = when (language) {
            AppLanguage.UR -> "[محفوظ متبادل تجزیہ] "
            AppLanguage.HI -> "[सुरक्षित फ़ॉलबैक सारांश] "
            AppLanguage.TE -> "[సేఫ్ ఫాల్‌బ్యాక్ సారాంశం] "
            AppLanguage.EN -> "[Safe Fallback Summary] "
        }
        return AiCheckInInsight(
            summary = "$prefix${eval.explanation} ${eval.recommendedNextStep}",
            recommendations = eval.suggestedRecommendations,
            fallbackUsed = true,
            errorNotice = notice
        )
    }

    companion object {
        private const val TAG = "FirebaseAiHealth"

        fun buildChatHistory(messages: List<AiChatMessage>, maxTurns: Int = 12): List<Content> {
            return messages
                .filter { it.text.isNotBlank() }
                .takeLast(maxTurns)
                .map { msg ->
                    val role = if (msg.sender == AiSender.USER.name) "user" else "model"
                    content(role) { text(msg.text) }
                }
        }
    }
}

object RuleBasedFallbackAdvisor {
    fun reply(
        prompt: String,
        recent: List<HealthCheckIn>,
        language: AppLanguage,
        role: AiAssistantRole = AiAssistantRole.WELLNESS_GUIDE
    ): String {
        val lower = prompt.lowercase()
        val emergencyHit = listOf("chest", "breath", "faint", "dizzy", "emergency", "pain").any { lower.contains(it) }
        val latest = recent.firstOrNull()

        val core = when {
            emergencyHit -> "If you are experiencing chest pain, shortness of breath, fainting, or severe symptoms, please seek emergency medical care or contact a qualified clinician right away."
            role == AiAssistantRole.BURNOUT_COACH || lower.contains("stress") || lower.contains("burnout") || lower.contains("anx") ->
                "Sustained stress and workload pressure can build fatigue over several days. Consider a 4-4-4-4 box breathing reset, 5-minute breaks every 90 minutes, and sharing workload concerns with a coordinator."
            role == AiAssistantRole.QUICK_SLEEP_COACH || lower.contains("sleep") || lower.contains("tired") || lower.contains("fatigue") ->
                "Recent sleep consistency strongly shapes daily recovery. Try dimming screens 45 minutes before bed, keeping a regular wake time, and taking a 10-minute daylight walk."
            role == AiAssistantRole.PUBLIC_HEALTH_SEARCH ->
                "Live Google Search grounding could not be reached offline. Standard WHO & CDC workplace guidance highlights 7–9 hours of sleep, regular hydration, movement breaks every hour, and early clinical follow-up for persistent symptoms."
            latest != null ->
                "Your latest check-in shows wellbeing ${latest.overallWellbeing}/10, sleep ${latest.sleepHours}h, and stress ${latest.stressLevel}/10 (${latest.computedRiskLevel}). Focus on steady hydration, rest recovery, and tracking tomorrow's trend."
            else ->
                "Start by logging a daily check-in covering sleep, stress, energy, and symptoms so I can help explain your wellness trends safely."
        }

        val disclaimer = when (language) {
            AppLanguage.HI -> "नोट: यह सुरक्षित फ़ॉलबैक मार्गदर्शन है और पेशेवर चिकित्सा देखभाल का विकल्प नहीं है।"
            AppLanguage.TE -> "గమనిక: ఇది సేఫ్ ఫాల్‌బ్యాక్ సూచన మాత్రమే; వృత్తిపరమైన వైద్య సంరక్షణకు ప్రత్యామ్నాయం కాదు."
            AppLanguage.UR -> "نوٹ: یہ محفوظ متبادل رہنمائی ہے اور پیشہ ورانہ طبی دیکھ بھال کا نعم البدل نہیں ہے۔"
            AppLanguage.EN -> "Note: Safe fallback guidance — this wellness information does not replace professional medical care."
        }
        return "$core\n\n$disclaimer"
    }
}
