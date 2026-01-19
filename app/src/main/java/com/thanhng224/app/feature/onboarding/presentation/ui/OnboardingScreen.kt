package com.thanhng224.app.feature.onboarding.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import com.thanhng224.app.presentation.ui.FlowerAnimation

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    // State to trigger animation
    var startAnimation by remember { mutableStateOf(false) }

    // Auto-start logic
    LaunchedEffect(Unit) {
        startAnimation = true
        // Wait for animation duration (approx 2.5s for visual finish)
        delay(3500) 
        onGetStarted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Black Background
        contentAlignment = Alignment.Center
    ) {
        if (startAnimation) {
            FlowerAnimation(
                onAnimationEnd = { 
                    // Optional: could trigger nav here if FlowerAnimation supports it, 
                    // but delay is safer if callback isn't reliable
                }
            )
        }
    }
}
