package com.thanhng224.app.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FlowerAnimation(
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing)
        )
        onAnimationFinished()
    }

    // Flower Particles
    val particles = remember { List(15) { FlowerParticle() } }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // Draw expanding background circle (Blue/Cyan gradient feel)
        drawCircle(
            color = Color(0xFF3B82F6).copy(alpha = (0.2f * (1 - progress.value)).coerceIn(0f, 1f)),
            radius = size.width * progress.value * 1.5f,
            center = Offset(centerX, centerY)
        )

        // Fade out flower near end
        val flowerAlpha = if (progress.value > 0.8f) {
            ((1f - progress.value) * 5f).coerceIn(0f, 1f)
        } else {
            1f
        }

        if (flowerAlpha > 0f) {
            withTransform({
                rotate(degrees = progress.value * 360f, pivot = Offset(centerX, centerY))
                scale(scaleX = progress.value, scaleY = progress.value, pivot = Offset(centerX, centerY))
            }) {
                drawFlower(
                    center = Offset(centerX, centerY),
                    radius = 100f,
                    petalColor = Color(0xFF06B6D4).copy(alpha = flowerAlpha), // Cyan
                    centerColor = Color(0xFF3B82F6).copy(alpha = flowerAlpha)  // Blue
                )
            }
        }

        // Draw floating particles
        particles.forEach { particle ->
            val currentRadius = lerp(0f, size.width, progress.value * particle.speed)
            val x = centerX + (cos(particle.angle) * currentRadius).toFloat()
            val y = centerY + (sin(particle.angle) * currentRadius).toFloat()
            
            drawCircle(
                color = particle.color.copy(alpha = (1 - progress.value).coerceIn(0f, 1f)),
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawFlower(center: Offset, radius: Float, petalColor: Color, centerColor: Color) {
    val petalCount = 6
    val angleStep = (2 * PI) / petalCount
    
    // Draw Petals
    for (i in 0 until petalCount) {
        val angle = i * angleStep
        val petalCenter = Offset(
            center.x + (cos(angle) * (radius * 0.6f)).toFloat(),
            center.y + (sin(angle) * (radius * 0.6f)).toFloat()
        )
        drawCircle(
            color = petalColor,
            radius = radius * 0.5f,
            center = petalCenter
        )
    }
    
    // Draw Center
    drawCircle(
        color = centerColor,
        radius = radius * 0.4f,
        center = center
    )
}

private data class FlowerParticle(
    val angle: Double = Random.nextDouble() * 2 * PI,
    val speed: Float = Random.nextFloat() * 0.5f + 0.5f,
    val size: Float = Random.nextFloat() * 10f + 5f,
    val color: Color = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF06B6D4), // Cyan
        Color(0xFF60A5FA)  // Light Blue
    ).random()
)
