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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.tfv.ata.core.AtaSettings
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
    AtaUiState(settings, vehicles, selected, snapshot)
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
  fun markOnboarded() = viewModelScope.launch { store.updateSettings { it.copy(onboarded = true) } }

  fun recordCarLocation() = viewModelScope.launch {
    val loc = org.tfv.ata.geo.Geo.lastLocation(getApplication()) ?: return@launch
    store.updateSettings {
      it.copy(carLat = loc.latitude, carLng = loc.longitude, carLocAt = System.currentTimeMillis())
    }
  }

  init {
    viewModelScope.launch {
      val s = store.settings.first()
      StatusPoller.sync(getApplication(), s)
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
    val resolved = actions ?: effectiveWearActions(store.settings.first())
    val v = currentVehicle()
    WearPush.pushConfig(getApplication(), resolved, v?.vin ?: "", v?.name ?: "")
  }

  fun updateSettings(transform: (AtaSettings) -> AtaSettings) = viewModelScope.launch {
    store.updateSettings(transform)
    ShortcutSync.refresh(getApplication())
    StatusPoller.sync(getApplication(), store.settings.first())
    runCatching {
      ActionsWidget().updateAll(getApplication())
      StatusWidget().updateAll(getApplication())
    }
  }

  fun refreshStatus() = viewModelScope.launch {
    if (_refreshing.value) return@launch
    _refreshing.value = true
    val snap = runCatching {
      kotlinx.coroutines.withContext(Dispatchers.IO) { StatusPoller.pollNow(getApplication(), wake = true) }
    }.getOrNull()
    if (snap != null) runCatching { StatusWidget().updateAll(getApplication()) }
    _refreshing.value = false
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
  }

  fun addVehicle(name: String, vin: String, home: String, work: String) =
    saveVehicle(Vehicle(UUID.randomUUID().toString(), name, vin, home, work))

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

  fun setHomeActions(actions: List<org.tfv.ata.core.AtaAction>) =
    updateSettings { it.copy(homeActionIds = actions.map { a -> a.id }) }

  fun setVehicleActions(actions: List<org.tfv.ata.core.AtaAction>) =
    updateSettings { it.copy(vehicleActionIds = actions.map { a -> a.id }) }

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

  fun toggleHomeFavorite(action: org.tfv.ata.core.AtaAction) = updateSettings { s ->
    val current = if (s.homeActionIds.isEmpty()) {
      org.tfv.ata.core.AtaAction.entries
        .filter { it.category == org.tfv.ata.core.AtaCategory.FUNCTION_B && it.enabled && !it.beta }
        .map { it.id }
    } else {
      s.homeActionIds
    }
    val updated = if (action.id in current) current - action.id else current + action.id
    s.copy(homeActionIds = updated)
  }
}

sealed interface PairUiState {
  data object Idle : PairUiState
  data class Running(val step: org.tfv.ata.core.ble.PairStep) : PairUiState
  /** [reached] is the last step that started (null: stopped before searching, e.g. Bluetooth off). */
  data class Finished(val result: org.tfv.ata.core.ble.PairResult, val reached: org.tfv.ata.core.ble.PairStep?) : PairUiState
}
