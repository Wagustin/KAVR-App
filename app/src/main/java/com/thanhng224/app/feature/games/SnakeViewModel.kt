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

// --- Constantes del Juego ---
private const val CASUAL_DELAY = 150L
private const val TRYHARD_INITIAL_DELAY = 200L
private const val MIN_DELAY = 50L
private const val SPEED_INCREASE_PER_POINT = 10L
private const val WIN_CONDITION_SIZE = 50

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
    
    private val gameMode: Int = savedStateHandle.get<Int>("gameMode") ?: 0 // 0 = Casual, 1 = Tryhard

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
        if (_gameState.value != GameState.PLAYING) return
        
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
            val currentDelay = if (gameMode == 0) {
                CASUAL_DELAY // Modo Casual
            } else {
                // Modo Tryhard
                max(MIN_DELAY, TRYHARD_INITIAL_DELAY - (_score.value * SPEED_INCREASE_PER_POINT))
            }
            delay(currentDelay)
            moveSnake()
        }
    }

    private fun moveSnake() {
        if (_snakeBody.value.isEmpty()) return
        val currentHead = _snakeBody.value.first()
        val newHead = when (direction) {
            Direction.UP -> currentHead.copy(second = currentHead.second - 1)
            Direction.DOWN -> currentHead.copy(second = currentHead.second + 1)
            Direction.LEFT -> currentHead.copy(first = currentHead.first - 1)
            Direction.RIGHT -> currentHead.copy(first = currentHead.first + 1)
        }

        if (isCollision(newHead)) {
            endGame(didWin = false)
            return
        }

        val newBody = mutableListOf(newHead).apply { addAll(_snakeBody.value) }

        if (newHead == _food.value) {
            _score.update { it + 1 }
            generateFood()
            if (newBody.size > WIN_CONDITION_SIZE) {
                endGame(didWin = true)
                return
            }
        } else {
            newBody.removeLast()
        }

        _snakeBody.value = newBody
    }

    private fun isCollision(head: Point): Boolean {
        return head.first < 0 || head.first >= GRID_SIZE || head.second < 0 || head.second >= GRID_SIZE || _snakeBody.value.contains(head)
    }
    
    private fun endGame(didWin: Boolean) {
        gameLoopJob?.cancel()
        _gameState.value = if (didWin) GameState.WON else GameState.GAMEOVER
        viewModelScope.launch {
            appPreferences.updateHighScore(_score.value)
        }
    }

    private fun generateFood() {
        var newFoodPosition: Point
        do {
            newFoodPosition = Point(Random.nextInt(GRID_SIZE), Random.nextInt(GRID_SIZE))
        } while (_snakeBody.value.contains(newFoodPosition))
        _food.value = newFoodPosition
    }

    private fun resetGame() {
        gameLoopJob?.cancel()
        direction = Direction.RIGHT
        _score.value = 0
        _snakeBody.value = listOf(Point(5, 5), Point(4, 5), Point(3, 5))
        generateFood()
        _gameState.value = GameState.IDLE
    }
}
