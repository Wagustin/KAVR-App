package com.thanhng224.app.feature.product.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.thanhng224.app.feature.memories.MemoriesViewModel
import com.thanhng224.app.presentation.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

val GoldColor = Color(0xFFFFD700)

@Composable
fun HomeScreen(
    navController: NavHostController,
    memoriesViewModel: MemoriesViewModel = hiltViewModel()
) {
    val photos by memoriesViewModel.photos.collectAsState()
    val scrollState = rememberLazyStaggeredGridState()

    // --- MOTOR DE SCROLL OPTIMIZADO PARA GAMA BAJA ---
    LaunchedEffect(photos) {
        if (photos.isNotEmpty()) {
            delay(500) // Pequeña pausa inicial
            val startTime = System.nanoTime()
            while (isActive) {
                // withFrameMillis sincroniza con el refresco de pantalla (60fps -> 16ms)
                // Es más eficiente que delay() fijo y evita "jank"
                withFrameMillis { frameTimeMillis ->
                    // Movimiento más rápido solicitado por usuario (4x más rápido que antes)
                     scrollState.dispatchRawDelta(4f) 
                }
            }
        }
    }

    // Contenedor que llena la pantalla
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (photos.isNotEmpty()) {
            
            // Lista cacheada
            val infiniteList = remember(photos) { 
                List(20) { photos }.flatten() 
            } 

            // 1. EL MOSAICO DE FONDO
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3),
                state = scrollState,
                userScrollEnabled = false,
                verticalItemSpacing = 4.dp,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.55f)
            ) {
                items(infiniteList) { photo ->
                    val randomHeight = remember(photo.resId) { 
                        (160 + (photo.resId % 100)).dp 
                    }
                    Image(
                        painter = painterResource(id = photo.resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(randomHeight)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // 2. VINETA NEGRA
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // 3. TARJETA DE CUMPLEAÑOS AESTHETIC
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f)) // Fondo oscuro semi-transp
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)) // Borde sutil
                    .padding(vertical = 32.dp, horizontal = 24.dp)
            ) {
                 Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // "Feliz Cumpleaños" con Gradiente y Cursiva
                    Text(
                        text = "Feliz\nCumpleaños",
                        style = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFB7B2), // Pastel Pink
                                    Color(0xFFFFDAC1), // Pastel Peach
                                    Color(0xFFE2F0CB)  // Pastel Green
                                )
                            ),
                            shadow = Shadow(
                                color = Color.Magenta.copy(alpha = 0.5f),
                                blurRadius = 25f
                            )
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // "Mi Amor" en blanco elegante
                    Text(
                        text = "Mi Amor",
                        style = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontSize = 48.sp, 
                            fontWeight = FontWeight.Normal,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black,
                                blurRadius = 10f
                            )
                        )
                    )

                    // Decoración de Flores/Estrellas (Visual)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌸", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("✨", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("🌸", fontSize = 24.sp)
                    }
                }
            }

        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando...", color = Color.White)
            }
        }
    }
}
