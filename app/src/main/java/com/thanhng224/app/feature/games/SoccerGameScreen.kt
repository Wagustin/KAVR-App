package com.thanhng224.app.feature.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoccerGameScreen(navController: NavController) {
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 0
    val keeperSpeed = when (difficulty) {
        0 -> 0.008f // Easy: Slower
        1 -> 0.015f // Medium: Standard
        else -> 0.020f // Hard: Fast (Tracking)
    }

    var score by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(5) }
    var gameMessage by remember { mutableStateOf("") }
    
    // Physics State (Use State objects to avoid recomposition loop)
    val ballPos = remember { mutableStateOf(Offset(0.5f, 0.8f)) }
    val ballVelocity = remember { mutableStateOf(Offset.Zero) }
    val isBallMoving = remember { mutableStateOf(false) }
    
    // Goalkeeper State
    val keeperX = remember { mutableFloatStateOf(0.5f) }
    val keeperDirection = remember { mutableFloatStateOf(1f) }
    
    // Game Loop - Optimized for A12 (Vsync Sync)
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameTime ->
                val delta = (frameTime - lastTime) / 16f 
                lastTime = frameTime
                
                // Keeper AI
                if (difficulty == 2) {
                    // HARD: Ball Tracking
                    val targetX = ballPos.value.x
                    // Move towards ball
                    if (keeperX.floatValue < targetX - 0.02f) {
                        keeperX.floatValue += keeperSpeed * delta
                    } else if (keeperX.floatValue > targetX + 0.02f) {
                        keeperX.floatValue -= keeperSpeed * delta
                    }
                } else {
                    // EASY / MEDIUM: Random Patrol
                    keeperX.floatValue += keeperSpeed * keeperDirection.floatValue * delta
                    if (keeperX.floatValue > 0.8f || keeperX.floatValue < 0.2f) keeperDirection.floatValue *= -1
                    
                    // Randomly change direction
                    if (Random.nextFloat() < (0.05f * delta)) keeperDirection.floatValue *= -1
                }
                
                // Ball Physics
                if (isBallMoving.value) {
                    ballPos.value += (ballVelocity.value * delta)
                    
                    // Goal Line Reached
                    if (ballPos.value.y < 0.15f) {
                        isBallMoving.value = false
                        // Collision Check
                        if (ballPos.value.x > keeperX.floatValue - 0.1f && ballPos.value.x < keeperX.floatValue + 0.1f) {
                            gameMessage = "¡ATAJADO!"
                            attempts--
                        } else if (ballPos.value.x > 0.1f && ballPos.value.x < 0.9f) {
                            gameMessage = "¡GOL!"
                            score++
                            attempts--
                        } else {
                            gameMessage = "¡FUERA!"
                            attempts--
                        }
                    }
                    
                    // Wall Bounce
                    if (ballPos.value.x < 0f || ballPos.value.x > 1f) {
                        ballVelocity.value = ballVelocity.value.copy(x = -ballVelocity.value.x)
                    }
                }
            }
        }
    }
    
    // Auto-reset message handling
    LaunchedEffect(gameMessage) {
        if (gameMessage.isNotEmpty()) {
            delay(1500)
            ballPos.value = Offset(0.5f, 0.8f)
            ballVelocity.value = Offset.Zero
            gameMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Penalties") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    Text("Goles: $score  |  Tiros: $attempts", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { 
                        score = 0
                        attempts = 5
                        ballPos.value = Offset(0.5f, 0.8f)
                        isBallMoving.value = false
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF2E7D32)) // Grass Green
        ) {
            if (attempts <= 0 && !isBallMoving.value) {
                // Game Over Overlay
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).zIndex(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡Juego Terminado!", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Puntaje Final: $score", color = Color.Yellow, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { 
                             score = 0
                             attempts = 5
                        }) {
                            Text("Jugar de Nuevo")
                        }
                    }
                }
            }
            
            // Interaction Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                         // Simple SWIPE detection
                         detectDragGestures(
                             onDragEnd = { /* No-op */ },
                             onDrag = { _, dragAmount -> 
                                  if (!isBallMoving.value) {
                                      // If dragging UP
                                      if (dragAmount.y < -5) {
                                          val speedX = dragAmount.x * 0.001f
                                          val speedY = dragAmount.y * 0.001f
                                          var vel = Offset(speedX, speedY).copy(y = -0.02f)
                                          vel = vel.copy(x = (dragAmount.x * 0.0005f).coerceIn(-0.02f, 0.02f))
                                          ballVelocity.value = vel
                                          isBallMoving.value = true
                                      }
                                  }
                             }
                         )
                    }
            ) {
                gameMessage.let {
                    if (it.isNotEmpty()) {
                        Text(
                            text = it,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center).zIndex(1f)
                        )
                    }
                }
                
                // Optimized Drawing using drawBehind inside a spacer/box
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val w = size.width
                            val h = size.height
                            
                            // 1. GRASS FIELD (Vibrant Stripes)
                            val stripeHeight = h / 12 // Bigger stripes
                            val darkGrass = Color(0xFF1B5E20) // Deep Arcade Green
                            val lightGrass = Color(0xFF2E7D32) // Lighter Arcade Green
                            
                            drawRect(color = darkGrass, size = size) // Base
                            
                            for (i in 0 until 12 step 2) {
                                drawRect(
                                    color = lightGrass,
                                    topLeft = Offset(0f, i * stripeHeight),
                                    size = Size(w, stripeHeight)
                                )
                            }
                            
                            // 2. FIELD LINES (Glowy White)
                            val lineWidth = 6f
                            val lineColor = Color.White.copy(alpha = 0.9f)
                            
                            // Penalty Box (Arcade style)
                            val penaltyBoxWidth = w * 0.7f
                            val penaltyBoxHeight = h * 0.25f
                            
                            drawRect(
                                color = lineColor,
                                style = Stroke(width = lineWidth),
                                topLeft = Offset((w - penaltyBoxWidth) / 2, 0f),
                                size = Size(penaltyBoxWidth, penaltyBoxHeight)
                            )
                            
                            // Goal Area (Small box inside)
                            val goalAreaWidth = w * 0.4f
                            val goalAreaHeight = h * 0.1f
                            drawRect(
                                color = lineColor,
                                style = Stroke(width = lineWidth),
                                topLeft = Offset((w - goalAreaWidth) / 2, 0f),
                                size = Size(goalAreaWidth, goalAreaHeight)
                            )
                            
                            // Penalty Spot
                            drawCircle(
                                color = lineColor,
                                radius = 10f,
                                center = Offset(w / 2, h * 0.18f)
                            )
                            
                            // Arc (D)
                            drawArc(
                                color = lineColor,
                                startAngle = 0f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset((w / 2) - (w * 0.15f), penaltyBoxHeight - (w * 0.15f)),
                                size = Size(w * 0.3f, w * 0.3f),
                                style = Stroke(width = lineWidth)
                            )
                            
                            // 3. GOAL NET (Crosshatch)
                            val goalX = (w - (w * 0.8f)) / 2
                            val goalY = h * 0.02f
                            val goalW = w * 0.8f
                            val goalH = h * 0.08f
                            
                            // Net backing
                            drawRect(color = Color.Black.copy(alpha=0.3f), topLeft = Offset(goalX, goalY), size = Size(goalW, goalH))
                            
                            // Net lines
                            val netSpacing = 20f
                            // Vertical
                            for (nx in 0 until (goalW / netSpacing).toInt()) {
                                drawLine(
                                    color = Color.White.copy(alpha=0.3f),
                                    start = Offset(goalX + nx * netSpacing, goalY),
                                    end = Offset(goalX + nx * netSpacing, goalY + goalH),
                                    strokeWidth = 2f
                                )
                            }
                            // Horizontal
                            for (ny in 0 until (goalH / netSpacing).toInt()) {
                                drawLine(
                                    color = Color.White.copy(alpha=0.3f),
                                    start = Offset(goalX, goalY + ny * netSpacing),
                                    end = Offset(goalX + goalW, goalY + ny * netSpacing),
                                    strokeWidth = 2f
                                )
                            }
                            
                            // Goal Posts
                            drawRect(
                                color = Color.White, 
                                topLeft = Offset(goalX, goalY), 
                                size = Size(goalW, goalH),
                                style = Stroke(width = 10f)
                            )
                        }
                )
                           
                            // Simple Crosshatch
                            val step = 20f
                            // Diagonals 1
                            // (Simplified for performance: just vertical lines behind)
                             for (i in 1..8) {
                                 drawLine(netColor, 
                                    start = Offset(w * 0.1f + (w * 0.8f * i / 9), h * 0.02f),
                                    end = Offset(w * 0.1f + (w * 0.8f * i / 9), h * 0.1f),
                                    strokeWidth = 2f
                                 )
                            }
                             // Horizontal lines
                            for (i in 1..3) {
                                 val y = h*0.02f + (h*0.08f * i/4)
                                 drawLine(netColor, start = Offset(w*0.1f, y), end = Offset(w*0.9f, y))
                            }
                            
                            // Draw Keeper
                            val keeperColor = Color(0xFFD32F2F) // Red Jersey
                            // Body
                            val keeperW = w * 0.16f
                            val keeperH = h * 0.06f
                            val keeperLeft = w * kX - keeperW/2
                            
                            drawRect(
                                color = keeperColor, 
                                topLeft = Offset(keeperLeft, h * 0.1f),
                                size = Size(keeperW, keeperH)
                            )
                            // Head
                            drawCircle(Color(0xFFFFCC80), radius = 15f, center = Offset(w * kX, h * 0.09f))
                            
                            // Draw Ball (With classic Texture)
                            val ballCenter = Offset(w * bPos.x, h * bPos.y)
                            val ballRadius = 25f
                            drawCircle(Color.White, radius = ballRadius, center = ballCenter)
                            // Hexagon patterns roughly
                            drawCircle(Color.Black, radius = ballRadius, center = ballCenter, style = Stroke(width = 2f))
                            drawCircle(Color.Black, radius = ballRadius/2, center = ballCenter, style = Stroke(width = 1f))
                        }
                 
                )
            }
            
            Text("¡Desliza rápido hacia arriba para chutar!", 
                color = Color.White.copy(alpha = 0.8f), 
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp))
        }
    }
}
