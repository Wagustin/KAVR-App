package com.thanhng224.app.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhng224.app.core.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

typealias Point = Pair<Int, Int>

enum class GameState { IDLE, PLAYING, GAME_OVER }
enum class Direction { UP, DOWN, LEFT, RIGHT }

const val GRID_SIZE = 20

@HiltViewModel
class SnakeViewModel @Inject constructor(
    private val appPreferences: AppPreferences
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

    init {
        resetGame()
    }

    fun startGame() {
        if (_gameState.value != GameState.PLAYING) {
            resetGame()
            _gameState.value = GameState.PLAYING
            viewModelScope.launch {
                gameLoop()
            }
        }
    }

    fun changeDirection(newDirection: Direction) {
        val isOpposite = when(direction) {
            Direction.UP -> newDirection == Direction.DOWN
            Direction.DOWN -> newDirection == Direction.UP
            Direction.LEFT -> newDirection == Direction.RIGHT
            Direction.RIGHT -> newDirection == Direction.LEFT
        }
        if (!isOpposite && _gameState.value == GameState.PLAYING) {
            direction = newDirection
        }
    }

    private suspend fun gameLoop() {
        while (_gameState.value == GameState.PLAYING) {
            // Movimiento más fluido y rápido con el tiempo
            val currentDelay = (200 - (_score.value * 2)).coerceAtLeast(60)
            delay(currentDelay.toLong())
            moveSnake()
        }
    }

    private fun moveSnake() {
        val currentHead = _snakeBody.value.first()
        val newHead = when (direction) {
            Direction.UP -> currentHead.copy(second = currentHead.second - 1)
            Direction.DOWN -> currentHead.copy(second = currentHead.second + 1)
            Direction.LEFT -> currentHead.copy(first = currentHead.first - 1)
            Direction.RIGHT -> currentHead.copy(first = currentHead.first + 1)
        }

        if (isCollision(newHead)) {
            _gameState.value = GameState.GAME_OVER
            viewModelScope.launch {
                appPreferences.updateHighScore(_score.value)
            }
            return
        }

        val newBody = mutableListOf(newHead)
        newBody.addAll(_snakeBody.value)

        if (newHead == _food.value) {
            _score.update { it + 1 }
            generateFood()
        } else {
            newBody.removeLast()
        }

        _snakeBody.value = newBody
    }

    private fun isCollision(head: Point): Boolean {
        return head.first < 0 || head.first >= GRID_SIZE || head.second < 0 || head.second >= GRID_SIZE || _snakeBody.value.contains(head)
    }

    private fun generateFood() {
        var newFoodPosition: Point
        do {
            newFoodPosition = Point(Random.nextInt(GRID_SIZE), Random.nextInt(GRID_SIZE))
        } while (_snakeBody.value.contains(newFoodPosition))
        _food.value = newFoodPosition
    }

    private fun resetGame() {
        direction = Direction.RIGHT
        _score.value = 0
        _snakeBody.value = listOf(Point(5, 5), Point(4, 5), Point(3, 5))
        generateFood()
        _gameState.value = GameState.IDLE
    }
}
