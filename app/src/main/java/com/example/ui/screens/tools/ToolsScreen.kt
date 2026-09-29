package com.example.ui.screens.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entities.ClipboardEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.theme.*
import kotlin.random.Random

@Composable
fun ToolsScreen(
    clipboardList: List<ClipboardEntity>,
    onAddClipboard: (String, String) -> Unit,
    onDeleteClipboard: (ClipboardEntity) -> Unit,
    onClearClipboard: () -> Unit,
    onSaveAsNote: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedToolTab by remember { mutableStateOf("Clipboard") }
    val toolTabs = listOf("Clipboard", "Keytool & SHA", "Calculator", "Password Gen", "Text Counter")

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
                        text = "Smart Tools & Utilities",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Productivity essentials & Developer helpers",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // Content according to selected tab
            when (selectedToolTab) {
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
                Text(text = "GOOGLE OAUTH CLIENT CONFIGURATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricBlueLight)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Client ID:", fontSize = 11.sp, color = TextMuted)
                Text(text = clientId, fontSize = 12.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Drive OAuth Scope:", fontSize = 11.sp, color = TextMuted)
                Text(text = "https://www.googleapis.com/auth/drive.file", fontSize = 12.sp, color = NeonCyan, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("Client ID", clientId))
                        Toast.makeText(context, "Client ID copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy Client ID", color = TextPrimary)
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
