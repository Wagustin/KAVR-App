package com.thanhng224.app.feature.games

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val remainingAttempts: Int = 0,
    val remainingTime: Long = 0,
    val isGameOver: Boolean = false,
    val winnerMessage: String = ""
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryUiState())
    val state = _state.asStateFlow()

    private var flippedCardId: Int? = null
    private val colors = listOf(
        Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta,
        Color(0xFFFF9800), Color(0xFF795548), Color(0xFF9C27B0), Color(0xFF607D8B),
        Color(0xFF8BC34A), Color(0xFF3F51B5), Color(0xFFE91E63), Color(0xFF009688),
        Color(0xFFFFEB3B), Color(0xFF000000)
    )

    // Argumentos guardados para reiniciar
    private val argPlayers: Int = savedStateHandle.get<Int>("players") ?: 1
    private val argSubmode: Int = savedStateHandle.get<Int>("submode") ?: 0
    private val argDiff: Int = savedStateHandle.get<Int>("difficulty") ?: 0

    init {
        initGame()
    }

    fun initGame() {
        // Generar 16 pares (32 cartas) para llenar una pantalla 4x8
        val selectedColors = (colors + colors).shuffled().take(16) // Asegura suficientes colores
        val gameColors = (selectedColors + selectedColors).shuffled()
        
        val cards = gameColors.mapIndexed { index, color ->
            MemoryCard(id = index, color = color)
        }

        // Configurar dificultad (intentos/tiempo) según argDiff...
        // (Por simplicidad en este prompt, ponemos valores base, puedes expandir esto luego)
        val initialAttempts = if(argSubmode == 1) (if(argDiff == 0) 15 else 10) else 0

        _state.update { 
            MemoryUiState(
                cards = cards, 
                isMultiplayer = argPlayers == 2,
                remainingAttempts = initialAttempts,
                isGameOver = false
            ) 
        }
        flippedCardId = null
    }

    fun onCardClicked(cardId: Int) {
        val currentCards = _state.value.cards.toMutableList()
        val card = currentCards.find { it.id == cardId } ?: return

        if (card.isFaceUp || card.isMatched || _state.value.isGameOver) return

        // Voltear carta
        card.isFaceUp = true
        _state.update { it.copy(cards = currentCards) }

        if (flippedCardId == null) {
            // Primera carta
            flippedCardId = cardId
        } else {
            // Segunda carta
            val firstCardId = flippedCardId!!
            val firstCard = currentCards.find { it.id == firstCardId }!!

            if (firstCard.color == card.color) {
                // MATCH!
                firstCard.isMatched = true
                card.isMatched = true
                handleMatch()
                flippedCardId = null
            } else {
                // NO MATCH
                viewModelScope.launch {
                    delay(1000)
                    firstCard.isFaceUp = false
                    card.isFaceUp = false
                    handleMismatch()
                    _state.update { it.copy(cards = currentCards) } // Refrescar UI
                    flippedCardId = null
                }
            }
        }
        checkWinCondition()
    }

    private fun handleMatch() {
        _state.update { state ->
            if (state.isMultiplayer) {
                if (state.currentPlayer == 1) state.copy(scoreP1 = state.scoreP1 + 1)
                else state.copy(scoreP2 = state.scoreP2 + 1)
            } else {
                state // Lógica single player score
            }
        }
    }

    private fun handleMismatch() {
        _state.update { state ->
            if (state.isMultiplayer) {
                state.copy(currentPlayer = if (state.currentPlayer == 1) 2 else 1)
            } else {
                if (argSubmode == 1 && state.remainingAttempts > 0) { // Modo Intentos
                     val newAttempts = state.remainingAttempts - 1
                     if (newAttempts == 0) state.copy(remainingAttempts = 0, isGameOver = true, winnerMessage = "¡Te quedaste sin intentos! 😢")
                     else state.copy(remainingAttempts = newAttempts)
                } else state
            }
        }
    }

    private fun checkWinCondition() {
        if (_state.value.cards.all { it.isMatched }) {
            val msg = if (_state.value.isMultiplayer) {
                if (_state.value.scoreP1 > _state.value.scoreP2) "¡Ganó Jugador 1! 🏆"
                else if (_state.value.scoreP2 > _state.value.scoreP1) "¡Ganó Jugador 2! 🏆"
                else "¡Empate! 🤝"
            } else {
                "¡Ganaste! 🎉"
            }
            _state.update { it.copy(isGameOver = true, winnerMessage = msg) }
        }
    }
}
