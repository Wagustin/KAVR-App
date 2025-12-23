package com.thanhng224.app.presentation.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.thanhng224.app.presentation.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION") // Suprimir la advertencia de hiltViewModel
@Composable
fun SettingsScreen(
    appViewModel: AppViewModel = hiltViewModel()
) {
    // --- ESTADOS (futura conexión a AppPreferences) ---
    // val achievements by appViewModel.achievements.collectAsState()
    val easyCompleted = true // Mockup
    val mediumCompleted = true // Mockup
    val hardCompleted = false // Mockup

    val allHardCompleted = easyCompleted && mediumCompleted && hardCompleted

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Logros y Ajustes") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Sección de Logros
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Logros", tint = Color(0xFFFFD700)) // Dorado
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Desafíos de Memory",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Lista de desafíos
                        AchievementItem("Completar en Fácil", easyCompleted)
                        AchievementItem("Completar en Medio", mediumCompleted)
                        AchievementItem("Completar en Difícil", hardCompleted)
                    }
                }
            }

            // Sección de Modo Secreto
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SecretModeCard(unlocked = allHardCompleted)
            }
            
            // Aquí se podrían añadir más ajustes en el futuro (logout, etc.)
        }
    }
}

@Composable
fun AchievementItem(text: String, completed: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = completed,
            onCheckedChange = null, // No se puede cambiar manualmente
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SecretModeCard(unlocked: Boolean) {
    val goldBrush = Brush.horizontalGradient(listOf(Color(0xFFD4AF37), Color(0xFFFFD700), Color(0xFFD4AF37)))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                width = if (unlocked) 2.dp else 1.dp,
                color = if (unlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = unlocked, onClick = { /* TODO: Navegar a modo ultra */ }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (unlocked) MaterialTheme.colorScheme.surface else Color.Transparent),
        elevation = CardDefaults.cardElevation(if (unlocked) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (unlocked) {
                Text(
                    text = "✨ MODO SECRETO DESBLOQUEADO ✨", 
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.background(goldBrush)
                )
            } else {
                Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Completa todos los desafíos en Difícil para desbloquear",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
