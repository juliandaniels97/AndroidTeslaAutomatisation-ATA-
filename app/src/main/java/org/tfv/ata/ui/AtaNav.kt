package org.tfv.ata.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.tfv.ata.R
import org.tfv.ata.ui.screens.AboutScreen
import org.tfv.ata.ui.screens.AutomationScreen
import org.tfv.ata.ui.screens.GuideScreen
import org.tfv.ata.ui.screens.GuideTargets
import org.tfv.ata.ui.screens.HomeScreen
import org.tfv.ata.ui.screens.MenuConfigScreen
import org.tfv.ata.ui.screens.PairScreen
import org.tfv.ata.ui.screens.ReportScreen
import org.tfv.ata.ui.screens.SettingsScreen
import org.tfv.ata.ui.screens.VehicleScreen
import org.tfv.ata.ui.screens.VehiclesScreen
import org.tfv.ata.ui.screens.WearConfigScreen
import org.tfv.ata.ui.screens.WearPairScreen

private enum class Tab(val route: String, val labelRes: Int) {
  HOME("home", R.string.tab_home),
  VEHICLE("vehicle", R.string.tab_vehicle),
  SETTINGS("settings", R.string.tab_settings),
}

@Composable
fun AtaNavHost(vm: AtaViewModel, state: AtaUiState) {
  val nav = rememberNavController()
  val backStack by nav.currentBackStackEntryAsState()
  val currentRoute = backStack?.destination?.route

  Scaffold(
    bottomBar = {
      if (currentRoute in Tab.entries.map { it.route }) {
        NavigationBar {
          Tab.entries.forEach { tab ->
            val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
              selected = selected,
              onClick = {
                nav.navigate(tab.route) {
                  popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = {
                Icon(
                  when (tab) {
                    Tab.HOME -> Icons.Filled.Dashboard
                    Tab.VEHICLE -> Icons.Filled.DirectionsCar
                    Tab.SETTINGS -> Icons.Filled.Settings
                  },
                  contentDescription = null,
                )
              },
              label = { Text(stringResource(tab.labelRes)) },
            )
          }
        }
      }
    },
  ) { padding ->
    NavHost(
      navController = nav,
      startDestination = Tab.HOME.route,
      modifier = Modifier.padding(padding),
    ) {
      composable(Tab.HOME.route) { HomeScreen(vm, state, onAddVehicle = { nav.navigate("vehicles") }) }
      composable(Tab.VEHICLE.route) {
        VehicleScreen(vm, state, onAddVehicle = { nav.navigate("vehicles") }, onPair = { nav.navigate("pair") })
      }
      composable(Tab.SETTINGS.route) {
        SettingsScreen(
          vm, state,
          onVehicles = { nav.navigate("vehicles") },
          onMenu = { nav.navigate("menu") },
          onWear = { nav.navigate("wear") },
          onAutomation = { nav.navigate("automation") },
          onAbout = { nav.navigate("about") },
          onGuide = { nav.navigate("guide") },
          onReport = { nav.navigate("report") },
        )
      }
      composable("vehicles") { VehiclesScreen(vm, state, onBack = { nav.popBackStack() }) }
      composable("menu") { MenuConfigScreen(vm, state, onBack = { nav.popBackStack() }) }
      composable("wear") { WearConfigScreen(vm, state, onBack = { nav.popBackStack() }, onPairWatch = { nav.navigate("wear_pair") }) }
      composable("wear_pair") {
        WearPairScreen(vm, state, onBack = { nav.popBackStack() }, onReport = { nav.navigate("report") })
      }
      composable("report") { ReportScreen(onBack = { nav.popBackStack() }) }
      composable("automation") { AutomationScreen(vm, state, onBack = { nav.popBackStack() }) }
      composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
      composable("pair") {
        PairScreen(
          vm, state,
          onBack = { nav.popBackStack() },
          onVehicles = { nav.navigate("vehicles") },
          onReport = { nav.navigate("report") },
        )
      }
      composable("guide") {
        GuideScreen(
          vm, state,
          onBack = { nav.popBackStack() },
          go = GuideTargets(
            vehicles = { nav.navigate("vehicles") },
            pair = { nav.navigate("pair") },
            menu = { nav.navigate("menu") },
            wear = { nav.navigate("wear") },
            automation = { nav.navigate("automation") },
          ),
        )
      }
    }
  }
}
