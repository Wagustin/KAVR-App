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
import androidx.compose.runtime.LaunchedEffect
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
    // Color Púrpura Vibrante para el tema oscuro (Unused now, but kept if needed for other things or removed)
    // val purpleColor = Color(0xFFBB86FC) 

    // Auto-start animation
    LaunchedEffect(Unit) {
        showAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fondo negro cinemático
    ) {
        // No Button anymore.
        
        // Flower Animation Overlay
        if (showAnimation) {
             FlowerAnimation(
                onAnimationFinished = onLoginSuccess
            )
        }
    }
}
