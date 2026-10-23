package com.example.data.repository

import com.example.data.database.dao.AppDao
import com.example.data.database.entities.*
import kotlinx.coroutines.flow.Flow

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
    suspend fun updateFinance(finance: FinanceEntity) = appDao.updateFinance(finance)
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
    suspend fun deleteFileByName(name: String) = appDao.deleteFileByName(name)

    // Settings
    val allSettings: Flow<List<AppSettingsEntity>> = appDao.getAllSettings()
    suspend fun getSetting(key: String): String? = appDao.getSetting(key)
    suspend fun setSetting(key: String, value: String) =
        appDao.setSetting(AppSettingsEntity(key, value))

    // Offline -> Cloud Migration Stats
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

    // Clean Workspace Defaults (No demo items as requested)
    suspend fun seedInitialDataIfNeeded() {
        if (getSetting("initialized") == "true") return

        // Default workspace configurations
        setSetting("user_name", "Rakib")
        setSetting("user_email", "mr4425390@gmail.com")
        setSetting("app_mode", "OFFLINE")
        setSetting("theme_accent", "Ocean Blue")
        setSetting("monthly_budget", "20000.0")
        setSetting("cloud_storage_used_gb", "0.0")
        setSetting("cloud_storage_total_gb", "15.0")
        setSetting("auto_backup", "true")
        setSetting("wifi_only", "false")
        setSetting("initialized", "true")
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
