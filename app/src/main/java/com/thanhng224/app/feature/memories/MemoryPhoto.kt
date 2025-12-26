package com.thanhng224.app.feature.memories

data class MemoryPhoto(
    val id: Int,
    val resId: Int, // Referencia al drawable R.drawable.xxx
    val description: String
)