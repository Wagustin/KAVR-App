package com.thanhng224.app.feature.games

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
class MemoryViewModel @Inject constructor() : ViewModel() {

    private val _cards = MutableStateFlow<List<MemoryCard>>(emptyList())
    val cards = _cards.asStateFlow()

    private var isProcessing = false

    init {
        initGame()
    }

    fun initGame() {
        // 6 distinct colors converted to ARGB Int
        val colors = listOf(
            Color.Red.toArgb(), 
            Color.Blue.toArgb(), 
            Color.Green.toArgb(),
            Color.Yellow.toArgb(), 
            Color.Magenta.toArgb(), 
            Color.Cyan.toArgb()
        )
        
        // Duplicate, shuffle, and map
        val gameCards = (colors + colors).shuffled().mapIndexed { index, colorVal ->
            MemoryCard(id = index, colorValue = colorVal)
        }
        
        _cards.value = gameCards
        isProcessing = false
    }

    fun onCardClick(cardId: Int) {
        if (isProcessing) return

        val currentCards = _cards.value.toMutableList()
        val cardIndex = currentCards.indexOfFirst { it.id == cardId }
        
        if (cardIndex == -1) return
        val card = currentCards[cardIndex]

        if (card.isFaceUp || card.isMatched) return

        // Flip card
        currentCards[cardIndex] = card.copy(isFaceUp = true)
        _cards.value = currentCards

        val faceUpUnmatched = currentCards.filter { it.isFaceUp && !it.isMatched }

        if (faceUpUnmatched.size == 2) {
            val (card1, card2) = faceUpUnmatched
            if (card1.colorValue == card2.colorValue) {
                // Match
                _cards.update { cards ->
                    cards.map { 
                        if (it.id == card1.id || it.id == card2.id) it.copy(isMatched = true) else it 
                    }
                }
            } else {
                // Mismatch
                isProcessing = true
                viewModelScope.launch {
                    delay(1000)
                    _cards.update { cards ->
                        cards.map { 
                            if (it.id == card1.id || it.id == card2.id) it.copy(isFaceUp = false) else it 
                        }
                    }
                    isProcessing = false
                }
            }
        }
    }
}
