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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryGameScreen(
    navController: NavController,
    viewModel: MemoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Juego de Memoria") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            if (state.isMultiplayer) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("J1: ${state.scoreP1}", color = if(state.currentPlayer==1) Color.Blue else Color.Gray, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("VS")
                    Text("J2: ${state.scoreP2}", color = if(state.currentPlayer==2) Color.Red else Color.Gray, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                    if (state.remainingAttempts > 0) {
                        Text("Vidas: ${state.remainingAttempts} ❤️", style = MaterialTheme.typography.titleLarge)
                    } else {
                        Text("Modo Zen 🧘", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            // --- TABLERO (Ocupa todo el espacio restante) ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(4), // 4 Columnas
                modifier = Modifier
                    .weight(1f) // CLAVE: Esto hace que llene la pantalla
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.cards) { card ->
                    MemoryCardItem(card = card, onClick = { viewModel.onCardClicked(card.id) })
                }
            }
        }
    }

    // --- POPUP GAME OVER ---
    if (state.isGameOver) {
        AlertDialog(
            onDismissRequest = { /* No cerrar clickeando fuera */ },
            title = { Text("Juego Terminado", textAlign = TextAlign.Center) },
            text = { 
                Text(
                    text = state.winnerMessage, 
                    style = MaterialTheme.typography.headlineSmall, 
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            confirmButton = {
                Button(onClick = { viewModel.initGame() }) { Text("Reiniciar") }
            },
            dismissButton = {
                TextButton(onClick = { navController.popBackStack() }) { Text("Salir") }
            }
        )
    }
}

@Composable
fun MemoryCardItem(card: MemoryCard, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp) 180f else 0f,
        animationSpec = tween(500), label = "rotation"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Cuadrado perfecto
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 8 * density
            }
            .background(
                color = if (rotation <= 90f) Color(0xFF673AB7) else card.color, // Morado si está cerrada
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = !card.isFaceUp && !card.isMatched) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Aquí podrías poner una imagen de interrogación si rotation <= 90
    }
}
