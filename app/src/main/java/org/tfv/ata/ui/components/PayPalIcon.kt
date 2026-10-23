package org.tfv.ata.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** The PayPal mark (two overlapping slanted "P"), one colour, for tinting like the Material icons. */
val PayPalIcon: ImageVector by lazy {
  fun PathBuilder.letterP(dx: Float, dy: Float) {
    // Stem and bowl …
    moveTo(6f + dx, 21f + dy)
    lineTo(8.6f + dx, 3f + dy)
    lineTo(14.5f + dx, 3f + dy)
    curveTo(18.5f + dx, 3f + dy, 19.5f + dx, 6f + dy, 18.6f + dx, 8.5f + dy)
    curveTo(17.6f + dx, 11.5f + dy, 15f + dx, 12.5f + dy, 12.2f + dx, 12.5f + dy)
    lineTo(10.5f + dx, 12.5f + dy)
    lineTo(9.2f + dx, 21f + dy)
    close()
    // … with the opening of the bowl.
    moveTo(11.2f + dx, 5.5f + dy)
    lineTo(14f + dx, 5.5f + dy)
    curveTo(15.8f + dx, 5.5f + dy, 16.4f + dx, 6.6f + dy, 16f + dx, 8f + dy)
    curveTo(15.6f + dx, 9.4f + dy, 14.4f + dx, 10f + dy, 13f + dx, 10f + dy)
    lineTo(10.8f + dx, 10f + dy)
    close()
  }
  ImageVector.Builder("PayPal", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black), fillAlpha = .55f, pathFillType = PathFillType.EvenOdd) { letterP(1.6f, 1.2f) }
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) { letterP(-1.2f, -1f) }
  }.build()
}
