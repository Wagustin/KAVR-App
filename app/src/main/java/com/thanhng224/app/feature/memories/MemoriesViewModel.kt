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
        // NOTA IMPORTANTE PARA EL USUARIO:
        // Asegúrate de haber renombrado tus archivos en res/drawable a minúsculas
        // Ejemplo: img_0707.jpg -> img_0707.jpg
        
        val tempList = listOf(
            // Usando los nombres de archivo que mostraste en la captura (convertidos a minúsculas)
            MemoryPhoto(1, R.drawable.img_0707, "Recuerdo 1"),
            MemoryPhoto(2, R.drawable.img_0847, "Recuerdo 2"),
            MemoryPhoto(3, R.drawable.img_1025, "Recuerdo 3"),
            MemoryPhoto(4, R.drawable.img_1045, "Recuerdo 4"),
            MemoryPhoto(5, R.drawable.img_1048, "Recuerdo 5"),
            MemoryPhoto(6, R.drawable.img_1508, "Recuerdo 6"),
            MemoryPhoto(7, R.drawable.img_1812, "Recuerdo 7"),
            MemoryPhoto(8, R.drawable.img_1978, "Recuerdo 8"),
            MemoryPhoto(9, R.drawable.img_1981, "Recuerdo 9"),
            MemoryPhoto(10, R.drawable.img_2136, "Recuerdo 10"),
            MemoryPhoto(11, R.drawable.img_2137, "Recuerdo 11"),
            MemoryPhoto(12, R.drawable.img_2149, "Recuerdo 12"),
            MemoryPhoto(13, R.drawable.kitkatq1, "KitKat 1"),
            MemoryPhoto(14, R.drawable.kitkatq2, "KitKat 2")
        )
        _photos.value = tempList
    }

    fun shufflePhotos() {
        _photos.update { it.shuffled() }
    }
}
