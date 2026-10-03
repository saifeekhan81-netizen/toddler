package com.example.ui.activities

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.audio.TinyAudioPlayer
import com.example.domain.ChildAge
import com.example.utils.HapticFeedbackUtil
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class Star(
    val id: Long,
    var x: Float,
    var y: Float,
    val baseRadius: Float,
    var currentScale: Float = 1f,
    var targetScale: Float = 1f,
    val rotationSpeed: Float,
    var rotation: Float = 0f,
    val color: Color,
    val speedY: Float,
    var twinklePhase: Float
)

private data class StarParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float,
    var alpha: Float = 1f,
    val decay: Float = 0.04f
)

private data class TrailPoint(
    val x: Float,
    val y: Float,
    var alpha: Float = 1f,
    val color: Color
)

@Composable
fun GlowingStarsActivity(
    audioPlayer: TinyAudioPlayer,
    hapticUtil: HapticFeedbackUtil,
    childAge: ChildAge,
    isWindingDown: Boolean,
    modifier: Modifier = Modifier
) {
    val stars = remember { mutableStateListOf<Star>() }
    val particles = remember { mutableStateListOf<StarParticle>() }
    val trailPoints = remember { mutableStateListOf<TrailPoint>() }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    val starCount = when (childAge) {
        ChildAge.ONE -> 7
        ChildAge.TWO -> 12
        ChildAge.THREE -> 16
    }

    val starBaseSize = when (childAge) {
        ChildAge.ONE -> 52f
        ChildAge.TWO -> 40f
        ChildAge.THREE -> 34f
    }

    // Animation ticker
    LaunchedEffect(Unit) {
        while (true) {
            val frameTimeNanos = awaitFrame()
            if (lastFrameTime == 0L) {
                lastFrameTime = frameTimeNanos
                continue
            }
            val dt = ((frameTimeNanos - lastFrameTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
            lastFrameTime = frameTimeNanos

            // Update trail
            val trailIter = trailPoints.iterator()
            while (trailIter.hasNext()) {
                val tp = trailIter.next()
                tp.alpha -= 0.035f * (dt * 60f)
                if (tp.alpha <= 0f) trailIter.remove()
            }

            // Update particles
            val partIter = particles.iterator()
            while (partIter.hasNext()) {
                val p = partIter.next()
                p.x += p.vx * dt * 60f
                p.y += p.vy * dt * 60f
                p.alpha -= p.decay * (dt * 60f)
                if (p.alpha <= 0f) partIter.remove()
            }

            // Update stars
            for (s in stars) {
                s.rotation += s.rotationSpeed * dt * 60f
                s.twinklePhase += dt * 2.5f
                s.y -= s.speedY * dt * 60f
                if (s.y < -100f) {
                    s.y = 2200f
                    s.x = Random.nextFloat() * 1000f
                }
                // Spring back from touch pulse
                s.currentScale += (s.targetScale - s.currentScale) * 0.15f
                if (s.currentScale > 1.05f && s.targetScale > 1f) {
                    s.targetScale = 1f
                }
            }
        }
    }

    fun onTouchStar(star: Star) {
        star.currentScale = 1.6f
        star.targetScale = 1.0f
        audioPlayer.play("chime")
        hapticUtil.playLightPop()

        for (i in 0 until 12) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 5f + 2f
            particles.add(
                StarParticle(
                    x = star.x,
                    y = star.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = star.color,
                    radius = Random.nextFloat() * 4f + 2f
                )
            )
        }
    }

    fun addTrail(x: Float, y: Float) {
        val colors = listOf(Color(0xFFFFF4B8), Color(0xFFFFD166), Color(0xFFB5EAEA), Color(0xFFFF9AA2))
        trailPoints.add(TrailPoint(x, y, alpha = 0.9f, color = colors.random()))
        if (trailPoints.size > 50) {
            trailPoints.removeAt(0)
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

                        addTrail(touchX, touchY)
                        val tolerance = if (childAge == ChildAge.ONE) 50f else 30f
                        val hit = stars.findLast { s ->
                            val dx = s.x - touchX
                            val dy = s.y - touchY
                            sqrt(dx * dx + dy * dy) <= (s.baseRadius + tolerance)
                        }
                        hit?.let { onTouchStar(it) }

                        do {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (change.pressed) {
                                    val px = change.position.x
                                    val py = change.position.y
                                    addTrail(px, py)
                                    val dragHit = stars.findLast { s ->
                                        val dx = s.x - px
                                        val dy = s.y - py
                                        sqrt(dx * dx + dy * dy) <= (s.baseRadius + tolerance)
                                    }
                                    if (dragHit != null && dragHit.currentScale < 1.2f) {
                                        onTouchStar(dragHit)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            if (stars.isEmpty()) {
                val w = size.width
                val h = size.height
                val starColors = listOf(
                    Color(0xFFFFF07C),
                    Color(0xFFFFDF6D),
                    Color(0xFFFFBCB5),
                    Color(0xFFCEE5D0),
                    Color(0xFFE0C3FC),
                    Color(0xFF98EECC)
                )
                for (i in 0 until starCount) {
                    stars.add(
                        Star(
                            id = System.nanoTime() + i,
                            x = Random.nextFloat() * (w - 80f) + 40f,
                            y = Random.nextFloat() * (h - 80f) + 40f,
                            baseRadius = starBaseSize * (0.8f + Random.nextFloat() * 0.4f),
                            rotationSpeed = (Random.nextFloat() - 0.5f) * 1.2f,
                            color = starColors.random(),
                            speedY = Random.nextFloat() * 0.5f + 0.2f,
                            twinklePhase = Random.nextFloat() * 6.28f
                        )
                    )
                }
            }

            // Night Sky Gradient
            val skyTop = if (isWindingDown) Color(0xFF070B14) else Color(0xFF0F1A30)
            val skyBottom = if (isWindingDown) Color(0xFF101622) else Color(0xFF1E2D4A)
            drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyBottom)))

            // Gentle Smiling Crescent Moon in corner
            drawMoon(Offset(size.width * 0.82f, size.height * 0.12f), 42f)

            // Draw Trails
            for (tp in trailPoints) {
                drawCircle(
                    color = tp.color.copy(alpha = tp.alpha),
                    radius = 8f * tp.alpha,
                    center = Offset(tp.x, tp.y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = tp.alpha * 0.6f),
                    radius = 3f * tp.alpha,
                    center = Offset(tp.x, tp.y)
                )
            }

            // Draw Stars
            for (s in stars) {
                val twinkle = (sin(s.twinklePhase) * 0.25f + 0.75f).coerceIn(0.5f, 1f)
                val currentRadius = s.baseRadius * s.currentScale * twinkle
                drawGlowingStar(s.x, s.y, currentRadius, s.rotation, s.color)
            }

            // Draw Burst Particles
            for (p in particles) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

private fun DrawScope.drawMoon(center: Offset, radius: Float) {
    // Soft moon aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x44FFF8D6), Color(0x00FFF8D6)),
            center = center,
            radius = radius * 2.2f
        ),
        radius = radius * 2.2f,
        center = center
    )
    // Moon disc
    drawCircle(
        color = Color(0xFFFFF4CC),
        radius = radius,
        center = center
    )
    // Friendly sleepy eyes
    val eyeY = center.y - 3f
    drawCircle(Color(0xFF8D774D), radius = 3.2f, center = Offset(center.x - 10f, eyeY))
    drawCircle(Color(0xFF8D774D), radius = 3.2f, center = Offset(center.x + 8f, eyeY))
    // Soft smiling cheek
    drawCircle(Color(0x66FFAAA6), radius = 5.5f, center = Offset(center.x - 12f, center.y + 7f))
    drawCircle(Color(0x66FFAAA6), radius = 5.5f, center = Offset(center.x + 10f, center.y + 7f))
}

private fun DrawScope.drawGlowingStar(cx: Float, cy: Float, radius: Float, rotationDeg: Float, color: Color) {
    // Soft aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.0f)),
            center = Offset(cx, cy),
            radius = radius * 2.3f
        ),
        radius = radius * 2.3f,
        center = Offset(cx, cy)
    )

    // 5-point Star Path
    val path = Path()
    val numPoints = 5
    val outerR = radius
    val innerR = radius * 0.45f

    for (i in 0 until numPoints * 2) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = (i * Math.PI / numPoints) - (Math.PI / 2)
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    rotate(rotationDeg, pivot = Offset(cx, cy)) {
        drawPath(path, color = color)
        // Center glow dot
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = innerR * 0.6f,
            center = Offset(cx, cy)
        )
    }
}
