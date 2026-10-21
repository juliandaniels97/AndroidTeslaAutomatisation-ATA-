package org.tfv.ata.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionPolicyTest {
  private val ble = AtaSettings(experimentalBle = true)
  private val allow = PolicyDecision.Allow
  private val confirm = PolicyDecision.Confirm

  private fun decide(action: AtaAction, source: TriggerSource, locked: Boolean = false, settings: AtaSettings = ble) =
    ActionPolicy.decide(action, source, locked, settings)

  @Test
  fun basicActionsAreAlwaysAllowedWhileActive() {
    for (source in TriggerSource.entries) {
      assertEquals(allow, decide(AtaAction.NAVIGATE_HOME, source, locked = true, settings = AtaSettings()))
    }
  }

  @Test
  fun passiveModeBlocksAutomationButNotTheApp() {
    val passive = ble.copy(active = false)
    assertEquals(PolicyDecision.Deny(DenyReason.INACTIVE), decide(AtaAction.CLIMATE_ON, TriggerSource.TRUSTED, settings = passive))
    assertEquals(PolicyDecision.Deny(DenyReason.INACTIVE), decide(AtaAction.NAVIGATE_HOME, TriggerSource.TRUSTED, settings = passive))
    assertEquals(allow, decide(AtaAction.CLIMATE_ON, TriggerSource.IN_APP, settings = passive))
  }

  @Test
  fun bleNeedsTheExperimentalSwitch() {
    assertEquals(
      PolicyDecision.Deny(DenyReason.BLE_DISABLED),
      decide(AtaAction.LOCK, TriggerSource.TRUSTED, settings = AtaSettings()),
    )
  }

  @Test
  fun routinesRunNonCriticalActionsEvenWhenLocked() {
    assertEquals(allow, decide(AtaAction.CLIMATE_ON, TriggerSource.TRUSTED, locked = true))
    assertEquals(allow, decide(AtaAction.LOCK, TriggerSource.TRUSTED, locked = true))
    assertEquals(
      PolicyDecision.Deny(DenyReason.PHONE_LOCKED),
      decide(AtaAction.CLIMATE_ON, TriggerSource.TRUSTED, locked = true, settings = ble.copy(automationWhenLocked = false)),
    )
  }

  @Test
  fun routinesConfirmCriticalActionsByDefault() {
    for (action in ActionPolicy.critical) {
      assertEquals(confirm, decide(action, TriggerSource.TRUSTED))
      assertEquals(confirm, decide(action, TriggerSource.TRUSTED, locked = true))
    }
  }

  @Test
  fun criticalOptInRunsDirectlyOnlyWhenUnlockedUnlessExtended() {
    val optIn = ble.copy(criticalDirectIds = listOf(AtaAction.UNLOCK.id))
    assertEquals(allow, decide(AtaAction.UNLOCK, TriggerSource.TRUSTED, settings = optIn))
    assertEquals(confirm, decide(AtaAction.UNLOCK, TriggerSource.TRUSTED, locked = true, settings = optIn))
    assertEquals(confirm, decide(AtaAction.TRUNK, TriggerSource.TRUSTED, settings = optIn))
    val locked = optIn.copy(criticalDirectWhenLocked = true)
    assertEquals(allow, decide(AtaAction.UNLOCK, TriggerSource.TRUSTED, locked = true, settings = locked))
  }

  @Test
  fun nfcTagsNeverOpenTheCarDirectly() {
    val optIn = ble.copy(criticalDirectIds = ActionPolicy.critical.map { it.id }, criticalDirectWhenLocked = true)
    assertEquals(confirm, decide(AtaAction.UNLOCK, TriggerSource.NFC, settings = optIn))
    assertEquals(allow, decide(AtaAction.CLIMATE_ON, TriggerSource.NFC))
  }

  @Test
  fun externalCallersAreDeniedUnlessAllowListed() {
    val denied = PolicyDecision.Deny(DenyReason.NOT_ALLOWED_EXTERNAL)
    assertEquals(denied, decide(AtaAction.CLIMATE_ON, TriggerSource.EXTERNAL))
    assertEquals(denied, decide(AtaAction.UNLOCK, TriggerSource.EXTERNAL))
    val listed = ble.copy(externalActionIds = listOf(AtaAction.CLIMATE_ON.id, AtaAction.UNLOCK.id))
    assertEquals(allow, decide(AtaAction.CLIMATE_ON, TriggerSource.EXTERNAL, settings = listed))
    assertEquals(PolicyDecision.Deny(DenyReason.PHONE_LOCKED), decide(AtaAction.CLIMATE_ON, TriggerSource.EXTERNAL, locked = true, settings = listed))
    assertEquals(denied, decide(AtaAction.UNLOCK, TriggerSource.EXTERNAL, settings = listed))
  }

  @Test
  fun watchMayRunCriticalActionsItConfirmed() {
    assertEquals(allow, decide(AtaAction.UNLOCK, TriggerSource.WATCH, locked = true))
    assertEquals(allow, decide(AtaAction.CLIMATE_ON, TriggerSource.WATCH, locked = true))
  }

  @Test
  fun valuesAreRangeChecked() {
    assertEquals(80, ActionPolicy.parseValue(AtaAction.CHARGE_LIMIT, "80"))
    assertEquals(null, ActionPolicy.parseValue(AtaAction.CHARGE_LIMIT, "120"))
    assertEquals(null, ActionPolicy.parseValue(AtaAction.CHARGE_LIMIT, "abc"))
    assertEquals(22, ActionPolicy.parseValue(AtaAction.SET_TEMPERATURE, "21,6"))
    assertEquals(16, ActionPolicy.parseValue(AtaAction.CHARGING_AMPS, " 16 "))
  }
}
