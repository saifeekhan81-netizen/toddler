package com.example.ui.activities

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import com.example.audio.TinyAudioPlayer
import com.example.domain.ChildAge
import com.example.utils.HapticFeedbackUtil
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class RainDrop(
    var x: Float,
    var y: Float,
    var vy: Float,
    var alpha: Float = 0.8f
)

private data class FallingApple(
    var x: Float,
    var y: Float,
    var vy: Float,
    var bounceCount: Int = 0,
    var alpha: Float = 1f
)

@Composable
fun LittleWorldActivity(
    audioPlayer: TinyAudioPlayer,
    hapticUtil: HapticFeedbackUtil,
    childAge: ChildAge,
    isWindingDown: Boolean,
    modifier: Modifier = Modifier
) {
    // Interactive Elements State
    var sunRotation by remember { mutableFloatStateOf(0f) }
    var sunPulse by remember { mutableFloatStateOf(1f) }

    var cloudRainTimer by remember { mutableFloatStateOf(0f) }
    val rainDrops = remember { mutableStateListOf<RainDrop>() }

    var carX by remember { mutableFloatStateOf(150f) }
    var carSpeed by remember { mutableFloatStateOf(0f) }
    var carWheelRot by remember { mutableFloatStateOf(0f) }

    var birdX by remember { mutableFloatStateOf(300f) }
    var birdY by remember { mutableFloatStateOf(260f) }
    var birdSpeedX by remember { mutableFloatStateOf(0f) }
    var birdFlap by remember { mutableFloatStateOf(0f) }

    var treeSway by remember { mutableFloatStateOf(0f) }
    val apples = remember { mutableStateListOf<FallingApple>() }

    var ballX by remember { mutableFloatStateOf(500f) }
    var ballY by remember { mutableFloatStateOf(1600f) }
    var ballVx by remember { mutableFloatStateOf(0f) }
    var ballVy by remember { mutableFloatStateOf(0f) }
    var isBallDragging by remember { mutableStateOf(false) }

    var lastFrameTime by remember { mutableLongStateOf(0L) }

    // Physics & Animation Loop
    LaunchedEffect(Unit) {
        while (true) {
            val frameTimeNanos = awaitFrame()
            if (lastFrameTime == 0L) {
                lastFrameTime = frameTimeNanos
                continue
            }
            val dt = ((frameTimeNanos - lastFrameTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
            lastFrameTime = frameTimeNanos

            // Sun subtle rotation
            sunRotation += 12f * dt
            if (sunPulse > 1f) {
                sunPulse = (sunPulse - dt * 1.5f).coerceAtLeast(1f)
            }

            // Cloud Rain
            if (cloudRainTimer > 0f) {
                cloudRainTimer -= dt
                if (Random.nextFloat() < 0.45f) {
                    rainDrops.add(
                        RainDrop(
                            x = 240f + (Random.nextFloat() * 220f - 110f),
                            y = 350f,
                            vy = Random.nextFloat() * 600f + 450f
                        )
                    )
                }
            }
            val rainIter = rainDrops.iterator()
            while (rainIter.hasNext()) {
                val drop = rainIter.next()
                drop.y += drop.vy * dt
                if (drop.y > 1700f) drop.alpha -= 0.15f
                if (drop.alpha <= 0f || drop.y > 1850f) rainIter.remove()
            }

            // Car Movement
            if (carSpeed > 0f) {
                carX += carSpeed * dt
                carWheelRot += carSpeed * dt * 2.5f
                carSpeed = (carSpeed - dt * 120f).coerceAtLeast(0f)
                if (carX > 1200f) {
                    carX = -250f
                }
            }

            // Bird Flight
            if (birdSpeedX != 0f) {
                birdX += birdSpeedX * dt
                birdFlap += dt * 18f
                birdY += sin(birdFlap) * 1.5f
                if (birdX > 1200f) {
                    birdX = -100f
                }
                if (birdX in 280f..320f && birdSpeedX > 0f && Random.nextFloat() < 0.05f) {
                    birdSpeedX = 0f
                }
            }

            // Tree Sway decay
            if (treeSway != 0f) {
                treeSway *= 0.94f
                if (kotlin.math.abs(treeSway) < 0.1f) treeSway = 0f
            }

            // Falling Apples
            val appleIter = apples.iterator()
            while (appleIter.hasNext()) {
                val a = appleIter.next()
                a.vy += 850f * dt // Gravity
                a.y += a.vy * dt
                val groundY = 1680f
                if (a.y >= groundY) {
                    a.y = groundY
                    if (a.bounceCount < 2) {
                        a.vy = -a.vy * 0.45f
                        a.bounceCount++
                    } else {
                        a.vy = 0f
                        a.alpha -= dt * 0.5f
                    }
                }
                if (a.alpha <= 0f) appleIter.remove()
            }

            // Ball Physics
            if (!isBallDragging) {
                ballVy += 980f * dt // Gravity
                ballX += ballVx * dt
                ballY += ballVy * dt
                ballVx *= 0.985f // Friction

                val groundY = 1660f
                if (ballY > groundY) {
                    ballY = groundY
                    ballVy = -ballVy * 0.65f
                    if (kotlin.math.abs(ballVy) < 40f) ballVy = 0f
                }
                if (ballX < 80f) {
                    ballX = 80f
                    ballVx = -ballVx * 0.7f
                } else if (ballX > 980f) {
                    ballX = 980f
                    ballVx = -ballVx * 0.7f
                }
            }
        }
    }

    // Touch Interaction Handlers
    fun onSunTap() {
        sunPulse = 1.4f
        audioPlayer.play("sparkle")
        hapticUtil.playLightPop()
    }

    fun onCloudTap() {
        cloudRainTimer = 3.5f
        audioPlayer.play("rain")
        hapticUtil.playSoftClick()
    }

    fun onBirdTap() {
        birdSpeedX = 320f
        audioPlayer.play("bird")
        hapticUtil.playSoftClick()
    }

    fun onTreeTap() {
        treeSway = 14f
        audioPlayer.play("apple")
        hapticUtil.playSoftClick()
        if (apples.size < 4) {
            apples.add(FallingApple(x = 810f + Random.nextFloat() * 60f - 30f, y = 1080f, vy = 0f))
        }
    }

    fun onCarTap() {
        carSpeed = 460f
        audioPlayer.play("car")
        hapticUtil.playLightPop()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(childAge) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tx = down.position.x
                        val ty = down.position.y

                        // Sun hit area (top right)
                        if (sqrt((tx - 860f) * (tx - 860f) + (ty - 220f) * (ty - 220f)) < 150f) {
                            onSunTap()
                        }
                        // Cloud hit area (top left)
                        else if (sqrt((tx - 240f) * (tx - 240f) + (ty - 260f) * (ty - 260f)) < 170f) {
                            onCloudTap()
                        }
                        // Bird hit area
                        else if (sqrt((tx - birdX) * (tx - birdX) + (ty - birdY) * (ty - birdY)) < 130f) {
                            onBirdTap()
                        }
                        // Tree hit area (right middle)
                        else if (tx in 650f..1000f && ty in 900f..1500f) {
                            onTreeTap()
                        }
                        // Car hit area (bottom road)
                        else if (tx in (carX - 80f)..(carX + 220f) && ty in 1680f..1880f) {
                            onCarTap()
                        }
                        // Ball hit area (generous drag target)
                        else if (sqrt((tx - ballX) * (tx - ballX) + (ty - ballY) * (ty - ballY)) < 120f) {
                            isBallDragging = true
                            ballVx = 0f
                            ballVy = 0f
                        }

                        do {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (isBallDragging && change.pressed) {
                                    val px = change.position.x
                                    val py = change.position.y
                                    ballVx = (px - ballX) * 15f
                                    ballVy = (py - ballY) * 15f
                                    ballX = px
                                    ballY = py.coerceAtMost(1660f)
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (isBallDragging) {
                            isBallDragging = false
                            audioPlayer.play("pop")
                            hapticUtil.playSoftClick()
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // Sky Gradient
            val skyTop = if (isWindingDown) Color(0xFFD6E3F8) else Color(0xFFC7E6FF)
            val skyBottom = if (isWindingDown) Color(0xFFF0EBE3) else Color(0xFFEBF7FF)
            drawRect(
                brush = Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = h * 0.7f),
                size = Size(w, h * 0.7f)
            )

            // Rolling Hills / Grass Landscape
            val hillBrush = Brush.verticalGradient(listOf(Color(0xFF8FD5A6), Color(0xFF67B982)))
            val hillPath = Path().apply {
                moveTo(0f, h * 0.62f)
                cubicTo(w * 0.35f, h * 0.58f, w * 0.7f, h * 0.65f, w, h * 0.61f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillPath, brush = hillBrush)

            // Road for the car
            val roadBrush = Brush.verticalGradient(listOf(Color(0xFF867E78), Color(0xFF6D6560)))
            drawRect(
                brush = roadBrush,
                topLeft = Offset(0f, 1720f),
                size = Size(w, 140f)
            )
            // Road dashes
            for (dashX in 0..1200 step 120) {
                drawRoundRect(
                    color = Color(0xFFEDE9E3),
                    topLeft = Offset(dashX.toFloat(), 1785f),
                    size = Size(60f, 10f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            // 1. Sun (interactive)
            drawSun(Offset(860f, 220f), 65f * sunPulse, sunRotation)

            // 2. Cloud (interactive)
            drawCloud(Offset(240f, 260f), 80f)

            // Raindrops
            for (drop in rainDrops) {
                drawLine(
                    color = Color(0xFF4EA8DE).copy(alpha = drop.alpha),
                    start = Offset(drop.x, drop.y),
                    end = Offset(drop.x - 3f, drop.y + 16f),
                    strokeWidth = 4f
                )
            }

            // 3. Tree (interactive)
            drawTree(Offset(820f, 1380f), treeSway)

            // Apples
            for (apple in apples) {
                drawCircle(color = Color(0xFFE63946).copy(alpha = apple.alpha), radius = 18f, center = Offset(apple.x, apple.y))
                drawCircle(color = Color.White.copy(alpha = 0.6f * apple.alpha), radius = 5f, center = Offset(apple.x - 5f, apple.y - 5f))
            }

            // 4. Bird (interactive)
            drawBird(Offset(birdX, birdY), birdFlap)

            // 5. Car (interactive)
            drawCar(Offset(carX, 1740f), carWheelRot)

            // 6. Ball (interactive drag)
            drawBall(Offset(ballX, ballY), 50f)
        }
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float, rotationDeg: Float) {
    // Sun Rays
    rotate(rotationDeg, pivot = center) {
        val rayCount = 10
        for (i in 0 until rayCount) {
            val angle = (i * 2 * Math.PI / rayCount).toFloat()
            val startX = center.x + cos(angle) * (radius * 1.05f)
            val startY = center.y + sin(angle) * (radius * 1.05f)
            val endX = center.x + cos(angle) * (radius * 1.55f)
            val endY = center.y + sin(angle) * (radius * 1.55f)
            drawLine(
                color = Color(0xFFFFB703),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 10f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
    // Sun Body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD166), Color(0xFFFFB703)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    // Smiling Face
    drawCircle(Color(0xFF6B4500), radius = 5f, center = Offset(center.x - 18f, center.y - 8f))
    drawCircle(Color(0xFF6B4500), radius = 5f, center = Offset(center.x + 18f, center.y - 8f))
    drawCircle(Color(0x77FF6B6B), radius = 10f, center = Offset(center.x - 26f, center.y + 10f))
    drawCircle(Color(0x77FF6B6B), radius = 10f, center = Offset(center.x + 26f, center.y + 10f))
    // Smile Arc
    val smilePath = Path().apply {
        moveTo(center.x - 14f, center.y + 12f)
        quadraticTo(center.x, center.y + 26f, center.x + 14f, center.y + 12f)
    }
    drawPath(smilePath, color = Color(0xFF6B4500), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
}

private fun DrawScope.drawCloud(center: Offset, size: Float) {
    val cloudColor = Color.White
    drawCircle(color = cloudColor, radius = size * 0.55f, center = Offset(center.x - size * 0.5f, center.y + size * 0.1f))
    drawCircle(color = cloudColor, radius = size * 0.7f, center = Offset(center.x, center.y - size * 0.15f))
    drawCircle(color = cloudColor, radius = size * 0.55f, center = Offset(center.x + size * 0.55f, center.y + size * 0.1f))
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(center.x - size * 0.8f, center.y + size * 0.1f),
        size = Size(size * 1.6f, size * 0.5f),
        cornerRadius = CornerRadius(size * 0.25f, size * 0.25f)
    )
    // Happy cloud eyes
    drawCircle(Color(0xFF758A99), radius = 4f, center = Offset(center.x - 16f, center.y + 10f))
    drawCircle(Color(0xFF758A99), radius = 4f, center = Offset(center.x + 16f, center.y + 10f))
    drawCircle(Color(0x44FFAAA6), radius = 7f, center = Offset(center.x - 26f, center.y + 20f))
    drawCircle(Color(0x44FFAAA6), radius = 7f, center = Offset(center.x + 26f, center.y + 20f))
}

private fun DrawScope.drawTree(baseCenter: Offset, swayDeg: Float) {
    // Trunk
    drawRoundRect(
        color = Color(0xFF8B5E3C),
        topLeft = Offset(baseCenter.x - 24f, baseCenter.y - 280f),
        size = Size(48f, 300f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    // Foliage with sway
    rotate(swayDeg, pivot = Offset(baseCenter.x, baseCenter.y - 200f)) {
        val leafyCenter = Offset(baseCenter.x, baseCenter.y - 320f)
        drawCircle(color = Color(0xFF40916C), radius = 130f, center = leafyCenter)
        drawCircle(color = Color(0xFF52B788), radius = 95f, center = Offset(leafyCenter.x - 45f, leafyCenter.y - 30f))
        drawCircle(color = Color(0xFF74C69D), radius = 85f, center = Offset(leafyCenter.x + 40f, leafyCenter.y - 25f))

        // Fixed apples in tree
        drawCircle(color = Color(0xFFE63946), radius = 16f, center = Offset(leafyCenter.x - 50f, leafyCenter.y + 20f))
        drawCircle(color = Color(0xFFE63946), radius = 16f, center = Offset(leafyCenter.x + 45f, leafyCenter.y + 15f))
        drawCircle(color = Color(0xFFE63946), radius = 16f, center = Offset(leafyCenter.x, leafyCenter.y - 45f))
    }
}

private fun DrawScope.drawBird(pos: Offset, flap: Float) {
    val wingAngle = sin(flap) * 25f
    // Bird Body
    drawOval(
        color = Color(0xFF48CAE4),
        topLeft = Offset(pos.x - 30f, pos.y - 18f),
        size = Size(60f, 36f)
    )
    // Bird Head
    drawCircle(color = Color(0xFF00B4D8), radius = 18f, center = Offset(pos.x + 24f, pos.y - 10f))
    // Beak
    val beak = Path().apply {
        moveTo(pos.x + 38f, pos.y - 14f)
        lineTo(pos.x + 54f, pos.y - 10f)
        lineTo(pos.x + 38f, pos.y - 6f)
        close()
    }
    drawPath(beak, color = Color(0xFFF77F00))
    // Eye
    drawCircle(Color.White, radius = 5f, center = Offset(pos.x + 28f, pos.y - 12f))
    drawCircle(Color.Black, radius = 2.5f, center = Offset(pos.x + 29f, pos.y - 12f))
    // Wing
    rotate(wingAngle, pivot = Offset(pos.x - 5f, pos.y - 8f)) {
        val wing = Path().apply {
            moveTo(pos.x - 10f, pos.y - 8f)
            quadraticTo(pos.x - 15f, pos.y - 36f, pos.x - 38f, pos.y - 24f)
            close()
        }
        drawPath(wing, color = Color(0xFF0096C7))
    }
}

private fun DrawScope.drawCar(pos: Offset, wheelRot: Float) {
    // Car Body
    val body = Path().apply {
        moveTo(pos.x, pos.y + 40f)
        lineTo(pos.x + 190f, pos.y + 40f)
        quadraticTo(pos.x + 200f, pos.y + 20f, pos.x + 190f, pos.y)
        lineTo(pos.x + 140f, pos.y)
        lineTo(pos.x + 110f, pos.y - 45f)
        lineTo(pos.x + 40f, pos.y - 45f)
        lineTo(pos.x + 20f, pos.y)
        lineTo(pos.x, pos.y)
        close()
    }
    drawPath(body, color = Color(0xFFEF476F))

    // Car Windows
    val window = Path().apply {
        moveTo(pos.x + 46f, pos.y - 5f)
        lineTo(pos.x + 58f, pos.y - 38f)
        lineTo(pos.x + 104f, pos.y - 38f)
        lineTo(pos.x + 128f, pos.y - 5f)
        close()
    }
    drawPath(window, color = Color(0xFFCAF0F8))

    // Wheels
    drawWheel(Offset(pos.x + 45f, pos.y + 42f), 24f, wheelRot)
    drawWheel(Offset(pos.x + 155f, pos.y + 42f), 24f, wheelRot)
}

private fun DrawScope.drawWheel(center: Offset, radius: Float, rot: Float) {
    drawCircle(Color(0xFF2B2D42), radius = radius, center = center)
    drawCircle(Color(0xFF8D99AE), radius = radius * 0.55f, center = center)
    rotate(rot, pivot = center) {
        drawLine(Color.White, start = Offset(center.x - radius * 0.45f, center.y), end = Offset(center.x + radius * 0.45f, center.y), strokeWidth = 3f)
        drawLine(Color.White, start = Offset(center.x, center.y - radius * 0.45f), end = Offset(center.x, center.y + radius * 0.45f), strokeWidth = 3f)
    }
}

private fun DrawScope.drawBall(center: Offset, radius: Float) {
    // Drop Shadow
    drawOval(
        color = Color(0x33000000),
        topLeft = Offset(center.x - radius * 0.9f, center.y + radius * 0.7f),
        size = Size(radius * 1.8f, radius * 0.4f)
    )
    // Ball base
    drawCircle(color = Color(0xFFFFD166), radius = radius, center = center)
    // Colorful stripes
    drawCircle(color = Color(0xFFEF476F), radius = radius, center = Offset(center.x - radius * 0.5f, center.y))
    drawCircle(color = Color(0xFF118AB2), radius = radius, center = Offset(center.x + radius * 0.5f, center.y))
    // Highlight gleam
    drawCircle(Color.White.copy(alpha = 0.65f), radius = radius * 0.28f, center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f))
}
