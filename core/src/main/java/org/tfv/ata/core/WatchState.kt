package org.tfv.ata.core

/**
 * What the watch reports to the phone's watch-pairing assistant: its setup, the progress of a pairing run
 * or connection test, and its Bluetooth log. Sent as "key=value" lines, followed by a blank line and the log.
 */
data class WatchState(
  val model: String = "",
  val sdk: Int = 0,
  val appVersion: String = "",
  /** Keystore ECDH for the watch's own key (Wear OS 4 / API 31+). */
  val supported: Boolean = false,
  val permission: Boolean = false,
  val bluetoothOn: Boolean = false,
  val locationPermission: Boolean = false,
  val locationOn: Boolean = false,
  val vin: String = "",
  val directMode: Boolean = false,
  /** The watch's own key works with the car (a session was established). */
  val paired: Boolean = false,
  /** "pair" or "probe" while one runs, else empty. */
  val running: String = "",
  /** PairStep name while pairing. */
  val step: String = "",
  /** PairOutcome name of the last pairing run. */
  val outcome: String = "",
  /** FailureCode name of the last failure. */
  val code: String = "",
  /** Result of the last connection test: "ok", "fail" or empty. */
  val probe: String = "",
  val log: String = "",
  val nfc: Boolean = false,
  val nfcOn: Boolean = false,
  val cardEmulation: Boolean = false,
  /** The watch's NFC key card is turned on. */
  val keyCard: Boolean = false,
  /** Seconds since the last card reader contact, -1 if none. */
  val nfcContactAgeSec: Long = -1,
  val nfcLog: String = "",
) {
  fun encode(): String = buildString {
    fun kv(k: String, v: Any) = append(k).append('=').append(v.toString().replace('\n', ' ')).append('\n')
    kv("model", model)
    kv("sdk", sdk)
    kv("app", appVersion)
    kv("supported", supported)
    kv("permission", permission)
    kv("bluetooth", bluetoothOn)
    kv("locperm", locationPermission)
    kv("locon", locationOn)
    kv("vin", vin)
    kv("direct", directMode)
    kv("paired", paired)
    kv("running", running)
    kv("step", step)
    kv("outcome", outcome)
    kv("code", code)
    kv("probe", probe)
    kv("nfc", nfc)
    kv("nfcon", nfcOn)
    kv("hce", cardEmulation)
    kv("keycard", keyCard)
    kv("nfcage", nfcContactAgeSec)
    append('\n')
    append(log)
    append(NFC_LOG_MARKER)
    append(nfcLog)
  }

  companion object {
    private const val NFC_LOG_MARKER = "\n--- nfc ---\n"

    fun decode(text: String): WatchState {
      val head = text.substringBefore("\n\n")
      val logs = if (text.contains("\n\n")) text.substringAfter("\n\n") else ""
      val log = logs.substringBefore(NFC_LOG_MARKER)
      val nfcLog = if (logs.contains(NFC_LOG_MARKER)) logs.substringAfter(NFC_LOG_MARKER) else ""
      val map = head.lines().filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
      fun b(k: String) = map[k] == "true"
      return WatchState(
        model = map["model"].orEmpty(),
        sdk = map["sdk"]?.toIntOrNull() ?: 0,
        appVersion = map["app"].orEmpty(),
        supported = b("supported"),
        permission = b("permission"),
        bluetoothOn = b("bluetooth"),
        locationPermission = b("locperm"),
        locationOn = b("locon"),
        vin = map["vin"].orEmpty(),
        directMode = b("direct"),
        paired = b("paired"),
        running = map["running"].orEmpty(),
        step = map["step"].orEmpty(),
        outcome = map["outcome"].orEmpty(),
        code = map["code"].orEmpty(),
        probe = map["probe"].orEmpty(),
        log = log,
        nfc = b("nfc"),
        nfcOn = b("nfcon"),
        cardEmulation = b("hce"),
        keyCard = b("keycard"),
        nfcContactAgeSec = map["nfcage"]?.toLongOrNull() ?: -1,
        nfcLog = nfcLog,
      )
    }
  }
}
