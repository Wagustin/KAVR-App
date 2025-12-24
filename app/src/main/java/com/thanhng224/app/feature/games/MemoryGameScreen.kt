package com.thanhng224.app.feature.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    navController: NavController
) {
    // Movemos el ViewModel dentro del cuerpo para evitar advertencias de parámetros por defecto
    val viewModel: MemoryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { 
            CenterAlignedTopAppBar(
                title = { Text("Memory Game", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { viewModel.initGame() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER MEJORADO - MARCADOR
            if (state.isMultiplayer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerScoreCard(
                        name = "Jugador 1",
                        score = state.scoreP1,
                        isActive = state.currentPlayer == 1,
                        activeColor = Color(0xFF2196F3) // Azul
                    )
                    
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    
                    PlayerScoreCard(
                        name = "Jugador 2",
                        score = state.scoreP2,
                        isActive = state.currentPlayer == 2,
                        activeColor = Color(0xFFE91E63) // Rosa
                    )
                }
            } else {
                // Modo Solo (Vidas o Tiempo)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viewModel.displayValue,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // TABLERO AJUSTADO
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val screenHeight = maxHeight
                
                // Calculamos el tamaño ideal de celda
                val spacing = 8.dp
                val rows = 6
                
                val availableHeight = screenHeight - (spacing * (rows - 1))
                val cellHeight = availableHeight / rows
                
                // Aumentamos el límite de altura máxima de 100dp a 125dp
                val maxCellHeight = 125.dp
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.wrapContentSize()
                ) {
                    items(state.cards) { card ->
                        // Altura dinámica
                        Box(modifier = Modifier.height(cellHeight.coerceAtMost(maxCellHeight))) {
                            MemoryCardItem(card = card, onClick = { viewModel.onCardClicked(card.id) })
                        }
                    }
                }
            }
        }
    }
    
    // --- DIALOGO FINAL ---
    if (state.isGameOver) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Text(if (state.isWin) "🏆" else "💀", style = MaterialTheme.typography.displayMedium) },
            title = { Text(if (state.isWin) "¡Juego Terminado!" else "Fin del Juego", textAlign = TextAlign.Center) },
            text = { 
                Text(
                    state.gameResultText, 
                    textAlign = TextAlign.Center, 
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.fillMaxWidth() 
                ) 
            },
            confirmButton = {
                Button(onClick = { viewModel.initGame() }) { Text("Jugar Otra Vez") }
            },
            dismissButton = {
                OutlinedButton(onClick = { navController.popBackStack() }) { Text("Salir") }
            }
        )
    }
}

@Composable
fun PlayerScoreCard(name: String, score: Int, isActive: Boolean, activeColor: Color) {
    val scale by animateFloatAsState(if (isActive) 1.1f else 1.0f, label = "scale")
    val elevation by animateFloatAsState(if (isActive) 8f else 2f, label = "elevation")
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = elevation.dp,
        tonalElevation = if (isActive) 4.dp else 1.dp,
        color = if (isActive) activeColor else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // Reducimos el ancho de 110.dp a 90.dp para hacerlos más pequeños
            .width(90.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp) // Reducimos padding de 12.dp a 8.dp
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp) // Reducimos tamaño de icono de 40.dp a 32.dp
                    .background(
                        color = if (isActive) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person, 
                    contentDescription = null, 
                    tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp) // Icono interno más pequeño
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall, // Fuente más pequeña
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge, // Fuente más pequeña (antes headlineMedium)
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MemoryCardItem(card: MemoryCard, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 400), 
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                enabled = !card.isFaceUp && !card.isMatched,
                onClick = onClick
            )
    ) {
        if (rotation <= 90f) {
            // DORSO
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF5E35B1),
                shadowElevation = 4.dp,
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "?", 
                        style = MaterialTheme.typography.headlineMedium, 
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // CARA
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                shape = RoundedCornerShape(8.dp),
                color = card.color,
                shadowElevation = 4.dp,
                border = BorderStroke(2.dp, Color.Black.copy(alpha = 0.1f))
            ) {
                if (card.isMatched) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
        }
    }
}