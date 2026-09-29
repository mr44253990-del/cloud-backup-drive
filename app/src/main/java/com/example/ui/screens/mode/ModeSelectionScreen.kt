package com.example.ui.screens.mode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
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
import com.example.ui.theme.*

@Composable
fun ModeSelectionScreen(
    onSelectMode: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(24.dp)
            .testTag("mode_selection_screen"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "WORKSPACE SETUP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose Your Mode",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select how you want to manage your notes and personal workspace. You can switch or migrate anytime.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card 1: OFFLINE MODE
            ModeCard(
                title = "OFFLINE",
                subtitle = "Local Workspace",
                description = "• No account required\n• Works without Internet\n• High-speed local Room Database\n• Can sync to Cloud anytime",
                icon = Icons.Default.Storage,
                accentColor = NeonCyan,
                gradient = Brush.linearGradient(listOf(Color(0xFF00363A), Color(0xFF081A2C))),
                onClick = { onSelectMode("OFFLINE") },
                testTag = "select_offline_mode"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: CLOUD MODE
            ModeCard(
                title = "CLOUD",
                subtitle = "Google Drive Connected",
                description = "• Google Account Integration\n• Dedicated 'Cloud Backup/' Drive folder\n• Automatic Multi-device Sync\n• Scoped drive.file security",
                icon = Icons.Default.CloudDone,
                accentColor = ElectricBlue,
                gradient = Brush.linearGradient(listOf(Color(0xFF0D2554), Color(0xFF13153A))),
                onClick = { onSelectMode("CLOUD") },
                testTag = "select_cloud_mode"
            )
        }

        Column {
            Text(
                text = "Rakib's Workspace • Client ID: 688078973753-q0j8243na8m0i5d0l7kvh4id9ckd21bt",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    gradient: Brush,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(20.dp)
            .testTag(testTag)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accentColor.copy(alpha = 0.25f))
                            .border(1.dp, accentColor, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "SELECT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
