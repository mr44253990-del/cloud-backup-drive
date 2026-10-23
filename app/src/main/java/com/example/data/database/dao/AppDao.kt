package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Notes ---
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET syncStatus = 'SYNCED' WHERE syncStatus = 'PENDING'")
    suspend fun markAllNotesSynced()

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, isPinned DESC, dueDate ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY isPinned DESC, dueDate ASC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncTasks(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET syncStatus = 'SYNCED' WHERE syncStatus = 'PENDING'")
    suspend fun markAllTasksSynced()

    // --- Finance ---
    @Query("SELECT * FROM finance_transactions ORDER BY date DESC")
    fun getAllFinance(): Flow<List<FinanceEntity>>

    @Query("SELECT * FROM finance_transactions WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchFinance(query: String): Flow<List<FinanceEntity>>

    @Query("SELECT * FROM finance_transactions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncFinance(): List<FinanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinance(finance: FinanceEntity): Long

    @Update
    suspend fun updateFinance(finance: FinanceEntity)

    @Delete
    suspend fun deleteFinance(finance: FinanceEntity)

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteFinanceById(id: Long)

    @Query("UPDATE finance_transactions SET syncStatus = 'SYNCED' WHERE syncStatus = 'PENDING'")
    suspend fun markAllFinanceSynced()

    // --- Clipboard ---
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC LIMIT 50")
    fun getAllClipboard(): Flow<List<ClipboardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClipboard(item: ClipboardEntity): Long

    @Delete
    suspend fun deleteClipboard(item: ClipboardEntity)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinnedClipboard()

    // --- Files ---
    @Query("SELECT * FROM file_records ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<FileRecordEntity>>

    @Query("SELECT * FROM file_records WHERE name LIKE '%' || :query || '%'")
    fun searchFiles(query: String): Flow<List<FileRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileRecordEntity): Long

    @Delete
    suspend fun deleteFile(file: FileRecordEntity)

    @Query("DELETE FROM file_records WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("DELETE FROM file_records WHERE name = :name")
    suspend fun deleteFileByName(name: String)

    // --- Settings ---
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSettingsEntity>>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSettingsEntity)

    // --- Categories ---
    @Query("SELECT * FROM custom_categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
