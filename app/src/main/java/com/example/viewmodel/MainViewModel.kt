package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MistralAiEngine
import com.example.cloud.oauth.GoogleOAuthEngine
import com.example.data.database.AppDatabase
import com.example.data.database.entities.*
import com.example.data.files.LocalFileManager
import com.example.data.repository.AppRepository
import com.example.ui.components.NavigationTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val localFileManager: LocalFileManager
    private val prefs = application.getSharedPreferences("cloud_backup_prefs", Context.MODE_PRIVATE)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AppRepository(db.appDao())
        localFileManager = LocalFileManager(application)
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            loadInitialSettings()
        }
    }

    // App Navigation: If onboarding already done previously, start directly on MAIN_WORKSPACE!
    val currentStep = MutableStateFlow(
        if (prefs.getBoolean("onboarding_completed", false)) AppStep.MAIN_WORKSPACE else AppStep.SPLASH
    )
    val currentTab = MutableStateFlow(NavigationTab.HOME)

    // App Security / PIN Lock (Initialized synchronously from prefs so no home screen flicker occurs!)
    val isAppLockEnabled = MutableStateFlow(prefs.getBoolean("app_lock_enabled_pref", false))
    val appLockPin = MutableStateFlow(prefs.getString("app_lock_pin_pref", "1234") ?: "1234")
    val isAppLocked = MutableStateFlow(prefs.getBoolean("app_lock_enabled_pref", false))

    // User settings and mode
    val userName = MutableStateFlow("Rakib")
    val userEmail = MutableStateFlow("mr4425390@gmail.com")
    val userAvatarUrl = MutableStateFlow("")
    val appMode = MutableStateFlow("OFFLINE") // OFFLINE or CLOUD
    val googleAccessToken = MutableStateFlow("")
    val googleRefreshToken = MutableStateFlow("")
    val monthlyBudget = MutableStateFlow(20000.0)
    val usedStorageGb = MutableStateFlow(0.0f)
    val totalStorageGb = MutableStateFlow(15.0f)
    val lastSyncTime = MutableStateFlow("Never")

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

    // Categories
    val customCategories = MutableStateFlow(
        listOf("All", "Personal", "Work", "Ideas", "Emergency", "Study", "Projects", "Finance", "Code", "Diary")
    )

    private suspend fun loadInitialSettings() {
        val onboardingDone = repository.getSetting("onboarding_completed") == "true" ||
                repository.getSetting("app_mode_configured") == "true"

        repository.getSetting("user_name")?.let { userName.value = it }
        repository.getSetting("user_email")?.let { userEmail.value = it }
        repository.getSetting("user_avatar")?.let { userAvatarUrl.value = it }
        repository.getSetting("app_mode")?.let { appMode.value = it }
        repository.getSetting("google_access_token")?.let { googleAccessToken.value = it }
        repository.getSetting("monthly_budget")?.toDoubleOrNull()?.let { monthlyBudget.value = it }
        repository.getSetting("cloud_storage_used_gb")?.toFloatOrNull()?.let { usedStorageGb.value = it }
        repository.getSetting("last_sync_timestamp")?.let { lastSyncTime.value = "Recently" }

        // Security check
        val lockEnabled = repository.getSetting("app_lock_enabled") == "true"
        isAppLockEnabled.value = lockEnabled
        repository.getSetting("app_lock_pin")?.let { appLockPin.value = it }
        if (lockEnabled) {
            isAppLocked.value = true
        }

        // Mistral Key configuration
        repository.getSetting("mistral_api_key")?.let {
            if (it.isNotBlank()) MistralAiEngine.currentApiKey = it
        }
        repository.getSetting("mistral_model")?.let {
            if (it.isNotBlank()) MistralAiEngine.currentModel = it
        }

        // Custom categories load
        repository.getSetting("saved_categories_csv")?.let { csv ->
            val list = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (list.isNotEmpty()) {
                val merged = (customCategories.value + list).distinct()
                customCategories.value = merged
            }
        }

        // If user already did onboarding previously, go straight to Main Workspace!
        if (onboardingDone) {
            prefs.edit().putBoolean("onboarding_completed", true).apply()
            currentStep.value = AppStep.MAIN_WORKSPACE
        }
    }

    fun unlockApp() {
        isAppLocked.value = false
    }

    fun setAppLock(enabled: Boolean, pin: String) {
        prefs.edit()
            .putBoolean("app_lock_enabled_pref", enabled)
            .putString("app_lock_pin_pref", pin)
            .apply()
        viewModelScope.launch {
            isAppLockEnabled.value = enabled
            appLockPin.value = pin
            repository.setSetting("app_lock_enabled", enabled.toString())
            repository.setSetting("app_lock_pin", pin)
        }
    }

    fun completeSplash() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        viewModelScope.launch {
            repository.setSetting("onboarding_completed", "true")
            repository.setSetting("app_mode_configured", "true")
            appMode.value = "OFFLINE"
            repository.setSetting("app_mode", "OFFLINE")
            currentStep.value = AppStep.MAIN_WORKSPACE
        }
    }

    fun completePermissionCenter() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        currentStep.value = AppStep.MAIN_WORKSPACE
    }

    fun selectMode(mode: String, context: Context? = null) {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        viewModelScope.launch {
            appMode.value = mode
            repository.setSetting("app_mode", mode)
            repository.setSetting("app_mode_configured", "true")
            repository.setSetting("onboarding_completed", "true")
            currentStep.value = AppStep.MAIN_WORKSPACE
            if (mode == "CLOUD" && googleAccessToken.value.isEmpty() && context != null) {
                startGoogleOAuthFlow(context)
            }
        }
    }

    fun logoutGoogleAccount() {
        viewModelScope.launch {
            googleAccessToken.value = ""
            googleRefreshToken.value = ""
            userAvatarUrl.value = ""
            userName.value = "Offline User"
            userEmail.value = "offline@workspace"
            appMode.value = "OFFLINE"

            repository.setSetting("google_access_token", "")
            repository.setSetting("google_refresh_token", "")
            repository.setSetting("user_avatar", "")
            repository.setSetting("user_name", "Offline User")
            repository.setSetting("user_email", "offline@workspace")
            repository.setSetting("app_mode", "OFFLINE")

            syncProgress.value = CloudSyncProgress(
                isSyncing = false,
                currentStep = "Logged out from Google Account. Switched to Local Offline Mode.",
                isComplete = true
            )
        }
    }

    fun addCustomCategory(newCat: String) {
        val trimmed = newCat.trim()
        if (trimmed.isNotBlank() && !customCategories.value.contains(trimmed)) {
            val updated = customCategories.value + trimmed
            customCategories.value = updated
            viewModelScope.launch {
                val csv = updated.filter { it != "All" }.joinToString(",")
                repository.setSetting("saved_categories_csv", csv)
                val token = googleAccessToken.value
                if (token.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        GoogleOAuthEngine.backupCategoriesToDrive(token, updated)
                    }
                }
            }
        }
    }

    fun updateMonthlyBudget(newBudget: Double) {
        viewModelScope.launch {
            monthlyBudget.value = newBudget
            repository.setSetting("monthly_budget", newBudget.toString())
        }
    }

    fun attachFile(fileName: String, data: ByteArray, mimeType: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val targetFile = java.io.File(localFileManager.attachmentsDir, fileName)
                targetFile.writeBytes(data)
            }
            val record = FileRecordEntity(
                name = fileName,
                sizeBytes = data.size.toLong(),
                mimeType = mimeType,
                folder = "Files",
                syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
            )
            val id = repository.insertFile(record)

            val token = googleAccessToken.value
            if (token.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    val driveId = GoogleOAuthEngine.backupAttachmentToDrive(token, fileName, data, mimeType)
                    if (driveId != null) {
                        repository.insertFile(record.copy(id = id, cloudUrl = driveId, syncStatus = "SYNCED"))
                    }
                }
            }
        }
    }

    fun attachFileFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    var fileName = "file_${System.currentTimeMillis()}"
                    var fileSize = 0L
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }

                    val safeFileName = fileName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
                    val targetFile = java.io.File(localFileManager.attachmentsDir, safeFileName)

                    contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (fileSize == 0L) fileSize = targetFile.length()

                    val folder = when {
                        mimeType.startsWith("image/") -> "Images"
                        mimeType.contains("pdf") || mimeType.contains("text") || safeFileName.endsWith(".txt") || safeFileName.endsWith(".doc") || safeFileName.endsWith(".pdf") -> "Documents"
                        mimeType.contains("zip") || mimeType.contains("rar") || safeFileName.endsWith(".zip") || safeFileName.endsWith(".rar") || safeFileName.endsWith(".7z") -> "Archives"
                        mimeType.contains("vnd.android.package-archive") || safeFileName.endsWith(".apk") -> "APKs"
                        mimeType.startsWith("audio/") || mimeType.startsWith("video/") -> "Media"
                        else -> "Files"
                    }

                    val record = FileRecordEntity(
                        name = safeFileName,
                        sizeBytes = fileSize,
                        mimeType = mimeType,
                        folder = folder,
                        cloudUrl = targetFile.absolutePath,
                        syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
                    )

                    val id = repository.insertFile(record)

                    val token = googleAccessToken.value
                    if (token.isNotBlank() && targetFile.exists()) {
                        val fileBytes = targetFile.readBytes()
                        val driveId = GoogleOAuthEngine.backupAttachmentToDrive(token, safeFileName, fileBytes, mimeType)
                        if (driveId != null) {
                            repository.insertFile(record.copy(id = id, cloudUrl = driveId, syncStatus = "SYNCED"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error attaching file from Uri", e)
                }
            }
        }
    }

    fun deleteFileRecord(fileRecord: FileRecordEntity) {
        viewModelScope.launch {
            repository.deleteFile(fileRecord)
            repository.deleteFileByName(fileRecord.name)
            withContext(Dispatchers.IO) {
                val targetFile = java.io.File(localFileManager.attachmentsDir, fileRecord.name)
                if (targetFile.exists()) targetFile.delete()
            }
        }
    }

    fun updateUserAvatar(avatarUrl: String) {
        viewModelScope.launch {
            userAvatarUrl.value = avatarUrl
            repository.setSetting("user_avatar", avatarUrl)
        }
    }

    // --- Google OAuth 2.0 Web Authentication & Drive Authorization ---
    fun startGoogleOAuthFlow(context: Context) {
        syncProgress.value = CloudSyncProgress(
            isSyncing = true,
            currentStep = "Starting local server & Google Web Login (:8080)...",
            progressPercent = 0.2f
        )

        GoogleOAuthEngine.startLocalServerAndLaunchBrowser(
            context = context,
            onCodeReceived = { authCode ->
                handleOAuthAuthorizationCode(authCode)
            },
            onError = { errMsg ->
                syncProgress.value = CloudSyncProgress(
                    isSyncing = false,
                    currentStep = "OAuth Error: $errMsg",
                    errorMessage = errMsg
                )
            }
        )
    }

    private var lastExchangedCode: String? = null

    fun handleDeepLinkIntent(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (!code.isNullOrEmpty()) {
            handleOAuthAuthorizationCode(code, GoogleOAuthEngine.LOOPBACK_REDIRECT_URI)
        }
    }

    fun handleOAuthAuthorizationCode(code: String, redirectUri: String = GoogleOAuthEngine.LOOPBACK_REDIRECT_URI) {
        if (code == lastExchangedCode) {
            Log.d("MainViewModel", "Code $code already exchanged or in progress, skipping duplicate.")
            return
        }
        lastExchangedCode = code

        viewModelScope.launch {
            syncProgress.value = CloudSyncProgress(
                isSyncing = true,
                currentStep = "Exchanging Authorization Code with Google...",
                progressPercent = 0.5f
            )

            val result = GoogleOAuthEngine.exchangeCodeForTokens(code, redirectUri)
            result.onSuccess { user ->
                userName.value = user.name
                userEmail.value = user.email
                userAvatarUrl.value = user.pictureUrl
                googleAccessToken.value = user.accessToken
                googleRefreshToken.value = user.refreshToken
                appMode.value = "CLOUD"

                val usedGb = (user.storageUsedBytes.toFloat() / (1024 * 1024 * 1024)).coerceAtLeast(0.1f)
                val totalGb = (user.storageTotalBytes.toFloat() / (1024 * 1024 * 1024)).coerceAtLeast(15.0f)
                usedStorageGb.value = usedGb
                totalStorageGb.value = totalGb

                repository.setSetting("user_name", user.name)
                repository.setSetting("user_email", user.email)
                repository.setSetting("user_avatar", user.pictureUrl)
                repository.setSetting("google_access_token", user.accessToken)
                repository.setSetting("app_mode", "CLOUD")
                repository.setSetting("onboarding_completed", "true")
                repository.setSetting("cloud_storage_used_gb", usedGb.toString())
                repository.setSetting("cloud_storage_total_gb", totalGb.toString())

                syncProgress.value = CloudSyncProgress(
                    isSyncing = true,
                    currentStep = "Restoring & Syncing with Google Drive...",
                    progressPercent = 0.7f
                )

                // Verify / create Drive folder structure
                val rootFolderId = GoogleOAuthEngine.getOrCreateCloudBackupFolder(user.accessToken)
                GoogleOAuthEngine.getOrCreateSubfolder(user.accessToken, rootFolderId, "Notes")
                GoogleOAuthEngine.getOrCreateSubfolder(user.accessToken, rootFolderId, "Tasks")
                GoogleOAuthEngine.getOrCreateSubfolder(user.accessToken, rootFolderId, "Finance")

                // Restore all existing items from Google Drive into Room DB & local storage
                val restored = GoogleOAuthEngine.restoreAllFromDrive(user.accessToken)
                restored.notes.forEach { note ->
                    repository.insertNote(note)
                    withContext(Dispatchers.IO) {
                        localFileManager.saveNoteToFile(note)
                    }
                }
                restored.tasks.forEach { task ->
                    repository.insertTask(task)
                }
                restored.finances.forEach { finance ->
                    repository.insertFinance(finance)
                }
                if (restored.categories.isNotEmpty()) {
                    val merged = (customCategories.value + restored.categories).distinct()
                    customCategories.value = merged
                    repository.setSetting("saved_categories_csv", merged.filter { it != "All" }.joinToString(","))
                }

                // Immediately upload any local notes, tasks and finance to Google Drive
                allNotes.value.forEach { note ->
                    GoogleOAuthEngine.backupNoteToDrive(user.accessToken, note)
                }
                allTasks.value.forEach { task ->
                    GoogleOAuthEngine.backupTaskToDrive(user.accessToken, task)
                }
                allFinance.value.forEach { finance ->
                    GoogleOAuthEngine.backupFinanceToDrive(user.accessToken, finance)
                }
                GoogleOAuthEngine.backupCategoriesToDrive(user.accessToken, customCategories.value)

                // Mark sync complete
                repository.executeCloudSync()
                lastSyncTime.value = "Just now"

                currentStep.value = AppStep.MAIN_WORKSPACE

                syncProgress.value = CloudSyncProgress(
                    isSyncing = false,
                    currentStep = "Connected to ${user.name}'s Google Drive! ${restored.notes.size} notes restored.",
                    progressPercent = 1.0f,
                    isComplete = true
                )
            }.onFailure { ex ->
                syncProgress.value = CloudSyncProgress(
                    isSyncing = false,
                    currentStep = "Connection Failed: ${ex.message}",
                    errorMessage = ex.message
                )
            }
        }
    }

    // --- Automatic Note Backup (Local Hidden File + Real Google Drive) ---
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
                syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
            )

            val savedId = if (id == 0L) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note)
                id
            }

            val savedNote = note.copy(id = savedId)

            // 1. Save to local hidden internal folder .cloud_backup/notes/
            withContext(Dispatchers.IO) {
                localFileManager.saveNoteToFile(savedNote)
            }

            // Remove previous duplicate file records
            val fileName = "note_${savedNote.id}.txt"
            repository.deleteFileByName(fileName)
            repository.deleteFileByName("note_${savedNote.id}.json")

            // Register single clean file record for .txt in database
            repository.insertFile(
                FileRecordEntity(
                    name = fileName,
                    sizeBytes = savedNote.content.toByteArray().size.toLong().coerceAtLeast(64L),
                    mimeType = "text/plain",
                    folder = "Notes",
                    syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
                )
            )

            // 2. Real Google Drive background upload if connected
            val token = googleAccessToken.value
            if (token.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    val driveFileId = GoogleOAuthEngine.backupNoteToDrive(token, savedNote)
                    if (driveFileId != null) {
                        repository.updateNote(savedNote.copy(cloudId = driveFileId, syncStatus = "SYNCED"))
                    }
                }
            }
        }
    }

    fun restoreFromGoogleDriveNow() {
        val token = googleAccessToken.value
        if (token.isBlank()) return
        viewModelScope.launch {
            syncProgress.value = CloudSyncProgress(
                isSyncing = true,
                currentStep = "Downloading all notes, tasks and finance from Google Drive...",
                progressPercent = 0.5f
            )
            val restored = GoogleOAuthEngine.restoreAllFromDrive(token)
            restored.notes.forEach { note ->
                repository.insertNote(note)
                withContext(Dispatchers.IO) {
                    localFileManager.saveNoteToFile(note)
                }
            }
            restored.tasks.forEach { task ->
                repository.insertTask(task)
            }
            restored.finances.forEach { finance ->
                repository.insertFinance(finance)
            }
            if (restored.categories.isNotEmpty()) {
                val merged = (customCategories.value + restored.categories).distinct()
                customCategories.value = merged
                repository.setSetting("saved_categories_csv", merged.filter { it != "All" }.joinToString(","))
            }
            lastSyncTime.value = "Just now"
            syncProgress.value = CloudSyncProgress(
                isSyncing = false,
                currentStep = "Restored ${restored.notes.size} notes, ${restored.tasks.size} tasks, ${restored.finances.size} finance items from Google Drive!",
                progressPercent = 1.0f,
                isComplete = true
            )
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            withContext(Dispatchers.IO) {
                localFileManager.deleteNoteFiles(note.id)
            }
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
                    syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
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
            val token = googleAccessToken.value
            val task = TaskEntity(
                id = id,
                title = title.ifBlank { "Untitled Task" },
                description = description,
                category = category,
                priority = priority,
                dueDate = dueDate,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis(),
                syncStatus = if (token.isNotEmpty()) "SYNCED" else "PENDING"
            )
            val savedId = if (id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
                id
            }

            val savedTask = task.copy(id = savedId)

            // 1. Save to local hidden task file
            withContext(Dispatchers.IO) {
                localFileManager.saveTaskToFile(savedTask)
            }

            // Schedule System Notification Reminder if dueDate exists
            if (dueDate != null && dueDate > System.currentTimeMillis()) {
                com.example.notifications.NotificationHelper.scheduleTaskReminder(
                    context = getApplication(),
                    taskId = savedId,
                    title = savedTask.title,
                    description = savedTask.description,
                    triggerAtMillis = dueDate
                )
            }

            // Remove duplicate file record before inserting single clean entry
            val fileName = "task_${savedTask.id}.txt"
            repository.deleteFileByName(fileName)
            repository.deleteFileByName("task_${savedTask.id}.json")

            repository.insertFile(
                FileRecordEntity(
                    name = fileName,
                    sizeBytes = savedTask.description.toByteArray().size.toLong().coerceAtLeast(64L),
                    mimeType = "text/plain",
                    folder = "Tasks",
                    syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
                )
            )

            // 2. Real Google Drive background upload if connected
            if (token.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    val driveFileId = GoogleOAuthEngine.backupTaskToDrive(token, savedTask)
                    if (driveFileId != null) {
                        repository.updateTask(savedTask.copy(cloudId = driveFileId, syncStatus = "SYNCED"))
                    }
                }
            }
        }
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted, updatedAt = System.currentTimeMillis())
            repository.updateTask(updated)
            if (updated.isCompleted) {
                com.example.notifications.NotificationHelper.cancelTaskReminder(getApplication(), task.id)
            }
            withContext(Dispatchers.IO) {
                localFileManager.saveTaskToFile(updated)
            }
            val token = googleAccessToken.value
            if (token.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    GoogleOAuthEngine.backupTaskToDrive(token, updated)
                }
            }
        }
    }

    fun toggleTaskPin(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isPinned = !task.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            com.example.notifications.NotificationHelper.cancelTaskReminder(getApplication(), task.id)
            repository.deleteTask(task)
            withContext(Dispatchers.IO) {
                localFileManager.deleteTaskFiles(task.id)
            }
        }
    }

    fun convertTaskToNote(task: TaskEntity) {
        saveNote(
            title = task.title,
            content = task.description.ifBlank { "Converted from task: ${task.title}" },
            category = task.category,
            priority = task.priority,
            tags = "Task",
            colorHex = "#2979FF"
        )
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
            val token = googleAccessToken.value
            val entity = FinanceEntity(
                title = title.ifBlank { if (isIncome) "Income" else "Expense" },
                amount = amount,
                isIncome = isIncome,
                category = category,
                notes = notes,
                date = System.currentTimeMillis(),
                syncStatus = if (token.isNotEmpty()) "SYNCED" else "PENDING"
            )
            val savedId = repository.insertFinance(entity)
            val savedEntity = entity.copy(id = savedId)

            withContext(Dispatchers.IO) {
                localFileManager.saveFinanceToFile(savedEntity)
            }

            // Always track finance files
            repository.insertFile(
                FileRecordEntity(
                    name = "tx_${savedEntity.id}.txt",
                    sizeBytes = 120L,
                    mimeType = "text/plain",
                    folder = "Finance",
                    syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
                )
            )
            repository.insertFile(
                FileRecordEntity(
                    name = "tx_${savedEntity.id}.json",
                    sizeBytes = 256L,
                    mimeType = "application/json",
                    folder = "Finance",
                    syncStatus = if (googleAccessToken.value.isNotEmpty()) "SYNCED" else "PENDING"
                )
            )

            if (token.isNotBlank()) {
                withContext(Dispatchers.IO) {
                    val driveFileId = GoogleOAuthEngine.backupFinanceToDrive(token, savedEntity)
                    if (driveFileId != null) {
                        repository.updateFinance(savedEntity.copy(cloudId = driveFileId, syncStatus = "SYNCED"))
                    }
                }
            }
        }
    }

    fun deleteFinance(finance: FinanceEntity) {
        viewModelScope.launch {
            repository.deleteFinance(finance)
        }
    }

    // --- Mistral AI Integration ---
    fun parseExpenseWithMistralAi(
        prompt: String,
        onSuccess: (MistralAiEngine.ParsedTransaction) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = MistralAiEngine.parseFinanceInput(prompt)
            result.onSuccess { tx ->
                addFinanceTransaction(tx.title, tx.amount, tx.isIncome, tx.category, tx.notes)
                onSuccess(tx)
            }.onFailure { ex ->
                onError(ex.message ?: "Failed to parse with Mistral AI")
            }
        }
    }

    fun generateNoteWithMistralAi(
        prompt: String,
        onSuccess: (MistralAiEngine.GeneratedNote) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = MistralAiEngine.generateNoteFromPrompt(prompt)
            result.onSuccess { gen ->
                saveNote(
                    title = gen.title,
                    content = gen.content,
                    category = gen.category,
                    priority = gen.priority,
                    tags = gen.tags,
                    colorHex = "#00E5FF"
                )
                onSuccess(gen)
            }.onFailure { ex ->
                onError(ex.message ?: "Failed to generate note with AI")
            }
        }
    }

    fun getFinancialInsightsWithMistralAi(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val list = allFinance.value
            val income = list.filter { it.isIncome }.sumOf { it.amount }
            val expense = list.filter { !it.isIncome }.sumOf { it.amount }
            val budget = monthlyBudget.value
            val summary = list.take(10).joinToString("; ") { "${it.title}: ৳${it.amount} (${it.category})" }

            val result = MistralAiEngine.getFinancialInsights(income, expense, budget, summary)
            result.onSuccess { advice ->
                onResult(advice)
            }.onFailure {
                onResult("Financial health looks balanced. Keep tracking expenses daily to stay within your monthly budget of ৳$budget.")
            }
        }
    }

    fun updateMistralAiConfig(key: String, model: String) {
        viewModelScope.launch {
            MistralAiEngine.currentApiKey = key
            MistralAiEngine.currentModel = model
            repository.setSetting("mistral_api_key", key)
            repository.setSetting("mistral_model", model)
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

    // --- Cloud Sync Workflow ---
    fun startCloudSync(context: Context? = null) {
        if (googleAccessToken.value.isEmpty()) {
            if (context != null) {
                startGoogleOAuthFlow(context)
            } else {
                syncProgress.value = CloudSyncProgress(
                    isSyncing = false,
                    currentStep = "Please connect your Google Account first.",
                    errorMessage = "No Google Account connected"
                )
            }
            return
        }

        viewModelScope.launch {
            syncProgress.value = CloudSyncProgress(
                isSyncing = true,
                currentStep = "Syncing with Google Drive (drive.file scope)...",
                progressPercent = 0.4f
            )
            delay(500)

            val stats = repository.getPendingSyncStats()
            syncProgress.value = syncProgress.value.copy(
                currentStep = "Uploading ${stats.pendingNotesCount} notes, ${stats.pendingTasksCount} tasks, ${stats.pendingFinanceCount} finance records to Cloud Backup/...",
                progressPercent = 0.8f
            )

            // Upload all pending items to Google Drive
            val token = googleAccessToken.value
            allNotes.value.forEach { note ->
                GoogleOAuthEngine.backupNoteToDrive(token, note)
            }
            allTasks.value.forEach { task ->
                GoogleOAuthEngine.backupTaskToDrive(token, task)
            }
            allFinance.value.forEach { finance ->
                GoogleOAuthEngine.backupFinanceToDrive(token, finance)
            }
            GoogleOAuthEngine.backupCategoriesToDrive(token, customCategories.value)

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
