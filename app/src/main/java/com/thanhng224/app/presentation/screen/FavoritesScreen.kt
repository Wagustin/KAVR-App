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
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
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
    HIDDEN, PLAYERS, GAME_TYPE, GAME_TYPE_MEMORY, DIFFICULTY, NINJA_MODE, PONG_MODE, MINIGOLF_MODE, MINIGOLF_SUBMODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    var currentFlow by remember { mutableStateOf(DialogStep.HIDDEN) }
    var selectedPlayerMode by remember { mutableIntStateOf(1) }
    var selectedGameType by remember { mutableIntStateOf(Screen.MemoryGame.SUBMODE_ZEN) }
    var selectedGame by remember { mutableStateOf<String?>(null) } // Tracks which game is being launched
    var selectedPongMode by remember { mutableIntStateOf(0) }
    var selectedNinjaMode by remember { mutableIntStateOf(0) }
    var selectedMiniGolfMode by remember { mutableIntStateOf(0) } // 0=1P, 1=2P
    var selectedMiniGolfSubMode by remember { mutableIntStateOf(0) } // 0=Training, 1=Time, 2=AI

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
                    onClick = { 
                        selectedGame = "SNAKE"
                        currentFlow = DialogStep.DIFFICULTY 
                    }
                )
            item {
                GameCard(
                    title = "Memory de Nosotros", 
                    description = "Encuentra los pares", 
                    icon = Icons.Default.Favorite, 
                    color = Color(0xFFE91E63),
                    onClick = { currentFlow = DialogStep.PLAYERS }
                )
            }

            item {
                GameCard(
                    title = "Reto Cronómetro ⏱️", 
                    description = "Pon a prueba tu precisión", 
                    icon = Icons.Default.Timer, 
                    color = Color(0xFFFF9800),
                    onClick = { 
                        selectedGame = "TIMER"
                        currentFlow = DialogStep.GAME_TYPE // Ask players first
                    }
                )
            }
            item {
                GameCard(
                    title = "Love Pong ❤️", 
                    description = "Agus vs Kat", 
                    icon = Icons.Default.Favorite, 
                    color = Color(0xFFE91E63),
                    onClick = { 
                        selectedGame = "PONG"
                        currentFlow = DialogStep.PONG_MODE
                    }
                )
            }
            item {
                GameCard(
                    title = "Ninja Throw ⚔️", 
                    description = "Afina tu puntería", 
                    icon = Icons.Default.Star, 
                    color = Color(0xFF263238),
                    onClick = { 
                        selectedGame = "NINJA"
                        currentFlow = DialogStep.NINJA_MODE
                    }
                )
            }
            item {
                GameCard(
                    title = "Fútbol Penalties ⚽", 
                    description = "Desliza para anotar", 
                    icon = Icons.Default.Gamepad, 
                    color = Color(0xFF2196F3),
                    onClick = { 
                        selectedGame = "SOCCER"
                        currentFlow = DialogStep.DIFFICULTY 
                    }
                )
            }
            item {
                GameCard(
                    title = "Mini Golf ⛳", 
                    description = "Hoyo en uno", 
                    icon = Icons.Default.Gamepad, 
                    color = Color(0xFF8BC34A),
                    onClick = { 
                        selectedGame = "MINIGOLF"
                        currentFlow = DialogStep.MINIGOLF_MODE
                    }
                )
            }
        }
    }



    // --- MANEJO DE DIÁLOGOS (Flow) ---
    when (currentFlow) {
        DialogStep.NINJA_MODE -> {
            NinjaModeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { mode ->
                    selectedNinjaMode = mode
                    selectedGame = "NINJA"
                    currentFlow = DialogStep.DIFFICULTY
                }
            )
        }
        
        DialogStep.PONG_MODE -> {
            PongModeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { mode ->
                    selectedPongMode = mode
                    if (mode == 1) {
                         // AI Mode -> Ask Difficulty
                         selectedGame = "PONG"
                         currentFlow = DialogStep.DIFFICULTY
                    } else {
                         // 2 Player -> Direct (Default Diff 1)
                         navController.navigate(Screen.PongGame.createRoute(mode, 1)) 
                         currentFlow = DialogStep.HIDDEN
                    }
                }
            )
        }
        
        DialogStep.MINIGOLF_MODE -> {
            MiniGolfModeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { mode ->
                    selectedMiniGolfMode = mode
                    if (mode == 0) {
                        // 1 Player -> Ask Submode
                        currentFlow = DialogStep.MINIGOLF_SUBMODE
                    } else {
                        // 2 Player -> Difficulty
                        selectedMiniGolfSubMode = 0 // Irrelevant for 2P but set default
                        currentFlow = DialogStep.DIFFICULTY
                    }
                }
            )
        }

        DialogStep.MINIGOLF_SUBMODE -> {
            MiniGolfSubModeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.MINIGOLF_MODE },
                onSelect = { submode ->
                    selectedMiniGolfSubMode = submode
                    currentFlow = DialogStep.DIFFICULTY
                }
            )
        }

        // --- Nuevos Juegos ---
        
        // Timer Game: Players -> Difficulty
        DialogStep.GAME_TYPE -> { // Step 1 for Timer: Players (Reuse existing name or logic)
             SimplePlayerSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { players ->
                    selectedPlayerMode = players
                    currentFlow = DialogStep.DIFFICULTY // Go to Difficulty next
                }
            )
        }

        // Paso 1: Jugadores (Memory)
        DialogStep.PLAYERS -> {
            SimplePlayerSelectionDialog(
                onDismiss = { currentFlow = DialogStep.HIDDEN },
                onSelect = { players ->
                    selectedPlayerMode = players
                    
                    if (players == 2) {
                        navController.navigate(
                            Screen.MemoryGame.createRoute(2, Screen.MemoryGame.SUBMODE_ZEN, Screen.MemoryGame.DIFFICULTY_EASY)
                        )
                        currentFlow = DialogStep.HIDDEN
                    } else {
                        currentFlow = DialogStep.GAME_TYPE_MEMORY
                    }
                }
            )
        }

        // Paso 2: Modo de Juego (Memory)
        DialogStep.GAME_TYPE_MEMORY -> {
            GameTypeSelectionDialog(
                onDismiss = { currentFlow = DialogStep.PLAYERS },
                onSelect = { type ->
                    selectedGameType = type
                    if (type == Screen.MemoryGame.SUBMODE_ZEN) {
                        navController.navigate(Screen.MemoryGame.createRoute(selectedPlayerMode, selectedGameType, Screen.MemoryGame.DIFFICULTY_EASY))
                        currentFlow = DialogStep.HIDDEN
                    } else {
                         selectedGame = "MEMORY" // Mark as Memory
                        currentFlow = DialogStep.DIFFICULTY
                    }
                }
            )
        }

        // Unified Difficulty Step for All Games
        DialogStep.DIFFICULTY -> {
            DifficultySelectionDialog(
                gameType = selectedGame,
                onDismiss = { currentFlow = DialogStep.HIDDEN }, // Or back to prev step if tracked
                onSelect = { difficulty ->
                    when (selectedGame) {
                        "MEMORY" -> navController.navigate(Screen.MemoryGame.createRoute(selectedPlayerMode, selectedGameType, difficulty))

                        "TIMER" -> navController.navigate(Screen.TimerGame.createRoute(selectedPlayerMode, difficulty))
                        "SOCCER" -> navController.navigate(Screen.SoccerGame.createRoute(difficulty))
                        "MINIGOLF" -> navController.navigate(Screen.MiniGolfGame.createRoute(selectedMiniGolfMode, selectedMiniGolfSubMode, difficulty))

                        "SNAKE" -> navController.navigate(Screen.SnakeGame.createRoute(difficulty))
                        "NINJA" -> navController.navigate(Screen.NinjaGame.createRoute(selectedNinjaMode, difficulty)) 
                        // Ah wait, Ninja flow uses `selectedPongMode` var? No, I need to check how Ninja sets its mode. 
                        // Looking at lines 150-160 (not shown), Ninja probably sets `selectedPongMode` (reused) or `selectedPlayerMode`.
                        // Re-checking Ninja Dialog call...
                    }
                    currentFlow = DialogStep.HIDDEN
                    selectedGame = null
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
fun PongModeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Modo Love Pong") {
        WideSelectionButton("Pareja (2 Jugadores) ❤️", Icons.Default.Favorite) { onSelect(0) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Solo vs Kat (AI) 🤖", Icons.Default.Person) { onSelect(1) }
    }
}

@Composable
fun DifficultySelectionDialog(gameType: String?, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val (easyText, medText, hardText) = when (gameType) {
        "PONG" -> Triple("IA Lenta (Fácil)", "IA Normal", "IA Imposible")
        "SNAKE" -> Triple("Velocidad Lenta", "Velocidad Normal", "Velocidad Rápida")
        "SOCCER" -> Triple("Portero Novato", "Portero Pro", "Campeón Mundial")
        "MINIGOLF" -> Triple("Hoyo Grande (Fácil)", "Hoyo Normal", "Hoyo Pequeño (Difícil)")
        "NINJA" -> Triple("Objetivo Lento", "Objetivo Rápido", "Modo Ninja")
        "TIMER" -> Triple("Margen 1s (Fácil)", "Margen 0.5s", "Margen 0.1s (Imposible)")
        "MEMORY", null -> Triple("Fácil (Más tiempo/Vidas)", "Medio (Estándar)", "Difícil (Menos tiempo/Vidas)")
        else -> Triple("Fácil", "Medio", "Difícil")
    }

    CustomDialogBase(onDismiss = onDismiss, title = "Dificultad") {
        WideSelectionButton("$easyText 🟢", null) { onSelect(Screen.MemoryGame.DIFFICULTY_EASY) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("$medText 🟡", null) { onSelect(Screen.MemoryGame.DIFFICULTY_MEDIUM) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("$hardText 🔴", null) { onSelect(Screen.MemoryGame.DIFFICULTY_HARD) }
    }
}

@Composable
fun MiniGolfModeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Mini Golf Players") {
        WideSelectionButton("1 Jugador 🏌️", Icons.Default.Person) { onSelect(0) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("2 Jugadores (Versus) ⚔️", Icons.Default.Gamepad) { onSelect(1) }
    }
}

@Composable
fun MiniGolfSubModeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Modo de Juego") {
        WideSelectionButton("Entrenamiento (Golpes) ⛳", Icons.Default.SelfImprovement) { onSelect(0) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Contrarreloj ⏱️", Icons.Default.Timer) { onSelect(1) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Vs IA 🤖", Icons.Default.SmartToy) { onSelect(2) }
    }
}

@Composable
fun NinjaModeSelectionDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    CustomDialogBase(onDismiss = onDismiss, title = "Modo Ninja") {
        WideSelectionButton("Duelo (2 Jugadores) ⚔️", Icons.Default.Person) { onSelect(0) }
        Spacer(modifier = Modifier.height(8.dp))
        WideSelectionButton("Supervivencia (Solo) 🎯", Icons.Default.Star) { onSelect(1) }
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