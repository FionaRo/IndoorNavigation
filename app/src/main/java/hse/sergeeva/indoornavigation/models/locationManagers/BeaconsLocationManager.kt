package hse.sergeeva.indoornavigation.models.locationManagers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import hse.sergeeva.indoornavigation.models.Location
import android.bluetooth.le.ScanSettings



class BeaconsLocationManager(
    context: Context,
    private val locationReceiver: (success: Boolean, location: Location?) -> Unit
) : ILocationManager, ScanCallback() {

    private val bluetoothManager =
        context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanning = false
    private val devices = hashMapOf<String, String?>()

    @SuppressLint("MissingPermission")
    override fun getLocation(): Boolean {
        if (scanning)
            bluetoothScanner.stopScan(this)
        scanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0)
            .build()

        bluetoothScanner.startScan(null, settings, this)
        return true
    }

    override fun stopScan() {
        scanning = false
        bluetoothScanner.stopScan(this)
    }

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        val device = result!!.device
        if (!devices.containsKey(device.address))
            devices[device.address] = device.name
    }

    override fun onScanFailed(errorCode: Int) {
        locationReceiver(false, null)
    }
}