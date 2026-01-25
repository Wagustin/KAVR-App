package com.thanhng224.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.thanhng224.app.presentation.App
import com.thanhng224.app.presentation.ui.theme.KotlinAndroidTemplateTheme
import dagger.hilt.android.AndroidEntryPoint

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var musicManager: com.thanhng224.app.core.audio.MusicManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start music on app launch
        musicManager.startMusic()

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val isLightTheme = true // TODO: Replace with actual theme check
        controller.isAppearanceLightNavigationBars = isLightTheme
        controller.isAppearanceLightStatusBars = isLightTheme
        setContent {
            KotlinAndroidTemplateTheme {
                App()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        musicManager.resumeMusic()
    }

    override fun onPause() {
        super.onPause()
        musicManager.pauseMusic()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
             musicManager.release()
        }
    }
}
