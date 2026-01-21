package com.thanhng224.app.feature.games

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

// --- CONFIG ---
const val PADDLE_WIDTH_DP = 80f
const val PADDLE_HEIGHT_DP = 20f
const val BALL_RADIUS_DP = 10f
const val INITIAL_SPEED = 10f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PongGameScreen(navController: NavController) {
    val context = LocalContext.current
    val mode = navController.currentBackStackEntry?.arguments?.getInt("mode") ?: 0 // 0=2P, 1=1P (AI)
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 1 // 0=Easy, 1=Med, 2=Hard

    // --- STATE ---
    var scores by remember { mutableStateOf(0 to 0) } // Top (Kat) vs Bottom (Agus)
    
    // Bitmaps for avatars
    var agusBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var katBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Load Bitmaps
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            agusBitmap = loadBitmapFromFolder(context, "agus")
            katBitmap = loadBitmapFromFolder(context, "kat")
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF1A1A2E) // Deep Space Purple
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
                currentScores = scores,
                onScoreUpdate = { scores = it }
            )
            
            // Score Overlay
            Text(
                text = "${scores.first}",
                color = Color(0xFFE91E63).copy(alpha = 0.5f), // Kat Score (Top)
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            )
             Text(
                text = "${scores.second}",
                color = Color(0xFF2196F3).copy(alpha = 0.5f), // Agus Score (Bottom)
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
    }
}

// Helper to load bitmap safely
fun loadBitmapFromFolder(context: Context, folderName: String): Bitmap? {
    return try {
        val folder = File(context.filesDir, "photos/$folderName")
        if (folder.exists() && folder.isDirectory) {
            val file = folder.listFiles()?.firstOrNull { 
                it.extension.lowercase() in listOf("jpg", "jpeg", "png") 
            }
            if (file != null) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun PongGameLoop(
    mode: Int,
    difficulty: Int,
    agusBitmap: Bitmap?,
    katBitmap: Bitmap?,
    currentScores: Pair<Int, Int>,
    onScoreUpdate: (Pair<Int, Int>) -> Unit
) {
    val density = LocalDensity.current
    
    // Size references
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Logic Stats
    var ballPos by remember { mutableStateOf(Offset(500f, 1000f)) }
    var ballVel by remember { mutableStateOf(Offset(5f, 5f)) }
    
    // Paddle X positions (0f to 1f relative to width)
    var paddleTopX by remember { mutableFloatStateOf(0.5f) }
    var paddleBottomX by remember { mutableFloatStateOf(0.5f) }
    
    // AI State
    var aiTargetX by remember { mutableFloatStateOf(0.5f) }
    var lastAiCalcTime by remember { mutableLongStateOf(0L) }

    // Speed Multiplier state for Ramp Up
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }

    // Game Loop
    LaunchedEffect(canvasSize) {
        if (canvasSize.width == 0) return@LaunchedEffect
        
        // Reset Ball
        ballPos = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
        ballVel = Offset(INITIAL_SPEED * (if(Random.nextBoolean()) 1 else -1), INITIAL_SPEED)
        speedMultiplier = 1.0f

        while (true) {
            withFrameNanos { time ->
                 val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                
                // Effective Speed
                val currentSpeedScale = speedMultiplier
                
                // --- ADVANCED AI LOGIC (KAT) ---
                if (mode == 1) {
                    val aiReactionDelay = when(difficulty) {
                        0 -> 500_000_000L // Easy: 500ms delay
                        1 -> 300_000_000L // Medium: 300ms delay
                        else -> 100_000_000L // Hard: 100ms delay
                    }
                    
                    val aiErrorMargin = when(difficulty) {
                        0 -> 0.2f // Easy: +/- 20% width error
                        1 -> 0.1f // Medium: +/- 10% width error
                        else -> 0.02f // Hard: Very precise
                    }
                    
                    val aiSpeed = when(difficulty) {
                        0 -> 0.03f // Easy: Slow
                        1 -> 0.06f // Med: Normal
                        else -> 0.12f // Hard: Fast
                    }
                    
                    // Only recalc prediction if enough time passed OR ball just hit bottom paddle (moving up)
                    // If ball moving DOWN (towards user), center paddle or relax
                    if (ballVel.y < 0) {
                        // Ball Moving UP -> Predict Trajectory
                        if (time - lastAiCalcTime > aiReactionDelay) {
                             // Predict impact X
                             val distY = ballPos.y - 50f // Dist to top paddle
                             val timeToHit = kotlin.math.abs(distY / (ballVel.y * currentSpeedScale))
                             var futureX = ballPos.x + (ballVel.x * currentSpeedScale * timeToHit)
                             
                             // Handle Bounces (Simple Reflection logic approximation)
                             // If futureX outside [0, w], reflect it.
                             // Rough bouncing logic for 1-2 bounces
                             while(futureX < 0 || futureX > w) {
                                 if(futureX < 0) futureX = -futureX
                                 if(futureX > w) futureX = 2*w - futureX
                             }
                             
                             // Add Error
                             val error = (Random.nextFloat() - 0.5f) * 2 * aiErrorMargin
                             aiTargetX = (futureX / w) + error
                             aiTargetX = aiTargetX.coerceIn(0f, 1f)
                             
                             lastAiCalcTime = time
                        }
                    } else {
                        // Ball Moving AWAY -> Return to Center (0.5) or track loosely
                         if (time - lastAiCalcTime > 1_000_000_000L) { // Once per sec
                             aiTargetX = 0.5f
                             lastAiCalcTime = time
                         }
                    }
                    
                    // Move AI Paddle smoothly
                    paddleTopX += (aiTargetX - paddleTopX) * aiSpeed
                    paddleTopX = paddleTopX.coerceIn(0f, 1f)
                }

                var newPos = ballPos + (ballVel * currentSpeedScale)
                var newVel = ballVel
                
                // Wall Collisions (Left/Right)
                if (newPos.x <= 0f || newPos.x >= w) {
                    newVel = newVel.copy(x = -newVel.x)
                    newPos = newPos.copy(x = newPos.x.coerceIn(0f, w))
                }
                
                // Paddle Dimensions
                val pW = with(density) { PADDLE_WIDTH_DP.dp.toPx() }
                val pH = with(density) { PADDLE_HEIGHT_DP.dp.toPx() }
                val bR = with(density) { BALL_RADIUS_DP.dp.toPx() }

                // Top Paddle Collision (Kat)
                val topPaddleRect = androidx.compose.ui.geometry.Rect(
                    offset = Offset((w * paddleTopX) - pW/2, 50f),
                    size = Size(pW, pH)
                )
                
                // Bottom Paddle Collision (Agus)
                val botPaddleRect = androidx.compose.ui.geometry.Rect(
                    offset = Offset((w * paddleBottomX) - pW/2, h - 50f - pH),
                    size = Size(pW, pH)
                )
                
                val ballRect = androidx.compose.ui.geometry.Rect(
                    center = newPos,
                    radius = bR
                )

                // Check Collisions
                if (ballVel.y < 0 && ballRect.overlaps(topPaddleRect)) {
                    // Hit Top
                    newVel = newVel.copy(y = abs(newVel.y)) // Speed up handled by multiplier
                    val hitOffset = (newPos.x - topPaddleRect.center.x) / (pW/2)
                    newVel = newVel.copy(x = newVel.x + hitOffset * 5f)
                    // RAMP UP SPEED
                    speedMultiplier += 0.05f 
                } else if (ballVel.y > 0 && ballRect.overlaps(botPaddleRect)) {
                    // Hit Bottom
                    newVel = newVel.copy(y = -abs(newVel.y)) 
                     val hitOffset = (newPos.x - botPaddleRect.center.x) / (pW/2)
                    newVel = newVel.copy(x = newVel.x + hitOffset * 5f)
                    // RAMP UP SPEED
                    speedMultiplier += 0.05f
                }

                // Scoring (Top/Bottom Walls)
                // Handled in separate LaunchedEffect for cleaner state update
                
                ballPos = newPos
                ballVel = newVel.copy(
                    x = newVel.x.coerceIn(-25f, 25f),
                    y = newVel.y.coerceIn(-30f, 30f) // Base cap (multiplier applies on top)
                )
            }
        }
    }
    
    // Score Logic Handling
    val h = canvasSize.height.toFloat()
    LaunchedEffect(ballPos.y) {
        if (h > 0) {
            if (ballPos.y < 0) {
                // Top Missed -> Bottom (Agus) Wins point
                onScoreUpdate(currentScores.copy(second = currentScores.second + 1))
                ballPos = Offset(canvasSize.width/2f, h/2f) 
                speedMultiplier = 1.0f // Reset Speed
            } else if (ballPos.y > h) {
                // Bottom Missed -> Top (Kat) Wins point
                 onScoreUpdate(currentScores.copy(first = currentScores.first + 1))
                 ballPos = Offset(canvasSize.width/2f, h/2f)
                 speedMultiplier = 1.0f // Reset Speed
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
                    val y = change.position.y
                    
                    if (mode == 0) {
                        // 2 Player Mode (Split Screen)
                        if (y < h / 2) {
                            paddleTopX = (paddleTopX + dragAmount.x / w).coerceIn(0f, 1f)
                        } else {
                            paddleBottomX = (paddleBottomX + dragAmount.x / w).coerceIn(0f, 1f)
                        }
                    } else {
                        // 1 Player Mode (AI)
                        // User only controls Bottom Paddle
                        // Can drag anywhere (or restrict to bottom half? Let's allow full screen control for better UX)
                        paddleBottomX = (paddleBottomX + dragAmount.x / w).coerceIn(0f, 1f)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pW = PADDLE_WIDTH_DP.dp.toPx()
            val pH = PADDLE_HEIGHT_DP.dp.toPx()

            // Draw Background Stars
            drawCircle(Color.White, radius = 2f, center = Offset(w*0.2f, h*0.2f))
            drawCircle(Color.White, radius = 3f, center = Offset(w*0.8f, h*0.1f))
            drawCircle(Color.White, radius = 2f, center = Offset(w*0.5f, h*0.6f))
            drawCircle(Color.White, radius = 1.5f, center = Offset(w*0.1f, h*0.8f))
            drawCircle(Color.White, radius = 2.5f, center = Offset(w*0.9f, h*0.5f))

            // Center Line
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(0f, h/2),
                end = Offset(w, h/2),
                strokeWidth = 2f
            )

            // TOP PADDLE (Kat)
            val topRect = androidx.compose.ui.geometry.Rect(
                offset = Offset((w * paddleTopX) - pW/2, 50f),
                size = Size(pW, pH)
            )
            // Image or Fallback
            if (katBitmap != null) {
                // Draw Bitmap clipped
                drawIntoCanvas { canvas ->
                   val path = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(topRect, CornerRadius(pH/2))) }
                   canvas.save()
                   canvas.clipPath(path)
                   canvas.nativeCanvas.drawBitmap(
                       katBitmap, 
                       null, 
                       android.graphics.Rect(topRect.left.toInt(), topRect.top.toInt(), topRect.right.toInt(), topRect.bottom.toInt()), 
                       null
                   )
                   canvas.restore()
                }
            } else {
                drawRoundRect(Color(0xFFE91E63), topLeft = topRect.topLeft, size = topRect.size, cornerRadius = CornerRadius(pH/2))
            }
            // Glow Border
            drawRoundRect(Color.White, topLeft = topRect.topLeft, size = topRect.size, cornerRadius = CornerRadius(pH/2), style = Stroke(width = 2f))

            // BOTTOM PADDLE (Agus)
            val botRect = androidx.compose.ui.geometry.Rect(
                offset = Offset((w * paddleBottomX) - pW/2, h - 50f - pH),
                size = Size(pW, pH)
            )
             if (agusBitmap != null) {
                drawIntoCanvas { canvas ->
                   val path = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(botRect, CornerRadius(pH/2))) }
                   canvas.save()
                   canvas.clipPath(path)
                   canvas.nativeCanvas.drawBitmap(
                       agusBitmap, 
                       null, 
                       android.graphics.Rect(botRect.left.toInt(), botRect.top.toInt(), botRect.right.toInt(), botRect.bottom.toInt()), 
                       null
                   )
                   canvas.restore()
                }
            } else {
                drawRoundRect(Color(0xFF2196F3), topLeft = botRect.topLeft, size = botRect.size, cornerRadius = CornerRadius(pH/2))
            }
             drawRoundRect(Color.White, topLeft = botRect.topLeft, size = botRect.size, cornerRadius = CornerRadius(pH/2), style = Stroke(width = 2f))

            // BALL
            drawCircle(Color.Yellow, radius = BALL_RADIUS_DP.dp.toPx(), center = ballPos)
            // Trail?
            drawCircle(Color.Yellow.copy(alpha=0.3f), radius = (BALL_RADIUS_DP*1.5f).dp.toPx(), center = ballPos)
        }
    }
}

// Ugly hack to bridge callback score reading inside composition loop 
// (Ideally this is hoisted properly but for speed in this one-shot tool usage):
// The caller passes a lambda that updates a state. Inside the composable drawing loop we can't easily read that state 
// back if it's not passed down. 
// Correct approach in production: GameState object `Scores` passed in.
// I will assume for now the simplified version above works for the visual demonstration.
// Fixing the compile error in the `LaunchedEffect(ballPos.y)` block:
// We need access to current scores to increment them.
// I'll assume the caller manages it.
// Actually, let's just make `scores` a mutable state inside `PongGameLoop` for simplicity if we can't change signature easily.
// But `PongGameScreen` owns it.
// So `PongGameLoop` needs `scores: Pair<Int,Int>` passed in.


