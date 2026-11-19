package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiAssistantRole
import com.example.data.AiClinicalOrchestrator
import com.example.data.AiHealthService
import com.example.data.AiProviderHealth
import com.example.data.AiStreamChunk
import com.example.data.ClinicalAiInvocationResult
import com.example.data.DemoHealthRepository
import com.example.data.GroundingSource
import com.example.data.HealthRepository
import com.example.data.PreferencesStore
import com.example.data.StructuredAiResponseParser
import com.example.data.local.InMemoryLocalSqlStore
import com.example.data.local.LocalSqlStore
import com.example.data.local.LocalSqlSummary
import com.example.engine.DemoDataFactory
import com.example.engine.EarlyWarningEngine
import com.example.engine.HeartRiskEducationEngine
import com.example.engine.ReportFormatter
import com.example.engine.RiskEvaluationResult
import com.example.engine.SafetyScreeningEngine
import com.example.localization.AppLanguage
import com.example.model.AiChatMessage
import com.example.model.AiDoctorMode
import com.example.model.AiHealthInsightRecord
import com.example.model.AiSender
import com.example.model.AiTaskType
import com.example.model.AlertStatus
import com.example.model.AuditLogEntry
import com.example.model.BloodPressureRecord
import com.example.model.CoordinatorOutreachCase
import com.example.model.DoctorAiMessage
import com.example.model.DoctorConversation
import com.example.model.DoctorDomain
import com.example.model.EmergencyScreenResult
import com.example.model.HealthCheckIn
import com.example.model.HealthReportUpload
import com.example.model.HeartHealthProfile
import com.example.model.HeartMeasurement
import com.example.model.HeartSymptomReport
import com.example.model.MedicalSafetyEvent
import com.example.model.NotifCategory
import com.example.model.RiskAlert
import com.example.model.RiskLevel
import com.example.model.SmartNotification
import com.example.model.StructuredAiResponse
import com.example.model.UserConsentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.model.WellnessRecommendation
import com.example.model.WorkforceAnalyticsSummary
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CheckInSubmissionOutcome(
    val evaluation: RiskEvaluationResult,
    val aiSummary: String,
    val fallbackUsed: Boolean,
    val aiNotice: String?
)

data class HealthDashboardMetrics(
    val latestWellnessScore: Int,
    val burnoutIndex: Int,
    val currentRiskLevel: RiskLevel,
    val avgSleepHours: Float,
    val avgStress: Float,
    val activeAlertsCount: Int,
    val unreadNotifsCount: Int,
    val completedRecsCount: Int,
    val totalRecsCount: Int
)

class HealthViewModel(
    private val repository: HealthRepository,
    private val aiService: AiHealthService,
    private val preferencesStore: PreferencesStore,
    val currentUserId: String,
    val initialDisplayName: String,
    val isDemoMode: Boolean,
    private val localSqlStore: LocalSqlStore = InMemoryLocalSqlStore(),
    private val clinicalOrchestrator: AiClinicalOrchestrator = AiClinicalOrchestrator()
) : ViewModel() {

    val language: StateFlow<AppLanguage> = preferencesStore.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppLanguage.EN)

    val darkModeOverride: StateFlow<Boolean?> = preferencesStore.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { err -> emit(handleStreamError(err, "profile")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val checkInsState: StateFlow<UiState<List<HealthCheckIn>>> = repository.observeCheckIns(currentUserId)
        .map<List<HealthCheckIn>, UiState<List<HealthCheckIn>>> { list ->
            localSqlStore.syncCheckIns(currentUserId, list)
            UiState.Success(list)
        }
        .catch { err -> emit(handleStreamError(err, "check-ins")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val localSqlSummary: StateFlow<LocalSqlSummary?> = localSqlStore.observeSqlSummary(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val groundingSourcesByMessage: StateFlow<Map<String, List<GroundingSource>>> =
        localSqlStore.observeGroundingSourcesByMessage(currentUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    val alertsState: StateFlow<UiState<List<RiskAlert>>> = repository.observeAlerts(currentUserId)
        .map<List<RiskAlert>, UiState<List<RiskAlert>>> { UiState.Success(it) }
        .catch { err -> emit(handleStreamError(err, "alerts")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val recommendationsState: StateFlow<UiState<List<WellnessRecommendation>>> =
        repository.observeRecommendations(currentUserId)
            .map<List<WellnessRecommendation>, UiState<List<WellnessRecommendation>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "recommendations")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val aiMessagesState: StateFlow<UiState<List<AiChatMessage>>> = repository.observeAiMessages(currentUserId)
        .map<List<AiChatMessage>, UiState<List<AiChatMessage>>> { UiState.Success(it) }
        .catch { err -> emit(handleStreamError(err, "AI messages")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val notificationsState: StateFlow<UiState<List<SmartNotification>>> =
        repository.observeNotifications(currentUserId)
            .map<List<SmartNotification>, UiState<List<SmartNotification>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "notifications")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val auditLogsState: StateFlow<UiState<List<AuditLogEntry>>> = repository.observeAuditLogs(currentUserId)
        .map<List<AuditLogEntry>, UiState<List<AuditLogEntry>>> { UiState.Success(it) }
        .catch { err -> emit(handleStreamError(err, "audit logs")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val dashboardMetrics: StateFlow<HealthDashboardMetrics> = combine(
        checkInsState,
        alertsState,
        recommendationsState,
        notificationsState
    ) { ciState, alState, recState, notifState ->
        val checkIns = (ciState as? UiState.Success)?.data.orEmpty()
        val alerts = (alState as? UiState.Success)?.data.orEmpty()
        val recs = (recState as? UiState.Success)?.data.orEmpty()
        val notifs = (notifState as? UiState.Success)?.data.orEmpty()
        val latest = checkIns.firstOrNull()
        val burnout = if (latest != null) EarlyWarningEngine.computeBurnoutIndex(latest, checkIns.take(7)) else 18
        val avgSleep = checkIns.take(7).map { it.sleepHours }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val avgStress = checkIns.take(7).map { it.stressLevel }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f

        HealthDashboardMetrics(
            latestWellnessScore = (latest?.overallWellbeing?.toInt() ?: 0) * 10,
            burnoutIndex = burnout,
            currentRiskLevel = RiskLevel.fromRaw(latest?.computedRiskLevel),
            avgSleepHours = avgSleep,
            avgStress = avgStress,
            activeAlertsCount = alerts.count { it.status == AlertStatus.ACTIVE.name },
            unreadNotifsCount = notifs.count { !it.readStatus },
            completedRecsCount = recs.count { it.completed },
            totalRecsCount = recs.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        HealthDashboardMetrics(0, 18, RiskLevel.GREEN, 0f, 0f, 0, 0, 0, 0)
    )

    private val _activeRoleOverride = MutableStateFlow<UserRole?>(null)
    val activeRole: StateFlow<UserRole> = combine(profileState, _activeRoleOverride) { profState, override ->
        override ?: UserRole.fromRaw((profState as? UiState.Success)?.data?.role)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UserRole.INDIVIDUAL)

    private val _isSubmittingCheckIn = MutableStateFlow(false)
    val isSubmittingCheckIn: StateFlow<Boolean> = _isSubmittingCheckIn.asStateFlow()

    private val _lastSubmissionOutcome = MutableStateFlow<CheckInSubmissionOutcome?>(null)
    val lastSubmissionOutcome: StateFlow<CheckInSubmissionOutcome?> = _lastSubmissionOutcome.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _isAiBusy = MutableStateFlow(false)
    val isAiBusy: StateFlow<Boolean> = _isAiBusy.asStateFlow()

    private val _actionStatusMessage = MutableStateFlow<String?>(null)
    val actionStatusMessage: StateFlow<String?> = _actionStatusMessage.asStateFlow()

    private val _workforceAnalytics = MutableStateFlow(DemoDataFactory.demoWorkforceAnalytics())
    val workforceAnalytics: StateFlow<WorkforceAnalyticsSummary> = _workforceAnalytics.asStateFlow()

    private val _coordinatorCases = MutableStateFlow(DemoDataFactory.demoCoordinatorCases())
    val coordinatorCases: StateFlow<List<CoordinatorOutreachCase>> = _coordinatorCases.asStateFlow()

    private val _aiCallsCount = MutableStateFlow(if (isDemoMode) 4 else 0)
    val aiCallsCount: StateFlow<Int> = _aiCallsCount.asStateFlow()

    private val _selectedAiRole = MutableStateFlow(AiAssistantRole.WELLNESS_GUIDE)
    val selectedAiRole: StateFlow<AiAssistantRole> = _selectedAiRole.asStateFlow()

    private val _googleSearchEnabled = MutableStateFlow(false)
    val googleSearchEnabled: StateFlow<Boolean> = _googleSearchEnabled.asStateFlow()

    // --- AI Doctor & AI Heart Specialist Streams & State ---
    val conversationsState: StateFlow<UiState<List<DoctorConversation>>> =
        repository.observeDoctorConversations(currentUserId)
            .map<List<DoctorConversation>, UiState<List<DoctorConversation>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "doctor conversations")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val allDoctorMessagesState: StateFlow<UiState<List<DoctorAiMessage>>> =
        repository.observeDoctorMessages(currentUserId)
            .map<List<DoctorAiMessage>, UiState<List<DoctorAiMessage>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "doctor messages")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeDoctorMessagesState: StateFlow<List<DoctorAiMessage>> = combine(
        allDoctorMessagesState,
        _activeConversationId
    ) { msgState, convId ->
        val all = (msgState as? UiState.Success)?.data.orEmpty()
        if (convId.isNullOrBlank()) emptyList() else all.filter { it.conversationId == convId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val heartProfileState: StateFlow<UiState<HeartHealthProfile?>> =
        repository.observeHeartProfile(currentUserId)
            .map<HeartHealthProfile?, UiState<HeartHealthProfile?>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "heart profile")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val heartMeasurementsState: StateFlow<UiState<List<HeartMeasurement>>> =
        repository.observeHeartMeasurements(currentUserId)
            .map<List<HeartMeasurement>, UiState<List<HeartMeasurement>>> { list ->
                localSqlStore.syncHeartMeasurements(currentUserId, list)
                UiState.Success(list)
            }
            .catch { err -> emit(handleStreamError(err, "heart measurements")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val bloodPressureState: StateFlow<UiState<List<BloodPressureRecord>>> =
        repository.observeBloodPressureRecords(currentUserId)
            .map<List<BloodPressureRecord>, UiState<List<BloodPressureRecord>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "blood pressure logs")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val heartSymptomReportsState: StateFlow<UiState<List<HeartSymptomReport>>> =
        repository.observeHeartSymptomReports(currentUserId)
            .map<List<HeartSymptomReport>, UiState<List<HeartSymptomReport>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "heart symptom reports")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val healthReportUploadsState: StateFlow<UiState<List<HealthReportUpload>>> =
        repository.observeHealthReportUploads(currentUserId)
            .map<List<HealthReportUpload>, UiState<List<HealthReportUpload>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "uploaded health reports")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val aiInsightsState: StateFlow<UiState<List<AiHealthInsightRecord>>> =
        repository.observeAiInsights(currentUserId)
            .map<List<AiHealthInsightRecord>, UiState<List<AiHealthInsightRecord>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "AI clinical insights")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val safetyEventsState: StateFlow<UiState<List<MedicalSafetyEvent>>> =
        repository.observeSafetyEvents(currentUserId)
            .map<List<MedicalSafetyEvent>, UiState<List<MedicalSafetyEvent>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "safety events")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val consentRecordsState: StateFlow<UiState<List<UserConsentRecord>>> =
        repository.observeConsentRecords(currentUserId)
            .map<List<UserConsentRecord>, UiState<List<UserConsentRecord>>> { UiState.Success(it) }
            .catch { err -> emit(handleStreamError(err, "consent records")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    private val _selectedDoctorMode = MutableStateFlow(AiDoctorMode.GENERAL_HEALTH)
    val selectedDoctorMode: StateFlow<AiDoctorMode> = _selectedDoctorMode.asStateFlow()

    private val _isDoctorAiBusy = MutableStateFlow(false)
    val isDoctorAiBusy: StateFlow<Boolean> = _isDoctorAiBusy.asStateFlow()

    private val _doctorAiError = MutableStateFlow<String?>(null)
    val doctorAiError: StateFlow<String?> = _doctorAiError.asStateFlow()

    private val _lastClinicalResult = MutableStateFlow<ClinicalAiInvocationResult?>(null)
    val lastClinicalResult: StateFlow<ClinicalAiInvocationResult?> = _lastClinicalResult.asStateFlow()

    private val _lastAppointmentPrepResult = MutableStateFlow<StructuredAiResponse?>(null)
    val lastAppointmentPrepResult: StateFlow<StructuredAiResponse?> = _lastAppointmentPrepResult.asStateFlow()

    private val _activeEmergencyBanner = MutableStateFlow<EmergencyScreenResult?>(null)
    val activeEmergencyBanner: StateFlow<EmergencyScreenResult?> = _activeEmergencyBanner.asStateFlow()

    private var lastSubmittedDoctorTurn: Triple<String, AiDoctorMode, AiTaskType>? = null

    val providerStatuses: List<AiProviderHealth>
        get() = clinicalOrchestrator.getProviderStatuses()

    init {
        if (!isDemoMode) {
            viewModelScope.launch {
                repository.ensureUserProfile(currentUserId, initialDisplayName)
                    .onFailure { err ->
                        _actionStatusMessage.value = err.localizedMessage ?: "Failed to initialize profile."
                    }
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            preferencesStore.setLanguage(lang)
            val currentProfile = (profileState.value as? UiState.Success)?.data
            if (currentProfile != null) {
                repository.updateProfilePreferences(
                    userId = currentUserId,
                    displayName = currentProfile.displayName,
                    department = currentProfile.department,
                    language = lang.code,
                    consentAnalytics = currentProfile.consentAnalytics,
                    consentAiGuidance = currentProfile.consentAiGuidance,
                    consentCoordinatorOutreach = currentProfile.consentCoordinatorOutreach,
                    remindersEnabled = currentProfile.remindersEnabled,
                    reminderHour = currentProfile.reminderHour,
                    reminderMinute = currentProfile.reminderMinute,
                    onboardingCompleted = currentProfile.onboardingCompleted
                )
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setDarkMode(enabled) }
    }

    fun switchDemoRole(role: UserRole) {
        if (isDemoMode) {
            _activeRoleOverride.value = role
            (repository as? DemoHealthRepository)?.setDemoRole(role)
        }
    }

    fun clearStatusMessage() {
        _actionStatusMessage.value = null
    }

    fun clearSubmissionOutcome() {
        _lastSubmissionOutcome.value = null
    }

    fun selectAiRole(role: AiAssistantRole) {
        _selectedAiRole.value = role
        if (role.defaultSearchEnabled) {
            _googleSearchEnabled.value = true
        }
    }

    fun toggleGoogleSearch(enabled: Boolean) {
        _googleSearchEnabled.value = enabled
    }

    fun completeOnboarding(displayName: String, department: String, consentAnalytics: Boolean, consentAi: Boolean) {
        viewModelScope.launch {
            val currentProfile = (profileState.value as? UiState.Success)?.data
            val result = repository.updateProfilePreferences(
                userId = currentUserId,
                displayName = displayName.ifBlank { initialDisplayName },
                department = department.ifBlank { "Operations" },
                language = language.value.code,
                consentAnalytics = consentAnalytics,
                consentAiGuidance = consentAi,
                consentCoordinatorOutreach = currentProfile?.consentCoordinatorOutreach ?: false,
                remindersEnabled = currentProfile?.remindersEnabled ?: true,
                reminderHour = currentProfile?.reminderHour ?: 9L,
                reminderMinute = currentProfile?.reminderMinute ?: 0L,
                onboardingCompleted = true
            )
            result.onSuccess {
                repository.appendAuditLog(
                    currentUserId,
                    "ONBOARDING_COMPLETED",
                    "Completed health profile and privacy consents.",
                    activeRole.value.name
                )
            }.onFailure { err ->
                _actionStatusMessage.value = err.localizedMessage ?: "Could not save onboarding profile."
            }
        }
    }

    fun updatePrivacyAndReminders(
        displayName: String,
        department: String,
        consentAnalytics: Boolean,
        consentAiGuidance: Boolean,
        consentCoordinatorOutreach: Boolean,
        remindersEnabled: Boolean,
        reminderHour: Long,
        reminderMinute: Long
    ) {
        viewModelScope.launch {
            val currentProfile = (profileState.value as? UiState.Success)?.data
            val result = repository.updateProfilePreferences(
                userId = currentUserId,
                displayName = displayName,
                department = department,
                language = language.value.code,
                consentAnalytics = consentAnalytics,
                consentAiGuidance = consentAiGuidance,
                consentCoordinatorOutreach = consentCoordinatorOutreach,
                remindersEnabled = remindersEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                onboardingCompleted = currentProfile?.onboardingCompleted ?: true
            )
            result.onSuccess {
                repository.appendAuditLog(
                    currentUserId,
                    "PRIVACY_SETTINGS_UPDATED",
                    "Analytics=$consentAnalytics, AI=$consentAiGuidance, Outreach=$consentCoordinatorOutreach, Reminders=$remindersEnabled",
                    activeRole.value.name
                )
                _actionStatusMessage.value = "Privacy & reminder preferences saved."
            }.onFailure { err ->
                _actionStatusMessage.value = err.localizedMessage ?: "Failed to update settings."
            }
        }
    }

    fun submitCheckIn(draft: HealthCheckIn) {
        if (_isSubmittingCheckIn.value) return
        viewModelScope.launch {
            _isSubmittingCheckIn.value = true
            try {
                val history = (checkInsState.value as? UiState.Success)?.data.orEmpty()
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val eval = EarlyWarningEngine.evaluate(
                    latest = draft.copy(dateKey = dateKey),
                    history = history,
                    userId = currentUserId
                )
                val checkInToSave = draft.copy(
                    userId = currentUserId,
                    dateKey = dateKey,
                    computedRiskLevel = eval.riskLevel.name
                )

                val saveResult = repository.saveCheckIn(currentUserId, checkInToSave)
                val savedId = saveResult.getOrElse { err ->
                    _actionStatusMessage.value = err.localizedMessage ?: "Failed to save check-in."
                    return@launch
                }

                // Generate alert & smart notification when warning level >= YELLOW
                val alert = EarlyWarningEngine.buildAlertFromEvaluation(eval, dateKey, currentUserId)
                if (alert != null) {
                    repository.saveAlert(currentUserId, alert)
                    repository.saveNotification(
                        currentUserId,
                        SmartNotification(
                            userId = currentUserId,
                            category = NotifCategory.ALERT.name,
                            title = "${eval.riskLevel.label}: ${eval.title}",
                            body = eval.recommendedNextStep,
                            readStatus = false
                        )
                    )
                }

                // Request AI check-in summary & recommendations if AI consent enabled
                val profile = (profileState.value as? UiState.Success)?.data
                val allowAi = profile?.consentAiGuidance ?: true
                val aiInsight = if (allowAi) {
                    _aiCallsCount.update { it + 1 }
                    aiService.analyzeCheckIn(
                        latest = checkInToSave,
                        history = history,
                        riskLevel = eval.riskLevel,
                        language = language.value,
                        userId = currentUserId
                    )
                } else {
                    com.example.data.AiCheckInInsight(
                        summary = "${eval.explanation} ${eval.recommendedNextStep}",
                        recommendations = eval.suggestedRecommendations,
                        fallbackUsed = true,
                        errorNotice = "AI guidance disabled in Privacy Settings; using rule-based prevention engine."
                    )
                }

                repository.updateCheckInAiSummary(currentUserId, savedId, aiInsight.summary)
                val recsToSave = (aiInsight.recommendations + eval.suggestedRecommendations).distinctBy { it.title }.take(4)
                if (recsToSave.isNotEmpty()) {
                    repository.saveRecommendations(currentUserId, recsToSave)
                }
                repository.appendAuditLog(
                    currentUserId,
                    "CHECK_IN_ANALYZED",
                    "Saved $dateKey check-in (Risk: ${eval.riskLevel.name}, AI fallback: ${aiInsight.fallbackUsed}).",
                    activeRole.value.name
                )

                _lastSubmissionOutcome.value = CheckInSubmissionOutcome(
                    evaluation = eval,
                    aiSummary = aiInsight.summary,
                    fallbackUsed = aiInsight.fallbackUsed,
                    aiNotice = aiInsight.errorNotice
                )
            } finally {
                _isSubmittingCheckIn.value = false
            }
        }
    }

    fun sendAiMessage(userText: String) {
        val clean = userText.trim()
        if (clean.isBlank() || _isAiBusy.value) return
        val profile = (profileState.value as? UiState.Success)?.data
        if (profile?.consentAiGuidance == false) {
            _actionStatusMessage.value = "Enable AI Wellness Guidance in Privacy & Settings to use the AI assistant."
            return
        }

        viewModelScope.launch {
            _isAiBusy.value = true
            _streamingReply.value = ""
            try {
                val lang = language.value
                val role = _selectedAiRole.value
                val searchEnabled = _googleSearchEnabled.value || role.defaultSearchEnabled
                val priorHistory = (aiMessagesState.value as? UiState.Success)?.data.orEmpty()

                repository.saveAiMessage(
                    currentUserId,
                    AiChatMessage(
                        userId = currentUserId,
                        sender = AiSender.USER.name,
                        text = clean,
                        language = lang.code,
                        fallbackUsed = false
                    )
                )

                val recent = (checkInsState.value as? UiState.Success)?.data.orEmpty()
                _aiCallsCount.update { it + 1 }
                aiService.streamAssistantReply(
                    userPrompt = clean,
                    history = priorHistory,
                    recentCheckIns = recent,
                    language = lang,
                    role = role,
                    useGoogleSearch = searchEnabled
                ).collect { chunk ->
                    when (chunk) {
                        is AiStreamChunk.Partial -> _streamingReply.value = chunk.accumulatedText
                        is AiStreamChunk.Completed -> {
                            _streamingReply.value = null
                            val savedIdResult = repository.saveAiMessage(
                                currentUserId,
                                AiChatMessage(
                                    userId = currentUserId,
                                    sender = AiSender.ASSISTANT.name,
                                    text = chunk.finalText,
                                    language = lang.code,
                                    fallbackUsed = chunk.fallbackUsed
                                )
                            )
                            val savedMsgId = savedIdResult.getOrNull()
                            if (!savedMsgId.isNullOrBlank() && chunk.groundingSources.isNotEmpty()) {
                                localSqlStore.saveGroundingSources(
                                    userId = currentUserId,
                                    messageId = savedMsgId,
                                    roleName = role.displayName,
                                    sources = chunk.groundingSources
                                )
                            }
                            if (chunk.errorNotice != null) {
                                _actionStatusMessage.value = chunk.errorNotice
                            }
                        }
                    }
                }
            } finally {
                _streamingReply.value = null
                _isAiBusy.value = false
            }
        }
    }

    fun toggleRecommendation(recId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleRecommendation(currentUserId, recId, completed)
                .onFailure { _actionStatusMessage.value = it.localizedMessage ?: "Failed to update recommendation." }
        }
    }

    fun updateAlertStatus(alertId: String, status: AlertStatus) {
        viewModelScope.launch {
            repository.updateAlertStatus(currentUserId, alertId, status)
                .onSuccess {
                    repository.appendAuditLog(
                        currentUserId,
                        "ALERT_${status.name}",
                        "Alert $alertId marked ${status.name}.",
                        activeRole.value.name
                    )
                }
                .onFailure { _actionStatusMessage.value = it.localizedMessage ?: "Failed to update alert." }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, notifId)
        }
    }

    fun addCoordinatorFollowUpNote(caseId: String, note: String) {
        if (note.isBlank()) return
        _coordinatorCases.update { list ->
            list.map { c ->
                if (c.caseId == caseId && c.consentGranted) {
                    c.copy(outreachNote = note.trim(), status = "Follow-Up Recorded")
                } else c
            }
        }
        viewModelScope.launch {
            repository.appendAuditLog(
                currentUserId,
                "COORDINATOR_OUTREACH",
                "Recorded follow-up note for $caseId.",
                UserRole.COORDINATOR.name
            )
            _actionStatusMessage.value = "Coordinator outreach note recorded."
        }
    }

    fun generatePersonalReportText(periodDays: Int): String {
        val profile = (profileState.value as? UiState.Success)?.data
        val checkIns = (checkInsState.value as? UiState.Success)?.data.orEmpty()
        val alerts = (alertsState.value as? UiState.Success)?.data.orEmpty()
        val recs = (recommendationsState.value as? UiState.Success)?.data.orEmpty()
        return ReportFormatter.buildPersonalWellnessReport(profile, checkIns, alerts, recs, periodDays, isDemoMode)
    }

    fun generateHeartReportText(): String {
        val profile = (profileState.value as? UiState.Success)?.data
        val heartProfile = (heartProfileState.value as? UiState.Success)?.data
        val measurements = (heartMeasurementsState.value as? UiState.Success)?.data.orEmpty()
        val bps = (bloodPressureState.value as? UiState.Success)?.data.orEmpty()
        val symReports = (heartSymptomReportsState.value as? UiState.Success)?.data.orEmpty()
        val safety = (safetyEventsState.value as? UiState.Success)?.data.orEmpty()
        return ReportFormatter.buildHeartHealthReport(
            profile = profile,
            heartProfile = heartProfile,
            measurements = measurements,
            bpRecords = bps,
            symptomReports = symReports,
            safetyEvents = safety,
            isDemo = isDemoMode
        )
    }

    fun generateExportJson(): String {
        val profile = (profileState.value as? UiState.Success)?.data
        val checkIns = (checkInsState.value as? UiState.Success)?.data.orEmpty()
        val alerts = (alertsState.value as? UiState.Success)?.data.orEmpty()
        val measurements = (heartMeasurementsState.value as? UiState.Success)?.data.orEmpty()
        val bps = (bloodPressureState.value as? UiState.Success)?.data.orEmpty()
        val uploads = (healthReportUploadsState.value as? UiState.Success)?.data.orEmpty()
        val safety = (safetyEventsState.value as? UiState.Success)?.data.orEmpty()
        return ReportFormatter.buildExportJson(
            profile = profile,
            checkIns = checkIns,
            alerts = alerts,
            heartMeasurements = measurements,
            bpRecords = bps,
            reportUploads = uploads,
            safetyEvents = safety,
            isDemo = isDemoMode
        )
    }

    // --- AI Doctor & AI Heart Specialist Clinical Actions ---
    fun selectDoctorMode(mode: AiDoctorMode) {
        _selectedDoctorMode.value = mode
    }

    fun clearEmergencyBanner() {
        _activeEmergencyBanner.value = null
    }

    fun selectConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        val conv = (conversationsState.value as? UiState.Success)?.data.orEmpty().firstOrNull { it.id == conversationId }
        if (conv != null) {
            _selectedDoctorMode.value = AiDoctorMode.fromRaw(conv.mode)
        }
        _doctorAiError.value = null
    }

    fun startNewConversation(domain: DoctorDomain, mode: AiDoctorMode) {
        _selectedDoctorMode.value = mode
        _doctorAiError.value = null
        _lastClinicalResult.value = null
        viewModelScope.launch {
            val created = repository.createOrUpdateConversation(
                currentUserId,
                DoctorConversation(
                    userId = currentUserId,
                    domain = domain.name,
                    mode = mode.name,
                    title = "${mode.displayName} Session",
                    lastPreview = "New conversation started",
                    messageCount = 0L
                )
            )
            created.onSuccess { newId ->
                _activeConversationId.value = newId
            }.onFailure { err ->
                _actionStatusMessage.value = err.localizedMessage ?: "Unable to start conversation."
            }
        }
    }

    fun clearActiveConversation() {
        val mode = _selectedDoctorMode.value
        startNewConversation(mode.domain, mode)
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversationAndMessages(currentUserId, conversationId)
                .onSuccess {
                    if (_activeConversationId.value == conversationId) {
                        _activeConversationId.value = null
                    }
                    _actionStatusMessage.value = "Conversation removed."
                }
                .onFailure { _actionStatusMessage.value = it.localizedMessage ?: "Failed to delete conversation." }
        }
    }

    fun sendDoctorMessage(
        prompt: String,
        mode: AiDoctorMode = _selectedDoctorMode.value,
        taskType: AiTaskType = mode.defaultTaskType,
        useGoogleSearch: Boolean = _googleSearchEnabled.value,
        clearEmergencyFirst: Boolean = false
    ) {
        val clean = prompt.trim()
        if (clean.isBlank() || _isDoctorAiBusy.value) return
        val profile = (profileState.value as? UiState.Success)?.data
        if (profile?.consentAiGuidance == false) {
            _actionStatusMessage.value = "Enable AI Wellness Guidance in Privacy & Settings to use AI Doctor."
            return
        }
        if (clearEmergencyFirst) {
            _activeEmergencyBanner.value = null
        }

        lastSubmittedDoctorTurn = Triple(clean, mode, taskType)
        _doctorAiError.value = null

        viewModelScope.launch {
            _isDoctorAiBusy.value = true
            try {
                val latestConsent = (consentRecordsState.value as? UiState.Success)?.data?.firstOrNull()
                val allowHeartContext = latestConsent?.consentHeartAiContext ?: true
                val checkIns = (checkInsState.value as? UiState.Success)?.data.orEmpty()
                val heartProfile = if (allowHeartContext) (heartProfileState.value as? UiState.Success)?.data else null
                val heartMeasurements = if (allowHeartContext) (heartMeasurementsState.value as? UiState.Success)?.data.orEmpty() else emptyList()

                // Ensure active conversation exists
                val convId = _activeConversationId.value ?: run {
                    val title = clean.take(48).ifBlank { "${mode.displayName} Session" }
                    repository.createOrUpdateConversation(
                        currentUserId,
                        DoctorConversation(
                            userId = currentUserId,
                            domain = mode.domain.name,
                            mode = mode.name,
                            title = title,
                            lastPreview = clean.take(120),
                            messageCount = 0L
                        )
                    ).getOrElse { err ->
                        _doctorAiError.value = err.localizedMessage ?: "Could not create conversation."
                        return@launch
                    }.also { _activeConversationId.value = it }
                }

                // Save user message
                repository.saveDoctorMessage(
                    currentUserId,
                    DoctorAiMessage(
                        userId = currentUserId,
                        conversationId = convId,
                        sender = "USER",
                        mode = mode.name,
                        taskType = taskType.name,
                        text = clean,
                        providerUsed = "User",
                        language = language.value.code
                    )
                )

                val history = activeDoctorMessagesState.value
                _aiCallsCount.update { it + 1 }
                val aiResult = clinicalOrchestrator.runClinicalTask(
                    userPrompt = clean,
                    mode = mode,
                    taskType = taskType,
                    language = language.value,
                    conversationHistory = history,
                    profile = profile,
                    heartProfile = heartProfile,
                    recentCheckIns = checkIns,
                    recentHeartMeasurements = heartMeasurements,
                    useGoogleSearch = useGoogleSearch
                )
                _lastClinicalResult.value = aiResult
                val resp = aiResult.response

                val isEmergency = resp.emergencyScreen.triggered
                if (isEmergency) {
                    _activeEmergencyBanner.value = resp.emergencyScreen
                    repository.saveSafetyEvent(
                        currentUserId,
                        MedicalSafetyEvent(
                            userId = currentUserId,
                            sourceModule = if (mode.domain == DoctorDomain.HEART_SPECIALIST) "HEART_CHAT" else "DOCTOR_CHAT",
                            matchedSignals = resp.emergencyScreen.matchedSignals,
                            reason = resp.emergencyScreen.reason,
                            actionTaken = "BLOCKED_NORMAL_AI_SHOWED_EMERGENCY_112"
                        )
                    )
                }

                val savedMsgResult = repository.saveDoctorMessage(
                    currentUserId,
                    DoctorAiMessage(
                        userId = currentUserId,
                        conversationId = convId,
                        sender = if (isEmergency) "SYSTEM_EMERGENCY" else "ASSISTANT",
                        mode = mode.name,
                        taskType = taskType.name,
                        text = resp.answer,
                        structuredJson = aiResult.rawJson,
                        providerUsed = resp.providerUsed,
                        emergencyTriggered = isEmergency,
                        fallbackUsed = resp.fallbackUsed,
                        language = language.value.code
                    )
                )

                val savedMsgId = savedMsgResult.getOrNull()
                if (!savedMsgId.isNullOrBlank() && aiResult.groundingSources.isNotEmpty()) {
                    localSqlStore.saveGroundingSources(
                        userId = currentUserId,
                        messageId = savedMsgId,
                        roleName = mode.displayName,
                        sources = aiResult.groundingSources
                    )
                }

                val existingConv = (conversationsState.value as? UiState.Success)?.data.orEmpty().firstOrNull { it.id == convId }
                repository.createOrUpdateConversation(
                    currentUserId,
                    DoctorConversation(
                        id = convId,
                        userId = currentUserId,
                        domain = mode.domain.name,
                        mode = mode.name,
                        title = existingConv?.title?.takeIf { !it.endsWith("Session") } ?: clean.take(56),
                        lastPreview = resp.summary.take(180),
                        messageCount = (history.size + 2).toLong()
                    )
                )

                repository.saveAiInsight(
                    currentUserId,
                    AiHealthInsightRecord(
                        userId = currentUserId,
                        domain = mode.domain.name,
                        taskType = taskType.name,
                        title = resp.summary.take(120),
                        summary = resp.answer.take(1800),
                        observations = resp.observations,
                        recommendations = resp.recommendations,
                        recommendedDoctorType = resp.doctorType,
                        urgency = resp.medicalAttention.urgency,
                        providerUsed = resp.providerUsed,
                        fallbackUsed = resp.fallbackUsed
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _doctorAiError.value = e.localizedMessage ?: "Failed to get AI clinical guidance."
            } finally {
                _isDoctorAiBusy.value = false
            }
        }
    }

    fun retryLastDoctorMessage() {
        val last = lastSubmittedDoctorTurn ?: return
        sendDoctorMessage(prompt = last.first, mode = last.second, taskType = last.third)
    }

    fun rateDoctorMessage(messageId: String, feedback: Long) {
        viewModelScope.launch {
            repository.updateDoctorMessageFeedback(currentUserId, messageId, feedback)
                .onSuccess {
                    _actionStatusMessage.value = if (feedback > 0) "Marked response helpful." else "Feedback recorded."
                }
        }
    }

    fun saveHeartMeasurement(measurement: HeartMeasurement) {
        viewModelScope.launch {
            val emergency = SafetyScreeningEngine.screenVitalThresholds(measurement)
            if (emergency.triggered) {
                _activeEmergencyBanner.value = emergency
                repository.saveSafetyEvent(
                    currentUserId,
                    MedicalSafetyEvent(
                        userId = currentUserId,
                        sourceModule = "HEART_MEASUREMENT",
                        matchedSignals = emergency.matchedSignals,
                        reason = emergency.reason,
                        actionTaken = "FLAGGED_CRITICAL_VITALS_EMERGENCY_BANNER"
                    )
                )
            }
            val labeled = measurement.copy(
                userId = currentUserId,
                sourceLabel = if (isDemoMode) "DEMO_FICTIONAL" else "USER_ENTERED",
                emergencyFlagged = emergency.triggered
            )
            repository.saveHeartMeasurement(currentUserId, labeled)
                .onSuccess { measId ->
                    val sys = labeled.systolicMmHg
                    val dia = labeled.diastolicMmHg
                    if (sys != null && dia != null) {
                        repository.saveBloodPressureRecord(
                            currentUserId,
                            BloodPressureRecord(
                                userId = currentUserId,
                                measurementId = measId,
                                recordedAtIso = labeled.recordedAtIso,
                                systolicMmHg = sys,
                                diastolicMmHg = dia,
                                pulseBpm = labeled.restingHeartRateBpm,
                                postureOrContext = if (labeled.activityLevel == "POST_EXERCISE") "POST_ACTIVITY" else "SITTING_REST",
                                notes = labeled.notes,
                                sourceLabel = labeled.sourceLabel
                            )
                        )
                    }
                    repository.appendAuditLog(
                        currentUserId,
                        "HEART_MEASUREMENT_LOGGED",
                        "Logged vitals at ${labeled.recordedAtIso} (Emergency flag: ${emergency.triggered}).",
                        activeRole.value.name
                    )
                    _actionStatusMessage.value = if (emergency.triggered) {
                        "Measurement saved — Urgent warning indicators detected."
                    } else {
                        "Heart measurement saved."
                    }
                }
                .onFailure { err ->
                    _actionStatusMessage.value = err.localizedMessage ?: "Failed to save measurement."
                }
        }
    }

    fun submitHeartSymptomCheck(
        symptoms: List<String>,
        severityScore: Long,
        durationLabel: String,
        associatedSymptoms: List<String>,
        notes: String
    ) {
        if (_isDoctorAiBusy.value) return
        viewModelScope.launch {
            _isDoctorAiBusy.value = true
            try {
                val measurements = (heartMeasurementsState.value as? UiState.Success)?.data.orEmpty()
                val combinedText = listOfNotNull(
                    symptoms.joinToString(", "),
                    associatedSymptoms.joinToString(", "),
                    notes
                ).joinToString(". ")

                // Independent pre-AI safety check FIRST
                val screen = SafetyScreeningEngine.screenTextAndSymptoms(
                    text = combinedText,
                    selectedSymptoms = symptoms + associatedSymptoms,
                    latestMeasurement = measurements.firstOrNull()
                )

                if (screen.triggered) {
                    _activeEmergencyBanner.value = screen
                    val emergencyResp = clinicalOrchestrator.buildEmergencyStructuredResponse(
                        AiDoctorMode.HEART_SPECIALIST,
                        screen
                    )
                    _lastClinicalResult.value = ClinicalAiInvocationResult(
                        response = emergencyResp,
                        rawJson = StructuredAiResponseParser.toJson(emergencyResp)
                    )
                    repository.saveSafetyEvent(
                        currentUserId,
                        MedicalSafetyEvent(
                            userId = currentUserId,
                            sourceModule = "HEART_SYMPTOM_CHECKER",
                            matchedSignals = screen.matchedSignals,
                            reason = screen.reason,
                            actionTaken = "STOPPED_AI_FLOW_URGENT_112_ALERT"
                        )
                    )
                    repository.saveHeartSymptomReport(
                        currentUserId,
                        HeartSymptomReport(
                            userId = currentUserId,
                            symptoms = symptoms.ifEmpty { listOf("Urgent symptom check") },
                            severityScore = severityScore,
                            durationLabel = durationLabel,
                            associatedSymptoms = associatedSymptoms,
                            notes = notes.takeIf { it.isNotBlank() },
                            emergencyTriggered = true,
                            screeningReason = screen.reason,
                            aiSummary = emergencyResp.summary,
                            urgencyLevel = "emergency"
                        )
                    )
                    return@launch
                }

                val profile = (profileState.value as? UiState.Success)?.data
                val heartProfile = (heartProfileState.value as? UiState.Success)?.data
                val checkIns = (checkInsState.value as? UiState.Success)?.data.orEmpty()
                val prompt = "Symptom check: primary=${symptoms.joinToString(", ")}, severity=$severityScore/10, duration=$durationLabel, associated=${associatedSymptoms.joinToString(", ")}. Notes: $notes"
                _aiCallsCount.update { it + 1 }
                val res = clinicalOrchestrator.runClinicalTask(
                    userPrompt = prompt,
                    mode = AiDoctorMode.HEART_SPECIALIST,
                    taskType = AiTaskType.HEALTH_SYMPTOM_INFORMATION,
                    language = language.value,
                    conversationHistory = emptyList(),
                    profile = profile,
                    heartProfile = heartProfile,
                    recentCheckIns = checkIns,
                    recentHeartMeasurements = measurements,
                    selectedSymptoms = symptoms + associatedSymptoms
                )
                _lastClinicalResult.value = res
                repository.saveHeartSymptomReport(
                    currentUserId,
                    HeartSymptomReport(
                        userId = currentUserId,
                        symptoms = symptoms.ifEmpty { listOf("General review") },
                        severityScore = severityScore,
                        durationLabel = durationLabel,
                        associatedSymptoms = associatedSymptoms,
                        notes = notes.takeIf { it.isNotBlank() },
                        emergencyTriggered = false,
                        screeningReason = "No immediate emergency red flags triggered.",
                        aiSummary = "${res.response.summary} ${res.response.answer}".take(1900),
                        urgencyLevel = res.response.medicalAttention.urgency
                    )
                )
            } finally {
                _isDoctorAiBusy.value = false
            }
        }
    }

    fun analyzeUploadedReport(
        title: String,
        reportType: String,
        fileName: String?,
        rawReportText: String,
        bitmap: Bitmap? = null
    ) {
        if (_isDoctorAiBusy.value) return
        val latestConsent = (consentRecordsState.value as? UiState.Success)?.data?.firstOrNull()
        if (latestConsent?.consentReportAnalysis == false) {
            _actionStatusMessage.value = "Enable Health Report Analysis in Privacy & Settings first."
            return
        }
        viewModelScope.launch {
            _isDoctorAiBusy.value = true
            try {
                val safeText = rawReportText.trim().ifBlank { "Image report attached ($fileName)" }
                val screen = SafetyScreeningEngine.screenTextAndSymptoms(safeText)
                if (screen.triggered) {
                    _activeEmergencyBanner.value = screen
                    repository.saveSafetyEvent(
                        currentUserId,
                        MedicalSafetyEvent(
                            userId = currentUserId,
                            sourceModule = "REPORT_EXPLAINER",
                            matchedSignals = screen.matchedSignals,
                            reason = screen.reason,
                            actionTaken = "FLAGGED_CRITICAL_REPORT_FINDING"
                        )
                    )
                }

                val profile = (profileState.value as? UiState.Success)?.data
                val heartProfile = (heartProfileState.value as? UiState.Success)?.data
                val checkIns = (checkInsState.value as? UiState.Success)?.data.orEmpty()
                val measurements = (heartMeasurementsState.value as? UiState.Success)?.data.orEmpty()
                val isEcg = reportType == "ECG_REPORT"
                val taskType = if (isEcg) AiTaskType.ECG_REPORT_EXPLANATION else AiTaskType.HEALTH_REPORT_EXPLANATION
                val mode = if (isEcg) AiDoctorMode.HEART_SPECIALIST else AiDoctorMode.REPORT_EXPLAINER

                _aiCallsCount.update { it + 1 }
                val result = clinicalOrchestrator.runClinicalTask(
                    userPrompt = "Explain this $reportType report ($title). Preserve exact units and reference ranges. Content:\n$safeText",
                    mode = mode,
                    taskType = taskType,
                    language = language.value,
                    conversationHistory = emptyList(),
                    profile = profile,
                    heartProfile = heartProfile,
                    recentCheckIns = checkIns,
                    recentHeartMeasurements = measurements,
                    reportBitmap = bitmap
                )
                _lastClinicalResult.value = result
                val resp = result.response

                repository.saveHealthReportUpload(
                    currentUserId,
                    HealthReportUpload(
                        userId = currentUserId,
                        title = title.ifBlank { "Health Report" },
                        reportType = reportType,
                        fileName = fileName,
                        extractedOrEnteredText = safeText.take(4800),
                        summary = "${resp.summary}\n${resp.answer}".take(1950),
                        outOfRangeFindings = resp.observations,
                        questionsForDoctor = resp.recommendations + resp.followUpQuestions,
                        emergencyTriggered = screen.triggered || resp.emergencyScreen.triggered,
                        providerUsed = resp.providerUsed
                    )
                )
                repository.appendAuditLog(
                    currentUserId,
                    "HEALTH_REPORT_EXPLAINED",
                    "Analyzed $reportType report (${resp.providerUsed}).",
                    activeRole.value.name
                )
            } finally {
                _isDoctorAiBusy.value = false
            }
        }
    }

    fun generateSafeHeartWellnessSuggestions() {
        viewModelScope.launch {
            val measurements = (heartMeasurementsState.value as? UiState.Success)?.data.orEmpty()
            val heartProfile = (heartProfileState.value as? UiState.Success)?.data
            val latest = measurements.firstOrNull()
            val hasUrgent = _activeEmergencyBanner.value?.triggered == true ||
                (latest != null && SafetyScreeningEngine.screenVitalThresholds(latest).triggered)
            val recs = HeartRiskEducationEngine.buildSafeHeartSuggestions(
                userId = currentUserId,
                latestMeasurement = latest,
                profile = heartProfile,
                hasUrgentSymptoms = hasUrgent
            )
            repository.saveRecommendations(currentUserId, recs)
                .onSuccess {
                    _actionStatusMessage.value = if (hasUrgent) {
                        "Urgent symptoms present: paused activity plans and added follow-up reminder."
                    } else {
                        "Added personalized heart wellness suggestions to your plan."
                    }
                }
        }
    }

    fun generateAppointmentPrep(clinicianType: String, focusNotes: String) {
        if (_isDoctorAiBusy.value) return
        viewModelScope.launch {
            _isDoctorAiBusy.value = true
            try {
                val profile = (profileState.value as? UiState.Success)?.data
                val heartProfile = (heartProfileState.value as? UiState.Success)?.data
                val checkIns = (checkInsState.value as? UiState.Success)?.data.orEmpty()
                val measurements = (heartMeasurementsState.value as? UiState.Success)?.data.orEmpty()
                val prompt = "Prepare an organized medical appointment checklist for a $clinicianType visit. User focus: ${focusNotes.ifBlank { "General review of recent health & vital logs" }}"
                _aiCallsCount.update { it + 1 }
                val res = clinicalOrchestrator.runClinicalTask(
                    userPrompt = prompt,
                    mode = AiDoctorMode.HEALTH_NAVIGATOR,
                    taskType = AiTaskType.APPOINTMENT_PREPARATION,
                    language = language.value,
                    conversationHistory = emptyList(),
                    profile = profile,
                    heartProfile = heartProfile,
                    recentCheckIns = checkIns,
                    recentHeartMeasurements = measurements
                )
                _lastAppointmentPrepResult.value = res.response
            } finally {
                _isDoctorAiBusy.value = false
            }
        }
    }

    fun saveHeartProfile(profile: HeartHealthProfile) {
        viewModelScope.launch {
            repository.saveHeartProfile(currentUserId, profile.copy(userId = currentUserId))
                .onSuccess {
                    repository.appendAuditLog(
                        currentUserId,
                        "HEART_PROFILE_UPDATED",
                        "Updated medications/allergies/heart baseline profile.",
                        activeRole.value.name
                    )
                    _actionStatusMessage.value = "Heart & medical context profile saved."
                }
                .onFailure { _actionStatusMessage.value = it.localizedMessage ?: "Failed to save heart profile." }
        }
    }

    fun saveConsentSnapshot(
        consentAnalytics: Boolean,
        consentAiGuidance: Boolean,
        consentHeartAiContext: Boolean,
        consentReportAnalysis: Boolean,
        consentCoordinatorOutreach: Boolean
    ) {
        viewModelScope.launch {
            repository.saveConsentRecord(
                currentUserId,
                UserConsentRecord(
                    userId = currentUserId,
                    consentAnalytics = consentAnalytics,
                    consentAiGuidance = consentAiGuidance,
                    consentHeartAiContext = consentHeartAiContext,
                    consentReportAnalysis = consentReportAnalysis,
                    consentCoordinatorOutreach = consentCoordinatorOutreach,
                    policyNote = "Updated via Privacy & Consent Manager"
                )
            )
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            localSqlStore.clearUserCache(currentUserId)
            repository.deleteAccountData(currentUserId)
                .onSuccess {
                    _actionStatusMessage.value = "Account health records deleted."
                    onDeleted()
                }
                .onFailure { err ->
                    _actionStatusMessage.value = err.localizedMessage ?: "Failed to delete records."
                }
        }
    }

    private fun <T> handleStreamError(error: Throwable, label: String): UiState<T> {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing $label", root)
        return UiState.Error(root.message ?: "Failed to load $label")
    }

    private companion object {
        const val TAG = "HealthVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
