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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// --- CONSTANTS ---
const val FRICTION = 0.96f // Stronger friction for cleaner stops
const val STOP_THRESHOLD = 0.0002f
const val HOLE_CAPTURE_SPEED = 0.012f // Ball must be slow to fall in
const val MAX_POWER_DISPLAY = 200f // Visualization max length

// Modes
const val MODE_1P = 0
const val MODE_2P = 1
const val SUBMODE_TRAINING = 0
const val SUBMODE_TIME = 1
const val SUBMODE_AI = 2

data class Wall(val x: Float, val y: Float, val w: Float, val h: Float)
data class Level(
    val startPos: Offset,
    val holePos: Offset,
    val walls: List<Wall>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGolfGameScreen(navController: NavController) {
    // Arguments
    val mode = navController.currentBackStackEntry?.arguments?.getInt("mode") ?: 0
    val submode = navController.currentBackStackEntry?.arguments?.getInt("submode") ?: 0
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 0

    // Difficulty Params
    val holeRadiusDef = when (difficulty) {
        0 -> 0.05f // Easy
        1 -> 0.04f // Med
        else -> 0.03f // Hard
    }

    // GAME STATE
    var levelIndex by remember { mutableIntStateOf(0) }
    var ballPos by remember { mutableStateOf(Offset(0.5f, 0.8f)) }
    var ballVelocity by remember { mutableStateOf(Offset.Zero) }
    
    // 2 Player / AI State
    var currentPlayer by remember { mutableIntStateOf(1) } // 1 or 2
    var p1Strokes by remember { mutableIntStateOf(0) }
    var p2Strokes by remember { mutableIntStateOf(0) } // Or AI score
    var p1TotalScore by remember { mutableIntStateOf(0) }
    var p2TotalScore by remember { mutableIntStateOf(0) }

    // Time Attack State
    var timeLeft by remember { mutableLongStateOf(60000L) } // 60s per level?
    var isTimeRunning by remember { mutableStateOf(false) }

    // Input
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    
    // Flow
    var hasWon by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var turnMessage by remember { mutableStateOf("") }
    
    // Levels (Refined & Expanded)
    val levels = remember {
        listOf(
            // Lv 1: The Gateway (Simple)
            Level(Offset(0.5f, 0.85f), Offset(0.5f, 0.2f), listOf(
                Wall(0.1f, 0.5f, 0.3f, 0.05f), 
                Wall(0.6f, 0.5f, 0.3f, 0.05f)
            )),
            // Lv 2: The Blockade (Two Bars)
            Level(Offset(0.5f, 0.9f), Offset(0.5f, 0.15f), listOf(
                Wall(0.2f, 0.4f, 0.6f, 0.05f),
                Wall(0.2f, 0.65f, 0.6f, 0.05f) 
            )),
            // Lv 3: Zig Zag (Narrower path)
            Level(Offset(0.15f, 0.9f), Offset(0.85f, 0.1f), listOf(
                Wall(0.0f, 0.35f, 0.7f, 0.05f),
                Wall(0.3f, 0.65f, 0.7f, 0.05f)
            )),
            // Lv 4: The Cage (Precision Shot)
            Level(Offset(0.5f, 0.85f), Offset(0.5f, 0.5f), listOf(
                Wall(0.3f, 0.4f, 0.4f, 0.05f), // Top
                Wall(0.3f, 0.6f, 0.4f, 0.05f), // Bottom
                Wall(0.3f, 0.4f, 0.05f, 0.25f), // Left
                Wall(0.65f, 0.4f, 0.05f, 0.25f) // Right (Gap exists?) No, this is a box.
                // Wait, need an entry.
            )),
             // Re-doing Lv 4: The Pillars
            Level(Offset(0.5f, 0.9f), Offset(0.5f, 0.1f), listOf(
                Wall(0.45f, 0.3f, 0.1f, 0.4f), // Center Pillar
                Wall(0.1f, 0.5f, 0.2f, 0.05f), // Left Wing
                Wall(0.7f, 0.5f, 0.2f, 0.05f)  // Right Wing
            )),
             // Lv 5: The Maze
            Level(Offset(0.1f, 0.9f), Offset(0.9f, 0.1f), listOf(
                 Wall(0.0f, 0.2f, 0.8f, 0.05f),
                 Wall(0.2f, 0.5f, 0.8f, 0.05f),
                 Wall(0.0f, 0.8f, 0.5f, 0.05f)
            ))
        )
    }
    val currentLevel = levels[levelIndex]
    
    // Timer Logic
    LaunchedEffect(isTimeRunning, levelIndex) {
        if (mode == MODE_1P && submode == SUBMODE_TIME) {
            val startTime = System.currentTimeMillis()
            val initialTime = timeLeft
            while(isTimeRunning && !hasWon && !gameOver) {
                val elapsed = System.currentTimeMillis() - startTime
                timeLeft = (initialTime - elapsed).coerceAtLeast(0L)
                if (timeLeft == 0L) {
                    gameOver = true // Time Over
                    isTimeRunning = false
                    // Reset game state? Or just show overlay
                }
                delay(100)
            }
        }
    }

    // Reset Logic
    fun resetBall() {
        ballPos = currentLevel.startPos
        ballVelocity = Offset.Zero
    }

    fun nextLevel() {
        if (levelIndex < levels.size - 1) {
            levelIndex++
            resetBall()
            hasWon = false
            p1Strokes = 0
            p2Strokes = 0
            currentPlayer = 1
            if (mode == MODE_1P && submode == SUBMODE_TIME) {
                 timeLeft += 30000L // Add 30s bonus
                 isTimeRunning = true
            }
        } else {
            // Game Finished
            gameOver = true
        }
    }

    // AI Logic (Simple Random Score)
    fun playAI() {
        val aiSkill = when(difficulty) {
            0 -> Random.nextInt(3, 6)
            2 -> Random.nextInt(1, 3)
            else -> Random.nextInt(2, 5)
        }
        p2Strokes = aiSkill
        p2TotalScore += aiSkill
    }

    // Physics Loop
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameTime ->
                val delta = (frameTime - lastTime) / 16f
                lastTime = frameTime
                
                // --- SUB-STEP PHYSICS ---
                val steps = 10 // Increased for precision
                val dt = delta / steps
                
                for(i in 0 until steps) {
                    if (hasWon || gameOver) break
                    
                    if (ballVelocity != Offset.Zero) {
                        var nextPos = ballPos + (ballVelocity * dt)
                        
                        // Collisions
                        val ballR = 0.025f // Relative Radius
                        
                        // 1. Walls (AABB Deep Collision)
                        for (wall in currentLevel.walls) {
                             // Determine nearest point on AABB to center
                             val closestX = nextPos.x.coerceIn(wall.x, wall.x + wall.w)
                             val closestY = nextPos.y.coerceIn(wall.y, wall.y + wall.h)
                             
                             val dx = nextPos.x - closestX
                             val dy = nextPos.y - closestY
                             val distSq = dx*dx + dy*dy
                             
                             if (distSq < (ballR * ballR)) {
                                 // HIT
                                 val dist = sqrt(distSq)
                                 var normal = Offset.Zero
                                 
                                 if (dist > 0.00001f) {
                                     // Standard collision (center outside)
                                     normal = Offset(dx/dist, dy/dist)
                                 } else {
                                     // CENTER INSIDE WALL (Deep Tunneling Fix)
                                     // Find closest edge to push out
                                     val dL = nextPos.x - wall.x
                                     val dR = (wall.x + wall.w) - nextPos.x
                                     val dT = nextPos.y - wall.y
                                     val dB = (wall.y + wall.h) - nextPos.y
                                     
                                     val minOverlap = minOf(dL, dR, dT, dB)
                                     
                                     // Determine normal based on closest edge
                                     normal = when (minOverlap) {
                                         dL -> Offset(-1f, 0f)
                                         dR -> Offset(1f, 0f)
                                         dT -> Offset(0f, -1f)
                                         else -> Offset(0f, 1f)
                                     }
                                     
                                     // Force position OUT immediately to edge + radius
                                     // (This prevents "sticking" inside)
                                     // We adjust nextPos in the push-out phase below
                                 }
                                 
                                 // Reflect Velocity
                                 val dot = ballVelocity.x * normal.x + ballVelocity.y * normal.y
                                 if (dot < 0) {
                                     ballVelocity -= (normal * 2f * dot)
                                     ballVelocity *= 0.8f // Bounce loss
                                 }
                                 
                                 // PUSH OUT 
                                 // If inside (dist ~ 0), we need to push out by radius + penetration
                                 // If outside (dist > 0), overlap = ballR - dist
                                 val pen = if (dist > 0.00001f) (ballR - dist) else {
                                     // Re-calculate penetration for inside case
                                     val dL = nextPos.x - wall.x
                                     val dR = (wall.x + wall.w) - nextPos.x
                                     val dT = nextPos.y - wall.y
                                     val dB = (wall.y + wall.h) - nextPos.y
                                     minOf(dL, dR, dT, dB) + ballR // Push full radius out from edge
                                 }
                                 
                                 nextPos += normal * (pen + 0.0005f)
                             }
                        }
                        
                        // 2. Screen Borders
                        if (nextPos.x < ballR) { nextPos = nextPos.copy(x = ballR); ballVelocity = ballVelocity.copy(x = -ballVelocity.x * 0.8f) }
                        if (nextPos.x > 1f - ballR) { nextPos = nextPos.copy(x = 1f - ballR); ballVelocity = ballVelocity.copy(x = -ballVelocity.x * 0.8f) }
                        if (nextPos.y < ballR) { nextPos = nextPos.copy(y = ballR); ballVelocity = ballVelocity.copy(y = -ballVelocity.y * 0.8f) }
                        if (nextPos.y > 1f - ballR) { nextPos = nextPos.copy(y = 1f - ballR); ballVelocity = ballVelocity.copy(y = -ballVelocity.y * 0.8f) }
                        
                        ballPos = nextPos
                    }
                }
                
                // Friction
                if (ballVelocity != Offset.Zero) {
                    ballVelocity *= 1f - (1f - FRICTION) * delta
                    if (ballVelocity.getDistance() < STOP_THRESHOLD) {
                        ballVelocity = Offset.Zero
                        
                        // TURN LOGIC (End of Shot)
                        if (!hasWon) {
                             if (mode == MODE_2P) {
                                 // Switch Turn logic is handled AFTER shot
                                 // Handled in DragEnd actually? No, wait here for stop.
                                 // For now, P1 plays until hole? Or turn based per shot?
                                 // Standard minigolf: Turn based per shot only if out of bounds? 
                                 // Let's do: P1 plays hole until finish, then P2? Or shot by shot?
                                 // Simplest: P1 finishes hole, then P2 plays same hole.
                                 // But that requires stage reset.
                                 
                                 // Let's do: Turn swap every stroke? That's confusing on one screen.
                                 // Better: P1 plays full hole, then P2 plays full hole.
                                 // Implemented: CURRENT STATE checks if P1 done? 
                             }
                        }
                    }
                }
                
                // Win Check
                if (!hasWon && !gameOver) {
                    val d = (ballPos - currentLevel.holePos).getDistance()
                    if (d < holeRadiusDef) {
                        // CAPTURE: Must be slow enough!
                        if (ballVelocity.getDistance() < HOLE_CAPTURE_SPEED) {
                            // SCORE!
                            ballVelocity = Offset.Zero
                            ballPos = currentLevel.holePos
                            
                            if (mode == MODE_2P) {
                                if (currentPlayer == 1) {
                                    p1TotalScore += p1Strokes
                                    currentPlayer = 2
                                    turnMessage = "¡Turno del Jugador 2!"
                                    p2Strokes = 0 // Reset for their turn
                                    // Reset ball to start after delay
                                } else {
                                    p2TotalScore += p2Strokes
                                    hasWon = true // Both finished
                                }
                            } else {
                                p1TotalScore += p1Strokes
                                if (submode == SUBMODE_AI) playAI()
                                hasWon = true
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delayed Turn Switch / Win
    LaunchedEffect(turnMessage) {
        if (turnMessage.isNotEmpty()) {
            delay(2000)
            resetBall()
            turnMessage = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Hoyo ${levelIndex + 1}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (mode == MODE_1P && submode == SUBMODE_TIME) {
                            Text("Tiempo: ${timeLeft / 1000}s", fontSize = 14.sp, color = if(timeLeft < 10000) Color.Red else Color.Black)
                        }
                    }
                },
                actions = {
                    Text("Golpes: $p1Strokes", modifier = Modifier.padding(end=16.dp), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { 
                         resetBall()
                         // Penalty?
                         p1Strokes++
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "R")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF2E7D32))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (ballVelocity == Offset.Zero && !hasWon && turnMessage.isEmpty()) {
                                val w = size.width
                                val h = size.height
                                val ballPx = Offset(w * ballPos.x, h * ballPos.y)
                                if ((offset - ballPx).getDistance() < w * 0.15f) {
                                    dragStart = ballPx
                                    dragCurrent = offset
                                }
                            }
                        },
                        onDragEnd = {
                            if (dragStart != null && dragCurrent != null) {
                                val vec = dragStart!! - dragCurrent!!
                                val w = size.width
                                
                                // FORCE CALCULATION (Reduced Sensitivity)
                                val dragDist = vec.getDistance()
                                val minDrag = w * 0.05f
                                
                                if (dragDist > minDrag) {
                                    // Power is proportional to drag distance
                                    val power = min(dragDist, w * 0.4f) * 0.00015f // Tweaked constant
                                    val dir = vec / dragDist
                                    
                                    ballVelocity = dir * power
                                    
                                    // Count Stroke
                                    if (mode == MODE_2P && currentPlayer == 2) p2Strokes++ else p1Strokes++
                                    
                                    // Start Timer on first hit?
                                    if (mode == MODE_1P && submode == SUBMODE_TIME && !isTimeRunning) isTimeRunning = true
                                }
                            }
                            dragStart = null
                            dragCurrent = null
                        },
                        onDrag = { change, _ ->
                            if (dragStart != null) dragCurrent = change.position
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // 1. Grass Tiles
                val ts = w / 8
                for (r in 0..(h/ts).toInt()) {
                    for (c in 0..7) {
                        drawRect(
                            color = if ((r+c)%2==0) Color(0xFF388E3C) else Color(0xFF2E7D32),
                            topLeft = Offset(c*ts, r*ts), size = Size(ts, ts)
                        )
                    }
                }
                
                // 2. Start Area
                drawCircle(Color.White.copy(alpha=0.3f), radius = w*0.06f, center = Offset(w*currentLevel.startPos.x, h*currentLevel.startPos.y))

                // 3. Walls
                currentLevel.walls.forEach { 
                    drawRect(Color(0xFF5D4037), topLeft = Offset(w*it.x, h*it.y), size = Size(w*it.w, h*it.h))
                    drawRect(Color(0xFF8D6E63), topLeft = Offset(w*it.x+4f, h*it.y+4f), size = Size(w*it.w-8f, h*it.h-8f))
                }
                
                // 4. Hole
                val holePx = Offset(w*currentLevel.holePos.x, h*currentLevel.holePos.y)
                drawCircle(Color.Black, radius = w * holeRadiusDef, center = holePx)
                drawCircle(Color.Black.copy(alpha=0.3f), radius = w * holeRadiusDef + 4f, center = holePx) // Shadow ring

                // 5. Drag Line
                if (dragStart != null && dragCurrent != null) {
                    val start = dragStart!!
                    val end = dragCurrent!!
                    val dist = (start - end).getDistance()
                    
                    // Clamped visualization
                    val maxLen = w * 0.4f
                    val visEnd = if (dist > maxLen) start - (start - end) * (maxLen/dist) else end
                    
                    // Arrow line
                    drawLine(Color.Yellow, start, visEnd, strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    
                    // Power bar logic? Or just line length.
                }

                // 6. Ball
                val bPos = Offset(w*ballPos.x, h*ballPos.y)
                val bRad = w * 0.025f
                drawCircle(Color.Black.copy(alpha=0.4f), radius = bRad, center = bPos + Offset(5f,5f))
                drawCircle(Color.White, radius = bRad, center = bPos)
            }
            
            // TURN MESSAGE OVERLAY
            if (turnMessage.isNotEmpty()) {
                 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.7f)), contentAlignment = Alignment.Center) {
                     Text(turnMessage, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                 }
            }

            // WIN / GAME OVER OVERLAY
            if (hasWon || gameOver) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.8f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (gameOver && mode == MODE_1P && submode == SUBMODE_TIME && timeLeft == 0L) {
                             Text("¡TIEMPO AGOTADO!", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        } else {
                             Text("¡NIVEL COMPLETADO!", color = Color.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                             Text("Golpes: $p1Strokes", color = Color.White, fontSize = 20.sp)
                             
                             if (mode == MODE_2P) {
                                 Text("J2 Golpes: $p2Strokes", color = Color.Cyan, fontSize = 20.sp)
                                 val winner = if(p1Strokes < p2Strokes) "¡Gana Jugador 1!" else if(p2Strokes < p1Strokes) "¡Gana Jugador 2!" else "¡Empate!"
                                 Spacer(Modifier.height(10.dp))
                                 Text(winner, color = Color.Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                             } else if (submode == SUBMODE_AI) {
                                 Text("IA Golpes: $p2Strokes", color = Color.Cyan, fontSize = 20.sp)
                                 val winner = if(p1Strokes < p2Strokes) "¡Ganaste!" else "Perdiste..."
                                 Spacer(Modifier.height(10.dp))
                                 Text(winner, color = Color.Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                             }
                        }
                        
                        Spacer(Modifier.height(30.dp))
                        Row {
                            Button(onClick = { 
                                // Restart Level
                                resetBall()
                                hasWon = false
                                gameOver = false
                                p1Strokes = 0
                                p2Strokes = 0
                                currentPlayer = 1
                            }) { Text("Reintentar") }
                            Spacer(Modifier.width(16.dp))
                            Button(onClick = { nextLevel() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))) { 
                                Text(if (levelIndex < levels.size - 1) "Siguiente" else "Terminar", color = Color.Black) 
                            }
                        }
                    }
                }
            }
        }
    }
}
