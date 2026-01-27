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
const val FRICTION = 0.95f // Slightly lower friction for better roll
const val STOP_THRESHOLD = 0.0001f
const val HOLE_CAPTURE_SPEED = 0.02f
const val MAX_POWER = 0.04f // Cap power
const val POWER_SCALE = 0.00012f // Reduced power multiplier 

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
    // Difficulty only affects AI or Obstacles now, not hole size
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 0

    // Difficulty Params
    val holeRadiusDef = 0.05f // Standard size for fairness

    // DATA
    data class Ball(
        var pos: Offset, 
        var vel: Offset = Offset.Zero, 
        val color: Color,
        var strokes: Int = 0,
        var holed: Boolean = false
    )

    // GAME STATE
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var levelIndex by remember { mutableIntStateOf(0) }
    
    // Initialize balls based on mode
    val balls = remember { mutableStateListOf<Ball>() }
    
    // Reset/Init Helper
    // Removed dummy currentLevel

    var currentPlayer by remember { mutableIntStateOf(0) } // 0 for P1, 1 for P2
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
            ).subList(0,4), par=4), 
            
            // 12. Bunker
            Level("Bunker", Offset(0.5f, 0.5f), Offset(0.1f, 0.1f), listOf(
                 Wall(0.4f, 0.4f, 0.2f, 0.02f), // Box around start
                 Wall(0.4f, 0.58f, 0.2f, 0.02f),
                 Wall(0.4f, 0.4f, 0.02f, 0.2f),
                 Wall(0.58f, 0.4f, 0.02f, 0.2f)
            ), par=4),

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

    // Initialize Balls
    fun resetBalls() {
        balls.clear()
        // Players always start at the same spot in this improved version, or slightly offset?
        // User asked for "simultaneous visibility". Overlapping is fine initially, they will separate.
        // To make it clear, let's offset them slightly or just keep them same.
        // 1vs1: P1 White, P2 Red
        if (mode == MODE_2P) {
            balls.add(Ball(currentLevel.startPos, Color.Black, Color.White))
            balls.add(Ball(currentLevel.startPos, Color.Black, Color.Red))
        } else {
            // 1P or AI
            balls.add(Ball(currentLevel.startPos, Color.Black, Color.White))
            if (submode == SUBMODE_AI) {
                 // in older logic AI played virtually. Here do we want physical AI ball?
                 // User request "1vs1... both balls visible". Usually implies human vs human or human vs AI.
                 // Let's assume P2 is AI or Human.
                 // For now, keep AI as "virtual score" for simplicity unless requested otherwise.
                 // "1vs1" usually implies local multiplayer.
            }
        }
        currentPlayer = 0
    }

    // Call reset on level change
    LaunchedEffect(levelIndex) {
        resetBalls()
        // If coming from another screen, ensure balls are set
        if (balls.isEmpty()) resetBalls()
    }
    
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
    fun nextLevel() {
        if (levelIndex < levels.size - 1) {
            levelIndex++
            // resetBalls handled by LaunchedEffect(levelIndex)
            hasWon = false
            p1TotalScore += balls[0].strokes
            if (mode == MODE_2P && balls.size > 1) {
                p2TotalScore += balls[1].strokes
            }
            if (mode == MODE_1P && submode == SUBMODE_TIME) {
                 timeLeft += 20000L // +20s bonus
                 isTimeRunning = true
            }
        } else {
            p1TotalScore += balls[0].strokes
            if (mode == MODE_2P && balls.size > 1) {
                p2TotalScore += balls[1].strokes
            }
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
                    
                    // Iterate all balls
                    balls.forEachIndexed { index, ball ->
                        if (!ball.holed) {
                            // Convert State to Pixels
                            var ballPx = Offset(ball.pos.x * w, ball.pos.y * h)
                            var velPx = Offset(ball.vel.x * w, ball.vel.y * h)
                            val ballRadiusPx = w * 0.025f 
                            
                            // Sub-steps
                            val steps = 8 // Reduced steps for performance, should be enough
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
                                if (velPx.getDistance() < (w * STOP_THRESHOLD)) { 
                                    velPx = Offset.Zero
                                    
                                    // Turn Switch Logic when ball stops
                                    // Only switch if this was the active ball moving?
                                    // With turn based, we moved one ball. 
                                    // We should detect "just stopped" to switch turn.
                                    // However, simpler to check "all balls stopped" in main loop.
                                }
                            }
                            
                            // Update State (Normalize back)
                            ball.pos = Offset(ballPx.x / w, ballPx.y / h)
                            ball.vel = Offset(velPx.x / w, velPx.y / h)
                            
                            // Hole Check
                            val holePx = Offset(currentLevel.holePos.x * w, currentLevel.holePos.y * h)
                            val d = (ballPx - holePx).getDistance()
                            val holeRadiusPx = w * holeRadiusDef
                            
                            if (d < holeRadiusPx) {
                                if (velPx.getDistance() < (w * HOLE_CAPTURE_SPEED)) {
                                    ball.vel = Offset.Zero
                                    ball.pos = currentLevel.holePos
                                    ball.holed = true
                                    
                                    // Win / Level End Check handled in main loop or effect
                                }
                            }
                        }
                    }
                    
                    // Turn Logic Check
                    // If current player's ball is stopped and we just finished a shot?
                    // We need to track if we are "in a shot".
                    // Input sets velocity -> ball moves.
                    // We wait until ALL balls stop.
                    // If balls[currentPlayer] is stopped AND we launched it... 
                    // Actually, simpler: if all balls stopped, and we aren't waiting for turn switch, enable input.
                    // Input increments stroke and sets velocity.
                    
                    // Check if level finished
                    if (!gameOver && !hasWon) {
                        if (mode == MODE_2P) {
                             if (balls.all { it.holed }) {
                                 hasWon = true
                             } else if (balls.isNotEmpty() && balls[currentPlayer].holed) {
                                 // Current player holed out, switch to other
                                 currentPlayer = (currentPlayer + 1) % balls.size
                                 // If that player is also holed (should be caught by 'all' check, but just in case)
                                 if (balls[currentPlayer].holed) {
                                      hasWon = true
                                 } else {
                                      turnMessage = "¡Turno del Jugador ${currentPlayer+1}!"
                                 }
                             }
                        } else {
                             if (balls.isNotEmpty() && balls[0].holed) {
                                  p1TotalScore += balls[0].strokes
                                  if (submode == SUBMODE_AI) playAI()
                                  hasWon = true
                             }
                        }
                        
                        // Switch turn if ball stopped?
                        // If velocity was > 0 and now is 0...
                        // We need a state "isMoving".
                    }
                }
            }
        }
    }
    
    // Valid turn switch monitor
    LaunchedEffect(balls.map { it.vel }) {
        if (balls.isNotEmpty()) {
             val moving = balls.any { it.vel != Offset.Zero }
             if (!moving && !hasWon && !gameOver) {
                 // stabilized
                 // If the current player's ball just stopped (and they made a stroke), switch?
                 // We don't have "just made a stroke" flag.
                 // Let's rely on velocity being set by input.
                 // If P1 shoots, vel > 0. Then eventually vel = 0.
                 // We need to know who shot.
                 // Simple approach: After input, set "isShooting = true". When vel -> 0, switch.
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
                    val currentBall = if (balls.isNotEmpty()) balls[currentPlayer] else null
                    val currentStrokes = currentBall?.strokes ?: 0
                    val label = if (mode == MODE_2P) "P${currentPlayer+1}: $currentStrokes" else "Golpes: $currentStrokes"
                    Text(label, modifier = Modifier.padding(end=16.dp), fontWeight = FontWeight.Bold, color = currentBall?.color ?: Color.Black)
                    IconButton(onClick = { 
                         resetBalls()
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
                            if (balls.isNotEmpty() && !hasWon && turnMessage.isEmpty()) {
                                // Check if any ball is moving
                                if (balls.none { it.vel != Offset.Zero }) {
                                    val ball = balls[currentPlayer]
                                    if (!ball.holed) {
                                        val w = size.width
                                        val h = size.height
                                        val ballPx = Offset(w * ball.pos.x, h * ball.pos.y)
                                        // Hit area check
                                        if ((offset - ballPx).getDistance() < w * 0.15f) {
                                            dragStart = ballPx
                                            dragCurrent = offset
                                        }
                                    }
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
                                    // Power Logic
                                    val power = (min(dragDist, w * 0.4f) * POWER_SCALE).coerceAtMost(MAX_POWER)
                                    val dir = vec / dragDist
                                    
                                    val ball = balls[currentPlayer]
                                    ball.vel = dir * power
                                    ball.strokes++
                                    
                                    if (mode == MODE_1P && submode == SUBMODE_TIME && !isTimeRunning) isTimeRunning = true
                                    
                                    // Prepare turn switch
                                    // We need to wait for ball to stop. 
                                    // We can launch a coroutine to watch this specific shot, or rely on global loop.
                                    // Global loop is better.
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

                // 6. Balls
                balls.forEach { ball ->
                     val bPos = Offset(w*ball.pos.x, h*ball.pos.y)
                     val bRad = w * 0.025f
                     if (!ball.holed) {
                         drawCircle(Color.Black.copy(alpha=0.4f), radius = bRad, center = bPos + Offset(5f,5f))
                         drawCircle(ball.color, radius = bRad, center = bPos)
                         drawCircle(Color.Black, radius = bRad, center = bPos, style = Stroke(width = 2f))
                     }
                }
            }
            
            // Turn Switch Logic in Composition
            val moving = remember(balls.map { it.vel }) { balls.any { it.vel != Offset.Zero } }
            // State variable to track if we just took a shot
            var justShot by remember { mutableStateOf(false) }
            
            LaunchedEffect(balls.map { it.strokes }) {
                 // Strokes changed, means we just shot
                 if (balls.isNotEmpty()) justShot = true
            }
            
            LaunchedEffect(moving) {
                if (!moving && justShot) {
                     // All balls stopped AND we just finished a shot
                     justShot = false
                     delay(500) // Small pause before switch
                     
                     if (mode == MODE_2P && !hasWon) {
                         // Switch player if current didn't hole out? 
                         // Or even if they did?
                         // "Turn based". Standard golf: farthest from hole goes first?
                         // Or just alternating? 1vs1 requests usually imply alternating.
                         // But if I hole out, I'm done.
                         
                         var nextP = (currentPlayer + 1) % balls.size
                         // Skip holed players
                         if (balls[nextP].holed) {
                              // If next is holed, check the one after (if > 2 players, but here 2)
                              // If both holed, handled by game loop
                              nextP = (nextP + 1) % balls.size
                         }
                         
                         if (!balls[nextP].holed && nextP != currentPlayer) {
                             currentPlayer = nextP
                             turnMessage = "Turno: P${currentPlayer+1}"
                         }
                     }
                }
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
                                 Text("J2 Golpes: ${if (balls.size > 1) balls[1].strokes else 0}", color = Color.Red, fontSize = 20.sp)
                                 
                                 val p1S = balls.getOrNull(0)?.strokes ?: 0
                                 val p2S = balls.getOrNull(1)?.strokes ?: 0
                                 
                                 val winner = if(p1S < p2S) "¡Gana Jugador 1!" else if(p2S < p1S) "¡Gana Jugador 2!" else "¡Empate!"
                                 Spacer(Modifier.height(10.dp))
                                 Text(winner, color = Color.Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                             } else if (submode == SUBMODE_AI) {
                                 Text("IA Golpes: $p2TotalScore", color = Color.Cyan, fontSize = 20.sp)
                                 val p1S = balls.getOrNull(0)?.strokes ?: 0
                                 val winner = if(p1S < p2TotalScore) "¡Ganaste!" else if (p1S == p2TotalScore) "¡Empate!" else "Perdiste..."
                                 Spacer(Modifier.height(10.dp))
                                 Text(winner, color = Color.Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                             }
                        }
                        
                        Spacer(Modifier.height(30.dp))
                        Row {
                            if (!gameOver) {
                                Button(onClick = {
                                    resetBalls()
                                    hasWon = false
                                    currentPlayer = 0
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
