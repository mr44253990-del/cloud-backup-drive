package com.example.data.repository

import com.example.data.database.dao.AppDao
import com.example.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val appDao: AppDao) {

    // Notes
    val allNotes: Flow<List<NoteEntity>> = appDao.getAllNotes()
    val pinnedNotes: Flow<List<NoteEntity>> = appDao.getPinnedNotes()
    fun searchNotes(query: String): Flow<List<NoteEntity>> = appDao.searchNotes(query)
    suspend fun insertNote(note: NoteEntity) = appDao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = appDao.updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = appDao.deleteNote(note)
    suspend fun deleteNoteById(id: Long) = appDao.deleteNoteById(id)

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = appDao.getAllTasks()
    val pendingTasks: Flow<List<TaskEntity>> = appDao.getPendingTasks()
    fun searchTasks(query: String): Flow<List<TaskEntity>> = appDao.searchTasks(query)
    suspend fun insertTask(task: TaskEntity) = appDao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = appDao.updateTask(task)
    suspend fun deleteTask(task: TaskEntity) = appDao.deleteTask(task)
    suspend fun deleteTaskById(id: Long) = appDao.deleteTaskById(id)

    // Finance
    val allFinance: Flow<List<FinanceEntity>> = appDao.getAllFinance()
    fun searchFinance(query: String): Flow<List<FinanceEntity>> = appDao.searchFinance(query)
    suspend fun insertFinance(finance: FinanceEntity) = appDao.insertFinance(finance)
    suspend fun deleteFinance(finance: FinanceEntity) = appDao.deleteFinance(finance)
    suspend fun deleteFinanceById(id: Long) = appDao.deleteFinanceById(id)

    // Clipboard
    val allClipboard: Flow<List<ClipboardEntity>> = appDao.getAllClipboard()
    suspend fun insertClipboard(item: ClipboardEntity) = appDao.insertClipboard(item)
    suspend fun deleteClipboard(item: ClipboardEntity) = appDao.deleteClipboard(item)
    suspend fun clearUnpinnedClipboard() = appDao.clearUnpinnedClipboard()

    // Files
    val allFiles: Flow<List<FileRecordEntity>> = appDao.getAllFiles()
    fun searchFiles(query: String): Flow<List<FileRecordEntity>> = appDao.searchFiles(query)
    suspend fun insertFile(file: FileRecordEntity) = appDao.insertFile(file)
    suspend fun deleteFile(file: FileRecordEntity) = appDao.deleteFile(file)
    suspend fun deleteFileById(id: Long) = appDao.deleteFileById(id)

    // Settings
    val allSettings: Flow<List<AppSettingsEntity>> = appDao.getAllSettings()
    suspend fun getSetting(key: String): String? = appDao.getSetting(key)
    suspend fun setSetting(key: String, value: String) =
        appDao.setSetting(AppSettingsEntity(key, value))

    // Offline -> Cloud Migration
    suspend fun getPendingSyncStats(): SyncStats {
        val notes = appDao.getPendingSyncNotes()
        val tasks = appDao.getPendingSyncTasks()
        val finance = appDao.getPendingSyncFinance()
        return SyncStats(
            pendingNotesCount = notes.size,
            pendingTasksCount = tasks.size,
            pendingFinanceCount = finance.size,
            totalPending = notes.size + tasks.size + finance.size
        )
    }

    suspend fun executeCloudSync(): SyncResult {
        val stats = getPendingSyncStats()
        appDao.markAllNotesSynced()
        appDao.markAllTasksSynced()
        appDao.markAllFinanceSynced()
        setSetting("last_sync_timestamp", System.currentTimeMillis().toString())
        return SyncResult(
            syncedNotes = stats.pendingNotesCount,
            syncedTasks = stats.pendingTasksCount,
            syncedFinance = stats.pendingFinanceCount,
            syncedAt = System.currentTimeMillis(),
            success = true
        )
    }

    // Initial Database Seeding with Rakib's workspace defaults if empty
    suspend fun seedInitialDataIfNeeded() {
        val existingNotes = allNotes.first()
        if (existingNotes.isNotEmpty()) return

        // Seed Notes
        appDao.insertNote(
            NoteEntity(
                title = "Android Personal Workspace Architecture",
                content = "Native Android Personal Workspace with Notes, Tasks, Files, Finance, Cloud Backup and Tools.\n\nStructure:\n• Room local database with offline-first persistence\n• Google Sign-In & Drive authorization separation\n• Google Drive folder: Cloud Backup/ (Notes, Tasks, Files, Finance, Backup)\n• Glassmorphic electric blue & neon dark UI",
                category = "Work",
                priority = "Critical",
                tags = "Android, Architecture, Drive, Room",
                colorHex = "#2979FF",
                isPinned = true,
                syncStatus = "SYNCED"
            )
        )
        appDao.insertNote(
            NoteEntity(
                title = "OAuth 2.0 Client Credentials & Keytool",
                content = "Client ID: 688078973753-q0j8243na8m0i5d0l7kvh4id9ckd21bt.apps.googleusercontent.com\nProject: mtxr-d8b90\n\nScope required: drive.file (safe scoped access for dedicated app folder)\n\nKeytool command for debug keystore:\nkeytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android",
                category = "Personal",
                priority = "Important",
                tags = "OAuth, Security, Keytool, Drive",
                colorHex = "#7C4DFF",
                isPinned = true,
                syncStatus = "SYNCED"
            )
        )
        appDao.insertNote(
            NoteEntity(
                title = "Ideas: High-Performance Note & File Engine",
                content = "1. Auto-save with debouncing\n2. Smart preview for Code, Checklist, Text, Links\n3. Biometric App Lock\n4. Cloud Backup manifest generator\n5. Fast full-text global search across all entities",
                category = "Ideas",
                priority = "Normal",
                tags = "Ideas, Roadmap",
                colorHex = "#00E5FF",
                isPinned = false,
                syncStatus = "SYNCED"
            )
        )

        // Seed Tasks
        appDao.insertTask(
            TaskEntity(
                title = "Verify Google Drive drive.file authorization scope",
                description = "Ensure separation of sign-in authentication and Drive authorization consent.",
                category = "Development",
                priority = "Critical",
                dueDate = System.currentTimeMillis() + 86400000L,
                isCompleted = false,
                isPinned = true,
                syncStatus = "SYNCED"
            )
        )
        appDao.insertTask(
            TaskEntity(
                title = "Test Offline to Cloud auto-migration sync queue",
                description = "Create records offline, then connect Google Account and verify batch upload.",
                category = "Testing",
                priority = "Important",
                dueDate = System.currentTimeMillis() + 172800000L,
                isCompleted = false,
                isPinned = false,
                syncStatus = "SYNCED"
            )
        )
        appDao.insertTask(
            TaskEntity(
                title = "Setup GitHub Actions APK build workflow with SHA-256",
                description = "Created .github/workflows/build-apk.yml with keytool fingerprints and artifact upload.",
                category = "DevOps",
                priority = "Normal",
                dueDate = System.currentTimeMillis(),
                isCompleted = true,
                isPinned = false,
                syncStatus = "SYNCED"
            )
        )

        // Seed Finance Transactions
        appDao.insertFinance(
            FinanceEntity(
                title = "Monthly Project Income",
                amount = 25000.0,
                isIncome = true,
                category = "Salary",
                notes = "September 2026 Dev earnings",
                syncStatus = "SYNCED"
            )
        )
        appDao.insertFinance(
            FinanceEntity(
                title = "Cloud Infrastructure & Domain",
                amount = 3500.0,
                isIncome = false,
                category = "Internet",
                notes = "Google Cloud & hosting subscriptions",
                syncStatus = "SYNCED"
            )
        )
        appDao.insertFinance(
            FinanceEntity(
                title = "Workspace Equipment & Office",
                amount = 5200.0,
                isIncome = false,
                category = "Bills",
                notes = "Desk monitor and setup gear",
                syncStatus = "SYNCED"
            )
        )
        appDao.insertFinance(
            FinanceEntity(
                title = "Groceries & Food Supplies",
                amount = 3800.0,
                isIncome = false,
                category = "Food",
                notes = "Weekly nutrition & meal supplies",
                syncStatus = "SYNCED"
            )
        )

        // Seed File Records
        appDao.insertFile(
            FileRecordEntity(
                name = "cloud_backup_manifest.json",
                sizeBytes = 24500L,
                mimeType = "application/json",
                folder = "Backup",
                syncStatus = "SYNCED"
            )
        )
        appDao.insertFile(
            FileRecordEntity(
                name = "workspace_architecture_v2.pdf",
                sizeBytes = 1420000L,
                mimeType = "application/pdf",
                folder = "Files",
                syncStatus = "SYNCED"
            )
        )
        appDao.insertFile(
            FileRecordEntity(
                name = "database_schema_export.sql",
                sizeBytes = 86000L,
                mimeType = "text/plain",
                folder = "Notes",
                syncStatus = "SYNCED"
            )
        )

        // Seed Clipboard
        appDao.insertClipboard(
            ClipboardEntity(
                content = "keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android",
                type = "Code",
                isPinned = true
            )
        )
        appDao.insertClipboard(
            ClipboardEntity(
                content = "688078973753-q0j8243na8m0i5d0l7kvh4id9ckd21bt.apps.googleusercontent.com",
                type = "Text",
                isPinned = true
            )
        )

        // Default Settings
        setSetting("user_name", "Rakib")
        setSetting("user_email", "mr4425390@gmail.com")
        setSetting("app_mode", "OFFLINE")
        setSetting("theme_accent", "Ocean Blue")
        setSetting("monthly_budget", "20000.0")
        setSetting("cloud_storage_used_gb", "2.4")
        setSetting("cloud_storage_total_gb", "15.0")
        setSetting("auto_backup", "true")
        setSetting("wifi_only", "false")
    }
}

data class SyncStats(
    val pendingNotesCount: Int,
    val pendingTasksCount: Int,
    val pendingFinanceCount: Int,
    val totalPending: Int
)

data class SyncResult(
    val syncedNotes: Int,
    val syncedTasks: Int,
    val syncedFinance: Int,
    val syncedAt: Long,
    val success: Boolean
)
