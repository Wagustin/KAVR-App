package com.thanhng224.app.presentation.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.scale
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
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing) // Más fluido
        )
        onAnimationFinished()
    }

    // Aumentamos partículas y añadimos variedad de "flores pequeñas"
    val particles = remember { List(40) { FlowerParticle() } } // De 15 a 40
    val smallFlowers = remember { List(12) { SmallFlowerParticle() } } // Nuevas flores orbitando
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        
        // Fondo expansivo más sutil
        drawCircle(
            color = Color(0xFF9C27B0).copy(alpha = (0.15f * (1 - progress.value)).coerceIn(0f, 1f)),
            radius = size.width * progress.value * 2f,
            center = Offset(centerX, centerY)
        )

        val animationValue = progress.value
        
        // Flor Principal
        val mainFlowerAlpha = if (animationValue > 0.85f) {
            ((1f - animationValue) * 6f).coerceIn(0f, 1f)
        } else {
            1f
        }

        if (mainFlowerAlpha > 0f) {
            // Rotación más suave y escala elástica
            val scale = if (animationValue < 0.5f) animationValue * 2f else 1f + (animationValue - 0.5f) * 0.5f
            
            withTransform({
                rotate(degrees = animationValue * 180f, pivot = Offset(centerX, centerY))
                scale(scaleX = scale, scaleY = scale, pivot = Offset(centerX, centerY))
            }) {
                drawFlower(
                    center = Offset(centerX, centerY),
                    radius = 120f,
                    petalColor = Color(0xFF00E5FF).copy(alpha = mainFlowerAlpha), // Cyan
                    centerColor = Color(0xFF9C27B0).copy(alpha = mainFlowerAlpha)  // Purple
                )
            }
        }

        // Flores pequeñas satélite que explotan hacia afuera
        smallFlowers.forEach { flower ->
            val dist = lerp(0f, size.width * 0.8f, animationValue * flower.speed)
            val x = centerX + (cos(flower.angle) * dist).toFloat()
            val y = centerY + (sin(flower.angle) * dist).toFloat()
            
            // Rotan sobre sí mismas mientras viajan
            withTransform({
                rotate(degrees = animationValue * 720f * flower.rotationDir, pivot = Offset(x, y))
                scale(scaleX = animationValue, scaleY = animationValue, pivot = Offset(x, y))
            }) {
                drawFlower(
                    center = Offset(x, y),
                    radius = flower.size,
                    petalColor = flower.color.copy(alpha = (1 - animationValue).coerceIn(0f, 1f)),
                    centerColor = Color.White.copy(alpha = (1 - animationValue).coerceIn(0f, 1f))
                )
            }
        }

        // Partículas flotantes (brillos)
        particles.forEach { particle ->
            val currentRadius = lerp(0f, size.width * 1.2f, animationValue * particle.speed)
            val x = centerX + (cos(particle.angle) * currentRadius).toFloat()
            val y = centerY + (sin(particle.angle) * currentRadius).toFloat()
            
            drawCircle(
                color = particle.color.copy(alpha = (1 - animationValue).coerceIn(0f, 1f)),
                radius = particle.size * (1 - animationValue * 0.5f), // Se hacen un poco más pequeñas
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawFlower(center: Offset, radius: Float, petalColor: Color, centerColor: Color) {
    val petalCount = 8 // Más pétalos para que se vea más tupida
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
    val speed: Float = Random.nextFloat() * 0.8f + 0.4f, // Más rápidas
    val size: Float = Random.nextFloat() * 8f + 3f,
    val color: Color = listOf(
        Color(0xFFE040FB), // Purple Accent
        Color(0xFF18FFFF), // Cyan Accent
        Color(0xFFFFFFFF)  // White
    ).random()
)

private data class SmallFlowerParticle(
    val angle: Double = Random.nextDouble() * 2 * PI,
    val speed: Float = Random.nextFloat() * 0.6f + 0.6f,
    val size: Float = Random.nextFloat() * 20f + 15f,
    val rotationDir: Int = if (Random.nextBoolean()) 1 else -1,
    val color: Color = listOf(
        Color(0xFFBA68C8), // Light Purple
        Color(0xFF4DD0E1)  // Light Cyan
    ).random()
)
