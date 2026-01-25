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
            gameState = gameState,
            snakeBody = snakeBody,
            food = food,
            currentDirection = viewModel.getCurrentDirection(),
            onDirectionChange = { viewModel.changeDirection(it) },
            onStartGame = { viewModel.startGame() }
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
@Composable
private fun SnakeBoard(
    gameState: GameState,
    snakeBody: List<Point>,
    food: Point,
    currentDirection: Direction,
    // onDirectionChange, onStartGame...
    onDirectionChange: (Direction) -> Unit,
    onStartGame: () -> Unit
) {
    // 1. Safe Asset Loading (Native Compose)
    val headUp = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_up)
    val headDown = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_down)
    val headLeft = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_left)
    val headRight = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_right)
    
    val headUpOpen = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_up_open)
    val headDownOpen = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_down_open)
    val headLeftOpen = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_left_open)
    val headRightOpen = ImageBitmap.imageResource(com.thanhng224.app.R.drawable.snake_head_right_open)

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellPx = if(GRID_COLS > 0) size.width / GRID_COLS else 0f
            if (cellPx <= 0.1f) return@Canvas 

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

            // SNAKE
            snakeBody.asReversed().forEachIndexed { index, point ->
                val isHead = index == snakeBody.lastIndex
                
                val topLeft = Offset(point.first * cellPx, point.second * cellPx)
                
                if (isHead) {
                    // Mouth Open Logic
                    val dist = abs(point.first - food.first) + abs(point.second - food.second)
                    val isMouthOpen = dist <= 2
                    
                    val image = if (isMouthOpen) {
                        when(currentDirection) {
                            Direction.UP -> headUpOpen
                            Direction.DOWN -> headDownOpen
                            Direction.LEFT -> headLeftOpen
                            Direction.RIGHT -> headRightOpen
                        }
                    } else {
                        when(currentDirection) {
                            Direction.UP -> headUp
                            Direction.DOWN -> headDown
                            Direction.LEFT -> headLeft
                            Direction.RIGHT -> headRight
                        }
                    }
                    
                    val finalSize = (cellPx * 3.5f).toInt() // Big head
                    val offsetAdjustment = (finalSize - cellPx) / 2
                    
                    drawImage(
                        image = image,
                        dstOffset = androidx.compose.ui.unit.IntOffset(
                            (topLeft.x - offsetAdjustment).toInt(),
                            (topLeft.y - offsetAdjustment).toInt()
                        ),
                        dstSize = androidx.compose.ui.unit.IntSize(finalSize, finalSize)
                    )
                } else {
                    // Body
                    drawRoundRect(
                        color = Color(0xFF2E7D32),
                        topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                        size = androidx.compose.ui.geometry.Size(cellPx - 4f, cellPx - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }

            // FOOD (Simplest possible: Red Circle)
            val foodCenter = Offset((food.first * cellPx) + cellPx/2, (food.second * cellPx) + cellPx/2)
            drawCircle(Color(0xFFF44336), cellPx * 0.4f, foodCenter)
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
