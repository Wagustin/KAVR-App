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
            delay(1000) // Pequeña pausa inicial
            while (isActive) {
                // Truco Anti-Lag: Mover más distancia (2px) pero con menos frecuencia (80ms)
                // Esto le da "respiro" al procesador entre cada cuadro de animación.
                scrollState.scrollBy(2f) 
                delay(80) 
            }
        }
    }

    // Contenedor que llena la pantalla y bloquea clicks (Escudo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
                // Al hacer click en cualquier lado, entramos al collage completo
                // navController.navigate(Screen.Memories.route) // REMOVED PER USER REQUEST
            }
    ) {
        if (photos.isNotEmpty()) {
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
                // Lista repetida para efecto infinito (tamaño reducido drásticamente para evitar crash de RAM - de 15 a 4)
                val infiniteList = List(4) { photos }.flatten()
                items(infiniteList) { photo ->
                    val randomHeight = remember(photo) { Random.nextInt(160, 260).dp }
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
