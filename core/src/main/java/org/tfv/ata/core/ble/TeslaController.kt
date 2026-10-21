package org.tfv.ata.core.ble

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.CommandResult
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.core.Vin
import java.security.SecureRandom

object TeslaController {
  private const val SCAN_TIMEOUT = 15_000L
  private const val CONNECT_TIMEOUT = 20_000L
  private const val REPLY_TIMEOUT = 10_000L
  private val random = SecureRandom()
  private val mutex = Mutex()

  suspend fun sendCommand(context: Context, vin: String, action: AtaAction, value: Int? = null): CommandResult =
    mutex.withLock {
      val command = Commands.forAction(action, value) ?: return CommandResult.NotImplemented
      val connection = open(context, vin) ?: return CommandResult.Failure("vehicle not reachable")
      val routing = ByteArray(16).also { random.nextBytes(it) }
      try {
        if (command.domain == Domain.INFOTAINMENT) runCatching { ensureAwake(connection, vin) }
        val session = handshake(connection, vin, command.domain, routing)
          ?: return CommandResult.Failure("handshake failed")
        val reply = sendAndAwait(connection, session.signedCommand(command.payload, routing), routing)
          ?: return CommandResult.Failure("no response")
        if (reply.fault != 0) return CommandResult.Failure("vehicle fault ${reply.fault}")
        session.decryptResponse(reply)
        CommandResult.Success
      } catch (e: Exception) {
        CommandResult.Failure(e.message ?: "error")
      } finally {
        connection.close()
      }
    }

  suspend fun readStatus(context: Context, vin: String): VehicleSnapshot? =
    mutex.withLock {
      val connection = open(context, vin) ?: return null
      val routing = ByteArray(16).also { random.nextBytes(it) }
      try {
        runCatching { ensureAwake(connection, vin) }
        val session = handshake(connection, vin, Domain.INFOTAINMENT, routing) ?: return null
        val command = Commands.vehicleData()
        val reply = sendAndAwait(connection, session.signedCommand(command.payload, routing), routing)
          ?: return null
        if (reply.fault != 0) return null
        val bytes = session.decryptResponse(reply) ?: reply.protobufBytes ?: return null
        StatusParser.parse(bytes)
      } catch (e: Exception) {
        null
      } finally {
        connection.close()
      }
    }

  suspend fun pairKey(context: Context, vin: String, owner: Boolean = false): CommandResult =
    mutex.withLock {
      val connection = open(context, vin) ?: return CommandResult.Failure("vehicle not reachable")
      try {
        if (connection.send(addKeyEnvelope(owner))) CommandResult.Success
        else CommandResult.Failure("send failed")
      } catch (e: Exception) {
        CommandResult.Failure(e.message ?: "error")
      } finally {
        connection.close()
      }
    }

  private suspend fun open(context: Context, vin: String): BleConnection? {
    val address = BleTransport.scanAddress(context, Vin.bleLocalName(vin), SCAN_TIMEOUT) ?: return null
    return BleTransport.connect(context, address, CONNECT_TIMEOUT)
  }

  private suspend fun ensureAwake(connection: BleConnection, vin: String) {
    val routing = ByteArray(16).also { random.nextBytes(it) }
    val session = handshake(connection, vin, Domain.VEHICLE_SECURITY, routing) ?: return
    val wake = Commands.forAction(AtaAction.WAKE, null) ?: return
    sendAndAwait(connection, session.signedCommand(wake.payload, routing), routing)
    delay(1500)
  }

  private suspend fun handshake(connection: BleConnection, vin: String, domain: Int, routing: ByteArray): Session? {
    val uuid = ByteArray(16).also { random.nextBytes(it) }
    val publicKey = VehicleKey.publicKeyBytes()
    if (!connection.send(sessionInfoRequest(domain, publicKey, uuid, routing))) return null
    val vinBytes = Vin.normalize(vin).toByteArray(Charsets.US_ASCII)
    val deadline = System.currentTimeMillis() + REPLY_TIMEOUT
    while (System.currentTimeMillis() < deadline) {
      val reply = receive(connection) ?: return null
      val parsed = ParsedResponse(reply)
      val info = parsed.sessionInfo ?: continue
      val tag = parsed.sessionInfoTag ?: continue
      val session = Session.fromHandshake(domain, vinBytes, VehicleKey.privateKey(), publicKey, uuid, info, tag)
      if (session != null) return session
    }
    return null
  }

  private suspend fun sendAndAwait(connection: BleConnection, request: ByteArray, routing: ByteArray): ParsedResponse? {
    if (!connection.send(request)) return null
    val deadline = System.currentTimeMillis() + REPLY_TIMEOUT
    while (System.currentTimeMillis() < deadline) {
      val reply = receive(connection) ?: return null
      val parsed = ParsedResponse(reply)
      if (parsed.sessionInfo != null && parsed.responseNonce == null && parsed.fault == 0) continue
      val addr = parsed.toRoutingAddress
      if (addr == null || addr.contentEquals(routing)) return parsed
    }
    return null
  }

  private suspend fun receive(connection: BleConnection): ByteArray? =
    withTimeoutOrNull(REPLY_TIMEOUT) {
      try {
        connection.inbox.receive()
      } catch (e: Exception) {
        null
      }
    }

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
