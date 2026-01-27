package com.thanhng224.app.feature.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.math.abs

// --- COLORS & STYLES ---
val GridOrange = Color(0xFFFF9800)
val GridLineColor = Color.Black
val P1Color = Color(0xFFE91E63) // Pink/Red
val P2Color = Color(0xFF2196F3) // Blue
val DisplayBg = Color(0xFF263238) // Dark Slate for LCD
val DisplayText = Color(0xFF81D4FA) // Light Blue Cyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerGameScreen(navController: NavController) {
    val navBackStackEntry = navController.currentBackStackEntry
    val players = navBackStackEntry?.arguments?.getInt("players") ?: 1
    val difficulty = navBackStackEntry?.arguments?.getInt("difficulty") ?: 0

    // BLIND MODE SETTINGS
    val hideTimeThreshold = when (difficulty) {
        0 -> 4.0f
        1 -> 3.0f
        else -> 2.0f
    }
    // --- GAME STATE ---
    var gameState by remember { mutableStateOf(TimerGameState.INTRO) }
    var countdownValue by remember { mutableIntStateOf(3) }
    
    // Random Target Logic
    // Start with default 5.00, but will be randomized on start
    var targetTimeValue by remember { mutableFloatStateOf(5.00f) }
    val targetTimeString = String.format("%.2f", targetTimeValue)

    // Player States
    // P1
    var p1Running by remember { mutableStateOf(false) }
    var p1StartTime by remember { mutableLongStateOf(0L) }
    var p1StopTime by remember { mutableFloatStateOf(0f) }
    var p1Display by remember { mutableStateOf("0.00") }
    var p1Finished by remember { mutableStateOf(false) }

    // P2
    var p2Running by remember { mutableStateOf(false) }
    var p2StartTime by remember { mutableLongStateOf(0L) }
    var p2StopTime by remember { mutableFloatStateOf(0f) }
    var p2Display by remember { mutableStateOf("0.00") }
    var p2Finished by remember { mutableStateOf(false) }

    // Helper to start game
    fun startGame() {
        gameState = TimerGameState.COUNTDOWN
        
        // Randomize Target (3.00s to 50.00s)
        val rnd = 300 + (Math.random() * 4700).toInt() // 300 + 0..4700 = 300..5000 (3s-50s)
        targetTimeValue = rnd / 100f
        
        // Reset values
        p1Finished = false
        p1Display = "0.00"
        p1StopTime = 0f
        
        p2Finished = false
        p2Display = "0.00"
        p2StopTime = 0f
    }

    // Helper to finish game/round
    fun checkFinish() {
        if (players == 1) {
            if (p1Finished) gameState = TimerGameState.FINISHED
        } else {
            if (p1Finished && p2Finished) gameState = TimerGameState.FINISHED
        }
    }

    // COUNTDOWN LOGIC
    LaunchedEffect(gameState) {
        if (gameState == TimerGameState.COUNTDOWN) {
            for (i in 3 downTo 1) {
                countdownValue = i
                delay(1000)
            }
            // START!
            gameState = TimerGameState.PLAYING
            val now = System.currentTimeMillis()
            
            p1StartTime = now
            p1Running = true
            
            if (players == 2) {
                p2StartTime = now
                p2Running = true
            }
        }
    }

    // TIMER LOOP
    LaunchedEffect(p1Running, p2Running) {
        if (p1Running || p2Running) {
            var lastUiUpdate = 0L
            while (gameState == TimerGameState.PLAYING) {
                val now = System.currentTimeMillis()
                
                // Throttle UI updates to ~30 FPS (33ms) to save CPU on A12
                // Logic remains high precision because we use System.currentTimeMillis() on click
                if (now - lastUiUpdate > 33) {
                    lastUiUpdate = now
                    
                    // Update P1
                    if (p1Running) {
                        val elapsed = (now - p1StartTime) / 1000f
                        if (elapsed < hideTimeThreshold) {
                            p1Display = String.format("%.2f", elapsed)
                        } else if (p1Display != "??.??") {
                            // Only set "??.??" once to avoid needless recompositions
                            p1Display = "??.??"
                        }
                    }

                    // Update P2
                    if (p2Running) {
                        val elapsed = (now - p2StartTime) / 1000f
                        if (elapsed < hideTimeThreshold) {
                            p2Display = String.format("%.2f", elapsed)
                        } else if (p2Display != "??.??") {
                            p2Display = "??.??"
                        }
                    }
                }
                
                withFrameMillis { } // Wait next frame
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exact Match", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        // Hard Reset
                        gameState = TimerGameState.INTRO
                        p1Running = false
                        p2Running = false
                        p1Finished = false
                        p2Finished = false
                        p1Display = "0.00"
                        p2Display = "0.00"
                        targetTimeValue = 5.00f // Reset to default on full reset
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GridOrange,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // 1. BACKGROUND GRID
            GridBackground()

            // 2. MAIN LAYOUT
            if (players == 1) {
                // SINGLE PLAYER LAYOUT (Centered)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TimerPlayerZone(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(0.8f),
                        playerColor = P1Color,
                        displayTime = p1Display,
                        targetTime = targetTimeString,
                        textColor = Color.White,
                        state = if (gameState == TimerGameState.FINISHED) PlayerState.Stopped(p1StopTime, targetTimeValue) 
                                else if (gameState == TimerGameState.PLAYING) PlayerState.Running 
                                else PlayerState.Idle,
                        onAction = {
                            if (gameState == TimerGameState.INTRO || gameState == TimerGameState.FINISHED) {
                                startGame()
                            } else if (gameState == TimerGameState.PLAYING && !p1Finished) {
                                p1Running = false
                                p1Finished = true
                                val elapsed = (System.currentTimeMillis() - p1StartTime) / 1000f
                                p1StopTime = elapsed
                                p1Display = String.format("%.2f", elapsed)
                                checkFinish()
                            }
                        }
                    )
                }
            } else {
                // TWO PLAYER LAYOUT (Split Screen)
                Column(Modifier.fillMaxSize()) {
                    // PLAYER 2 (Top, Rotated)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .rotate(180f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimerPlayerZone(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
                            playerColor = P2Color,
                            displayTime = p2Display,
                            targetTime = targetTimeString,
                            textColor = Color.White,
                            state = if (gameState == TimerGameState.FINISHED) PlayerState.Stopped(p2StopTime, targetTimeValue)
                                    else if (gameState == TimerGameState.PLAYING && p2Finished) PlayerState.Waiting // Finished but waiting for P1
                                    else if (gameState == TimerGameState.PLAYING) PlayerState.Running
                                    else PlayerState.Idle,
                            onAction = {
                                if (gameState == TimerGameState.INTRO || gameState == TimerGameState.FINISHED) {
                                    // Only global start needed? Let's say either can start
                                    startGame() 
                                } else if (gameState == TimerGameState.PLAYING && !p2Finished) {
                                    p2Running = false
                                    p2Finished = true
                                    val elapsed = (System.currentTimeMillis() - p2StartTime) / 1000f
                                    p2StopTime = elapsed
                                    p2Display = String.format("%.2f", elapsed)
                                    checkFinish()
                                }
                            }
                        )
                    }

                    // DIVIDER
                    Box(Modifier.fillMaxWidth().height(4.dp).background(Color.Black))

                    // PLAYER 1 (Bottom, Normal)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimerPlayerZone(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
                            playerColor = P1Color,
                            displayTime = p1Display,
                            targetTime = targetTimeString,
                            textColor = Color.White,
                            state = if (gameState == TimerGameState.FINISHED) PlayerState.Stopped(p1StopTime, targetTimeValue)
                                    else if (gameState == TimerGameState.PLAYING && p1Finished) PlayerState.Waiting
                                    else if (gameState == TimerGameState.PLAYING) PlayerState.Running
                                    else PlayerState.Idle,

                            onAction = {
                                if (gameState == TimerGameState.INTRO || gameState == TimerGameState.FINISHED) {
                                    startGame()
                                } else if (gameState == TimerGameState.PLAYING && !p1Finished) {
                                    p1Running = false
                                    p1Finished = true
                                    val elapsed = (System.currentTimeMillis() - p1StartTime) / 1000f
                                    p1StopTime = elapsed
                                    p1Display = String.format("%.2f", elapsed)
                                    checkFinish()
                                }
                            }
                        )
                    }
                }
            }

            // 3. CENTRAL TARGET DISPLAY (Overlay)
            // Always visible, on top of divider
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Card background for visibility
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(2.dp, GridOrange),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TARGET", color = GridOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = targetTimeString,
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // 4. WINNER OVERLAY
            if (gameState == TimerGameState.FINISHED && players == 2) {
                 val diff1 = abs(p1StopTime - targetTimeValue)
                 val diff2 = abs(p2StopTime - targetTimeValue)
                 val winner = when {
                     diff1 < diff2 -> "PLAYER 1 WINS!"
                     diff2 < diff1 -> "PLAYER 2 WINS!"
                     else -> "DRAW!"
                 }
                 
                 Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.7f)).clickable { /* Consume clicks */ }, contentAlignment = Alignment.Center) {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Text(winner, color = GridOrange, fontSize = 40.sp, fontWeight = FontWeight.Black)
                         Spacer(Modifier.height(16.dp))
                         Text("P1 Diff: ${String.format("%.3f", diff1)}", color = P1Color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                         Text("P2 Diff: ${String.format("%.3f", diff2)}", color = P2Color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                         Spacer(Modifier.height(32.dp))
                         Button(
                             onClick = { startGame() },
                             colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                         ) {
                             Text("PLAY AGAIN", color = Color.Black, fontWeight = FontWeight.Bold)
                         }
                         Spacer(Modifier.height(16.dp))
                         OutlinedButton(onClick = { navController.popBackStack() }) { 
                             Text("EXIT", color = Color.White) 
                         }
                     }
                 }
            }

            // 5. COUNTDOWN OVERLAY (Last -> Top)
            if (gameState == TimerGameState.COUNTDOWN) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$countdownValue",
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// --- SUBCOMPONENTS ---

@Composable
fun GridBackground() {
    Canvas(modifier = Modifier.fillMaxSize().background(GridOrange)) {
        val step = 60.dp.toPx()
        val w = size.width
        val h = size.height
        
        // Vertical lines
        for (x in 0..wdht_counts(w, step)) {
             drawLine(
                 color = GridLineColor,
                 start = Offset(x * step, 0f),
                 end = Offset(x * step, h),
                 strokeWidth = 3f
             )
        }
        // Horizontal lines
        for (y in 0..wdht_counts(h, step)) {
            drawLine(
                color = GridLineColor,
                start = Offset(0f, y * step),
                end = Offset(w, y * step),
                strokeWidth = 3f
            )
        }
    }
}
fun wdht_counts(total: Float, step: Float): Int = (total / step).toInt() + 1


sealed class PlayerState {
    object Idle : PlayerState()
    object Running : PlayerState()
    object Waiting : PlayerState() // Stopped, waiting for opponent
    data class Stopped(val time: Float, val target: Float) : PlayerState()
}

@Composable
fun TimerPlayerZone(
    modifier: Modifier = Modifier,
    playerColor: Color,
    displayTime: String,
    targetTime: String,
    textColor: Color,
    state: PlayerState,
    onAction: () -> Unit
) {
    // Styling constants
    val cardShape = RoundedCornerShape(20.dp)
    val buttonShape = RoundedCornerShape(12.dp)
    
    // Animate scale for "Pressed" effect if we wanted, simplifed for now
    
    Card(
        modifier = modifier
            .shadow(8.dp, cardShape)
            .border(4.dp, Color.Black, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCC80)) // Lighter Orange inside
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            
            // 1. DIGITAL DISPLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DisplayBg)
                    .border(2.dp, Color.Gray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = displayTime,
                    // fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, // Monospace is standard
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp,
                    color = DisplayText,
                    modifier = Modifier.padding(end = 16.dp),
                    letterSpacing = 2.sp
                )
                
                // Small Label - Removed from here as it is now central
                /*
                Text(
                    text = "TARGET: $targetTime",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                )
                */
            }
            
            // 2. RESULT MESSAGE (Overlay Logic)
            if (state is PlayerState.Stopped) {
                val diff = abs(state.time - state.target)
                val msg = when {
                    diff < 0.01f -> "PERFECT! \uD83D\uDC8E"
                    diff < 0.1f -> "AMAZING!"
                    diff < 0.5f -> "GOOD"
                    else -> "TRY AGAIN"
                }
                Text(msg, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black)
            } else if (state is PlayerState.Waiting) {
                Text("WAITING...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            
            // 3. BIG ACTION BUTTON
            val btnColor = when(state) {
                PlayerState.Idle, is PlayerState.Stopped -> playerColor // "START" or "RETRY"
                PlayerState.Running -> Color.Red // "STOP"
                PlayerState.Waiting -> Color.Gray
            }
            
            val btnText = when(state) {
                PlayerState.Idle -> "START!"
                PlayerState.Running -> "STOP!"
                PlayerState.Waiting -> "..."
                is PlayerState.Stopped -> "RETRY"
            }
            
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if(isPressed) 0.95f else 1f)
            
            // Custom Button Surface
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(80.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null, 
                        onClick = onAction,
                        enabled = state !is PlayerState.Waiting
                    ),
                shape = buttonShape,
                color = btnColor,
                border = androidx.compose.foundation.BorderStroke(3.dp, Color.Black),
                shadowElevation = if(isPressed) 2.dp else 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = btnText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                }
            }
        }
    }
}

enum class TimerGameState {
    INTRO, COUNTDOWN, PLAYING, FINISHED
}
