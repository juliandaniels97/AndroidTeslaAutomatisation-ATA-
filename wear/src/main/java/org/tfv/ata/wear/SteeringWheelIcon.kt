package org.tfv.ata.wear

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Steering wheel (rim, hub and three spokes) in the style of the filled Material icons. */
val SteeringWheelIcon: ImageVector by lazy {
  ImageVector.Builder("SteeringWheel", 24.dp, 24.dp, 24f, 24f).apply {
    // Rim.
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
      moveTo(2f, 12f)
      arcToRelative(10f, 10f, 0f, true, true, 20f, 0f)
      arcToRelative(10f, 10f, 0f, true, true, -20f, 0f)
      close()
      moveTo(4.2f, 12f)
      arcToRelative(7.8f, 7.8f, 0f, true, true, 15.6f, 0f)
      arcToRelative(7.8f, 7.8f, 0f, true, true, -15.6f, 0f)
      close()
    }
    // Side spokes.
    path(fill = SolidColor(Color.Black)) {
      moveTo(4f, 10.6f)
      lineTo(20f, 10.6f)
      lineTo(20f, 12.8f)
      lineTo(4f, 12.8f)
      close()
    }
    // Lower spoke.
    path(fill = SolidColor(Color.Black)) {
      moveTo(10.9f, 12f)
      lineTo(13.1f, 12f)
      lineTo(13.1f, 20f)
      lineTo(10.9f, 20f)
      close()
    }
    // Hub.
    path(fill = SolidColor(Color.Black)) {
      moveTo(8.6f, 12f)
      arcToRelative(3.4f, 3.4f, 0f, true, true, 6.8f, 0f)
      arcToRelative(3.4f, 3.4f, 0f, true, true, -6.8f, 0f)
      close()
    }
  }.build()
}
