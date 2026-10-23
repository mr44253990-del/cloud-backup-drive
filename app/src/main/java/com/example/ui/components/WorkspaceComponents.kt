package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AnimatedGlowGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    backgroundColor: Color = DarkSurfaceElevated,
    glowColors: List<Color> = listOf(NeonCyan, ElectricBlue, Color(0xFF7C4DFF), NeonCyan),
    borderWidth: Dp = 1.5.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowBorder")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowAngle"
    )

    val clickableModifier = if (onClick != null) {
        modifier.clip(shape).clickable { onClick() }
    } else {
        modifier.clip(shape)
    }

    Column(
        modifier = clickableModifier
            .background(backgroundColor, shape)
            .border(
                borderWidth,
                Brush.sweepGradient(
                    colors = glowColors
                ),
                shape
            )
            .padding(18.dp),
        content = content
    )
}

@Composable
fun QuickCopyButton(
    textToCopy: String,
    label: String = "Copy",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(1800)
            isCopied = false
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCopied) NeonEmerald.copy(alpha = 0.2f) else DarkSurface)
            .border(
                0.5.dp,
                if (isCopied) NeonEmerald else DarkSurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable {
                val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Copied Content", textToCopy))
                isCopied = true
                android.widget.Toast.makeText(context, "Copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                contentDescription = label,
                tint = if (isCopied) NeonEmerald else NeonCyan,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isCopied) "Copied!" else label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCopied) NeonEmerald else NeonCyan
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = DarkSurfaceGlass,
    borderColor: Color = DarkSurfaceBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable { onClick() }
    } else {
        modifier.clip(shape)
    }

    Column(
        modifier = clickableModifier
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StorageCircularRing(
    usedGb: Float,
    totalGb: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = NeonCyan,
    secondaryColor: Color = ElectricBlue,
    trackColor: Color = DarkSurfaceElevated
) {
    val targetProgress = (usedGb / totalGb).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "storageProgress"
    )

    val percentage = (targetProgress * 100).toInt()
    val availableGb = (totalGb - usedGb).coerceAtLeast(0f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(170.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 14.dp.toPx()
            // Track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Progress arc with gradient
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to primaryColor,
                    0.7f to secondaryColor,
                    1.0f to primaryColor
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$percentage%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = String.format("%.1f GB", availableGb),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeonCyan
            )
            Text(
                text = "AVAILABLE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (bgColor, textColor) = when (priority) {
        "Critical" -> PriorityCritical.copy(alpha = 0.2f) to PriorityCritical
        "Urgent" -> PriorityUrgent.copy(alpha = 0.2f) to PriorityUrgent
        "Important" -> PriorityImportant.copy(alpha = 0.2f) to ElectricBlueLight
        else -> PriorityNormal.copy(alpha = 0.2f) to NeonCyan
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = priority,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun SyncStatusChip(syncStatus: String) {
    val isSynced = syncStatus == "SYNCED"
    val color = if (isSynced) NeonEmerald else NeonAmber
    val text = if (isSynced) "Synced" else "Local / Pending"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    gradient: Brush = Brush.horizontalGradient(listOf(ElectricBlue, NeonCyanMuted)),
    testTag: String = "gradient_button"
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBar(
    userName: String,
    userAvatarUrl: String = "",
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isCloudMode: Boolean
) {
    Surface(
        color = DeepNavy,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(ElectricBlue, NeonCyan)))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Backup Logo",
                        tint = DeepNavy,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CLOUD BACKUP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isCloudMode) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceElevated)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isCloudMode) "DRIVE" else "OFFLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloudMode) NeonCyan else TextSecondary
                            )
                        }
                    }
                    Text(
                        text = "Notebook • Your Notes Always Safe",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("top_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search notes, tasks, files",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onProfileClick() }
                        .testTag("user_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(2.dp, if (isCloudMode) NeonCyan else DarkSurfaceBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        var isImageError by remember(userAvatarUrl) { mutableStateOf(false) }
                        if (userAvatarUrl.isNotBlank() && !isImageError) {
                            AsyncImage(
                                model = userAvatarUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                onError = { isImageError = true }
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase().ifBlank { "U" },
                                fontWeight = FontWeight.Bold,
                                color = if (isCloudMode) NeonCyan else TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Green/Cyan sync status dot on the bottom right
                    if (isCloudMode) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(NeonEmerald)
                                .border(1.5.dp, DeepNavy, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

enum class NavigationTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.Home),
    NOTES("Notes", Icons.Default.Description),
    TASKS("Tasks", Icons.Default.CheckCircle),
    MONEY("Money", Icons.Default.AccountBalanceWallet),
    TOOLS("Tools", Icons.Default.AutoAwesome)
}

@Composable
fun WorkspaceBottomNav(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceGlass,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(DarkSurfaceBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                val iconTint = if (isSelected) NeonCyan else TextMuted
                val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = textWeight,
                        color = iconTint
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(width = 16.dp, height = 2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(NeonCyan)
                        )
                    }
                }
            }
        }
    }
}
