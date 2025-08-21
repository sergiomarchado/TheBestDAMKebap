// navigation/AppNavHost.kt
package com.sergiom.thebestdamkebap.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Standalone.SPLASH
    ) {
        splashRoute(navController)
        authGraph(navController)
        homeGraph(navController)
    }
}
