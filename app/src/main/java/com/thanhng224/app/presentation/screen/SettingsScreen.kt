@file:Suppress("DEPRECATION")

package com.thanhng224.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thanhng224.app.presentation.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    // CORRECCIÓN: Usar hiltViewModel() en lugar de viewModel() para inyección de Hilt correcta
    val appViewModel: AppViewModel = hiltViewModel()
    
    // ESTADOS (Mockup temporal hasta conectar persistencia real)
    // En el futuro, estos vendrán de un ViewModel/DataStore
    val memoryAchievements = listOf(
        Achievement("Principiante", "Completa nivel Fácil", false),
        Achievement("Avanzado", "Completa nivel Medio", false),
        Achievement("Maestro", "Completa nivel Difícil", false),
        Achievement("Velocista", "Gana en menos de 2 min", false)
    )

    val miniGolfAchievements = listOf(
        Achievement("Hoyo en Uno", "Consigue un Hoyo en Uno", false),
        Achievement("Golfista Pro", "Completa los 3 niveles", false),
        Achievement("Estratega", "Completa el nivel 2", false)
    )

    val soccerAchievements = listOf(
        Achievement("Goleador", "Anota 3 goles", false),
        Achievement("Hat-Trick", "Anota 3 goles en una ronda", false),
        Achievement("Imparable", "Anota 5 goles en total", false)
    )

    val timerAchievements = listOf(
        Achievement("Cronometrista", "Diferencia < 0.50s", false),
        Achievement("Precisión", "Diferencia < 0.20s", false),
        Achievement("Ganador", "Gana una partida", false)
    )

    val pongAchievements = listOf(
        Achievement("Jugador", "Juega una partida", false),
        Achievement("Peloteo", "Rally > 5 golpes", false),
        Achievement("Invencible", "Gana una partida", false)
    )

    val ninjaAchievements = listOf(
        Achievement("Aprendiz", "Acierta 3 veces seguidas", false),
        Achievement("Ojo de Halcón", "Acierta 5 veces seguidas", false),
        Achievement("Ninja Veloz", "Gana en < 60 segundos", false)
    )
    
    val isDarkMode by appViewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración y Logros", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- SECCIÓN 1: CONFIGURACIÓN GENERAL ---
            item {
                SectionHeader("Ajustes Generales", Icons.Default.Settings)
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Modo Oscuro", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { appViewModel.toggleTheme() }
                            )
                        }
                    }
                }
            }

            // --- SECCIÓN 1.5: REPRODUCTOR DE MÚSICA ---
            item {
                SectionHeader("Reproductor de Música", Icons.Default.MusicNote)
                Spacer(modifier = Modifier.height(8.dp))
                
                MusicPlayerCard(appViewModel)
            }

            // --- SECCIÓN 2: LOGROS (Dividido por Juegos) ---
            item {
                SectionHeader("Mis Logros", Icons.Default.EmojiEvents)
            }
            
            // Logros de Memory
            item {
                GameAchievementsCard(
                    gameName = "Memory",
                    color = Color(0xFFE91E63), // Pink
                    achievements = memoryAchievements
                )
            }
            


            // Logros de Mini Golf
            item {
                GameAchievementsCard(
                    gameName = "Mini Golf",
                    color = Color(0xFF8BC34A), // Light Green
                    achievements = miniGolfAchievements
                )
            }

            // Logros de Soccer
            item {
                GameAchievementsCard(
                    gameName = "Soccer",
                    color = Color(0xFF2E7D32), // Dark Green
                    achievements = soccerAchievements
                )
            }

            // Logros de Crocodile


            // Logros de Timer
            item {
                GameAchievementsCard(
                    gameName = "Timer",
                    color = Color(0xFFFF9800), // Orange
                    achievements = timerAchievements
                )
            }

            // Logros de Love Pong
            item {
                GameAchievementsCard(
                    gameName = "Love Pong",
                    color = Color(0xFFE91E63), // Pink
                    achievements = pongAchievements
                )
            }

            // Logros de Ninja
            item {
                GameAchievementsCard(
                    gameName = "Ninja Throw",
                    color = Color(0xFF263238), // Dark Blue Grey
                    achievements = ninjaAchievements
                )
            }
            
            // Espacio final para que no tape el BottomBar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// --- CLASES Y COMPONENTES AUXILIARES ---

data class Achievement(val title: String, val description: String, val isUnlocked: Boolean)

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun GameAchievementsCard(gameName: String, color: Color, achievements: List<Achievement>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Cabecera del Juego
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.15f))
                    .padding(16.dp)
            ) {
                Text(
                    text = gameName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            // Lista de Logros
            Column(modifier = Modifier.padding(16.dp)) {
                achievements.forEachIndexed { index, achievement ->
                    AchievementRow(achievement, color)
                    if (index < achievements.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementRow(achievement: Achievement, activeColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono de Estado
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (achievement.isUnlocked) activeColor else Color.Gray.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (achievement.isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (achievement.isUnlocked) Color.White else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Textos
        Column {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (achievement.isUnlocked) {
             Icon(
                 Icons.Default.Star, 
                 contentDescription = null, 
                 tint = Color(0xFFFFD700), // Gold
                 modifier = Modifier.size(16.dp)
             )
        }
    }
}

@Composable
fun MusicPlayerCard(viewModel: AppViewModel) {
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(1) } // Avoid divide by zero
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var volume by remember { mutableFloatStateOf(50f) } // 0..100
    var isDraggingSlider by remember { mutableStateOf(false) }

    // Polling for progress
    LaunchedEffect(isPlaying, isDraggingSlider) {
        if (isPlaying && !isDraggingSlider) {
            while (true) {
                currentPosition = viewModel.getMusicPosition()
                duration = viewModel.getMusicDuration().coerceAtLeast(1)
                sliderPosition = currentPosition.toFloat()
                delay(500)
            }
        } else if (!isPlaying && !isDraggingSlider) {
            // Update once when paused to ensure valid state
            currentPosition = viewModel.getMusicPosition()
            duration = viewModel.getMusicDuration().coerceAtLeast(1)
            sliderPosition = currentPosition.toFloat()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousTrack() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(onClick = { viewModel.toggleMusic() }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = { viewModel.nextTrack() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { 
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        viewModel.seekMusic(sliderPosition.toInt())
                        isDraggingSlider = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Volume
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = volume,
                    onValueChange = { 
                        volume = it
                        viewModel.setMusicVolume(it / 100f) // 0.0 to 1.0 needed for MediaPlayer
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 1000) / 60
    return String.format("%02d:%02d", minutes, seconds)
}