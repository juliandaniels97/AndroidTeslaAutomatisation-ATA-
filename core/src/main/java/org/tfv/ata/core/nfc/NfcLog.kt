package org.tfv.ata.core.nfc

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Log of what the car's card reader asked the NFC key card. Kept in a file because the card service also runs
 * while ATA isn't open; the assistant and the bug report show it.
 */
object NfcLog {
  private const val FILE = "nfc_log.txt"
  private const val MAX_LINES = 200
  private val _lines = MutableStateFlow<List<String>?>(null)
  private val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
  private val writer = java.util.concurrent.Executors.newSingleThreadExecutor()

  /** Unix millis of the last reader contact (SELECT), 0 if none since the app started. */
  private val _lastContact = MutableStateFlow(0L)
  val lastContact: StateFlow<Long> = _lastContact.asStateFlow()

  fun lines(context: Context): StateFlow<List<String>?> {
    load(context)
    return _lines.asStateFlow()
  }

  @Synchronized
  fun add(context: Context, message: String) {
    load(context)
    val next = (_lines.value.orEmpty() + "${time.format(Date())}  $message").takeLast(MAX_LINES)
    _lines.value = next
    // The card reader waits for answers: write the file off the calling thread.
    val text = next.joinToString("\n")
    val target = file(context)
    writer.execute { runCatching { target.writeText(text) } }
  }

  fun contact() {
    _lastContact.value = System.currentTimeMillis()
  }

  @Synchronized
  fun clear(context: Context) {
    _lines.value = emptyList()
    runCatching { file(context).delete() }
  }

  fun text(context: Context): String = lines(context).value.orEmpty().joinToString("\n")

  @Synchronized
  private fun load(context: Context) {
    if (_lines.value != null) return
    _lines.value = runCatching { file(context).readLines().filter { it.isNotBlank() } }.getOrDefault(emptyList())
  }

  private fun file(context: Context) = File(context.applicationContext.filesDir, FILE)
}
