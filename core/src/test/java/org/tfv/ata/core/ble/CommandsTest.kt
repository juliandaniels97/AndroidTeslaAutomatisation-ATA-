package org.tfv.ata.core.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory

class CommandsTest {
  @Test
  fun everyEnabledBleActionHasACommand() {
    AtaAction.entries
      .filter { it.enabled && it.category == AtaCategory.FUNCTION_A }
      .forEach { action ->
        val value = if (action.needsValue) org.tfv.ata.core.ActionPolicy.valueRange(action)?.let { (it.first + it.last) / 2 } ?: 50 else null
        assertNotNull("no command for ${action.id}", Commands.forAction(action, value))
      }
  }

  @Test
  fun driverDoorOpensViaClosureMoveRequest() {
    val command = Commands.forAction(AtaAction.DOOR_DRIVER_OPEN, null)!!
    assertEquals(Domain.VEHICLE_SECURITY, command.domain)
    // UnsignedMessage.closureMoveRequest (4) → frontDriverDoor (1) = OPEN (3).
    assertEquals(3L, ProtoReader(command.payload).message(4)!!.varint(1))
  }

  @Test
  fun mediaNextIsVehicleAction19() {
    val command = Commands.forAction(AtaAction.MEDIA_NEXT, null)!!
    assertEquals(Domain.INFOTAINMENT, command.domain)
    assertNotNull(ProtoReader(command.payload).message(2)!!.bytes(19))
  }

  @Test
  fun seatHeaterCarriesLevelAndSeat() {
    val command = Commands.forAction(AtaAction.SEAT_HEATER_REAR_LEFT, 2)!!
    val action = ProtoReader(command.payload).message(2)!!.message(36)!!.message(1)!!
    // Level 2 = SEAT_HEATER_MED (field 4), rear left = field 9.
    assertNotNull(action.bytes(4))
    assertNotNull(action.bytes(9))
    assertEquals(null, Commands.forAction(AtaAction.SEAT_HEATER_FRONT_LEFT, 4))
  }

  @Test
  fun volumeIsAbsoluteFloatOrZigzagStep() {
    val set = ProtoReader(Commands.forAction(AtaAction.SET_VOLUME, 45)!!.payload).message(2)!!.message(16)!!
    assertEquals(4.5f, Float.fromBits(set.fixed32(3)!!), 0.001f)
    val up = ProtoReader(Commands.forAction(AtaAction.VOLUME_UP, null)!!.payload).message(2)!!.message(16)!!
    val down = ProtoReader(Commands.forAction(AtaAction.VOLUME_DOWN, null)!!.payload).message(2)!!.message(16)!!
    assertEquals(2L, up.varint(1))
    assertEquals(1L, down.varint(1))
  }

  @Test
  fun oneSideTemperatureKeepsTheOtherSide() {
    val temp = ProtoReader(Commands.forAction(AtaAction.SET_TEMPERATURE_DRIVER, 23, 20f to 19f)!!.payload)
      .message(2)!!.message(14)!!
    assertEquals(23f, Float.fromBits(temp.fixed32(6)!!), 0.001f)
    assertEquals(19f, Float.fromBits(temp.fixed32(7)!!), 0.001f)
  }

  @Test
  fun autoSeatClimateIsVehicleAction48() {
    val seat = ProtoReader(Commands.forAction(AtaAction.SEAT_AUTO_FRONT_RIGHT, 1)!!.payload).message(2)!!.message(48)!!.message(1)!!
    assertEquals(1L, seat.varint(1))
    assertEquals(2L, seat.varint(2))
    assertEquals(null, Commands.forAction(AtaAction.SEAT_AUTO_FRONT_LEFT, 2))
  }
}
