package com.thanhng224.app.feature.product.presentation.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
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
                // Truco Anti-Lag: Mover más distancia (2px) pero con menos frecuencia (60ms)
                // Esto le da "respiro" al procesador entre cada cuadro de animación.
                scrollState.scrollBy(2f) 
                delay(60) 
            }
        }
    }

    // Contenedor que llena la pantalla y bloquea clicks (Escudo)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                // Al hacer click en cualquier lado, entramos al collage completo
                navController.navigate(Screen.Memories.route)
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
                // Lista repetida para efecto infinito (tamaño controlado para no saturar RAM)
                val infiniteList = List(15) { photos }.flatten()
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

            // 3. TEXTO CENTRAL GIGANTE
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LuxuryText(text = "Feliz", fontSize = 65f)
                LuxuryText(text = "Cumpleaños", fontSize = 65f)
                Spacer(modifier = Modifier.height(16.dp))
                LuxuryText(text = "Mi Amor", fontSize = 90f) // GIGANTE
                
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "(Toca para ver nuestros recuerdos)",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando...", color = Color.White)
            }
        }
    }
}

@Composable
fun LuxuryText(text: String, fontSize: Float) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Carga de fuente segura: Evita error de compilación si no existe R.font
    val customTypeface = remember {
        try {
            @Suppress("DiscouragedApi") // Se suprime warning ya que es la única forma dinámica segura
            val fontId = context.resources.getIdentifier("mi_letra_elegante", "font", context.packageName)
            if (fontId != 0) {
                ResourcesCompat.getFont(context, fontId)
            } else {
                Typeface.DEFAULT_BOLD
            }
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    val textSizePx = with(density) { fontSize.sp.toPx() }
    val strokeWidthPx = with(density) { (fontSize * 0.04f).dp.toPx() } // Borde proporcional

    // PINTURAS (Paint) optimizadas se crean una sola vez con 'remember'
    
    // 1. Sombra Negra Fuerte (Para legibilidad extrema)
    val shadowPaint = remember {
        Paint().apply {
            typeface = customTypeface
            color = Color.Transparent.toArgb() // El color base no importa, solo la sombra
            style = Paint.Style.FILL
            setShadowLayer(20f, 0f, 0f, Color.Black.toArgb()) // Radio de sombra grande
        }
    }

    // 2. Borde Dorado
    val strokePaint = remember {
        Paint().apply {
            typeface = customTypeface
            color = GoldColor.toArgb()
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            isAntiAlias = true
        }
    }

    // 3. Relleno Blanco
    val fillPaint = remember {
        Paint().apply {
            typeface = customTypeface
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    // Cálculo aproximado del tamaño del Canvas para que contenga el texto
    val canvasWidth = with(density) { (textSizePx * text.length * 0.7f).toDp() }
    val canvasHeight = with(density) { (textSizePx * 1.5f).toDp() }

    Canvas(modifier = Modifier.width(canvasWidth).height(canvasHeight)) {
        val textWidth = strokePaint.measureText(text)
        
        drawContext.canvas.nativeCanvas.let { canvas ->
             // Centrar en el canvas
            val xPos = (size.width - textWidth) / 2
            val yPos = size.height - (size.height - textSizePx) / 2

            // Orden de dibujo: Sombra -> Borde -> Relleno
            canvas.drawText(text, xPos, yPos, shadowPaint)
            canvas.drawText(text, xPos, yPos, strokePaint)
            canvas.drawText(text, xPos, yPos, fillPaint)
        }
    }
}
