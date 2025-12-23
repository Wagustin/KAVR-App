package com.thanhng224.app.feature.games

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

@HiltViewModel
class MemoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var isProcessing = false

    init {
        // CORRECCIÓN: Leer "players", "submode", "difficulty"
        val players = savedStateHandle.get<Int>("players") ?: 1
        val submode = savedStateHandle.get<Int>("submode") ?: 0
        val difficulty = savedStateHandle.get<Int>("difficulty") ?: 0
        
        initGame(players, submode, difficulty)
    }

    fun initGame(
        players: Int = (_uiState.value.gameMode.let { if (it == MemoryGameMode.MULTIPLAYER) 2 else 1 }),
        submode: Int = (_uiState.value.gameMode.let { 
            when(it) {
                MemoryGameMode.ATTEMPTS -> 1
                MemoryGameMode.TIMER -> 2
                else -> 0
            }
        }),
        difficulty: Int = (_uiState.value.difficulty?.ordinal ?: 0)
    ) {
        timerJob?.cancel()

        val gameMode = when {
            players == 2 -> MemoryGameMode.MULTIPLAYER
            else -> when (submode) {
                1 -> MemoryGameMode.ATTEMPTS
                2 -> MemoryGameMode.TIMER
                else -> MemoryGameMode.ZEN
            }
        }
        
        val gameDifficulty = when (difficulty) {
            0 -> Difficulty.EASY
            2 -> Difficulty.HARD
            else -> Difficulty.MEDIUM
        }

        val baseColors = listOf(
            Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
            Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
            Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548),
            Color(0xFF9E9E9E), Color(0xFF607D8B) // 18 colors base
        )

        val numberOfPairs = when (gameDifficulty) {
            Difficulty.EASY -> 12
            Difficulty.HARD -> 18
            else -> 16
        }

        val gameCards = (baseColors.take(numberOfPairs) + baseColors.take(numberOfPairs))
            .shuffled()
            .mapIndexed { index, color ->
                MemoryCard(id = index, colorValue = color.toArgb())
            }

        var time = 0L
        var attempts = 0
        when (gameMode) {
            MemoryGameMode.TIMER -> {
                time = when (gameDifficulty) {
                    Difficulty.EASY -> 240_000L
                    Difficulty.HARD -> 60_000L
                    else -> 150_000L
                }
                startTimer(time)
            }
            MemoryGameMode.ATTEMPTS -> {
                attempts = when (gameDifficulty) {
                    Difficulty.EASY -> 10
                    Difficulty.HARD -> 4
                    else -> 7
                }
            }
            else -> { /* No-op */ }
        }

        _uiState.value = MemoryUiState(
            cards = gameCards,
            gameMode = gameMode,
            difficulty = if (gameMode == MemoryGameMode.ZEN || gameMode == MemoryGameMode.MULTIPLAYER) null else gameDifficulty,
            gameStatus = GameStatus.PLAYING,
            remainingTime = time,
            remainingAttempts = attempts,
            isMultiplayer = gameMode == MemoryGameMode.MULTIPLAYER,
        )
        isProcessing = false
    }

    private fun startTimer(initialTime: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var currentTime = initialTime
            while (currentTime > 0 && _uiState.value.gameStatus == GameStatus.PLAYING) {
                delay(1000)
                currentTime -= 1000
                _uiState.update { it.copy(remainingTime = currentTime) }
            }
            if (currentTime <= 0) {
                _uiState.update { it.copy(gameStatus = GameStatus.LOST) }
            }
        }
    }

    fun onCardClick(cardId: Int) {
        if (isProcessing || _uiState.value.gameStatus != GameStatus.PLAYING) return

        val currentState = _uiState.value
        val cardIndex = currentState.cards.indexOfFirst { it.id == cardId }
        val card = currentState.cards[cardIndex]

        if (card.isFaceUp || card.isMatched) return

        val updatedCards = currentState.cards.toMutableList()
        updatedCards[cardIndex] = card.copy(isFaceUp = true)
        _uiState.update { it.copy(cards = updatedCards) }

        val faceUpCards = updatedCards.filter { it.isFaceUp && !it.isMatched }

        if (faceUpCards.size == 2) {
            isProcessing = true
            val (card1, card2) = faceUpCards
            if (card1.colorValue == card2.colorValue) {
                handleMatch(card1, card2)
            } else {
                handleMismatch(card1, card2)
            }
        }
    }

    private fun handleMatch(card1: MemoryCard, card2: MemoryCard) {
        val updatedCards = _uiState.value.cards.map {
            if (it.id == card1.id || it.id == card2.id) it.copy(isMatched = true) else it
        }

        _uiState.update { state ->
            state.copy(
                cards = updatedCards,
                scorePlayer1 = if (state.currentPlayer == 1) state.scorePlayer1 + 1 else state.scorePlayer1,
                scorePlayer2 = if (state.currentPlayer == 2) state.scorePlayer2 + 1 else state.scorePlayer2
            )
        }

        if (updatedCards.all { it.isMatched }) {
            _uiState.update { it.copy(gameStatus = GameStatus.WON) }
            timerJob?.cancel()
        }
        isProcessing = false
    }

    private fun handleMismatch(card1: MemoryCard, card2: MemoryCard) {
        viewModelScope.launch {
            delay(1000)
            val revertedCards = _uiState.value.cards.map {
                if (it.id == card1.id || it.id == card2.id) it.copy(isFaceUp = false) else it
            }
            
            var currentAttempts = _uiState.value.remainingAttempts
            if (_uiState.value.gameMode == MemoryGameMode.ATTEMPTS) {
                currentAttempts--
            }

            _uiState.update { state ->
                state.copy(
                    cards = revertedCards,
                    remainingAttempts = currentAttempts,
                    currentPlayer = if (state.isMultiplayer) if (state.currentPlayer == 1) 2 else 1 else state.currentPlayer
                )
            }
            
            if (_uiState.value.gameMode == MemoryGameMode.ATTEMPTS && currentAttempts <= 0) {
                _uiState.update { it.copy(gameStatus = GameStatus.LOST) }
            }
            
            isProcessing = false
        }
    }
}
