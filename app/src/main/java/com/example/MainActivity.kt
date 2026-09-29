package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.entities.NoteEntity
import com.example.ui.components.*
import com.example.ui.screens.dialogs.*
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.mode.ModeSelectionScreen
import com.example.ui.screens.money.MoneyScreen
import com.example.ui.screens.notes.NotesScreen
import com.example.ui.screens.permissions.PermissionScreen
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

        setContent {
            MyApplicationTheme {
                val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val appMode by viewModel.appMode.collectAsStateWithLifecycle()
                val userName by viewModel.userName.collectAsStateWithLifecycle()
                val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
                val monthlyBudget by viewModel.monthlyBudget.collectAsStateWithLifecycle()
                val usedStorageGb by viewModel.usedStorageGb.collectAsStateWithLifecycle()
                val totalStorageGb by viewModel.totalStorageGb.collectAsStateWithLifecycle()
                val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
                val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()

                val notes by viewModel.allNotes.collectAsStateWithLifecycle()
                val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
                val financeList by viewModel.allFinance.collectAsStateWithLifecycle()
                val clipboardList by viewModel.allClipboard.collectAsStateWithLifecycle()
                val files by viewModel.allFiles.collectAsStateWithLifecycle()

                var showSearchDialog by remember { mutableStateOf(false) }
                var showProfileDialog by remember { mutableStateOf(false) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                var searchSelectedNote by remember { mutableStateOf<NoteEntity?>(null) }

                val isCloudMode = appMode == "CLOUD"

                // Back handling
                BackHandler(enabled = currentTab != NavigationTab.HOME && currentStep == AppStep.MAIN_WORKSPACE) {
                    viewModel.currentTab.value = NavigationTab.HOME
                }

                when (currentStep) {
                    AppStep.SPLASH -> {
                        SplashScreen(
                            onContinue = { viewModel.completeSplash() }
                        )
                    }

                    AppStep.PERMISSION_CENTER -> {
                        PermissionScreen(
                            onContinue = { viewModel.completePermissionCenter() }
                        )
                    }

                    AppStep.MODE_SELECTION -> {
                        ModeSelectionScreen(
                            onSelectMode = { mode -> viewModel.selectMode(mode) }
                        )
                    }

                    AppStep.MAIN_WORKSPACE -> {
                        Scaffold(
                            modifier = Modifier.fillMaxSize().background(DeepNavy),
                            topBar = {
                                WorkspaceTopBar(
                                    userName = userName,
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
                                            onBackupNowClick = { viewModel.startCloudSync() },
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
                                            onSaveNote = { id, title, content, category, priority, tags, colorHex, isPinned ->
                                                viewModel.saveNote(id, title, content, category, priority, tags, colorHex, isPinned)
                                            },
                                            onDeleteNote = { note -> viewModel.deleteNote(note) },
                                            onTogglePin = { note -> viewModel.toggleNotePin(note) },
                                            onConvertToTask = { note -> viewModel.convertNoteToTask(note) }
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
                                            onAddTransaction = { title, amount, isIncome, category, notesText ->
                                                viewModel.addFinanceTransaction(title, amount, isIncome, category, notesText)
                                            },
                                            onDeleteTransaction = { f -> viewModel.deleteFinance(f) }
                                        )
                                    }

                                    NavigationTab.TOOLS -> {
                                        ToolsScreen(
                                            clipboardList = clipboardList,
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
                                }
                            )
                        }

                        if (showProfileDialog) {
                            ProfileDialog(
                                userName = userName,
                                userEmail = userEmail,
                                isCloudMode = isCloudMode,
                                usedGb = usedStorageGb,
                                totalGb = totalStorageGb,
                                lastSyncTime = lastSyncTime,
                                onDismiss = { showProfileDialog = false },
                                onSyncNow = { viewModel.startCloudSync() },
                                onSwitchMode = {
                                    viewModel.currentStep.value = AppStep.MODE_SELECTION
                                    showProfileDialog = false
                                }
                            )
                        }

                        if (showSettingsDialog) {
                            SettingsDialog(
                                onDismiss = { showSettingsDialog = false },
                                onSyncNow = { viewModel.startCloudSync() }
                            )
                        }

                        if (syncProgress.isSyncing || syncProgress.isComplete) {
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
}
