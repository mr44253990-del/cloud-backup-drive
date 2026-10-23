package com.example.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.database.entities.ClipboardEntity
import com.example.data.database.entities.FileRecordEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.QuickCopyButton
import com.example.ui.theme.*
import java.io.File
import kotlin.random.Random

@Composable
fun ToolsScreen(
    files: List<FileRecordEntity> = emptyList(),
    clipboardList: List<ClipboardEntity>,
    onAttachFileUri: (Uri) -> Unit = {},
    onDeleteFile: (FileRecordEntity) -> Unit = {},
    onAddClipboard: (String, String) -> Unit,
    onDeleteClipboard: (ClipboardEntity) -> Unit,
    onClearClipboard: () -> Unit,
    onSaveAsNote: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedToolTab by remember { mutableStateOf("File Vault") }
    val toolTabs = listOf("File Vault", "Clipboard", "Keytool & SHA", "Calculator", "Password Gen", "Text Counter", "Storage Converter")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .testTag("tools_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PERSONAL WORKSPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Smart Tools & File Vault",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Categorized files, dev utilities & productivity essentials",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tool Switcher Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                toolTabs.forEach { tab ->
                    val isSelected = selectedToolTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ElectricBlue else DarkSurfaceElevated)
                            .border(1.dp, if (isSelected) ElectricBlue else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedToolTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = tab,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Content according to selected tab
            when (selectedToolTab) {
                "File Vault" -> FileVaultToolView(
                    files = files,
                    onAttachFileUri = onAttachFileUri,
                    onDeleteFile = onDeleteFile
                )
                "Clipboard" -> ClipboardToolView(
                    clipboardList = clipboardList,
                    onAddClipboard = onAddClipboard,
                    onDeleteClipboard = onDeleteClipboard,
                    onClearClipboard = onClearClipboard,
                    onSaveAsNote = onSaveAsNote
                )
                "Keytool & SHA" -> KeytoolHelperView()
                "Calculator" -> CalculatorToolView()
                "Password Gen" -> PasswordGeneratorView()
                "Text Counter" -> TextCounterToolView()
                "Storage Converter" -> StorageConverterView()
            }
        }
    }
}

@Composable
fun ClipboardToolView(
    clipboardList: List<ClipboardEntity>,
    onAddClipboard: (String, String) -> Unit,
    onDeleteClipboard: (ClipboardEntity) -> Unit,
    onClearClipboard: () -> Unit,
    onSaveAsNote: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassCard(shape = RoundedCornerShape(16.dp)) {
            Text(text = "SMART CLIPBOARD CLIP", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Paste or type text to save in clipboard manager...", color = TextMuted) },
                modifier = Modifier.fillMaxWidth().height(90.dp).testTag("clipboard_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClearClipboard) {
                    Text("Clear All", color = PriorityCritical, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onAddClipboard(inputText, "Text")
                            inputText = ""
                            Toast.makeText(context, "Saved to Clipboard history!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Clip")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "SAVED CLIPS (${clipboardList.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(clipboardList, key = { it.id }) { item ->
                GlassCard(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = DarkSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.content,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("Clip", item.content))
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onSaveAsNote(item.content) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.NoteAdd, contentDescription = "To Note", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onDeleteClipboard(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeytoolHelperView() {
    val context = LocalContext.current
    val keytoolCmd = "keytool -list -v -keystore \"\$HOME/.android/debug.keystore\" -alias androiddebugkey -storepass android -keypass android"
    val clientId = "688078973753-q0j8243na8m0i5d0l7kvh4id9ckd21bt.apps.googleusercontent.com"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                backgroundColor = DarkSurfaceElevated,
                borderColor = NeonCyan.copy(alpha = 0.5f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "DEBUG KEYSTORE SHA-1 & SHA-256", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Use this terminal command in Linux / AndroidIDE / Termux to extract your SHA fingerprints for Google Sign-In & Google Cloud OAuth:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = keytoolCmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NeonEmerald
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Keytool Command", keytoolCmd))
                        Toast.makeText(context, "Keytool command copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Keytool Command")
                }
            }
        }

        item {
            GlassCard(shape = RoundedCornerShape(18.dp)) {
                Text(text = "GOOGLE OAUTH WEB & DRIVE CONFIGURATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricBlueLight)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Web Client ID (Active):", fontSize = 11.sp, color = TextMuted)
                Text(text = "688078973753-npfpcakg6ffhbgesdiri82u79qcvvlbv.apps.googleusercontent.com", fontSize = 11.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Authorized Redirect URIs (Add to Google Cloud Console):", fontSize = 11.sp, color = TextMuted)
                Text(text = "1. http://127.0.0.1:8080/callback\n2. http://localhost:8080/callback\n3. cloudbackup://oauth2callback", fontSize = 11.sp, color = NeonCyan, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Drive OAuth Scope:", fontSize = 11.sp, color = TextMuted)
                Text(text = "https://www.googleapis.com/auth/drive.file", fontSize = 11.sp, color = NeonEmerald, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Redirect URI", "http://127.0.0.1:8080/callback"))
                            Toast.makeText(context, "Redirect URI copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Redirect URI", color = TextPrimary, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Client ID", "688078973753-npfpcakg6ffhbgesdiri82u79qcvvlbv.apps.googleusercontent.com"))
                            Toast.makeText(context, "Web Client ID copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Client ID", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            GlassCard(shape = RoundedCornerShape(18.dp)) {
                Text(text = "GITHUB ACTIONS APK WORKFLOW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A workflow has been configured in .github/workflows/build-apk.yml that automatically generates the APK on push, computes SHA-1 and SHA-256 fingerprints, and outputs the downloadable artifact.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun CalculatorToolView() {
    var display by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }

    fun onNumber(num: String) {
        display = if (display == "0") num else display + num
    }

    fun onOp(op: String) {
        operand1 = display.toDoubleOrNull()
        pendingOp = op
        display = "0"
    }

    fun onEqual() {
        val op2 = display.toDoubleOrNull()
        if (operand1 != null && op2 != null && pendingOp != null) {
            val res = when (pendingOp) {
                "+" -> operand1!! + op2
                "-" -> operand1!! - op2
                "×" -> operand1!! * op2
                "÷" -> if (op2 != 0.0) operand1!! / op2 else 0.0
                else -> op2
            }
            display = if (res % 1.0 == 0.0) res.toInt().toString() else String.format("%.2f", res)
            operand1 = null
            pendingOp = null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
                .padding(20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = display,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad buttons
        val rows = listOf(
            listOf("C", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=", "")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { btn ->
                    if (btn.isNotEmpty()) {
                        val isOp = btn in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = btn in listOf("C", "±", "%")
                        val btnColor = when {
                            btn == "=" -> ElectricBlue
                            isOp -> DarkSurfaceElevated
                            isSpecial -> DarkSurfaceGlass
                            else -> DarkSurface
                        }
                        val textColor = when {
                            btn == "=" -> Color.White
                            isOp -> NeonCyan
                            isSpecial -> NeonPink
                            else -> TextPrimary
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(btnColor)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    when (btn) {
                                        "C" -> {
                                            display = "0"
                                            operand1 = null
                                            pendingOp = null
                                        }
                                        "=" -> onEqual()
                                        "+", "-", "×", "÷" -> onOp(btn)
                                        "." -> if (!display.contains(".")) display += "."
                                        "±" -> {
                                            val v = display.toDoubleOrNull() ?: 0.0
                                            display = (-v).toString()
                                        }
                                        else -> onNumber(btn)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btn,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordGeneratorView() {
    val context = LocalContext.current
    var passwordLength by remember { mutableStateOf(16f) }
    var generatedPassword by remember { mutableStateOf("K8#mQ9!zL2@vX4\$w") }

    fun generate() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val len = passwordLength.toInt()
        val pwd = (1..len).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        generatedPassword = pwd
    }

    GlassCard(shape = RoundedCornerShape(18.dp)) {
        Text(text = "PASSWORD GENERATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = generatedPassword,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Length: ${passwordLength.toInt()} characters", fontSize = 12.sp, color = TextSecondary)
        Slider(
            value = passwordLength,
            onValueChange = { passwordLength = it },
            valueRange = 8f..32f,
            steps = 23,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = ElectricBlue)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { generate() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Generate")
            }

            OutlinedButton(
                onClick = {
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Password", generatedPassword))
                    Toast.makeText(context, "Password copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
            ) {
                Text("Copy", color = NeonCyan)
            }
        }
    }
}

@Composable
fun TextCounterToolView() {
    var text by remember { mutableStateOf("") }
    val charCount = text.length
    val wordCount = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
    val lineCount = if (text.isEmpty()) 0 else text.split("\n").size

    Column {
        GlassCard(shape = RoundedCornerShape(18.dp)) {
            Text(text = "TEXT FORMATTER & COUNTER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Enter or paste text to analyze...", color = TextMuted) },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextCounterCard(title = "Words", count = wordCount.toString(), modifier = Modifier.weight(1f))
                TextCounterCard(title = "Characters", count = charCount.toString(), modifier = Modifier.weight(1f))
                TextCounterCard(title = "Lines", count = lineCount.toString(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TextCounterCard(title: String, count: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
fun StorageConverterView() {
    var inputMbText by remember { mutableStateOf("1024") }
    val mbVal = inputMbText.toDoubleOrNull() ?: 0.0

    val bytes = mbVal * 1024 * 1024
    val kb = mbVal * 1024
    val gb = mbVal / 1024
    val tb = mbVal / (1024 * 1024)

    GlassCard(shape = RoundedCornerShape(18.dp)) {
        Text(text = "STORAGE & DATA SIZE CONVERTER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = inputMbText,
            onValueChange = { inputMbText = it },
            placeholder = { Text("Enter size in Megabytes (MB)", color = TextMuted) },
            label = { Text("Size in Megabytes (MB)", color = NeonCyan) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StorageUnitRow(label = "Bytes (B)", value = String.format("%,.0f B", bytes))
            StorageUnitRow(label = "Kilobytes (KB)", value = String.format("%,.2f KB", kb))
            StorageUnitRow(label = "Gigabytes (GB)", value = String.format("%.3f GB", gb))
            StorageUnitRow(label = "Terabytes (TB)", value = String.format("%.6f TB", tb))
        }
    }
}

@Composable
fun StorageUnitRow(label: String, value: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText(label, value))
                Toast.makeText(context, "$value copied!", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
    }
}

@Composable
fun FileVaultToolView(
    files: List<FileRecordEntity>,
    onAttachFileUri: (Uri) -> Unit,
    onDeleteFile: (FileRecordEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var fileToPreview by remember { mutableStateOf<FileRecordEntity?>(null) }
    var textPreviewContent by remember { mutableStateOf<String?>(null) }
    var previewType by remember { mutableStateOf("INSPECT") } // IMAGE, TEXT, INSPECT

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onAttachFileUri(it)
            Toast.makeText(context, "File attached to Vault!", Toast.LENGTH_SHORT).show()
        }
    }

    val totalSizeBytes = remember(files) { files.sumOf { it.sizeBytes } }
    val totalSizeMb = String.format("%.2f MB", totalSizeBytes.toDouble() / (1024 * 1024))

    val categories = listOf("All", "Images 📸", "Docs 📄", "Archives 📦", "APKs 🤖", "Media 🎵", "Others 📁")

    val filteredFiles = remember(files, selectedCategory) {
        files.filter { file ->
            val ext = file.name.substringAfterLast('.', "").lowercase()
            val mime = file.mimeType.lowercase()
            when (selectedCategory) {
                "Images 📸" -> mime.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
                "Docs 📄" -> mime.contains("pdf") || mime.contains("text") || ext in listOf("txt", "pdf", "doc", "docx", "csv", "json", "md")
                "Archives 📦" -> mime.contains("zip") || mime.contains("rar") || ext in listOf("zip", "rar", "7z", "tar", "gz")
                "APKs 🤖" -> mime.contains("vnd.android.package-archive") || ext == "apk"
                "Media 🎵" -> mime.startsWith("audio/") || mime.startsWith("video/") || ext in listOf("mp3", "mp4", "wav", "m4a", "mkv")
                "Others 📁" -> true
                else -> true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GlassCard(shape = RoundedCornerShape(18.dp), backgroundColor = DarkSurfaceElevated) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "CATEGORIZED FILE VAULT", fontSize = 11.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    Text(text = "${files.size} Total Files • $totalSizeMb Used", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Images, Text, ZIP, APKs & Documents auto-synced", fontSize = 11.sp, color = TextSecondary)
                }

                Button(
                    onClick = { fileLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) ElectricBlue else DarkSurface)
                        .border(1.dp, if (isSelected) NeonCyan else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files List
        if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No files found in $selectedCategory", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tap 'Upload' above to store any image, document, ZIP or APK", color = TextMuted, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredFiles, key = { it.id }) { fileRecord ->
                    val ext = fileRecord.name.substringAfterLast('.', "").lowercase()
                    val isImg = fileRecord.mimeType.startsWith("image/") || ext in listOf("jpg", "jpeg", "png", "webp")
                    val isText = fileRecord.mimeType.contains("text") || ext in listOf("txt", "json", "csv", "md", "log")
                    val isApk = ext == "apk" || fileRecord.mimeType.contains("package-archive")
                    val isZip = ext in listOf("zip", "rar", "7z", "tar")

                    val icon = when {
                        isImg -> Icons.Default.Image
                        isText -> Icons.Default.Article
                        isApk -> Icons.Default.Android
                        isZip -> Icons.Default.FolderZip
                        else -> Icons.Default.InsertDriveFile
                    }

                    val iconTint = when {
                        isImg -> NeonCyan
                        isText -> ElectricBlue
                        isApk -> NeonEmerald
                        isZip -> NeonPurple
                        else -> TextSecondary
                    }

                    val formattedSize = remember(fileRecord.sizeBytes) {
                        val bytes = fileRecord.sizeBytes
                        when {
                            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
                            bytes >= 1024 -> String.format("%d KB", bytes / 1024)
                            else -> "$bytes Bytes"
                        }
                    }

                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = DarkSurfaceElevated,
                        onClick = {
                            fileToPreview = fileRecord
                            when {
                                isImg -> previewType = "IMAGE"
                                isText -> {
                                    previewType = "TEXT"
                                    try {
                                        val localFile = File(context.filesDir, ".cloud_backup/attachments/${fileRecord.name}")
                                        textPreviewContent = if (localFile.exists()) localFile.readText() else "File content preview..."
                                    } catch (e: Exception) {
                                        textPreviewContent = "Could not load text file preview: ${e.message}"
                                    }
                                }
                                else -> previewType = "INSPECT"
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(iconTint.copy(alpha = 0.15f))
                                        .border(1.dp, iconTint.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileRecord.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(text = formattedSize, fontSize = 11.sp, color = NeonCyan)
                                        Text(text = "• ${fileRecord.folder}", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val file = File(context.filesDir, ".cloud_backup/attachments/${fileRecord.name}")
                                            if (file.exists()) {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = fileRecord.mimeType.ifBlank { "*/*" }
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Share/Export File"))
                                            } else {
                                                Toast.makeText(context, "Sharing file details: ${fileRecord.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Exporting ${fileRecord.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share/Export", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteFile(fileRecord) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = PriorityCritical, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // File Preview / Inspection Dialog
    fileToPreview?.let { fileRecord ->
        Dialog(onDismissRequest = { fileToPreview = null }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "FILE PREVIEW & VAULT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                        IconButton(onClick = { fileToPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (previewType == "IMAGE") {
                        val localFile = File(context.filesDir, ".cloud_backup/attachments/${fileRecord.name}")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (localFile.exists()) {
                                AsyncImage(
                                    model = localFile,
                                    contentDescription = fileRecord.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(48.dp))
                                    Text(text = fileRecord.name, color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    } else if (previewType == "TEXT") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "TEXT CONTENT", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                    QuickCopyButton(textToCopy = textPreviewContent ?: "")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = textPreviewContent ?: "No content",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary
                                )
                            }
                        }
                    } else {
                        // INSPECT
                        GlassCard(shape = RoundedCornerShape(14.dp), backgroundColor = DarkSurface) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "File Name: ${fileRecord.name}", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                Text(text = "Type: ${fileRecord.mimeType}", color = TextSecondary, fontSize = 12.sp)
                                Text(text = "Folder: ${fileRecord.folder}", color = NeonCyan, fontSize = 12.sp)
                                Text(text = "Sync Status: ${fileRecord.syncStatus}", color = NeonEmerald, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onDeleteFile(fileRecord)
                                fileToPreview = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PriorityCritical.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PriorityCritical),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete File", color = PriorityCritical, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { fileToPreview = null },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
