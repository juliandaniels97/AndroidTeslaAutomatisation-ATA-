package org.tfv.ata.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tfv.ata.ui.AtaViewModel
import org.tfv.ata.ui.theme.NeonBlue
import org.tfv.ata.ui.theme.NeonRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ActivationOverlay(vm: AtaViewModel) {
  val connecting by vm.connecting.collectAsStateWithLifecycle()
  val fx by vm.fx.collectAsStateWithLifecycle()

  if (connecting) {
    val transition = rememberInfiniteTransition(label = "connecting")
    val p by transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
      label = "pulse",
    )
    Canvas(Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height * 0.42f)
      for (i in 0..2) {
        val fr = (p + i / 3f) % 1f
        drawCircle(
          color = NeonBlue.copy(alpha = (1f - fr) * 0.45f),
          radius = fr * size.minDimension * 0.45f,
          center = center,
          style = Stroke(width = 4f),
        )
      }
    }
  }

  val event = fx
  if (event != null) {
    val progress = remember(event.id) { Animatable(0f) }
    LaunchedEffect(event.id) {
      progress.snapTo(0f)
      progress.animateTo(1f, tween(750))
    }
    val color = if (event.success) NeonBlue else NeonRed
    Canvas(Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height * 0.42f)
      val p = progress.value
      val fade = 1f - p
      for (i in 0..2) {
        drawCircle(
          color = color.copy(alpha = fade * 0.6f),
          radius = (p + i * 0.12f) * size.minDimension * 0.5f,
          center = center,
          style = Stroke(width = 6f * fade + 1f),
        )
      }
      event.bearing?.let { bearing ->
        val rad = Math.toRadians(bearing.toDouble())
        val len = p * size.minDimension * 0.5f
        val end = Offset(
          center.x + (sin(rad) * len).toFloat(),
          center.y - (cos(rad) * len).toFloat(),
        )
        drawLine(color.copy(alpha = fade), center, end, strokeWidth = 8f * fade + 2f)
      }
    }
  }
}
