package com.thanhng224.app.feature.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- COLORS ---
val CrocSkinDark = Color(0xFF2E7D32)
val CrocSkinLight = Color(0xFF4CAF50)
val CrocScaleColor = Color(0xFF1B5E20)
val GumColor = Color(0xFFE57373)
val ToothWhite = Color(0xFFFAFAFA)
val ToothPressed = Color(0xFFBDBDBD) // Dirty/Pressed tooth
val EyeColor = Color(0xFFFFEB3B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrocodileGameScreen(navController: NavController) {
    val difficulty = navController.currentBackStackEntry?.arguments?.getInt("difficulty") ?: 0
    val numBadTeeth = difficulty + 1 
    
    // 13 Teeth
    var teeth by remember { mutableStateOf(List(13) { false }) } 
    var badToothIndices by remember(numBadTeeth) { 
        mutableStateOf(generateBadTeeth(numBadTeeth)) 
    }
    var isGameOver by remember { mutableStateOf(false) }
    
    // Animation for mouth closing
    // 1f = Open, 0f = Closed/Bitten
    val mouthOpenProgress by animateFloatAsState(
        targetValue = if (isGameOver) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f), label = "mouthAnim"
    )

    fun resetGame() {
        teeth = List(13) { false }
        badToothIndices = generateBadTeeth(numBadTeeth)
        isGameOver = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crocodile Challenge", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { resetGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CrocSkinDark,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF81C784), Color(0xFFC8E6C9)))), // Swampy gradient
            contentAlignment = Alignment.Center
        ) {
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                
                // MAIN GAME AREA
                Box(
                    modifier = Modifier.size(360.dp, 400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. LOWER JAW (Base + Teeth)
                    LowerJaw(
                        modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-20).dp),
                        teethState = teeth,
                        onToothClick = { index ->
                            if (!isGameOver && !teeth[index]) {
                                if (badToothIndices.contains(index)) {
                                    isGameOver = true
                                } else {
                                    val newTeeth = teeth.toMutableList()
                                    newTeeth[index] = true
                                    teeth = newTeeth
                                }
                            }
                        }
                    )
                    
                    // 2. UPPER JAW (Rotates/Translates down)
                    // Pivot is roughly at the back of the head
                    UpperJaw(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 40.dp) // Adjust initial position relative to lower jaw
                            .graphicsLayer {
                                // Simple hinge logic: Rotate from "Open" (-40 deg) to "Closed" (0 deg) ?
                                // Or purely translation for simplicity in Top-Down view?
                                // User reference looked like a side/iso view head.
                                // Let's simplify: Rotate the upper jaw around a back pivot.
                                
                                val openAngle = -45f
                                val closedAngle = 5f // Slightly overlap
                                rotationX = openAngle + (closedAngle - openAngle) * (1f - mouthOpenProgress)
                                
                                // Adjust Y to meet the lower jaw
                                translationY = (1f - mouthOpenProgress) * 150f
                            },
                        isAngry = isGameOver
                    )
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                if (isGameOver) {
                    Text(
                        "OUCH! \uD83E\uDDB7",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { resetGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = CrocSkinDark)
                    ) {
                        Text("TRY AGAIN", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        "Find the sore tooth...",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun LowerJaw(
    modifier: Modifier = Modifier,
    teethState: List<Boolean>,
    onToothClick: (Int) -> Unit
) {
    Box(modifier = modifier.size(320.dp, 200.dp)) {
        // Organic Jaw Shape
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            val path = Path().apply {
                moveTo(w * 0.1f, h * 0.3f)
                // Curve for the back left
                quadraticTo(w * 0.05f, h * 0.8f, w * 0.2f, h * 0.9f)
                // Bottom curve (Chin)
                quadraticTo(w * 0.5f, h * 1.05f, w * 0.8f, h * 0.9f)
                // Back right
                quadraticTo(w * 0.95f, h * 0.8f, w * 0.9f, h * 0.3f)
                // Inner mouth curve (Gum line placeholder)
                quadraticTo(w * 0.5f, h * 0.45f, w * 0.1f, h * 0.3f)
                close()
            }
            
            drawPath(path, color = CrocSkinLight)
            drawPath(path, color = CrocSkinDark, style = Stroke(width = 6f))
            
            // Texture Scales
            for(i in 0..15) {
                val x = w * (0.2f + Random.nextFloat() * 0.6f)
                val y = h * (0.6f + Random.nextFloat() * 0.3f)
                drawCircle(CrocScaleColor.copy(alpha=0.3f), radius = 5f + Random.nextFloat()*8f, center = Offset(x,y))
            }

            // Gums
            val gumPath = Path().apply {
                moveTo(w * 0.15f, h * 0.35f)
                quadraticTo(w * 0.5f, h * 0.55f, w * 0.85f, h * 0.35f)
                // Thickness
                quadraticTo(w * 0.5f, h * 0.75f, w * 0.15f, h * 0.35f) 
            }
            drawPath(gumPath, color = GumColor)
        }
        
        // TEETH LAYOUT
        // We place transparent clickables over the visual teeth drawn on canvas? 
        // Or render individual Composables. Composables are easier for interaction.
        Box(modifier = Modifier.fillMaxSize()) {
            val radiusX = 130f
            val radiusY = 70f
            val centerX = 160.dp.value // Approx dp to px conversion is sketchy without density, but let's rely on Box sizing
            // Let's use absolute logic inside a custom layout or fixed offsets.
            // Fixed offsets for 13 teeth along a curve.
            
            teethState.forEachIndexed { index, isPressed ->
                // Arc from 10 degrees to 170 degrees
                val totalAngle = 160f
                val startAngle = 10f
                val anglePerTooth = totalAngle / (teethState.size - 1)
                val angleDeg = 180f - (startAngle + index * anglePerTooth) // 170 down to 10
                val angleRad = Math.toRadians(angleDeg.toDouble())
                
                // Elliptical positioning
                // These values need tuning to match the jaw drawing
                val xOffset = 160.dp + (110.dp * cos(angleRad).toFloat()) - 15.dp // Center - width/2
                val yOffset = 60.dp + (60.dp * sin(angleRad).toFloat()) - 10.dp
                
                Tooth(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(30.dp, 36.dp),
                    isPressed = isPressed,
                    onClick = { onToothClick(index) }
                )
            }
        }
    }
}

@Composable
fun Tooth(
    modifier: Modifier,
    isPressed: Boolean,
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
            val w = size.width
            val h = size.height
            val color = if (isPressed) ToothPressed else ToothWhite
            
            // Triangular / Conical Shape
            val path = Path().apply {
                moveTo(0f, h) // Bottom Left
                quadraticTo(w * 0.5f, -h * 0.2f, w, h) // Peak
                close()
            }
            
            drawPath(path, color)
            drawPath(path, Color(0xFFE0E0E0), style = Stroke(width = 2f))
        }
    }
}

@Composable
fun UpperJaw(
    modifier: Modifier = Modifier,
    isAngry: Boolean
) {
    Box(modifier = modifier.size(340.dp, 220.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Head Shape (Top View / Slight Perspective)
            val path = Path().apply {
                moveTo(w * 0.2f, h * 0.9f) // Jaw hinge left
                // Snout Left
                quadraticTo(w * 0.1f, h * 0.5f, w * 0.3f, h * 0.1f) 
                // Snout Top
                quadraticTo(w * 0.5f, h * 0.0f, w * 0.7f, h * 0.1f)
                // Snout Right
                quadraticTo(w * 0.9f, h * 0.5f, w * 0.8f, h * 0.9f)
                // Back of head
                quadraticTo(w * 0.5f, h * 0.8f, w * 0.2f, h * 0.9f)
                close()
            }
            
            drawPath(path, CrocSkinLight)
            drawPath(path, CrocSkinDark, style = Stroke(width = 8f))
            
            // Texture Scales
            for(i in 0..20) {
                val x = w * (0.3f + Random.nextFloat() * 0.4f)
                val y = h * (0.1f + Random.nextFloat() * 0.5f)
                drawCircle(CrocScaleColor.copy(alpha=0.4f), radius = 4f + Random.nextFloat()*10f, center = Offset(x,y))
            }
            
            // Eyes (Ridges)
            drawOval(
                color = CrocSkinDark,
                topLeft = Offset(w * 0.15f, h * 0.45f),
                size = Size(w * 0.15f, h * 0.15f)
            )
             drawOval(
                color = CrocSkinDark,
                topLeft = Offset(w * 0.7f, h * 0.45f),
                size = Size(w * 0.15f, h * 0.15f)
            )
            
            // Eyeballs
            val eyeY = h * 0.48f
            drawCircle(Color.White, radius = 18f, center = Offset(w * 0.225f, eyeY))
            // Pupil
            drawOval(
                color = Color.Black,
                topLeft = Offset(w * 0.225f - 4f, eyeY - 10f),
                size = Size(8f, 20f)
            )
            
            drawCircle(Color.White, radius = 18f, center = Offset(w * 0.775f, eyeY))
             drawOval(
                color = Color.Black,
                topLeft = Offset(w * 0.775f - 4f, eyeY - 10f),
                size = Size(8f, 20f)
            )

            // Nostrils
            val nostrilColor = Color(0xFF1B5E20)
            drawOval(nostrilColor, topLeft = Offset(w * 0.42f, h * 0.15f), size = Size(16f, 10f))
             drawOval(nostrilColor, topLeft = Offset(w * 0.54f, h * 0.15f), size = Size(16f, 10f))
        }
    }
}

fun generateBadTeeth(count: Int): Set<Int> {
    val bad = mutableSetOf<Int>()
    val r = Random.Default
    while (bad.size < count) {
        bad.add(r.nextInt(13))
    }
    return bad
}
