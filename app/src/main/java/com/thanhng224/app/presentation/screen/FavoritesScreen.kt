package com.thanhng224.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.thanhng224.app.presentation.navigation.Screen

private enum class DialogStep {
    HIDDEN, PLAYERS, GAME_TYPE, DIFFICULTY, SNAKE_MODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    var currentFlow by remember { mutableStateOf(DialogStep.HIDDEN) }
    var selectedPlayerMode by remember { mutableIntStateOf(1) }
    var selectedGameType by remember { mutableIntStateOf(Screen.MemoryGame.SUBMODE_ZEN) }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Juegos", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GameCard(
                    title = "Snake Game 🐍", 
                    description = "El clásico juego de la serpiente", 
                    icon = Icons.Default.Gamepad, 
                    color = Color(0xFF4CAF50),
                    onClick = { currentFlow = DialogStep.SNAKE_MODE }
                )
            }
            item {
                GameCard(
                    title = "Memory de Nosotros", 
                    description = "Encuentra los pares", 
                    icon = Icons.Default.Favorite, 
                    color = Color(0xFFE91E63),
                    onClick = { currentFlow = DialogStep.PLAYERS }
                )
            }
        }
    }

    // --- MANEJO DE DIÁLOGOS (Flow) ---
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

        // Paso 1: Jugadores
        DialogStep.PLAYERS -> {
            SimplePlayerSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { players ->
                    selectedPlayerMode = players
                    
                    if (players == 2) {
                        // MODO MULTIJUGADOR: Acceso directo (sin dificultad ni tipo)
                        navController.navigate(
                            Screen.MemoryGame.createRoute(
                                players = 2,
                                submode = Screen.MemoryGame.SUBMODE_ZEN, // Default
                                difficulty = Screen.MemoryGame.DIFFICULTY_EASY // Default
                            )
                        )
                        currentFlow = DialogStep.HIDDEN
                    } else {
                        // MODO 1 JUGADOR: Sigue el flujo normal
                        currentFlow = DialogStep.GAME_TYPE
                    }
                }
            )
        }

        // Paso 2: Modo de Juego
        DialogStep.GAME_TYPE -> {
            GameTypeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.PLAYERS },
                onSelect = { type ->
                    selectedGameType = type
                    // Si es Zen (Casual), saltamos selección de dificultad (es único nivel)
                    if (type == Screen.MemoryGame.SUBMODE_ZEN) {
                        navController.navigate(Screen.MemoryGame.createRoute(selectedPlayerMode, selectedGameType, Screen.MemoryGame.DIFFICULTY_EASY))
                        currentFlow = DialogStep.HIDDEN
                    } else {
                        currentFlow = DialogStep.DIFFICULTY
                    }
                }
            )
        }

        // Paso 3: Dificultad (Solo si no es Casual y 1 Jugador)
        DialogStep.DIFFICULTY -> {
            DifficultySelectionDialog(
                onDismiss = { 
                    currentFlow = DialogStep.GAME_TYPE 
                },
                onSelect = { difficulty ->
                    navController.navigate(Screen.MemoryGame.createRoute(selectedPlayerMode, selectedGameType, difficulty))
                    currentFlow = DialogStep.HIDDEN
                }
            )
        }
        
        DialogStep.HIDDEN -> { /* Nada */ }
    }
}

// --- COMPONENTES UI REUTILIZABLES ---

@Composable
fun CustomDialogBase(onDismiss: () -> Unit, title: String, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { 
            Text(
                title, 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, 
                modifier = Modifier.fillMaxWidth()
            ) 
        },
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
    CustomDialogBase(onDismiss = onDismiss, title = "¿Cuántos juegan?") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigSelectionButton(Modifier.weight(1f), Icons.Default.Person, "1 Jugador", MaterialTheme.colorScheme.primaryContainer) { onSelect(1) }
            BigSelectionButton(Modifier.weight(1f), Icons.Default.Person, "1 VS 1", MaterialTheme.colorScheme.tertiaryContainer) { onSelect(2) }
        }
    }
}

@Composable
fun SnakeModeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Modo Snake") {
        WideSelectionButton("Casual (Lento)", Icons.Default.SelfImprovement) { onSelect(0) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Tryhard (Acelera)", Icons.Default.Timer) { onSelect(1) }
    }
}

@Composable
fun GameTypeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Modo de Juego") {
        WideSelectionButton("Casual (Sin estrés) 🌸", Icons.Default.SelfImprovement) { onSelect(Screen.MemoryGame.SUBMODE_ZEN) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Vidas (Límite fallos) ❤️", Icons.Default.Favorite) { onSelect(Screen.MemoryGame.SUBMODE_ATTEMPTS) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Contra Reloj ⏱️", Icons.Default.Timer) { onSelect(Screen.MemoryGame.SUBMODE_TIMER) }
    }
}

@Composable
fun DifficultySelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Dificultad") {
        WideSelectionButton("Fácil (Más tiempo/Vidas) 🟢", null) { onSelect(Screen.MemoryGame.DIFFICULTY_EASY) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Medio (Estándar) 🟡", null) { onSelect(Screen.MemoryGame.DIFFICULTY_MEDIUM) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Difícil (Menos tiempo/Vidas) 🔴", null) { onSelect(Screen.MemoryGame.DIFFICULTY_HARD) }
    }
}

@Composable
fun BigSelectionButton(modifier: Modifier = Modifier, icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Black.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun WideSelectionButton(text: String, icon: ImageVector?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun GameCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}