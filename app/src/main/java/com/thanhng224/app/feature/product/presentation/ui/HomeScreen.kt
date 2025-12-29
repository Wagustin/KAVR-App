@file:Suppress("DEPRECATION")

package com.thanhng224.app.feature.product.presentation.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import com.thanhng224.app.feature.memories.MemoryPhoto
import com.thanhng224.app.presentation.ui.FlowerAnimation
import kotlinx.coroutines.delay

// Definición de colores
val DarkCyanColor = Color(0xFF006064) // Cian Oscuro / Azul Petróleo
val DarkBlueColor = Color(0xFF1A237E) // Azul Oscuro Profundo
val BorderColor = DarkCyanColor // Usaremos este para los bordes

// Forma de Diamante (Rombo)
val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f) // Arriba centro
    lineTo(size.width, size.height / 2f) // Derecha centro
    lineTo(size.width / 2f, size.height) // Abajo centro
    lineTo(0f, size.height / 2f) // Izquierda centro
    close()
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    memoriesViewModel: MemoriesViewModel = hiltViewModel()
) {
    val photos by memoriesViewModel.photos.collectAsState()
    var showAnimation by remember { mutableStateOf(false) }

    // Ciclo automático para mezclar fotos (Motor Dinámico)
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Cambia las fotos cada 5 segundos
            memoriesViewModel.shufflePhotos()
        }
    }

    // Contenedor principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable { showAnimation = true } // Al hacer click, lanza la animación
    ) {
        if (photos.isNotEmpty()) {
            // 1. Fondo de Collage Geométrico con Transición Suave
            Crossfade(
                targetState = photos, 
                animationSpec = tween(durationMillis = 1500),
                label = "BackgroundAnimation"
            ) { currentPhotos ->
                GeometricCollageBackground(photos = currentPhotos)
            }

            // 2. Diamante Central (Rombo) con Fotos adentro
            // Usamos un Box sin rotación pero con clip en forma de diamante para que las fotos se vean rectas
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(350.dp) // Hacemos el diamante un poco más grande
                    .border(6.dp, BorderColor, DiamondShape) // Borde con forma de diamante
                    .clip(DiamondShape) // Recortamos el contenido en forma de diamante
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Dentro del rombo también mostramos 4 fotos (como afuera)
                // Usamos las siguientes 4 fotos de la lista mezclada
                val innerPhotos = if (photos.size >= 8) photos.subList(4, 8) else photos.take(4)

                // Fondo de fotos interno (Recto, sin rotar)
                GeometricCollageBackground(photos = innerPhotos, showOverlay = false)

                // Cruz divisoria del rombo (Vertical y Horizontal)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    // Vertical
                    drawLine(
                        color = BorderColor,
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = strokeWidth
                    )
                    // Horizontal
                    drawLine(
                        color = BorderColor,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = strokeWidth
                    )
                }

                // Texto SUPERPUESTO
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ElegantText(
                        text = "Feliz Cumpleaños\nMi Amor",
                        modifier = Modifier
                    )
                }
            }
        } else {
            // Placeholder
            Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                Text("Cargando recuerdos...", color = Color.White)
            }
        }
        
        // 3. Capa de animación de flores (se superpone cuando está activa)
        if (showAnimation) {
            FlowerAnimation(
                modifier = Modifier.fillMaxSize(),
                onAnimationFinished = {
                    showAnimation = false
                }
            )
        }
    }
}

@Composable
fun GeometricCollageBackground(photos: List<MemoryPhoto>, showOverlay: Boolean = true) {
    // Un grid simple de 2x2
    Column(modifier = Modifier.fillMaxSize()) {
        // Fila Superior
        Row(modifier = Modifier.weight(1f)) {
            CollageImageItem(
                photo = photos.getOrNull(0),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                showOverlay = showOverlay
            )
            CollageImageItem(
                photo = photos.getOrNull(1),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                showOverlay = showOverlay
            )
        }
        // Fila Inferior
        Row(modifier = Modifier.weight(1f)) {
            CollageImageItem(
                photo = photos.getOrNull(2),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                showOverlay = showOverlay
            )
            CollageImageItem(
                photo = photos.getOrNull(3),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                showOverlay = showOverlay
            )
        }
    }
    
    // Líneas divisorias (Cruz) para el fondo principal
    if (showOverlay) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            // Línea vertical
            drawLine(
                color = BorderColor,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = strokeWidth
            )
            // Línea horizontal
            drawLine(
                color = BorderColor,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = strokeWidth
            )
        }
    }
}

@Composable
fun CollageImageItem(photo: MemoryPhoto?, modifier: Modifier, showOverlay: Boolean = true) {
    Box(modifier = modifier) {
        if (photo != null) {
            Image(
                painter = painterResource(id = photo.resId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Overlay sutil para el fondo, para que el rombo central destaque más
            if (showOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
        }
    }
}

@Composable
fun ElegantText(text: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val customTypeface = remember {
        try {
            val fontId = context.resources.getIdentifier("mi_letra_elegante", "font", context.packageName)
            if (fontId != 0) {
                ResourcesCompat.getFont(context, fontId)
            } else {
                Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            }
        } catch (e: Exception) {
            Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
    }

    // TAMAÑO DE TEXTO MUY GRANDE
    val textSizePx = with(density) { 80.sp.toPx() } 
    val strokeWidthPx = with(density) { 3.dp.toPx() }

    val strokePaint = remember {
        Paint().apply {
            typeface = customTypeface
            color = BorderColor.toArgb() // Borde del texto igual al de los marcos
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.White.toArgb()) // Sombra blanca para resaltar
            textAlign = Paint.Align.CENTER 
        }
    }

    val fillPaint = remember {
        Paint().apply {
            typeface = customTypeface
            color = Color.White.toArgb() // Texto blanco
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(12f, 0f, 0f, Color.Black.copy(alpha=0.6f).toArgb()) // Sombra oscura de fondo
            textAlign = Paint.Align.CENTER
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxWidth().height(400.dp)) {
        val lines = text.split("\n")
        val lineHeight = textSizePx * 1.1f
        val totalHeight = lines.size * lineHeight
        
        var currentY = center.y - (totalHeight / 2) + (textSizePx / 3)

        drawIntoCanvas { canvas ->
            lines.forEach { line ->
                canvas.nativeCanvas.drawText(line, center.x, currentY, strokePaint)
                canvas.nativeCanvas.drawText(line, center.x, currentY, fillPaint)
                currentY += lineHeight
            }
        }
    }
}
