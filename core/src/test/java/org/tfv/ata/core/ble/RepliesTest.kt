package org.tfv.ata.core.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepliesTest {

  @Test
  fun vcsecEmptyReplyIsTerminal() {
    assertEquals(AppReply.Done, VcsecReply.forCommand(ByteArray(0)))
  }

  @Test
  fun vcsecWaitMeansBusy() {
    val wait = buildProto { message(4, buildProto { enum(1, 1) }) }
    assertEquals(AppReply.Busy, VcsecReply.forCommand(wait))
  }

  @Test
  fun vcsecNominalErrorFails() {
    val error = buildProto { message(46, buildProto { enum(1, 4) }) }
    assertTrue(VcsecReply.forCommand(error) is AppReply.Error)
  }

  @Test
  fun vcsecCommandStatusOkKeepsWaiting() {
    val ok = buildProto { message(4, buildProto { enum(1, 0) }) }
    assertEquals(AppReply.KeepWaiting, VcsecReply.forCommand(ok))
  }

  @Test
  fun whitelistWaitsForCardTapThenSucceeds() {
    val wait = buildProto { message(4, buildProto { enum(1, 1) }) }
    assertEquals(AppReply.KeepWaiting, VcsecReply.forWhitelist(wait))
    val done = buildProto { message(4, buildProto { message(3, buildProto { enum(1, 0); enum(3, 0) }) }) }
    assertEquals(AppReply.Done, VcsecReply.forWhitelist(done))
    val full = buildProto { message(4, buildProto { enum(1, 2); message(3, buildProto { enum(1, 4) }) }) }
    assertTrue(VcsecReply.forWhitelist(full) is AppReply.Error)
  }

  @Test
  fun vehicleStatusParses() {
    val status = buildProto { message(1, buildProto { enum(2, 1); enum(3, 2) }) }
    val parsed = VcsecReply.vehicleStatus(status)!!
    assertTrue(parsed.locked)
    assertTrue(parsed.asleep)
  }

  @Test
  fun carServerErrorCarriesReason() {
    val response = buildProto {
      message(1, buildProto {
        enum(1, 1)
        message(2, buildProto { string(1, "cable not connected") })
      })
    }
    assertEquals(AppReply.Error("cable not connected"), CarServerReply.parse(response))
    assertEquals(AppReply.Done, CarServerReply.parse(buildProto { message(1, buildProto { enum(1, 0) }) }))
  }

  @Test
  fun truncatedInputDoesNotThrow() {
    val valid = buildProto { bytes(10, ByteArray(40) { it.toByte() }); varint(52, 2) }
    for (cut in 0..valid.size) {
      ProtoReader(valid.copyOf(cut))
      ParsedResponse(valid.copyOf(cut))
    }
    // Length prefix far beyond the buffer.
    ProtoReader(byteArrayOf(0x52, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x0F))
    // Overlong varint.
    ProtoReader(ByteArray(20) { 0xFF.toByte() })
  }

  @Test
  fun statusParserReadsBatteryRangeAndUnknownLock() {
    val response = buildProto {
      message(2, buildProto {
        message(3, buildProto {
          varint(114, 80)
          float(111, 200f)
          message(1, buildProto { message(5, ByteArray(0)) })
        })
      })
    }
    val snapshot = StatusParser.parse(response)!!
    assertEquals(80, snapshot.batteryPercent)
    assertEquals(321, snapshot.rangeKm)
    assertTrue(snapshot.charging)
    assertEquals(null, snapshot.locked)
  }
}
