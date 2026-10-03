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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import com.example.audio.TinyAudioPlayer
import com.example.domain.ChildAge
import com.example.utils.HapticFeedbackUtil
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class Bubble(
    val id: Long,
    var x: Float,
    var y: Float,
    val radius: Float,
    val baseColor: Color,
    val speedY: Float,
    val swayAmp: Float,
    val swaySpeed: Float,
    val phase: Float,
    val isSpecial: Boolean = false
)

private data class BubbleParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val radius: Float,
    var alpha: Float = 1f,
    val decay: Float = 0.035f
)

private val BubbleColors = listOf(
    Color(0xFFFF9AA2), // Soft Pink
    Color(0xFFFFB7B2), // Coral
    Color(0xFFFFDAC1), // Peach
    Color(0xFFE2F0CB), // Mint
    Color(0xFFB5EAD7), // Aquamarine
    Color(0xFFC7CEEA), // Lavender
    Color(0xFF85E3FF), // Sky
    Color(0xFFFFF3A7)  // Sunshine
)

@Composable
fun BubblePopActivity(
    audioPlayer: TinyAudioPlayer,
    hapticUtil: HapticFeedbackUtil,
    childAge: ChildAge,
    isWindingDown: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbles = remember { mutableStateListOf<Bubble>() }
    val particles = remember { mutableStateListOf<BubbleParticle>() }
    var lastFrameTime by remember { mutableLongStateOf(0L) }

    val targetCount = when (childAge) {
        ChildAge.ONE -> 6
        ChildAge.TWO -> 10
        ChildAge.THREE -> 14
    }

    val speedMultiplier = when {
        isWindingDown -> 0.45f
        childAge == ChildAge.ONE -> 0.65f
        childAge == ChildAge.TWO -> 0.95f
        else -> 1.25f
    }

    val baseRadius = when (childAge) {
        ChildAge.ONE -> 65f
        ChildAge.TWO -> 50f
        ChildAge.THREE -> 42f
    }

    // Animation Loop
    LaunchedEffect(Unit) {
        while (true) {
            val frameTimeNanos = awaitFrame()
            if (lastFrameTime == 0L) {
                lastFrameTime = frameTimeNanos
                continue
            }
            val dt = ((frameTimeNanos - lastFrameTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
            lastFrameTime = frameTimeNanos

            // Update particles
            val particleIterator = particles.iterator()
            while (particleIterator.hasNext()) {
                val p = particleIterator.next()
                p.x += p.vx * dt * 60f
                p.y += p.vy * dt * 60f
                p.alpha -= p.decay * (dt * 60f)
                if (p.alpha <= 0f) {
                    particleIterator.remove()
                }
            }

            // Update bubbles
            val timeSec = frameTimeNanos / 1_000_000_000f
            for (b in bubbles) {
                b.y -= b.speedY * speedMultiplier * dt * 60f
                b.x += sin(timeSec * b.swaySpeed + b.phase) * b.swayAmp * dt * 60f

                // Wrap around when exiting top
                if (b.y < -b.radius * 2) {
                    b.y = 2200f
                    b.x = Random.nextFloat() * 1000f
                }
            }
        }
    }

    fun popBubble(bubble: Bubble) {
        bubbles.remove(bubble)
        if (bubble.isSpecial) {
            audioPlayer.play("sparkle")
            hapticUtil.playLightPop()
            // Burst into extra sparkling particles
            for (i in 0 until 18) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 7f + 2f
                particles.add(
                    BubbleParticle(
                        x = bubble.x,
                        y = bubble.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        color = Color(0xFFFFF7C2),
                        radius = Random.nextFloat() * 7f + 3f,
                        decay = 0.025f
                    )
                )
            }
        } else {
            audioPlayer.play("pop")
            hapticUtil.playSoftClick()
            // Standard bubble burst
            for (i in 0 until 10) {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val speed = Random.nextFloat() * 5f + 1.5f
                particles.add(
                    BubbleParticle(
                        x = bubble.x,
                        y = bubble.y,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        color = bubble.baseColor,
                        radius = Random.nextFloat() * 5f + 2.5f
                    )
                )
            }
        }

        // Spawn a replacement bubble smoothly at the bottom
        val isSpecial = Random.nextFloat() < 0.15f
        bubbles.add(
            Bubble(
                id = System.nanoTime(),
                x = Random.nextFloat() * 900f + 50f,
                y = 2100f + Random.nextFloat() * 200f,
                radius = if (isSpecial) baseRadius * 1.55f else baseRadius * (0.8f + Random.nextFloat() * 0.5f),
                baseColor = BubbleColors.random(),
                speedY = Random.nextFloat() * 1.6f + 1.1f,
                swayAmp = Random.nextFloat() * 1.5f + 0.5f,
                swaySpeed = Random.nextFloat() * 1.8f + 1.0f,
                phase = Random.nextFloat() * 6.28f,
                isSpecial = isSpecial
            )
        )
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

                        // Check hit with expanded touch tolerance
                        val hitTolerance = if (childAge == ChildAge.ONE) 45f else 25f
                        val hit = bubbles.findLast { b ->
                            val dx = b.x - touchX
                            val dy = b.y - touchY
                            sqrt(dx * dx + dy * dy) <= (b.radius + hitTolerance)
                        }
                        hit?.let { popBubble(it) }

                        // Multi-touch drag tracking
                        do {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                if (change.pressed) {
                                    val px = change.position.x
                                    val py = change.position.y
                                    val draggedHit = bubbles.findLast { b ->
                                        val dx = b.x - px
                                        val dy = b.y - py
                                        sqrt(dx * dx + dy * dy) <= (b.radius + hitTolerance)
                                    }
                                    draggedHit?.let { popBubble(it) }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            // Initialize bubbles on first draw with canvas bounds
            if (bubbles.isEmpty()) {
                val w = size.width
                val h = size.height
                for (i in 0 until targetCount) {
                    val isSpecial = (i == 0)
                    bubbles.add(
                        Bubble(
                            id = System.nanoTime() + i,
                            x = Random.nextFloat() * (w - 100f) + 50f,
                            y = Random.nextFloat() * (h * 0.9f) + 50f,
                            radius = if (isSpecial) baseRadius * 1.55f else baseRadius * (0.8f + Random.nextFloat() * 0.5f),
                            baseColor = BubbleColors.random(),
                            speedY = Random.nextFloat() * 1.6f + 1.1f,
                            swayAmp = Random.nextFloat() * 1.5f + 0.5f,
                            swaySpeed = Random.nextFloat() * 1.8f + 1.0f,
                            phase = Random.nextFloat() * 6.28f,
                            isSpecial = isSpecial
                        )
                    )
                }
            }

            // Background subtle gradient
            val bgTop = if (isWindingDown) Color(0xFFE2EAF8) else Color(0xFFF1F8FF)
            val bgBottom = if (isWindingDown) Color(0xFFD3E0F2) else Color(0xFFE7F0FD)
            drawRect(
                brush = Brush.verticalGradient(listOf(bgTop, bgBottom))
            )

            // Draw floating bubbles
            for (b in bubbles) {
                drawBubble(b)
            }

            // Draw burst particles
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

private fun DrawScope.drawBubble(bubble: Bubble) {
    val center = Offset(bubble.x, bubble.y)

    // Outer glow for special sparkle bubble
    if (bubble.isSpecial) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFF176), Color(0x00FFF176)),
                center = center,
                radius = bubble.radius * 1.4f
            ),
            radius = bubble.radius * 1.4f,
            center = center
        )
    }

    // Soft bubble translucent body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bubble.baseColor.copy(alpha = 0.25f),
                bubble.baseColor.copy(alpha = 0.55f)
            ),
            center = Offset(bubble.x - bubble.radius * 0.25f, bubble.y - bubble.radius * 0.25f),
            radius = bubble.radius
        ),
        radius = bubble.radius,
        center = center
    )

    // Bubble rim outline
    drawCircle(
        color = bubble.baseColor.copy(alpha = 0.85f),
        radius = bubble.radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
    )

    // Specular highlight gleam
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = bubble.radius * 0.26f,
        center = Offset(bubble.x - bubble.radius * 0.35f, bubble.y - bubble.radius * 0.35f)
    )

    // Secondary subtle rim reflection
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = bubble.radius * 0.12f,
        center = Offset(bubble.x + bubble.radius * 0.3f, bubble.y + bubble.radius * 0.35f)
    )
}
