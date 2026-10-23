package org.tfv.ata.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsBackupTest {
  /** Tesla WMI, check digit matches. */
  private val cleanVin = "5YJ3E7EA2KF123456"

  /** Tesla WMI, check digit does not match (informational only). */
  private val badCheckVin = "5YJ3E7EB2KF123456"

  /** Valid format and check digit, not a Tesla WMI. */
  private val otherWmiVin = "1M8GDM9AXKP042788"

  @Test
  fun roundtripKeepsLayoutAndWhitelistedVehicles() {
    val source = AtaSettings(
      theme = ThemeMode.LIGHT,
      design = UiDesign.MATERIAL,
      dynamicColor = true,
      haptics = false,
      directionFx = true,
      car3d = false,
      tileColumns = 5,
      multiTiles = false,
      homeActionIds = listOf("m_lock", "lock", "navigate_home"),
      vehicleActionIds = listOf("m_climate", "climate_on"),
      launcherMenuActionIds = listOf("lock", "unlock"),
      routineActionIds = listOf("honk"),
      widgetActionIds = listOf("flash_lights", "wake"),
      widgetDesign = WidgetDesign.NEON,
      widgetTranslucent = true,
      wearActionIds = listOf("lock", "climate_off"),
      wearGrid = 7,
      wearComplicationIds = listOf("honk", "flash_lights", "wake", "lock"),
    )
    val vehicle = Vehicle(
      id = "id-1",
      name = "Mein Auto",
      vin = cleanVin,
      navHome = "HOME_ADDR",
      navWork = "WORK_ADDR",
      bodyStyle = "MODEL_S",
      paint = "ULTRA_RED",
      neonColor = "PINK",
    )
    val json = SettingsBackup.export(source, listOf(vehicle), vehicle.id, 0L, "1.9.7")
    assertTrue(json.contains("\"format\": \"ata-settings\""))
    assertTrue(json.contains("\"schema\": 1"))
    assertTrue(json.contains("\"app\": \"1.9.7\""))
    assertTrue(json.contains("1970-01-01T00:00:00Z"))
    assertTrue(json.contains("widgetActionIds"))
    assertTrue(json.contains("wearComplicationIds"))
    assertTrue(json.contains("vehicleActionIds"))
    assertFalse(json.contains("HOME_ADDR"))
    assertFalse(json.contains("WORK_ADDR"))
    assertFalse(json.contains("\"id\""))
    assertFalse(json.contains(DemoCar.vehicle.vin))

    val ready = ready(json.toByteArray(), sdkInt = 34) { "generated" }
    val out = ready.write.settings
    assertEquals(ThemeMode.LIGHT, out.theme)
    assertEquals(UiDesign.MATERIAL, out.design)
    assertEquals(true, out.dynamicColor)
    assertEquals(false, out.haptics)
    assertEquals(true, out.directionFx)
    assertEquals(false, out.car3d)
    assertEquals(5, out.tileColumns)
    assertEquals(false, out.multiTiles)
    assertEquals(source.homeActionIds, out.homeActionIds)
    assertEquals(source.vehicleActionIds, out.vehicleActionIds)
    assertEquals(source.launcherMenuActionIds, out.launcherMenuActionIds)
    assertEquals(source.routineActionIds, out.routineActionIds)
    assertEquals(source.widgetActionIds, out.widgetActionIds)
    assertEquals(WidgetDesign.NEON, out.widgetDesign)
    assertEquals(true, out.widgetTranslucent)
    assertEquals(source.wearActionIds, out.wearActionIds)
    assertEquals(7, out.wearGrid)
    assertEquals(source.wearComplicationIds, out.wearComplicationIds)
    assertEquals(5, ready.summary.tileCount)
    assertEquals(2, ready.summary.wearActionCount)

    assertEquals(1, ready.write.vehicles.size)
    val imported = ready.write.vehicles.single()
    assertEquals("generated", imported.id)
    assertEquals("Mein Auto", imported.name)
    assertEquals(cleanVin, imported.vin)
    assertEquals("", imported.navHome)
    assertEquals("", imported.navWork)
    assertEquals("MODEL_S", imported.bodyStyle)
    assertEquals("ULTRA_RED", imported.paint)
    assertEquals("PINK", imported.neonColor)
    assertEquals("generated", ready.write.selectedId)
    assertTrue(ready.summary.wmiWarnings.isEmpty())
    assertTrue(ready.summary.checkDigitWarnings.isEmpty())
    assertEquals(1, ready.summary.vehiclesNew)
  }

  @Test
  fun nonWhitelistFieldsStayUntouched() {
    val current = AtaSettings(
      haptics = true,
      criticalDirectIds = listOf("honk"),
      externalActionIds = listOf("lock"),
      biometricLock = false,
      active = true,
      presets = listOf("set_temperature|21"),
      homeAddress = "SECRET_ADDRESS_XYZ",
      workAddress = "SECRET_WORK",
      carLat = 1.25,
      carLng = 7.5,
      demoMode = true,
      onboarded = true,
      statusModeMigrated = true,
      vehicleTilesMerged = true,
      proximityUnlock = true,
    )
    val raw = """
      {
        "format": "ata-settings",
        "schema": 1,
        "trigger_token": "secret-token",
        "nfc_token": "secret-nfc",
        "layout": {
          "haptics": false,
          "criticalDirectIds": ["unlock"],
          "externalActionIds": ["wake"],
          "biometricLock": true,
          "active": false,
          "presets": ["set_temperature|19"],
          "homeAddress": "OTHER",
          "workAddress": "OTHER",
          "carLat": 9.0,
          "carLng": 9.0,
          "demoMode": false,
          "onboarded": false,
          "statusModeMigrated": false,
          "vehicleTilesMerged": false,
          "proximityUnlock": false
        }
      }
    """.trimIndent()
    val ready = ready(raw.toByteArray(), current)
    val out = ready.write.settings
    assertEquals(false, out.haptics)
    assertEquals(listOf("honk"), out.criticalDirectIds)
    assertEquals(listOf("lock"), out.externalActionIds)
    assertEquals(false, out.biometricLock)
    assertEquals(true, out.active)
    assertEquals(listOf("set_temperature|21"), out.presets)
    assertEquals("SECRET_ADDRESS_XYZ", out.homeAddress)
    assertEquals("SECRET_WORK", out.workAddress)
    assertEquals(1.25, out.carLat, 0.0)
    assertEquals(7.5, out.carLng, 0.0)
    assertEquals(true, out.demoMode)
    assertEquals(true, out.onboarded)
    assertEquals(true, out.statusModeMigrated)
    assertEquals(true, out.vehicleTilesMerged)
    assertEquals(true, out.proximityUnlock)
    assertEquals(false, ready.summary.criticalOverlap)
  }

  @Test
  fun exportOmitsPresetsTokensAndDemoVehicle() {
    val demo = DemoCar.vehicle.copy(navHome = "DEMO_SECRET")
    val real = Vehicle("real", "Real", cleanVin, navHome = "SECRET_ADDRESS_XYZ")
    val settings = AtaSettings(
      presets = listOf("set_temperature|21"),
      criticalDirectIds = listOf("SECRET_CRITICAL"),
      biometricLock = true,
    )
    val json = SettingsBackup.export(settings, listOf(demo, real), demo.id, 0L, "1.9.7")
    assertFalse(json.contains("set_temperature|21"))
    assertFalse(json.contains("presets"))
    assertFalse(json.contains("criticalDirectIds"))
    assertFalse(json.contains("biometricLock"))
    assertFalse(json.contains("trigger_token"))
    assertFalse(json.contains("DEMO_SECRET"))
    assertFalse(json.contains("SECRET_ADDRESS_XYZ"))
    assertFalse(json.contains(DemoCar.vehicle.vin))
    assertTrue(json.contains(cleanVin))
    assertFalse(json.contains("\"selectedVin\": \"$cleanVin\""))
  }

  @Test
  fun brokenAndForeignJsonChangeNothing() {
    val current = AtaSettings(haptics = false)
    val vehicles = listOf(Vehicle("a", "A", cleanVin))
    listOf("{", "", "   ", "[1,2]", "null", "{\"format\":\"ata-settings\",\"schema\":1,\"layout\":{\"haptics\":\"no\"}}").forEach { raw ->
      assertEquals(SettingsBackup.RejectReason.CORRUPT, rejected(raw.toByteArray(), current, vehicles))
    }
    assertEquals(SettingsBackup.RejectReason.NOT_ATA, rejected("""{"hello":1}""".toByteArray(), current, vehicles))
    assertEquals(
      SettingsBackup.RejectReason.NOT_ATA,
      rejected("""{"format":"not-ata","schema":2}""".toByteArray(), current, vehicles),
    )
  }

  @Test
  fun fileOver64KbIsRejectedAndExactLimitIsAccepted() {
    assertEquals(
      SettingsBackup.RejectReason.TOO_LARGE,
      rejected(ByteArray(SettingsBackup.MAX_BYTES + 1)),
    )
    val prefix = """{"format":"ata-settings","schema":1,"pad":""""
    val suffix = """"}"""
    val pad = SettingsBackup.MAX_BYTES - prefix.length - suffix.length
    val raw = prefix + "x".repeat(pad) + suffix
    assertEquals(SettingsBackup.MAX_BYTES, raw.toByteArray(Charsets.UTF_8).size)
    val current = AtaSettings(haptics = false, theme = ThemeMode.LIGHT)
    val ready = ready(raw.toByteArray(), current)
    assertEquals(false, ready.write.settings.haptics)
    assertEquals(ThemeMode.LIGHT, ready.write.settings.theme)
    assertEquals(
      SettingsBackup.RejectReason.TOO_LARGE,
      rejected((raw + " ").toByteArray()),
    )
  }

  @Test
  fun higherSchemaIsRejected() {
    val raw = """{"format":"ata-settings","schema":2,"layout":"broken","haptics":false}"""
    assertEquals(SettingsBackup.RejectReason.NEWER, rejected(raw.toByteArray(), AtaSettings(haptics = true)))
    assertEquals(SettingsBackup.RejectReason.CORRUPT, rejected("""{"format":"ata-settings","schema":0}""".toByteArray()))
    assertEquals(SettingsBackup.RejectReason.CORRUPT, rejected("""{"format":"ata-settings","schema":-1}""".toByteArray()))
  }

  @Test
  fun unknownActionIdsAreDroppedAndListsDeduped() {
    val raw = """
      {
        "format":"ata-settings","schema":1,
        "layout":{
          "homeActionIds":["lock","not_a_real_action","m_lock","lock","m_nope","m_seat_cool","m_seats"],
          "vehicleActionIds":["climate_on","made_up"],
          "widgetActionIds":["flash_lights","m_lock","nope"],
          "wearComplicationIds":["honk","not_real","wake"],
          "launcherMenuActionIds":["lock"]
        }
      }
    """.trimIndent()
    val ready = ready(raw.toByteArray())
    assertEquals(listOf("lock", "m_lock", "m_seats"), ready.write.settings.homeActionIds)
    assertEquals(listOf("climate_on"), ready.write.settings.vehicleActionIds)
    assertEquals(listOf("flash_lights"), ready.write.settings.widgetActionIds)
    assertEquals(listOf("honk", "wake"), ready.write.settings.wearComplicationIds)
  }

  @Test
  fun boundariesClampNumbersListsAndNames() {
    val columns = ready("""{"format":"ata-settings","schema":1,"layout":{"tileColumns":1}}""".toByteArray())
    assertEquals(2, columns.write.settings.tileColumns)
    assertEquals(0, ready(layout("tileColumns", "-3")).write.settings.tileColumns)
    assertEquals(0, ready(layout("tileColumns", "0")).write.settings.tileColumns)
    assertEquals(2, ready(layout("tileColumns", "2")).write.settings.tileColumns)
    assertEquals(6, ready(layout("tileColumns", "6")).write.settings.tileColumns)
    assertEquals(6, ready(layout("tileColumns", "9")).write.settings.tileColumns)

    val gridBase = AtaSettings(wearGrid = 7)
    assertEquals(4, ready(layout("wearGrid", "0"), gridBase).write.settings.wearGrid)
    assertEquals(4, ready(layout("wearGrid", "4"), gridBase).write.settings.wearGrid)
    assertEquals(4, ready(layout("wearGrid", "5"), gridBase).write.settings.wearGrid)
    assertEquals(7, ready(layout("wearGrid", "7"), gridBase).write.settings.wearGrid)
    assertEquals(7, ready(layout("wearGrid", "8"), gridBase).write.settings.wearGrid)

    val menu = (1..5).joinToString(",") { "\"${menuId(it)}\"" }
    val menuReady = ready("""{"format":"ata-settings","schema":1,"layout":{"launcherMenuActionIds":[$menu]}}""".toByteArray())
    assertEquals(listOf("lock", "unlock", "honk", "flash_lights"), menuReady.write.settings.launcherMenuActionIds)

    val slots = """["lock","unlock","honk","flash_lights","wake"]"""
    val slotReady = ready("""{"format":"ata-settings","schema":1,"layout":{"wearComplicationIds":$slots}}""".toByteArray())
    assertEquals(listOf("lock", "unlock", "honk", "flash_lights"), slotReady.write.settings.wearComplicationIds)

    val longName = "B".repeat(45)
    val named = ready(
      """{"format":"ata-settings","schema":1,"vehicles":[{"name":" $longName ","vin":"$cleanVin"}]}""".toByteArray(),
    ) { "new-1" }
    assertEquals(40, named.write.vehicles.single().name.length)
    assertEquals("B".repeat(40), named.write.vehicles.single().name)

    assertEquals(
      ThemeMode.DARK,
      ready(layout("theme", "\"BLUE\""), AtaSettings(theme = ThemeMode.LIGHT)).write.settings.theme,
    )
    assertEquals(
      "",
      ready(
        """{"format":"ata-settings","schema":1,"vehicles":[{"name":"N","vin":"$cleanVin","bodyStyle":"NOT_A_CAR","paint":"ULTRA_RED","neonColor":"NOPE"}]}""".toByteArray(),
      ) { "new-1" }.write.vehicles.single().bodyStyle,
    )
    assertEquals(
      "ULTRA_RED",
      ready(
        """{"format":"ata-settings","schema":1,"vehicles":[{"name":"N","vin":"$cleanVin","bodyStyle":"NOT_A_CAR","paint":"ULTRA_RED","neonColor":"NOPE"}]}""".toByteArray(),
      ) { "new-1" }.write.vehicles.single().paint,
    )
  }

  @Test
  fun vinLowercaseDuplicateAndInvalid() {
    val existing = Vehicle(
      id = "keep-me",
      name = "Old",
      vin = cleanVin,
      navHome = "Home street",
      navWork = "Work street",
      bodyStyle = "MODEL_3",
      paint = "PEARL_WHITE",
      neonColor = "BLUE",
    )
    val other = Vehicle(id = "other", name = "Other", vin = numberedVin(2), navHome = "Stay")
    val raw = """
      {
        "format":"ata-settings","schema":1,
        "selectedVin":"  ${cleanVin.lowercase()}  ",
        "vehicles":[
          {"name":"First","vin":"${cleanVin.lowercase()}","bodyStyle":"CYBERTRUCK","paint":"ULTRA_RED","neonColor":"RED","id":"evil","navHome":"SECRET_ADDRESS_XYZ"},
          {"name":"Second","vin":"${cleanVin.lowercase()}","bodyStyle":"MODEL_S"},
          {"name":"Bad","vin":"5YJ3E7EA2KF12345I"},
          {"name":"Short","vin":"5YJ3"}
        ]
      }
    """.trimIndent()
    val ready = ready(raw.toByteArray(), vehicles = listOf(existing, other), selectedId = "other") { "should-not-be-used" }
    assertEquals(2, ready.write.vehicles.size)
    val updated = ready.write.vehicles.first()
    assertEquals("keep-me", updated.id)
    assertEquals(cleanVin, updated.vin)
    assertEquals("First", updated.name)
    assertEquals("Home street", updated.navHome)
    assertEquals("Work street", updated.navWork)
    assertEquals("CYBERTRUCK", updated.bodyStyle)
    assertEquals("ULTRA_RED", updated.paint)
    assertEquals("RED", updated.neonColor)
    assertEquals(other, ready.write.vehicles[1])
    assertEquals("keep-me", ready.write.selectedId)
    assertEquals(1, ready.summary.vehiclesUpdated)
    assertEquals(0, ready.summary.vehiclesNew)
    assertTrue(ready.summary.drops.any { it.reason == SettingsBackup.DropReason.DUPLICATE && it.label.contains("Second") })
    assertTrue(ready.summary.drops.any { it.reason == SettingsBackup.DropReason.INVALID && it.label.contains("Bad") })
    assertTrue(ready.summary.drops.any { it.reason == SettingsBackup.DropReason.INVALID && it.label.contains("Short") })
    assertFalse(ready.write.vehicles.any { it.navHome == "SECRET_ADDRESS_XYZ" || it.id == "evil" })
  }

  @Test
  fun nothingIsDeletedAndEmptyVehicleListKeepsExisting() {
    val a = Vehicle("a", "A", cleanVin, navHome = "H")
    val b = Vehicle("b", "B", numberedVin(3), navWork = "W")
    val placeholder = Vehicle("ph", "Placeholder", "", navHome = "Place")
    val raw = """{"format":"ata-settings","schema":1,"vehicles":[{"name":"A2","vin":"$cleanVin"}]}"""
    val ready = ready(raw.toByteArray(), vehicles = listOf(a, placeholder, b), selectedId = "b")
    assertEquals(listOf("a", "ph", "b"), ready.write.vehicles.map { it.id })
    assertEquals("A2", ready.write.vehicles[0].name)
    assertEquals("H", ready.write.vehicles[0].navHome)
    assertEquals(placeholder, ready.write.vehicles[1])
    assertEquals(b, ready.write.vehicles[2])

    val empty = ready("""{"format":"ata-settings","schema":1,"vehicles":[]}""".toByteArray(), vehicles = listOf(a, b), selectedId = "a")
    assertEquals(listOf(a, b), empty.write.vehicles)
    assertEquals("a", empty.write.selectedId)
  }

  @Test
  fun selectionFallsBackWhenVinIsInvalidOrDropped() {
    val current = Vehicle("keep", "Keep", cleanVin)
    val invalid = """{"format":"ata-settings","schema":1,"selectedVin":"NOT_A_VIN","vehicles":[]}"""
    assertEquals("keep", ready(invalid.toByteArray(), vehicles = listOf(current), selectedId = "keep").write.selectedId)

    val full = (1..MAX).map { Vehicle("id$it", "N$it", numberedVin(it)) }
    val extra = numberedVin(50)
    val overflow = """{"format":"ata-settings","schema":1,"selectedVin":"$extra","vehicles":[{"name":"Extra","vin":"$extra"}]}"""
    val dropped = ready(overflow.toByteArray(), vehicles = full, selectedId = "id1") { "new-x" }
    assertEquals(MAX, dropped.write.vehicles.size)
    assertEquals("id1", dropped.write.selectedId)
    assertTrue(dropped.summary.drops.any { it.reason == SettingsBackup.DropReason.TOTAL_LIMIT })
    assertFalse(dropped.write.vehicles.any { it.vin == extra })
  }

  @Test
  fun selectionFollowsVinOfNewOrExistingVehicle() {
    val a = Vehicle("a", "A", cleanVin)
    val b = Vehicle("b", "B", numberedVin(4))
    val raw = """{"format":"ata-settings","schema":1,"selectedVin":"${numberedVin(4)}","vehicles":[]}"""
    assertEquals("b", ready(raw.toByteArray(), vehicles = listOf(a, b), selectedId = "a").write.selectedId)

    val created = numberedVin(8)
    val file = """{"format":"ata-settings","schema":1,"selectedVin":"$created","vehicles":[{"name":"Neu","vin":"$created"}]}"""
    val ready = ready(file.toByteArray(), vehicles = listOf(a), selectedId = "a") { "brand-new" }
    assertEquals("brand-new", ready.write.selectedId)
    assertEquals("brand-new", ready.write.vehicles.last().id)
    assertEquals(created, ready.write.vehicles.last().vin)
    assertEquals(a, ready.write.vehicles.first())
  }

  @Test
  fun fileAndTotalVehicleLimits() {
    val vins = (1..11).map { numberedVin(it) }
    val body = vins.joinToString(",") { """{"name":"N$it","vin":"$it"}""" }
    var n = 0
    val planned = ready("""{"format":"ata-settings","schema":1,"vehicles":[$body]}""".toByteArray()) { "id-${++n}" }
    assertEquals(10, planned.write.vehicles.size)
    assertEquals((1..10).map { numberedVin(it) }, planned.write.vehicles.map { it.vin })
    assertTrue(planned.summary.drops.any { it.reason == SettingsBackup.DropReason.FILE_LIMIT && it.label.contains(numberedVin(11)) })

    val existing = (1..9).map { Vehicle("e$it", "E$it", numberedVin(it)) }
    val more = (10..12).joinToString(",") { """{"name":"N$it","vin":"${numberedVin(it)}"}""" }
    var m = 0
    val capped = ready(
      """{"format":"ata-settings","schema":1,"vehicles":[$more]}""".toByteArray(),
      vehicles = existing,
    ) { "n-${++m}" }
    assertEquals(10, capped.write.vehicles.size)
    assertEquals(numberedVin(10), capped.write.vehicles.last().vin)
    assertEquals(1, capped.summary.vehiclesNew)
    assertEquals(2, capped.summary.drops.count { it.reason == SettingsBackup.DropReason.TOTAL_LIMIT })
  }

  @Test
  fun demoVehicleIsNeverChangedOrDuplicated() {
    val demo = Vehicle(DemoCar.VEHICLE_ID, "Demo Model Y", DemoCar.vehicle.vin, navHome = "KEEP")
    val raw = """{"format":"ata-settings","schema":1,"vehicles":[{"name":"Hacked","vin":"${DemoCar.vehicle.vin}","bodyStyle":"ROADSTER"}]}"""
    val ready = ready(raw.toByteArray(), vehicles = listOf(demo), selectedId = null) { "new-demo" }
    assertEquals(listOf(demo), ready.write.vehicles)
    assertEquals(null, ready.write.selectedId)
    assertTrue(ready.summary.drops.any { it.reason == SettingsBackup.DropReason.DEMO })
    assertEquals(0, ready.summary.vehiclesNew)
  }

  @Test
  fun wmiAndCheckDigitAreWarningsOnly() {
    val raw = """
      {"format":"ata-settings","schema":1,"vehicles":[
        {"name":"Other","vin":"$otherWmiVin"},
        {"name":"Check","vin":"$badCheckVin"}
      ]}
    """.trimIndent()
    var n = 0
    val ready = ready(raw.toByteArray()) { "w-${++n}" }
    assertEquals(2, ready.write.vehicles.size)
    assertEquals(listOf(otherWmiVin, badCheckVin), ready.write.vehicles.map { it.vin })
    assertTrue(ready.summary.wmiWarnings.any { it.contains("Other") })
    assertFalse(ready.summary.wmiWarnings.any { it.contains("Check") })
    assertTrue(ready.summary.checkDigitWarnings.any { it.contains("Check") })
    assertFalse(ready.summary.checkDigitWarnings.any { it.contains("Other") })
  }

  @Test
  fun dynamicColorIsForcedOffBelowAndroid12() {
    val raw = """{"format":"ata-settings","schema":1,"layout":{"dynamicColor":true}}"""
    assertEquals(false, ready(raw.toByteArray(), sdkInt = 30).write.settings.dynamicColor)
    assertEquals(true, ready(raw.toByteArray(), sdkInt = 31).write.settings.dynamicColor)
    assertEquals(true, ready(raw.toByteArray(), sdkInt = 34).write.settings.dynamicColor)
  }

  @Test
  fun criticalOverlapMentionsImportedTilesOnly() {
    val current = AtaSettings(criticalDirectIds = listOf("unlock"))
    val direct = """{"format":"ata-settings","schema":1,"layout":{"homeActionIds":["unlock"]}}"""
    assertTrue(ready(direct.toByteArray(), current).summary.criticalOverlap)
    val tile = """{"format":"ata-settings","schema":1,"layout":{"homeActionIds":["m_lock"]}}"""
    assertTrue(ready(tile.toByteArray(), current).summary.criticalOverlap)
    val other = """{"format":"ata-settings","schema":1,"layout":{"homeActionIds":["honk"]}}"""
    assertFalse(ready(other.toByteArray(), current).summary.criticalOverlap)
    val absent = """{"format":"ata-settings","schema":1,"layout":{"haptics":false}}"""
    assertFalse(ready(absent.toByteArray(), current.copy(homeActionIds = listOf("unlock"))).summary.criticalOverlap)
  }

  @Test
  fun missingWhitelistKeyKeepsCurrentValue() {
    val current = AtaSettings(haptics = false, theme = ThemeMode.LIGHT, tileColumns = 3)
    val ready = ready("""{"format":"ata-settings","schema":1,"layout":{"car3d":false}}""".toByteArray(), current)
    assertEquals(false, ready.write.settings.car3d)
    assertEquals(false, ready.write.settings.haptics)
    assertEquals(ThemeMode.LIGHT, ready.write.settings.theme)
    assertEquals(3, ready.write.settings.tileColumns)
  }

  private fun layout(key: String, value: String): ByteArray =
    """{"format":"ata-settings","schema":1,"layout":{"$key":$value}}""".toByteArray()

  private fun menuId(n: Int): String = listOf("lock", "unlock", "honk", "flash_lights", "wake")[n - 1]

  /** 5YJ + 14 digits. Valid charset, Tesla WMI, not necessarily a matching check digit. */
  private fun numberedVin(n: Int): String = "5YJ" + n.toString().padStart(14, '0')

  private fun ready(
    bytes: ByteArray,
    current: AtaSettings = AtaSettings(),
    vehicles: List<Vehicle> = emptyList(),
    selectedId: String? = null,
    sdkInt: Int = 34,
    newId: () -> String = { "new-id" },
  ): SettingsBackup.Outcome.Ready {
    val outcome = SettingsBackup.plan(bytes, current, vehicles, selectedId, sdkInt, newId)
    check(outcome is SettingsBackup.Outcome.Ready) { "expected Ready, was $outcome" }
    return outcome
  }

  private fun rejected(
    bytes: ByteArray,
    current: AtaSettings = AtaSettings(),
    vehicles: List<Vehicle> = emptyList(),
  ): SettingsBackup.RejectReason {
    val outcome = SettingsBackup.plan(bytes, current, vehicles, null, 34)
    check(outcome is SettingsBackup.Outcome.Rejected) { "expected Rejected, was $outcome" }
    return outcome.reason
  }

  private companion object {
    const val MAX = SettingsBackup.MAX_VEHICLES
  }
}
