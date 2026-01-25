package com.thanhng224.app.feature.games

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

// --- CONFIG ---
const val PADDLE_LONG_SIDE_DP = 140f 
const val PADDLE_SHORT_SIDE_DP = 50f
const val BALL_RADIUS_DP = 12f
const val INITIAL_SPEED = 12.5f 

enum class PongGameState {
    WAITING_FOR_SIDE,
    COUNTDOWN,
    PLAYING,
    PAUSED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PongGameScreen(navController: NavController) {
    val context = LocalContext.current
    val mode = navController.currentBackStackEntry?.arguments?.getInt("mode") ?: 0 // 0=2P, 1=1P (AI)
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 1 

    // --- STATE ---
    var scores by remember { mutableStateOf(0 to 0) }
    var isSwapped by remember { mutableStateOf(false) }
    
    // Game State Management
    var gameState by remember { mutableStateOf(if (mode == 1) PongGameState.WAITING_FOR_SIDE else PongGameState.COUNTDOWN) }

    // Bitmaps
    var agusBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var katBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // FORCE LANDSCAPE ORIENTATION & IMMERSIVE MODE
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        
        // 1. Force Landscape
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // 2. Hide System Bars (Immersive Mode)
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        onDispose {
            // Restore
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            if (window != null) {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            agusBitmap = loadBitmapFromDrawable(context, "b_agus")
            katBitmap = loadBitmapFromDrawable(context, "b_kat")
        }
    }
    
    // Side Selection Dialog (1P Only)
    if (gameState == PongGameState.WAITING_FOR_SIDE) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* Force Choice */ },
            title = { 
                Text(
                    "Elige tu Lado", 
                    color = Color.Black, 
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize() 
                ) 
            },
            text = { 
                Text(
                    "¿En qué lado quieres jugar?", 
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize()
                ) 
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { 
                    isSwapped = false // Right Side (Agus)
                    gameState = PongGameState.COUNTDOWN
                }) {
                    Text("Lado Agus")
                }
            },
            dismissButton = {
                androidx.compose.material3.Button(onClick = { 
                    isSwapped = true // Left Side (Kathy)
                    gameState = PongGameState.COUNTDOWN
                }) {
                    Text("Lado Kathy")
                }
            },
            containerColor = Color.White
        )
    }

    Scaffold(
        // Hide TopBar in Playing/Paused mode to go fully immersive? 
        // User asked for "todo sea pantalla". So we should hide the Scaffold TopBar during Game.
        // We will implement a custom Pause button instead.
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
                gameState = gameState,
                onScoreUpdate = { scores = it },
                onGameFinished = { gameState = PongGameState.PLAYING } 
            )
            
            // --- UI OVERLAYS ---
            
            // 1. Countdown Overlay
            if (gameState == PongGameState.COUNTDOWN) {
                var count by remember { mutableIntStateOf(3) }
                LaunchedEffect(Unit) {
                    for (i in 3 downTo 1) {
                        count = i
                        delay(1000)
                    }
                    gameState = PongGameState.PLAYING
                }
                
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$count",
                        color = Color.Yellow,
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } 
            // 2. Pause Overlay
            else if (gameState == PongGameState.PAUSED) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.7f)).clickable { /* Consume clicks */ }, contentAlignment = Alignment.Center) {
                     Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                         Text("PAUSA", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                         Spacer(Modifier.height(32.dp))
                         
                         // Resume
                         Button(
                             onClick = { gameState = PongGameState.PLAYING },
                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                             modifier = Modifier.size(width = 200.dp, height = 60.dp)
                         ) {
                             Icon(Icons.Default.PlayArrow, contentDescription = null)
                             Spacer(Modifier.size(8.dp))
                             Text("Continuar", fontSize = 20.sp)
                         }
                         
                         Spacer(Modifier.height(16.dp))
                         
                         // Exit
                         Button(
                             onClick = { navController.popBackStack() },
                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                             modifier = Modifier.size(width = 200.dp, height = 60.dp)
                         ) {
                             Text("Salir", fontSize = 20.sp)
                         }
                     }
                }
            }
            // 3. Play Mode UI (Score + Pause Button)
            else {
                 val score1 = scores.first
                 val score2 = scores.second
                 
                 // Score (Background)
                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$score1 - $score2",
                        color = Color.White.copy(alpha = 0.15f),
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold
                    )
                 }
                 
                 // Pause Button (Top Center, Halfway UP)
                 // "mitad mitad superior" -> Top Center
                 if (gameState == PongGameState.PLAYING) {
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                         IconButton(
                             onClick = { gameState = PongGameState.PAUSED },
                             modifier = Modifier.padding(top = 16.dp).size(64.dp)
                         ) {
                             Icon(
                                 Icons.Default.Pause, 
                                 contentDescription = "Pausa", 
                                 tint = Color.White.copy(alpha = 0.5f),
                                 modifier = Modifier.size(48.dp)
                             )
                         }
                     }
                 }
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
    gameState: PongGameState,
    onScoreUpdate: (Pair<Int, Int>) -> Unit,
    onGameFinished: () -> Unit
) {
    val density = LocalDensity.current
    
    // Pre-calc dimensions 
    val pShortPx = with(density) { PADDLE_SHORT_SIDE_DP.dp.toPx() }
    val pLongPx = with(density) { PADDLE_LONG_SIDE_DP.dp.toPx() }
    val ballRPx = with(density) { BALL_RADIUS_DP.dp.toPx() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // GAME STATE
    var ballPos by remember { mutableStateOf(Offset(500f, 1000f)) }
    var ballVel by remember { mutableStateOf(Offset(5f, 5f)) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }

    var p1Pos by remember { mutableFloatStateOf(0.5f) } 
    var p2Pos by remember { mutableFloatStateOf(0.5f) }

    var aiTarget by remember { mutableFloatStateOf(0.5f) }
    var lastAiCalcTime by remember { mutableLongStateOf(0L) }
    
    var currentScoreP1 by remember { mutableIntStateOf(0) }
    var currentScoreP2 by remember { mutableIntStateOf(0) }

    // Reset Ball Logic
    LaunchedEffect(canvasSize, gameState) {
        if (canvasSize.width == 0) return@LaunchedEffect
        
        if (gameState == PongGameState.COUNTDOWN) {
             // Center ball during countdown
            ballPos = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
            val dirX = if(Random.nextBoolean()) 1 else -1
            val dirY = if(Random.nextBoolean()) 1 else -1
            ballVel = Offset(INITIAL_SPEED * dirX, INITIAL_SPEED * dirY)
            speedMultiplier = 1.0f
        }
    }

    // GAME LOOP
    LaunchedEffect(canvasSize, mode, difficulty, isSwapped, gameState) {
        if (canvasSize.width == 0) return@LaunchedEffect
        
        // Only run loop if PLAYING
        while (gameState == PongGameState.PLAYING) {
            withFrameNanos { time ->
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                if (w <= 0 || h <= 0) return@withFrameNanos
                
                val isLandscape = w > h
                val speed = speedMultiplier
                
                // --- 1. AI LOGIC ---
                if (mode == 1) {
                    val aiIsP1 = !isSwapped // if not swapped, AI is P1 (Top/Left)
                    val aiPaddlePos = if (aiIsP1) p1Pos else p2Pos
                    val ballMovingTowardsAI = if (isLandscape) {
                         if (aiIsP1) ballVel.x < 0 else ballVel.x > 0 
                    } else {
                         if (aiIsP1) ballVel.y < 0 else ballVel.y > 0
                    }
                    val reactionDelay = when(difficulty) { 0 -> 600_000_000L else -> 50_000_000L } 
                    
                    if (ballMovingTowardsAI && (time - lastAiCalcTime > reactionDelay)) {
                        val currentAxisPos = if (isLandscape) ballPos.y else ballPos.x
                        val currentAxisVel = if (isLandscape) ballVel.y else ballVel.x
                        val otherAxisPos = if (isLandscape) ballPos.x else ballPos.y
                        val otherAxisVel = if (isLandscape) ballVel.x else ballVel.y
                        val limit = if (isLandscape) w else h
                        val targetLine = if (aiIsP1) 50f else limit - 50f
                        
                        val dist = targetLine - otherAxisPos
                        val vel = if(abs(otherAxisVel) < 0.1f) 0.1f else otherAxisVel
                        val timeToHit = abs(dist / vel)
                        var hitPos = currentAxisPos + (currentAxisVel * timeToHit)
                        val crossSize = if (isLandscape) h else w
                        while (hitPos < 0 || hitPos > crossSize) {
                            if (hitPos < 0) hitPos = -hitPos
                            if (hitPos > crossSize) hitPos = 2*crossSize - hitPos
                        }
                        val err = if(difficulty==0) (Random.nextFloat()-0.5f)*0.3f else 0f
                        aiTarget = (hitPos / crossSize) + err
                        aiTarget = aiTarget.coerceIn(0f, 1f)
                        lastAiCalcTime = time
                    } else if (!ballMovingTowardsAI) {
                         if (time - lastAiCalcTime > 1_000_000_000L) {
                             aiTarget = 0.5f
                             lastAiCalcTime = time
                         }
                    }
                    val lerpSpeed = if(difficulty==0) 0.03f else 0.12f
                    val newPos = aiPaddlePos + (aiTarget - aiPaddlePos) * lerpSpeed
                    if (aiIsP1) p1Pos = newPos else p2Pos = newPos
                }
                
                // --- 2. PHYSICS ---
                var nextPos = ballPos + (ballVel * speed)
                var nextVel = ballVel
                
                val p1Size = if (isLandscape) Size(pShortPx, pLongPx) else Size(pLongPx, pShortPx)
                val p1Rect = if (isLandscape) {
                    androidx.compose.ui.geometry.Rect(Offset(30f, (h * p1Pos) - p1Size.height/2), p1Size)
                } else {
                    androidx.compose.ui.geometry.Rect(Offset((w * p1Pos) - p1Size.width/2, 30f), p1Size)
                }
                
                val p2Rect = if (isLandscape) {
                    androidx.compose.ui.geometry.Rect(Offset(w - 30f - p1Size.width, (h * p2Pos) - p1Size.height/2), p1Size)
                } else {
                    androidx.compose.ui.geometry.Rect(Offset((w * p2Pos) - p1Size.width/2, h - 30f - p1Size.height), p1Size)
                }
                
                val ballR = ballRPx
                val ballRect = androidx.compose.ui.geometry.Rect(center = nextPos, radius = ballR)
                
                if (isLandscape) {
                    if (nextPos.y - ballR < 0 || nextPos.y + ballR > h) {
                        nextVel = nextVel.copy(y = -nextVel.y)
                        nextPos = nextPos.copy(y = nextPos.y.coerceIn(ballR, h - ballR))
                    }
                } else {
                    if (nextPos.x - ballR < 0 || nextPos.x + ballR > w) {
                        nextVel = nextVel.copy(x = -nextVel.x)
                        nextPos = nextPos.copy(x = nextPos.x.coerceIn(ballR, w - ballR))
                    }
                }
                
                if (isLandscape) {
                    if (nextVel.x < 0 && ballRect.overlaps(p1Rect)) {
                        nextVel = nextVel.copy(x = abs(nextVel.x))
                        speedMultiplier += 0.05f
                    } else if (nextVel.x > 0 && ballRect.overlaps(p2Rect)) {
                        nextVel = nextVel.copy(x = -abs(nextVel.x))
                        speedMultiplier += 0.05f
                    }
                } else {
                     if (nextVel.y < 0 && ballRect.overlaps(p1Rect)) {
                        nextVel = nextVel.copy(y = abs(nextVel.y))
                        speedMultiplier += 0.05f
                    } else if (nextVel.y > 0 && ballRect.overlaps(p2Rect)) {
                        nextVel = nextVel.copy(y = -abs(nextVel.y))
                        speedMultiplier += 0.05f
                    }
                }
                
                var scored = false
                if (isLandscape) {
                    if (nextPos.x < 0) { currentScoreP2++; scored = true } 
                    else if (nextPos.x > w) { currentScoreP1++; scored = true }
                } else {
                     if (nextPos.y < 0) { currentScoreP2++; scored = true } 
                     else if (nextPos.y > h) { currentScoreP1++; scored = true }
                }
                
                if (scored) {
                    onScoreUpdate(currentScoreP1 to currentScoreP2)
                    nextPos = Offset(w/2, h/2)
                    speedMultiplier = 1.0f
                    val dirX = if(Random.nextBoolean()) 1 else -1
                    val dirY = if(Random.nextBoolean()) 1 else -1
                    nextVel = Offset(INITIAL_SPEED * dirX, INITIAL_SPEED * dirY)
                }
                ballPos = nextPos
                ballVel = nextVel
            }
        }
    }

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
                    val inputPos = change.position
                    val dragDelta = if(isL) dragAmount.y else dragAmount.x
                    val mainAxisSize = if(isL) w else h
                    val posOnMainAxis = if(isL) inputPos.x else inputPos.y
                    val isTouchingP1Zone = posOnMainAxis < (mainAxisSize / 2)
                    
                    // Allow Dragging even when paused? No.
                    // Allow Dragging in COUNTDOWN or PLAYING for positioning.
                    if (gameState == PongGameState.PLAYING || gameState == PongGameState.COUNTDOWN) { 
                        if (mode == 0) { // 2P
                            val deltaNorm = dragDelta / crossAxisSize
                            if (isTouchingP1Zone) p1Pos = (p1Pos + deltaNorm).coerceIn(0f, 1f)
                            else p2Pos = (p2Pos + deltaNorm).coerceIn(0f, 1f)
                        } else { // 1P
                            val userIsP1 = isSwapped
                            val deltaNorm = dragDelta / crossAxisSize
                            if (userIsP1) p1Pos = (p1Pos + deltaNorm).coerceIn(0f, 1f)
                            else p2Pos = (p2Pos + deltaNorm).coerceIn(0f, 1f)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val isL = w > h
            val centerStart = if(isL) Offset(w/2, 0f) else Offset(0f, h/2)
            val centerEnd = if(isL) Offset(w/2, h) else Offset(w, h/2)
            drawLine(Color.White.copy(alpha=0.1f), centerStart, centerEnd, strokeWidth = 4f)
            
            val pShort = pShortPx
            val pLong = pLongPx
            val corner = CornerRadius(12f, 12f)
            
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

            drawCircle(Color.Yellow, radius = ballRPx, center = ballPos)
        }
    }
}

fun Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}
