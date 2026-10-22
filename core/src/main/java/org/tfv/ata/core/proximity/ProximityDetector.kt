package org.tfv.ata.core.proximity

import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Decides when walking up to the car should unlock it, from the signal strength (RSSI) of the car's Bluetooth
 * advertisement. Fires once per approach: only after the phone was away from the car (weak signal for a while,
 * or the car not seen at all), never right after starting next to the car, and not twice within [cooldownMs].
 */
class ProximityDetector(
  /** Smoothed RSSI at or above which the phone counts as near (see [rssiForMeters]). */
  private val nearRssi: Int,
  private val cooldownMs: Long = 2 * 60_000L,
  /** How long the signal must stay weak, or the car unseen, before the next approach can unlock. */
  private val awayAfterMs: Long = 45_000L,
) {
  enum class State { STARTING, AWAY, NEAR }

  var state: State = State.STARTING
    private set
  /** Smoothed RSSI, null while the car isn't seen. */
  var smoothed: Double? = null
    private set

  private var nearCount = 0
  private var weakSince: Long? = null
  private var lastSample = 0L
  private var lastTrigger = Long.MIN_VALUE / 2

  /** Feeds one RSSI reading; returns true when the car should be unlocked now. */
  fun onSample(rssi: Int, now: Long): Boolean {
    lastSample = now
    val ema = smoothed?.let { ALPHA * rssi + (1 - ALPHA) * it } ?: rssi.toDouble()
    smoothed = ema
    val near = ema >= nearRssi
    when (state) {
      // Started next to the car: wait until the phone has been away first.
      State.STARTING -> state = if (near) State.NEAR else State.AWAY
      State.AWAY -> {
        nearCount = if (near) nearCount + 1 else 0
        if (nearCount >= NEAR_SAMPLES) {
          state = State.NEAR
          nearCount = 0
          weakSince = null
          if (now - lastTrigger >= cooldownMs) {
            lastTrigger = now
            return true
          }
        }
      }
      State.NEAR -> {
        if (ema < nearRssi - HYSTERESIS_DB) {
          val since = weakSince ?: now.also { weakSince = it }
          if (now - since >= awayAfterMs) state = State.AWAY
        } else {
          weakSince = null
        }
      }
    }
    return false
  }

  /** Call regularly: a car that hasn't been seen for [awayAfterMs] is out of range. */
  fun onTick(now: Long) {
    if (state != State.STARTING && smoothed != null && now - lastSample >= awayAfterMs) {
      state = State.AWAY
      smoothed = null
      nearCount = 0
      weakSince = null
    }
  }

  companion object {
    private const val ALPHA = .35
    private const val NEAR_SAMPLES = 2
    private const val HYSTERESIS_DB = 8

    /** Rough RSSI of the car's advertisement at [meters] (log-distance model, -59 dBm at 1 m). */
    fun rssiForMeters(meters: Int): Int = (-59 - 20 * log10(meters.coerceAtLeast(1).toDouble())).roundToInt()
  }
}
