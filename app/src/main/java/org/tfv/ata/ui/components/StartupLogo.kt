package org.tfv.ata.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.tfv.ata.R

private const val ZOOM_MS = 1000
private const val HOLD_MS = 150
private const val FADE_MS = 280
private const val ZOOM_FROM = 1.0f
private const val ZOOM_TO = 1.08f
private const val LOGO_ASPECT = 940f / 1672f

/** Navy of the logo artwork's edges, also used as the system splash background. */
val SplashNavy = Color(0xFF01040F)

/** Start animation: the original portrait logo fills the screen and slowly zooms in for one second. */
@Composable
fun StartupLogo(onDone: () -> Unit) {
  val zoom = remember { Animatable(ZOOM_FROM) }
  val alpha = remember { Animatable(1f) }

  LaunchedEffect(Unit) {
    coroutineScope {
      async { zoom.animateTo(ZOOM_TO, tween(ZOOM_MS, easing = FastOutSlowInEasing)) }
      delay((ZOOM_MS + HOLD_MS).toLong())
      alpha.animateTo(0f, tween(FADE_MS, easing = FastOutSlowInEasing))
    }
    onDone()
  }

  BoxWithConstraints(
    Modifier
      .fillMaxSize()
      .graphicsLayer { this.alpha = alpha.value }
      .background(SplashNavy)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDone,
      ),
  ) {
    // Portrait screens: crop so the artwork always fills the screen. Wider screens (tablet, landscape):
    // fill the height; the artwork's navy edges blend into the background.
    val portrait = maxWidth / maxHeight <= LOGO_ASPECT * 1.25f
    Image(
      painter = painterResource(R.drawable.ata_splash),
      contentDescription = null,
      contentScale = if (portrait) ContentScale.Crop else ContentScale.FillHeight,
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          scaleX = zoom.value
          scaleY = zoom.value
        },
    )
  }
}
