package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.DemoHealthRepository
import com.example.data.FirebaseAiHealthService
import com.example.data.FirestoreHealthRepository
import com.example.data.PreferencesStore
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.local.LocalHealthDatabase
import com.example.data.local.RoomLocalHealthCacheRepository
import com.example.data.signOutUser
import com.example.engine.DemoDataFactory
import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import com.example.model.AiDoctorMode
import com.example.model.DoctorDomain
import com.example.model.UserRole
import com.example.ui.components.DemoModePill
import com.example.ui.components.LanguageSelectorChip
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AlertsAndNotificationsScreen
import com.example.ui.screens.AppointmentPrepScreen
import com.example.ui.screens.AuthAndLandingScreen
import com.example.ui.screens.CheckInScreen
import com.example.ui.screens.ConversationHistoryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DoctorChatScreen
import com.example.ui.screens.DoctorHomeScreen
import com.example.ui.screens.HeartChatScreen
import com.example.ui.screens.HeartDashboardScreen
import com.example.ui.screens.HeartHomeScreen
import com.example.ui.screens.HeartMeasurementScreen
import com.example.ui.screens.HeartSymptomCheckerScreen
import com.example.ui.screens.MentalWellnessScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PreventionPlanScreen
import com.example.ui.screens.ReportExplainerScreen
import com.example.ui.screens.SettingsPrivacyScreen
import com.example.ui.screens.TrendsAndReportsScreen
import com.example.ui.screens.WorkforceAndAdminScreen
import com.example.ui.theme.YaqeenHealthTheme
import com.example.viewmodel.HealthViewModel
import com.example.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class PrimaryTab(val route: String, val icon: ImageVector) {
    DASHBOARD("dashboard", Icons.Filled.Dashboard),
    CHECK_IN("checkin", Icons.Filled.AddTask),
    AI_DOCTOR("doctor", Icons.Filled.MedicalServices),
    HEART("heart", Icons.Filled.Favorite),
    ORG_HUB("workforce", Icons.Filled.Groups)
}

sealed interface AppScreen {
    data class Tab(val tab: PrimaryTab) : AppScreen
    data object DoctorChat : AppScreen
    data object DoctorHistory : AppScreen
    data object HeartChat : AppScreen
    data object HeartDashboard : AppScreen
    data object HeartMeasurements : AppScreen
    data object HeartSymptomChecker : AppScreen
    data object ReportExplainer : AppScreen
    data object AppointmentPrep : AppScreen
    data object MentalWellness : AppScreen
    data object LegacyAssistant : AppScreen
    data object TrendsReports : AppScreen
    data object PreventionPlan : AppScreen
    data object AlertsNotifications : AppScreen
    data object SettingsPrivacy : AppScreen
}

@Composable
fun YaqeenRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { PreferencesStore(context.applicationContext) }
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val language by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)
    val darkOverride by prefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = null)
    val strings = remember(language) { AppStrings.get(language) }

    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var demoRole by remember { mutableStateOf<UserRole?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    YaqeenHealthTheme(
        darkTheme = darkOverride ?: isSystemInDarkTheme(),
        isRtl = language.isRtl
    ) {
        val activeUser = currentUser
        when {
            activeUser == null && demoRole == null -> {
                AuthAndLandingScreen(
                    language = language,
                    strings = strings,
                    onLanguageChange = { lang -> scope.launch { prefs.setLanguage(lang) } },
                    onAuthSuccess = {},
                    onEnterDemoMode = { selectedRole -> demoRole = selectedRole }
                )
            }
            activeUser != null -> {
                val uid = activeUser.uid
                key(uid) {
                    val vm: HealthViewModel = viewModel(
                        key = uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY])
                                val dbId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(dbId)
                                val localDb = LocalHealthDatabase.getInstance(app)
                                HealthViewModel(
                                    repository = FirestoreHealthRepository(db, auth),
                                    aiService = FirebaseAiHealthService(),
                                    preferencesStore = prefs,
                                    currentUserId = uid,
                                    initialDisplayName = activeUser.displayName ?: "Team Member",
                                    isDemoMode = false,
                                    localSqlStore = RoomLocalHealthCacheRepository(localDb.localHealthDao())
                                )
                            }
                        }
                    )
                    AuthenticatedShell(
                        viewModel = vm,
                        onSignOutOrExit = {
                            signOutUser(credentialManager, scope)
                        }
                    )
                }
            }
            else -> {
                val initialDemoRole = demoRole ?: UserRole.INDIVIDUAL
                key("demo_${initialDemoRole.name}") {
                    val vm: HealthViewModel = viewModel(
                        key = "demo_${initialDemoRole.name}",
                        factory = viewModelFactory {
                            initializer {
                                HealthViewModel(
                                    repository = DemoHealthRepository(initialDemoRole),
                                    aiService = FirebaseAiHealthService(),
                                    preferencesStore = prefs,
                                    currentUserId = DemoDataFactory.DEMO_UID,
                                    initialDisplayName = "Zara Siddiqui (Demo)",
                                    isDemoMode = true
                                )
                            }
                        }
                    )
                    AuthenticatedShell(
                        viewModel = vm,
                        onSignOutOrExit = { demoRole = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedShell(
    viewModel: HealthViewModel,
    onSignOutOrExit: () -> Unit
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val darkOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
    val strings = remember(language) { AppStrings.get(language) }

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val checkInsState by viewModel.checkInsState.collectAsStateWithLifecycle()
    val alertsState by viewModel.alertsState.collectAsStateWithLifecycle()
    val recsState by viewModel.recommendationsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.aiMessagesState.collectAsStateWithLifecycle()
    val notifsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val auditLogsState by viewModel.auditLogsState.collectAsStateWithLifecycle()

    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmittingCheckIn.collectAsStateWithLifecycle()
    val submissionOutcome by viewModel.lastSubmissionOutcome.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val isAiBusy by viewModel.isAiBusy.collectAsStateWithLifecycle()
    val statusMessage by viewModel.actionStatusMessage.collectAsStateWithLifecycle()
    val workforceAnalytics by viewModel.workforceAnalytics.collectAsStateWithLifecycle()
    val coordinatorCases by viewModel.coordinatorCases.collectAsStateWithLifecycle()
    val aiCallsCount by viewModel.aiCallsCount.collectAsStateWithLifecycle()
    val selectedAiRole by viewModel.selectedAiRole.collectAsStateWithLifecycle()
    val googleSearchEnabled by viewModel.googleSearchEnabled.collectAsStateWithLifecycle()
    val localSqlSummary by viewModel.localSqlSummary.collectAsStateWithLifecycle()
    val groundingSourcesByMessage by viewModel.groundingSourcesByMessage.collectAsStateWithLifecycle()

    // Clinical AI Doctor & Heart Specialist States
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeDoctorMessages by viewModel.activeDoctorMessagesState.collectAsStateWithLifecycle()
    val heartProfileState by viewModel.heartProfileState.collectAsStateWithLifecycle()
    val heartMeasurementsState by viewModel.heartMeasurementsState.collectAsStateWithLifecycle()
    val bloodPressureState by viewModel.bloodPressureState.collectAsStateWithLifecycle()
    val heartSymptomReportsState by viewModel.heartSymptomReportsState.collectAsStateWithLifecycle()
    val healthReportUploadsState by viewModel.healthReportUploadsState.collectAsStateWithLifecycle()
    val aiInsightsState by viewModel.aiInsightsState.collectAsStateWithLifecycle()
    val safetyEventsState by viewModel.safetyEventsState.collectAsStateWithLifecycle()
    val consentRecordsState by viewModel.consentRecordsState.collectAsStateWithLifecycle()
    val selectedDoctorMode by viewModel.selectedDoctorMode.collectAsStateWithLifecycle()
    val isDoctorAiBusy by viewModel.isDoctorAiBusy.collectAsStateWithLifecycle()
    val doctorAiError by viewModel.doctorAiError.collectAsStateWithLifecycle()
    val lastClinicalResult by viewModel.lastClinicalResult.collectAsStateWithLifecycle()
    val lastAppointmentPrepResult by viewModel.lastAppointmentPrepResult.collectAsStateWithLifecycle()
    val activeEmergencyBanner by viewModel.activeEmergencyBanner.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val checkIns = (checkInsState as? UiState.Success)?.data.orEmpty()
    val alerts = (alertsState as? UiState.Success)?.data.orEmpty()
    val recs = (recsState as? UiState.Success)?.data.orEmpty()
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val notifications = (notifsState as? UiState.Success)?.data.orEmpty()
    val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()

    val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
    val heartProfile = (heartProfileState as? UiState.Success)?.data
    val heartMeasurements = (heartMeasurementsState as? UiState.Success)?.data.orEmpty()
    val bpRecords = (bloodPressureState as? UiState.Success)?.data.orEmpty()
    val heartSymptomReports = (heartSymptomReportsState as? UiState.Success)?.data.orEmpty()
    val healthReportUploads = (healthReportUploadsState as? UiState.Success)?.data.orEmpty()
    val aiInsights = (aiInsightsState as? UiState.Success)?.data.orEmpty()
    val safetyEvents = (safetyEventsState as? UiState.Success)?.data.orEmpty()
    val consentRecords = (consentRecordsState as? UiState.Success)?.data.orEmpty()

    if (!viewModel.isDemoMode && profile != null && !profile.onboardingCompleted) {
        OnboardingScreen(
            initialName = profile.displayName,
            onComplete = { name, dept, analytics, ai ->
                viewModel.completeOnboarding(name, dept, analytics, ai)
            }
        )
        return
    }

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Tab(PrimaryTab.DASHBOARD)) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun navigateBackTarget(screen: AppScreen): AppScreen = when (screen) {
        AppScreen.DoctorChat, AppScreen.DoctorHistory -> AppScreen.Tab(PrimaryTab.AI_DOCTOR)
        AppScreen.HeartChat, AppScreen.HeartDashboard, AppScreen.HeartMeasurements, AppScreen.HeartSymptomChecker -> AppScreen.Tab(PrimaryTab.HEART)
        else -> AppScreen.Tab(PrimaryTab.DASHBOARD)
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Also surface stream errors cleanly without crashing
    LaunchedEffect(checkInsState, alertsState) {
        val err = (checkInsState as? UiState.Error)?.message ?: (alertsState as? UiState.Error)?.message
        if (err != null) {
            snackbarHostState.showSnackbar(err)
        }
    }

    val isSecondary = currentScreen !is AppScreen.Tab
    if (isSecondary) {
        BackHandler {
            currentScreen = navigateBackTarget(currentScreen)
        }
    }

    fun tabLabel(tab: PrimaryTab): String = when (tab) {
        PrimaryTab.DASHBOARD -> strings.navDashboard
        PrimaryTab.CHECK_IN -> strings.navCheckIn
        PrimaryTab.AI_DOCTOR -> "Doctor"
        PrimaryTab.HEART -> "Heart"
        PrimaryTab.ORG_HUB -> strings.navWorkforce
    }

    val currentDark = darkOverride ?: isSystemInDarkTheme()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        if (isSecondary) {
                            IconButton(onClick = { currentScreen = navigateBackTarget(currentScreen) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.appTitle, style = MaterialTheme.typography.titleLarge)
                            if (viewModel.isDemoMode) {
                                Spacer(modifier = Modifier.width(8.dp))
                                DemoModePill(label = "DEMO")
                            }
                        }
                    },
                    actions = {
                        LanguageSelectorChip(
                            current = language,
                            onSelect = { viewModel.setLanguage(it) }
                        )
                        IconButton(
                            onClick = { currentScreen = AppScreen.AlertsNotifications },
                            modifier = Modifier.testTag("topbar_alerts_button")
                        ) {
                            val badgeTotal = metrics.activeAlertsCount + metrics.unreadNotifsCount
                            BadgedBox(
                                badge = {
                                    if (badgeTotal > 0) Badge { Text("$badgeTotal") }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = strings.alertsAndNotifs)
                            }
                        }
                        IconButton(
                            onClick = { currentScreen = AppScreen.SettingsPrivacy },
                            modifier = Modifier.testTag("topbar_settings_button")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = strings.privacyAndSettings)
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        PrimaryTab.entries.forEach { tab ->
                            val selected = (currentScreen as? AppScreen.Tab)?.tab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = AppScreen.Tab(tab) },
                                icon = { Icon(tab.icon, contentDescription = tabLabel(tab)) },
                                label = { Text(tabLabel(tab), maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        PrimaryTab.entries.forEach { tab ->
                            val selected = (currentScreen as? AppScreen.Tab)?.tab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentScreen = AppScreen.Tab(tab) },
                                icon = { Icon(tab.icon, contentDescription = tabLabel(tab)) },
                                label = { Text(tabLabel(tab)) }
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                        when (val scr = currentScreen) {
                            is AppScreen.Tab -> when (scr.tab) {
                                PrimaryTab.DASHBOARD -> DashboardScreen(
                                    strings = strings,
                                    profile = profile,
                                    metrics = metrics,
                                    checkIns = checkIns,
                                    alerts = alerts,
                                    recommendations = recs,
                                    onStartCheckIn = { currentScreen = AppScreen.Tab(PrimaryTab.CHECK_IN) },
                                    onOpenTrendsReports = { currentScreen = AppScreen.TrendsReports },
                                    onOpenPreventionPlan = { currentScreen = AppScreen.PreventionPlan },
                                    onOpenAlerts = { currentScreen = AppScreen.AlertsNotifications },
                                    onOpenAssistant = { currentScreen = AppScreen.LegacyAssistant },
                                    onOpenDoctorHome = { currentScreen = AppScreen.Tab(PrimaryTab.AI_DOCTOR) },
                                    onOpenHeartHome = { currentScreen = AppScreen.Tab(PrimaryTab.HEART) },
                                    onOpenReportExplainer = { currentScreen = AppScreen.ReportExplainer },
                                    onOpenMentalWellness = { currentScreen = AppScreen.MentalWellness },
                                    onToggleRecommendation = { id, done -> viewModel.toggleRecommendation(id, done) }
                                )
                                PrimaryTab.CHECK_IN -> CheckInScreen(
                                    strings = strings,
                                    isSubmitting = isSubmitting,
                                    outcome = submissionOutcome,
                                    onSubmitCheckIn = { viewModel.submitCheckIn(it) },
                                    onDismissOutcome = { viewModel.clearSubmissionOutcome() },
                                    onNavigateToDashboard = { currentScreen = AppScreen.Tab(PrimaryTab.DASHBOARD) },
                                    onNavigateToAssistant = {
                                        viewModel.selectDoctorMode(AiDoctorMode.GENERAL_HEALTH)
                                        currentScreen = AppScreen.DoctorChat
                                    }
                                )
                                PrimaryTab.AI_DOCTOR -> DoctorHomeScreen(
                                    providerStatuses = viewModel.providerStatuses,
                                    conversationsCount = conversations.size,
                                    onOpenModeChat = { mode ->
                                        viewModel.selectDoctorMode(mode)
                                        val matchingConv = conversations.firstOrNull { it.mode == mode.name }
                                        if (matchingConv != null) {
                                            viewModel.selectConversation(matchingConv.id)
                                        } else {
                                            viewModel.startNewConversation(mode.domain, mode)
                                        }
                                        currentScreen = if (mode == AiDoctorMode.HEART_SPECIALIST) {
                                            AppScreen.HeartChat
                                        } else {
                                            AppScreen.DoctorChat
                                        }
                                    },
                                    onOpenReportExplainer = { currentScreen = AppScreen.ReportExplainer },
                                    onOpenHeartHome = { currentScreen = AppScreen.Tab(PrimaryTab.HEART) },
                                    onOpenAppointmentPrep = { currentScreen = AppScreen.AppointmentPrep },
                                    onOpenHistory = { currentScreen = AppScreen.DoctorHistory }
                                )
                                PrimaryTab.HEART -> HeartHomeScreen(
                                    measurements = heartMeasurements,
                                    bpRecords = bpRecords,
                                    emergencyBanner = activeEmergencyBanner,
                                    onOpenHeartChat = {
                                        val heartConv = conversations.firstOrNull { it.domain == DoctorDomain.HEART_SPECIALIST.name }
                                        if (heartConv != null) {
                                            viewModel.selectConversation(heartConv.id)
                                        } else {
                                            viewModel.startNewConversation(DoctorDomain.HEART_SPECIALIST, AiDoctorMode.HEART_SPECIALIST)
                                        }
                                        currentScreen = AppScreen.HeartChat
                                    },
                                    onOpenHeartDashboard = { currentScreen = AppScreen.HeartDashboard },
                                    onOpenMeasurements = { currentScreen = AppScreen.HeartMeasurements },
                                    onOpenSymptomChecker = { currentScreen = AppScreen.HeartSymptomChecker },
                                    onOpenReportExplainer = { currentScreen = AppScreen.ReportExplainer },
                                    onOpenAppointmentPrep = { currentScreen = AppScreen.AppointmentPrep },
                                    onDismissEmergency = { viewModel.clearEmergencyBanner() }
                                )
                                PrimaryTab.ORG_HUB -> WorkforceAndAdminScreen(
                                    strings = strings,
                                    activeRole = activeRole,
                                    isDemoMode = viewModel.isDemoMode,
                                    analytics = workforceAnalytics,
                                    coordinatorCases = coordinatorCases,
                                    auditLogs = auditLogs,
                                    aiCallsCount = aiCallsCount,
                                    onSwitchDemoRole = { viewModel.switchDemoRole(it) },
                                    onRecordFollowUpNote = { cid, note -> viewModel.addCoordinatorFollowUpNote(cid, note) }
                                )
                            }
                            AppScreen.DoctorChat -> DoctorChatScreen(
                                selectedMode = selectedDoctorMode,
                                messages = activeDoctorMessages,
                                isBusy = isDoctorAiBusy,
                                errorMessage = doctorAiError,
                                emergencyBanner = activeEmergencyBanner,
                                googleSearchEnabled = googleSearchEnabled,
                                groundingSourcesByMessage = groundingSourcesByMessage,
                                onSelectMode = { viewModel.selectDoctorMode(it) },
                                onToggleGoogleSearch = { viewModel.toggleGoogleSearch(it) },
                                onSendMessage = { prompt -> viewModel.sendDoctorMessage(prompt, selectedDoctorMode) },
                                onNewChat = { viewModel.startNewConversation(DoctorDomain.DOCTOR, selectedDoctorMode) },
                                onRetry = { viewModel.retryLastDoctorMessage() },
                                onFeedback = { id, score -> viewModel.rateDoctorMessage(id, score) },
                                onDismissEmergency = { viewModel.clearEmergencyBanner() },
                                onOpenAppointmentPrep = { currentScreen = AppScreen.AppointmentPrep }
                            )
                            AppScreen.DoctorHistory -> ConversationHistoryScreen(
                                conversations = conversations,
                                onResumeConversation = { conv ->
                                    viewModel.selectConversation(conv.id)
                                    currentScreen = if (conv.domain == DoctorDomain.HEART_SPECIALIST.name) {
                                        AppScreen.HeartChat
                                    } else {
                                        AppScreen.DoctorChat
                                    }
                                },
                                onDeleteConversation = { viewModel.deleteConversation(it) },
                                onStartNewDoctorChat = {
                                    viewModel.startNewConversation(DoctorDomain.DOCTOR, AiDoctorMode.GENERAL_HEALTH)
                                    currentScreen = AppScreen.DoctorChat
                                },
                                onStartNewHeartChat = {
                                    viewModel.startNewConversation(DoctorDomain.HEART_SPECIALIST, AiDoctorMode.HEART_SPECIALIST)
                                    currentScreen = AppScreen.HeartChat
                                }
                            )
                            AppScreen.HeartChat -> HeartChatScreen(
                                messages = activeDoctorMessages,
                                isBusy = isDoctorAiBusy,
                                errorMessage = doctorAiError,
                                emergencyBanner = activeEmergencyBanner,
                                groundingSourcesByMessage = groundingSourcesByMessage,
                                onSendMessage = { prompt ->
                                    viewModel.sendDoctorMessage(prompt, AiDoctorMode.HEART_SPECIALIST)
                                },
                                onNewChat = {
                                    viewModel.startNewConversation(DoctorDomain.HEART_SPECIALIST, AiDoctorMode.HEART_SPECIALIST)
                                },
                                onRetry = { viewModel.retryLastDoctorMessage() },
                                onFeedback = { id, score -> viewModel.rateDoctorMessage(id, score) },
                                onDismissEmergency = { viewModel.clearEmergencyBanner() },
                                onOpenSymptomChecker = { currentScreen = AppScreen.HeartSymptomChecker }
                            )
                            AppScreen.HeartDashboard -> HeartDashboardScreen(
                                measurements = heartMeasurements,
                                bpRecords = bpRecords,
                                symptomReports = heartSymptomReports,
                                aiInsights = aiInsights,
                                onGenerateHeartSuggestions = { viewModel.generateSafeHeartWellnessSuggestions() },
                                onExportHeartReportText = { viewModel.generateHeartReportText() },
                                onOpenAppointmentPrep = { currentScreen = AppScreen.AppointmentPrep },
                                onOpenMeasurements = { currentScreen = AppScreen.HeartMeasurements }
                            )
                            AppScreen.HeartMeasurements -> HeartMeasurementScreen(
                                measurements = heartMeasurements,
                                emergencyBanner = activeEmergencyBanner,
                                onSaveMeasurement = { viewModel.saveHeartMeasurement(it) },
                                onExportHeartReportText = { viewModel.generateHeartReportText() },
                                onDismissEmergency = { viewModel.clearEmergencyBanner() }
                            )
                            AppScreen.HeartSymptomChecker -> HeartSymptomCheckerScreen(
                                isBusy = isDoctorAiBusy,
                                emergencyBanner = activeEmergencyBanner,
                                lastResult = lastClinicalResult,
                                symptomReports = heartSymptomReports,
                                onSubmitCheck = { syms, sev, dur, assoc, notes ->
                                    viewModel.submitHeartSymptomCheck(syms, sev, dur, assoc, notes)
                                },
                                onDismissEmergency = { viewModel.clearEmergencyBanner() }
                            )
                            AppScreen.ReportExplainer -> ReportExplainerScreen(
                                isBusy = isDoctorAiBusy,
                                lastResult = lastClinicalResult,
                                uploads = healthReportUploads,
                                onAnalyzeReport = { title, type, fileName, rawText, bitmap ->
                                    viewModel.analyzeUploadedReport(title, type, fileName, rawText, bitmap)
                                }
                            )
                            AppScreen.AppointmentPrep -> AppointmentPrepScreen(
                                isBusy = isDoctorAiBusy,
                                heartProfile = heartProfile,
                                measurements = heartMeasurements,
                                prepResult = lastAppointmentPrepResult,
                                onSaveMedicationsAndAllergies = { viewModel.saveHeartProfile(it) },
                                onGenerateChecklist = { doc, notes -> viewModel.generateAppointmentPrep(doc, notes) },
                                onExportHeartSummary = { viewModel.generateHeartReportText() }
                            )
                            AppScreen.MentalWellness -> MentalWellnessScreen(
                                strings = strings,
                                metrics = metrics,
                                onOpenPreventionPlan = { currentScreen = AppScreen.PreventionPlan }
                            )
                            AppScreen.LegacyAssistant -> AiAssistantScreen(
                                strings = strings,
                                messages = messages,
                                streamingReply = streamingReply,
                                isBusy = isAiBusy,
                                selectedRole = selectedAiRole,
                                googleSearchEnabled = googleSearchEnabled,
                                groundingSourcesByMessage = groundingSourcesByMessage,
                                onSelectRole = { viewModel.selectAiRole(it) },
                                onToggleGoogleSearch = { viewModel.toggleGoogleSearch(it) },
                                onSendMessage = { viewModel.sendAiMessage(it) }
                            )
                            AppScreen.TrendsReports -> TrendsAndReportsScreen(
                                strings = strings,
                                checkIns = checkIns,
                                onBuildReportText = { days -> viewModel.generatePersonalReportText(days) }
                            )
                            AppScreen.PreventionPlan -> PreventionPlanScreen(
                                strings = strings,
                                recommendations = recs,
                                onToggleRecommendation = { id, done -> viewModel.toggleRecommendation(id, done) }
                            )
                            AppScreen.AlertsNotifications -> AlertsAndNotificationsScreen(
                                strings = strings,
                                profile = profile,
                                alerts = alerts,
                                notifications = notifications,
                                onUpdateAlertStatus = { id, st -> viewModel.updateAlertStatus(id, st) },
                                onMarkNotificationRead = { id -> viewModel.markNotificationRead(id) },
                                onSaveReminderSettings = { enabled, hr, min ->
                                    viewModel.updatePrivacyAndReminders(
                                        displayName = profile?.displayName ?: "Team Member",
                                        department = profile?.department ?: "Operations",
                                        consentAnalytics = profile?.consentAnalytics ?: true,
                                        consentAiGuidance = profile?.consentAiGuidance ?: true,
                                        consentCoordinatorOutreach = profile?.consentCoordinatorOutreach ?: false,
                                        remindersEnabled = enabled,
                                        reminderHour = hr,
                                        reminderMinute = min
                                    )
                                }
                            )
                            AppScreen.SettingsPrivacy -> SettingsPrivacyScreen(
                                strings = strings,
                                language = language,
                                darkMode = currentDark,
                                profile = profile,
                                heartProfile = heartProfile,
                                latestConsent = consentRecords.firstOrNull(),
                                safetyEvents = safetyEvents,
                                providerStatuses = viewModel.providerStatuses,
                                localSqlSummary = localSqlSummary,
                                isDemoMode = viewModel.isDemoMode,
                                onSelectLanguage = { viewModel.setLanguage(it) },
                                onToggleDarkMode = { viewModel.setDarkMode(it) },
                                onSavePrivacy = { name, dept, cAnalytics, cAi, cOutreach ->
                                    viewModel.updatePrivacyAndReminders(
                                        displayName = name,
                                        department = dept,
                                        consentAnalytics = cAnalytics,
                                        consentAiGuidance = cAi,
                                        consentCoordinatorOutreach = cOutreach,
                                        remindersEnabled = profile?.remindersEnabled ?: true,
                                        reminderHour = profile?.reminderHour ?: 9L,
                                        reminderMinute = profile?.reminderMinute ?: 0L
                                    )
                                },
                                onSaveClinicalConsent = { cAnalytics, cAi, cHeartAi, cReports, cOutreach ->
                                    viewModel.saveConsentSnapshot(cAnalytics, cAi, cHeartAi, cReports, cOutreach)
                                },
                                onSaveHeartProfile = { hp -> viewModel.saveHeartProfile(hp) },
                                onExportJson = { viewModel.generateExportJson() },
                                onDeleteAccount = {
                                    viewModel.deleteAccount(onDeleted = onSignOutOrExit)
                                },
                                onSignOutOrExitDemo = onSignOutOrExit
                            )
                        }
                    }
                }
            }
        }
    }
}
