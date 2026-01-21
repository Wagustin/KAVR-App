package com.thanhng224.app.feature.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrocodileGameScreen(navController: NavController) {
    // --- COLORS (Toy Aesthetic) ---
    val ToyGreenLight = Color(0xFF76FF03) // Bright Lime
    val ToyGreenDark = Color(0xFF33691E)  // Darker Green for shadows/outline
    val MouthRed = Color(0xFFC62828)      // Plastic Red
    val ToothWhite = Color(0xFFF5F5F5)    // Plastic White
    val ToothPressed = Color(0xFF9E9E9E)  // Grayed out
    
    // --- GAME STATE ---
    var teethState by remember { mutableStateOf(List(13) { false }) } // 13 Teeth
    var badToothIndex by remember { mutableIntStateOf(Random.nextInt(13)) }
    var isGameOver by remember { mutableStateOf(false) }
    var isMouthClosing by remember { mutableStateOf(false) }

    // Animation: Jaw rotation
    val jawAngle by animateFloatAsState(
        targetValue = if (isMouthClosing) 45f else 0f, // 0 is open, 45 closes it down visually
        animationSpec = tween(durationMillis = 300, easing = LinearEasing), label = "jawAngle"
    )
    
    // Shake Effect on biting
    val shakeOffset by animateFloatAsState(
        targetValue = if (isMouthClosing) 10f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shake"
    )

    fun onToothClick(index: Int) {
        if (isGameOver) return
        
        val newList = teethState.toMutableList()
        newList[index] = true
        teethState = newList
        
        if (index == badToothIndex) {
            isMouthClosing = true
            isGameOver = true
            // Vibrate? Sound?
        }
    }

    fun resetGame() {
        teethState = List(13) { false }
        badToothIndex = Random.nextInt(13)
        isGameOver = false
        isMouthClosing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Croc Dentist", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ToyGreenLight,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFAED581)), // Softer Green Background
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                
                // GAME AREA
                Box(
                    modifier = Modifier.size(360.dp, 500.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // LOWER JAW (Static base)
                    ToyLowerJaw(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        teethState = teethState,
                        onToothClick = ::onToothClick,
                        colors = ToyColors(ToyGreenLight, ToyGreenDark, MouthRed, ToothWhite, ToothPressed)
                    )
                    
                    // UPPER JAW (Animated)
                    // Pivot roughly at the back
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 80.dp) // Push down to hinge
                            .graphicsLayer {
                                rotationX = jawAngle // Rotate down
                                translationY = if(isMouthClosing) 150f else 0f // Move down to "snap"
                                // Add Shake
                                if (isMouthClosing) {
                                    translationX = if (System.currentTimeMillis() % 100 > 50) 5f else -5f
                                }
                            }
                    ) {
                         ToyUpperJaw(colors = ToyColors(ToyGreenLight, ToyGreenDark, MouthRed, ToothWhite, ToothPressed))
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                if (isGameOver) {
                    Text(
                        "OUCH!", 
                        style = MaterialTheme.typography.displayMedium, 
                        color = Color.Red,
                        fontWeight = FontWeight.Black
                    )
                    Button(
                        onClick = ::resetGame,
                        colors = ButtonDefaults.buttonColors(containerColor = ToyGreenDark)
                    ) {
                        Text("TRY AGAIN", fontSize = 20.sp)
                    }
                } else {
                    Text(
                        "Find the sore tooth...",
                        style = MaterialTheme.typography.titleLarge,
                         fontWeight = FontWeight.Bold,
                         color = Color(0xFF33691E)
                    )
                }
            }
        }
    }
}

// --- TOY COMPOSABLES ---

data class ToyColors(val light: Color, val dark: Color, val red: Color, val tooth: Color, val pressed: Color)

@Composable
fun ToyLowerJaw(
    modifier: Modifier = Modifier,
    teethState: List<Boolean>,
    onToothClick: (Int) -> Unit,
    colors: ToyColors
) {
    Box(modifier = modifier.size(320.dp, 220.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // 1. Green Rim (The plastic body)
            // Draw simplified U shape for jaw body
             drawArc(
                color = colors.light,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(0f, 0f), // Fills bottom half roughly
                size = Size(w, h * 1.8f) // Stretched
            )
             // Border
             drawArc(
                color = colors.dark,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(0f, 0f), 
                size = Size(w, h * 1.8f),
                style = Stroke(width = 8f)
            )

            // 2. Red Interior (The Mouth)
            drawArc(
                color = colors.red,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.1f, h * 0.1f), 
                size = Size(w * 0.8f, h * 1.4f)
            )
        }
        
        // 3. TEETH
        // Arranged in a semi-circle inside the red area
        Box(modifier = Modifier.fillMaxSize().padding(top = 40.dp)) {
             teethState.forEachIndexed { index, isPressed ->
                val totalAngle = 180f
                val startAngle = 0f
                val anglePerTooth = totalAngle / (teethState.size + 1)
                
                // Calculate position along arc
                val angleDeg = 180f - (startAngle + (index + 1) * anglePerTooth)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                
                // Adjust radius to fit
                val radiusX = 130.dp
                val radiusY = 90.dp // flattened oval
                
                val xOffset = 145.dp + (radiusX.value * cos(angleRad).toFloat()).dp
                val yOffset = 50.dp + (radiusY.value * sin(angleRad).toFloat()).dp
                
                ToyTooth(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(24.dp, 32.dp)
                        .graphicsLayer {
                             rotationZ = 90f - angleDeg // Rotate to face outward
                        },
                    isPressed = isPressed,
                    colors = colors,
                    onClick = { onToothClick(index) }
                )
             }
        }
    }
}

@Composable
fun ToyUpperJaw(colors: ToyColors) {
    Box(modifier = Modifier.size(340.dp, 200.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // 1. The Head (Lid)
            val headPath = Path().apply {
                // Top dome
                moveTo(w * 0.1f, h * 0.6f)
                cubicTo(
                    w * 0.1f, h * 0.1f, 
                    w * 0.9f, h * 0.1f, 
                    w * 0.9f, h * 0.6f
                )
                // Snout ridges
                quadraticTo(w * 0.8f, h * 0.5f, w * 0.7f, h * 0.6f)
                quadraticTo(w * 0.6f, h * 0.55f, w * 0.5f, h * 0.6f) // Center dip
                quadraticTo(w * 0.4f, h * 0.55f, w * 0.3f, h * 0.6f)
                quadraticTo(w * 0.2f, h * 0.5f, w * 0.1f, h * 0.6f)
                close()
            }
            
            drawPath(path = headPath, color = colors.light)
            drawPath(path = headPath, color = colors.dark, style = Stroke(width = 6f))

            // 2. Eyes (Pop out)
            // Left Eye
            val eyeY = h * 0.25f
            val eyeRadius = 35f
            drawCircle(Color.White, radius = eyeRadius, center = Offset(w * 0.25f, eyeY))
            drawCircle(Color.Black, radius = eyeRadius, center = Offset(w * 0.25f, eyeY), style = Stroke(width = 3f))
            // Blue Iris
            drawCircle(Color(0xFF29B6F6), radius = 18f, center = Offset(w * 0.25f + 5f, eyeY))
            // Pupil
            drawCircle(Color.Black, radius = 10f, center = Offset(w * 0.25f + 5f, eyeY))
            // Reflection
            drawCircle(Color.White, radius = 5f, center = Offset(w * 0.25f, eyeY - 8f))

            // Right Eye
             drawCircle(Color.White, radius = eyeRadius, center = Offset(w * 0.75f, eyeY))
             drawCircle(Color.Black, radius = eyeRadius, center = Offset(w * 0.75f, eyeY), style = Stroke(width = 3f))
             drawCircle(Color(0xFF29B6F6), radius = 18f, center = Offset(w * 0.75f - 5f, eyeY))
             drawCircle(Color.Black, radius = 10f, center = Offset(w * 0.75f - 5f, eyeY))
             drawCircle(Color.White, radius = 5f, center = Offset(w * 0.75f - 10f, eyeY - 8f))
             
             // Nostrils
             drawOval(colors.dark, topLeft = Offset(w * 0.4f, h * 0.45f), size = Size(15f, 10f))
             drawOval(colors.dark, topLeft = Offset(w * 0.55f, h * 0.45f), size = Size(15f, 10f))
        }
    }
}

@Composable
fun ToyTooth(
    modifier: Modifier,
    isPressed: Boolean,
    colors: ToyColors,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = if (isPressed) colors.pressed else colors.tooth
            
            // Rounded Rectangle / Capsule Shape
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(15f, 15f),
                size = size
            )
            // Border/Depth
            drawRoundRect(
                color = email_gray_border(),
                cornerRadius = CornerRadius(15f, 15f),
                size = size,
                style = Stroke(width = 2f)
            )
        }
    }
}
fun email_gray_border() = Color.LightGray
