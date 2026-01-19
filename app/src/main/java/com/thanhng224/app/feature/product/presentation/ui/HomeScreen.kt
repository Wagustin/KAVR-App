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
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
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

    // --- MOTOR DE SCROLL OPTIMIZADO CON DELTA TIME (Velocidad Constante) ---
    LaunchedEffect(photos) {
        if (photos.isNotEmpty()) {
            delay(500) // Esperar a que cargue UI
            var lastTime = System.nanoTime()
            val speedPxPerSec = 250f // Velocidad objetivo: 250 pixels por segundo

            while (isActive) {
                withFrameNanos { time ->
                    val dt = (time - lastTime) / 1_000_000_000f // Convertir nanosegundos a segundos
                    lastTime = time
                    
                    // Limitar dt para evitar saltos grandes si el hilo se congela un momento
                    val safeDt = dt.coerceAtMost(0.1f) 
                    
                    if (safeDt > 0) {
                        val scrollDelta = speedPxPerSec * safeDt
                        scrollState.dispatchRawDelta(scrollDelta)
                    }
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
            // Reducida a 8 repeticiones para mejorar la carga inicial y uso de memoria
            val infiniteList = remember(photos) { 
                List(8) { photos }.flatten() 
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
                // Usamos itemsIndexed con key única para optimizar el recomposición de Compose
                itemsIndexed(
                    items = infiniteList,
                    key = { index, _ -> index } // Key estable basada en posición
                ) { index, photo ->
                    
                    // Altura predecible y cíclica para evitar calculos aleatorios (CPU más ligero)
                    // Ciclo de 4 alturas diferentes: 180, 240, 160, 220
                    val height = remember(index) {
                        when (index % 4) {
                            0 -> 180.dp
                            1 -> 240.dp
                            2 -> 160.dp
                            else -> 220.dp
                        }
                    }
                    
                    Image(
                        painter = painterResource(id = photo.resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height)
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
