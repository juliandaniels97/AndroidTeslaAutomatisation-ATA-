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
        val value = if (action.needsValue) 50 else null
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
}
