package com.example.pitchsense.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pitchsense.ui.screens.*

/**
 * PitchSenseApp manages the app's navigation state.
 * It uses 'rememberNavController' to track where the user is in the app.
 */
@Composable
fun PitchSenseApp() {
    val navController = rememberNavController()

    /**
     * NavHost acts as a container for the screens.
     * 'startDestination' defines which screen appears first when the app opens.
     */
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // Defines the route "login" and links it to the LoginScreen composable
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    /**
                     * After successful login, we navigate to the overview.
                     * popUpTo("login") { inclusive = true } ensures the user
                     * cannot go back to the login screen using the back button.
                     */
                    navController.navigate("overview") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // The main dashboard screen
        composable("overview") {
            OverviewScreen(
                onNavigateToAdvancedStats = { navController.navigate("advanced_stats") },
                onNavigateToHeatMap = { navController.navigate("heat_map") },
                onNavigateToPitchSequence = { navController.navigate("pitch_sequence") }
            )
        }

        // Secondary feature screens. onBackClick simply reverses the last navigation action.
        composable("advanced_stats") {
            AdvancedStatsScreen(onBackClick = { navController.popBackStack() })
        }
        composable("heat_map") {
            HeatMapScreen(onBackClick = { navController.popBackStack() })
        }
        composable("pitch_sequence") {
            PitchSequenceScreen(onBackClick = { navController.popBackStack() })
        }
    }
}