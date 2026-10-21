package org.tfv.ata.core.ble

object Domain {
  const val VEHICLE_SECURITY = 2
  const val INFOTAINMENT = 3
}

internal object Rm {
  const val TO_DESTINATION = 6
  const val FROM_DESTINATION = 7
  const val PROTOBUF_BYTES = 10
  const val SIGNED_MESSAGE_STATUS = 12
  const val SIGNATURE_DATA = 13
  const val SESSION_INFO_REQUEST = 14
  const val SESSION_INFO = 15
  const val REQUEST_UUID = 50
  const val UUID = 51
  const val FLAGS = 52
}

internal object DestF {
  const val DOMAIN = 1
  const val ROUTING_ADDRESS = 2
}

internal object SigF {
  const val SIGNER_IDENTITY = 1
  const val AES_GCM_PERSONALIZED = 5
  const val SESSION_INFO_TAG = 6
  const val AES_GCM_RESPONSE = 9
}

internal object GcmF {
  const val EPOCH = 1
  const val NONCE = 2
  const val COUNTER = 3
  const val EXPIRES_AT = 4
  const val TAG = 5
}

internal object RespF {
  const val NONCE = 1
  const val COUNTER = 2
  const val TAG = 3
}

internal object SiF {
  const val COUNTER = 1
  const val PUBLIC_KEY = 2
  const val EPOCH = 3
  const val CLOCK_TIME = 4
  const val STATUS = 5
}

internal object KeyIdF {
  const val PUBLIC_KEY = 1
}

internal object HmacTagF {
  const val TAG = 1
}

const val FLAG_ENCRYPT_RESPONSE_MASK = 1 shl 1

/** An unauthenticated request, e.g. VCSEC GET_STATUS (doesn't need a session and doesn't wake the car). */
internal fun unsignedRequest(domain: Int, payload: ByteArray, uuid: ByteArray, routingAddress: ByteArray): ByteArray =
  buildProto {
    message(Rm.TO_DESTINATION, buildProto { enum(DestF.DOMAIN, domain) })
    message(Rm.FROM_DESTINATION, buildProto { bytes(DestF.ROUTING_ADDRESS, routingAddress) })
    bytes(Rm.PROTOBUF_BYTES, payload)
    bytes(Rm.UUID, uuid)
  }

internal fun sessionInfoRequest(domain: Int, publicKey: ByteArray, uuid: ByteArray, routingAddress: ByteArray): ByteArray =
  buildProto {
    message(Rm.TO_DESTINATION, buildProto { enum(DestF.DOMAIN, domain) })
    message(Rm.FROM_DESTINATION, buildProto { bytes(DestF.ROUTING_ADDRESS, routingAddress) })
    message(Rm.SESSION_INFO_REQUEST, buildProto { bytes(1, publicKey) })
    bytes(Rm.UUID, uuid)
  }

internal class ParsedResponse(data: ByteArray) {
  private val reader = ProtoReader(data)
  val fromDomain: Int = reader.message(Rm.FROM_DESTINATION)?.varint(DestF.DOMAIN)?.toInt() ?: 0
  val toRoutingAddress: ByteArray? = reader.message(Rm.TO_DESTINATION)?.bytes(DestF.ROUTING_ADDRESS)
  val sessionInfo: ByteArray? = reader.bytes(Rm.SESSION_INFO)
  val protobufBytes: ByteArray? = reader.bytes(Rm.PROTOBUF_BYTES)
  val requestUuid: ByteArray? = reader.bytes(Rm.REQUEST_UUID)
  val flags: Int = reader.varint(Rm.FLAGS)?.toInt() ?: 0
  private val status = reader.message(Rm.SIGNED_MESSAGE_STATUS)
  val fault: Int = status?.varint(2)?.toInt() ?: 0
  /** UniversalMessage.OperationStatus_E: 0 OK, 1 WAIT, 2 ERROR. */
  val operationStatus: Int = status?.varint(1)?.toInt() ?: 0
  private val sig = reader.message(Rm.SIGNATURE_DATA)
  val sessionInfoTag: ByteArray? = sig?.message(SigF.SESSION_INFO_TAG)?.bytes(HmacTagF.TAG)
  private val gcmResponse = sig?.message(SigF.AES_GCM_RESPONSE)
  val responseNonce: ByteArray? = gcmResponse?.bytes(RespF.NONCE)
  val responseCounter: Int = gcmResponse?.varint(RespF.COUNTER)?.toInt() ?: 0
  val responseTag: ByteArray? = gcmResponse?.bytes(RespF.TAG)
}

internal class SessionInfo(data: ByteArray) {
  private val r = ProtoReader(data)
  val counter: Int = r.varint(SiF.COUNTER)?.toInt() ?: 0
  val publicKey: ByteArray = r.bytes(SiF.PUBLIC_KEY) ?: ByteArray(0)
  val epoch: ByteArray = r.bytes(SiF.EPOCH) ?: ByteArray(0)
  val clockTime: Int = r.fixed32(SiF.CLOCK_TIME) ?: 0
  /** Session_Info_Status: 0 OK, 1 KEY_NOT_ON_WHITELIST. */
  val status: Int = r.varint(SiF.STATUS)?.toInt() ?: 0
}

internal const val FAULT_UNKNOWN_KEY_ID = 3

/** UniversalMessage.MessageFault_E as readable text, e.g. "vehicle fault 10 (decoding error)". */
fun faultName(fault: Int): String {
  val name = when (fault) {
    0 -> "none"
    1 -> "busy"
    2 -> "timeout"
    3 -> "unknown key"
    4 -> "inactive key"
    5 -> "invalid signature"
    6 -> "invalid token or counter"
    7 -> "insufficient privileges"
    8 -> "invalid domains"
    9 -> "invalid command"
    10 -> "decoding error"
    11 -> "internal error"
    12 -> "wrong personalization"
    13 -> "bad parameter"
    14 -> "key list full"
    15 -> "incorrect epoch"
    16 -> "IV incorrect length"
    17 -> "time expired"
    18 -> "not provisioned with identity"
    19 -> "could not hash metadata"
    else -> "unknown"
  }
  return "vehicle fault $fault ($name)"
}

/**
 * Add-key request as Tesla's vehicle-command sends it over BLE: a bare ToVCSECMessage whose SignedMessage
 * (signature type PRESENT_KEY) carries an UnsignedMessage with the whitelist operation. The car then asks to
 * confirm with a key card. Role: 2 owner, 3 driver. Form factor 7: Android device.
 */
internal fun addKeyRequest(publicKey: ByteArray, owner: Boolean): ByteArray {
  val unsigned = buildProto {
    message(16, buildProto {
      message(5, buildProto {
        message(1, buildProto { bytes(1, publicKey) })
        enum(4, if (owner) 2 else 3)
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
internal const val SESSION_STATUS_KEY_NOT_ON_WHITELIST = 1
internal const val OPERATION_STATUS_WAIT = 1
