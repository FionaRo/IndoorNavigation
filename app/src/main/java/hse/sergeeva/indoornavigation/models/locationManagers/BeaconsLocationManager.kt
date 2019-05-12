package hse.sergeeva.indoornavigation.models.locationManagers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import hse.sergeeva.indoornavigation.models.Location
import android.bluetooth.le.ScanSettings
import java.util.*
import android.support.annotation.NonNull
import android.util.Log
import kotlin.experimental.and
import kotlin.math.min


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

        if (result.scanRecord != null) {
            val scanRecord = result.scanRecord.bytes

            var startByte = 2
            var patternFound = false
            while (startByte <= 5) {
                if (scanRecord[startByte + 2].toInt() and 0xff == 0x02 && // identifies an iBeacon
                    scanRecord[startByte + 3].toInt() and 0xff == 0x15
                ) {
                    patternFound = true
                    break
                }
                startByte++
            }

            if (patternFound) {
                val uuidBytes = ByteArray(16)
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16)
                val uuid = bytesToUuid(uuidBytes)

                // get the major from hex result
                val major =
                    byteArrayToInteger(Arrays.copyOfRange(scanRecord, startByte + 20, startByte + 22))

                // get the minor from hex result
                val minor =
                    byteArrayToInteger(Arrays.copyOfRange(scanRecord, startByte + 22, startByte + 24))

                Log.d("Beacon", "$major:$minor")

            }
        }

        if (!devices.containsKey(device.address))
            devices[device.address] = device.name
    }

    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

    fun bytesToUuid(bytes: ByteArray): UUID {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[v.ushr(4)]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }

        val hex = String(hexChars)

        return UUID.fromString(
            hex.substring(0, 8) + "-" +
                    hex.substring(8, 12) + "-" +
                    hex.substring(12, 16) + "-" +
                    hex.substring(16, 20) + "-" +
                    hex.substring(20, 32)
        )
    }

    fun byteArrayToInteger(byteArray: ByteArray): Int {
        return (byteArray[0].toInt() and 0xff) * 0x100 + (byteArray[1].toInt() and 0xff)
    }

    override fun onScanFailed(errorCode: Int) {
        locationReceiver(false, null)
    }
}