package com.thanhng224.app.feature.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlin.math.abs
import com.thanhng224.app.R

@Composable
fun SnakeGameScreen(
    navController: NavController,
    viewModel: SnakeViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val snakeBody by viewModel.snakeBody.collectAsState()
    val food by viewModel.food.collectAsState()
    val score by viewModel.score.collectAsState()
    val highScore by viewModel.highScore.collectAsState(initial = 0)
    
    // Retrieve Difficulty from NavArgs if possible, or default
    val navBackStackEntry = navController.currentBackStackEntry
    val difficulty = navBackStackEntry?.arguments?.getInt("difficulty") ?: 1
    
    LaunchedEffect(Unit) {
        viewModel.setDifficulty(difficulty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Score Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Score: $score", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("High: $highScore", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        // --- Game Board ---
        SnakeBoard(
            gameState = gameState,
            snakeBody = snakeBody,
            food = food,
            currentDirection = viewModel.getCurrentDirection(),
            isMouthOpen = viewModel.isMouthOpen(),
            onDirectionChange = { viewModel.changeDirection(it) },
            onStartGame = { viewModel.startGame() }
        )
    }

    // --- Game Over Dialog ---
    if (gameState == GameState.GAMEOVER || gameState == GameState.WON) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (gameState == GameState.WON) "¡Ganaste!" else "Fin del Juego") },
            text = { Text("Puntaje final: $score") },
            confirmButton = { Button(onClick = { viewModel.startGame() }) { Text("Reiniciar") } },
            dismissButton = { TextButton(onClick = { navController.popBackStack() }) { Text("Salir") } }
        )
    }
}

@Composable
private fun SnakeBoard(
    gameState: GameState,
    snakeBody: List<Point>,
    food: Point,
    currentDirection: Direction,
    isMouthOpen: Boolean,
    onDirectionChange: (Direction) -> Unit,
    onStartGame: () -> Unit
) {
    // --- LOAD ASSETS SAFELY (Native Compose) ---
    val headUp = ImageBitmap.imageResource(R.drawable.snake_head_up)
    val headDown = ImageBitmap.imageResource(R.drawable.snake_head_down)
    val headLeft = ImageBitmap.imageResource(R.drawable.snake_head_left)
    val headRight = ImageBitmap.imageResource(R.drawable.snake_head_right)
    
    val headUpOpen = ImageBitmap.imageResource(R.drawable.snake_head_up_open)
    val headDownOpen = ImageBitmap.imageResource(R.drawable.snake_head_down_open)
    val headLeftOpen = ImageBitmap.imageResource(R.drawable.snake_head_left_open)
    val headRightOpen = ImageBitmap.imageResource(R.drawable.snake_head_right_open)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if(GRID_ROWS > 0) GRID_COLS.toFloat() / GRID_ROWS.toFloat() else 0.6f)
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF81C784)) // Grass Green
            .pointerInput(gameState) {
                if (gameState == GameState.PLAYING) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        if (abs(x) > abs(y)) {
                            if (x > 0) onDirectionChange(Direction.RIGHT) else onDirectionChange(Direction.LEFT)
                        } else {
                            if (y > 0) onDirectionChange(Direction.DOWN) else onDirectionChange(Direction.UP)
                        }
                    }
                } else {
                    detectTapGestures { if (gameState == GameState.IDLE) onStartGame() }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellPx = size.width / GRID_COLS
            if (cellPx <= 1f) return@Canvas

            // Draw Grid Lines (Subtle)
            val gridColor = Color.White.copy(alpha=0.1f)
            for (i in 0..GRID_COLS) drawLine(gridColor, Offset(i*cellPx, 0f), Offset(i*cellPx, size.height))
            for (j in 0..GRID_ROWS) drawLine(gridColor, Offset(0f, j*cellPx), Offset(size.width, j*cellPx))

            // Draw Food
            drawCircle(
                color = Color(0xFFD32F2F), // Red Apple
                radius = cellPx * 0.4f,
                center = Offset((food.first * cellPx) + cellPx/2, (food.second * cellPx) + cellPx/2)
            )

            // Draw Snake
            snakeBody.asReversed().forEachIndexed { index, point ->
                val isHead = index == snakeBody.lastIndex
                val topLeft = Offset(point.first * cellPx, point.second * cellPx)
                
                if (isHead) {
                    // Draw Custom Head
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
                    
                    // Slight scale up for the head
                    val headSize = (cellPx * 3.2f).toInt()
                    val offset = (headSize - cellPx) / 2
                    
                    drawImage(
                        image = image,
                        dstOffset = androidx.compose.ui.unit.IntOffset( (topLeft.x - offset).toInt(), (topLeft.y - offset).toInt() ),
                        dstSize = androidx.compose.ui.unit.IntSize(headSize, headSize)
                    )
                } else {
                    // Draw Body Segment
                    drawRoundRect(
                        color = Color(0xFF2E7D32),
                        topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                        size = androidx.compose.ui.geometry.Size(cellPx - 4f, cellPx - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }
        }
        
        // Idle Overlay
        if (gameState == GameState.IDLE) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.4f)), contentAlignment = Alignment.Center) {
                Text("TAP TO START", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
}
