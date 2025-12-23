package com.thanhng224.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.thanhng224.app.R

/**
 * Constantes para evitar 'magic numbers' en la navegación del Memory Game.
 */
object MemoryGameConfig {
    // Submodos
    const val SUBMODE_ZEN = 0
    const val SUBMODE_ATTEMPTS = 1
    const val SUBMODE_TIMER = 2

    // Dificultades
    const val DIFFICULTY_EASY = 0
    const val DIFFICULTY_MEDIUM = 1
    const val DIFFICULTY_HARD = 2
}

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    open val unreadCount: Int = 0

    data object Onboarding : Screen("onboarding", R.string.nav_onboarding, Icons.Default.Home)
    data object Login : Screen("login", R.string.nav_login, Icons.Default.Person)
    data object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    data object Favorites : Screen("favorites", R.string.nav_favorites, Icons.Default.Favorite)
    data object Profile : Screen("profile", R.string.nav_profile, Icons.Default.Person)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    
    data object SnakeGame : Screen("snake_game/{mode}", R.string.game_snake, Icons.Default.Gamepad) {
        fun createRoute(mode: Int) = "snake_game/$mode"
    }
    
    // CORRECCIÓN: La ruta ahora acepta los 3 parámetros obligatorios.
    data object MemoryGame : Screen(
        route = "memory_game/{mode}/{submode}/{difficulty}",
        titleRes = R.string.game_memory,
        icon = Icons.Default.Star
    ) {
        /**
         * Helper para construir la ruta de navegación de forma segura.
         * @param mode 1=SinglePlayer, 2=MultiPlayer
         * @param submode 0=Zen, 1=Vidas, 2=Tiempo
         * @param difficulty 0=Fácil, 1=Medio, 2=Difícil
         */
        // CORRECCIÓN: El helper ahora pide los 3 enteros.
        fun createRoute(mode: Int, submode: Int, difficulty: Int) = 
            "memory_game/$mode/$submode/$difficulty"
    }
}
