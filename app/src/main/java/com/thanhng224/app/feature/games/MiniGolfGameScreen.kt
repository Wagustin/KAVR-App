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
                        onDragStart = { offset ->
                             if (ballVelocity == Offset.Zero && !hasWon) {
                                  // Check if touch is near ball
                                  val w = size.width
                                  val h = size.height
                                  val ballPixel = Offset(w * ballPos.x, h * ballPos.y)
                                  val dist = (offset - ballPixel).getDistance()
                                  
                                  if (dist < w * 0.15f) { // Generous hit area
                                      dragStart = offset // Start exactly where user touched? Or snap to ball?
                                      // Slingshot feel: Start is the anchor.
                                      // Actually, for "Drag from ball", let's make the ball the anchor.
                                      dragStart = ballPixel
                                      dragCurrent = offset
                                  }
                             }
                        },
                        onDragEnd = {
                             if (dragStart != null && dragCurrent != null && !hasWon) {
                                 // Slingshot: Vector is Start (Ball) - Current (Finger)
                                 // So if I drag DOWN, Vector is UP (Launch forward).
                                 val dragVec = dragStart!! - dragCurrent!! 
                                 
                                 // Scale power
                                 val powerX = dragVec.x * 0.00015f // Tweaked Scaling
                                 val powerY = dragVec.y * 0.00015f
                                 
                                 val rawVel = Offset(powerX, powerY)
                                 val mag = rawVel.getDistance()
                                 if (mag > 0.002f) { // Min power threshold
                                     ballVelocity = if (mag > MAX_POWER) {
                                         rawVel / mag * MAX_POWER
                                     } else {
                                         rawVel
                                     }
                                 }
                             }
                             dragStart = null
                             dragCurrent = null
                        },
                        onDrag = { change, _ -> 
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
                
                // CHECKERED BACKGROUND
                val tileSize = w / 10f // 10 tiles wide
                val rows = (h / tileSize).toInt() + 1
                val cols = 10
                
                for(row in 0 until rows) {
                    for(col in 0 until cols) {
                        val color = if ((row + col) % 2 == 0) {
                            Color(0xFF4CAF50) // Green 500
                        } else {
                            Color(0xFF66BB6A) // Green 400 (Ligher)
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(col * tileSize, row * tileSize),
                            size = Size(tileSize, tileSize)
                        )
                    }
                }

                // Draw Hole
                drawCircle(
                    color = Color(0xFF1B5E20), // Dark hole
                    radius = w * holeRadius,
                    center = Offset(w * currentLevel.holePos.x, h * currentLevel.holePos.y)
                )
                 drawCircle(
                    color = Color.Black.copy(alpha=0.5f), 
                    radius = w * holeRadius * 0.8f,
                    center = Offset(w * currentLevel.holePos.x, h * currentLevel.holePos.y)
                )
                
                // Draw Walls
                currentLevel.walls.forEach { wall ->
                    val wx = w * wall.x
                    val wy = h * wall.y
                    val ww = w * wall.w
                    val wh = h * wall.h
                    
                    // Shadow
                     drawRect(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset(wx + 5f, wy + 5f),
                        size = Size(ww, wh)
                    )

                    drawRect(
                        color = Color(0xFFE0E0E0), // Concrete/White block
                        topLeft = Offset(wx, wy),
                        size = Size(ww, wh)
                    )
                     drawRect(
                        color = Color(0xFF9E9E9E), // Border
                        topLeft = Offset(wx, wy),
                        size = Size(ww, wh),
                        style = Stroke(width = 3f)
                    )
                }

                // Drag Line (Aiming)
                // Draw BEHIND ball
                if (dragStart != null && dragCurrent != null) {
                    val dragVec = dragCurrent!! - dragStart!!
                    // Invert: Pull back to shoot forward? Or Drag to shoot?
                    // User said "drag from ball". Usually this implies Slingshot.
                    // Drag BACK means shoot FORWARD.
                    
                    val ballPixelPos = Offset(w * ballPos.x, h * ballPos.y)
                    
                    // Valid drag visualization
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = ballPixelPos,
                        end = ballPixelPos + dragVec,
                        strokeWidth = 8f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    // Arrow head?
                }

                // Draw Ball
                val ballPixelPos = Offset(w * ballPos.x, h * ballPos.y)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f), // Shadow
                    radius = w * 0.025f,
                    center = ballPixelPos + Offset(3f, 3f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.025f,
                    center = ballPixelPos
                )
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
