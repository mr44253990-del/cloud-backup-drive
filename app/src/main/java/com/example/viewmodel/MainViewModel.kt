package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entities.*
import com.example.data.repository.AppRepository
import com.example.data.repository.SyncStats
import com.example.ui.components.NavigationTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppStep {
    SPLASH,
    PERMISSION_CENTER,
    MODE_SELECTION,
    MAIN_WORKSPACE
}

data class CloudSyncProgress(
    val isSyncing: Boolean = false,
    val currentStep: String = "",
    val progressPercent: Float = 0f,
    val syncedNotes: Int = 0,
    val syncedTasks: Int = 0,
    val syncedFinance: Int = 0,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db.appDao())
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            loadInitialSettings()
        }
    }

    // App Navigation
    val currentStep = MutableStateFlow(AppStep.SPLASH)
    val currentTab = MutableStateFlow(NavigationTab.HOME)

    // User settings and mode
    val userName = MutableStateFlow("Rakib")
    val userEmail = MutableStateFlow("mr4425390@gmail.com")
    val appMode = MutableStateFlow("OFFLINE") // OFFLINE or CLOUD
    val monthlyBudget = MutableStateFlow(20000.0)
    val usedStorageGb = MutableStateFlow(2.4f)
    val totalStorageGb = MutableStateFlow(15.0f)
    val lastSyncTime = MutableStateFlow("2 minutes ago")

    // Cloud Sync State
    val syncProgress = MutableStateFlow(CloudSyncProgress())

    // Database entities flows
    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFinance: StateFlow<List<FinanceEntity>> = repository.allFinance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClipboard: StateFlow<List<ClipboardEntity>> = repository.allClipboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles: StateFlow<List<FileRecordEntity>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query
    val searchQuery = MutableStateFlow("")

    private suspend fun loadInitialSettings() {
        repository.getSetting("user_name")?.let { userName.value = it }
        repository.getSetting("user_email")?.let { userEmail.value = it }
        repository.getSetting("app_mode")?.let { appMode.value = it }
        repository.getSetting("monthly_budget")?.toDoubleOrNull()?.let { monthlyBudget.value = it }
        repository.getSetting("cloud_storage_used_gb")?.toFloatOrNull()?.let { usedStorageGb.value = it }
    }

    fun completeSplash() {
        viewModelScope.launch {
            val configuredMode = repository.getSetting("app_mode_configured")
            if (configuredMode == "true") {
                currentStep.value = AppStep.MAIN_WORKSPACE
            } else {
                currentStep.value = AppStep.PERMISSION_CENTER
            }
        }
    }

    fun completePermissionCenter() {
        currentStep.value = AppStep.MODE_SELECTION
    }

    fun selectMode(mode: String) {
        viewModelScope.launch {
            appMode.value = mode
            repository.setSetting("app_mode", mode)
            repository.setSetting("app_mode_configured", "true")
            currentStep.value = AppStep.MAIN_WORKSPACE
            if (mode == "CLOUD") {
                startCloudSync()
            }
        }
    }

    // --- Note Actions ---
    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        category: String,
        priority: String,
        tags: String,
        colorHex: String,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            val note = NoteEntity(
                id = id,
                title = title.ifBlank { "Untitled Note" },
                content = content,
                category = category,
                priority = priority,
                tags = tags,
                colorHex = colorHex,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis(),
                syncStatus = if (appMode.value == "CLOUD") "SYNCED" else "PENDING"
            )
            if (id == 0L) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note)
            }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun convertNoteToTask(note: NoteEntity) {
        viewModelScope.launch {
            repository.insertTask(
                TaskEntity(
                    title = note.title,
                    description = note.content,
                    category = note.category,
                    priority = note.priority,
                    dueDate = System.currentTimeMillis() + 86400000L,
                    isCompleted = false,
                    syncStatus = if (appMode.value == "CLOUD") "SYNCED" else "PENDING"
                )
            )
        }
    }

    // --- Task Actions ---
    fun saveTask(
        id: Long = 0,
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: Long? = null,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                id = id,
                title = title.ifBlank { "Untitled Task" },
                description = description,
                category = category,
                priority = priority,
                dueDate = dueDate,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis(),
                syncStatus = if (appMode.value == "CLOUD") "SYNCED" else "PENDING"
            )
            if (id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
        }
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted, updatedAt = System.currentTimeMillis()))
        }
    }

    fun toggleTaskPin(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isPinned = !task.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun convertTaskToNote(task: TaskEntity) {
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(
                    title = task.title,
                    content = task.description.ifBlank { "Task converted from: ${task.title}" },
                    category = task.category,
                    priority = task.priority,
                    tags = "Task",
                    colorHex = "#2979FF",
                    syncStatus = if (appMode.value == "CLOUD") "SYNCED" else "PENDING"
                )
            )
        }
    }

    // --- Finance Actions ---
    fun addFinanceTransaction(
        title: String,
        amount: Double,
        isIncome: Boolean,
        category: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.insertFinance(
                FinanceEntity(
                    title = title.ifBlank { if (isIncome) "Income" else "Expense" },
                    amount = amount,
                    isIncome = isIncome,
                    category = category,
                    notes = notes,
                    date = System.currentTimeMillis(),
                    syncStatus = if (appMode.value == "CLOUD") "SYNCED" else "PENDING"
                )
            )
        }
    }

    fun deleteFinance(finance: FinanceEntity) {
        viewModelScope.launch {
            repository.deleteFinance(finance)
        }
    }

    // --- Clipboard Actions ---
    fun addClipboardItem(content: String, type: String = "Text") {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.insertClipboard(
                ClipboardEntity(
                    content = content,
                    type = type,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteClipboardItem(item: ClipboardEntity) {
        viewModelScope.launch {
            repository.deleteClipboard(item)
        }
    }

    fun clearClipboardHistory() {
        viewModelScope.launch {
            repository.clearUnpinnedClipboard()
        }
    }

    // --- File Records Actions ---
    fun addFileRecord(name: String, sizeBytes: Long, mimeType: String, folder: String) {
        viewModelScope.launch {
            repository.insertFile(
                FileRecordEntity(
                    name = name,
                    sizeBytes = sizeBytes,
                    mimeType = mimeType,
                    folder = folder,
                    syncStatus = "SYNCED"
                )
            )
        }
    }

    fun deleteFileRecord(file: FileRecordEntity) {
        viewModelScope.launch {
            repository.deleteFile(file)
        }
    }

    // --- Cloud Sync / Migration Workflow ---
    fun startCloudSync() {
        viewModelScope.launch {
            syncProgress.value = CloudSyncProgress(
                isSyncing = true,
                currentStep = "Connecting to Google Cloud Account...",
                progressPercent = 0.15f
            )
            delay(600)

            syncProgress.value = syncProgress.value.copy(
                currentStep = "Authorizing Google Drive (drive.file scope)...",
                progressPercent = 0.35f
            )
            delay(700)

            syncProgress.value = syncProgress.value.copy(
                currentStep = "Verifying 'Cloud Backup/' folder structure...",
                progressPercent = 0.60f
            )
            delay(600)

            val stats = repository.getPendingSyncStats()
            syncProgress.value = syncProgress.value.copy(
                currentStep = "Uploading ${stats.pendingNotesCount} notes, ${stats.pendingTasksCount} tasks, ${stats.pendingFinanceCount} finance records...",
                progressPercent = 0.85f
            )
            delay(800)

            val result = repository.executeCloudSync()
            lastSyncTime.value = "Just now"

            syncProgress.value = CloudSyncProgress(
                isSyncing = false,
                currentStep = "All data synchronized with Google Drive!",
                progressPercent = 1.0f,
                syncedNotes = result.syncedNotes,
                syncedTasks = result.syncedTasks,
                syncedFinance = result.syncedFinance,
                isComplete = true
            )
        }
    }

    fun dismissSyncDialog() {
        syncProgress.value = CloudSyncProgress()
    }
}
