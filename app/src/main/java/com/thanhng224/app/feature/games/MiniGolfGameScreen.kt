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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

// --- CONSTANTS ---
const val FRICTION = 0.97f // Slightly higher friction
const val STOP_THRESHOLD = 0.0001f
const val HOLE_CAPTURE_SPEED = 0.015f 
const val MAX_POWER_DISPLAY = 250f 

// Modes
const val MODE_1P = 0
const val MODE_2P = 1
const val SUBMODE_TRAINING = 0
const val SUBMODE_TIME = 1
const val SUBMODE_AI = 2

data class Wall(val x: Float, val y: Float, val w: Float, val h: Float)
data class Level(
    val name: String,
    val startPos: Offset,
    val holePos: Offset,
    val walls: List<Wall>,
    val par: Int = 3
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
        0 -> 0.05f 
        1 -> 0.04f 
        else -> 0.035f 
    }

    // GAME STATE
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var levelIndex by remember { mutableIntStateOf(0) }
    var ballPos by remember { mutableStateOf(Offset(0.5f, 0.8f)) }
    var ballVelocity by remember { mutableStateOf(Offset.Zero) }
    
    // Score
    var currentPlayer by remember { mutableIntStateOf(1) } // 1 or 2
    var p1Strokes by remember { mutableIntStateOf(0) }
    var p2Strokes by remember { mutableIntStateOf(0) } 
    var p1TotalScore by remember { mutableIntStateOf(0) }
    var p2TotalScore by remember { mutableIntStateOf(0) }

    // Time Attack
    var timeLeft by remember { mutableLongStateOf(60000L) }
    var isTimeRunning by remember { mutableStateOf(false) }

    // Input & Flow
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember { mutableStateOf<Offset?>(null) }
    var hasWon by remember { mutableStateOf(false) } // Level Complete
    var gameOver by remember { mutableStateOf(false) } // All Levels Done
    var turnMessage by remember { mutableStateOf("") }
    
    // --- LEVEL DESIGN (15 Levels) ---
    val levels = remember {
        listOf(
            // 1. Warm Up
            Level("Calentamiento", Offset(0.5f, 0.85f), Offset(0.5f, 0.2f), listOf(
                Wall(0.1f, 0.5f, 0.3f, 0.05f), 
                Wall(0.6f, 0.5f, 0.3f, 0.05f)
            ), par=2),
            
            // 2. The Gate
            Level("La Puerta", Offset(0.5f, 0.9f), Offset(0.5f, 0.15f), listOf(
                Wall(0.0f, 0.4f, 0.4f, 0.05f),
                Wall(0.6f, 0.4f, 0.4f, 0.05f)
            ), par=3),
            
            // 3. Zig Zag
            Level("Zig Zag", Offset(0.15f, 0.9f), Offset(0.85f, 0.1f), listOf(
                Wall(0.0f, 0.3f, 0.7f, 0.05f),
                Wall(0.3f, 0.6f, 0.7f, 0.05f)
            ), par=3),
            
            // 4. Narrow Pass
            Level("Pasillo Estrecho", Offset(0.5f, 0.9f), Offset(0.5f, 0.1f), listOf(
                Wall(0.3f, 0.3f, 0.1f, 0.4f),
                Wall(0.6f, 0.3f, 0.1f, 0.4f),
                Wall(0.0f, 0.5f, 0.25f, 0.05f),
                Wall(0.75f, 0.5f, 0.25f, 0.05f)
            ), par=4),
            
            // 5. The Box
            Level("La Caja", Offset(0.5f, 0.9f), Offset(0.5f, 0.5f), listOf(
                 Wall(0.3f, 0.4f, 0.4f, 0.02f), // Top
                 Wall(0.3f, 0.6f, 0.4f, 0.02f), // Bot
                 Wall(0.3f, 0.4f, 0.02f, 0.2f), // Left
                 Wall(0.7f, 0.4f, 0.02f, 0.22f) // Right (Gap?) No, solid box requires bounce
            ).let { 
                // Add Entrance Gap
                listOf(
                    Wall(0.3f, 0.35f, 0.4f, 0.02f), // Top Box
                    Wall(0.3f, 0.35f, 0.02f, 0.3f), // Left Box
                    Wall(0.68f, 0.35f, 0.02f, 0.3f), // Right Box
                    Wall(0.3f, 0.65f, 0.15f, 0.02f), // Bot Left
                    Wall(0.55f, 0.65f, 0.15f, 0.02f) // Bot Right (Gap in center)
                ) 
            }, par=3),

            // 6. Scattered
            Level("Dispersos", Offset(0.1f, 0.9f), Offset(0.9f, 0.1f), listOf(
                 Wall(0.2f, 0.2f, 0.1f, 0.1f),
                 Wall(0.5f, 0.4f, 0.1f, 0.1f),
                 Wall(0.7f, 0.7f, 0.1f, 0.1f),
                 Wall(0.3f, 0.6f, 0.1f, 0.1f)
            ), par=3),

            // 7. Slalom
            Level("Slalom", Offset(0.5f, 0.95f), Offset(0.5f, 0.05f), listOf(
                 Wall(0.0f, 0.2f, 0.6f, 0.03f),
                 Wall(0.4f, 0.4f, 0.6f, 0.03f),
                 Wall(0.0f, 0.6f, 0.6f, 0.03f),
                 Wall(0.4f, 0.8f, 0.6f, 0.03f)
            ), par=5),

            // 8. The Cross
            Level("La Cruz", Offset(0.1f, 0.9f), Offset(0.9f, 0.1f), listOf(
                 Wall(0.45f, 0.2f, 0.1f, 0.6f),
                 Wall(0.2f, 0.45f, 0.6f, 0.1f)
            ), par=3),

            // 9. Twin Rooms
            Level("Gemelos", Offset(0.2f, 0.8f), Offset(0.8f, 0.2f), listOf(
                 Wall(0.0f, 0.5f, 1.0f, 0.05f), // Divider
                 Wall(0.4f, 0.4f, 0.2f, 0.2f) // Center Block blocking middle path
            ), par=4),

            // 10. Pillars of Hercules
            Level("Hercules", Offset(0.5f, 0.9f), Offset(0.5f, 0.1f), listOf(
                 Wall(0.1f, 0.3f, 0.1f, 0.4f),
                 Wall(0.3f, 0.3f, 0.1f, 0.4f),
                 Wall(0.6f, 0.3f, 0.1f, 0.4f),
                 Wall(0.8f, 0.3f, 0.1f, 0.4f)
            ), par=4),
            
            // 11. The Grid
            Level("La Rejilla", Offset(0.5f, 0.95f), Offset(0.5f, 0.05f), listOf(
                 Wall(0.2f, 0.2f, 0.6f, 0.02f),
                 Wall(0.2f, 0.4f, 0.6f, 0.02f),
                 Wall(0.2f, 0.6f, 0.6f, 0.02f),
                 Wall(0.2f, 0.8f, 0.6f, 0.02f),
                 Wall(0.48f, 0.0f, 0.04f, 1.0f) // Vertical divider with gaps needed?
            ).subList(0,4), par=4), // Simplified, just bars
            
            // 12. Bunker
            Level("Bunker", Offset(0.5f, 0.5f), Offset(0.1f, 0.1f), listOf(
                 Wall(0.4f, 0.4f, 0.2f, 0.02f), // Box around start
                 Wall(0.4f, 0.58f, 0.2f, 0.02f),
                 Wall(0.4f, 0.4f, 0.02f, 0.2f),
                 Wall(0.58f, 0.4f, 0.02f, 0.2f)
            ), par=4), // Trap! Needs precise shot out

            // 13. Maze II
            Level("Laberinto II", Offset(0.1f, 0.1f), Offset(0.9f, 0.9f), listOf(
                 Wall(0.2f, 0.0f, 0.05f, 0.8f),
                 Wall(0.4f, 0.2f, 0.05f, 0.8f),
                 Wall(0.6f, 0.0f, 0.05f, 0.8f),
                 Wall(0.8f, 0.2f, 0.05f, 0.8f)
            ), par=6),

            // 14. Corner Pocket
            Level("Esquina", Offset(0.1f, 0.1f), Offset(0.9f, 0.9f), listOf(
                 Wall(0.8f, 0.8f, 0.2f, 0.02f), // Guarding corner
                 Wall(0.8f, 0.8f, 0.02f, 0.2f)
            ), par=3),
            
            // 15. The End
            Level("Final", Offset(0.5f, 0.9f), Offset(0.5f, 0.5f), listOf(
                 Wall(0.2f, 0.2f, 0.6f, 0.05f),
                 Wall(0.2f, 0.7f, 0.6f, 0.05f),
                 Wall(0.2f, 0.2f, 0.05f, 0.5f),
                 Wall(0.75f, 0.2f, 0.05f, 0.5f),
                 // Inner Obstacles
                 Wall(0.4f, 0.4f, 0.2f, 0.2f)
            ), par=5)
        )
    }
    
    // Ensure index valid
    if (levelIndex >= levels.size) levelIndex = 0
    val currentLevel = levels[levelIndex]
    
    // Timer
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
                 timeLeft += 20000L // +20s bonus
                 isTimeRunning = true
            }
        } else {
            gameOver = true
        }
    }

    fun playAI() {
        // AI Logic based on difficulty/par
        val baseStrokes = currentLevel.par
        val variance = when(difficulty) {
            0 -> Random.nextInt(0, 3) // Easy: Par + 0..2
            1 -> Random.nextInt(-1, 2) // Med: Par -1..1
            else -> Random.nextInt(-2, 1) // Hard: Par -2..0
        }
        val score = (baseStrokes + variance).coerceAtLeast(1)
        p2Strokes = score
        p2TotalScore += score
    }

    // Physics Loop (Pixel Correct)
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameTime ->
                val delta = (frameTime - lastTime) / 16f
                lastTime = frameTime
                
                if (screenSize.width > 0 && screenSize.height > 0) {
                    val w = screenSize.width.toFloat()
                    val h = screenSize.height.toFloat()
                    
                    // Convert State to Pixels
                    var ballPx = Offset(ballPos.x * w, ballPos.y * h)
                    var velPx = Offset(ballVelocity.x * w, ballVelocity.y * h)
                    val ballRadiusPx = w * 0.025f 
                    
                    // Sub-steps
                    val steps = 16
                    val dt = delta / steps
                    
                    for(i in 0 until steps) {
                        if (hasWon || gameOver) break
                        
                        // Physics Step
                        if (velPx != Offset.Zero) {
                            var nextPx = ballPx + (velPx * dt)
                            
                            // 1. Walls
                            for (wall in currentLevel.walls) {
                                 // Wall Rect in Pixels
                                 val wallLeft = wall.x * w
                                 val wallTop = wall.y * h
                                 val wallRight = (wall.x + wall.w) * w
                                 val wallBottom = (wall.y + wall.h) * h
                                 
                                 // Closest Point
                                 val closestX = nextPx.x.coerceIn(wallLeft, wallRight)
                                 val closestY = nextPx.y.coerceIn(wallTop, wallBottom)
                                 
                                 val dx = nextPx.x - closestX
                                 val dy = nextPx.y - closestY
                                 val distSq = dx*dx + dy*dy
                                 
                                 if (distSq < (ballRadiusPx * ballRadiusPx)) {
                                     val dist = sqrt(distSq)
                                     var normal = Offset.Zero
                                     
                                     if (dist > 0.0001f) {
                                         normal = Offset(dx/dist, dy/dist)
                                     } else {
                                         // Tunneling Fix
                                         val dL = nextPx.x - wallLeft
                                         val dR = wallRight - nextPx.x
                                         val dT = nextPx.y - wallTop
                                         val dB = wallBottom - nextPx.y
                                         val minOverlap = minOf(dL, dR, dT, dB)
                                         normal = when (minOverlap) {
                                             dL -> Offset(-1f, 0f)
                                             dR -> Offset(1f, 0f)
                                             dT -> Offset(0f, -1f)
                                             else -> Offset(0f, 1f)
                                         }
                                     }
                                     
                                     // Refection
                                     val dot = velPx.x * normal.x + velPx.y * normal.y
                                     if (dot < 0) {
                                         velPx -= (normal * 2f * dot)
                                         velPx *= 0.8f 
                                     }
                                     
                                     // Push Out
                                     val overlap = ballRadiusPx - dist
                                     val push = if (overlap > 0) overlap else 0.001f
                                     nextPx += normal * (push + 0.1f)
                                 }
                            }
                            
                            // 2. Borders
                            if (nextPx.x < ballRadiusPx) { 
                                nextPx = nextPx.copy(x = ballRadiusPx); velPx = velPx.copy(x = abs(velPx.x) * 0.8f) 
                            }
                            if (nextPx.x > w - ballRadiusPx) { 
                                nextPx = nextPx.copy(x = w - ballRadiusPx); velPx = velPx.copy(x = -abs(velPx.x) * 0.8f) 
                            }
                            if (nextPx.y < ballRadiusPx) { 
                                nextPx = nextPx.copy(y = ballRadiusPx); velPx = velPx.copy(y = abs(velPx.y) * 0.8f) 
                            }
                            if (nextPx.y > h - ballRadiusPx) { 
                                nextPx = nextPx.copy(y = h - ballRadiusPx); velPx = velPx.copy(y = -abs(velPx.y) * 0.8f) 
                            }
                            
                            ballPx = nextPx
                        }
                    }
                    
                    // Friction
                    if (velPx != Offset.Zero) {
                        velPx *= 1f - (1f - FRICTION) * delta
                        if (velPx.getDistance() < (w * STOP_THRESHOLD)) { // Threshold scaled by screen width
                            velPx = Offset.Zero
                        }
                    }
                    
                    // Update State (Normalize back)
                    ballPos = Offset(ballPx.x / w, ballPx.y / h)
                    ballVelocity = Offset(velPx.x / w, velPx.y / h)
                    
                    // Win Check
                    if (!hasWon && !gameOver) {
                        val holePx = Offset(currentLevel.holePos.x * w, currentLevel.holePos.y * h)
                        val d = (ballPx - holePx).getDistance()
                        val holeRadiusPx = w * holeRadiusDef
                        
                        if (d < holeRadiusPx) {
                            if (velPx.getDistance() < (w * HOLE_CAPTURE_SPEED)) { // Speed scaled
                                ballVelocity = Offset.Zero
                                ballPos = currentLevel.holePos
                                
                                if (mode == MODE_2P) {
                                    if (currentPlayer == 1) {
                                        p1TotalScore += p1Strokes
                                        currentPlayer = 2
                                        turnMessage = "¡Turno del Jugador 2!"
                                        p2Strokes = 0
                                    } else {
                                        p2TotalScore += p2Strokes
                                        hasWon = true
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
    }
    
    // Delayed Turn Switch
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
                        Text("${currentLevel.name} (Par ${currentLevel.par})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (mode == MODE_1P && submode == SUBMODE_TIME) {
                            Text("Tiempo: ${timeLeft / 1000}s", fontSize = 14.sp, color = if(timeLeft < 10000) Color.Red else Color.Black)
                        }
                    }
                },
                actions = {
                    val currentStrokes = if (mode == MODE_2P && currentPlayer == 2) p2Strokes else p1Strokes
                    val label = if (mode == MODE_2P) "P$currentPlayer: $currentStrokes" else "Golpes: $currentStrokes"
                    Text(label, modifier = Modifier.padding(end=16.dp), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { 
                         resetBall()
                         // Penalty
                         if (mode == MODE_2P && currentPlayer == 2) p2Strokes++ else p1Strokes++
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
                .onSizeChanged { screenSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (ballVelocity == Offset.Zero && !hasWon && turnMessage.isEmpty()) {
                                val w = size.width
                                val h = size.height
                                val ballPx = Offset(w * ballPos.x, h * ballPos.y)
                                // Hit area check
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
                                val dragDist = vec.getDistance()
                                val minDrag = w * 0.05f
                                
                                if (dragDist > minDrag) {
                                    val power = min(dragDist, w * 0.4f) * 0.00018f // Tuned Power
                                    val dir = vec / dragDist
                                    ballVelocity = dir * power
                                    
                                    if (mode == MODE_2P && currentPlayer == 2) p2Strokes++ else p1Strokes++
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
                
                // 1. Grass
                val ts = w / 8
                for (r in 0..(h/ts).toInt()) {
                    for (c in 0..7) {
                        drawRect(
                            color = if ((r+c)%2==0) Color(0xFF388E3C) else Color(0xFF2E7D32),
                            topLeft = Offset(c*ts, r*ts), size = Size(ts, ts)
                        )
                    }
                }
                
                // 2. Start
                drawCircle(Color.White.copy(alpha=0.3f), radius = w*0.06f, center = Offset(w*currentLevel.startPos.x, h*currentLevel.startPos.y))

                // 3. Walls
                currentLevel.walls.forEach { 
                    drawRect(Color(0xFF5D4037), topLeft = Offset(w*it.x, h*it.y), size = Size(w*it.w, h*it.h))
                    drawRect(Color(0xFF8D6E63), topLeft = Offset(w*it.x+4f, h*it.y+4f), size = Size(w*it.w-8f, h*it.h-8f))
                }
                
                // 4. Hole
                val holePx = Offset(w*currentLevel.holePos.x, h*currentLevel.holePos.y)
                drawCircle(Color.Black, radius = w * holeRadiusDef, center = holePx)
                drawCircle(Color.Black.copy(alpha=0.3f), radius = w * holeRadiusDef + 4f, center = holePx) 

                // 5. Drag Line
                if (dragStart != null && dragCurrent != null) {
                    val start = dragStart!!
                    val end = dragCurrent!!
                    val dist = (start - end).getDistance()
                    val maxLen = w * 0.4f
                    val visEnd = if (dist > maxLen) start - (start - end) * (maxLen/dist) else end
                    drawLine(Color.Yellow, start, visEnd, strokeWidth = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }

                // 6. Ball
                val bPos = Offset(w*ballPos.x, h*ballPos.y)
                val bRad = w * 0.025f
                drawCircle(Color.Black.copy(alpha=0.4f), radius = bRad, center = bPos + Offset(5f,5f))
                drawCircle(Color.White, radius = bRad, center = bPos)
            }
            
            // Overlays
            if (turnMessage.isNotEmpty()) {
                 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.7f)), contentAlignment = Alignment.Center) {
                     Text(turnMessage, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                 }
            }

            if (hasWon || gameOver) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.8f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (gameOver && mode == MODE_1P && submode == SUBMODE_TIME && timeLeft == 0L) {
                             Text("¡TIEMPO AGOTADO!", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        } else if (gameOver) {
                             Text("¡JUEGO COMPLETADO!", color = Color.Green, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                             Text("Total Golpes: $p1TotalScore", color = Color.White, fontSize = 24.sp)
                        } else {
                             Text("¡NIVEL COMPLETADO!", color = Color.Yellow, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                             Text("Golpes: $p1Strokes / Par ${currentLevel.par}", color = Color.White, fontSize = 20.sp)
                             
                             if (mode == MODE_2P) {
                                 Text("J2 Golpes: $p2Strokes", color = Color.Cyan, fontSize = 20.sp)
                                 val winner = if(p1Strokes < p2Strokes) "¡Gana Jugador 1!" else if(p2Strokes < p1Strokes) "¡Gana Jugador 2!" else "¡Empate!"
                                 Spacer(Modifier.height(10.dp))
                                 Text(winner, color = Color.Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                             } else if (submode == SUBMODE_AI) {
                                 Text("IA Golpes: $p2Strokes", color = Color.Cyan, fontSize = 20.sp)
                                 val winner = if(p1Strokes < p2Strokes) "¡Ganaste!" else if (p1Strokes == p2Strokes) "¡Empate!" else "Perdiste..."
                                 Spacer(Modifier.height(10.dp))
                                 Text(winner, color = Color.Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                             }
                        }
                        
                        Spacer(Modifier.height(30.dp))
                        Row {
                            if (!gameOver) {
                                Button(onClick = { 
                                    resetBall()
                                    hasWon = false
                                    p1Strokes = 0
                                    p2Strokes = 0
                                    currentPlayer = 1
                                }) { Text("Reintentar") }
                                Spacer(Modifier.width(16.dp))
                                Button(onClick = { nextLevel() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))) { 
                                    Text("Siguiente", color = Color.Black) 
                                }
                            } else {
                                Button(onClick = { navController.popBackStack() }) { Text("Volver al Menú") }
                            }
                        }
                    }
                }
            }
        }
    }
}
