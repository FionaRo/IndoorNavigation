package hse.sergeeva.indoornavigation.presenters.locationManagers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import hse.sergeeva.indoornavigation.models.Location
import android.bluetooth.le.ScanSettings
import java.util.*
import hse.sergeeva.indoornavigation.algorithms.LatLngDistance
import hse.sergeeva.indoornavigation.algorithms.Triangulation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class BeaconsLocationManager(
    context: Context,
    private val locationReceiver: (success: Boolean, location: Location?) -> Unit
) : ILocationManager, ScanCallback() {

    private val bluetoothManager =
        context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanning = false
    private val devices = mutableListOf<Pair<Pair<Int, Int>, Double>>()
    private val beaconLocations = hashMapOf(
        Pair(2, 2021) to Location(56.268159, 43.876984, 2),
        Pair(2, 2022) to Location(56.268185, 43.877077, 2),
        Pair(2, 2023) to Location(56.268102, 43.877048, 2),
        Pair(2, 2024) to Location(56.268136, 43.877143, 2)
    )

    init {
        GlobalScope.launch {
            worker()
        }
    }

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

                devices += Pair(
                    Pair(major, minor),
                    Triangulation.rssiToDistance(result.rssi)
                )
                if (devices.size > 3)
                    devices.removeAt(0)
            }
        }
    }

    private fun worker() {
        while (scanning) {
            Thread.sleep(500)

            if (devices.size < 3) continue

            val d1 = devices[0]
            val d2 = devices[1]
            val d3 = devices[2]

            val p1 = beaconLocations[d1.first]
            val p2 = beaconLocations[d2.first]
            val p3 = beaconLocations[d3.first]

            if (p1 == null || p2 == null || p3 == null) continue

            val lld1 = LatLngDistance(p1.latitude, p1.longitude, d1.second)
            val lld2 = LatLngDistance(p2.latitude, p2.longitude, d2.second)
            val lld3 = LatLngDistance(p3.latitude, p3.longitude, d3.second)

            val location = Triangulation.triangulateLocation(lld1, lld2, lld3)

            if (location.latitude == 0.0 || location.longitude == 0.0)
                locationReceiver(false, null)
            else
                locationReceiver(true, Location(location.latitude, location.longitude, d1.first.first))
        }
    }

    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

    private fun bytesToUuid(bytes: ByteArray): UUID {
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

    private fun byteArrayToInteger(byteArray: ByteArray): Int {
        return (byteArray[0].toInt() and 0xff) * 0x100 + (byteArray[1].toInt() and 0xff)
    }

    override fun onScanFailed(errorCode: Int) {
        locationReceiver(false, null)
    }
}