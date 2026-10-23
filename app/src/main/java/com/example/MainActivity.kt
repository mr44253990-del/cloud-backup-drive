package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.MistralAiEngine
import com.example.data.database.entities.NoteEntity
import com.example.ui.components.*
import com.example.ui.screens.dialogs.*
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.mode.ModeSelectionScreen
import com.example.ui.screens.money.MoneyScreen
import com.example.ui.screens.notes.NotesScreen
import com.example.ui.screens.permissions.PermissionScreen
import com.example.ui.screens.security.AppLockScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.screens.tools.ToolsScreen
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppStep
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial deep link if app opened via oauth redirect
        intent?.data?.let { uri ->
            viewModel.handleDeepLinkIntent(uri)
        }

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val appMode by viewModel.appMode.collectAsStateWithLifecycle()
                val userName by viewModel.userName.collectAsStateWithLifecycle()
                val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
                val userAvatarUrl by viewModel.userAvatarUrl.collectAsStateWithLifecycle()
                val googleAccessToken by viewModel.googleAccessToken.collectAsStateWithLifecycle()
                val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
                val usedStorageGb by viewModel.usedStorageGb.collectAsStateWithLifecycle()
                val totalStorageGb by viewModel.totalStorageGb.collectAsStateWithLifecycle()
                val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
                val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
                val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

                // Security state
                val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
                val appLockPin by viewModel.appLockPin.collectAsStateWithLifecycle()
                val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()

                val notes by viewModel.allNotes.collectAsStateWithLifecycle()
                val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
                val financeList by viewModel.allFinance.collectAsStateWithLifecycle()
                val clipboardList by viewModel.allClipboard.collectAsStateWithLifecycle()
                val files by viewModel.allFiles.collectAsStateWithLifecycle()

                var showSearchDialog by remember { mutableStateOf(false) }
                var showProfileDialog by remember { mutableStateOf(false) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var searchSelectedNote by remember { mutableStateOf<NoteEntity?>(null) }

                val isCloudMode = appMode == "CLOUD" && googleAccessToken.isNotEmpty()

                // If App Lock is enabled and locked, display Security Keypad Lock
                if (isAppLockEnabled && isAppLocked) {
                    AppLockScreen(
                        correctPin = appLockPin,
                        onUnlocked = { viewModel.unlockApp() }
                    )
                    return@MyApplicationTheme
                }

                // Back handling
                BackHandler(enabled = currentTab != NavigationTab.HOME && currentStep == AppStep.MAIN_WORKSPACE) {
                    viewModel.currentTab.value = NavigationTab.HOME
                }

                when (currentStep) {
                    AppStep.SPLASH -> {
                        SplashScreen(
                            onContinue = { viewModel.completeSplash() },
                            onGoogleSignIn = { viewModel.startGoogleOAuthFlow(context) }
                        )
                    }

                    AppStep.PERMISSION_CENTER -> {
                        PermissionScreen(
                            onContinue = { viewModel.completePermissionCenter() },
                            onStartGoogleDriveAuth = { viewModel.startGoogleOAuthFlow(context) }
                        )
                    }

                    AppStep.MODE_SELECTION -> {
                        ModeSelectionScreen(
                            onSelectMode = { mode -> viewModel.selectMode(mode, context) }
                        )
                    }

                    AppStep.MAIN_WORKSPACE -> {
                        Scaffold(
                            modifier = Modifier.fillMaxSize().background(DeepNavy),
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            topBar = {
                                WorkspaceTopBar(
                                    userName = userName,
                                    userAvatarUrl = userAvatarUrl,
                                    onSearchClick = { showSearchDialog = true },
                                    onProfileClick = { showProfileDialog = true },
                                    onSettingsClick = { showSettingsDialog = true },
                                    isCloudMode = isCloudMode
                                )
                            },
                            bottomBar = {
                                WorkspaceBottomNav(
                                    currentTab = currentTab,
                                    onTabSelected = { tab -> viewModel.currentTab.value = tab }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DeepNavy)
                                    .padding(innerPadding)
                            ) {
                                when (currentTab) {
                                    NavigationTab.HOME -> {
                                        HomeScreen(
                                            userName = userName,
                                            isCloudMode = isCloudMode,
                                            usedGb = usedStorageGb,
                                            totalGb = totalStorageGb,
                                            lastSyncTime = lastSyncTime,
                                            notes = notes,
                                            tasks = tasks,
                                            files = files,
                                            financeList = financeList,
                                            onBackupNowClick = { viewModel.startCloudSync(context) },
                                            onNoteClick = {
                                                viewModel.currentTab.value = NavigationTab.NOTES
                                            },
                                            onTaskToggle = { task -> viewModel.toggleTaskComplete(task) },
                                            onNavigateTab = { tab -> viewModel.currentTab.value = tab }
                                        )
                                    }

                                    NavigationTab.NOTES -> {
                                        NotesScreen(
                                            notes = notes,
                                            categories = customCategories,
                                            onAddCategory = { viewModel.addCustomCategory(it) },
                                            onSaveNote = { id, title, content, category, priority, tags, colorHex, isPinned ->
                                                viewModel.saveNote(id, title, content, category, priority, tags, colorHex, isPinned)
                                            },
                                            onDeleteNote = { note -> viewModel.deleteNote(note) },
                                            onTogglePin = { note -> viewModel.toggleNotePin(note) },
                                            onConvertToTask = { note -> viewModel.convertNoteToTask(note) },
                                            onAiGenerateNote = { prompt, onSuccess, onError ->
                                                viewModel.generateNoteWithMistralAi(prompt, onSuccess, onError)
                                            }
                                        )
                                    }

                                    NavigationTab.TASKS -> {
                                        TasksScreen(
                                            tasks = tasks,
                                            onSaveTask = { id, title, description, category, priority, dueDate, isPinned ->
                                                viewModel.saveTask(id, title, description, category, priority, dueDate, isPinned)
                                            },
                                            onToggleComplete = { task -> viewModel.toggleTaskComplete(task) },
                                            onTogglePin = { task -> viewModel.toggleTaskPin(task) },
                                            onDeleteTask = { task -> viewModel.deleteTask(task) },
                                            onConvertToNote = { task -> viewModel.convertTaskToNote(task) }
                                        )
                                    }

                                    NavigationTab.MONEY -> {
                                        MoneyScreen(
                                            financeList = financeList,
                                            monthlyBudgetLimit = monthlyBudget,
                                            onUpdateMonthlyBudget = { newBudget -> viewModel.updateMonthlyBudget(newBudget) },
                                            onAddTransaction = { title, amount, isIncome, category, notesText ->
                                                viewModel.addFinanceTransaction(title, amount, isIncome, category, notesText)
                                            },
                                            onDeleteTransaction = { f -> viewModel.deleteFinance(f) },
                                            onAiParseExpense = { prompt, onSuccess, onError ->
                                                viewModel.parseExpenseWithMistralAi(prompt, onSuccess, onError)
                                            },
                                            onAiGetAdvice = { onResult ->
                                                viewModel.getFinancialInsightsWithMistralAi(onResult)
                                            }
                                        )
                                    }

                                     NavigationTab.TOOLS -> {
                                        ToolsScreen(
                                            files = files,
                                            clipboardList = clipboardList,
                                            onAttachFileUri = { uri -> viewModel.attachFileFromUri(context, uri) },
                                            onDeleteFile = { fileRecord -> viewModel.deleteFileRecord(fileRecord) },
                                            onAddClipboard = { text, type -> viewModel.addClipboardItem(text, type) },
                                            onDeleteClipboard = { item -> viewModel.deleteClipboardItem(item) },
                                            onClearClipboard = { viewModel.clearClipboardHistory() },
                                            onSaveAsNote = { content ->
                                                viewModel.saveNote(
                                                    title = "Saved from Clipboard",
                                                    content = content,
                                                    category = "Personal",
                                                    priority = "Normal",
                                                    tags = "Clipboard",
                                                    colorHex = "#2979FF"
                                                )
                                                viewModel.currentTab.value = NavigationTab.NOTES
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Dialogs
                        if (showSearchDialog) {
                            GlobalSearchDialog(
                                notes = notes,
                                tasks = tasks,
                                files = files,
                                financeList = financeList,
                                onDismiss = { showSearchDialog = false },
                                onSelectNote = {
                                    searchSelectedNote = it
                                    viewModel.currentTab.value = NavigationTab.NOTES
                                },
                                onSelectTask = {
                                    viewModel.currentTab.value = NavigationTab.TASKS
                                },
                                onSelectFinance = {
                                    viewModel.currentTab.value = NavigationTab.MONEY
                                },
                                onSelectFile = {
                                    viewModel.currentTab.value = NavigationTab.TOOLS
                                }
                            )
                        }

                        if (showProfileDialog) {
                            ProfileDialog(
                                userName = userName,
                                userEmail = userEmail,
                                userAvatarUrl = userAvatarUrl,
                                isCloudMode = isCloudMode,
                                usedGb = usedStorageGb,
                                totalGb = totalStorageGb,
                                lastSyncTime = lastSyncTime,
                                onDismiss = { showProfileDialog = false },
                                onSyncNow = { viewModel.startCloudSync(context) },
                                onRestoreFromDrive = { viewModel.restoreFromGoogleDriveNow() },
                                onLogoutAccount = { viewModel.logoutGoogleAccount() },
                                onSwitchMode = {
                                    viewModel.currentStep.value = AppStep.MODE_SELECTION
                                    showProfileDialog = false
                                },
                                onManualCodeSubmit = { manualCode ->
                                    viewModel.handleOAuthAuthorizationCode(manualCode)
                                },
                                onUpdateAvatar = { avatarUrl ->
                                    viewModel.updateUserAvatar(avatarUrl)
                                }
                            )
                        }

                        if (showSettingsDialog) {
                            SettingsDialog(
                                isAppLockEnabled = isAppLockEnabled,
                                appLockPin = appLockPin,
                                onSetAppLock = { enabled, pin -> viewModel.setAppLock(enabled, pin) },
                                mistralApiKey = MistralAiEngine.currentApiKey,
                                mistralModel = MistralAiEngine.currentModel,
                                onUpdateMistralConfig = { key, model ->
                                    viewModel.updateMistralAiConfig(key, model)
                                },
                                onDismiss = { showSettingsDialog = false },
                                onSyncNow = { viewModel.startCloudSync(context) },
                                onRestoreFromDrive = { viewModel.restoreFromGoogleDriveNow() },
                                onLogoutAccount = { viewModel.logoutGoogleAccount() },
                                isCloudMode = isCloudMode
                            )
                        }

                        if (syncProgress.isSyncing || syncProgress.isComplete || syncProgress.errorMessage != null) {
                            CloudSyncDialog(
                                progress = syncProgress,
                                onDismiss = { viewModel.dismissSyncDialog() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            viewModel.handleDeepLinkIntent(uri)
        }
    }
}
