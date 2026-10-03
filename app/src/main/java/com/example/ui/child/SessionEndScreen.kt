package com.example.ui.child

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionEndScreen(
    onRequestUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAnim = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        pulseAnim.animateTo(
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F1A2D), Color(0xFF060912))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Starry night canvas background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starPositions = listOf(
                Offset(size.width * 0.2f, size.height * 0.15f),
                Offset(size.width * 0.75f, size.height * 0.22f),
                Offset(size.width * 0.15f, size.height * 0.7f),
                Offset(size.width * 0.85f, size.height * 0.65f),
                Offset(size.width * 0.45f, size.height * 0.35f),
                Offset(size.width * 0.6f, size.height * 0.8f)
            )
            for (p in starPositions) {
                drawCircle(color = Color(0x99FFF9D2), radius = 3.5f, center = p)
                drawCircle(color = Color(0x33FFF9D2), radius = 9f, center = p)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Sleeping Moon Icon
            Box(
                modifier = Modifier
                    .scale(pulseAnim.value)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFE082)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = "Moon",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Play time is finished",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Parent: Hold 3 fingers for 3 seconds or tap below to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestUnlock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF37474F),
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier.testTag("parent_unlock_trigger_button")
            ) {
                Text(
                    text = "Parent Unlock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}
