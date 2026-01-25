package com.thanhng224.app.feature.games

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ArrowBack


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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlin.math.abs
import com.thanhng224.app.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
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

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Snake", fontWeight = FontWeight.Bold)
                        // Score in AppBar for better visibility
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$highScore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
    // Root container with Dark Background for "Borders"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF263238)), // Dark Blue-Grey Background (Hole-in-one style)
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Score Card ---
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = "SCORE: $score",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700), // Gold
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        // --- Game Board Container ---
        // This box constrains the board size and provides the "Dark Margins" effect relative to the screen
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Restrict width to 90% of screen = Visible side margins
                .fillMaxHeight(0.75f) // Restrict height = Visible top/bottom margins
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black) // Inner dark frame
                .padding(4.dp), // Thin black border line
            contentAlignment = Alignment.Center
        ) {
            SnakeBoard(
                gameState = gameState,
                snakeBody = snakeBody,
                food = food,
                currentDirection = viewModel.getCurrentDirection(),
                isMouthOpen = viewModel.isMouthOpen(),
                score = score,
                onDirectionChange = { viewModel.changeDirection(it) },
                onStartGame = { viewModel.startGame() }
            )
        }
    }

    // --- Game Over Dialog ---
    if (gameState == GameState.GAMEOVER || gameState == GameState.WON) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(if (gameState == GameState.WON) "¡Ganaste! 🎉" else "Fin del Juego 💀") },
            text = { 
                Column {
                    Text("Puntaje final: $score")
                    if (score > (highScore ?: 0)) {
                        Text("¡Nuevo Récord!", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = { Button(onClick = { viewModel.startGame() }) { Text("Jugar de nuevo") } },
            dismissButton = { TextButton(onClick = { navController.popBackStack() }) { Text("Salir") } }
        )
    }
    }
}

@Composable
private fun SnakeBoard(
    gameState: GameState,
    snakeBody: List<Point>,
    food: Point,
    currentDirection: Direction,
    isMouthOpen: Boolean,
    score: Int,
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

    // Colors
    val boardColor1 = Color(0xFFACCE72) // Light Grass
    val boardColor2 = Color(0xFFA2C765) // Darker Grass (Checkerboard)
    val bodyColor = Color(0xFF458648) // Dark Green Body

    // Haptics
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(score) {
        if (score > 0) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }
    LaunchedEffect(gameState) {
        if (gameState == GameState.GAMEOVER || gameState == GameState.WON) {
             haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    // Samsung A12 Optimization: Removed expensive RenderEffect Blur.
    // Low-end GPUs struggle with real-time blur. Using simple dim overlay instead.

    // Removed watermark logic for cleanliness
    
    Box(
        modifier = Modifier
            .fillMaxSize() // Fill the constrained container defined above
            .clip(RoundedCornerShape(20.dp)) // Inner roundness
            .background(boardColor1)
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
            val cellPx = size.width / SnakeViewModel.GRID_COLS
            if (cellPx <= 1f) return@Canvas



            // 1. Draw Checkerboard Background
                for (i in 0 until SnakeViewModel.GRID_COLS) {
                    for (j in 0 until SnakeViewModel.GRID_ROWS) {
                        if ((i + j) % 2 == 1) {
                            drawRect(
                                color = boardColor2,
                                topLeft = Offset(i * cellPx, j * cellPx),
                                size = androidx.compose.ui.geometry.Size(cellPx, cellPx)
                            )
                        }
                    }
                }

                // 2. Draw Food
                val foodCenter = Offset((food.first * cellPx) + cellPx/2, (food.second * cellPx) + cellPx/2)
                drawCircle(color = Color.Black.copy(alpha = 0.2f), radius = cellPx * 0.35f, center = Offset(foodCenter.x + 4f, foodCenter.y + 4f))
                drawCircle(color = Color(0xFFE53935), radius = cellPx * 0.4f, center = foodCenter)
                drawCircle(color = Color(0xFF81C784), radius = cellPx * 0.15f, center = Offset(foodCenter.x + cellPx*0.2f, foodCenter.y - cellPx*0.3f))

                // 3. Draw Snake
                snakeBody.asReversed().forEachIndexed { index, point ->
                    val isHead = index == snakeBody.lastIndex
                    val topLeft = Offset(point.first * cellPx, point.second * cellPx)
                    
                    if (isHead) {
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
                        val headSize = (cellPx * 2.8f).toInt()
                        val offset = (headSize - cellPx) / 2
                        drawImage(image = image, dstOffset = androidx.compose.ui.unit.IntOffset( (topLeft.x - offset).toInt(), (topLeft.y - offset).toInt() ), dstSize = androidx.compose.ui.unit.IntSize(headSize, headSize))
                    } else {
                        val bodySize = cellPx * 0.9f
                        val bodyOffset = (cellPx - bodySize) / 2
                        drawRoundRect(color = bodyColor, topLeft = Offset(topLeft.x + bodyOffset, topLeft.y + bodyOffset), size = androidx.compose.ui.geometry.Size(bodySize, bodySize), cornerRadius = androidx.compose.ui.geometry.CornerRadius(bodySize * 0.3f, bodySize * 0.3f))
                    }
                }
            }

        
        // Idle/Game Over Overlay (Simple Dim)
        if (gameState != GameState.PLAYING) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.5f)), contentAlignment = Alignment.Center) {
                if (gameState == GameState.IDLE) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TAP PARA JUGAR", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Desliza para moverte", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha=0.8f))
                    }
                }
            }
        }
    }
}
