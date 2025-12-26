package com.thanhng224.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.thanhng224.app.R

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    open val unreadCount: Int = 0

    data object Onboarding : Screen("onboarding", R.string.nav_onboarding, Icons.Default.Home)
    data object Login : Screen("login", R.string.nav_login, Icons.Default.Person)
    data object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    data object Favorites : Screen("favorites", R.string.nav_favorites, Icons.Default.Favorite)
    data object Profile : Screen("profile", R.string.nav_profile, Icons.Default.Person)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    data object Memories : Screen("memories", R.string.nav_home, Icons.Default.Favorite)

    data object SnakeGame : Screen("snake_game/{gameMode}", R.string.game_snake, Icons.Default.Gamepad) {
        fun createRoute(gameMode: Int) = "snake_game/$gameMode"
    }

    data object MemoryGame : Screen("memory_game/{players}/{submode}/{difficulty}", R.string.game_memory, Icons.Default.Star) {
        fun createRoute(players: Int, submode: Int, difficulty: Int) = "memory_game/$players/$submode/$difficulty"

        // Constantes para los modos
        const val SUBMODE_ZEN = 0
        const val SUBMODE_ATTEMPTS = 1
        const val SUBMODE_TIMER = 2
        
        // Constantes para dificultad
        const val DIFFICULTY_EASY = 0
        const val DIFFICULTY_MEDIUM = 1
        const val DIFFICULTY_HARD = 2
    }
}
