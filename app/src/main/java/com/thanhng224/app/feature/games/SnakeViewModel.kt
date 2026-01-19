package com.thanhng224.app.feature.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhng224.app.core.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
import kotlin.random.Random



// --- AJUSTE DE VELOCIDADES ---
private const val CASUAL_DELAY = 200L         // Lento y relajante
private const val TRYHARD_START_DELAY = 150L  // Velocidad media inicial
private const val TRYHARD_MIN_DELAY = 40L     // Velocidad máxima injugable
private const val SPEED_STEP = 8L             // Cuánto baja el delay por cada manzana (más alto = acelera antes)

@HiltViewModel
class SnakeViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState.IDLE)
    val gameState = _gameState.asStateFlow()

    private val _snakeBody = MutableStateFlow<List<Point>>(emptyList())
    val snakeBody = _snakeBody.asStateFlow()

    private val _food = MutableStateFlow(Point(0, 0))
    val food = _food.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    val highScore = appPreferences.highScore

    private var direction = Direction.RIGHT
    private var gameLoopJob: Job? = null
    
    // Leemos el modo. 0 = Easy, 1 = Medium, 2 = Hard
    private val difficulty: Int = savedStateHandle.get<Int>("difficulty") ?: 0

    init {
        resetGame()
    }

    fun startGame() {
        if (_gameState.value == GameState.PLAYING) return
        resetGame()
        _gameState.value = GameState.PLAYING
        gameLoopJob = viewModelScope.launch {
            gameLoop()
        }
    }

    fun changeDirection(newDirection: Direction) {
        val isOpposite = when (direction) {
            Direction.UP -> newDirection == Direction.DOWN
            Direction.DOWN -> newDirection == Direction.UP
            Direction.LEFT -> newDirection == Direction.RIGHT
            Direction.RIGHT -> newDirection == Direction.LEFT
        }
        if (!isOpposite) {
            direction = newDirection
        }
    }

    private suspend fun gameLoop() {
        while (_gameState.value == GameState.PLAYING) {
            val delayTime = when (difficulty) {
                0 -> CASUAL_DELAY // Easy: Fixed Slow
                1 -> { // Medium: Moderate Start, Slow Accel
                     val speedUp = _score.value * (SPEED_STEP / 2)
                     max(TRYHARD_MIN_DELAY + 40, TRYHARD_START_DELAY - speedUp)
                }
                else -> { // Hard: Fast Start, Fast Accel
                     val speedUp = _score.value * SPEED_STEP
                     max(TRYHARD_MIN_DELAY, (TRYHARD_START_DELAY - 50) - speedUp)
                }
            }
            
            delay(delayTime)
            moveSnake()
        }
    }

    private fun moveSnake() {
        if (_snakeBody.value.isEmpty()) return
        
        val head = _snakeBody.value.first()
        val newHead = when (direction) {
            Direction.UP -> Point(head.first, head.second - 1)
            Direction.DOWN -> Point(head.first, head.second + 1)
            Direction.LEFT -> Point(head.first - 1, head.second)
            Direction.RIGHT -> Point(head.first + 1, head.second)
        }

        // Colisión Paredes o Cuerpo
        if (newHead.first !in 0 until GRID_SIZE || 
            newHead.second !in 0 until GRID_SIZE || 
            _snakeBody.value.contains(newHead)) {
            endGame()
            return
        }

        val newBody = _snakeBody.value.toMutableList()
        newBody.add(0, newHead)

        if (newHead == _food.value) {
            _score.update { it + 1 }
            generateFood()
        } else {
            newBody.removeAt(newBody.size - 1)
        }
        _snakeBody.value = newBody
    }

    private fun generateFood() {
        var newFood: Point
        do {
            newFood = Point(Random.nextInt(GRID_SIZE), Random.nextInt(GRID_SIZE))
        } while (_snakeBody.value.contains(newFood))
        _food.value = newFood
    }

    private fun endGame(win: Boolean = false) {
        gameLoopJob?.cancel()
        _gameState.value = if (win) GameState.WON else GameState.GAMEOVER
        viewModelScope.launch {
            appPreferences.updateHighScore(_score.value)
        }
    }

    private fun resetGame() {
        direction = Direction.RIGHT
        _score.value = 0
        _snakeBody.value = listOf(Point(5, 10), Point(4, 10), Point(3, 10))
        generateFood()
        _gameState.value = GameState.IDLE
    }
}