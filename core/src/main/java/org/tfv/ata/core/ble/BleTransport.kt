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
    when (newState) {
      BluetoothProfile.STATE_CONNECTED -> if (!connectedDef.isCompleted) connectedDef.complete(true)
      BluetoothProfile.STATE_DISCONNECTED -> {
        if (!connectedDef.isCompleted) connectedDef.complete(false)
        inbox.close()
      }
    }
  }

  override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
    if (!servicesDef.isCompleted) servicesDef.complete(status == BluetoothGatt.GATT_SUCCESS)
  }

  override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
    if (!mtuDef.isCompleted) mtuDef.complete(mtu)
  }

  override fun onCharacteristicWrite(gatt: BluetoothGatt, c: BluetoothGattCharacteristic, status: Int) {
    writeDef?.takeIf { !it.isCompleted }?.complete(status == BluetoothGatt.GATT_SUCCESS)
  }

  override fun onDescriptorWrite(gatt: BluetoothGatt, d: BluetoothGattDescriptor, status: Int) {
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
      val service = g.getService(SERVICE) ?: return@withTimeoutOrNull false
      val write = service.getCharacteristic(WRITE) ?: return@withTimeoutOrNull false
      val read = service.getCharacteristic(READ) ?: return@withTimeoutOrNull false
      writeChar = write
      g.setCharacteristicNotification(read, true)
      val cccd = read.getDescriptor(CCCD)
      if (cccd != null) {
        descDef = CompletableDeferred()
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        g.writeDescriptor(cccd)
        descDef?.await()
      }
      g.requestMtu(512)
      blockLength = (withTimeoutOrNull(3000) { mtuDef.await() } ?: 23).coerceIn(23, 517) - 3
      true
    } ?: false
    if (!ok) close()
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
      if (!submitted) return false
      if (withTimeoutOrNull(3000) { def.await() } != true) return false
      offset = end
    }
    true
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
  suspend fun scanAddress(context: Context, localName: String, timeoutMs: Long): String? {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val scanner = manager.adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return null
    val found = CompletableDeferred<String?>()
    val cb = object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        if (!found.isCompleted) found.complete(result.device.address)
      }
    }
    val filters = listOf(ScanFilter.Builder().setDeviceName(localName).build())
    val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    scanner.startScan(filters, settings, cb)
    return try {
      withTimeoutOrNull(timeoutMs) { found.await() }
    } finally {
      runCatching { scanner.stopScan(cb) }
    }
  }

  suspend fun connect(context: Context, address: String, timeoutMs: Long): BleConnection? {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val device = manager.adapter?.getRemoteDevice(address) ?: return null
    val connection = BleConnection(context)
    return if (connection.open(device, timeoutMs)) connection else null
  }
}
