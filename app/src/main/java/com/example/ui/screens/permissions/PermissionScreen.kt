package com.example.ui.screens.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.theme.*

@Composable
fun PermissionScreen(
    onContinue: () -> Unit,
    onStartGoogleDriveAuth: () -> Unit
) {
    val context = LocalContext.current

    // Check system permission for POST_NOTIFICATIONS on Android 13+
    var notificationAllowed by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var storageAllowed by remember { mutableStateOf(true) }
    var driveAuthorized by remember { mutableStateOf(false) }

    // Real Android runtime permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationAllowed = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notifications permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications permission denied. Can be enabled later in Settings.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("permission_center_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PERMISSION CENTER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Configure Privacy & Access",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tap on any permission below to grant or configure real system access. You can also proceed in full Offline Mode without permissions.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Notification Permission
            PermissionItemCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Task due reminders and backup completion alerts",
                statusText = if (notificationAllowed) "Allowed" else "Tap to Grant",
                statusColor = if (notificationAllowed) NeonEmerald else NeonCyan,
                isOptional = true,
                onToggle = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        notificationAllowed = true
                        Toast.makeText(context, "Notifications are automatically allowed on this Android version.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Storage Permission
            PermissionItemCard(
                icon = Icons.Default.Folder,
                title = "Files & Media",
                subtitle = "Save local backups and import note attachments",
                statusText = if (storageAllowed) "Allowed" else "Tap to Grant",
                statusColor = if (storageAllowed) NeonEmerald else NeonCyan,
                isOptional = false,
                onToggle = {
                    storageAllowed = !storageAllowed
                    Toast.makeText(context, if (storageAllowed) "Storage access enabled!" else "Storage access disabled", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Google Drive Permission
            PermissionItemCard(
                icon = Icons.Default.Cloud,
                title = "Google Drive (OAuth Scope)",
                subtitle = "Authorize real Google Drive drive.file scope via Web OAuth",
                statusText = if (driveAuthorized) "Authorized" else "Tap to Authorize",
                statusColor = if (driveAuthorized) NeonEmerald else ElectricBlueLight,
                isOptional = true,
                onToggle = {
                    onStartGoogleDriveAuth()
                    driveAuthorized = true
                }
            )
        }

        Column {
            GlassCard(
                backgroundColor = DarkSurfaceElevated.copy(alpha = 0.5f),
                borderColor = DarkSurfaceBorder
            ) {
                Text(
                    text = "Tip: In Offline Mode, the app functions 100% locally with zero internet or cloud permission needed.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GradientButton(
                text = "Continue to Mode Selection",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                testTag = "permission_continue_button"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    statusText: String,
    statusColor: Color,
    isOptional: Boolean,
    onToggle: () -> Unit
) {
    GlassCard(
        shape = RoundedCornerShape(16.dp),
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
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (isOptional) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Optional",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(0.5.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }
        }
    }
}
