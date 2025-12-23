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

sealed class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    open val unreadCount: Int = 0

    data object Onboarding : Screen("onboarding", R.string.nav_onboarding, Icons.Default.Home)
    data object Login : Screen("login", R.string.nav_login, Icons.Default.Person)
    data object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    data object Favorites : Screen("favorites", R.string.nav_favorites, Icons.Default.Favorite) {
        override val unreadCount: Int = 0
    }
    data object Profile : Screen("profile", R.string.nav_profile, Icons.Default.Person)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    
    // New Game Screens
    data object SnakeGame : Screen("snake_game", R.string.game_snake, Icons.Default.Gamepad)
    data object MemoryGame : Screen("memory_game", R.string.game_memory, Icons.Default.Star)
}
