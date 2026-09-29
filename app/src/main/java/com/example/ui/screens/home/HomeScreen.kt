package com.example.ui.screens.home

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entities.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    userName: String,
    isCloudMode: Boolean,
    usedGb: Float,
    totalGb: Float,
    lastSyncTime: String,
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    files: List<FileRecordEntity>,
    financeList: List<FinanceEntity>,
    onBackupNowClick: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onTaskToggle: (TaskEntity) -> Unit,
    onNavigateTab: (NavigationTab) -> Unit
) {
    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    val pinnedNotes = notes.filter { it.isPinned }
    val pendingSyncCount = notes.count { it.syncStatus == "PENDING" } +
            tasks.count { it.syncStatus == "PENDING" } +
            financeList.count { it.syncStatus == "PENDING" }

    val totalIncome = financeList.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = financeList.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. User Greeting
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "GOOD DAY, ${userName.uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "What would you like to organize today?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // 2. Cloud Status Card with Circular Ring
        item {
            GlassCard(
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkSurfaceElevated,
                borderColor = if (isCloudMode) ElectricBlue.copy(alpha = 0.5f) else DarkSurfaceBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isCloudMode) NeonEmerald else NeonAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCloudMode) "CLOUD SYNC ACTIVE" else "OFFLINE LOCAL MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloudMode) NeonEmerald else NeonAmber,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isCloudMode) "Google Drive Backup" else "Local Storage Only",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isCloudMode) "Last synced $lastSyncTime" else "$pendingSyncCount items waiting for sync",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onBackupNowClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCloudMode) ElectricBlue else DarkSurfaceBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("backup_now_button")
                        ) {
                            Icon(
                                imageVector = if (isCloudMode) Icons.Default.Sync else Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCloudMode) "Backup Now" else "Connect Drive",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    StorageCircularRing(
                        usedGb = usedGb,
                        totalGb = totalGb,
                        primaryColor = if (isCloudMode) NeonCyan else NeonAmber,
                        secondaryColor = ElectricBlue
                    )
                }
            }
        }

        // 3. Quick Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickStatCard(
                    title = "Notes",
                    count = notes.size.toString(),
                    icon = Icons.Default.Description,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(NavigationTab.NOTES) }
                )
                QuickStatCard(
                    title = "Tasks",
                    count = pendingTasks.size.toString(),
                    icon = Icons.Default.CheckCircle,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(NavigationTab.TASKS) }
                )
                QuickStatCard(
                    title = "Files",
                    count = files.size.toString(),
                    icon = Icons.Default.Folder,
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(NavigationTab.TOOLS) }
                )
                QuickStatCard(
                    title = "Balance",
                    count = "৳${balance.toInt()}",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = NeonEmerald,
                    modifier = Modifier.weight(1.2f),
                    onClick = { onNavigateTab(NavigationTab.MONEY) }
                )
            }
        }

        // 4. Today's Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S TASKS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "View All (${tasks.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    modifier = Modifier.clickable { onNavigateTab(NavigationTab.TASKS) }
                )
            }
        }

        if (pendingTasks.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        text = "All tasks completed! Enjoy your day.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(pendingTasks.take(3)) { task ->
                TaskQuickRow(task = task, onToggle = { onTaskToggle(task) })
            }
        }

        // 5. Recent & Pinned Notes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT NOTES",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "All Notes (${notes.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeonCyan,
                    modifier = Modifier.clickable { onNavigateTab(NavigationTab.NOTES) }
                )
            }
        }

        items(notes.take(3)) { note ->
            NoteQuickCard(note = note, onClick = { onNoteClick(note) })
        }

        // 6. Recent Files & Drive Manifest
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT FILES IN CLOUD BACKUP/",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${files.size} Files",
                    fontSize = 12.sp,
                    color = NeonCyan
                )
            }
        }

        items(files.take(3)) { file ->
            GlassCard(
                shape = RoundedCornerShape(14.dp),
                backgroundColor = DarkSurfaceGlass
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = file.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${file.folder}/ • ${(file.sizeBytes / 1024)} KB",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                    SyncStatusChip(syncStatus = file.syncStatus)
                }
            }
        }
    }
}

@Composable
fun QuickStatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun TaskQuickRow(task: TaskEntity, onToggle: () -> Unit) {
    GlassCard(
        shape = RoundedCornerShape(14.dp),
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NeonCyan,
                        uncheckedColor = DarkSurfaceBorder
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) TextMuted else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.category,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            PriorityBadge(priority = task.priority)
        }
    }
}

@Composable
fun NoteQuickCard(note: NoteEntity, onClick: () -> Unit) {
    GlassCard(
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = NeonAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = note.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                PriorityBadge(priority = note.priority)
                Spacer(modifier = Modifier.height(6.dp))
                SyncStatusChip(syncStatus = note.syncStatus)
            }
        }
    }
}
