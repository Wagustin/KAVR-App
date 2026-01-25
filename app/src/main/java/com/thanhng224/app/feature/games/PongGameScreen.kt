package com.thanhng224.app.feature.games

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

// --- CONFIG ---
// Expanded dimensions for better photo visibility
const val PADDLE_LONG_SIDE_DP = 140f 
const val PADDLE_SHORT_SIDE_DP = 50f
const val BALL_RADIUS_DP = 12f
const val INITIAL_SPEED = 12.5f 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PongGameScreen(navController: NavController) {
    val context = LocalContext.current
    val mode = navController.currentBackStackEntry?.arguments?.getInt("mode") ?: 0 // 0=2P, 1=1P (AI)
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 1 

    // --- STATE ---
    var scores by remember { mutableStateOf(0 to 0) } // P1 vs P2
    
    // Side Selection (Only relevant for 1P)
    // false = Standard (User Bottom/Right, AI Top/Left)
    // true = Swapped (User Top/Left, AI Bottom/Right)
    var isSwapped by remember { mutableStateOf(false) }

    // Bitmaps
    var agusBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var katBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            agusBitmap = loadBitmapFromDrawable(context, "b_agus")
            katBitmap = loadBitmapFromDrawable(context, "b_kat")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val diffText = if(mode==1) when(difficulty) { 0->" (Fácil)" 1->" (Medio)" else->" (Difícil)" } else " (Versus)"
                    Text("Love Pong$diffText", fontWeight = FontWeight.Bold, color = Color.White) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Swap Sides Button (1P Only)
                    if (mode == 1) {
                         IconButton(onClick = { isSwapped = !isSwapped }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Cambiar Lado", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF1A1A2E) 
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF1A1A2E))
        ) {
            PongGameLoop(
                mode = mode,
                difficulty = difficulty,
                agusBitmap = agusBitmap,
                katBitmap = katBitmap,
                isSwapped = isSwapped,
                onScoreUpdate = { scores = it }
            )
            
            // SCORE OVERLAY
            val score1 = scores.first
            val score2 = scores.second
            
            // Simple centered overlay
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "$score1 - $score2",
                    color = Color.White.copy(alpha = 0.15f),
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper to load bitmap
fun loadBitmapFromDrawable(context: Context, name: String): Bitmap? {
    return try {
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
    } catch (e: Exception) { null }
}

@Composable
fun PongGameLoop(
    mode: Int,
    difficulty: Int,
    agusBitmap: Bitmap?,
    katBitmap: Bitmap?,
    isSwapped: Boolean,
    onScoreUpdate: (Pair<Int, Int>) -> Unit
) {
    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // GAME STATE
    var ballPos by remember { mutableStateOf(Offset(500f, 1000f)) }
    var ballVel by remember { mutableStateOf(Offset(5f, 5f)) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }

    // Positions (0f..1f relative to main axis)
    // P1 = Default Top (Portrait) or Left (Landscape) -> Usually AI/Kat
    var p1Pos by remember { mutableFloatStateOf(0.5f) } 
    // P2 = Default Bottom (Portrait) or Right (Landscape) -> Usually User/Agus
    var p2Pos by remember { mutableFloatStateOf(0.5f) }

    // AI
    var aiTarget by remember { mutableFloatStateOf(0.5f) }
    var lastAiCalcTime by remember { mutableLongStateOf(0L) }
    
    // Local Score Tracking to pass out updates only on change
    var currentScoreP1 by remember { mutableIntStateOf(0) }
    var currentScoreP2 by remember { mutableIntStateOf(0) }

    LaunchedEffect(canvasSize, isSwapped) {
        if (canvasSize.width == 0) return@LaunchedEffect
        // Reset center
        ballPos = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        ballVel = Offset(INITIAL_SPEED, INITIAL_SPEED)
        speedMultiplier = 1.0f
    }

    // GAME LOOP
    LaunchedEffect(canvasSize, mode, difficulty, isSwapped) {
        if (canvasSize.width == 0) return@LaunchedEffect
        
        while (true) {
            withFrameNanos { time ->
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                if (w <= 0 || h <= 0) return@withFrameNanos
                
                val isLandscape = w > h

                val speed = speedMultiplier
                
                // --- 1. AI LOGIC ---
                if (mode == 1) {
                    val aiIsP1 = !isSwapped // if not swapped, AI is P1 (Top/Left)
                    // P1 is at 0 (Top/Left), P2 is at 1 (Bottom/Right)
                    
                    val aiPaddlePos = if (aiIsP1) p1Pos else p2Pos
                    
                    // Ball moving towards AI?
                    // Landscape: P1(Left=0), P2(Right=w). BallVel.x < 0 -> Towards P1
                    // Portrait: P1(Top=0), P2(Bot=h). BallVel.y < 0 -> Towards P1
                    
                    val ballMovingTowardsAI = if (isLandscape) {
                         if (aiIsP1) ballVel.x < 0 else ballVel.x > 0 
                    } else {
                         if (aiIsP1) ballVel.y < 0 else ballVel.y > 0
                    }

                    // Params
                    val reactionDelay = when(difficulty) { 0 -> 600_000_000L else -> 50_000_000L } // Simplified
                    
                    if (ballMovingTowardsAI && (time - lastAiCalcTime > reactionDelay)) {
                        // Prediction
                        val currentAxisPos = if (isLandscape) ballPos.y else ballPos.x
                        val currentAxisVel = if (isLandscape) ballVel.y else ballVel.x
                        val otherAxisPos = if (isLandscape) ballPos.x else ballPos.y
                        val otherAxisVel = if (isLandscape) ballVel.x else ballVel.y
                        val limit = if (isLandscape) w else h
                        val targetLine = if (aiIsP1) 50f else limit - 50f
                        
                        // Simple linear prediction
                        val dist = targetLine - otherAxisPos
                        // Avoid div by zero
                        val vel = if(abs(otherAxisVel) < 0.1f) 0.1f else otherAxisVel
                        val timeToHit = abs(dist / vel)
                        var hitPos = currentAxisPos + (currentAxisVel * timeToHit)
                        
                        // Bounce approx
                        val crossSize = if (isLandscape) h else w
                        // Rough bounce simulation
                        while (hitPos < 0 || hitPos > crossSize) {
                            if (hitPos < 0) hitPos = -hitPos
                            if (hitPos > crossSize) hitPos = 2*crossSize - hitPos
                        }
                        
                        // Error
                        val err = if(difficulty==0) (Random.nextFloat()-0.5f)*0.3f else 0f
                        aiTarget = (hitPos / crossSize) + err
                        aiTarget = aiTarget.coerceIn(0f, 1f)
                        lastAiCalcTime = time
                    } else if (!ballMovingTowardsAI) {
                         // Centering
                         if (time - lastAiCalcTime > 1_000_000_000L) {
                             aiTarget = 0.5f
                             lastAiCalcTime = time
                         }
                    }
                    
                    // Move
                    val lerpSpeed = if(difficulty==0) 0.03f else 0.12f
                    val newPos = aiPaddlePos + (aiTarget - aiPaddlePos) * lerpSpeed
                    if (aiIsP1) p1Pos = newPos else p2Pos = newPos
                }
                
                // --- 2. PHYSICS ---
                var nextPos = ballPos + (ballVel * speed)
                var nextVel = ballVel
                
                // Dimensions
                val p1Size = if (isLandscape) Size(PADDLE_SHORT_SIDE_DP.dp.toPx(), PADDLE_LONG_SIDE_DP.dp.toPx()) 
                             else Size(PADDLE_LONG_SIDE_DP.dp.toPx(), PADDLE_SHORT_SIDE_DP.dp.toPx())
                
                // P1 Rect (Top/Left)
                val p1Rect = if (isLandscape) {
                    androidx.compose.ui.geometry.Rect(
                        offset = Offset(30f, (h * p1Pos) - p1Size.height/2), // Left margin
                        size = p1Size
                    )
                } else {
                    androidx.compose.ui.geometry.Rect(
                        offset = Offset((w * p1Pos) - p1Size.width/2, 30f), // Top margin
                        size = p1Size
                    )
                }
                
                // P2 Rect (Bottom/Right)
                val p2Rect = if (isLandscape) {
                    androidx.compose.ui.geometry.Rect(
                        offset = Offset(w - 30f - p1Size.width, (h * p2Pos) - p1Size.height/2),
                        size = p1Size
                    )
                } else {
                    androidx.compose.ui.geometry.Rect(
                        offset = Offset((w * p2Pos) - p1Size.width/2, h - 30f - p1Size.height),
                        size = p1Size
                    )
                }
                
                val ballR = BALL_RADIUS_DP.dp.toPx()
                val ballRect = androidx.compose.ui.geometry.Rect(center = nextPos, radius = ballR)
                
                // Wall Bounces (Non-scoring walls)
                if (isLandscape) {
                    // Floor/Ceiling bounce
                    if (nextPos.y - ballR < 0 || nextPos.y + ballR > h) {
                        nextVel = nextVel.copy(y = -nextVel.y)
                        nextPos = nextPos.copy(y = nextPos.y.coerceIn(ballR, h - ballR))
                    }
                } else {
                    // Left/Right Walls bounce
                    if (nextPos.x - ballR < 0 || nextPos.x + ballR > w) {
                        nextVel = nextVel.copy(x = -nextVel.x)
                        nextPos = nextPos.copy(x = nextPos.x.coerceIn(ballR, w - ballR))
                    }
                }
                
                // Paddle Collisions
                var hit = false
                if (isLandscape) {
                    // X Axis Scoring physics
                    // Moving Left (<0) -> Check P1
                    if (nextVel.x < 0 && ballRect.overlaps(p1Rect)) {
                        nextVel = nextVel.copy(x = abs(nextVel.x))
                        hit = true
                        speedMultiplier += 0.05f
                    }
                    // Moving Right (>0) -> Check P2
                    else if (nextVel.x > 0 && ballRect.overlaps(p2Rect)) {
                        nextVel = nextVel.copy(x = -abs(nextVel.x))
                        hit = true
                        speedMultiplier += 0.05f
                    }
                } else {
                    // Y Axis Scoring physics
                     if (nextVel.y < 0 && ballRect.overlaps(p1Rect)) {
                        nextVel = nextVel.copy(y = abs(nextVel.y))
                        hit = true
                        speedMultiplier += 0.05f
                    }
                    else if (nextVel.y > 0 && ballRect.overlaps(p2Rect)) {
                        nextVel = nextVel.copy(y = -abs(nextVel.y))
                        hit = true
                        speedMultiplier += 0.05f
                    }
                }
                
                // Scoring
                var scored = false
                if (isLandscape) {
                    if (nextPos.x < 0) { // P1 Missed -> P2 Point
                        currentScoreP2++
                        scored = true
                    } else if (nextPos.x > w) { // P2 Missed -> P1 Point
                        currentScoreP1++
                        scored = true
                    }
                } else {
                     if (nextPos.y < 0) { // Top Missed -> Bot Point
                         currentScoreP2++
                         scored = true
                     } else if (nextPos.y > h) { // Bot Missed -> Top Point
                         currentScoreP1++
                         scored = true
                     }
                }
                
                if (scored) {
                    onScoreUpdate(currentScoreP1 to currentScoreP2)
                    // Reset
                    nextPos = Offset(w/2, h/2)
                    speedMultiplier = 1.0f
                    // Random serve
                    val dirX = if(Random.nextBoolean()) 1 else -1
                    val dirY = if(Random.nextBoolean()) 1 else -1
                    nextVel = Offset(INITIAL_SPEED * dirX, INITIAL_SPEED * dirY)
                }

                ballPos = nextPos
                ballVel = nextVel
            }
        }
    }

    // INPUT & DRAWING
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val w = size.width
                    val h = size.height
                    val isL = w > h
                    val crossAxisSize = if(isL) h else w
                    
                    // Input Mapping
                    val inputPos = change.position
                    val dragDelta = if(isL) dragAmount.y else dragAmount.x
                    
                    // Which side are we touching?
                    // Portrait: Top half (<h/2) -> P1, Bot half -> P2
                    // Landscape: Left half (<w/2) -> P1, Right half -> P2
                    val mainAxisSize = if(isL) w else h
                    val posOnMainAxis = if(isL) inputPos.x else inputPos.y
                    val isTouchingP1Zone = posOnMainAxis < (mainAxisSize / 2)
                    
                    // User control logic
                    if (mode == 0) { // 2P
                        val deltaNorm = dragDelta / crossAxisSize
                        if (isTouchingP1Zone) p1Pos = (p1Pos + deltaNorm).coerceIn(0f, 1f)
                        else p2Pos = (p2Pos + deltaNorm).coerceIn(0f, 1f)
                    } else { // 1P
                        // Control the User's Paddle.
                        // If !isSwapped: User is P2 (Bot/Right).
                        // If isSwapped: User is P1 (Top/Left).
                        val userIsP1 = isSwapped
                        val deltaNorm = dragDelta / crossAxisSize
                        if (userIsP1) p1Pos = (p1Pos + deltaNorm).coerceIn(0f, 1f)
                        else p2Pos = (p2Pos + deltaNorm).coerceIn(0f, 1f)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val isL = w > h
            
            // Draw Background Elements
            val centerStart = if(isL) Offset(w/2, 0f) else Offset(0f, h/2)
            val centerEnd = if(isL) Offset(w/2, h) else Offset(w, h/2)
            drawLine(Color.White.copy(alpha=0.1f), centerStart, centerEnd, strokeWidth = 4f)
            
            // Dimensions
            val pShort = PADDLE_SHORT_SIDE_DP.dp.toPx()
            val pLong = PADDLE_LONG_SIDE_DP.dp.toPx()
            val corner = CornerRadius(12f, 12f)
            
            // --- DRAW P1 (Top/Left) ---
            // Colors: P1 usually Pink (Kat), P2 usually Blue (Agus)
            // But if Bitmaps exist: P1=Kat, P2=Agus.
            
            val p1S = if(isL) Size(pShort, pLong) else Size(pLong, pShort)
            val p1TL = if(isL) Offset(30f, (h*p1Pos)-pLong/2) else Offset((w*p1Pos)-pLong/2, 30f)
            val p1Rect = androidx.compose.ui.geometry.Rect(p1TL, p1S)
            
            if (katBitmap != null) {
                 drawIntoCanvas { canvas ->
                   val path = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p1Rect, corner)) }
                   canvas.save()
                   canvas.clipPath(path)
                   canvas.nativeCanvas.drawBitmap(katBitmap, null, android.graphics.Rect(p1Rect.left.toInt(), p1Rect.top.toInt(), p1Rect.right.toInt(), p1Rect.bottom.toInt()), null)
                   canvas.restore()
                }
            } else {
                drawRoundRect(Color(0xFFE91E63), topLeft = p1TL, size = p1S, cornerRadius = corner)
            }
            drawRoundRect(Color.White, topLeft = p1TL, size = p1S, cornerRadius = corner, style = Stroke(2f))

            // --- DRAW P2 (Bot/Right) ---
            val p2S = if(isL) Size(pShort, pLong) else Size(pLong, pShort)
            val p2TL = if(isL) Offset(w - 30f - pShort, (h*p2Pos)-pLong/2) else Offset((w*p2Pos)-pLong/2, h - 30f - pShort)
            val p2Rect = androidx.compose.ui.geometry.Rect(p2TL, p2S)

             if (agusBitmap != null) {
                 drawIntoCanvas { canvas ->
                   val path = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p2Rect, corner)) }
                   canvas.save()
                   canvas.clipPath(path)
                   canvas.nativeCanvas.drawBitmap(agusBitmap, null, android.graphics.Rect(p2Rect.left.toInt(), p2Rect.top.toInt(), p2Rect.right.toInt(), p2Rect.bottom.toInt()), null)
                   canvas.restore()
                }
            } else {
                drawRoundRect(Color(0xFF2196F3), topLeft = p2TL, size = p2S, cornerRadius = corner)
            }
            drawRoundRect(Color.White, topLeft = p2TL, size = p2S, cornerRadius = corner, style = Stroke(2f))

            // Ball
            drawCircle(Color.Yellow, radius = BALL_RADIUS_DP.dp.toPx(), center = ballPos)
        }
    }
}
