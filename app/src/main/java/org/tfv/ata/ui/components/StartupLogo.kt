package org.tfv.ata.ui.components

import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.tfv.ata.R

/** The animation itself is 1.5 s (animated WebP, played once); this is only a safety net. */
private const val MAX_ANIMATION_MS = 2_500L
private const val HOLD_MS = 120L
private const val FADE_MS = 250
private const val ANIMATION_ASPECT = 400f / 736f

/** Navy of the animation's edges, also used as the system splash background. */
val SplashNavy = Color(0xFF01040F)

/** Start animation: the ATA logo animation, full screen, played once (1.5 s), then fades into the app. */
@Composable
fun StartupLogo(onDone: () -> Unit) {
  val done by rememberUpdatedState(onDone)
  val ended = remember { CompletableDeferred<Unit>() }
  val alpha = remember { Animatable(1f) }

  LaunchedEffect(Unit) {
    withTimeoutOrNull(MAX_ANIMATION_MS) { ended.await() }
    delay(HOLD_MS)
    alpha.animateTo(0f, tween(FADE_MS, easing = FastOutSlowInEasing))
    done()
  }

  BoxWithConstraints(
    Modifier
      .fillMaxSize()
      .graphicsLayer { this.alpha = alpha.value }
      .background(SplashNavy)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { done() },
      ),
  ) {
    // Portrait screens: crop so the animation always fills the screen. Wider screens (tablet, landscape):
    // show it whole; its navy edges blend into the background.
    val portrait = maxWidth / maxHeight <= ANIMATION_ASPECT * 1.25f
    AndroidView(
      factory = { context ->
        ImageView(context).apply {
          scaleType = if (portrait) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
          val drawable = runCatching {
            ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, R.drawable.ata_start))
          }.getOrNull()
          setImageDrawable(drawable)
          if (drawable is AnimatedImageDrawable) {
            drawable.repeatCount = 0
            drawable.registerAnimationCallback(
              object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(d: Drawable?) {
                  ended.complete(Unit)
                }
              },
            )
            drawable.start()
          } else {
            ended.complete(Unit)
          }
        }
      },
      modifier = Modifier.fillMaxSize(),
    )
  }
}
