package org.tfv.ata.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled._3dRotation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.tfv.ata.R
import org.tfv.ata.core.BodyStyle
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.ui.components.car.CarCamera
import org.tfv.ata.ui.components.car.CarModel
import org.tfv.ata.ui.components.car.CarFxState
import org.tfv.ata.ui.components.car.CarStatus
import org.tfv.ata.ui.components.car.drawBatteryRing
import org.tfv.ata.ui.components.car.drawNeonCar
import org.tfv.ata.ui.components.car.drawFlatCar
import org.tfv.ata.ui.components.car.rememberCarLook
import org.tfv.ata.ui.components.car.rememberFrameClock
import org.tfv.ata.ui.theme.AtaColors
import kotlin.math.min
import kotlin.math.sin

/**
 * The car ([vehicle]'s body style and colour, neon or Material design) inside its battery ring. In 3D ([threeD]) it turns slowly, otherwise it is a static
 * side view. Shows doors, lids and effects after actions; a tap ([onOpen]) opens the full-screen view.
 */
@Composable
fun VehicleArt(
  snapshot: VehicleSnapshot?,
  modifier: Modifier = Modifier,
  fx: CarFxState = CarFxState(),
  vehicle: Vehicle? = null,
  threeD: Boolean = true,
  onOpen: (() -> Unit)? = null,
) {
  val shape = CarModel.shape(vehicle?.body ?: BodyStyle.MODEL_3)
  val look = rememberCarLook(vehicle, lineWidth = if (AtaColors.current.isDark) 2.4f else 3f)
  val known = snapshot != null && snapshot.updatedAt > 0
  val charging = snapshot?.charging == true
  val climate = snapshot?.climateOn == true
  val palette = AtaColors.current
  val now by rememberFrameClock()
  val status = CarStatus(charging = charging, climate = climate, sentry = snapshot?.sentry == true, battery = if (known) snapshot?.batteryPercent else null, seatHeat = listOfNotNull(snapshot?.seatHeatFrontLeft, snapshot?.seatHeatFrontRight, snapshot?.seatHeatRearLeft, snapshot?.seatHeatRearCenter, snapshot?.seatHeatRearRight).any { it > 0 })
  val badgePulse by rememberInfiniteTransition(label = "badge").animateFloat(
    initialValue = 1f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "badgePulse",
  )

  Box(
    modifier = modifier.fillMaxWidth().height(280.dp).padding(horizontal = 12.dp, vertical = 6.dp)
      .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = min(size.width, size.height) / 2f - 10f
      drawBatteryRing(
        center, radius, if (known) snapshot?.batteryPercent else null, charging, now, palette.isDark && !palette.material, palette.muted,
        material = palette.material, chargeKw = snapshot?.chargingPowerKw ?: 0, chargeLimit = snapshot?.chargeLimit,
      )
      // Gentle idle turn so the car reads as 3D; the full-screen view turns with the finger.
      val yaw = if (threeD) -35f + 14f * sin(now / 4000f) else CarCamera.SIDE_YAW
      val inner = radius * 2f * .92f
      val camera = if (threeD) CarCamera(yaw, 20f, inner, inner, zoom = 1.35f * shape.fit) else CarCamera.side(inner, inner, zoom = 1.3f * shape.fit)
      translate(center.x - inner / 2f, center.y - inner / 2f) {
        if (threeD) drawNeonCar(shape, camera, yaw, fx, status, now, look.style, androidx.compose.ui.geometry.Size(inner, inner))
        else drawFlatCar(shape, camera, fx, status, now, look.flat)
      }
    }
    if (onOpen != null) {
      Box(
        Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp).scale(badgePulse).size(36.dp)
          .clip(CircleShape).background(palette.accent.copy(alpha = .18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(if (threeD) Icons.Filled._3dRotation else Icons.Filled.OpenInFull, contentDescription = stringResource(R.string.car3d_open), tint = palette.accent, modifier = Modifier.size(20.dp))
      }
    }
    if (known) {
      Row(Modifier.align(Alignment.BottomCenter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        snapshot?.locked?.let { locked ->
          StateChip(if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen, if (locked) palette.success else palette.warning)
        }
        if (charging) {
          val kw = snapshot?.chargingPowerKw ?: 0
          val minutes = snapshot?.minutesToFull ?: 0
          val text = listOfNotNull(
            "$kw kW".takeIf { kw > 0 },
            stringResource(R.string.st_full_in_short, org.tfv.ata.ui.tiles.formatMinutes(minutes)).takeIf { minutes > 0 },
          ).joinToString(" · ").ifEmpty { null }
          StateChip(Icons.Filled.Bolt, palette.success, text, pulse = true)
        }
        if (climate) StateChip(Icons.Filled.AcUnit, palette.accent)
        if (snapshot?.sentry == true) StateChip(Icons.Filled.Shield, palette.critical)
      }
    }
  }
}

@Composable
private fun StateChip(icon: ImageVector, tint: Color, text: String? = null, pulse: Boolean = false) {
  val glow = if (pulse) {
    rememberInfiniteTransition(label = "chip").animateFloat(.6f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "chipGlow").value
  } else {
    1f
  }
  Row(
    Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = tint.copy(alpha = glow), modifier = Modifier.size(18.dp))
    if (text != null) {
      androidx.compose.material3.Text(
        text,
        color = tint,
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp),
      )
    }
  }
}
