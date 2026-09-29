package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Personal", // Personal, Work, Ideas, Emergency, Custom
    val priority: String = "Normal", // Normal, Important, Urgent, Critical
    val tags: String = "",
    val colorHex: String = "#1E88E5",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val reminderTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, CONFLICT
    val cloudId: String? = null
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val priority: String = "Normal", // Normal, Important, Urgent, Critical
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val subtasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "finance_transactions")
data class FinanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val isIncome: Boolean, // true for Income, false for Expense
    val category: String = "Other", // Food, Transport, Internet, Shopping, Bills, Education, Salary, Other
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "clipboard_items")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val type: String = "Text", // Text, URL, Email, Code, Number
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "file_records")
data class FileRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val folder: String = "Notes", // Notes, Tasks, Files, Finance, Backup
    val cloudUrl: String = "",
    val syncStatus: String = "SYNCED",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
