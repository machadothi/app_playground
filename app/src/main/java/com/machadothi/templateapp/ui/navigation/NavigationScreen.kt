package com.machadothi.templateapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.machadothi.templateapp.ui.screen.filter.FiltersScreen
import com.machadothi.templateapp.ui.screen.graph.GraphScreen
import com.machadothi.templateapp.ui.screen.graph.GraphType
import com.machadothi.templateapp.ui.screen.graph.humidity.HumidityGraphScreen
import com.machadothi.templateapp.ui.screen.graph.temperature.TemperatureGraphScreen
import com.machadothi.templateapp.ui.screen.sensor.SensorsScreen

@Composable
fun NavigationScreen(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Sensors,
        modifier = modifier
    ) {
        composable<NavRoutes.Sensors> {
            SensorsScreen(
                onFiltersSelected = {
                    navController.navigate(NavRoutes.Graph)
                }
            )
        }
        composable<NavRoutes.Filter> {
            FiltersScreen()
        }
        composable<NavRoutes.Graph> {
            GraphScreen(onGraphSelected = { graph ->
                when (graph) {
                    GraphType.TEMPERATURE -> navController.navigate(NavRoutes.Graph.Temperature)
                    GraphType.HUMIDITY -> navController.navigate(NavRoutes.Graph.Humidity)
                }
            })
        }
        composable<NavRoutes.Graph.Humidity> {
            HumidityGraphScreen()
        }
        composable<NavRoutes.Graph.Temperature> {
            TemperatureGraphScreen()
        }
    }
}