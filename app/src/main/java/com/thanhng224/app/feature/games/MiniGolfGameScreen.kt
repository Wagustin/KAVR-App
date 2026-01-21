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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- PHYSICS CONSTANTS ---
const val FRICTION = 0.98f
const val MAX_POWER = 0.05f

data class Wall(val x: Float, val y: Float, val w: Float, val h: Float)

data class Level(
    val startPos: Offset,
    val holePos: Offset,
    val walls: List<Wall>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGolfGameScreen(navController: NavController) {
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 0
    val holeRadius = when (difficulty) {
        0 -> 0.05f
        1 -> 0.04f
        else -> 0.03f
    }

    var levelIndex by remember { mutableIntStateOf(0) }
    var ballPos by remember { mutableStateOf(Offset(0.5f, 0.8f)) }
    var ballVelocity by remember { mutableStateOf(Offset.Zero) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var hasWon by remember { mutableStateOf(false) }

    // LEVELS
    val levels = remember {
        listOf(
            // Lv 1: Simple
            Level(
                startPos = Offset(0.5f, 0.8f),
                holePos = Offset(0.5f, 0.2f),
                walls = emptyList()
            ),
            // Lv 2: Middle Block
            Level(
                startPos = Offset(0.5f, 0.8f),
                holePos = Offset(0.5f, 0.15f),
                walls = listOf(Wall(0.3f, 0.4f, 0.4f, 0.05f))
            ),
            // Lv 3: Zig Zag
            Level(
                startPos = Offset(0.1f, 0.9f),
                holePos = Offset(0.9f, 0.1f),
                walls = listOf(
                    Wall(0.0f, 0.3f, 0.7f, 0.05f),
                    Wall(0.3f, 0.6f, 0.7f, 0.05f)
                )
            )
        )
    }

    val currentLevel = levels[levelIndex]

    // Reset Ball on Level Change
    LaunchedEffect(levelIndex) {
        ballPos = currentLevel.startPos
        ballVelocity = Offset.Zero
        hasWon = false
    }

    // GAME LOOP - Optimized
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameTime ->
                val delta = (frameTime - lastTime) / 16f
                lastTime = frameTime
                
                if (ballVelocity != Offset.Zero && !hasWon) {
                    // Apply Velocity
                    var nextPos = ballPos + (ballVelocity * delta)
                    
                    // Wall Collisions (Screen Edges)
                    if (nextPos.x < 0.05f || nextPos.x > 0.95f) {
                        ballVelocity = ballVelocity.copy(x = -ballVelocity.x)
                        nextPos = nextPos.copy(x = ballPos.x) // Simple resolve
                    }
                    if (nextPos.y < 0.05f || nextPos.y > 0.95f) {
                         ballVelocity = ballVelocity.copy(y = -ballVelocity.y)
                         nextPos = nextPos.copy(y = ballPos.y)
                    }

                    // Obstacle Collisions
                    // Simple AABB vs Point check mechanism
                    currentLevel.walls.forEach { wall ->
                        if (nextPos.x > wall.x && nextPos.x < wall.x + wall.w &&
                            nextPos.y > wall.y && nextPos.y < wall.y + wall.h) {
                                
                            val prevX = ballPos.x
                            // val prevY = ballPos.y // Unused but part of logic
                            
                            val inX = prevX > wall.x && prevX < wall.x + wall.w
                            
                            if (inX) {
                                ballVelocity = ballVelocity.copy(y = -ballVelocity.y)
                            } else {
                                ballVelocity = ballVelocity.copy(x = -ballVelocity.x)
                            }
                            nextPos = ballPos 
                        }
                    }

                    ballPos = nextPos
                    
                    // Friction
                    // Friction should be exponential decay or linear? Exponential `v *= 0.98` is standard but framerate dependent.
                    // With delta: v = v * pow(0.98, delta) roughly.
                    // For simplicity and A12, simple linear approx or just multiplier is fine if delta ~ 1.
                    val frictionFactor = 1f - (1f - FRICTION) * delta
                    ballVelocity *= frictionFactor
                    
                    // Stop if too slow
                    if (ballVelocity.getDistance() < 0.0005f) {
                        ballVelocity = Offset.Zero
                    }
                    
                    // Win Check (Hole)
                    val distToHole = (ballPos - currentLevel.holePos).getDistance()
                    if (distToHole < holeRadius * 0.8f && ballVelocity.getDistance() < 0.01f) { 
                        hasWon = true
                        ballVelocity = Offset.Zero
                        ballPos = currentLevel.holePos
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mini Golf - Nivel ${levelIndex + 1}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        // Reset Level
                        ballPos = currentLevel.startPos
                        ballVelocity = Offset.Zero
                        hasWon = false
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
                .background(Color(0xFF8BC34A)) // Grass
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { 
                             if (ballVelocity == Offset.Zero && !hasWon) {
                                  dragStart = it 
                                  dragCurrent = it
                             }
                        },
                        onDragEnd = {
                             if (dragStart != null && dragCurrent != null && !hasWon) {
                                 // Calculate Power Vector
                                 val dragVec = dragStart!! - dragCurrent!!
                                 // Convert pixels to screen ratio roughly
                                 // Let's assume Screen Width ~1080 -> 1.0f width. 
                                 // Factor 0.00005f
                                 val powerX = dragVec.x * 0.00008f
                                 val powerY = dragVec.y * 0.00008f
                                 
                                 // Cap power
                                 val rawVel = Offset(powerX, powerY)
                                 val mag = rawVel.getDistance()
                                 ballVelocity = if (mag > MAX_POWER) {
                                     rawVel / mag * MAX_POWER
                                 } else {
                                     rawVel
                                 }
                             }
                             dragStart = null
                             dragCurrent = null
                        },
                        onDrag = { change, dragAmount -> 
                            if (dragStart != null) {
                                dragCurrent = change.position
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw Hole
                drawCircle(
                    color = Color.Black,
                    radius = w * holeRadius,
                    center = Offset(w * currentLevel.holePos.x, h * currentLevel.holePos.y)
                )
                
                // Draw Walls
                val wallColor = Color(0xFF5D4037)
                val wallBorderColor = Color(0xFF3E2723)
                val woodDark = Color(0xFF3E2723)
                
                currentLevel.walls.forEach { wall ->
                    val wx = w * wall.x
                    val wy = h * wall.y
                    val ww = w * wall.w
                    val wh = h * wall.h
                    
                    drawRect(
                        color = wallColor, // Brown wood
                        topLeft = Offset(wx, wy),
                        size = Size(ww, wh)
                    )
                    
                    // Wood Grain (Simple lines)
                    val grainSpacing = 15f
                    val lines = (ww / grainSpacing).toInt()
                    for(i in 0..lines) {
                         val xOff = wx + i * grainSpacing
                         if(xOff < wx + ww) {
                             drawLine(woodDark, 
                                start = Offset(xOff, wy),
                                end = Offset(xOff, wy + wh),
                                strokeWidth = 2f
                             )
                         }
                    }

                    // Border
                    drawRect(
                         color = wallBorderColor, 
                         topLeft = Offset(wx, wy), 
                         size = Size(ww, wh), 
                         style = Stroke(width = 4f)
                    )
                }

                // Draw Ball
                drawCircle(
                    color = Color.White,
                    radius = w * 0.025f,
                    center = Offset(w * ballPos.x, h * ballPos.y)
                )

                // Drag Line (Aiming)
                if (dragStart != null && dragCurrent != null) {
                    // Draw line from ball... opposite to drag?
                    // Angry birds style: Drag back -> Shoot forward.
                    // The dragStart was supposedly on screen. 
                    // Let's just draw relative to ball.
                    
                    val dragVec = dragStart!! - dragCurrent!!
                    val endPoint = Offset(
                        w * ballPos.x + dragVec.x,
                        h * ballPos.y + dragVec.y
                    )
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(w * ballPos.x, h * ballPos.y),
                        end = endPoint,
                        strokeWidth = 5f
                    )
                }
            }
            
            if (hasWon) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡HOYO EN UNO! ⛳", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (levelIndex < levels.size - 1) {
                            Button(onClick = { levelIndex++ }) {
                                Text("Siguiente Nivel ->")
                            }
                        } else {
                            Text("¡Juego Completado!", color = Color.Yellow)
                            Button(onClick = { levelIndex = 0 }) {
                                Text("Volver a Empezar")
                            }
                        }
                    }
                }
            }
        }
    }
}
