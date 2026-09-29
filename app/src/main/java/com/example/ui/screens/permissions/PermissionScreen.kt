package com.example.ui.screens.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.theme.*

@Composable
fun PermissionScreen(
    onContinue: () -> Unit
) {
    var notificationAllowed by remember { mutableStateOf(true) }
    var storageAllowed by remember { mutableStateOf(true) }
    var driveAuthorized by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(24.dp)
            .testTag("permission_center_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PERMISSION CENTER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configure Privacy & Access",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cloud Backup respects your privacy. Grant permissions based on your preferred workspace mode.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Permission Cards
            PermissionItemCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Task due reminders and backup completion alerts",
                statusText = if (notificationAllowed) "Allowed" else "Not Granted",
                statusColor = if (notificationAllowed) NeonEmerald else TextMuted,
                isOptional = true,
                onToggle = { notificationAllowed = !notificationAllowed }
            )

            Spacer(modifier = Modifier.height(14.dp))

            PermissionItemCard(
                icon = Icons.Default.Folder,
                title = "Files & Media",
                subtitle = "Save local backups and import note attachments",
                statusText = if (storageAllowed) "Allowed" else "Not Granted",
                statusColor = if (storageAllowed) NeonEmerald else TextMuted,
                isOptional = false,
                onToggle = { storageAllowed = !storageAllowed }
            )

            Spacer(modifier = Modifier.height(14.dp))

            PermissionItemCard(
                icon = Icons.Default.Cloud,
                title = "Google Drive (OAuth Scope)",
                subtitle = "Separated authorization flow with safe drive.file scope",
                statusText = if (driveAuthorized) "Authorized" else "Cloud Mode Only",
                statusColor = if (driveAuthorized) NeonEmerald else NeonCyanMuted,
                isOptional = true,
                onToggle = { driveAuthorized = !driveAuthorized }
            )
        }

        Column {
            GlassCard(
                backgroundColor = DarkSurfaceElevated.copy(alpha = 0.5f),
                borderColor = DarkSurfaceBorder
            ) {
                Text(
                    text = "Tip: You can use the app completely in Offline Mode without any Google Account or internet permission.",
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
