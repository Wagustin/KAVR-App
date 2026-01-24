

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

// --- TIPOS DE COMIDA ---
enum class FoodType {
    JUNK, GOLDEN, HEALTHY
}

// --- AJUSTE DE VELOCIDADES ---
private const val CASUAL_DELAY = 220L         // Lento y relajante (Slower)
private const val TRYHARD_START_DELAY = 170L  // Velocidad media inicial (Slower)
private const val TRYHARD_MIN_DELAY = 40L     // Velocidad máxima injugable
private const val SPEED_STEP = 8L             // Cuánto baja el delay por cada manzana

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
    
    // Nueva info de comida
    private val _foodType = MutableStateFlow(FoodType.JUNK)
    val foodType = _foodType.asStateFlow()
    
    private val _foodEmoji = MutableStateFlow("🍎")
    val foodEmoji = _foodEmoji.asStateFlow()
    
    // Healthy Mode
    private var healthyModeEndTime = 0L
    private val _isHealthyMode = MutableStateFlow(false)
    val isHealthyMode = _isHealthyMode.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score = _score.asStateFlow()

    val highScore = appPreferences.highScore

    private var direction = Direction.RIGHT
    private var gameLoopJob: Job? = null
    
    // Leemos el modo. 0 = Easy, 1 = Medium, 2 = Hard
    private val difficulty: Int = savedStateHandle.get<Int>("difficulty") ?: 0

    // EMOJIS
    private val junkFoods = listOf("🍔", "🍕", "🌭", "🍟", "🌮", "🍩", "🍪", "🍦", "🍫")
    private val healthyFoods = listOf("🥗", "🥦", "🥕", "🍎", "🍇", "🍉", "🍓", "🥑", "🥒")
    private val goldenFood = "🌟"

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
    
    // Public getter for direction to query in UI for Head Rotation
    fun getCurrentDirection(): Direction = direction

    private suspend fun gameLoop() {
        while (_gameState.value == GameState.PLAYING) {
            val currentTime = System.currentTimeMillis()
            
            // Check Healthy Mode Expiry
            if (_isHealthyMode.value && currentTime > healthyModeEndTime) {
                _isHealthyMode.value = false
                // If curr food was healthy type, maybe regen to junk? Or just keep until eaten.
                // Generar nueva comida para "transformar" de vuelta a chatarra si no la comió
                if (_foodType.value == FoodType.HEALTHY) {
                    generateFood()
                }
            }
            
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
        if (newHead.first !in 0 until GRID_COLS || 
            newHead.second !in 0 until GRID_ROWS || 
            _snakeBody.value.contains(newHead)) {
            endGame()
            return
        }

        val newBody = _snakeBody.value.toMutableList()
        newBody.add(0, newHead)

        if (newHead == _food.value) {
            // SCORING LOGIC
            val points = when(_foodType.value) {
                FoodType.GOLDEN -> 5
                FoodType.HEALTHY -> 3
                FoodType.JUNK -> 1
            }
            _score.update { it + points }
            
            // MODE TRIGGER
            if (_foodType.value == FoodType.GOLDEN) {
                _isHealthyMode.value = true
                healthyModeEndTime = System.currentTimeMillis() + 15_000L // 15 seconds
            }
            
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
        
        // DETERMINE TYPE
        if (_isHealthyMode.value) {
            _foodType.value = FoodType.HEALTHY
            _foodEmoji.value = healthyFoods.random()
        } else {
            // 10% chance for Golden
            if (Random.nextFloat() < 0.10f) {
                _foodType.value = FoodType.GOLDEN
                _foodEmoji.value = goldenFood
            } else {
                _foodType.value = FoodType.JUNK
                _foodEmoji.value = junkFoods.random()
            }
        }
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
        _isHealthyMode.value = false
        generateFood()
        _gameState.value = GameState.IDLE
    }
}