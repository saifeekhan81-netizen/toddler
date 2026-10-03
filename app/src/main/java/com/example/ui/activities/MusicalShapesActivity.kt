package com.example.ui.activities

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private enum class ShapeKind {
    CIRCLE, ROUND_SQUARE, TRIANGLE, STAR, HEART, OVAL
}

private class MusicalShape(
    val id: Int,
    val kind: ShapeKind,
    var x: Float,
    var y: Float,
    val size: Float,
    val baseColor: Color,
    val noteIndex: Int,
    var speedX: Float,
    var speedY: Float
) {
    val scaleAnim = Animatable(1f)
    var isBright = false
}

private data class SoundRing(
    val x: Float,
    val y: Float,
    val color: Color,
    var radius: Float = 10f,
    var alpha: Float = 0.8f
)

@Composable
fun MusicalShapesActivity(
    audioPlayer: TinyAudioPlayer,
    hapticUtil: HapticFeedbackUtil,
    childAge: ChildAge,
    isWindingDown: Boolean,
    modifier: Modifier = Modifier
) {
    val shapes = remember { mutableStateListOf<MusicalShape>() }
    val soundRings = remember { mutableStateListOf<SoundRing>() }
    val scope = rememberCoroutineScope()
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    val shapeCount = when (childAge) {
        ChildAge.ONE -> 4
        ChildAge.TWO -> 5
        ChildAge.THREE -> 6
    }

    val speedMult = when {
        isWindingDown -> 0.3f
        childAge == ChildAge.ONE -> 0.5f
        childAge == ChildAge.TWO -> 0.85f
        else -> 1.15f
    }

    // Animation Ticker
    LaunchedEffect(Unit) {
        while (true) {
            val frameTimeNanos = awaitFrame()
            if (lastFrameTime == 0L) {
                lastFrameTime = frameTimeNanos
                continue
            }
            val dt = ((frameTimeNanos - lastFrameTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
            lastFrameTime = frameTimeNanos

            // Update sound ripples
            val ringIter = soundRings.iterator()
            while (ringIter.hasNext()) {
                val ring = ringIter.next()
                ring.radius += 180f * dt
                ring.alpha -= 1.6f * dt
                if (ring.alpha <= 0f) ringIter.remove()
            }

            // Move shapes with gentle bounce against edges
            for (s in shapes) {
                s.x += s.speedX * speedMult * dt * 60f
                s.y += s.speedY * speedMult * dt * 60f

                // Bounce bounds
                if (s.x < s.size * 0.7f) {
                    s.x = s.size * 0.7f
                    s.speedX = kotlin.math.abs(s.speedX)
                } else if (s.x > 1000f) {
                    s.x = 1000f
                    s.speedX = -kotlin.math.abs(s.speedX)
                }
                if (s.y < s.size * 0.7f + 120f) {
                    s.y = s.size * 0.7f + 120f
                    s.speedY = kotlin.math.abs(s.speedY)
                } else if (s.y > 1950f) {
                    s.y = 1950f
                    s.speedY = -kotlin.math.abs(s.speedY)
                }
            }
        }
    }

    fun playShape(shape: MusicalShape) {
        audioPlayer.playNote(shape.noteIndex)
        hapticUtil.playSoftClick()

        soundRings.add(
            SoundRing(
                x = shape.x,
                y = shape.y,
                color = shape.baseColor
            )
        )

        scope.launch {
            shape.isBright = true
            shape.scaleAnim.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            shape.scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            shape.isBright = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(childAge) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchX = down.position.x
                        val touchY = down.position.y

                        val tolerance = if (childAge == ChildAge.ONE) 60f else 35f
                        val hit = shapes.findLast { s ->
                            val dx = s.x - touchX
                            val dy = s.y - touchY
                            sqrt(dx * dx + dy * dy) <= (s.size * 0.65f + tolerance)
                        }
                        hit?.let { playShape(it) }

                        // Multi-touch tracking
                        do {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (change.pressed) {
                                    val px = change.position.x
                                    val py = change.position.y
                                    val touchHit = shapes.findLast { s ->
                                        val dx = s.x - px
                                        val dy = s.y - py
                                        sqrt(dx * dx + dy * dy) <= (s.size * 0.65f + tolerance)
                                    }
                                    if (touchHit != null && !touchHit.scaleAnim.isRunning) {
                                        playShape(touchHit)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            if (shapes.isEmpty()) {
                val w = size.width
                val h = size.height
                val kinds = listOf(
                    ShapeKind.CIRCLE,
                    ShapeKind.ROUND_SQUARE,
                    ShapeKind.TRIANGLE,
                    ShapeKind.STAR,
                    ShapeKind.HEART,
                    ShapeKind.OVAL
                )
                val colors = listOf(
                    Color(0xFFFFB703), // Sun Gold
                    Color(0xFF06D6A0), // Bright Mint
                    Color(0xFF118AB2), // Ocean Blue
                    Color(0xFFEF476F), // Rose Coral
                    Color(0xFF8338EC), // Purple
                    Color(0xFFFF8FA3)  // Cotton Candy
                )
                val baseShapeSize = when (childAge) {
                    ChildAge.ONE -> 175f
                    ChildAge.TWO -> 150f
                    ChildAge.THREE -> 135f
                }

                // Grid layout with gentle spacing
                for (i in 0 until shapeCount) {
                    val col = i % 2
                    val row = i / 2
                    val px = (w * 0.3f) + (col * w * 0.42f) + (Random.nextFloat() * 40f - 20f)
                    val py = (h * 0.25f) + (row * h * 0.24f) + (Random.nextFloat() * 40f - 20f)
                    shapes.add(
                        MusicalShape(
                            id = i,
                            kind = kinds[i % kinds.size],
                            x = px,
                            y = py,
                            size = baseShapeSize,
                            baseColor = colors[i % colors.size],
                            noteIndex = i % 6,
                            speedX = (Random.nextFloat() - 0.5f) * 1.2f,
                            speedY = (Random.nextFloat() - 0.5f) * 1.2f
                        )
                    )
                }
            }

            // Warm canvas background
            val bgTop = if (isWindingDown) Color(0xFFF0EBE3) else Color(0xFFFFF9F2)
            val bgBottom = if (isWindingDown) Color(0xFFE4DDD4) else Color(0xFFFFF1E6)
            drawRect(brush = Brush.verticalGradient(listOf(bgTop, bgBottom)))

            // Draw sound ripples
            for (ring in soundRings) {
                drawCircle(
                    color = ring.color.copy(alpha = ring.alpha.coerceIn(0f, 1f)),
                    radius = ring.radius,
                    center = Offset(ring.x, ring.y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
                )
            }

            // Draw shapes
            for (s in shapes) {
                drawMusicalShape(s)
            }
        }
    }
}

private fun DrawScope.drawMusicalShape(s: MusicalShape) {
    val scaleVal = s.scaleAnim.value
    scale(scaleVal, pivot = Offset(s.x, s.y)) {
        val color = if (s.isBright) s.baseColor.copy(alpha = 0.95f) else s.baseColor
        val center = Offset(s.x, s.y)
        val half = s.size / 2f

        // Soft drop shadow
        drawCircle(
            color = Color(0x18000000),
            radius = half * 1.12f,
            center = Offset(s.x, s.y + 12f)
        )

        when (s.kind) {
            ShapeKind.CIRCLE -> {
                drawCircle(color = color, radius = half, center = center)
                drawCircle(color = Color.White.copy(alpha = 0.45f), radius = half * 0.35f, center = Offset(s.x - half * 0.35f, s.y - half * 0.35f))
            }
            ShapeKind.ROUND_SQUARE -> {
                val roundRect = RoundRect(
                    left = s.x - half,
                    top = s.y - half,
                    right = s.x + half,
                    bottom = s.y + half,
                    cornerRadius = CornerRadius(38f, 38f)
                )
                val path = Path().apply { addRoundRect(roundRect) }
                drawPath(path, color = color)
                drawCircle(color = Color.White.copy(alpha = 0.45f), radius = half * 0.3f, center = Offset(s.x - half * 0.35f, s.y - half * 0.35f))
            }
            ShapeKind.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(s.x, s.y - half * 1.1f)
                    lineTo(s.x + half * 1.15f, s.y + half * 0.9f)
                    lineTo(s.x - half * 1.15f, s.y + half * 0.9f)
                    close()
                }
                drawPath(path, color = color)
                drawCircle(color = Color.White.copy(alpha = 0.45f), radius = half * 0.25f, center = Offset(s.x, s.y - half * 0.2f))
            }
            ShapeKind.STAR -> {
                val path = Path()
                val numPoints = 5
                val outerR = half * 1.15f
                val innerR = half * 0.52f
                for (i in 0 until numPoints * 2) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = (i * Math.PI / numPoints) - (Math.PI / 2)
                    val x = s.x + (r * cos(angle)).toFloat()
                    val y = s.y + (r * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color = color)
                drawCircle(color = Color.White.copy(alpha = 0.5f), radius = innerR * 0.5f, center = center)
            }
            ShapeKind.HEART -> {
                val path = Path().apply {
                    val w = half * 1.8f
                    val h = half * 1.7f
                    moveTo(s.x, s.y + h * 0.45f)
                    cubicTo(s.x - w * 0.7f, s.y - h * 0.2f, s.x - w * 0.5f, s.y - h * 0.65f, s.x, s.y - h * 0.2f)
                    cubicTo(s.x + w * 0.5f, s.y - h * 0.65f, s.x + w * 0.7f, s.y - h * 0.2f, s.x, s.y + h * 0.45f)
                    close()
                }
                drawPath(path, color = color)
            }
            ShapeKind.OVAL -> {
                drawOval(
                    color = color,
                    topLeft = Offset(s.x - half * 1.25f, s.y - half * 0.85f),
                    size = Size(half * 2.5f, half * 1.7f)
                )
                drawCircle(color = Color.White.copy(alpha = 0.45f), radius = half * 0.28f, center = Offset(s.x - half * 0.4f, s.y - half * 0.25f))
            }
        }

        // Friendly cute face on shapes
        val eyeOffset = half * 0.28f
        drawCircle(color = Color(0xFF261D1A), radius = 6.5f, center = Offset(s.x - eyeOffset, s.y))
        drawCircle(color = Color(0xFF261D1A), radius = 6.5f, center = Offset(s.x + eyeOffset, s.y))
        // Smiling cheeks
        drawCircle(color = Color(0x66FF5A5F), radius = 9f, center = Offset(s.x - eyeOffset * 1.4f, s.y + 11f))
        drawCircle(color = Color(0x66FF5A5F), radius = 9f, center = Offset(s.x + eyeOffset * 1.4f, s.y + 11f))
    }
}
