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
        0 -> 0.01f
        1 -> 0.015f
        else -> 0.025f
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
                keeperX.floatValue += keeperSpeed * keeperDirection.floatValue * delta
                if (keeperX.floatValue > 0.8f || keeperX.floatValue < 0.2f) keeperDirection.floatValue *= -1
                
                // Randomly change direction
                if (Random.nextFloat() < (0.05f * delta)) keeperDirection.floatValue *= -1
                
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
                            
                            // State Reads
                            val bPos = ballPos.value
                            val kX = keeperX.floatValue
                            
                            // Draw Field
                            drawRect(color = Color.White, style = Stroke(width = 5f), topLeft = Offset(50f, 50f), size = Size(w - 100f, h - 100f)) 
                            
                            // Draw Goal
                            drawRect(
                                color = Color.White, 
                                topLeft = Offset(w * 0.1f, h * 0.05f), 
                                size = Size(w * 0.8f, h * 0.1f),
                                style = Stroke(width = 8f)
                            )
                            
                            // Draw Net
                            val netColor = Color(0x80FFFFFF)
                            for (i in 1..5) {
                                 drawLine(netColor, 
                                    start = Offset(w * 0.1f + (w * 0.8f * i / 6), h * 0.05f),
                                    end = Offset(w * 0.1f + (w * 0.8f * i / 6), h * 0.15f)
                                 )
                            }
                            
                            // Draw Keeper
                            val keeperColor = Color(0xFFD32F2F)
                            drawRect(
                                color = keeperColor, 
                                topLeft = Offset(w * (kX - 0.08f), h * 0.12f),
                                size = Size(w * 0.16f, h * 0.06f)
                            )
                            
                            // Draw Ball
                            drawCircle(
                                color = Color.White,
                                radius = 25f,
                                center = Offset(w * bPos.x, h * bPos.y)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 25f,
                                center = Offset(w * bPos.x, h * bPos.y),
                                style = Stroke(width = 2f)
                            )
                        }
                )
            }
            
            Text("¡Desliza rápido hacia arriba para chutar!", 
                color = Color.White.copy(alpha = 0.8f), 
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp))
        }
    }
}
