package com.thanhng224.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun ProfileScreen() {
    // Generate random flower positions once
    val flowerClusters = remember { List(4) { Random.nextFloat() } }

    val PaperColor = Color(0xFFFDFBF7) // Define PaperColor here

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3E2723)) // Dark Wood Background
            .padding(top = 48.dp, bottom = 100.dp, start = 16.dp, end = 16.dp), // Más margen vertical para no chocar con barras
        contentAlignment = Alignment.Center
    ) {
        // --- EL PAPEL (Carta) ---
        Box(
            modifier = Modifier
                .fillMaxSize() // Ahora llena el espacio reducido por el padding padre
                .clip(RoundedCornerShape(4.dp))
                .background(PaperColor)
                .padding(24.dp) // Margen interno del papel
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // 1. Paper Texture/Lines
                    val lineHeight = 35.dp.toPx()
                    val startY = 80.dp.toPx()
                    var y = startY
                    
                    while (y < height - 40.dp.toPx()) {
                        drawLine(
                            color = Color(0xFFECEFF1), // Very faint blue
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                        y += lineHeight
                    }

                    // 2. "Flores Nube" (Baby's Breath) Decoration
                    // Draw clusters at top-left and bottom-right
                    drawBabyBreathCluster(Offset(40f, 40f))
                    drawBabyBreathCluster(Offset(width - 40f, height - 40f))
                    drawBabyBreathCluster(Offset(width - 30f, 50f))
                    drawBabyBreathCluster(Offset(40f, height - 60f))
                }
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()), // Allow scrolling if text is long
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Para ti, Kitkat",
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Poem Body
                Text(
                    text = "No soy perfecto, pero todo lo que hago por ti\n" +
                           "lo hago con amor.\n\n" +
                           "Esta app es solo una excusa\n" +
                           "para recordarte lo importante que eres para mí\n" +
                           "y lo feliz que me haces.\n\n" +
                           "Feliz cumpleaños 💖\n\n" +
                           "Esta app existe porque tú existes en mi vida,\n",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF424242)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Signature
                Text(
                    text = "siempre tuyo,\nwagustincito",
                    fontFamily = FontFamily.Cursive,
                    fontSize = 28.sp,
                    textAlign = TextAlign.End,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// Helper to draw "Flores Nube" (Gypsophila)
// Clusters of tiny white dots on thin stems
fun DrawScope.drawBabyBreathCluster(center: Offset) {
    val stemColor = Color(0xFFA5D6A7) // Light Green
    val flowerColor = Color.White
    val shadowColor = Color(0xFFE0E0E0)
    
    val rnd = Random(center.hashCode())

    // Draw Stems
    for (i in 0..15) {
        val angle = rnd.nextFloat() * 6.28f
        val length = 30f + rnd.nextFloat() * 40f
        val endX = center.x + kotlin.math.cos(angle) * length
        val endY = center.y + kotlin.math.sin(angle) * length
        
        drawLine(
            color = stemColor,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 2f
        )
        
        // Draw tiny flowers (blooms) at end
        drawCircle(
            color = shadowColor,
            radius = 6f,
            center = Offset(endX + 2f, endY + 2f)
        )
        drawCircle(
            color = flowerColor,
            radius = 5f,
            center = Offset(endX, endY)
        )
        
        // Some flowers along the stem
        if (rnd.nextBoolean()) {
            val midX = (center.x + endX) / 2
            val midY = (center.y + endY) / 2
            drawCircle(flowerColor, radius = 4f, center = Offset(midX, midY))
        }
    }
}
