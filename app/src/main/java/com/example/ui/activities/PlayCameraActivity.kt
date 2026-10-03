package com.example.ui.activities

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TinyAudioPlayer
import com.example.domain.ChildAge
import com.example.utils.HapticFeedbackUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

private data class PhotoCard(
    val title: String,
    val icon: ImageVector,
    val bgColor: Color,
    val rotation: Float
)

private val StickerPhotos = listOf(
    PhotoCard("Happy Puppy", Icons.Rounded.Pets, Color(0xFFFFD166), -4f),
    PhotoCard("Sunny Day", Icons.Rounded.WbSunny, Color(0xFFFF9F1C), 3f),
    PhotoCard("Sparkle Star", Icons.Rounded.Star, Color(0xFF06D6A0), -6f),
    PhotoCard("Sweet Heart", Icons.Rounded.Favorite, Color(0xFFEF476F), 5f),
    PhotoCard("Little Bear", Icons.Rounded.Camera, Color(0xFF118AB2), -3f)
)

@Composable
fun PlayCameraActivity(
    audioPlayer: TinyAudioPlayer,
    hapticUtil: HapticFeedbackUtil,
    childAge: ChildAge,
    isWindingDown: Boolean,
    modifier: Modifier = Modifier
) {
    var showFlash by remember { mutableStateOf(false) }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }
    var photoCardVisible by remember { mutableStateOf(false) }
    var photoOffsetX by remember { mutableStateOf(0f) }
    var photoOffsetY by remember { mutableStateOf(0f) }
    val shutterScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun takePretendPhoto() {
        audioPlayer.play("camera")
        hapticUtil.playLightPop()

        scope.launch {
            shutterScale.animateTo(0.85f, animationSpec = tween(60))
            shutterScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }

        // Soft gentle flash
        showFlash = true
        scope.launch {
            delay(120)
            showFlash = false
        }

        // Generate instant photo card
        photoCardVisible = false
        currentPhotoIndex = Random.nextInt(StickerPhotos.size)
        photoOffsetX = 0f
        photoOffsetY = 0f
        scope.launch {
            delay(180)
            photoCardVisible = true
            audioPlayer.play("pop")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isWindingDown) Color(0xFF354458) else Color(0xFF4A5568),
                        if (isWindingDown) Color(0xFF1F2836) else Color(0xFF2D3748)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Toy Camera Body
        Box(
            modifier = Modifier
                .size(340.dp, 440.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFB703), Color(0xFFFB8500))
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // Top stripe with toy flash & sensor
                Box(
                    modifier = Modifier
                        .size(width = 280.dp, height = 48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFE07A00)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFE8A3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashOn,
                            contentDescription = "Toy Flash",
                            tint = Color(0xFFFB8500),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Giant Camera Lens (Touchable)
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2D42))
                        .pointerInput(Unit) {
                            detectTapGestures { takePretendPhoto() }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Lens Rings
                    Box(
                        modifier = Modifier
                            .size(175.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8D99AE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(135.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF0077B6), Color(0xFF023E8A))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Lens reflection shine
                            Canvas(modifier = Modifier.size(110.dp)) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.5f),
                                    radius = 24.dp.toPx(),
                                    center = Offset(32.dp.toPx(), 32.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.25f),
                                    radius = 12.dp.toPx(),
                                    center = Offset(74.dp.toPx(), 74.dp.toPx())
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Giant Shutter Button
                Box(
                    modifier = Modifier
                        .scale(shutterScale.value)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF476F))
                        .pointerInput(Unit) {
                            detectTapGestures { takePretendPhoto() }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF7096))
                    )
                }
            }
        }

        // Emerging Instant Photo Card (Pretend photo)
        val photo = StickerPhotos[currentPhotoIndex]
        AnimatedVisibility(
            visible = photoCardVisible,
            enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                modifier = Modifier
                    .padding(bottom = 50.dp)
                    .size(width = 220.dp, height = 270.dp)
                    .offset { IntOffset(photoOffsetX.roundToInt(), photoOffsetY.roundToInt()) }
                    .rotate(photo.rotation)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            photoOffsetX += dragAmount.x
                            photoOffsetY += dragAmount.y
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            audioPlayer.play("pop")
                            hapticUtil.playSoftClick()
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 192.dp, height = 180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(photo.bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = photo.icon,
                            contentDescription = photo.title,
                            tint = Color.White,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = photo.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2B2D42),
                        fontSize = 17.sp
                    )
                }
            }
        }

        // Soft Screen Flash Overlay
        if (showFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDDFFFDF7))
            )
        }
    }
}
