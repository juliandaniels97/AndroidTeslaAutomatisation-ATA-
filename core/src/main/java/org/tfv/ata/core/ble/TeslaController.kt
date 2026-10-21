package org.tfv.ata.core.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.FailureCode
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.core.Vin
import java.security.SecureRandom

enum class PairOutcome { ADDED, AWAITING_CONFIRMATION, FAILED }

/** Progress of [TeslaController.pairKey], for the pairing assistant. */
enum class PairStep { SEARCHING, CONNECTING, WAITING_FOR_CARD }

data class PairResult(val outcome: PairOutcome, val code: FailureCode? = null, val detail: String = "")

object TeslaController {
  private const val SCAN_TIMEOUT = 20_000L
  private const val CONNECT_TIMEOUT = 20_000L
  private const val REPLY_TIMEOUT = 10_000L
  private const val STATUS_TIMEOUT = 5_000L
  private const val PAIR_TIMEOUT = 60_000L
  private const val PAIR_POLL = 4_000L
  private const val MAX_BUSY_RETRIES = 3
  private const val WAKE_POLLS = 10

  /** Faults that carry fresh session info: re-handshake once and resend (protocol.md "Recovering"). */
  private val RESYNC_FAULTS = setOf(5, 6, 15, 17)

  private val random = SecureRandom()
  private val mutex = Mutex()

  private class BleException(
    val code: FailureCode,
    message: String,
    val resync: Boolean = false,
  ) : Exception(message)

  fun hasBlePermission(context: Context): Boolean =
    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT).all {
      ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

  suspend fun sendCommand(context: Context, vin: String, action: AtaAction, value: Int? = null): CommandResult =
    mutex.withLock {
      val command = Commands.forAction(action, value)
        ?: return@withLock if (action.needsValue) {
          CommandResult.Failure("missing or invalid value", FailureCode.INVALID_VALUE)
        } else {
          CommandResult.NotImplemented
        }
      withVehicle<CommandResult>(context, vin, onError = { code, msg -> CommandResult.Failure(msg, code) }) { conn ->
        val routing = newId()
        val infotainment = command.domain == Domain.INFOTAINMENT
        if (infotainment) ensureAwake(conn, vin, routing)
        val attempts = if (infotainment) 3 else 1
        var session = handshakeWithRetry(conn, vin, command.domain, routing, attempts)
        try {
          execute(conn, session, command.payload, routing, command.domain)
        } catch (e: BleException) {
          if (!e.resync) throw e
          session = handshakeWithRetry(conn, vin, command.domain, routing, 1)
          execute(conn, session, command.payload, routing, command.domain)
        }
        CommandResult.Success
      }
    }

  /**
   * Reads the vehicle status. Without [wake], a sleeping car is not woken: only VCSEC (locks) is asked and the
   * result has `asleep = true` (callers keep battery/range from the previous reading).
   */
  suspend fun readStatus(context: Context, vin: String, wake: Boolean = false): VehicleSnapshot? =
    mutex.withLock {
      withVehicle<VehicleSnapshot?>(context, vin, onError = { _, _ -> null }) { conn ->
        val routing = newId()
        val vcsec = vcsecStatus(conn, routing)
        if (!wake && vcsec?.asleep == true) {
          return@withVehicle VehicleSnapshot(locked = vcsec.locked, asleep = true, updatedAt = System.currentTimeMillis())
        }
        if (wake) ensureAwake(conn, vin, routing)
        val session = handshakeWithRetry(conn, vin, Domain.INFOTAINMENT, routing, if (wake) 3 else 1)
        val bytes = execute(conn, session, Commands.vehicleData().payload, routing, Domain.INFOTAINMENT)
          ?: return@withVehicle null
        val snapshot = StatusParser.parse(bytes) ?: return@withVehicle null
        if (vcsec != null) snapshot.copy(locked = vcsec.locked) else snapshot
      }
    }

  /**
   * Asks the car to add this device's key. The car then shows a prompt and the owner confirms by tapping an
   * already-paired key card on the card reader. While waiting, a VCSEC handshake is tried every few seconds:
   * it only succeeds once the key is on the car's key list, so [PairOutcome.ADDED] is never a guess.
   */
  suspend fun pairKey(
    context: Context,
    vin: String,
    owner: Boolean = false,
    onStep: (PairStep) -> Unit = {},
  ): PairResult =
    mutex.withLock {
      withVehicle<PairResult>(context, vin, onError = { code, msg -> PairResult(PairOutcome.FAILED, code, msg) }, onStep = onStep) { conn ->
        val routing = newId()
        // Same envelope as Tesla's vehicle-command: an unsigned RoutableMessage to the VCSEC domain.
        if (!conn.send(unsignedRequest(Domain.VEHICLE_SECURITY, addKeyEnvelope(owner), newId(), routing))) {
          return@withVehicle PairResult(PairOutcome.FAILED, FailureCode.NOT_REACHABLE, "send failed")
        }
        onStep(PairStep.WAITING_FOR_CARD)
        val deadline = System.currentTimeMillis() + PAIR_TIMEOUT
        while (System.currentTimeMillis() < deadline) {
          val pollUntil = minOf(deadline, System.currentTimeMillis() + PAIR_POLL)
          while (System.currentTimeMillis() < pollUntil) {
            val raw = receive(conn, pollUntil - System.currentTimeMillis()) ?: break
            val parsed = runCatching { ParsedResponse(raw) }.getOrNull()
            if (parsed != null && parsed.toRoutingAddress != null && !parsed.isFor(routing)) continue
            if (parsed != null && parsed.fault != 0 && parsed.fault != FAULT_UNKNOWN_KEY_ID) {
              return@withVehicle PairResult(PairOutcome.FAILED, FailureCode.VEHICLE_REJECTED, "vehicle fault ${parsed.fault}")
            }
            // Replies may come as a RoutableMessage or as a bare FromVCSECMessage.
            val payload = parsed?.protobufBytes ?: raw
            val reply = VcsecReply.forWhitelist(payload)
            if (reply is AppReply.Error) {
              return@withVehicle PairResult(PairOutcome.FAILED, FailureCode.VEHICLE_REJECTED, reply.detail)
            }
          }
          if (keyAccepted(conn, vin)) return@withVehicle PairResult(PairOutcome.ADDED)
        }
        PairResult(PairOutcome.AWAITING_CONFIRMATION)
      }
    }

  /** True once the car knows this key: a VCSEC session can be established. */
  private suspend fun keyAccepted(conn: BleConnection, vin: String): Boolean =
    try {
      handshake(conn, vin, Domain.VEHICLE_SECURITY, newId()) != null
    } catch (e: BleException) {
      if (e.code == FailureCode.KEY_NOT_PAIRED) false else throw e
    }

  private suspend fun <T> withVehicle(
    context: Context,
    vin: String,
    onError: (FailureCode, String) -> T,
    onStep: (PairStep) -> Unit = {},
    block: suspend (BleConnection) -> T,
  ): T {
    if (!VehicleKey.isSupported) return onError(FailureCode.UNSUPPORTED_DEVICE, "Android 12 or newer required")
    if (!hasBlePermission(context)) return onError(FailureCode.PERMISSION, "Bluetooth permission missing")
    if (!BleTransport.isBluetoothOn(context)) return onError(FailureCode.BLUETOOTH_OFF, "Bluetooth is off")
    var connection: BleConnection? = null
    return try {
      onStep(PairStep.SEARCHING)
      val device = BleTransport.scanDevice(context, Vin.bleLocalName(Vin.normalize(vin)), SCAN_TIMEOUT)
        ?: return onError(FailureCode.NOT_REACHABLE, "vehicle not found")
      onStep(PairStep.CONNECTING)
      val conn = BleTransport.connect(context, device, CONNECT_TIMEOUT)
        ?: return onError(FailureCode.NOT_REACHABLE, "connection failed")
      connection = conn
      block(conn)
    } catch (e: CancellationException) {
      throw e
    } catch (e: BleException) {
      onError(e.code, e.message ?: "error")
    } catch (e: SecurityException) {
      onError(FailureCode.PERMISSION, "Bluetooth permission missing")
    } catch (e: UnsupportedOperationException) {
      onError(FailureCode.UNSUPPORTED_DEVICE, e.message ?: "unsupported")
    } catch (e: Exception) {
      onError(FailureCode.OTHER, e.message ?: e.javaClass.simpleName)
    } finally {
      connection?.close()
    }
  }

  /** Wakes infotainment only if VCSEC reports it asleep, then waits until it is up. */
  private suspend fun ensureAwake(conn: BleConnection, vin: String, routing: ByteArray) {
    val status = vcsecStatus(conn, routing)
    if (status != null && !status.asleep) return
    val session = handshakeWithRetry(conn, vin, Domain.VEHICLE_SECURITY, routing, 1)
    val wake = Commands.forAction(AtaAction.WAKE, null) ?: return
    execute(conn, session, wake.payload, routing, Domain.VEHICLE_SECURITY)
    repeat(WAKE_POLLS) {
      delay(2_000)
      val now = vcsecStatus(conn, routing)
      if (now != null && !now.asleep) return
    }
  }

  /** Unauthenticated VCSEC GET_STATUS: locks + sleep state, works while the car sleeps and doesn't wake it. */
  private suspend fun vcsecStatus(conn: BleConnection, routing: ByteArray): VcsecStatus? {
    val request = unsignedRequest(Domain.VEHICLE_SECURITY, Commands.statusRequest().payload, newId(), routing)
    if (!conn.send(request)) return null
    val deadline = System.currentTimeMillis() + STATUS_TIMEOUT
    while (System.currentTimeMillis() < deadline) {
      val raw = receive(conn, deadline - System.currentTimeMillis()) ?: return null
      val parsed = ParsedResponse(raw)
      if (!parsed.isFor(routing)) continue
      VcsecReply.vehicleStatus(parsed.protobufBytes)?.let { return it }
    }
    return null
  }

  private suspend fun handshakeWithRetry(
    conn: BleConnection,
    vin: String,
    domain: Int,
    routing: ByteArray,
    attempts: Int,
  ): Session {
    repeat(attempts) { attempt ->
      handshake(conn, vin, domain, routing)?.let { return it }
      if (attempt < attempts - 1) delay(2_000)
    }
    throw BleException(FailureCode.NO_RESPONSE, "handshake timed out")
  }

  private suspend fun handshake(conn: BleConnection, vin: String, domain: Int, routing: ByteArray): Session? {
    val uuid = newId()
    val publicKey = VehicleKey.publicKeyBytes()
    if (!conn.send(sessionInfoRequest(domain, publicKey, uuid, routing))) {
      throw BleException(FailureCode.NOT_REACHABLE, "send failed")
    }
    val vinBytes = Vin.normalize(vin).toByteArray(Charsets.US_ASCII)
    val deadline = System.currentTimeMillis() + REPLY_TIMEOUT
    while (System.currentTimeMillis() < deadline) {
      val raw = receive(conn, deadline - System.currentTimeMillis()) ?: return null
      val parsed = ParsedResponse(raw)
      if (parsed.toRoutingAddress != null && !parsed.isFor(routing)) continue
      if (parsed.fault != 0) throw faultException(parsed.fault)
      val info = parsed.sessionInfo ?: continue
      if (SessionInfo(info).status == SESSION_STATUS_KEY_NOT_ON_WHITELIST) {
        throw BleException(FailureCode.KEY_NOT_PAIRED, "key not paired with this car")
      }
      val tag = parsed.sessionInfoTag ?: continue
      // A wrong tag means a reply to another request (or tampering): ignore it.
      Session.fromHandshake(domain, vinBytes, VehicleKey.privateKey(), publicKey, uuid, info, tag)?.let { return it }
    }
    return null
  }

  /**
   * Sends a signed command and reads replies until a terminal one. Returns the reply payload (plaintext),
   * throws [BleException] when the car rejects the command or doesn't answer.
   */
  private suspend fun execute(
    conn: BleConnection,
    session: Session,
    payload: ByteArray,
    routing: ByteArray,
    domain: Int,
  ): ByteArray? {
    for (attempt in 0 until MAX_BUSY_RETRIES) {
      if (!conn.send(session.signedCommand(payload, routing))) {
        throw BleException(FailureCode.NOT_REACHABLE, "send failed")
      }
      var busy = false
      val deadline = System.currentTimeMillis() + REPLY_TIMEOUT
      while (!busy && System.currentTimeMillis() < deadline) {
        val raw = receive(conn, deadline - System.currentTimeMillis())
          ?: throw BleException(FailureCode.NO_RESPONSE, "no response")
        val parsed = ParsedResponse(raw)
        if (!parsed.isFor(routing)) continue
        if (parsed.fault != 0) throw faultException(parsed.fault)
        if (parsed.operationStatus == OPERATION_STATUS_WAIT) {
          busy = true
          continue
        }
        val body = when (val reply = session.readReply(parsed)) {
          is ReplyPayload.Plain -> reply.bytes
          is ReplyPayload.Decrypted -> reply.bytes
          ReplyPayload.AuthFailed, ReplyPayload.Replayed -> continue
        }
        val app = if (domain == Domain.VEHICLE_SECURITY) VcsecReply.forCommand(body) else CarServerReply.parse(body)
        when (app) {
          AppReply.Done -> return body
          AppReply.KeepWaiting -> Unit
          AppReply.Busy -> busy = true
          is AppReply.Error -> throw BleException(FailureCode.VEHICLE_REJECTED, app.detail)
        }
      }
      if (!busy) throw BleException(FailureCode.NO_RESPONSE, "no response")
      delay(1_000L * (attempt + 1))
    }
    throw BleException(FailureCode.VEHICLE_REJECTED, "vehicle busy")
  }

  private fun faultException(fault: Int): BleException =
    if (fault == FAULT_UNKNOWN_KEY_ID) {
      BleException(FailureCode.KEY_NOT_PAIRED, "key not paired with this car")
    } else {
      BleException(FailureCode.VEHICLE_REJECTED, "vehicle fault $fault", resync = fault in RESYNC_FAULTS)
    }

  private suspend fun receive(conn: BleConnection, timeoutMs: Long): ByteArray? {
    if (timeoutMs <= 0) return null
    return withTimeoutOrNull(timeoutMs) { conn.inbox.receiveCatching().getOrNull() }
  }

  private fun newId(): ByteArray = ByteArray(16).also { random.nextBytes(it) }

  private fun ParsedResponse.isFor(routing: ByteArray): Boolean = toRoutingAddress?.contentEquals(routing) == true

  private fun addKeyEnvelope(owner: Boolean): ByteArray {
    val role = if (owner) 2 else 3
    val unsigned = buildProto {
      message(16, buildProto {
        message(5, buildProto {
          message(1, buildProto { bytes(1, VehicleKey.publicKeyBytes()) })
          enum(4, role)
        })
        message(6, buildProto { enum(1, 7) })
      })
    }
    return buildProto {
      message(1, buildProto {
        bytes(2, unsigned)
        enum(3, 2)
      })
    }
  }
}
