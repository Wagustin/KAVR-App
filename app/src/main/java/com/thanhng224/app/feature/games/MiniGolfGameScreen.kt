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
import kotlinx.coroutines.withContext
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
    val ballRadiusRel = 0.025f

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
            // Lv 3: Zig Zag (Improved positioning)
            Level(
                startPos = Offset(0.15f, 0.85f), // Better start pos
                holePos = Offset(0.9f, 0.1f),
                walls = listOf(
                    Wall(0.0f, 0.3f, 0.6f, 0.05f),
                    Wall(0.4f, 0.6f, 0.6f, 0.05f)
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
        dragStart = null
        dragCurrent = null
    }

    // GAME LOOP
    LaunchedEffect(Unit) {
        var lastTime = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameTime ->
                val delta = (frameTime - lastTime) / 16f
                lastTime = frameTime
                
                val subSteps = 5
                val subDelta = delta / subSteps
                
                for (i in 0 until subSteps) {
                    if (ballVelocity == Offset.Zero || hasWon) break

                    // Apply Velocity (Sub-step)
                    var nextPos = ballPos + (ballVelocity * subDelta)
                    
                    // --- COLLISION RESOLUTION (Robust) ---
                    var collided = false
                    
                    // 1. Screen Edges Collision
                    if (nextPos.x < ballRadiusRel) {
                        nextPos = nextPos.copy(x = ballRadiusRel)
                        ballVelocity = ballVelocity.copy(x = -ballVelocity.x * 0.8f) 
                        collided = true
                    } else if (nextPos.x > 1f - ballRadiusRel) {
                        nextPos = nextPos.copy(x = 1f - ballRadiusRel)
                        ballVelocity = ballVelocity.copy(x = -ballVelocity.x * 0.8f)
                        collided = true
                    }
                    
                    if (nextPos.y < ballRadiusRel) {
                            nextPos = nextPos.copy(y = ballRadiusRel)
                            ballVelocity = ballVelocity.copy(y = -ballVelocity.y * 0.8f)
                            collided = true
                    } else if (nextPos.y > 1f - ballRadiusRel) {
                        nextPos = nextPos.copy(y = 1f - ballRadiusRel)
                        ballVelocity = ballVelocity.copy(y = -ballVelocity.y * 0.8f)
                        collided = true
                    }

                    // 2. Wall Collision (Circle vs AABB)
                    currentLevel.walls.forEach { wall ->
                        val closestX = nextPos.x.coerceIn(wall.x, wall.x + wall.w)
                        val closestY = nextPos.y.coerceIn(wall.y, wall.y + wall.h)
                        
                        val distX = nextPos.x - closestX
                        val distY = nextPos.y - closestY
                        val distSq = distX*distX + distY*distY
                        
                        if (distSq < (ballRadiusRel * ballRadiusRel)) {
                            val dist = sqrt(distSq)
                            val normal = if (dist > 0.0001f) Offset(distX/dist, distY/dist) else {
                                val dL = nextPos.x - wall.x
                                val dR = (wall.x + wall.w) - nextPos.x
                                val dT = nextPos.y - wall.y
                                val dB = (wall.y + wall.h) - nextPos.y
                                
                                val minD = minOf(dL, dR, dT, dB)
                                when(minD) {
                                    dL -> Offset(-1f, 0f)
                                    dR -> Offset(1f, 0f)
                                    dT -> Offset(0f, -1f)
                                    else -> Offset(0f, 1f)
                                }
                            }
                            
                            val dot = ballVelocity.x * normal.x + ballVelocity.y * normal.y
                            if (dot < 0) {
                                ballVelocity = ballVelocity - (normal * 2f * dot)
                                ballVelocity *= 0.8f 
                            }
                            
                            val overlap = ballRadiusRel - dist
                            if (overlap > 0) {
                                nextPos += normal * (overlap + 0.0001f)
                            }
                        }
                    }

                    ballPos = nextPos
                }
                
                // Friction (Applied once per frame for consistent feel)
                if (ballVelocity != Offset.Zero && !hasWon) {
                    val frictionFactor = 1f - (1f - FRICTION) * delta
                    ballVelocity *= frictionFactor
                    
                    // Stop Threshold
                    if (ballVelocity.getDistance() < 0.0003f) {
                        ballVelocity = Offset.Zero
                    }
                }
                
                // Win Check (After all sub-steps)
                if (!hasWon) {
                    val distToHole = (ballPos - currentLevel.holePos).getDistance()
                    if (distToHole < holeRadius * 0.5f) {
                            if (ballVelocity.getDistance() < 0.015f) {
                                hasWon = true
                                ballVelocity = Offset.Zero
                                ballPos = currentLevel.holePos
                            }
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
                .background(Color(0xFF4CAF50)) // Base Green
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                             if (ballVelocity == Offset.Zero && !hasWon) {
                                  val w = size.width
                                  val h = size.height
                                  val ballPixel = Offset(w * ballPos.x, h * ballPos.y)
                                  val dist = (offset - ballPixel).getDistance()
                                  
                                  if (dist < w * 0.15f) { 
                                      dragStart = ballPixel
                                      dragCurrent = offset
                                  }
                             }
                        },
                        onDragEnd = {
                             if (dragStart != null && dragCurrent != null && !hasWon) {
                                 // Drag Back -> Shoot Forward
                                 val dragVec = dragStart!! - dragCurrent!! 
                                 
                                 val powerX = dragVec.x * 0.0002f 
                                 val powerY = dragVec.y * 0.0002f
                                 
                                 val rawVel = Offset(powerX, powerY)
                                 val mag = rawVel.getDistance()
                                 if (mag > 0.002f) { 
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
                
                // 1. PREMIUM CHECKERED GRASS
                val tileSize = w / 10f 
                val rows = (h / tileSize).toInt() + 1
                val cols = 10
                
                for(row in 0 until rows) {
                    for(col in 0 until cols) {
                        val isEven = (row + col) % 2 == 0
                        val color = if (isEven) Color(0xFF43A047) else Color(0xFF4CAF50)
                        
                        drawRect(
                            color = color,
                            topLeft = Offset(col * tileSize, row * tileSize),
                            size = Size(tileSize, tileSize)
                        )
                    }
                }

                // 2. HOLE (WITH DEPTH)
                val holeCenter = Offset(w * currentLevel.holePos.x, h * currentLevel.holePos.y)
                val holeRadPx = w * holeRadius
                
                // Outer Shadow/Gradient for Depth
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha=0.8f), Color.Black.copy(alpha=0.2f)),
                        center = holeCenter,
                        radius = holeRadPx
                    ),
                    radius = holeRadPx,
                    center = holeCenter
                )
                
                // FLAG (Red Triangle + Pole)
                // Only draw if ball not inside or won
                if (!hasWon) {
                    val poleBase = holeCenter
                    val poleTop = holeCenter - Offset(0f, w * 0.15f) // Pole height based on width
                    
                    // Pole
                    drawLine(
                        color = Color.White,
                        start = poleBase,
                        end = poleTop,
                        strokeWidth = 4f
                    )
                    
                    // Flag (Triangle)
                    val path = Path().apply {
                        moveTo(poleTop.x, poleTop.y)
                        lineTo(poleTop.x + w * 0.08f, poleTop.y + w * 0.03f)
                        lineTo(poleTop.x, poleTop.y + w * 0.06f)
                        close()
                    }
                    drawPath(path, Color.Red)
                }

                // 3. WALLS (ARCADE STYLE)
                currentLevel.walls.forEach { wall ->
                    val wx = w * wall.x
                    val wy = h * wall.y
                    val ww = w * wall.w
                    val wh = h * wall.h
                    
                    // Simple Box Shadow
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.4f),
                        topLeft = Offset(wx + 8f, wy + 8f),
                        size = Size(ww, wh),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
                    )

                    // Main Wall (Vibrant Brick/Wood Color)
                    drawRoundRect(
                        color = Color(0xFFFF7043), // Deep Orange
                        topLeft = Offset(wx, wy),
                        size = Size(ww, wh),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
                    )
                    
                    // Border
                    drawRoundRect(
                        color = Color(0xFFBF360C), // Darker Orange Outline
                        topLeft = Offset(wx, wy),
                        size = Size(ww, wh),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                        style = Stroke(width = 6f)
                    )
                }

                // 4. DRAG LINE (AIMING)
                if (dragStart != null && dragCurrent != null) {
                    val dragVec = dragCurrent!! - dragStart!!
                    val ballPixelPos = Offset(w * ballPos.x, h * ballPos.y)
                    
                    // Arcade Arrow
                    drawLine(
                        color = Color.Yellow,
                        start = ballPixelPos,
                        end = ballPixelPos - dragVec,
                        strokeWidth = 10f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                // 5. BALL (ARCADE STYLE)
                val ballPixelPos = Offset(w * ballPos.x, h * ballPos.y)
                val ballRadPx = w * ballRadiusRel
                
                // Simple Shadow
                 drawCircle(
                    color = Color.Black.copy(alpha = 0.3f), 
                    radius = ballRadPx,
                    center = ballPixelPos + Offset(6f, 6f)
                )
                
                // Solid White Ball + Thick Outline
                drawCircle(
                    color = Color.White,
                    radius = ballRadPx,
                    center = ballPixelPos
                )
                 drawCircle(
                    color = Color.Black,
                    radius = ballRadPx,
                    center = ballPixelPos,
                    style = Stroke(width = 4f)
                )
            }
            
            if (hasWon) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡HOYO! 🎯", color = Color.Yellow, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (levelIndex < levels.size - 1) {
                            Button(
                                onClick = { levelIndex++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)) // Bright Green
                            ) {
                                Text("NEXT LEVEL ▶", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        } else {
                            Text("YOU WIN! 🏆", color = Color.Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { levelIndex = 0 }) {
                                Text("REPLAY ↺")
                            }
                        }
                    }
                }
            }
        }
    }
}
