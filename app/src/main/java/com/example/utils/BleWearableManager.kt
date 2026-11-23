package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class DiscoveredWearableDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val supportsHealthProfile: Boolean
)

enum class WearableConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

data class BleWearableState(
    val isBleSupported: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasPermission: Boolean = false,
    val isScanning: Boolean = false,
    val discoveredDevices: List<DiscoveredWearableDevice> = emptyList(),
    val connectionStatus: WearableConnectionStatus = WearableConnectionStatus.DISCONNECTED,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val liveHeartRateBpm: Int? = null,
    val liveSpO2Percent: Int? = null,
    val batteryPercent: Int? = null,
    val lastUpdatedMillis: Long? = null,
    val heartRateHistory: List<Int> = emptyList(),
    val statusNote: String = ""
)

class BleWearableManager(private val context: Context) {

    companion object {
        private val UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val UUID_HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val UUID_PLX_SERVICE = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb")
        private val UUID_PLX_SPOT_CHECK = UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb")
        private val UUID_PLX_CONTINUOUS = UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val UUID_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        private val UUID_CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val SCAN_TIMEOUT_MS = 12_000L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var activeGatt: BluetoothGatt? = null
    private var activeScanner: BluetoothLeScanner? = null
    private val pendingNotificationChars = ArrayDeque<BluetoothGattCharacteristic>()

    private val _state = MutableStateFlow(BleWearableState())
    val state: StateFlow<BleWearableState> = _state.asStateFlow()

    init {
        refreshHardwareStatus()
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasBlePermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refreshHardwareStatus() {
        val bleSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            bluetoothAdapter != null
        val enabled = try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
        val permitted = hasBlePermissions()
        _state.update {
            it.copy(
                isBleSupported = bleSupported,
                isBluetoothEnabled = enabled,
                hasPermission = permitted
            )
        }
    }

    private val scanStopRunnable = Runnable {
        stopScan()
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            if (!hasBlePermissions()) return

            val deviceName = try {
                device.name ?: result.scanRecord?.deviceName
            } catch (_: SecurityException) {
                null
            }
            val displayName = deviceName?.takeIf { it.isNotBlank() } ?: "BLE Device (${device.address.takeLast(5)})"
            val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()
            val supportsHealth = serviceUuids.contains(UUID_HEART_RATE_SERVICE) ||
                serviceUuids.contains(UUID_PLX_SERVICE) ||
                displayName.contains("watch", ignoreCase = true) ||
                displayName.contains("band", ignoreCase = true) ||
                displayName.contains("fit", ignoreCase = true) ||
                displayName.contains("pulse", ignoreCase = true) ||
                displayName.contains("oximeter", ignoreCase = true) ||
                displayName.contains("garmin", ignoreCase = true) ||
                displayName.contains("mi ", ignoreCase = true) ||
                displayName.contains("amazfit", ignoreCase = true) ||
                displayName.contains("polar", ignoreCase = true)

            val item = DiscoveredWearableDevice(
                name = displayName,
                address = device.address,
                rssi = result.rssi,
                supportsHealthProfile = supportsHealth
            )

            _state.update { current ->
                val updatedList = (current.discoveredDevices.filterNot { it.address == item.address } + item)
                    .sortedWith(compareByDescending<DiscoveredWearableDevice> { it.supportsHealthProfile }.thenByDescending { it.rssi })
                current.copy(discoveredDevices = updatedList)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _state.update {
                it.copy(
                    isScanning = false,
                    statusNote = "BLE scan failed (code $errorCode)"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        refreshHardwareStatus()
        if (!_state.value.hasPermission) {
            _state.update { it.copy(statusNote = "Bluetooth permission required to scan nearby smartwatches.") }
            return
        }
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _state.update { it.copy(isScanning = false, statusNote = "Please turn on Bluetooth to scan wearables.") }
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _state.update { it.copy(isScanning = false, statusNote = "Bluetooth LE scanner unavailable on this device.") }
            return
        }

        stopScan()
        activeScanner = scanner
        _state.update {
            it.copy(
                isScanning = true,
                discoveredDevices = emptyList(),
                statusNote = "Scanning for BLE Heart Rate, SpO2 & Smartwatch wearables..."
            )
        }

        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, scanCallback)
            mainHandler.removeCallbacks(scanStopRunnable)
            mainHandler.postDelayed(scanStopRunnable, SCAN_TIMEOUT_MS)
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isScanning = false,
                    statusNote = e.localizedMessage ?: "Unable to start BLE scan."
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        mainHandler.removeCallbacks(scanStopRunnable)
        if (_state.value.isScanning && hasBlePermissions()) {
            try {
                activeScanner?.stopScan(scanCallback)
            } catch (_: Exception) {}
        }
        _state.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, displayName: String) {
        refreshHardwareStatus()
        if (!hasBlePermissions()) {
            _state.update { it.copy(statusNote = "Missing Bluetooth Connect permission.") }
            return
        }
        val adapter = bluetoothAdapter ?: return
        stopScan()
        disconnect()

        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(address)
            _state.update {
                it.copy(
                    connectionStatus = WearableConnectionStatus.CONNECTING,
                    connectedDeviceName = displayName,
                    connectedDeviceAddress = address,
                    statusNote = "Connecting to $displayName..."
                )
            }
            activeGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    connectionStatus = WearableConnectionStatus.DISCONNECTED,
                    connectedDeviceName = null,
                    connectedDeviceAddress = null,
                    statusNote = "Connection error: ${e.localizedMessage ?: "Unknown"}"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        pendingNotificationChars.clear()
        try {
            if (hasBlePermissions()) {
                activeGatt?.disconnect()
            }
            activeGatt?.close()
        } catch (_: Exception) {}
        activeGatt = null
        _state.update {
            it.copy(
                connectionStatus = WearableConnectionStatus.DISCONNECTED,
                connectedDeviceName = null,
                connectedDeviceAddress = null,
                statusNote = "Wearable disconnected."
            )
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.update {
                    it.copy(
                        connectionStatus = WearableConnectionStatus.CONNECTED,
                        statusNote = "Connected! Discovering health services (HR, SpO2)..."
                    )
                }
                if (hasBlePermissions()) {
                    try {
                        gatt.discoverServices()
                    } catch (_: Exception) {}
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                try {
                    gatt.close()
                } catch (_: Exception) {}
                if (activeGatt == gatt) activeGatt = null
                _state.update {
                    it.copy(
                        connectionStatus = WearableConnectionStatus.DISCONNECTED,
                        statusNote = "Device disconnected (status=$status)."
                    )
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS || !hasBlePermissions()) return

            pendingNotificationChars.clear()
            gatt.getService(UUID_HEART_RATE_SERVICE)?.getCharacteristic(UUID_HEART_RATE_MEASUREMENT)?.let {
                pendingNotificationChars.addLast(it)
            }
            gatt.getService(UUID_PLX_SERVICE)?.getCharacteristic(UUID_PLX_CONTINUOUS)?.let {
                pendingNotificationChars.addLast(it)
            }
            gatt.getService(UUID_PLX_SERVICE)?.getCharacteristic(UUID_PLX_SPOT_CHECK)?.let {
                pendingNotificationChars.addLast(it)
            }

            if (pendingNotificationChars.isNotEmpty()) {
                enableNextNotification(gatt)
                _state.update { it.copy(statusNote = "Streaming live wearable vitals...") }
            } else {
                _state.update {
                    it.copy(statusNote = "Connected to wearable (no standard GATT HR/SpO2 characteristic exposed; check Health Sync / manual log).")
                }
            }

            // Read battery level if available
            gatt.getService(UUID_BATTERY_SERVICE)?.getCharacteristic(UUID_BATTERY_LEVEL)?.let { batChar ->
                if (pendingNotificationChars.isEmpty()) {
                    try {
                        gatt.readCharacteristic(batChar)
                    } catch (_: Exception) {}
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (pendingNotificationChars.isNotEmpty()) {
                enableNextNotification(gatt)
            } else {
                gatt.getService(UUID_BATTERY_SERVICE)?.getCharacteristic(UUID_BATTERY_LEVEL)?.let { batChar ->
                    try {
                        gatt.readCharacteristic(batChar)
                    } catch (_: Exception) {}
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val data = characteristic.value ?: return
            handleCharacteristicData(characteristic.uuid, data)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleCharacteristicData(characteristic.uuid, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                val data = characteristic.value ?: return
                handleCharacteristicData(characteristic.uuid, data)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicData(characteristic.uuid, value)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNextNotification(gatt: BluetoothGatt) {
        val next = pendingNotificationChars.removeFirstOrNull() ?: return
        if (!hasBlePermissions()) return
        try {
            gatt.setCharacteristicNotification(next, true)
            val cccd = next.getDescriptor(UUID_CLIENT_CHAR_CONFIG)
            if (cccd != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(cccd)
                }
            } else {
                enableNextNotification(gatt)
            }
        } catch (_: Exception) {
            enableNextNotification(gatt)
        }
    }

    private fun handleCharacteristicData(uuid: UUID, value: ByteArray) {
        if (value.isEmpty()) return
        val now = System.currentTimeMillis()
        when (uuid) {
            UUID_HEART_RATE_MEASUREMENT -> {
                val flags = value[0].toInt() and 0xFF
                val isUint16 = (flags and 0x01) != 0
                val bpm = if (isUint16 && value.size >= 3) {
                    ((value[2].toInt() and 0xFF) shl 8) or (value[1].toInt() and 0xFF)
                } else if (value.size >= 2) {
                    value[1].toInt() and 0xFF
                } else {
                    return
                }
                if (bpm in 25..240) {
                    _state.update { current ->
                        val updatedHistory = (current.heartRateHistory + bpm).takeLast(24)
                        current.copy(
                            liveHeartRateBpm = bpm,
                            lastUpdatedMillis = now,
                            heartRateHistory = updatedHistory
                        )
                    }
                }
            }
            UUID_PLX_CONTINUOUS, UUID_PLX_SPOT_CHECK -> {
                // Standard Bluetooth PLX Measurement: byte 0 = flags, bytes 1..2 = SFLOAT SpO2, bytes 3..4 = SFLOAT PR
                if (value.size >= 3) {
                    val spo2Raw = decodeSfloatInt(value[1], value[2])
                    val prRaw = if (value.size >= 5) decodeSfloatInt(value[3], value[4]) else null
                    _state.update { current ->
                        current.copy(
                            liveSpO2Percent = if (spo2Raw != null && spo2Raw in 50..100) spo2Raw else current.liveSpO2Percent,
                            liveHeartRateBpm = if (prRaw != null && prRaw in 25..240) prRaw else current.liveHeartRateBpm,
                            lastUpdatedMillis = now
                        )
                    }
                }
            }
            UUID_BATTERY_LEVEL -> {
                val battery = value[0].toInt() and 0xFF
                if (battery in 0..100) {
                    _state.update { it.copy(batteryPercent = battery) }
                }
            }
        }
    }

    private fun decodeSfloatInt(lowByte: Byte, highByte: Byte): Int? {
        val raw = ((highByte.toInt() and 0xFF) shl 8) or (lowByte.toInt() and 0xFF)
        var mantissa = raw and 0x0FFF
        if ((mantissa and 0x0800) != 0) mantissa = mantissa or -0x1000
        var exponent = (raw shr 12) and 0x0F
        if ((exponent and 0x08) != 0) exponent = exponent or -0x10
        val value = mantissa * Math.pow(10.0, exponent.toDouble())
        return if (value.isNaN() || value.isInfinite()) null else value.toInt()
    }

    fun cleanup() {
        stopScan()
        disconnect()
    }
}
