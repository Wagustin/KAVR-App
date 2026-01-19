package com.thanhng224.app.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thanhng224.app.R

@Composable
fun ProfileScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3E2723)), // Background Color: Dark Wood
        contentAlignment = Alignment.Center
    ) {
        // Paper Sheet
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.8f)
                .background(Color(0xFFFDFBF7)) // Off-white paper color
                .drawBehind {
                    val width = size.width
                    val height = size.height

                    // Paper Lines
                    val lineHeight = 40.dp.toPx()
                    val startY = 100.dp.toPx()
                    var y = startY
                    
                    while (y < height - 40.dp.toPx()) {
                        drawLine(
                            color = Color(0xFFB0BEC5), // Light Blue line
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 2f
                        )
                        y += lineHeight
                    }

                    // Margin Line
                    drawLine(
                        color = Color(0xFFFFCDD2), // Red margin
                        start = Offset(width * 0.15f, 0f),
                        end = Offset(width * 0.15f, height),
                        strokeWidth = 2f
                    )
                }
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Date / Header
                Text(
                    text = "Para Ti",
                    fontFamily = FontFamily.Cursive,
                    fontSize = 32.sp,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.End)
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Body Text Placeholder
                Text(
                    text = "Escribe aquí tu dedicatoria...",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    lineHeight = 40.sp, 
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
