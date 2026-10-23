package com.example.ui.screens.security

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AppLockScreen(
    correctPin: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun vibrate(durationMs: Long = 40) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(durationMs)
        }
    }

    fun onDigitPress(digit: String) {
        if (enteredPin.length < 4) {
            vibrate(30)
            val newPin = enteredPin + digit
            enteredPin = newPin
            isError = false
            if (newPin.length == 4) {
                if (newPin == correctPin) {
                    vibrate(60)
                    onUnlocked()
                } else {
                    vibrate(150)
                    isError = true
                    enteredPin = ""
                }
            }
        }
    }

    fun onDeletePress() {
        if (enteredPin.isNotEmpty()) {
            vibrate(25)
            enteredPin = enteredPin.dropLast(1)
            isError = false
        }
    }

    fun triggerBiometricAuth() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(context)
                    .setTitle("Unlock Cloud Notebook")
                    .setSubtitle("Fingerprint or Biometric Credential")
                    .setDescription("Verify your fingerprint to unlock your workspace")
                    .setNegativeButton("Use PIN", context.mainExecutor) { _, _ -> }
                    .build()

                val cancelSignal = android.os.CancellationSignal()
                prompt.authenticate(
                    cancelSignal,
                    context.mainExecutor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            vibrate(50)
                            onUnlocked()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            vibrate(120)
                            isError = true
                            android.widget.Toast.makeText(context, "Fingerprint not recognized. Try again or enter PIN.", android.widget.Toast.LENGTH_SHORT).show()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            if (errorCode != android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                                errorCode != android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                                android.widget.Toast.makeText(context, "$errString", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else {
                android.widget.Toast.makeText(context, "Biometric requires Android 9+. Please enter PIN.", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Biometric not configured or hardware not present, gracefully ask for PIN
        }
    }

    LaunchedEffect(Unit) {
        triggerBiometricAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DeepNavy, DarkSurface, Color(0xFF060B14))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lock Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
                    .border(1.5.dp, if (isError) PriorityCritical else NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = if (isError) PriorityCritical else NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "WORKSPACE LOCKED",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isError) PriorityCritical else NeonCyan,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isError) "Incorrect PIN. Try again." else "Enter your 4-digit PIN to continue",
                fontSize = 14.sp,
                color = if (isError) PriorityCritical else TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isError -> PriorityCritical
                                    filled -> NeonCyan
                                    else -> DarkSurfaceElevated
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isError -> PriorityCritical
                                    filled -> NeonCyan
                                    else -> DarkSurfaceBorder
                                },
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Keypad (1 to 9, Biometric, 0, Backspace)
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keypad.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                vibrate(30)
                                                triggerBiometricAuth()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                "DEL" -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable { onDeletePress() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Delete",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .background(DarkSurfaceElevated)
                                            .border(1.dp, DarkSurfaceBorder, CircleShape)
                                            .clickable { onDigitPress(key) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
