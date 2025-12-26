package com.thanhng224.app.feature.memories

import androidx.lifecycle.ViewModel
import com.thanhng224.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MemoriesViewModel @Inject constructor() : ViewModel() {

    private val _photos = MutableStateFlow<List<MemoryPhoto>>(emptyList())
    val photos = _photos.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        // LISTA DE FOTOS PRE-CARGADAS
        // NOTA: Asegúrate de tener imágenes en res/drawable llamadas us_1, us_2, etc.
        // Si no existen, usa iconos del sistema temporalmente para evitar errores de compilación.
        val tempList = listOf(
            MemoryPhoto(1, R.drawable.ic_launcher_background, "El comienzo ❤️"), // Cambiar por R.drawable.us_1
            MemoryPhoto(2, R.drawable.ic_launcher_foreground, "Esa vez en el parque"), // Cambiar por R.drawable.us_2
            MemoryPhoto(3, R.drawable.ic_launcher_background, "Tu sonrisa"),
            MemoryPhoto(4, R.drawable.ic_launcher_foreground, "Cumpleaños feliz"),
            MemoryPhoto(5, R.drawable.ic_launcher_background, "Viaje juntos"),
            MemoryPhoto(6, R.drawable.ic_launcher_foreground, "Cena romántica")
        )
        _photos.value = tempList
    }

    fun shufflePhotos() {
        _photos.update { it.shuffled() }
    }
}