package com.thanhng224.app.feature.games

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat

/**
 * Loads a bitmap from drawable resources safely.
 * - Handles VectorDrawables (converts to Bitmap)
 * - Downscales to specified width/height to prevent OOM
 * - Returns null instead of crashing on error
 */
fun loadBitmapSafe(context: Context, name: String, w: Int, h: Int): Bitmap? {
    return try {
        val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (resId == 0) return null
        
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        
        // Create scaled bitmap
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        bitmap
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}
