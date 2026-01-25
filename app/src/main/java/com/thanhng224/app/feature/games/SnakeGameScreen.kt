package com.thanhng224.app.feature.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.abs

@Suppress("DEPRECATION")
@Composable
fun SnakeGameScreen(
    navController: NavController,
    viewModel: SnakeViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val snakeBody by viewModel.snakeBody.collectAsState()
    val food by viewModel.food.collectAsState()
    val foodType by viewModel.foodType.collectAsState()
    val score by viewModel.score.collectAsState()
    val highScore by viewModel.highScore.collectAsState(initial = 0)

    // Animations
    val eatingAnim = remember { Animatable(1f) }
    LaunchedEffect(score) {
        if (score > 0) {
            eatingAnim.snapTo(1.3f)
            eatingAnim.animateTo(1f)
        }
    }

    // High Score Logic
    val isNewRecord = score > highScore && score > 0
    val highScoreScale by animateFloatAsState(
        targetValue = if (isNewRecord) 1.3f else 1f,
        label = "scale"
    )
    val highScoreColor by animateColorAsState(
         targetValue = if (isNewRecord) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
         label = "color"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Marcadores ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score: $score",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isNewRecord) "NEW RECORD: $score" else "High Score: $highScore",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = highScoreColor,
                modifier = Modifier.graphicsLayer {
                    scaleX = highScoreScale
                    scaleY = highScoreScale
                }
            )
        }

        // --- Tablero de Juego ---
        SnakeBoard(
            gameState = gameState,
            snakeBody = snakeBody,
            food = food,
            foodType = foodType,
            currentDirection = viewModel.getCurrentDirection(),
            isHealthyMode = viewModel.isHealthyMode.collectAsState().value,
            eatingScale = eatingAnim.value,
            onDirectionChange = { viewModel.changeDirection(it) },
            onStartGame = { viewModel.startGame() }
        )
    }

    // --- POPUP FINAL ---
    if (gameState == GameState.GAMEOVER || gameState == GameState.WON) {
        FinalScoreDialog(
            gameState = gameState,
            finalScore = score,
            onRestart = { viewModel.startGame() },
            onExit = { navController.popBackStack() } 
        )
    }
}

@Composable
private fun SnakeBoard(
    gameState: GameState,
    snakeBody: List<Point>,
    food: Point,
    foodType: FoodType,
    currentDirection: Direction,
    isHealthyMode: Boolean,
    eatingScale: Float,
    onDirectionChange: (Direction) -> Unit,
    onStartGame: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Protect against DivisionByZero
            .aspectRatio(if(GRID_ROWS > 0) GRID_COLS.toFloat() / GRID_ROWS.toFloat() else 0.6f)
            .padding(4.dp) 
            .clip(RoundedCornerShape(16.dp)) 
            .background(Color(0xFF81C784)) 
            .pointerInput(gameState) {
                if (gameState == GameState.PLAYING) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        when {
                            abs(x) > abs(y) -> if (x > 0) onDirectionChange(Direction.RIGHT) else onDirectionChange(Direction.LEFT)
                            else -> if (y > 0) onDirectionChange(Direction.DOWN) else onDirectionChange(Direction.UP)
                        }
                    }
                } else {
                    detectTapGestures {
                        if (gameState == GameState.IDLE) onStartGame()
                    }
                }
            }
    ) {
        // HEALTHY MODE
        val borderColor = if (isHealthyMode) animateColorAsState(
            targetValue = if(System.currentTimeMillis() % 500 < 250) Color.Yellow else Color(0xFFFFD700),
            label = "flash"
        ).value else Color.Transparent
        
        if (isHealthyMode) {
            Box(Modifier.fillMaxSize().background(Color.Yellow.copy(alpha=0.1f)).border(4.dp, borderColor))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellPx = if(GRID_COLS > 0) size.width / GRID_COLS else 0f
            if (cellPx <= 0.1f) return@Canvas // Prevent crash if measured size is invalid

            // Grid
            val bgColor = Color(0xFF263238) 
            val gridColor = Color.White.copy(alpha = 0.05f) 
            drawRect(color = bgColor, size = size)

            val strokeWidth = 1.dp.toPx()
            for (i in 0..GRID_COLS) {
                drawLine(gridColor, Offset(i * cellPx, 0f), Offset(i * cellPx, size.height), strokeWidth)
            }
            for (j in 0..GRID_ROWS) {
                drawLine(gridColor, Offset(0f, j * cellPx), Offset(size.width, j * cellPx), strokeWidth)
            }

            // SNAKE BODY & HEAD
            snakeBody.asReversed().forEachIndexed { index, point ->
                val isHead = index == snakeBody.lastIndex
                val color = if (isHead) Color(0xFF1B5E20) else Color(0xFF2E7D32)
                
                val topLeft = Offset(point.first * cellPx, point.second * cellPx)
                val center = Offset(topLeft.x + cellPx/2, topLeft.y + cellPx/2)
                
                if (isHead) {
                    // HEAD (Simple Geometric Shape)
                    // Scale effect when eating
                    val scale = if(eatingScale > 1f) eatingScale else 1f
                    val headSize = cellPx * scale
                    val headOffset = topLeft - Offset((headSize - cellPx)/2, (headSize - cellPx)/2)
                    
                    drawRoundRect(
                        color = color,
                        topLeft = headOffset,
                        size = androidx.compose.ui.geometry.Size(headSize, headSize),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(headSize/3, headSize/3)
                    )
                    
                    // Eyes (White Sclera + Black Pupil)
                    val eyeRadius = headSize * 0.15f
                    val pupilRadius = headSize * 0.07f
                    val eyeOffset = headSize * 0.25f
                    
                    // Calculate Eye Positions based on Direction
                    val (leftEye, rightEye) = when(currentDirection) {
                         Direction.UP -> Pair(
                             Offset(center.x - eyeOffset, center.y - eyeOffset), 
                             Offset(center.x + eyeOffset, center.y - eyeOffset)
                         )
                         Direction.DOWN -> Pair(
                             Offset(center.x + eyeOffset, center.y + eyeOffset), 
                             Offset(center.x - eyeOffset, center.y + eyeOffset)
                         )
                         Direction.LEFT -> Pair(
                             Offset(center.x - eyeOffset, center.y + eyeOffset), 
                             Offset(center.x - eyeOffset, center.y - eyeOffset)
                         )
                         Direction.RIGHT -> Pair(
                             Offset(center.x + eyeOffset, center.y - eyeOffset), 
                             Offset(center.x + eyeOffset, center.y + eyeOffset)
                         )
                    }
                    
                    // Draw Eye 1
                    drawCircle(Color.White, radius = eyeRadius, center = leftEye)
                    drawCircle(Color.Black, radius = pupilRadius, center = leftEye)
                    
                    // Draw Eye 2
                    drawCircle(Color.White, radius = eyeRadius, center = rightEye)
                    drawCircle(Color.Black, radius = pupilRadius, center = rightEye)
                    
                } else {
                    // BODY SEGMENT
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                        size = androidx.compose.ui.geometry.Size(cellPx - 4f, cellPx - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }

            // FOOD (Colored Circles)
            val foodColor = when(foodType) {
                FoodType.GOLDEN -> Color(0xFFFFD700) 
                FoodType.HEALTHY -> Color(0xFF4CAF50) 
                else -> Color(0xFFF44336) 
            }
            
            val foodCenter = Offset((food.first * cellPx) + cellPx/2, (food.second * cellPx) + cellPx/2)
            drawCircle(foodColor, cellPx * 0.4f, foodCenter)
            
            if (foodType == FoodType.GOLDEN) {
                drawCircle(Color.White.copy(alpha=0.6f), cellPx * 0.15f, Offset(foodCenter.x - 4f, foodCenter.y - 4f))
            }
        }
        
        // IDLE OVERLAY
        if (gameState == GameState.IDLE) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TAP TO START",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Local helper removed (Use GameUtils.loadBitmapSafe in same package)

@Composable
private fun FinalScoreDialog(
    gameState: GameState,
    finalScore: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(if (gameState == GameState.WON) "¡Ganaste!" else "Fin del Juego", fontWeight = FontWeight.Bold)
        },
        text = { Text("Puntaje final: $finalScore") },
        confirmButton = { Button(onClick = onRestart) { Text("Reiniciar") } },
        dismissButton = { TextButton(onClick = onExit) { Text("Salir") } }
    )
}
