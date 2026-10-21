package org.tfv.ata.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import org.tfv.ata.core.VehicleSnapshot
import kotlin.math.min

@Composable
fun VehicleArt(snapshot: VehicleSnapshot?, modifier: Modifier = Modifier) {
  val battery = snapshot?.batteryPercent ?: 0
  val charging = snapshot?.charging == true
  val climate = snapshot?.climateOn == true
  val lockedKnown = snapshot != null
  val locked = snapshot?.locked == true

  val transition = rememberInfiniteTransition(label = "vehicle")
  val pulse by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "pulse",
  )
  val sweep by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
    label = "sweep",
  )
  val animatedBattery by animateFloatAsState(
    targetValue = battery.coerceIn(0, 100) / 100f,
    animationSpec = tween(900, easing = FastOutSlowInEasing),
    label = "battery",
  )
  val ringColor by animateColorAsState(
    targetValue = when {
      charging -> Color(0xFF2ECC71)
      battery <= 15 -> Color(0xFFE74C3C)
      else -> Color(0xFF3FA7FF)
    },
    animationSpec = tween(600),
    label = "ring",
  )

  Box(modifier = modifier.fillMaxWidth().height(260.dp).padding(16.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
      val w = size.width
      val h = size.height
      val cx = w / 2f
      val cy = h / 2f
      val radius = min(w, h) / 2f - 14f

      drawCircle(
        color = ringColor.copy(alpha = 0.12f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 16f),
      )
      drawArc(
        color = ringColor,
        startAngle = -90f,
        sweepAngle = 360f * animatedBattery,
        useCenter = false,
        topLeft = Offset(cx - radius, cy - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 16f, cap = StrokeCap.Round),
      )

      if (charging) {
        drawArc(
          color = Color(0xFF2ECC71).copy(alpha = 0.5f),
          startAngle = sweep,
          sweepAngle = 40f,
          useCenter = false,
          topLeft = Offset(cx - radius, cy - radius),
          size = Size(radius * 2, radius * 2),
          style = Stroke(width = 16f, cap = StrokeCap.Round),
        )
      }

      val bodyColor = if (climate) {
        lerp(Color(0xFF4A5560), Color(0xFF3FA7FF), pulse * 0.6f)
      } else {
        Color(0xFF53606D)
      }
      val carW = radius * 0.9f
      val carH = radius * 1.5f
      drawRoundRect(
        color = bodyColor,
        topLeft = Offset(cx - carW / 2f, cy - carH / 2f),
        size = Size(carW, carH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(carW * 0.4f, carW * 0.4f),
      )
      drawRoundRect(
        color = Color(0xFF2B333B),
        topLeft = Offset(cx - carW * 0.32f, cy - carH * 0.22f),
        size = Size(carW * 0.64f, carH * 0.4f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(carW * 0.2f, carW * 0.2f),
      )

      val glow = when {
        !lockedKnown -> Color(0xFF8A929E)
        locked -> Color(0xFF2ECC71)
        else -> Color(0xFFF1C40F)
      }
      drawCircle(
        color = glow.copy(alpha = 0.25f + 0.35f * pulse),
        radius = carW * 0.14f,
        center = Offset(cx, cy + carH * 0.34f),
      )
    }
  }
}
