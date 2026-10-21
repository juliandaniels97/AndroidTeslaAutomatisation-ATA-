package org.tfv.ata.core.ble

import android.os.SystemClock
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Technical log of the latest Bluetooth session, shown in the pairing assistant to diagnose problems. */
object BleLog {
  private const val MAX_LINES = 300
  private var start = SystemClock.elapsedRealtime()
  private val _lines = MutableStateFlow<List<String>>(emptyList())
  val lines: StateFlow<List<String>> = _lines.asStateFlow()

  @Synchronized
  fun reset() {
    start = SystemClock.elapsedRealtime()
    _lines.value = emptyList()
  }

  @Synchronized
  fun add(message: String) {
    val t = (SystemClock.elapsedRealtime() - start) / 1000.0
    _lines.value = (_lines.value + String.format(Locale.US, "%6.1fs  %s", t, message)).takeLast(MAX_LINES)
  }

  fun text(): String = _lines.value.joinToString("\n")
}

internal fun ByteArray.hexPrefix(max: Int = 24): String =
  take(max).joinToString("") { "%02x".format(it) } + if (size > max) "…" else ""
