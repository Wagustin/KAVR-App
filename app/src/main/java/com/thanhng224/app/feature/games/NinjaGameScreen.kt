package com.thanhng224.app.feature.games

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- CONFIG ---
const val TARGET_RADIUS_DP = 60f
const val KNIFE_LEN_DP = 40f
const val SPEED_RADIANS = 0.05f
const val THROW_SPEED = 30f

data class Knife(
    var distance: Float, // Distance from center
    var angle: Float, // Angle around target (if stuck)
    val owner: Int, // 0 = P1 (Bottom), 1 = P2 (Top)
    var state: KnifeState = KnifeState.FLYING
)

enum class KnifeState { FLYING, STUCK, REBOUND }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NinjaGameScreen(navController: NavController) {
    val mode = navController.currentBackStackEntry?.arguments?.getInt("mode") ?: 0 
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 0
    // mode 0 = Versus (2P)
    // mode 1 = Survival (1P)
    
    // Difficulty Settings
    val (baseSpeed, collisionThreshold, speedMultiplier) = when(difficulty) {
        0 -> Triple(0.025f, 0.3f, 1.02f) // Easy: Slow, Forgiving, 2% incease
        1 -> Triple(0.035f, 0.25f, 1.02f) // Medium: Normal, Normal, 2% increase
        else -> Triple(0.05f, 0.15f, 1.03f) // Hard: Fast, Strict, 3% increase
    }

    // --- STATE ---
    var scores by remember { mutableStateOf(0 to 0) } // Top(P2) vs Bot(P1) / Survival: (High Score, Current Score)
    var targetRotation by remember { mutableFloatStateOf(0f) }
    var rotationSpeed by remember { mutableFloatStateOf(baseSpeed) }
    var gameOver by remember { mutableStateOf(false) }

    // Knives
    val knives = remember { mutableStateListOf<Knife>() }
    
    // Game Loop
    LaunchedEffect(gameOver, mode) {
        if (gameOver) return@LaunchedEffect
        
        // Reset Logic on restart handled by UI toggle 
        
        while(true) {
            withFrameMillis { _ ->
                // Rotate Target
                targetRotation = (targetRotation + rotationSpeed) % (2 * PI.toFloat())
                
                // Survival Mode Speed Up
                if (mode == 1 && scores.second > 0 && scores.second % 5 == 0) {
                     // rotationSpeed += 0.0001f // Gradual speed up? 
                     // Kept simple for now
                }

                // Update Knives
                val iterator = knives.iterator()
                while(iterator.hasNext()) {
                    val k = iterator.next()
                    
                    if (k.state == KnifeState.FLYING) {
                        if (k.owner == 0) { // Bottom Player / Single Player
                             k.distance -= THROW_SPEED
                             if (k.distance <= TARGET_RADIUS_DP * 3) { 
                                     // Collision logic
                                     // In 1P (mode 1), check collision with ANY stuck knife
                                     // In 2P (mode 0), only own rebound?
                                     
                                     // We need accurate angle calculation
                                     val hitAngle = (PI/2).toFloat() - targetRotation
                                     
                                     // NORMALIZE ANGLES to 0..2PI
                                     val normLength = (TARGET_RADIUS_DP * 3 * PI * 2).toFloat() // Circumference approx
                                     
                                     // Check collision
                                     var collided = false
                                     if (mode == 1) {
                                         // Survival: Rebound = Game Over
                                         if (checkCollision(hitAngle, knives, collisionThreshold)) collided = true
                                     } else {
                                         // Versus: Just bounce off
                                         // (Simplified for now)
                                     }
                                     
                                     if (collided) {
                                         k.state = KnifeState.REBOUND
                                         if (mode == 1) {
                                             gameOver = true
                                         }
                                     } else {
                                         k.state = KnifeState.STUCK
                                         k.distance = TARGET_RADIUS_DP * 3 
                                         k.angle = hitAngle
                                         scores = scores.copy(second = scores.second + 1)
                                         
                                         // Speed up rotation in Survival
                                         if (mode == 1) {
                                             if (Random.nextBoolean()) rotationSpeed = -rotationSpeed // Randomize direction
                                             rotationSpeed *= speedMultiplier // Faster per hit
                                         }
                                     }
                             }
                        } else { // Top Player (Only in Mode 0)
                             k.distance += THROW_SPEED // FIX: Was -= (bug)
                             // dist starts negative (-800), moves to 0. 
                             // Frame 1: -800 + 30 = -770. Correct. Approaching 0.
                             
                             if (k.distance >= -TARGET_RADIUS_DP * 3) {
                                  // Top Hit Logic
                                  // Hit Angle (Top is 3PI/2 270 deg)
                                  val hitAngle = (3 * PI / 2).toFloat() - targetRotation
                                  
                                  k.state = KnifeState.STUCK
                                  k.distance = -TARGET_RADIUS_DP * 3
                                  k.angle = hitAngle
                                  scores = scores.copy(first = scores.first + 1)
                             }
                        }
                    } else if (k.state == KnifeState.REBOUND) {
                        k.distance += 15f
                        if (kotlin.math.abs(k.distance) > 2000f) iterator.remove()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
             TopAppBar(
                title = { Text(if (mode == 1) "Survival Mode" else "Ninja Duel", fontWeight = FontWeight.Bold, color = Color.White) },
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
        containerColor = Color(0xFF263238) 
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF546E7A))
        ) {
            // Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val bambooColor = Color(0xFF37474F)
                val segmentHeight = 120f
                listOf(w * 0.1f, w * 0.9f, w * 0.05f, w * 0.95f).forEachIndexed { i, x ->
                    val width = if(i < 2) 40f else 25f
                    var y = -50f
                    while (y < h) {
                        drawRoundRect(
                            color = bambooColor,
                            topLeft = Offset(x - width/2, y),
                            size = Size(width, segmentHeight - 5f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f)
                        )
                        drawRect(
                            color = Color(0xFF263238),
                            topLeft = Offset(x - width/2 - 5f, y + segmentHeight - 10f),
                            size = Size(width + 10f, 10f)
                        )
                        y += segmentHeight
                    }
                }
            }
            
            // Game Canvas
            Box(Modifier.fillMaxSize()) {
                NinjaGameCanvas(
                    targetRotation = targetRotation,
                    knives = knives
                )
            }
            
            // Inputs
            if (mode == 0) {
                // VERSUS 2P
                Column(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                knives.add(Knife(distance = -800f, angle = 0f, owner = 1))
                            }
                    ) {
                         Text("${scores.first}", fontSize = 60.sp, color = Color.White, modifier = Modifier.align(Alignment.TopCenter).padding(top=50.dp), fontWeight = FontWeight.Bold)
                         // Rotation 180 for top player text?
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                knives.add(Knife(distance = 800f, angle = 0f, owner = 0))
                            }
                    ) {
                         Text("${scores.second}", fontSize = 60.sp, color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom=50.dp), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // SURVIVAL 1P
                // Track High Score locally for this session (since no ViewModel yet)
                 var localHighScore by remember { mutableStateOf(0) } // Simplified persistence
                 
                 // Animation State for Score
                 val isNewRecord = scores.second > localHighScore && scores.second > 0
                 val scoreScale by androidx.compose.animation.core.animateFloatAsState(
                     targetValue = if (isNewRecord) 1.2f else 1f,
                     label = "scale"
                 )
                 val scoreColor by androidx.compose.animation.animateColorAsState(
                     targetValue = if (isNewRecord) Color(0xFFFFD700) else Color.White.copy(alpha=0.5f),
                     label = "color"
                 )

                 Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!gameOver) {
                                knives.add(Knife(distance = 800f, angle = 0f, owner = 0))
                            } else {
                                // Reset
                                knives.clear()
                                // Update High Score if needed (should be done on game over technically, 
                                // but we update live for "New Record" effect)
                                if (scores.second > localHighScore) localHighScore = scores.second
                                
                                scores = localHighScore to 0
                                rotationSpeed = baseSpeed
                                gameOver = false
                            }
                        }
                ) {
                     Text(
                         text = if (isNewRecord) "NEW RECORD: ${scores.second}" else "${scores.second}",
                         fontSize = if (isNewRecord) 50.sp else 100.sp, 
                         color = scoreColor, 
                         modifier = Modifier
                             .align(Alignment.Center)
                             .padding(bottom = 200.dp)
                             .graphicsLayer {
                                 scaleX = scoreScale
                                 scaleY = scoreScale
                             }, 
                         fontWeight = FontWeight.Bold
                     )
                     
                     if (gameOver) {
                         Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.6f)), contentAlignment = Alignment.Center) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Text("GAME OVER", fontSize = 50.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                 Text("Score: ${scores.second}", fontSize = 30.sp, color = Color.White)
                                 Text("High Score: $localHighScore", fontSize = 20.sp, color = Color.Yellow)
                                 Text("Tap to Retry", fontSize = 20.sp, color = Color.Gray)
                             }
                         }
                     }
                }
            }
        }
    }
}

fun checkCollision(hitAngle: Float, knives: List<Knife>, threshold: Float): Boolean {
    // Normalise hit angle to 0..2PI
    val normalizedHit = (hitAngle % (2 * PI.toFloat())).let { if (it < 0) it + 2 * PI.toFloat() else it }
    
    // Check against all STUCK knives
    return knives.any { k ->
        if (k.state == KnifeState.STUCK) {
            val normalizedK = (k.angle % (2 * PI.toFloat())).let { if (it < 0) it + 2 * PI.toFloat() else it }
            kotlin.math.abs(normalizedHit - normalizedK) < threshold
        } else false
    }
}

@Composable
fun NinjaGameCanvas(
    targetRotation: Float,
    knives: List<Knife>
) {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        val radiusPx = TARGET_RADIUS_DP.dp.toPx()
        
        // Save logic to rotate target
        rotate(degrees = Math.toDegrees(targetRotation.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
            // Draw Target Board
            drawCircle(Color(0xFF5D4037), radiusPx, center = Offset(cx, cy)) // Wood
            drawCircle(Color.White, radiusPx * 0.7f, center = Offset(cx, cy), style = Stroke(width = 20f))
            drawCircle(Color(0xFFE53935), radiusPx * 0.2f, center = Offset(cx, cy)) // Bullseye
            
            // Draw Stuck Knives (They rotate with target)
            knives.forEach { k ->
                if (k.state == KnifeState.STUCK) {
                    rotate(degrees = Math.toDegrees(k.angle.toDouble()).toFloat() - Math.toDegrees(targetRotation.toDouble()).toFloat() + 90f, pivot = Offset(cx, cy)) {
                        // Draw Knife at correct radius? 
                        // Wait, if I rotate canvas by targetRotation, then stuck knives tracked by 'angle'
                        // should be drawn at 'angle' relative to 0.
                        // Actually, k.angle IS the angle on the target.
                        // So if I am inside the rotated reference frame of the target, I just draw at k.angle.
                    }
                    
                    // Let's simplify:
                    // Draw target.
                    // Loop stuck knives.
                    // x = cx + r * cos(k.angle)
                    // y = cy + r * sin(k.angle)
                    // Draw knife rotated to point to center.
                }
            }
        }
        
        // Redraw stuck knives? 
        // Correct approach:
        // Inside `rotate(targetRotation)` block:
        // Draw target.
        // Draw stuck knives at their `k.angle`. `k.angle` is relative to target's 0.
        // If P1 hits at screen PI/2, and target rot is 0, k.angle = PI/2.
        // If target rot is PI/4, P1 hits at PI/2, k.angle = PI/2 - PI/4 = PI/4.
        
        // Re-implement inside rotate:
        
        // 1. Draw Target (Base)
    }
    
    // Separate Draw implementation to handle rotation cleanly
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val radiusPx = TARGET_RADIUS_DP.dp.toPx()
        
        rotate(degrees = Math.toDegrees(targetRotation.toDouble()).toFloat(), pivot = Offset(cx, cy)) {
             drawCircle(Color(0xFF5D4037), radiusPx, center = Offset(cx, cy))
             drawCircle(Color(0xFF8D6E63), radiusPx * 0.8f, center = Offset(cx, cy), style = Stroke(width = 5f))
             drawCircle(Color(0xFFE53935), radiusPx * 0.2f, center = Offset(cx, cy))
             
             // Stuck Knives
             knives.filter { it.state == KnifeState.STUCK }.forEach { k ->
                 // k.angle is position on the perimeter relative to target center
                 val kx = cx + radiusPx * cos(k.angle)
                 val ky = cy + radiusPx * sin(k.angle)
                 
                 // Draw knife pointing center
                 // Knife rotation = k.angle + 90deg? 
                 rotate(degrees = Math.toDegrees(k.angle.toDouble()).toFloat() + 90f, pivot = Offset(kx, ky)) {
                       drawRect(Color.LightGray, topLeft = Offset(kx - 10f, ky), size = Size(20f, 60f))
                       drawRect(Color.Gray, topLeft = Offset(kx - 5f, ky + 60f), size = Size(10f, 40f)) // Handle
                 }
             }
        }
        
        // Flying Knives (Independent of rotation)
        knives.filter { it.state == KnifeState.FLYING || it.state == KnifeState.REBOUND }.forEach { k ->
             if (k.owner == 0) { // Moving UP
                  val yPos = cy + k.distance
                  drawRect(Color.LightGray, topLeft = Offset(cx - 10f, yPos), size = Size(20f, 60f))
                  drawRect(Color.Gray, topLeft = Offset(cx - 5f, yPos + 60f), size = Size(10f, 40f))
             } else { // Moving DOWN
                 // P2 (Top) - moves down. distance is negative.
                  val yPos = cy + k.distance 
                  // Knife needs to face down
                  rotate(180f, pivot = Offset(cx, yPos)) {
                      drawRect(Color.LightGray, topLeft = Offset(cx - 10f, yPos), size = Size(20f, 60f))
                      drawRect(Color.Gray, topLeft = Offset(cx - 5f, yPos + 60f), size = Size(10f, 40f))
                  }
             }
        }
    }
}
