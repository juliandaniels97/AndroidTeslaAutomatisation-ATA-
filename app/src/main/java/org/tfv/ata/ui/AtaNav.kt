package org.tfv.ata.ui

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import org.tfv.ata.ui.screens.BackupScreen
import org.tfv.ata.ui.screens.AutomationScreen
import org.tfv.ata.ui.screens.Car3DScreen
import org.tfv.ata.ui.screens.BehaviorScreen
import org.tfv.ata.ui.screens.CarFinderScreen
import org.tfv.ata.ui.screens.DesignScreen
import org.tfv.ata.ui.screens.GuideScreen
import org.tfv.ata.ui.screens.GuideTargets
import org.tfv.ata.ui.screens.HomeScreen
import org.tfv.ata.ui.screens.KeyCardScreen
import org.tfv.ata.ui.screens.MenuConfigScreen
import org.tfv.ata.ui.screens.MenuSection
import org.tfv.ata.ui.screens.PairScreen
import org.tfv.ata.ui.screens.ReportScreen
import org.tfv.ata.ui.screens.SettingsScreen
import org.tfv.ata.ui.screens.VehicleScreen
import org.tfv.ata.ui.screens.VehiclesScreen
import org.tfv.ata.ui.screens.WearConfigScreen
import org.tfv.ata.ui.screens.WearKeyCardScreen
import org.tfv.ata.ui.screens.WearPairScreen

private enum class Tab(val route: String, val labelRes: Int) {
  HOME("home", R.string.tab_favorites),
  VEHICLE("vehicle", R.string.tab_vehicle),
  SETTINGS("settings", R.string.tab_settings),
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AtaNavHost(vm: AtaViewModel, state: AtaUiState, pendingRoute: String? = null, onRouteShown: () -> Unit = {}) {
  val nav = rememberNavController()
  LaunchedEffect(pendingRoute, state.loaded) {
    // Only once the NavHost (and its graph) exists.
    if (pendingRoute != null && state.loaded) {
      nav.navigate(pendingRoute) { launchSingleTop = true }
      onRouteShown()
    }
  }
  val backStack by nav.currentBackStackEntryAsState()
  val currentRoute = backStack?.destination?.route
  // Start where the app was left (vehicle the first time); remember each tab switch.
  val startRoute = remember(state.loaded) {
    Tab.entries.firstOrNull { it.route == state.settings.lastTab }?.route ?: Tab.VEHICLE.route
  }
  LaunchedEffect(currentRoute) {
    if (state.loaded && currentRoute != null && Tab.entries.any { it.route == currentRoute }) vm.rememberTab(currentRoute)
  }
  if (!state.loaded) return

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
                    Tab.HOME -> Icons.Filled.Star
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
      startDestination = startRoute,
      // Consume the bars' insets here, so the screens' own top bars don't add the status bar a second time.
      modifier = Modifier.padding(padding).consumeWindowInsets(padding),
    ) {
      composable(Tab.HOME.route) { HomeScreen(vm, state, onAddVehicle = { nav.navigate("vehicles") }) }
      composable(Tab.VEHICLE.route) {
        VehicleScreen(
          vm, state,
          onAddVehicle = { nav.navigate("vehicles") },
          onPair = { nav.navigate("pair") },
          onFindCar = { nav.navigate("find_car") },
          onOpen3d = { nav.navigate("car3d") },
        )
      }
      composable(Tab.SETTINGS.route) {
        SettingsScreen(
          vm, state,
          onVehicles = { nav.navigate("vehicles") },
          onMenu = { nav.navigate("menu") },
          onDesign = { nav.navigate("design") },
          onBehavior = { nav.navigate("behavior") },
          onWear = { nav.navigate("wear") },
          onAutomation = { nav.navigate("automation") },
          onAbout = { nav.navigate("about") },
          onGuide = { nav.navigate("guide") },
          onReport = { nav.navigate("report") },
          onBackup = { nav.navigate("backup") },
          onKeyCard = { nav.navigate("nfc_card") },
        )
      }
      composable("behavior") { BehaviorScreen(vm, state, onBack = { nav.popBackStack() }, onKeyCard = { nav.navigate("nfc_card") }) }
      composable("design") { DesignScreen(vm, state, onBack = { nav.popBackStack() }) }
      composable("vehicles") { VehiclesScreen(vm, state, onBack = { nav.popBackStack() }) }
      composable("menu") { MenuConfigScreen(vm, state, onBack = { nav.popBackStack() }, onOpen = { nav.navigate("menu/${it.route}") }) }
      composable("menu/{section}") { entry ->
        val section = MenuSection.entries.firstOrNull { it.route == entry.arguments?.getString("section") }
        MenuConfigScreen(vm, state, onBack = { nav.popBackStack() }, section = section)
      }
      composable("wear") {
        WearConfigScreen(
          vm, state,
          onBack = { nav.popBackStack() },
          onPairWatch = { nav.navigate("wear_pair") },
          onWatchKeyCard = { nav.navigate("wear_nfc_card") },
        )
      }
      composable("wear_nfc_card") {
        WearKeyCardScreen(onBack = { nav.popBackStack() }, onReport = { nav.navigate("report") })
      }
      composable("wear_pair") {
        WearPairScreen(vm, state, onBack = { nav.popBackStack() }, onReport = { nav.navigate("report") })
      }
      composable("report") { ReportScreen(onBack = { nav.popBackStack() }) }
      composable("backup") { BackupScreen(vm, onBack = { nav.popBackStack() }) }
      composable("car3d") { Car3DScreen(vm, state, onBack = { nav.popBackStack() }) }
      composable("find_car") { CarFinderScreen(state, onBack = { nav.popBackStack() }) }
      composable("nfc_card") {
        KeyCardScreen(vm, state, onBack = { nav.popBackStack() }, onReport = { nav.navigate("report") })
      }
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
            keyCard = { nav.navigate("nfc_card") },
          ),
        )
      }
    }
  }
}
