package com.thanhng224.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.thanhng224.app.presentation.navigation.MemoryGameConfig
import com.thanhng224.app.presentation.navigation.Screen

// CORRECCIÓN: Enum para gestionar la máquina de estados de los diálogos
private enum class DialogStep {
    HIDDEN, PLAYERS, GAME_TYPE, DIFFICULTY, SNAKE_MODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    // --- ESTADOS ---
    var currentFlow by remember { mutableStateOf(DialogStep.HIDDEN) }
    
    // Estados para el flujo de configuración de Memory Game
    var selectedPlayerMode by remember { mutableIntStateOf(1) }
    var selectedGameType by remember { mutableIntStateOf(MemoryGameConfig.SUBMODE_ZEN) }

    // --- UI PRINCIPAL ---
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Juegos") },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GameCard("Snake Game 🐍", "El clásico juego de la serpiente", Icons.Default.Gamepad, Color(0xFF4CAF50)) {
                    currentFlow = DialogStep.SNAKE_MODE
                }
            }
            item {
                GameCard("Memory de Nosotros", "Encuentra los pares", Icons.Default.Favorite, Color(0xFFE91E63)) {
                    currentFlow = DialogStep.PLAYERS // Inicia el flujo del Memory Game
                }
            }
        }
    }

    // --- LÓGICA DE DIÁLOGOS ---
    
    when (currentFlow) {
        DialogStep.SNAKE_MODE -> {
            SnakeModeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { mode ->
                    navController.navigate(Screen.SnakeGame.createRoute(mode))
                    currentFlow = DialogStep.HIDDEN
                }
            )
        }

        DialogStep.PLAYERS -> {
            SimplePlayerSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { mode ->
                    selectedPlayerMode = mode
                    if (mode == 2) { // Multijugador va directo
                        navController.navigate(Screen.MemoryGame.createRoute(2, MemoryGameConfig.SUBMODE_ZEN, MemoryGameConfig.DIFFICULTY_MEDIUM))
                        currentFlow = DialogStep.HIDDEN
                    } else { // Un jugador avanza
                        currentFlow = DialogStep.GAME_TYPE
                    }
                }
            )
        }

        DialogStep.GAME_TYPE -> {
            GameTypeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.PLAYERS }, // Volver
                onSelect = { type ->
                    selectedGameType = type
                    if (type == MemoryGameConfig.SUBMODE_ZEN) { // Zen va directo
                        navController.navigate(Screen.MemoryGame.createRoute(1, MemoryGameConfig.SUBMODE_ZEN, MemoryGameConfig.DIFFICULTY_MEDIUM))
                        currentFlow = DialogStep.HIDDEN
                    } else { // Otros avanzan a dificultad
                        currentFlow = DialogStep.DIFFICULTY
                    }
                }
            )
        }

        DialogStep.DIFFICULTY -> {
            DifficultySelectionDialog(
                onDismiss = { currentFlow = DialogStep.GAME_TYPE }, // Volver
                onSelect = { difficulty ->
                    navController.navigate(Screen.MemoryGame.createRoute(selectedPlayerMode, selectedGameType, difficulty))
                    currentFlow = DialogStep.HIDDEN
                }
            )
        }
        
        DialogStep.HIDDEN -> { /* No mostrar diálogos */ }
    }
}

// --- COMPONENTES DE DIÁLOGO REUTILIZABLES ---

@Composable
fun CustomDialogBase(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content()
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regresar")
                }
            }
        },
        confirmButton = {},
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@Composable
fun SimplePlayerSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "¿Cuántos jugadores?") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigSelectionButton(Modifier.weight(1f), Icons.Default.Person, "1", MaterialTheme.colorScheme.primaryContainer) { onSelect(1) }
            BigSelectionButton(Modifier.weight(1f), Icons.Default.Person, "2", MaterialTheme.colorScheme.tertiaryContainer) { onSelect(2) }
        }
    }
}

@Composable
fun SnakeModeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Modo de Juego") {
        WideSelectionButton("Casual  leisurely", null) { onSelect(0) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Tryhard 🥵", null) { onSelect(1) }
    }
}

@Composable
fun GameTypeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Estilo de Juego") {
        WideSelectionButton("Zen (Sin estrés) 🌸", Icons.Default.SelfImprovement) { onSelect(MemoryGameConfig.SUBMODE_ZEN) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Por Vidas ❤️", Icons.Default.Favorite) { onSelect(MemoryGameConfig.SUBMODE_ATTEMPTS) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Contra Reloj ⏱️", Icons.Default.Timer) { onSelect(MemoryGameConfig.SUBMODE_TIMER) }
    }
}

@Composable
fun DifficultySelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Dificultad") {
        WideSelectionButton("Fácil (16 cartas) 🟢", null) { onSelect(MemoryGameConfig.DIFFICULTY_EASY) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Medio (20 cartas) 🟡", null) { onSelect(MemoryGameConfig.DIFFICULTY_MEDIUM) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Difícil (24 cartas) 🔴", null) { onSelect(MemoryGameConfig.DIFFICULTY_HARD) }
    }
}

@Composable
fun BigSelectionButton(modifier: Modifier = Modifier, icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.7f))
            Text(text, style = MaterialTheme.typography.titleLarge, color = Color.Black.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun WideSelectionButton(text: String, icon: ImageVector?, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun GameCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}
