package com.example.ui.screens.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.ai.MistralAiEngine
import com.example.data.database.entities.*
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.theme.*
import com.example.viewmodel.CloudSyncProgress
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun GlobalSearchDialog(
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    files: List<FileRecordEntity>,
    financeList: List<FinanceEntity>,
    onDismiss: () -> Unit,
    onSelectNote: (NoteEntity) -> Unit,
    onSelectTask: (TaskEntity) -> Unit = {},
    onSelectFinance: (FinanceEntity) -> Unit = {},
    onSelectFile: (FileRecordEntity) -> Unit = {}
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
                            GlassCard(
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    onSelectTask(task)
                                    onDismiss()
                                }
                            ) {
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
                            GlassCard(
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    onSelectFinance(f)
                                    onDismiss()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${f.title} • ৳${f.amount.toInt()}", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }

                        items(matchedFiles) { file ->
                            GlassCard(
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    onSelectFile(file)
                                    onDismiss()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = file.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
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
    userAvatarUrl: String = "",
    isCloudMode: Boolean,
    usedGb: Float,
    totalGb: Float,
    lastSyncTime: String,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onRestoreFromDrive: () -> Unit = {},
    onLogoutAccount: () -> Unit = {},
    onSwitchMode: () -> Unit,
    onManualCodeSubmit: (String) -> Unit,
    onUpdateAvatar: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var manualCodeInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onUpdateAvatar(it.toString())
            android.widget.Toast.makeText(context, "Profile Picture Updated!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }
                }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (isCloudMode) ElectricBlue else DarkSurface)
                                    .border(2.5.dp, if (isCloudMode) NeonCyan else DarkSurfaceBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                var isError by remember(userAvatarUrl) { mutableStateOf(false) }
                                if (userAvatarUrl.isNotBlank() && !isError) {
                                    AsyncImage(
                                        model = userAvatarUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        onError = { isError = true }
                                    )
                                } else {
                                    Text(
                                        text = userName.take(1).uppercase().ifBlank { "U" },
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Camera edit icon overlay
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                                    .border(1.5.dp, DeepNavy, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Change Avatar", tint = DeepNavy, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                            Text(text = "Change Profile Picture 📸", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }

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
                    }
                }

                // Mode Info Card
                item {
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
                            text = "Drive Storage: ${String.format("%.1f", usedGb)} GB used of ${String.format("%.1f", totalGb)} GB",
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }

                item {
                    GradientButton(
                        text = if (isCloudMode) "Sync with Google Drive Now" else "Connect Google Account (Web)",
                        onClick = {
                            onSyncNow()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isCloudMode) {
                    item {
                        OutlinedButton(
                            onClick = {
                                onRestoreFromDrive()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore All Data from Google Drive", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                onLogoutAccount()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PriorityCritical.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = PriorityCritical, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout / Disconnect Google Account", color = PriorityCritical, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    TextButton(onClick = { showManualInput = !showManualInput }) {
                        Text(
                            text = if (showManualInput) "Hide Manual OAuth Code" else "Paste Authorization Code Manually",
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }
                }

                if (showManualInput) {
                    item {
                        GlassCard(shape = RoundedCornerShape(14.dp), backgroundColor = DarkSurface) {
                            Text(text = "MANUAL AUTH CODE SUBMIT", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it },
                                placeholder = { Text("Paste code=4/0A... here", fontSize = 12.sp, color = TextMuted) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkSurfaceBorder
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (manualCodeInput.isNotBlank()) {
                                        onManualCodeSubmit(manualCodeInput.trim())
                                        manualCodeInput = ""
                                        onDismiss()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Submit Code & Authorize")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    isAppLockEnabled: Boolean,
    appLockPin: String,
    onSetAppLock: (Boolean, String) -> Unit,
    mistralApiKey: String,
    mistralModel: String,
    onUpdateMistralConfig: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onRestoreFromDrive: () -> Unit = {},
    onLogoutAccount: () -> Unit = {},
    isCloudMode: Boolean = false
) {
    val context = LocalContext.current
    var autoBackup by remember { mutableStateOf(true) }
    var wifiOnly by remember { mutableStateOf(false) }

    var lockEnabled by remember { mutableStateOf(isAppLockEnabled) }
    var pinCode by remember { mutableStateOf(appLockPin) }

    var aiKey by remember { mutableStateOf(mistralApiKey.ifBlank { MistralAiEngine.DEFAULT_API_KEY }) }
    var selectedModel by remember { mutableStateOf(mistralModel.ifBlank { MistralAiEngine.DEFAULT_MODEL }) }
    val models = listOf("mistral-small-latest", "mistral-large-latest", "codestral-latest", "open-mistral-7b")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
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
                }

                // 1. App Lock & Security
                item {
                    GlassCard(shape = RoundedCornerShape(16.dp), backgroundColor = DarkSurface) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "APP LOCK & SECURITY", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Enable PIN Lock", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(text = "Lock app when leaving or restarting", fontSize = 11.sp, color = TextMuted)
                            }
                            Switch(
                                checked = lockEnabled,
                                onCheckedChange = {
                                    lockEnabled = it
                                    onSetAppLock(it, pinCode)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricBlue)
                            )
                        }

                        if (lockEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = pinCode,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                        pinCode = it
                                        onSetAppLock(lockEnabled, it)
                                    }
                                },
                                label = { Text("4-digit Security PIN", color = NeonCyan) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkSurfaceBorder
                                ),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                // 2. Mistral AI Model & API Key Configuration
                item {
                    GlassCard(shape = RoundedCornerShape(16.dp), backgroundColor = DarkSurface) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "MISTRAL AI CONFIGURATION", fontSize = 11.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "API Key", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = aiKey,
                            onValueChange = { aiKey = it },
                            placeholder = { Text("mstrl_...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonEmerald,
                                unfocusedBorderColor = DarkSurfaceBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "Active AI Model", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            models.forEach { m ->
                                val isSelected = selectedModel == m
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) NeonEmerald else DarkSurfaceElevated)
                                        .clickable { selectedModel = m }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = m,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) DeepNavy else TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                onUpdateMistralConfig(aiKey.trim(), selectedModel)
                                Toast.makeText(context, "Mistral AI config updated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save AI Settings", color = DeepNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Backup & Sync Section
                item {
                    Text(text = "BACKUP & SYNC", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Auto Cloud Sync", fontSize = 13.sp, color = TextPrimary)
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
                        Text(text = "Sync on Wi-Fi Only", fontSize = 13.sp, color = TextPrimary)
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = { wifiOnly = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = ElectricBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isCloudMode) {
                        OutlinedButton(
                            onClick = {
                                onRestoreFromDrive()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore All Data from Drive", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = {
                                onLogoutAccount()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PriorityCritical.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = PriorityCritical, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout Google Account", color = PriorityCritical, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Data & Privacy Section
                item {
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
                }

                item {
                    GradientButton(
                        text = "Sync All Data to Drive Now",
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
