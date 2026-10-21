package org.tfv.ata.core.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("MissingPermission")
class BleConnection internal constructor(private val context: Context) : BluetoothGattCallback() {
  val inbox = Channel<ByteArray>(Channel.UNLIMITED)

  private var gatt: BluetoothGatt? = null
  private var writeChar: BluetoothGattCharacteristic? = null
  private var blockLength = 20
  private val opMutex = Mutex()
  private val buffer = ByteArrayOutputStream()
  private var lastRx = 0L

  private var connectedDef = CompletableDeferred<Boolean>()
  private var servicesDef = CompletableDeferred<Boolean>()
  private var mtuDef = CompletableDeferred<Int>()
  private var writeDef: CompletableDeferred<Boolean>? = null
  private var descDef: CompletableDeferred<Boolean>? = null

  override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
    BleLog.add("connection state=$newState status=$status")
    when (newState) {
      BluetoothProfile.STATE_CONNECTED -> if (!connectedDef.isCompleted) connectedDef.complete(true)
      BluetoothProfile.STATE_DISCONNECTED -> {
        if (!connectedDef.isCompleted) connectedDef.complete(false)
        inbox.close()
      }
    }
  }

  override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    BleLog.add("services discovered status=$status count=${gatt.services.size}")
    if (!servicesDef.isCompleted) servicesDef.complete(status == BluetoothGatt.GATT_SUCCESS)
  }

  override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
    BleLog.add("mtu=$mtu status=$status")
    if (!mtuDef.isCompleted) mtuDef.complete(mtu)
  }

  override fun onCharacteristicWrite(gatt: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
    writeDef?.takeIf { !it.isCompleted }?.complete(status == BluetoothGatt.GATT_SUCCESS)
  }

  override fun onDescriptorWrite(gatt: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
    BleLog.add("subscribe status=$status")
    descDef?.takeIf { !it.isCompleted }?.complete(status == BluetoothGatt.GATT_SUCCESS)
  }

  @Deprecated("Deprecated in Java")
  override fun onCharacteristicChanged(gatt: BluetoothGatt, c: BluetoothGattCharacteristic) {
    rx(c.value ?: return)
  }

  override fun onCharacteristicChanged(gatt: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray) {
    rx(value)
  }

  internal suspend fun open(device: BluetoothDevice, timeoutMs: Long): Boolean {
    val g = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
    } else {
      device.connectGatt(context, false, this)
    }
    gatt = g
    val ok = withTimeoutOrNull(timeoutMs) {
      if (!connectedDef.await()) return@withTimeoutOrNull false
      if (!g.discoverServices()) return@withTimeoutOrNull false
      if (!servicesDef.await()) return@withTimeoutOrNull false
      val service = g.getService(SERVICE)
      if (service == null) {
        BleLog.add("Tesla service not found on this device")
        return@withTimeoutOrNull false
      }
      val write = service.getCharacteristic(WRITE)
      val read = service.getCharacteristic(READ)
      if (write == null || read == null) {
        BleLog.add("Tesla characteristics missing (write=${write != null} read=${read != null})")
        return@withTimeoutOrNull false
      }
      writeChar = write
      BleLog.add("characteristics ok, read properties=0x${Integer.toHexString(read.properties)}")
      g.setCharacteristicNotification(read, true)
      val cccd = read.getDescriptor(CCCD)
      if (cccd != null) {
        // The car answers on this characteristic with indications or notifications: subscribe to what it offers.
        val value = if (read.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
          BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        } else {
          BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }
        descDef = CompletableDeferred()
        writeDescriptor(g, cccd, value)
        withTimeoutOrNull(5_000) { descDef?.await() } ?: BleLog.add("subscribe timed out")
      } else {
        BleLog.add("no CCCD descriptor on read characteristic")
      }
      g.requestMtu(512)
      blockLength = (withTimeoutOrNull(3000) { mtuDef.await() } ?: 23).coerceIn(23, 517) - 3
      true
    } ?: false
    if (!ok) {
      BleLog.add("connection setup failed")
      close()
    }
    return ok
  }

  suspend fun send(message: ByteArray): Boolean = opMutex.withLock {
    val g = gatt ?: return false
    val char = writeChar ?: return false
    val framed = ByteArray(message.size + 2)
    framed[0] = (message.size ushr 8).toByte()
    framed[1] = message.size.toByte()
    System.arraycopy(message, 0, framed, 2, message.size)
    var offset = 0
    while (offset < framed.size) {
      val end = (offset + blockLength).coerceAtMost(framed.size)
      val chunk = framed.copyOfRange(offset, end)
      val def = CompletableDeferred<Boolean>()
      writeDef = def
      val submitted = writeChunk(g, char, chunk)
      if (!submitted) { BleLog.add("write rejected by Android"); return false }
      if (withTimeoutOrNull(3000) { def.await() } != true) { BleLog.add("write not confirmed"); return false }
      offset = end
    }
    BleLog.add("sent ${message.size} bytes")
    true
  }

  @Suppress("DEPRECATION")
  private fun writeDescriptor(g: BluetoothGatt, d: BluetoothGattDescriptor, value: ByteArray) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      g.writeDescriptor(d, value)
    } else {
      d.value = value
      g.writeDescriptor(d)
    }
  }

  @Suppress("DEPRECATION")
  private fun writeChunk(g: BluetoothGatt, char: BluetoothGattCharacteristic, chunk: ByteArray): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      g.writeCharacteristic(char, chunk, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) ==
        BluetoothGatt.GATT_SUCCESS
    } else {
      char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
      char.value = chunk
      g.writeCharacteristic(char)
    }

  private fun rx(data: ByteArray) {
    val now = System.currentTimeMillis()
    if (now - lastRx > RX_TIMEOUT_MS) buffer.reset()
    lastRx = now
    buffer.write(data)
    var bytes = buffer.toByteArray()
    while (bytes.size >= 2) {
      val len = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
      if (len > 1024) { buffer.reset(); return }
      if (bytes.size < len + 2) break
      BleLog.add("received $len bytes")
      inbox.trySend(bytes.copyOfRange(2, 2 + len))
      val remaining = bytes.copyOfRange(2 + len, bytes.size)
      buffer.reset()
      buffer.write(remaining)
      bytes = remaining
    }
  }

  fun close() {
    runCatching { gatt?.disconnect() }
    runCatching { gatt?.close() }
    gatt = null
    inbox.close()
  }

  companion object {
    private const val RX_TIMEOUT_MS = 1000L
    private val SERVICE = UUID.fromString(org.tfv.ata.core.Ble.SERVICE_UUID)
    private val WRITE = UUID.fromString(org.tfv.ata.core.Ble.WRITE_UUID)
    private val READ = UUID.fromString(org.tfv.ata.core.Ble.READ_UUID)
    private val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
  }
}

@SuppressLint("MissingPermission")
object BleTransport {
  /** Tesla BLE names: "S" + 16 hex digits + a letter (C = car). */
  private val TESLA_NAME = Regex("^S[0-9a-f]{16}[A-Z]$")

  fun isBluetoothOn(context: Context): Boolean {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return manager?.adapter?.isEnabled == true
  }

  /**
   * Finds the car by its BLE local name. First with a hardware name filter; some phones don't match names in
   * the controller, so the rest of the time is spent on an unfiltered scan that compares names in software.
   * Returns the device from the scan result (keeps the car's random address type for connecting).
   */
  suspend fun scanDevice(context: Context, localName: String, timeoutMs: Long): BluetoothDevice? {
    BleLog.add("looking for $localName")
    connectedDevice(context, localName)?.let { return it }
    val filtered = scan(context, localName, timeoutMs / 2, useFilter = true)
    return filtered ?: scan(context, localName, timeoutMs - timeoutMs / 2, useFilter = false)
  }

  /**
   * The car may already be connected to this phone (e.g. by the Tesla app) and then stop advertising.
   * Android shares that link: ATA can open its own GATT client on the same device.
   */
  private fun connectedDevice(context: Context, localName: String): BluetoothDevice? {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val connected = runCatching { manager.getConnectedDevices(BluetoothProfile.GATT) }.getOrDefault(emptyList())
    BleLog.add("already connected: " + connected.joinToString { it.name ?: "?" }.ifEmpty { "none" })
    return connected.firstOrNull { it.name == localName }?.also { BleLog.add("using existing connection") }
  }

  private suspend fun scan(context: Context, localName: String, timeoutMs: Long, useFilter: Boolean): BluetoothDevice? {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val scanner = manager.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return null
    val found = CompletableDeferred<BluetoothDevice?>()
    val teslas = mutableSetOf<String>()
    var seen = 0
    val cb = object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        seen++
        val name = result.scanRecord?.deviceName ?: result.device.name
        if (name != null && TESLA_NAME.matches(name) && teslas.add(name)) BleLog.add("Tesla seen: $name rssi=${result.rssi}")
        if (name == localName && !found.isCompleted) found.complete(result.device)
      }

      override fun onBatchScanResults(results: MutableList<ScanResult>) {
        results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
      }

      override fun onScanFailed(errorCode: Int) {
        BleLog.add("scan failed error=$errorCode")
        if (!found.isCompleted) found.complete(null)
      }
    }
    val filters = if (useFilter) listOf(ScanFilter.Builder().setDeviceName(localName).build()) else emptyList()
    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    BleLog.add(if (useFilter) "scan with name filter" else "scan without filter")
    scanner.startScan(filters, settings, cb)
    return try {
      withTimeoutOrNull(timeoutMs) { found.await() }.also {
        BleLog.add(if (it != null) "car found" else "not found ($seen advertisements seen)")
      }
    } finally {
      runCatching { scanner.stopScan(cb) }
    }
  }

  suspend fun connect(context: Context, device: BluetoothDevice, timeoutMs: Long): BleConnection? {
    val connection = BleConnection(context)
    return if (connection.open(device, timeoutMs)) connection else null
  }
}
