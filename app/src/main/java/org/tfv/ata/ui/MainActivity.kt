package org.tfv.ata.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.tfv.ata.ui.components.ActivationOverlay
import org.tfv.ata.ui.components.OnboardingSheet
import org.tfv.ata.ui.components.StartupLogo
import org.tfv.ata.ui.theme.AtaTheme
import org.tfv.ata.ui.theme.SystemBarsForTheme

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val vm: AtaViewModel = viewModel()
      val state by vm.state.collectAsStateWithLifecycle()
      AtaTheme(
        themeMode = state.settings.theme,
        dynamicColor = state.settings.dynamicColor,
      ) {
        SystemBarsForTheme()
        var splashDone by remember { mutableStateOf(false) }
        Box(Modifier.fillMaxSize()) {
          AtaNavHost(vm, state)
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
}
