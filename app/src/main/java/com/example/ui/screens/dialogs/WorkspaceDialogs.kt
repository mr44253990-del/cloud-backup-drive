package com.example.ui.screens.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.entities.*
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.theme.*
import com.example.viewmodel.CloudSyncProgress

@Composable
fun GlobalSearchDialog(
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    files: List<FileRecordEntity>,
    financeList: List<FinanceEntity>,
    onDismiss: () -> Unit,
    onSelectNote: (NoteEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matchedNotes = if (query.isBlank()) emptyList() else notes.filter {
        it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
    }
    val matchedTasks = if (query.isBlank()) emptyList() else tasks.filter {
        it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
    }
    val matchedFiles = if (query.isBlank()) emptyList() else files.filter {
        it.name.contains(query, ignoreCase = true)
    }
    val matchedFinance = if (query.isBlank()) emptyList() else financeList.filter {
        it.title.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GLOBAL WORKSPACE SEARCH",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search notes, tasks, files & finance...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                    modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (query.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SearchCountBadge("Notes", matchedNotes.size, ElectricBlue)
                        SearchCountBadge("Tasks", matchedTasks.size, NeonCyan)
                        SearchCountBadge("Files", matchedFiles.size, NeonPurple)
                        SearchCountBadge("Money", matchedFinance.size, NeonEmerald)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (query.isBlank()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                Text("Type anything to search across the entire workspace", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(matchedNotes) { note ->
                            GlassCard(
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    onSelectNote(note)
                                    onDismiss()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = note.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text(text = note.content, maxLines = 1, color = TextSecondary, fontSize = 11.sp, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        items(matchedTasks) { task ->
                            GlassCard(shape = RoundedCornerShape(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = task.title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                                        Text(text = "Task • ${task.priority}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        items(matchedFinance) { f ->
                            GlassCard(shape = RoundedCornerShape(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${f.title} • ৳${f.amount.toInt()}", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchCountBadge(label: String, count: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = "$label: $count", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ProfileDialog(
    userName: String,
    userEmail: String,
    isCloudMode: Boolean,
    usedGb: Float,
    totalGb: Float,
    lastSyncTime: String,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onSwitchMode: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isCloudMode) ElectricBlue else DarkSurface)
                        .border(2.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = userEmail,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Info Card
                GlassCard(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = DarkSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "CURRENT MODE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isCloudMode) "Google Drive Cloud" else "Local Offline Mode",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloudMode) NeonCyan else NeonAmber
                            )
                        }
                        TextButton(onClick = onSwitchMode) {
                            Text("Switch Mode", color = ElectricBlueLight, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Google Drive Folder: Cloud Backup/\n(Notes, Tasks, Files, Finance, Backup)",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Storage: ${String.format("%.1f", usedGb)} GB used of ${String.format("%.1f", totalGb)} GB",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                GradientButton(
                    text = if (isCloudMode) "Sync with Google Drive Now" else "Connect Google Account",
                    onClick = {
                        onSyncNow()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit
) {
    val context = LocalContext.current
    var autoBackup by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SETTINGS & PREFERENCES",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Backup Section
                Text(text = "BACKUP & SYNC", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Auto Cloud Sync", fontSize = 14.sp, color = TextPrimary)
                    Switch(
                        checked = autoBackup,
                        onCheckedChange = { autoBackup = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricBlue)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sync on Wi-Fi Only", fontSize = 14.sp, color = TextPrimary)
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = { wifiOnly = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricBlue)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security Section
                Text(text = "DATA & PRIVACY", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val manifestJson = """
                        {
                          "app": "Cloud Backup",
                          "author": "Rakib",
                          "version": "1.0",
                          "exportedAt": "${System.currentTimeMillis()}",
                          "manifest": "cloud_backup_manifest.json"
                        }
                        """.trimIndent()
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Manifest", manifestJson))
                        Toast.makeText(context, "Exported Manifest JSON to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Data Manifest JSON", color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(20.dp))

                GradientButton(
                    text = "Sync All Now",
                    onClick = {
                        onSyncNow()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun CloudSyncDialog(
    progress: CloudSyncProgress,
    onDismiss: () -> Unit
) {
    val animatedPercent by animateFloatAsState(
        targetValue = progress.progressPercent,
        label = "syncPercent"
    )

    Dialog(onDismissRequest = { if (!progress.isSyncing) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (progress.isComplete) NeonEmerald.copy(alpha = 0.2f) else ElectricBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (progress.isComplete) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (progress.isComplete) NeonEmerald else NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (progress.isComplete) "SYNC COMPLETED" else "PREPARING CLOUD SYNC",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = progress.currentStep,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                LinearProgressIndicator(
                    progress = { animatedPercent },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (progress.isComplete) NeonEmerald else NeonCyan,
                    trackColor = DarkSurfaceBorder
                )

                if (progress.isComplete) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "• Synced ${progress.syncedNotes} Notes\n• Synced ${progress.syncedTasks} Tasks\n• Synced ${progress.syncedFinance} Finance Records\n• Google Drive Folder: Cloud Backup/",
                        fontSize = 12.sp,
                        color = NeonCyan,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = DeepNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
