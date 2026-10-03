package com.example.ui.child

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.security.ParentAuthManager
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun ParentExitGestureDetector(
    onTriggerUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var touchHoldTimeMs by remember { mutableLongStateOf(0L) }
    var touchCenter by remember { mutableStateOf(Offset.Zero) }
    var isHoldingThreeFingers by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableFloatStateOf(0f) }

    // Hold progress timer
    LaunchedEffect(isHoldingThreeFingers) {
        if (isHoldingThreeFingers) {
            val startTime = System.currentTimeMillis()
            while (isHoldingThreeFingers) {
                val elapsed = System.currentTimeMillis() - startTime
                touchHoldTimeMs = elapsed
                if (elapsed >= 1000L) {
                    // Show progress between 1.0s and 3.0s
                    progressFraction = ((elapsed - 1000f) / 2000f).coerceIn(0f, 1f)
                }
                if (elapsed >= 3000L) {
                    isHoldingThreeFingers = false
                    progressFraction = 0f
                    onTriggerUnlock()
                    break
                }
                delay(20)
            }
        } else {
            touchHoldTimeMs = 0L
            progressFraction = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.filter { it.pressed }

                        if (pressedPointers.size == 3) {
                            var sumX = 0f
                            var sumY = 0f
                            for (p in pressedPointers) {
                                sumX += p.position.x
                                sumY += p.position.y
                            }
                            val centroid = Offset(sumX / 3f, sumY / 3f)

                            if (!isHoldingThreeFingers) {
                                isHoldingThreeFingers = true
                                touchCenter = centroid
                            } else {
                                // Check if movement exceeded threshold
                                val dx = centroid.x - touchCenter.x
                                val dy = centroid.y - touchCenter.y
                                if (sqrt(dx * dx + dy * dy) > 85f) {
                                    // Movement too high, reset
                                    isHoldingThreeFingers = false
                                }
                            }
                        } else {
                            isHoldingThreeFingers = false
                        }
                    }
                }
            }
    ) {
        content()

        // Subtle parent indicator (only visible after 1.0s hold)
        if (isHoldingThreeFingers && touchHoldTimeMs >= 1000L) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { progressFraction },
                        color = Color.White,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(76.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hold for Parent Check…",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ParentCheckDialog(
    storedPinHash: String,
    storedPinSalt: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    onAddFiveMinutes: (() -> Unit)? = null
) {
    var enteredPin by remember { mutableStateOf("") }
    var isPinError by remember { mutableStateOf(false) }
    var isRecoveryMode by remember { mutableStateOf(false) }
    var recoveryChallenge by remember { mutableStateOf(ParentAuthManager.createRecoveryChallenge()) }
    var recoveryError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .testTag("parent_check_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Parent Gate",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isRecoveryMode) "Parent Verification" else "Parent Check",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isRecoveryMode) "Solve the calculation to exit safely" else "Enter your 4-digit PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!isRecoveryMode) {
                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..3) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isPinError -> MaterialTheme.colorScheme.error
                                            isFilled -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }

                    if (isPinError) {
                        Text(
                            text = "Incorrect PIN. Try again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Number Pad
                    PinPad(
                        onDigit = { digit ->
                            if (enteredPin.length < 4) {
                                isPinError = false
                                val newPin = enteredPin + digit
                                enteredPin = newPin
                                if (newPin.length == 4) {
                                    if (ParentAuthManager.verifyPin(newPin, storedPinHash, storedPinSalt)) {
                                        onSuccess()
                                    } else {
                                        isPinError = true
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onBackspace = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                                isPinError = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                isRecoveryMode = true
                                recoveryChallenge = ParentAuthManager.createRecoveryChallenge()
                            }
                        ) {
                            Text("Forgot PIN?")
                        }

                        if (onAddFiveMinutes != null) {
                            Button(
                                onClick = {
                                    if (ParentAuthManager.verifyPin(enteredPin, storedPinHash, storedPinSalt)) {
                                        onAddFiveMinutes()
                                    } else {
                                        // Require correct PIN or allow adding after PIN
                                        isPinError = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.testTag("add_time_button")
                            ) {
                                Text("+ 5 Min")
                            }
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Resume Play")
                        }
                    }
                } else {
                    // Secondary Adult Gate Challenge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = recoveryChallenge.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (recoveryError) {
                                Text(
                                    text = "Incorrect answer. Try this one:",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Option Buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (opt in recoveryChallenge.options) {
                            OutlinedButton(
                                onClick = {
                                    if (opt == recoveryChallenge.correctAnswer) {
                                        onSuccess()
                                    } else {
                                        recoveryError = true
                                        recoveryChallenge = ParentAuthManager.createRecoveryChallenge()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(opt.toString(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(onClick = { isRecoveryMode = false }) {
                        Text("Back to PIN")
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (item in row) {
                    when (item) {
                        "" -> {
                            Spacer(modifier = Modifier.size(68.dp))
                        }
                        "DEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable { onBackspace() }
                                    .testTag("pin_backspace"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onDigit(item) }
                                    .testTag("pin_digit_$item"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
