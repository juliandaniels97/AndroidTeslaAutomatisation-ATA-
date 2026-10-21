package org.tfv.ata.ui

import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.tfv.ata.widget.CarFinderWidget
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.tfv.ata.core.WearContract
import org.tfv.ata.nfc.KeyCardRegistration
import org.tfv.ata.ui.components.ActivationOverlay
import org.tfv.ata.ui.components.OnboardingSheet
import org.tfv.ata.ui.components.StartupLogo
import org.tfv.ata.ui.theme.AtaTheme
import org.tfv.ata.ui.theme.SystemBarsForTheme

class MainActivity : FragmentActivity() {
  /** Screen to open from a deep link (e.g. the watch opening its pairing assistant); null once shown. */
  private val pendingRoute = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) pendingRoute.value = routeFor(intent)
    enableEdgeToEdge()
    setContent {
      val vm: AtaViewModel = viewModel()
      val state by vm.state.collectAsStateWithLifecycle()
      AtaTheme(
        themeMode = state.settings.theme,
        dynamicColor = state.settings.dynamicColor,
      ) {
        SystemBarsForTheme()
        // Survives rotation, so the start animation only plays once per launch.
        var splashDone by rememberSaveable { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
          AtaNavHost(vm, state, pendingRoute.value, onRouteShown = { pendingRoute.value = null })
          ActivationOverlay(vm)
          if (!splashDone) {
            StartupLogo(onDone = { splashDone = true })
          } else if (!state.settings.onboarded) {
            OnboardingSheet(onDismiss = { vm.markOnboarded() })
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    routeFor(intent)?.let { pendingRoute.value = it }
  }

  private fun routeFor(intent: Intent?): String? = when (intent?.data?.toString()) {
    WearContract.DEEP_LINK_WEAR_PAIR -> "wear_pair"
    DEEP_LINK_FIND_CAR -> "find_car"
    else -> null
  }

  companion object {
    /** Opened by the "find my car" widget (explicit intent, not exported as a link). */
    const val DEEP_LINK_FIND_CAR = "ata://find-car"
  }

  override fun onResume() {
    super.onResume()
    // The "find my car" widget shows the distance from the phone's position: refresh it when the app opens.
    lifecycleScope.launch { runCatching { CarFinderWidget().updateAll(this@MainActivity) } }
    KeyCardRegistration.setForeground(this, resumed = true)
  }

  override fun onPause() {
    KeyCardRegistration.setForeground(this, resumed = false)
    super.onPause()
  }
}
