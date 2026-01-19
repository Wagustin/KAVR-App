package com.thanhng224.app.feature.product.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
                    // Movimiento constante muy lento: 0.5 pixels por frame
                    // Esto es visualmente fluido pero computacionalmente barato
                     scrollState.dispatchRawDelta(1f) 
                }
            }
        }
    }

    // Contenedor que llena la pantalla y bloquea clicks (Escudo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)

    ) {
        if (photos.isNotEmpty()) {
            
            // Lista cacheada para evitar recalculos en cada frame
            // Reducida a 2 repeticiones, suficiente para llenar pantalla sin saturar RAM
            val infiniteList = remember(photos) { 
                List(20) { photos }.flatten() 
            } // 20 repeticiones para asegurar "infinito" visual sin ser infinito real

            // 1. EL MOSAICO DE FONDO
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(3), // 3 Columnas se ve mejor
                state = scrollState,
                userScrollEnabled = false,
                verticalItemSpacing = 4.dp,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.55f) // Oscurecido para resaltar texto
            ) {

                items(infiniteList) { photo ->
                    // Altura aleatoria determinista basada en el ID de la foto para evitar saltos en recomposición
                    val randomHeight = remember(photo.resId) { 
                        (160 + (photo.resId % 100)).dp 
                    }
                    
                    Image(
                        painter = painterResource(id = photo.resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop, // Crop es ligero si la imagen no es gigante
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(randomHeight)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // 2. VINETA NEGRA (Sombra en los bordes)
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

            // 3. TEXTO CENTRAL GIGANTE EN CAJA
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f)) // Fondo semitransparente para legibilidad
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Feliz\nCumpleaños",
                        style = TextStyle(
                            fontSize = 48.sp, // Reduced from 60.sp to prevent wrapping
                            fontWeight = FontWeight.Bold,
                            color = GoldColor,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black,
                                blurRadius = 20f
                            )
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Mi Amor",
                        style = TextStyle(
                            fontSize = 58.sp, // Reduced from 72.sp
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(
                                color = Color.Black,
                                blurRadius = 20f
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    // Text removed per user request (No memories link)
                }
            }

        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando...", color = Color.White)
            }
        }
    }
}
