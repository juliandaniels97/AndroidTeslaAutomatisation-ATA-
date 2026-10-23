package org.tfv.ata.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tfv.ata.core.AtaSettings
import org.tfv.ata.core.SettingsBackup
import org.tfv.ata.core.Vehicle
import org.tfv.ata.core.VehicleSnapshot
import org.tfv.ata.data.AtaStore
import org.tfv.ata.shortcut.ShortcutSync
import org.tfv.ata.status.StatusPoller
import org.tfv.ata.wear.WearPush
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.AtaCategory
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tfv.ata.widget.ActionsWidget
import org.tfv.ata.widget.StatusWidget
import java.util.UUID

data class AtaUiState(
  val settings: AtaSettings = AtaSettings(),
  val vehicles: List<Vehicle> = emptyList(),
  val selected: Vehicle? = null,
  val snapshot: VehicleSnapshot? = null,
  /** Demo mode: the settings as stored ([settings] then shows Bluetooth as on). */
  val stored: AtaSettings? = null,
  /** False until the stored settings have been read (the start tab depends on them). */
  val loaded: Boolean = false,
)

data class ActivationFx(val id: Long, val success: Boolean, val bearing: Float?)

@OptIn(ExperimentalCoroutinesApi::class)
class AtaViewModel(app: Application) : AndroidViewModel(app) {
  private val store = AtaStore.get(app)

  private val selectedVehicle = combine(
    store.vehicles,
    store.selectedVehicleId,
  ) { vehicles, selectedId ->
    vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()
  }

  val state = combine(
    store.settings,
    store.vehicles,
    selectedVehicle,
    selectedVehicle.flatMapLatest { v -> if (v == null) flowOf(null) else store.snapshot(v.id) },
  ) { settings, vehicles, selected, snapshot ->
    AtaUiState(settings, vehicles, selected, snapshot, loaded = true)
  }.flatMapLatest { ui ->
    // Demo mode: a made-up car that is "connected" (no Bluetooth needed) with its own status.
    if (!ui.settings.demoMode) {
      flowOf(ui)
    } else {
      store.snapshot(org.tfv.ata.core.DemoCar.VEHICLE_ID).map { demo ->
        ui.copy(
          settings = ui.settings.copy(experimentalBle = true),
          stored = ui.settings,
          selected = org.tfv.ata.core.DemoCar.vehicle,
          snapshot = demo ?: org.tfv.ata.core.DemoCar.start(System.currentTimeMillis()),
        )
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AtaUiState())

  private val _refreshing = MutableStateFlow(false)
  val refreshing = _refreshing.asStateFlow()

  private val _connecting = MutableStateFlow(false)
  val connecting = _connecting.asStateFlow()
  private val _fx = MutableStateFlow<ActivationFx?>(null)
  val fx = _fx.asStateFlow()

  fun setConnecting(value: Boolean) { _connecting.value = value }
  fun playFx(success: Boolean = true, bearing: Float? = null) {
    _fx.value = ActivationFx(System.nanoTime(), success, bearing)
  }
  private val _carFx = MutableStateFlow(org.tfv.ata.ui.components.car.CarFxState())
  /** Doors, lids, lights … the car drawing animates after an action reached the car. */
  val carFx = _carFx.asStateFlow()

  fun onCarAction(action: AtaAction) {
    _carFx.value = _carFx.value.with(action, System.currentTimeMillis())
  }

  /** Puts the drawing back as it was (after the animation demo). */
  fun restoreCarFx(fx: org.tfv.ata.ui.components.car.CarFxState) {
    _carFx.value = fx
  }

  fun markOnboarded() = viewModelScope.launch { store.updateSettings { it.copy(onboarded = true) } }

  fun recordCarLocation() = viewModelScope.launch {
    val loc = org.tfv.ata.geo.Geo.lastLocation(getApplication()) ?: return@launch
    store.updateSettings {
      it.copy(carLat = loc.latitude, carLng = loc.longitude, carLocAt = System.currentTimeMillis())
    }
  }

  init {
    viewModelScope.launch {
      // 1.9.4: the status interval became three modes; an interval set before stays an interval.
      store.updateSettings { st ->
        if (st.statusModeMigrated) st
        else if (st.pollIntervalMinutes > 0) st.copy(
          statusMode = org.tfv.ata.core.StatusMode.INTERVAL,
          intervalMinutes = st.pollIntervalMinutes.coerceIn(5, 180),
          statusModeMigrated = true,
        )
        else st.copy(statusModeMigrated = true)
      }
      // 1.9.2: the actions tab became "Favourites"; its tiles move to the end of the vehicle tab.
      store.updateSettings { st ->
        if (st.vehicleTilesMerged) st
        else if (st.vehicleActionIds.isEmpty()) st.copy(vehicleTilesMerged = true)
        else st.copy(
          // New in 1.9.2 (seats, ventilation, volume) first, then the former actions tab at the end.
          vehicleActionIds = (
            st.vehicleActionIds +
              org.tfv.ata.ui.tiles.TileCatalog.newIn192 +
              org.tfv.ata.ui.tiles.TileCatalog.basicDefaults.map { it.id }
            ).distinct(),
          vehicleTilesMerged = true,
        )
      }
      val s = store.settings.first()
      StatusPoller.sync(getApplication(), s)
      org.tfv.ata.proximity.ProximityService.sync(getApplication(), s, currentVehicle())
    }
    pushWearConfig()
  }

  private fun effectiveWearActions(s: AtaSettings): List<AtaAction> {
    val resolved = s.wearActionIds.mapNotNull { AtaAction.fromId(it) }.filter { it.enabled && !it.needsValue }
    return resolved.ifEmpty {
      AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_B && it.enabled && !it.beta && !it.needsValue }
    }
  }

  private suspend fun currentVehicle(): Vehicle? {
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    return vehicles.firstOrNull { it.id == selectedId } ?: vehicles.firstOrNull()
  }

  /** Push the watch action list plus the selected vehicle's VIN/name, so the watch needs no VIN entry. */
  private fun pushWearConfig(actions: List<AtaAction>? = null) = viewModelScope.launch {
    val settings = store.settings.first()
    val resolved = actions ?: effectiveWearActions(settings)
    val v = currentVehicle()
    val complications = settings.wearComplicationIds.map { id -> AtaAction.fromId(id)?.takeIf { it.enabled && !it.needsValue } ?: AtaAction.LOCK }
    WearPush.pushConfig(getApplication(), resolved, v?.vin ?: "", v?.name ?: "", settings.wearGrid, complications)
  }

  fun updateSettings(transform: (AtaSettings) -> AtaSettings) = viewModelScope.launch {
    val before = store.settings.first()
    store.updateSettings(transform)
    val after = store.settings.first()
    if (before.wearGrid != after.wearGrid || before.wearComplicationIds != after.wearComplicationIds) pushWearConfig()
    org.tfv.ata.proximity.ProximityService.sync(getApplication(), after, currentVehicle())
    ShortcutSync.refresh(getApplication())
    StatusPoller.sync(getApplication(), store.settings.first())
    runCatching {
      ActionsWidget().updateAll(getApplication())
      StatusWidget().updateAll(getApplication())
      org.tfv.ata.widget.CarFinderWidget().updateAll(getApplication())
    }
  }

  /** Reads the car status over BLE; [onDone] gets false if it couldn't be read (details in the BLE log). */
  fun refreshStatus(wake: Boolean = true, onDone: (Boolean) -> Unit = {}) = viewModelScope.launch {
    if (_refreshing.value) return@launch
    _refreshing.value = true
    val snap = runCatching {
      kotlinx.coroutines.withContext(Dispatchers.IO) { StatusPoller.pollNow(getApplication(), wake = wake) }
    }.getOrNull()
    if (snap != null) runCatching { StatusWidget().updateAll(getApplication()) }
    _refreshing.value = false
    onDone(snap != null)
  }

  /** Renews the automation token and re-issues shortcuts / widgets so ATA's own triggers keep working. */
  fun rotateTriggerToken(onDone: () -> Unit = {}) = viewModelScope.launch {
    store.rotateTriggerToken()
    ShortcutSync.refresh(getApplication())
    runCatching { ActionsWidget().updateAll(getApplication()) }
    onDone()
  }

  /** Invalidates every NFC tag written so far. */
  fun rotateNfcToken() = viewModelScope.launch { store.rotateNfcToken() }

  private val _pairState = MutableStateFlow<PairUiState>(PairUiState.Idle)
  /** Progress of the pairing assistant. */
  val pairState = _pairState.asStateFlow()

  fun pairKey(vin: String) = viewModelScope.launch {
    if (_pairState.value is PairUiState.Running || _probing.value) return@launch
    _pairState.value = PairUiState.Running(org.tfv.ata.core.ble.PairStep.SEARCHING)
    var reached: org.tfv.ata.core.ble.PairStep? = null
    val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
      org.tfv.ata.core.ble.TeslaController.pairKey(getApplication(), vin) { step ->
        reached = step
        _pairState.value = PairUiState.Running(step)
      }
    }
    _pairState.value = PairUiState.Finished(result, reached)
  }

  private val _probe = MutableStateFlow<org.tfv.ata.core.ble.ProbeResult?>(null)
  private val _probing = MutableStateFlow(false)
  /** Result of the last connection test (null: none yet). */
  val probe = _probe.asStateFlow()
  val probing = _probing.asStateFlow()

  fun probeVehicle(vin: String) = viewModelScope.launch {
    if (_probing.value || _pairState.value is PairUiState.Running) return@launch
    _probing.value = true
    _probe.value = kotlinx.coroutines.withContext(Dispatchers.IO) {
      org.tfv.ata.core.ble.TeslaController.probe(getApplication(), vin)
    }
    _probing.value = false
  }

  fun resetPairing() {
    if (_pairState.value !is PairUiState.Running) _pairState.value = PairUiState.Idle
  }

  fun saveVehicle(vehicle: Vehicle) = viewModelScope.launch {
    val current = store.vehicles.first()
    val merged = current.filter { it.id != vehicle.id } + vehicle
    store.saveVehicles(merged)
    if (current.none { it.id == vehicle.id }) store.selectVehicle(vehicle.id)
    pushWearConfig()
    org.tfv.ata.proximity.ProximityService.sync(getApplication(), store.settings.first(), currentVehicle())
  }

  fun addVehicle(name: String, vin: String, home: String, work: String, bodyStyle: String = "") =
    saveVehicle(Vehicle(UUID.randomUUID().toString(), name, vin, home, work, bodyStyle))

  fun deleteVehicle(id: String) = viewModelScope.launch {
    val remaining = store.vehicles.first().filter { it.id != id }
    store.saveVehicles(remaining)
    store.selectVehicle(remaining.firstOrNull()?.id)
    pushWearConfig()
  }

  fun selectVehicle(id: String) = viewModelScope.launch {
    store.selectVehicle(id)
    pushWearConfig()
  }

  /** Sends menu and selected car to the watch again (e.g. from the watch-pairing assistant). */
  fun resyncWear() {
    pushWearConfig()
  }

  fun setWearActions(actions: List<AtaAction>) {
    updateSettings { it.copy(wearActionIds = actions.map { a -> a.id }) }
    pushWearConfig(actions.filter { !it.needsValue }.ifEmpty {
      AtaAction.entries.filter { it.category == AtaCategory.FUNCTION_B && it.enabled && !it.beta && !it.needsValue }
    })
  }

  /** Stores the favourites' tile order (action and multi-tile ids). */
  fun setFavoriteTiles(keys: List<String>) = updateSettings { it.copy(homeActionIds = keys) }

  /** Stores the vehicle tab's tile order (action and multi-tile ids). */
  fun setVehicleTiles(keys: List<String>) = updateSettings { it.copy(vehicleActionIds = keys) }

  fun toggleFavoriteTile(key: String) = updateSettings { s ->
    val current = s.homeActionIds.ifEmpty { org.tfv.ata.ui.tiles.TileCatalog.favoriteDefaults }
    s.copy(homeActionIds = org.tfv.ata.ui.tiles.TileCatalog.toggle(current, key))
  }

  /** Turns the demo mode on (always from the same start: 50 %, charging) or off. */
  fun setDemoMode(on: Boolean) = viewModelScope.launch {
    if (on) store.saveSnapshot(org.tfv.ata.core.DemoCar.VEHICLE_ID, org.tfv.ata.core.DemoCar.start(System.currentTimeMillis()))
    store.updateSettings { it.copy(demoMode = on) }
    runCatching { StatusWidget().updateAll(getApplication()) }
  }

  fun rememberTab(route: String) = updateSettings { if (it.lastTab == route) it else it.copy(lastTab = route) }

  suspend fun exportBackup(appVersion: String): String {
    val settings = store.settings.first()
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    return SettingsBackup.export(settings, vehicles, selectedId, System.currentTimeMillis(), appVersion)
  }

  suspend fun previewBackup(bytes: ByteArray, sdkInt: Int): SettingsBackup.Outcome {
    val settings = store.settings.first()
    val vehicles = store.vehicles.first()
    val selectedId = store.selectedVehicleId.first()
    return SettingsBackup.plan(bytes, settings, vehicles, selectedId, sdkInt)
  }

  /** Validates again inside the single store edit, then refreshes shortcuts, widgets and the watch. */
  fun commitBackup(bytes: ByteArray, sdkInt: Int, onDone: (BackupCommit) -> Unit) = viewModelScope.launch {
    val wrote = try {
      store.importLayout { settings, vehicles, selectedId ->
        when (val outcome = runCatching {
          SettingsBackup.plan(bytes, settings, vehicles, selectedId, sdkInt)
        }.getOrNull()) {
          is SettingsBackup.Outcome.Ready -> outcome.write
          else -> null
        }
      }
    } catch (_: Throwable) {
      false
    }
    if (!wrote) {
      onDone(BackupCommit.REJECTED)
      return@launch
    }
    var sideFailed = false
    try {
      ShortcutSync.refresh(getApplication())
    } catch (_: Throwable) {
      sideFailed = true
    }
    try {
      ActionsWidget().updateAll(getApplication())
      StatusWidget().updateAll(getApplication())
      org.tfv.ata.widget.CarFinderWidget().updateAll(getApplication())
    } catch (_: Throwable) {
      sideFailed = true
    }
    try {
      pushWearConfig()
    } catch (_: Throwable) {
      sideFailed = true
    }
    onDone(if (sideFailed) BackupCommit.IMPORTED_PARTIAL else BackupCommit.IMPORTED)
  }
}

enum class BackupCommit { REJECTED, IMPORTED, IMPORTED_PARTIAL }

sealed interface PairUiState {
  data object Idle : PairUiState
  data class Running(val step: org.tfv.ata.core.ble.PairStep) : PairUiState
  /** [reached] is the last step that started (null: stopped before searching, e.g. Bluetooth off). */
  data class Finished(val result: org.tfv.ata.core.ble.PairResult, val reached: org.tfv.ata.core.ble.PairStep?) : PairUiState
}
