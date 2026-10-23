package com.example.data.files

import android.content.Context
import android.util.Log
import com.example.data.database.entities.NoteEntity
import org.json.JSONObject
import java.io.File

class LocalFileManager(private val context: Context) {

    private val TAG = "LocalFileManager"

    // Hidden folder inside app internal storage
    val rootBackupDir: File = File(context.filesDir, ".cloud_backup").apply {
        if (!exists()) mkdirs()
    }

    val notesDir: File = File(rootBackupDir, "notes").apply {
        if (!exists()) mkdirs()
    }

    val tasksDir: File = File(rootBackupDir, "tasks").apply {
        if (!exists()) mkdirs()
    }

    val financeDir: File = File(rootBackupDir, "finance").apply {
        if (!exists()) mkdirs()
    }

    val attachmentsDir: File = File(rootBackupDir, "attachments").apply {
        if (!exists()) mkdirs()
    }

    fun saveNoteToFile(note: NoteEntity): File {
        return try {
            val safeTitle = note.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(30)
            val textFile = File(notesDir, "note_${note.id}_$safeTitle.txt")
            textFile.writeText(
                "Title: ${note.title}\n" +
                "Category: ${note.category}\n" +
                "Priority: ${note.priority}\n" +
                "Tags: ${note.tags}\n" +
                "Updated: ${note.updatedAt}\n\n" +
                note.content
            )

            // Also save structured JSON file for clean restore/migration
            val jsonFile = File(notesDir, "note_${note.id}.json")
            val json = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("category", note.category)
                put("priority", note.priority)
                put("tags", note.tags)
                put("colorHex", note.colorHex)
                put("isPinned", note.isPinned)
                put("updatedAt", note.updatedAt)
            }
            jsonFile.writeText(json.toString(2))
            textFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note file", e)
            File(notesDir, "note_${note.id}.txt")
        }
    }

    fun deleteNoteFiles(noteId: Long) {
        try {
            notesDir.listFiles()?.filter { it.name.startsWith("note_${noteId}") }?.forEach {
                it.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note files", e)
        }
    }

    fun saveTaskToFile(task: com.example.data.database.entities.TaskEntity): File {
        return try {
            val safeTitle = task.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(30)
            val textFile = File(tasksDir, "task_${task.id}_$safeTitle.txt")
            textFile.writeText(
                "Title: ${task.title}\n" +
                "Category: ${task.category}\n" +
                "Priority: ${task.priority}\n" +
                "Completed: ${task.isCompleted}\n" +
                "Due: ${task.dueDate}\n" +
                "Updated: ${task.updatedAt}\n\n" +
                task.description
            )
            val jsonFile = File(tasksDir, "task_${task.id}.json")
            val json = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("category", task.category)
                put("priority", task.priority)
                put("dueDate", task.dueDate ?: 0L)
                put("isCompleted", task.isCompleted)
                put("isPinned", task.isPinned)
                put("updatedAt", task.updatedAt)
            }
            jsonFile.writeText(json.toString(2))
            textFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving task file", e)
            File(tasksDir, "task_${task.id}.txt")
        }
    }

    fun deleteTaskFiles(taskId: Long) {
        try {
            tasksDir.listFiles()?.filter { it.name.startsWith("task_${taskId}") }?.forEach {
                it.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task files", e)
        }
    }

    fun saveFinanceToFile(finance: com.example.data.database.entities.FinanceEntity): File {
        return try {
            val safeTitle = finance.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_").take(30)
            val textFile = File(financeDir, "tx_${finance.id}_$safeTitle.txt")
            textFile.writeText(
                "Title: ${finance.title}\n" +
                "Amount: ${finance.amount}\n" +
                "Type: ${if (finance.isIncome) "Income" else "Expense"}\n" +
                "Category: ${finance.category}\n" +
                "Date: ${finance.date}\n" +
                "Notes: ${finance.notes}"
            )
            val jsonFile = File(financeDir, "tx_${finance.id}.json")
            val json = JSONObject().apply {
                put("id", finance.id)
                put("title", finance.title)
                put("amount", finance.amount)
                put("isIncome", finance.isIncome)
                put("category", finance.category)
                put("notes", finance.notes)
                put("date", finance.date)
            }
            jsonFile.writeText(json.toString(2))
            textFile
        } catch (e: Exception) {
            Log.e(TAG, "Error saving finance file", e)
            File(financeDir, "tx_${finance.id}.txt")
        }
    }

    fun deleteFinanceFiles(financeId: Long) {
        try {
            financeDir.listFiles()?.filter { it.name.startsWith("tx_${financeId}") }?.forEach {
                it.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting finance files", e)
        }
    }

    fun readTextFile(file: File): String {
        return if (file.exists()) file.readText() else ""
    }

    fun getAllLocalNotesFiles(): List<File> {
        return notesDir.listFiles()?.toList() ?: emptyList()
    }
}
