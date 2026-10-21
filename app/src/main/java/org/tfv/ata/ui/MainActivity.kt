package org.tfv.ata.ui

import android.content.Intent
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

  private fun routeFor(intent: Intent?): String? =
    if (intent?.data?.toString() == WearContract.DEEP_LINK_WEAR_PAIR) "wear_pair" else null

  override fun onResume() {
    super.onResume()
    KeyCardRegistration.setForeground(this, resumed = true)
  }

  override fun onPause() {
    KeyCardRegistration.setForeground(this, resumed = false)
    super.onPause()
  }
}
