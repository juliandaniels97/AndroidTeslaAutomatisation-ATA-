package org.tfv.ata.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val REVEAL_MS = 620
private const val FADE_MS = 220

@Composable
fun StartupLogo(onDone: () -> Unit) {
  val p = remember { Animatable(0f) }
  val boxAlpha = remember { Animatable(1f) }

  LaunchedEffect(Unit) {
    p.animateTo(1f, tween(REVEAL_MS, easing = FastOutSlowInEasing))
    boxAlpha.animateTo(0f, tween(FADE_MS))
    onDone()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .graphicsLayer { alpha = boxAlpha.value }
      .background(
        androidx.compose.ui.graphics.Brush.radialGradient(
          colors = listOf(Color(0xFF13203A), Color(0xFF070B14)),
        ),
      )
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { onDone() },
      ),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      val grow = 0.90f + 0.10f * p.value
      Canvas(
        modifier = Modifier
          .size(240.dp)
          .graphicsLayer { scaleX = grow; scaleY = grow },
      ) {
        val unit = size.minDimension / 108f
        scale(unit, unit, pivot = Offset.Zero) {
          val prog = p.value
          val sweepA = ((prog) / 0.30f).coerceIn(0f, 1f)
          val boltA = ((prog - 0.22f) / 0.34f).coerceIn(0f, 1f)
          val aLeft = ((prog - 0.30f) / 0.32f).coerceIn(0f, 1f)
          val tMid = ((prog - 0.42f) / 0.32f).coerceIn(0f, 1f)
          val aRight = ((prog - 0.54f) / 0.32f).coerceIn(0f, 1f)

          val cyan = Color(0xFF5FB8F2)
          val white = Color(0xFFEAF2FF)
          val red = Color(0xFFFF4034)

          // headlight sweeps
          line(Offset(26f, 28f), Offset(50f, 38f), cyan.copy(alpha = sweepA), 4f)
          line(Offset(82f, 28f), Offset(58f, 38f), cyan.copy(alpha = sweepA), 4f)

          // bolt
          val bolt = Path().apply {
            moveTo(56f, 30f); lineTo(48f, 40f); lineTo(53f, 40f); lineTo(50f, 47f)
            lineTo(60f, 36f); lineTo(55f, 36f); lineTo(58f, 30f); close()
          }
          drawPath(bolt, red.copy(alpha = boltA))

          // A  T  A
          glyphA(26f, white.copy(alpha = aLeft))
          // T
          line(Offset(44f, 48f), Offset(64f, 48f), white.copy(alpha = tMid), 7.5f)
          line(Offset(54f, 48f), Offset(54f, 82f), white.copy(alpha = tMid), 7.5f)
          glyphA(70f, white.copy(alpha = aRight))
        }
      }

      Spacer(Modifier.height(14.dp))
      val subA = ((p.value - 0.55f) / 0.45f).coerceIn(0f, 1f)
      Text(
        text = "ANDROID TESLA AUTOMATISATION",
        color = Color(0xFF8FD0FF).copy(alpha = subA),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.graphicsLayer { alpha = subA },
      )
    }
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.line(
  start: Offset,
  end: Offset,
  color: Color,
  width: Float,
) = drawLine(color, start, end, strokeWidth = width, cap = StrokeCap.Round)

/** Draws a capital A whose left leg bottom sits at x. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.glyphA(x: Float, color: Color) {
  line(Offset(x - 4f, 82f), Offset(x + 6f, 48f), color, 7.5f)
  line(Offset(x + 6f, 48f), Offset(x + 16f, 82f), color, 7.5f)
  line(Offset(x, 66f), Offset(x + 12f, 66f), color, 7.5f)
}
