package com.thanhng224.app.feature.auth.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thanhng224.app.presentation.ui.FlowerAnimation

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var showAnimation by remember { mutableStateOf(false) }

    // Color Púrpura Vibrante para el tema oscuro
    val purpleColor = Color(0xFFBB86FC)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Fondo oscuro
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Empujamos el contenido hacia abajo (posición solicitada previamente)
            Spacer(modifier = Modifier.weight(1.3f))

            // Botón Estético y Moderno (Minimalista)
            Button(
                onClick = { 
                    showAnimation = true 
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f) // Ancho elegante
                    .height(56.dp)
                    .border(
                        BorderStroke(2.dp, purpleColor), // Borde púrpura
                        shape = RoundedCornerShape(percent = 50) // Forma redondeada (Pill shape)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent, // Fondo transparente
                    contentColor = purpleColor // Texto púrpura
                ),
                shape = RoundedCornerShape(percent = 50),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = "INICIAR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp // Espaciado limpio
                )
            }
            
            // Espacio inferior
            Spacer(modifier = Modifier.weight(0.7f))
        }
        
        // Flower Animation Overlay
        AnimatedVisibility(
            visible = showAnimation,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut()
        ) {
            FlowerAnimation(
                onAnimationFinished = onLoginSuccess
            )
        }
    }
}
