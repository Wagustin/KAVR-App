package com.thanhng224.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.thanhng224.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    // Estado para controlar qué juego se seleccionó. Si es null, no hay diálogo.
    var showGameModeDialog by remember { mutableStateOf<Screen?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Juegos") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GameCard(
                    title = "Snake Game 🐍",
                    description = "El clásico juego de la serpiente",
                    icon = Icons.Default.Gamepad,
                    color = Color(0xFF4CAF50), // Verde
                    // Al hacer clic, guardamos que queremos jugar al Snake y mostramos el diálogo
                    onClick = { showGameModeDialog = Screen.SnakeGame }
                )
            }
            
            item {
                GameCard(
                    title = "Memory de Nosotros",
                    description = "Encuentra los pares (versión colores)",
                    icon = Icons.Default.Favorite,
                    color = Color(0xFFE91E63), // Rosa
                    // Al hacer clic, guardamos que queremos jugar al Memory y mostramos el diálogo
                    onClick = { showGameModeDialog = Screen.MemoryGame }
                )
            }
        }
    }

    // Lógica del Diálogo
    if (showGameModeDialog != null) {
        GameModeSelectionDialog(
            onDismiss = { showGameModeDialog = null },
            onModeSelected = { mode ->
                // Determinamos la ruta basada en el juego seleccionado y el modo
                val route = when (showGameModeDialog) {
                    Screen.SnakeGame -> Screen.SnakeGame.createRoute(mode)
                    Screen.MemoryGame -> Screen.MemoryGame.createRoute(mode)
                    else -> null
                }
                
                route?.let {
                    navController.navigate(it)
                    showGameModeDialog = null
                }
            }
        )
    }
}

@Composable
fun GameModeSelectionDialog(
    onDismiss: () -> Unit,
    onModeSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "¿Cuántos jugadores?", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column {
                Text("Selecciona el modo de juego:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón 1 Jugador
                    Button(
                        onClick = { onModeSelected(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("1", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    // Botón 2 Jugadores
                    Button(
                        onClick = { onModeSelected(2) },
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Text("2", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
            }
        },
        confirmButton = {}, // No usamos botón de confirmación estándar
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@Composable
fun GameCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
