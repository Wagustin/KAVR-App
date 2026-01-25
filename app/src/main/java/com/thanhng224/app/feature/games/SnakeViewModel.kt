package com.thanhng224.app.feature.games

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
import kotlin.random.Random

// Constants


enum class Direction { UP, DOWN, LEFT, RIGHT }
enum class GameState { IDLE, PLAYING, GAMEOVER, WON }
typealias Point = Pair<Int, Int>

@HiltViewModel
class SnakeViewModel @Inject constructor(
    private val appPreferences: AppPreferences
) : ViewModel() {

    companion object {
        const val GRID_COLS = 15
        const val GRID_ROWS = 25
    }

    private val _gameState = MutableStateFlow(GameState.IDLE)
    val gameState = _gameState.asStateFlow()

    private val _snakeBody = MutableStateFlow<List<Point>>(listOf(Point(5, 10), Point(4, 10), Point(3, 10)))
    val snakeBody = _snakeBody.asStateFlow()

    private val _food = MutableStateFlow(Point(0, 0))
    val food = _food.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    val highScore = appPreferences.highScore

    private var direction = Direction.RIGHT
    private var gameLoopJob: Job? = null
    
    // Difficulty delay map
    private val difficultyDelays = mapOf(
        0 to 200L, // Easy
        1 to 150L, // Medium
        2 to 100L  // Hard
    )
    private var currentDifficulty = 1

    init {
        resetGame()
    }
    
    fun setDifficulty(diff: Int) {
        currentDifficulty = diff
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
    
    fun getCurrentDirection(): Direction = direction
    
    // Check if mouth should be open (Visual only)
    fun isMouthOpen(): Boolean {
        val head = _snakeBody.value.firstOrNull() ?: return false
        val f = _food.value
        val dist = kotlin.math.abs(head.first - f.first) + kotlin.math.abs(head.second - f.second)
        return dist <= 2
    }

    private suspend fun gameLoop() {
        while (_gameState.value == GameState.PLAYING) {
            val baseDelay = difficultyDelays[currentDifficulty] ?: 150L
            // Slight speedup per 5 points, capped at -50ms
            val speedUp = (_score.value / 5) * 5L
            val finalDelay = (baseDelay - speedUp).coerceAtLeast(60L)
            
            delay(finalDelay)
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

        // Collision Check (Walls or Self)
        if (newHead.first !in 0 until GRID_COLS || 
            newHead.second !in 0 until GRID_ROWS || 
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
            newFood = Point(Random.nextInt(GRID_COLS), Random.nextInt(GRID_ROWS))
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
