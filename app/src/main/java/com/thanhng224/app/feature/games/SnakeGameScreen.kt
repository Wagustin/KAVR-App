package com.thanhng224.app.feature.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlin.math.abs

@Suppress("DEPRECATION")
@Composable
fun SnakeGameScreen(
    navController: NavController, // Parámetro añadido para la navegación
    viewModel: SnakeViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val snakeBody by viewModel.snakeBody.collectAsState()
    val food by viewModel.food.collectAsState()
    val score by viewModel.score.collectAsState()
    val highScore by viewModel.highScore.collectAsState(initial = 0)

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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Score: $score",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "High Score: $highScore",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // --- Tablero de Juego ---
        SnakeBoard(
            gameState = gameState,
            snakeBody = snakeBody,
            food = food,
            foodEmoji = viewModel.foodEmoji.collectAsState().value,
            currentDirection = viewModel.getCurrentDirection(),
            isHealthyMode = viewModel.isHealthyMode.collectAsState().value,
            onDirectionChange = { viewModel.changeDirection(it) },
            onStartGame = { viewModel.startGame() }
        )
    }

    // --- POPUP FINAL (Game Over / Win) ---
    if (gameState == GameState.GAMEOVER || gameState == GameState.WON) {
        FinalScoreDialog(
            gameState = gameState,
            finalScore = score,
            onRestart = { viewModel.startGame() },
            onExit = { navController.popBackStack() } // Usa el NavController
        )
    }
}

@Composable
private fun SnakeBoard(
    gameState: GameState,
    snakeBody: List<Point>,
    food: Point,
    foodEmoji: String,
    currentDirection: Direction,
    isHealthyMode: Boolean,
    onDirectionChange: (Direction) -> Unit,
    onStartGame: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // --- LOAD SNAKE HEAD IMAGES ---
    // Standard Heads
    var headUp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var headDown by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var headLeft by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var headRight by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // Open Mouth Heads
    var headUpOpen by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var headDownOpen by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var headLeftOpen by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var headRightOpen by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // Load Standard
            headUp = loadBitmapFromDrawable(context, "snake_head_up")
            headDown = loadBitmapFromDrawable(context, "snake_head_down")
            headLeft = loadBitmapFromDrawable(context, "snake_head_left")
            headRight = loadBitmapFromDrawable(context, "snake_head_right")
            
            // Load Open
            headUpOpen = loadBitmapFromDrawable(context, "snake_head_up_open")
            headDownOpen = loadBitmapFromDrawable(context, "snake_head_down_open")
            headLeftOpen = loadBitmapFromDrawable(context, "snake_head_left_open")
            headRightOpen = loadBitmapFromDrawable(context, "snake_head_right_open")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp)) // Rounded board
            .background(Color(0xFF81C784)) // Base grass color
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
        // HEALTHY MODE VISUALS
        val borderColor = if (isHealthyMode) animateColorAsState(
            targetValue = if(System.currentTimeMillis() % 500 < 250) Color.Yellow else Color(0xFFFFD700),
            label = "flash"
        ).value else Color.Transparent
        
        if (isHealthyMode) {
            Box(Modifier.fillMaxSize().background(Color.Yellow.copy(alpha=0.1f)).border(4.dp, borderColor))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / GRID_SIZE
            val cellPx = size.width / GRID_SIZE
            
            // Text Paint for Emojis
            val textPaint = android.graphics.Paint().apply {
                textSize = cellPx * 0.8f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // 1. CHECKERBOARD BACKGROUND
            val darkGrass = Color(0xFF66BB6A)
            val lightGrass = Color(0xFF81C784)
            val goldenGrass = Color(0xFFFFEB3B).copy(alpha=0.3f)
            
            for (i in 0 until GRID_SIZE) {
                for (j in 0 until GRID_SIZE) {
                    val isEven = (i + j) % 2 == 0
                    var color = if (isEven) lightGrass else darkGrass
                    if (isHealthyMode && isEven) color = androidx.compose.ui.graphics.Color.Yellow.copy(alpha=0.2f)
                    
                    drawRect(
                        color = color,
                        topLeft = Offset(i * cellPx, j * cellPx),
                        size = androidx.compose.ui.geometry.Size(cellPx, cellPx)
                    )
                }
            }

            // 2. SNAKE BODY
            // Draw from tail to head
            snakeBody.asReversed().forEachIndexed { index, point ->
                val isHead = index == snakeBody.lastIndex
                val color = if (isHead) Color(0xFF1B5E20) else Color(0xFF2E7D32)
                
                val topLeft = Offset(point.first * cellPx, point.second * cellPx)
                val center = Offset(topLeft.x + cellPx/2, topLeft.y + cellPx/2)
                
                if (isHead) {
                    // Calculate distance to food
                    val dist = abs(point.first - food.first) + abs(point.second - food.second)
                    val isMouthOpen = dist <= 2
                    
                    // Try to use Bitmap
                    val bitmap = if (isMouthOpen) {
                        when(currentDirection) {
                            Direction.UP -> headUpOpen ?: headUp
                            Direction.DOWN -> headDownOpen ?: headDown
                            Direction.LEFT -> headLeftOpen ?: headLeft
                            Direction.RIGHT -> headRightOpen ?: headRight
                        }
                    } else {
                        when(currentDirection) {
                            Direction.UP -> headUp
                            Direction.DOWN -> headDown
                            Direction.LEFT -> headLeft
                            Direction.RIGHT -> headRight
                        }
                    } ?: headDown // Fallback to headDown or whatever available? Or shape
                    
                    if (bitmap != null) {
                            drawIntoCanvas { canvas ->
                                // "No hagas con un circulo" (No circle clip)
                                // "Se ven muy grande" (Too big) -> Reduce scales
                                
                                val boost = if (isMouthOpen) 1.15f else 1.0f
                                

                                
                                // Clean reset for new images
                                // Uniform scale for all directions since assets are now consistent
                                val baseScale = 3.2f 
                                
                                val scaleW = baseScale * boost
                                val scaleH = baseScale * boost
                                
                                val w = cellPx * scaleW
                                val h = cellPx * scaleH
                                
                                val offX = (w - cellPx) / 2
                                val offY = (h - cellPx) / 2
                                
                                val rect = androidx.compose.ui.geometry.Rect(
                                    left = topLeft.x - offX,
                                    top = topLeft.y - offY,
                                    right = topLeft.x - offX + w,
                                    bottom = topLeft.y - offY + h
                                )
                                
                                // Draw Bitmap DIRECTLY (No Circle Clip)
                                canvas.nativeCanvas.drawBitmap(
                                    bitmap, 
                                    null, 
                                    android.graphics.Rect(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()), 
                                    null
                                )
                            }
                    } else {
                        // FALLBACK HEAD
                        drawRoundRect(
                            color = color,
                            topLeft = topLeft,
                            size = androidx.compose.ui.geometry.Size(cellPx, cellPx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellPx/2, cellPx/2)
                        )
                        // Eyes
                        drawCircle(Color.White, radius = cellPx * 0.15f, center = Offset(center.x - cellPx*0.2f, center.y - cellPx*0.1f))
                        drawCircle(Color.White, radius = cellPx * 0.15f, center = Offset(center.x + cellPx*0.2f, center.y - cellPx*0.1f))
                        drawCircle(Color.Black, radius = cellPx * 0.07f, center = Offset(center.x - cellPx*0.2f, center.y - cellPx*0.1f))
                        drawCircle(Color.Black, radius = cellPx * 0.07f, center = Offset(center.x + cellPx*0.2f, center.y - cellPx*0.1f))
                    }
                } else {
                    // Body segment
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                        size = androidx.compose.ui.geometry.Size(cellPx - 4f, cellPx - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }

            // 3. FOOD (Emoji)
            val foodCenter = Offset((food.first * cellPx) + (cellPx / 2), (food.second * cellPx) + (cellPx / 2))
            
            drawIntoCanvas { canvas ->
                val xPos = (food.first * cellPx) + (cellPx / 2)
                val yPos = (food.second * cellPx) + (cellPx / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
                
                canvas.nativeCanvas.drawText(foodEmoji, xPos, yPos, textPaint)
            }
        }

        // INITIAL MESSAGE
        if (gameState == GameState.IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
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

@Composable
private fun FinalScoreDialog(
    gameState: GameState,
    finalScore: Int,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* No se puede cerrar tocando fuera */ },
        title = {
            Text(
                text = if (gameState == GameState.WON) "¡Ganaste!" else "Fin del Juego",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Puntaje final: $finalScore",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text("Reiniciar")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("Salir")
            }
        }
    )
}
