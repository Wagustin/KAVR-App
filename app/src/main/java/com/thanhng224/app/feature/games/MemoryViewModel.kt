package com.thanhng224.app.feature.games

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

/**
 * Data class to represent a single card in the Memory Game.
 * Usamos Int para el color para evitar problemas de runtime con inline classes de Compose.
 */
data class MemoryCard(
    val id: Int,
    val colorValue: Int, // Cambiado de Color a Int
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _cards = MutableStateFlow<List<MemoryCard>>(emptyList())
    val cards = _cards.asStateFlow()

    private val _currentPlayer = MutableStateFlow(1)
    val currentPlayer = _currentPlayer.asStateFlow()

    private val _scorePlayer1 = MutableStateFlow(0)
    val scorePlayer1 = _scorePlayer1.asStateFlow()

    private val _scorePlayer2 = MutableStateFlow(0)
    val scorePlayer2 = _scorePlayer2.asStateFlow()

    private val _isMultiplayer = MutableStateFlow(false)
    val isMultiplayer = _isMultiplayer.asStateFlow()
    
    private val _isGameOver = MutableStateFlow(false)
    val isGameOver = _isGameOver.asStateFlow()

    private var isProcessing = false

    init {
        // Obtener el modo (1 o 2 jugadores) de los argumentos de navegación
        val mode = savedStateHandle.get<Int>("mode") ?: 1
        _isMultiplayer.value = mode == 2
        initGame()
    }

    fun initGame() {
        // 6 colores distintos convertidos a ARGB Int
        val colors = listOf(
            Color.Red.toArgb(), 
            Color.Blue.toArgb(), 
            Color.Green.toArgb(),
            Color.Yellow.toArgb(), 
            Color.Magenta.toArgb(), 
            Color.Cyan.toArgb()
        )
        
        // Duplicar, barajar y mapear
        val gameCards = (colors + colors).shuffled().mapIndexed { index, colorVal ->
            MemoryCard(id = index, colorValue = colorVal)
        }
        
        _cards.value = gameCards
        _scorePlayer1.value = 0
        _scorePlayer2.value = 0
        _currentPlayer.value = 1
        _isGameOver.value = false
        isProcessing = false
    }

    fun onCardClick(cardId: Int) {
        if (isProcessing) return
        if (_isGameOver.value) return

        val currentCards = _cards.value.toMutableList()
        val cardIndex = currentCards.indexOfFirst { it.id == cardId }
        
        if (cardIndex == -1) return
        val card = currentCards[cardIndex]

        // Si la carta ya está volteada o emparejada, no hacer nada
        if (card.isFaceUp || card.isMatched) return

        // Voltear carta
        currentCards[cardIndex] = card.copy(isFaceUp = true)
        _cards.value = currentCards

        val faceUpUnmatched = currentCards.filter { it.isFaceUp && !it.isMatched }

        if (faceUpUnmatched.size == 2) {
            val (card1, card2) = faceUpUnmatched
            
            // Comprobar si coinciden
            if (card1.colorValue == card2.colorValue) {
                // Match!
                _cards.update { cards ->
                    cards.map { 
                        if (it.id == card1.id || it.id == card2.id) it.copy(isMatched = true) else it 
                    }
                }
                
                // Sumar punto al jugador actual
                if (_currentPlayer.value == 1) {
                    _scorePlayer1.update { it + 1 }
                } else {
                    _scorePlayer2.update { it + 1 }
                }
                
                // Comprobar si el juego ha terminado
                checkGameState()
                
            } else {
                // No Match (Mismatch)
                isProcessing = true
                viewModelScope.launch {
                    delay(1000)
                    // Voltear cartas de nuevo
                    _cards.update { cards ->
                        cards.map { 
                            if (it.id == card1.id || it.id == card2.id) it.copy(isFaceUp = false) else it 
                        }
                    }
                    
                    // Cambiar turno solo si es multijugador
                    if (_isMultiplayer.value) {
                        _currentPlayer.update { if (it == 1) 2 else 1 }
                    }
                    
                    isProcessing = false
                }
            }
        }
    }
    
    private fun checkGameState() {
        if (_cards.value.all { it.isMatched }) {
            _isGameOver.value = true
        }
    }
}
