package com.thanhng224.app.feature.games

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryCard(val id: Int, val imageRes: Int, val isFaceUp: Boolean = false, val isMatched: Boolean = false)

data class MemoryUiState(
    val cards: List<MemoryCard> = emptyList(),
    val isMultiplayer: Boolean = false,
    val currentPlayer: Int = 1,
    val scoreP1: Int = 0,
    val scoreP2: Int = 0,
    val remainingAttempts: Int = 0, // Para modo Vidas
    val remainingTime: Long = 0,    // Para modo Tiempo (segundos)
    val isGameOver: Boolean = false,
    val isWin: Boolean = false,
    val gameResultText: String = ""
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryUiState())
    val state = _state.asStateFlow()

    private var flippedCardId: Int? = null
    private var timerJob: Job? = null
    private var isProcessing = false // Evita clicks múltiples durante la animación de error

    // Lista de recursos de fotos (Copiado de MemoriesViewModel + R.drawable prefix)
    // Asumimos que los imports de R están presentes o los agregamos.
    private val photoResources = listOf(
        com.thanhng224.app.R.drawable.img_0707,
        com.thanhng224.app.R.drawable.img_0847,
        com.thanhng224.app.R.drawable.img_1025,
        com.thanhng224.app.R.drawable.img_1045,
        com.thanhng224.app.R.drawable.img_1048,
        com.thanhng224.app.R.drawable.img_1508,
        com.thanhng224.app.R.drawable.img_1812,
        com.thanhng224.app.R.drawable.img_1978,
        com.thanhng224.app.R.drawable.img_1981,
        com.thanhng224.app.R.drawable.img_2136,
        com.thanhng224.app.R.drawable.img_2137,
        com.thanhng224.app.R.drawable.img_2149,
        com.thanhng224.app.R.drawable.kitkatq1,
        com.thanhng224.app.R.drawable.kitkatq2
    )

    // Argumentos
    private val argPlayers = savedStateHandle.get<Int>("players") ?: 1
    private val argSubmode = savedStateHandle.get<Int>("submode") ?: 0 // 0=Zen, 1=Vidas, 2=Tiempo
    private val argDiff = savedStateHandle.get<Int>("difficulty") ?: 0 // 0=Easy, 1=Med, 2=Hard

    init {
        initGame()
    }

    fun initGame() {
        timerJob?.cancel()
        flippedCardId = null
        isProcessing = false
        
        // 1. Configurar Cartas: SIEMPRE 24 cartas (12 pares) para 6x4
        val nbPairs = 12
        
        // Seleccionar 12 fotos al azar
        val selectedPhotos = photoResources.shuffled().take(nbPairs)
        
        // Duplicar y barajar
        val gameCards = (selectedPhotos + selectedPhotos).shuffled().mapIndexed { index, resId ->
            MemoryCard(index, resId)
        }

        // 2. Configurar Reglas según Dificultad
        var initialLives = 0
        var initialTime = 0L

        if (argSubmode == 1) { // Vidas
            initialLives = when(argDiff) {
                0 -> 20 // Fácil: aumentado de 15
                1 -> 15 // Medio: aumentado de 10
                else -> 10 // Difícil: aumentado de 5
            }
        } else if (argSubmode == 2) { // Tiempo
            initialTime = when(argDiff) {
                0 -> 120L // Fácil: aumentado de 90s (2 min)
                1 -> 90L  // Medio: aumentado de 60s (1.5 min)
                else -> 60L // Difícil: aumentado de 40s (1 min)
            }
            startTimer(initialTime)
        }

        _state.update { 
            MemoryUiState(
                cards = gameCards,
                isMultiplayer = argPlayers == 2,
                remainingAttempts = initialLives,
                remainingTime = initialTime,
                isGameOver = false,
                isWin = false,
                scoreP1 = 0,
                scoreP2 = 0,
                currentPlayer = 1
            )
        }
    }

    private fun startTimer(seconds: Long) {
        timerJob = viewModelScope.launch {
            var timeLeft = seconds
            while (timeLeft > 0 && !_state.value.isGameOver) {
                delay(1000)
                timeLeft--
                _state.update { it.copy(remainingTime = timeLeft) }
            }
            if (timeLeft == 0L && !_state.value.isWin) {
                endGame(win = false, msg = "¡Se acabó el tiempo! ⏳")
            }
        }
    }

    fun onCardClicked(cardId: Int) {
        if (_state.value.isGameOver || isProcessing) return
        
        val currentCards = _state.value.cards
        val card = currentCards.find { it.id == cardId } ?: return

        if (card.isFaceUp || card.isMatched) return

        // 1. Voltear la carta seleccionada
        _state.update { state -> 
            state.copy(cards = state.cards.map { if (it.id == cardId) it.copy(isFaceUp = true) else it })
        }

        if (flippedCardId == null) {
            // Primera carta del par
            flippedCardId = cardId
        } else {
            // Segunda carta del par
            val firstId = flippedCardId!!
            val firstCard = currentCards.find { it.id == firstId }!!

            if (firstCard.imageRes == card.imageRes) {
                // -> ACIERTO
                _state.update { state ->
                    val newCards = state.cards.map { 
                        if (it.id == firstId || it.id == cardId) it.copy(isMatched = true) else it 
                    }
                    
                    var nextState = state.copy(cards = newCards)
                    
                    // Sumar puntos
                    if (nextState.isMultiplayer) {
                        if (nextState.currentPlayer == 1) {
                            nextState = nextState.copy(scoreP1 = nextState.scoreP1 + 1)
                        } else {
                            nextState = nextState.copy(scoreP2 = nextState.scoreP2 + 1)
                        }
                    }
                    nextState
                }
                flippedCardId = null
                checkWin()
            } else {
                // -> FALLO
                isProcessing = true // Bloquear input
                viewModelScope.launch {
                    delay(800) // Esperar para que el usuario vea el error
                    _state.update { state ->
                        // Voltear cartas boca abajo
                        val newCards = state.cards.map { 
                            if (it.id == firstId || it.id == cardId) it.copy(isFaceUp = false) else it 
                        }
                        
                        var nextState = state.copy(cards = newCards)
                        
                        // Penalización / Cambio de turno
                        if (nextState.isMultiplayer) {
                            nextState = nextState.copy(currentPlayer = if (nextState.currentPlayer == 1) 2 else 1)
                        } else {
                            // Restar vida
                            if (argSubmode == 1) {
                                val newLives = nextState.remainingAttempts - 1
                                nextState = nextState.copy(remainingAttempts = newLives)
                                if (newLives <= 0) {
                                    timerJob?.cancel()
                                    nextState = nextState.copy(isGameOver = true, isWin = false, gameResultText = "¡Sin vidas! 💔")
                                }
                            }
                        }
                        nextState
                    }
                    flippedCardId = null
                    isProcessing = false // Desbloquear input
                }
            }
        }
    }

    private fun checkWin() {
        if (_state.value.cards.all { it.isMatched }) {
            val msg = if (_state.value.isMultiplayer) {
                val p1 = _state.value.scoreP1
                val p2 = _state.value.scoreP2
                if (p1 > p2) "¡Gana Jugador 1! 🏆" else if (p2 > p1) "¡Gana Jugador 2! 🏆" else "¡Empate! 🤝"
            } else {
                if (argSubmode == 2) "¡A tiempo! ⚡" else "¡Completado! 🎉"
            }
            endGame(true, msg)
        }
    }

    private fun endGame(win: Boolean, msg: String) {
        timerJob?.cancel()
        _state.update { it.copy(isGameOver = true, isWin = win, gameResultText = msg) }
    }
    
    val displayValue: String
        get() = when(argSubmode) {
            1 -> "Vidas: ${_state.value.remainingAttempts}"
            2 -> {
                val m = _state.value.remainingTime / 60
                val s = _state.value.remainingTime % 60
                "%02d:%02d".format(m, s)
            }
            else -> "Zen"
        }
}