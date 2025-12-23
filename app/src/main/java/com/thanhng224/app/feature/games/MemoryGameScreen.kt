package com.thanhng224.app.feature.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Suppress("DEPRECATION")
@Composable
fun MemoryGameScreen(
    navController: NavController? = null,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    // Estados del ViewModel
    val cards by viewModel.cards.collectAsState()

    val isMultiplayer by viewModel.isMultiplayer.collectAsState()
    val currentPlayer by viewModel.currentPlayer.collectAsState()
    val scorePlayer1 by viewModel.scorePlayer1.collectAsState()
    val scorePlayer2 by viewModel.scorePlayer2.collectAsState()

    val isGameOver by viewModel.isGameOver.collectAsState()

    // Colores para jugadores
    val player1Color = Color(0xFF2196F3) // Azul
    val player2Color = Color(0xFFE91E63) // Rosa/Rojo

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.initGame() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reiniciar Juego",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // --- HEADER (Puntuaciones) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isMultiplayer) {
                    // Modo 2 Jugadores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Jugador 1
                        PlayerScoreItem(
                            label = "Jugador 1",
                            score = scorePlayer1,
                            isActive = currentPlayer == 1,
                            color = player1Color,
                            alignment = Alignment.Start
                        )

                        // VS o Separador visual
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        // Jugador 2
                        PlayerScoreItem(
                            label = "Jugador 2",
                            score = scorePlayer2,
                            isActive = currentPlayer == 2,
                            color = player2Color,
                            alignment = Alignment.End
                        )
                    }
                } else {
                    // Modo 1 Jugador
                    Text(
                        text = "Puntos: $scorePlayer1",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // --- TABLERO (Grid) ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Ocupa todo el espacio restante
            ) {
                items(cards) { card ->
                    MemoryCardItem(
                        card = card,
                        onClick = { viewModel.onCardClick(card.id) }
                    )
                }
            }
        }

        // --- DIÁLOGO FIN DE JUEGO ---
        if (isGameOver) {
            AlertDialog(
                onDismissRequest = { /* No cerrar al hacer click fuera */ },
                title = {
                    Text(
                        text = "¡Juego Terminado!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    val message = if (isMultiplayer) {
                        when {
                            scorePlayer1 > scorePlayer2 -> "¡Ganó el Jugador 1! 🏆"
                            scorePlayer2 > scorePlayer1 -> "¡Ganó el Jugador 2! 🏆"
                            else -> "¡Es un Empate! 🤝"
                        }
                    } else {
                        "¡Ganaste! 🎉\nPuntos finales: $scorePlayer1"
                    }
                    
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = { viewModel.initGame() }) {
                        Text("Reiniciar")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { navController?.popBackStack() }) {
                        Text("Menú")
                    }
                }
            )
        }
    }
}

@Composable
fun PlayerScoreItem(
    label: String,
    score: Int,
    isActive: Boolean,
    color: Color,
    alignment: Alignment.Horizontal
) {
    val scale = if (isActive) 1.2f else 1.0f
    val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal
    
    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.scale(scale)
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = fontWeight,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "$score",
            color = color,
            fontWeight = fontWeight,
            style = MaterialTheme.typography.headlineSmall
        )
        // Indicador de turno activo
        if (isActive) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun MemoryCardItem(
    card: MemoryCard,
    onClick: () -> Unit
) {
    // Animamos la rotación: 0 grados si está boca abajo, 180 si está boca arriba o emparejada
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f) // Cuadrado
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density // Efecto de perspectiva 3D
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
                // Si la rotación es <= 90 grados, mostramos el dorso (Morado)
                // Si es > 90, mostramos el frente (Color)
                // Usamos Color(card.colorValue) porque convertimos a Int
                if (rotation <= 90f) Color(0xFF6200EE) else Color(card.colorValue)
            )
            .clickable(enabled = !card.isFaceUp && !card.isMatched) {
                onClick()
            }
    )
}
