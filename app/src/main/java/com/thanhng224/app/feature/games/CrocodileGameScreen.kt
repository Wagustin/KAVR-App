package com.thanhng224.app.feature.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrocodileGameScreen(navController: NavController) {
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 0
    // Easy(0)=1 bad, Medium(1)=2 bad, Hard(2)=3 bad
    val numBadTeeth = difficulty + 1 
    
    var teeth by remember { mutableStateOf(List(13) { false }) } // false = good, true = pressed
    var badToothIndices by remember(numBadTeeth) { 
        mutableStateOf(generateBadTeeth(numBadTeeth)) 
    }
    var isGameOver by remember { mutableStateOf(false) }
    var mouthAngle by remember { mutableFloatStateOf(0f) }

    val animatedMouthAngle by animateFloatAsState(
        targetValue = if (isGameOver) 45f else 0f,
        animationSpec = tween(durationMillis = 200)
    )

    fun resetGame() {
        teeth = List(13) { false }
        badToothIndices = generateBadTeeth(numBadTeeth)
        isGameOver = false
        mouthAngle = 0f
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cocodrilo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { resetGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFE0F7FA)), // Light Cyan Cyan Background
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            Text(
                if (isGameOver) "¡TE MORDIÓ!" else "Presiona un diente...",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isGameOver) Color.Red else Color.Black
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // CANVAS DRAWING OF CROCODILE
            Box(
                modifier = Modifier
                    .size(300.dp, 350.dp)
                    .clickable(enabled = false) {} // Consume clicks in empty areas
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val headColor = Color(0xFF4CAF50)
                    val jawColor = Color(0xFF388E3C)
                    val toothColor = Color.White
                    val pressedToothColor = Color.Gray

                    // --- UPPER JAW (The one that falls) ---
                    // Pivot point for rotation is at the back of the jaw (top-left ish in this simplified view, wait no, back)
                    // Let's assume side view for simplicity? No, top down is easier for buttons.
                    // Actually, a "Top Down" open mouth view is best for clicking teeth.
                    // But to show "snapping", maybe simpler:
                    
                    // Let's do a Top-Down view of the lower jaw with teeth.
                    // And the game over is just a "Visual Snap" (screen shake or red overlay) + Text.
                    
                    // Wait, user asked for "se cierre la boca". 
                    // Let's simulate a side/isometric view.
                    
                    // Simply: Upper Jaw (Top Half), Lower Jaw (Bottom Half).
                    // Teeth are on the Lower Jaw.
                    // When bitten, Upper Jaw rotates down.
                }
                
                // We compose the "Lower Jaw" with buttons manually because detecting canvas clicks is annoying.
                // Re-implementation strategy:
                // Draw the head geometry on Canvas.
                // Overlay "Tooth" Composables on top of the "Gum" line.

                CrocodileHead(
                    mouthOpenProgress = if (isGameOver) 0f else 1f, // 1 = Open, 0 = Closed
                    teethState = teeth,
                    onToothClick = { index ->
                        if (!isGameOver && !teeth[index]) {
                            if (badToothIndices.contains(index)) {
                                isGameOver = true
                            } else {
                                // Update logic
                                teeth = teeth.toMutableList().also { it[index] = true }
                            }
                        }
                    }
                )
            }
            
            if (isGameOver) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Jugar de Nuevo", fontSize = 18.sp)
                }
            }
        }
    }
}

fun generateBadTeeth(count: Int): Set<Int> {
    val bad = mutableSetOf<Int>()
    while (bad.size < count) {
        bad.add(Random.nextInt(13))
    }
    return bad
}

@Composable
fun CrocodileHead(
    mouthOpenProgress: Float, // 1f = Open, 0.0f = Closed (Snapped)
    teethState: List<Boolean>,
    onToothClick: (Int) -> Unit
) {
    // Upper Jaw Angle: -45 degrees (Open) to 0 degrees (Closed)
    // We animate a "Box" that represents the upper jaw.
    
    val upperJawAngle by animateFloatAsState(
        targetValue = if (mouthOpenProgress < 0.5f) 0f else -45f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
    )

    Box(contentAlignment = Alignment.Center) {
        
        // 1. LOWER JAW (Static Base)
        Canvas(modifier = Modifier.size(300.dp, 200.dp)) {
            // Jaw Shape
            drawRoundRect(
                color = Color(0xFF4CAF50),
                topLeft = Offset(20f, 50f),
                size = Size(260f, 150f),
                cornerRadius = CornerRadius(40f, 40f)
            )
            // Gum
            drawArc(
                color = Color(0xFFE57373),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(40f, 70f),
                size = Size(220f, 120f)
            )
        }

        // 2. TEETH (Interactive Buttons arranged in an arc)
        // We manually position them based on an Arc formula
        Box(modifier = Modifier.size(220.dp, 120.dp).offset(y = 20.dp)) {
            val radiusX = 100.dp
            val radiusY = 60.dp
            val centerX = 110.dp
            val centerY = 0.dp // Top of arch
            
            teethState.forEachIndexed { index, isPressed ->
                // Calculate position on semi-ellipse
                // Angle goes from PI (left) to 2PI (right)? No, 180 to 360 relative to center
                // Let's simply spread 13 teeth from 10 degrees to 170 degrees
                
                val angleDeg = 180f - (index * (180f / (teethState.size - 1)))
                val angleRad = Math.toRadians(angleDeg.toDouble())
                
                // Ellipse param
                val x = centerX + (radiusX.value * Math.cos(angleRad)).dp - 12.dp // -half size
                val y = centerY + (radiusY.value * Math.sin(angleRad)).dp // + offset?
                
                // To make them look lower-jaw style, invert Y logic essentially
                // Actually, let's keep it simple: Semicircle UP facing.
                
                Box(
                    modifier = Modifier
                        .offset(x = x, y = y + 40.dp)
                        .size(24.dp)
                        .background(
                            color = if (isPressed) Color.Gray else Color.White,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onToothClick(index) }
                )
            }
        }

        // 3. UPPER JAW (Animated Overlay)
        // We rotate this entire Box
        Box(
            modifier = Modifier
                .size(300.dp, 200.dp)
                .offset(y = (-100).dp) // Move up so pivot aligns
                .graphicsLayer {
                    rotationX = upperJawAngle // 3D rotation looks cool? Or just 2D
                    // Let's try 2D translation/scale for "Closing" effect
                    // Simpler: Just Translate Y down to cover teeth
                   
                    translationY = if (upperJawAngle == 0f) 150f else 0f
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Upper Head
                drawRoundRect(
                    color = Color(0xFF388E3C), // Darker Green
                    topLeft = Offset(10f, 10f),
                    size = Size(280f, 180f),
                    cornerRadius = CornerRadius(50f, 50f)
                )
                // Eyes
                drawCircle(Color.White, radius = 20f, center = Offset(70f, 60f))
                drawCircle(Color.White, radius = 20f, center = Offset(230f, 60f))
                drawCircle(Color.Black, radius = 8f, center = Offset(70f, 60f))
                drawCircle(Color.Black, radius = 8f, center = Offset(230f, 60f))
                
                // Nose
                drawCircle(Color(0xFF1B5E20), radius = 6f, center = Offset(130f, 20f))
                drawCircle(Color(0xFF1B5E20), radius = 6f, center = Offset(170f, 20f))
            }
        }
    }
}


