package com.thanhng224.app.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thanhng224.app.feature.auth.presentation.ui.LoginScreen
import com.thanhng224.app.feature.games.MemoryGameScreen
import com.thanhng224.app.feature.games.SnakeGameScreen
import com.thanhng224.app.feature.memories.MemoriesScreen
import com.thanhng224.app.feature.onboarding.presentation.ui.OnboardingScreen
import com.thanhng224.app.feature.product.presentation.ui.HomeScreen
import com.thanhng224.app.presentation.navigation.Screen
import com.thanhng224.app.presentation.screen.FavoritesScreen
import com.thanhng224.app.presentation.screen.ProfileScreen
import com.thanhng224.app.presentation.screen.SettingsScreen
import com.thanhng224.app.presentation.ui.theme.KotlinAndroidTemplateTheme
import com.thanhng224.app.presentation.viewmodel.AppViewModel

@Composable
fun App(
    appViewModel: AppViewModel = hiltViewModel()
) {
    val items = listOf(Screen.Home, Screen.Favorites, Screen.Profile, Screen.Settings)
    val navController = rememberNavController()
    val startDestination by appViewModel.startDestination.collectAsStateWithLifecycle()
    val isDarkMode by appViewModel.isDarkMode.collectAsStateWithLifecycle()

    KotlinAndroidTemplateTheme(
        darkTheme = isDarkMode,
        dynamicColor = true // Enabled for Android 12+ support
    ) {
        if (startDestination == null) {
            // Show loading while determining start destination
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@KotlinAndroidTemplateTheme
        }

        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in listOf(
            Screen.Home.route,
            Screen.Favorites.route,
            Screen.Profile.route,
            Screen.Settings.route
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Scaffold(
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination!!
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(
                                onGetStarted = {
                                    appViewModel.completeOnboarding()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Screen.Home.route) { HomeScreen(navController) }
                        composable(Screen.Favorites.route) { FavoritesScreen(navController) }
                        composable(Screen.Profile.route) { ProfileScreen() }
                        composable(Screen.Settings.route) {
                            SettingsScreen()
                        }
                        composable(Screen.Memories.route) { MemoriesScreen(navController) }
                        
                        // CORRECCIÓN: Rutas de juegos con argumentos actualizados
                        composable(
                            route = Screen.SnakeGame.route,
                            arguments = listOf(navArgument("difficulty") { type = NavType.IntType })
                        ) { 
                            SnakeGameScreen(navController = navController)
                        }
                        
                        composable(
                            route = Screen.MemoryGame.route,
                            arguments = listOf(
                                navArgument("players") { type = NavType.IntType },
                                navArgument("submode") { type = NavType.IntType },
                                navArgument("difficulty") { type = NavType.IntType },
                            )
                        ) { 
                            MemoryGameScreen(navController = navController)
                        }

                        // NUEVOS JUEGOS
                        composable(
                            route = Screen.CrocodileGame.route,
                            arguments = listOf(navArgument("difficulty") { type = NavType.IntType })
                        ) { 
                            com.thanhng224.app.feature.games.CrocodileGameScreen(navController) 
                        }
                        composable(
                            route = Screen.TimerGame.route,
                            arguments = listOf(
                                navArgument("players") { type = NavType.IntType },
                                navArgument("difficulty") { type = NavType.IntType }
                            )
                        ) {
                             com.thanhng224.app.feature.games.TimerGameScreen(navController)
                        }
                        composable(
                            route = Screen.SoccerGame.route,
                            arguments = listOf(navArgument("difficulty") { type = NavType.IntType })
                        ) {
                            com.thanhng224.app.feature.games.SoccerGameScreen(navController)
                        }
                        composable(
                            route = Screen.MiniGolfGame.route,
                            arguments = listOf(navArgument("difficulty") { type = NavType.IntType })
                        ) {
                            com.thanhng224.app.feature.games.MiniGolfGameScreen(navController)
                        }
                    }

                    if (showBottomBar) {
                        FloatingNavBar(
                            items = items,
                            navController = navController,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavBar(
    items: List<Screen>,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(24.dp))
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0) // 💥 key line
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == screen.route } == true

                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.9f else if (selected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "navItemScale"
                    )

                    val offsetY by animateDpAsState(
                        targetValue = if (isPressed) 3.dp else 0.dp,
                        label = "navItemOffsetY"
                    )

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentDestination?.route != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        interactionSource = interactionSource,
                        icon = {
                            if (screen is Screen.Favorites && screen.unreadCount > 0) {
                                BadgedBox(badge = { Badge { Text("${screen.unreadCount}") } }) {
                                    Icon(screen.icon, contentDescription = stringResource(screen.titleRes))
                                }
                            } else {
                                Icon(
                                    screen.icon,
                                    contentDescription = stringResource(screen.titleRes),
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .offset(y = offsetY)
                                )
                            }
                        },
                        label = {
                            AnimatedVisibility(visible = selected) { Text(stringResource(screen.titleRes)) }
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
