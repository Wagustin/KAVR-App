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

data class MemoryCard(val id: Int, val color: Color, var isFaceUp: Boolean = false, var isMatched: Boolean = false)

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

    // Paleta de 16 colores base
    private val colors = listOf(
        Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta,
        Color(0xFFFF9800), Color(0xFF795548), Color(0xFF9C27B0), Color(0xFF607D8B),
        Color(0xFF8BC34A), Color(0xFF3F51B5), Color(0xFFE91E63), Color(0xFF009688),
        Color(0xFFFFEB3B), Color(0xFF000000)
    )

    // Lectura segura de argumentos
    private val argPlayers = savedStateHandle.get<Int>("players") ?: 1
    private val argSubmode = savedStateHandle.get<Int>("submode") ?: 0 // 0=Zen, 1=Vidas, 2=Tiempo
    private val argDiff = savedStateHandle.get<Int>("difficulty") ?: 0 // 0=Easy, 1=Med, 2=Hard

    init {
        initGame()
    }

    fun initGame() {
        timerJob?.cancel()
        
        // 1. Generar 24 cartas (12 pares) para grid 4x6
        val selectedColors = colors.shuffled().take(12) 
        val gameCards = (selectedColors + selectedColors).shuffled().mapIndexed { index, color ->
            MemoryCard(index, color)
        }

        // 2. Configurar Reglas según Dificultad
        var initialLives = 0
        var initialTime = 0L

        if (argSubmode == 1) { // Vidas
            initialLives = when(argDiff) {
                0 -> 15 // Fácil
                1 -> 10 // Medio
                else -> 5 // Difícil
            }
        } else if (argSubmode == 2) { // Tiempo
            initialTime = when(argDiff) {
                0 -> 180L // 3 min
                1 -> 120L // 2 min
                else -> 60L // 1 min
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
                isWin = false
            )
        }
        flippedCardId = null
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
        if (_state.value.isGameOver) return
        
        val currentList = _state.value.cards.toMutableList()
        val card = currentList.find { it.id == cardId } ?: return

        if (card.isFaceUp || card.isMatched) return

        // Voltear
        card.isFaceUp = true
        _state.update { it.copy(cards = currentList) }

        if (flippedCardId == null) {
            flippedCardId = cardId
        } else {
            val firstId = flippedCardId!!
            val firstCard = currentList.find { it.id == firstId }!!

            if (firstCard.color == card.color) {
                // Acierto
                firstCard.isMatched = true
                card.isMatched = true
                handleMatch()
                flippedCardId = null
                checkWin()
            } else {
                // Fallo
                viewModelScope.launch {
                    delay(800)
                    firstCard.isFaceUp = false
                    card.isFaceUp = false
                    handleMistake()
                    _state.update { it.copy(cards = currentList) }
                    flippedCardId = null
                }
            }
        }
    }

    private fun handleMatch() {
        if (_state.value.isMultiplayer) {
            _state.update { 
                if (it.currentPlayer == 1) it.copy(scoreP1 = it.scoreP1 + 1)
                else it.copy(scoreP2 = it.scoreP2 + 1)
            }
        }
    }

    private fun handleMistake() {
        _state.update { 
            if (it.isMultiplayer) {
                it.copy(currentPlayer = if (it.currentPlayer == 1) 2 else 1)
            } else {
                // Restar vida si aplica
                if (argSubmode == 1) {
                    val newLives = it.remainingAttempts - 1
                    if (newLives <= 0) endGame(false, "¡Sin vidas! 💔")
                    it.copy(remainingAttempts = newLives)
                } else it
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
                "¡Completado! 🎉"
            }
            endGame(true, msg)
        }
    }

    private fun endGame(win: Boolean, msg: String) {
        timerJob?.cancel()
        _state.update { it.copy(isGameOver = true, isWin = win, gameResultText = msg) }
    }
    
    // Helper para mostrar texto en UI
    val displayValue: String
        get() = when(argSubmode) {
            1 -> "Vidas: ${_state.value.remainingAttempts}"
            2 -> {
                val m = _state.value.remainingTime / 60
                val s = _state.value.remainingTime % 60
                "%02d:%02d".format(m, s)
            }
            else -> "Zen Mode 🧘"
        }
}