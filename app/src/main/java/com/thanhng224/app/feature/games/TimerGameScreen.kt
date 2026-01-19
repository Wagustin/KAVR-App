package com.thanhng224.app.feature.games

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerGameScreen(navController: NavController) {
    // Get args
    val navBackStackEntry = navController.currentBackStackEntry
    val players = navBackStackEntry?.arguments?.getInt("players") ?: 1
    val difficulty = navBackStackEntry?.arguments?.getInt("difficulty") ?: 0
    
    // Difficulty Settings: Time after which timer becomes "??.??"
    // Target is 5.00s
    // Easy: Hide at 4.0s (1s blind)
    // Medium: Hide at 3.0s (2s blind)
    // Hard: Hide at 2.0s (3s blind)
    val hideTime = when (difficulty) {
        0 -> 4.0f
        1 -> 3.0f
        else -> 2.0f
    }

    var currentPlayer by remember { mutableIntStateOf(1) }
    var p1Score by remember { mutableFloatStateOf(0f) } // Diff from 5.0
    var p2Score by remember { mutableFloatStateOf(0f) }
    
    var state by remember { mutableStateOf(GameState.INTRO) }
    var startTime by remember { mutableLongStateOf(0L) }
    var currentTimeDisplay by remember { mutableStateOf("0.00") }
    var timerRunning by remember { mutableStateOf(false) }

    // Constants
    val targetTime = 5.00f

    val scope = rememberCoroutineScope()

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            startTime = System.currentTimeMillis()
            while (timerRunning) {
                // Update on every frame for smooth "visual" (though we hide it later)
                withFrameMillis {
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                    currentTimeDisplay = if (elapsed > hideTime) "??.??" else String.format("%.2f", elapsed)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reto 5 Segundos ⏱️") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        // Reset Total
                        state = GameState.INTRO
                        currentPlayer = 1
                        p1Score = 0f
                        p2Score = 0f
                        currentTimeDisplay = "0.00"
                        timerRunning = false
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFFFF3E0)), // Light Orange
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Header Info
            if (players == 2) {
                Text(
                    text = if (state == GameState.PlayingP1 || state == GameState.INTRO) "Turno: Jugador 1" 
                           else if (state == GameState.ResultP1 || state == GameState.PlayingP2) "Turno: Jugador 2" 
                           else "Resultados",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Main Timer Display
            Text(
                text = currentTimeDisplay,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentTimeDisplay == "??.??") Color.Gray else Color.Black
            )
            
            Text(
                text = "Meta: 5.00s",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Interaction Button
            Button(
                onClick = {
                    when (state) {
                        GameState.INTRO -> {
                            state = if (players == 1) GameState.PlayingSolo else GameState.PlayingP1
                            timerRunning = true
                        }
                        GameState.PlayingSolo -> {
                            timerRunning = false
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                            p1Score = abs(targetTime - elapsed)
                            currentTimeDisplay = String.format("%.2f", elapsed)
                            state = GameState.ResultSolo
                        }
                        GameState.ResultSolo -> {
                            // Restart Solo
                            currentTimeDisplay = "0.00"
                            state = GameState.INTRO
                        }
                        
                        // 2 Player Logic
                        GameState.PlayingP1 -> {
                            timerRunning = false
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                            p1Score = abs(targetTime - elapsed)
                            currentTimeDisplay = String.format("%.2f", elapsed)
                            scope.launch {
                                delay(2000) // Show result briefly
                                state = GameState.WaitForP2
                                currentTimeDisplay = "0.00"
                            }
                            state = GameState.ResultP1 // Temporary state to block input
                        }
                        GameState.WaitForP2 -> {
                            state = GameState.PlayingP2
                            timerRunning = true
                        }
                        GameState.PlayingP2 -> {
                            timerRunning = false
                            val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                            p2Score = abs(targetTime - elapsed)
                            currentTimeDisplay = String.format("%.2f", elapsed)
                            state = GameState.FinalResult
                        }
                        GameState.FinalResult -> {
                             // Reset
                             state = GameState.INTRO
                             currentPlayer = 1
                             currentTimeDisplay = "0.00"
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerRunning) Color.Red else Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = if (timerRunning) "STOP" else if (state == GameState.FinalResult || state == GameState.ResultSolo) "RETRY" else "GO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Result Display
            if (state == GameState.ResultSolo) {
                ResultText(diff = p1Score)
            } else if (state == GameState.FinalResult) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("J1 Diff: ${String.format("%.2f", p1Score)}", fontWeight = FontWeight.Bold)
                    Text("J2 Diff: ${String.format("%.2f", p2Score)}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (p1Score < p2Score) "¡Gana Jugador 1! 🏆" else if (p2Score < p1Score) "¡Gana Jugador 2! 🏆" else "¡Empate!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ResultText(diff: Float) {
    val message = when {
        diff == 0.00f -> "¡PERFECTO! 💎"
        diff < 0.1f -> "¡Divino! 🤩"
        diff < 0.5f -> "Muy bien 👍"
        else -> "Intenta de nuevo 😅"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Diferencia: ${String.format("%.2f", diff)}s")
        Text(message, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

enum class GameState {
    INTRO, PlayingSolo, ResultSolo,
    PlayingP1, ResultP1, WaitForP2, PlayingP2, FinalResult
}
