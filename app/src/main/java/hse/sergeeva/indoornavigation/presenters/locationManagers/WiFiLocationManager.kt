package hse.sergeeva.indoornavigation.presenters.locationManagers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import com.android.volley.Response
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.models.googleApi.*
import hse.sergeeva.indoornavigation.models.openCellIdApi.CellIdLocation
import hse.sergeeva.indoornavigation.models.openCellIdApi.CellIdWiFiAccessPoint
import hse.sergeeva.indoornavigation.models.openCellIdApi.OpenCellIdApi
import hse.sergeeva.indoornavigation.models.wigletApi.WigletApi
import hse.sergeeva.indoornavigation.models.yandexApi.*

class WiFiLocationManager(
    private val context: Context,
    private val locationReceiver: (success: Boolean, location: Location?) -> Unit
) : BroadcastReceiver(), ILocationManager {

    private var wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val googleApi: GoogleApi = GoogleApi(context)
    private var mills: Long = 0
    private var scanStopped = false
    private val openCellIdApi = OpenCellIdApi(context)
    private val yandexApi = YandexApi(context)


    //scan limitations 4 times in 2 minute
    //start scan is deprecated
    override fun getLocation(): Boolean {
        val currentMills = System.currentTimeMillis()
        if (currentMills - mills < 30 * 1000) return true
        mills = System.currentTimeMillis()

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(this, intentFilter)
        val success = wifiManager.startScan()
        if (!success)
            Log.d("WiFiLocationManager", "Scan wi-fi failed")
        return success
    }

    override fun stopScan() {
        scanStopped = true
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (scanStopped) return

        val success = intent!!.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
        if (success) {
            processWifiResultToYandex(wifiManager.scanResults)
        } else {
            Log.d("WiFiLocationManager", "Cannot get scan results")
            locationReceiver(false, null)
        }
    }

    private fun processWifiResultToGoogle(scanResults: List<ScanResult>) {
        val wifiData = arrayListOf<GoogleWiFiAccessPoint>()
        for (wifi in scanResults) {
            var channel = 0
            if (wifi.centerFreq0 != 0)
                channel = (wifi.centerFreq0 - 2412) / 5 + 1
            else if (wifi.centerFreq1 != 0)
                channel = (wifi.centerFreq1 - 2412) / 5 + 1

            wifiData.add(GoogleWiFiAccessPoint(wifi.BSSID, wifi.level, channel = channel))
        }
        googleApi.getLocation(
            wifiData = wifiData,
            onSuccess = ::onSuccessDetermineLocationGoogle,
            onError = ::onErrorDetermineLocationGoogle
        )
    }

    private fun processWifiResultToWiglet(scanResults: List<ScanResult>) {
        val wiglet = WigletApi(context)
        for (wifi in scanResults) {
            wiglet.getNetworkDetailByBssid(wifi.BSSID, Response.Listener { resp ->
                val str = resp.toString()
            })
            wiglet.searchNetworkByBssid(wifi.BSSID, Response.Listener { resp ->
                val str = resp.toString()
            })
            wiglet.searchNetworkByName(wifi.SSID, Response.Listener { resp ->
                val str = resp.toString()
            })
        }
    }

    private fun processWifiResultToCellId(scanResults: List<ScanResult>) {
        val wifiData = arrayListOf<CellIdWiFiAccessPoint>()
        val openCellIdApi = OpenCellIdApi(context)
        for (wifi in scanResults) {
            var channel = 0
            var freq = 0
            if (wifi.centerFreq0 != 0) {
                channel = (wifi.centerFreq0 - 2412) / 5 + 1
                freq = wifi.centerFreq0
            } else if (wifi.centerFreq1 != 0) {
                channel = (wifi.centerFreq1 - 2412) / 5 + 1
                freq = wifi.centerFreq1
            }

            wifiData.add(CellIdWiFiAccessPoint(wifi.BSSID, wifi.level, channel = channel, frequency = freq))
        }

        openCellIdApi.getLocation(wifiData = wifiData, onSuccess = ::onResultCellId, onError = ::onResultCellId)
    }

    private fun processWifiResultToYandex(scanResults: List<ScanResult>) {
        val wifiData = arrayListOf<YandexWifiPoint>()
        for (wifi in scanResults) {
            wifiData.add(YandexWifiPoint(wifi.BSSID, wifi.level))
        }

        yandexApi.getLocation(wifiData = wifiData, onSuccess = ::onSuccessDetermineLocationYandex, onError = ::onErrorDetermineLocationYandex)
    }

    private fun onSuccessDetermineLocationGoogle(googleLocation: GoogleLocation) {
        if (scanStopped) return

        val currentLocation = Location(
            latitude = googleLocation.location.lat,
            longitude = googleLocation.location.lng,
            accuracy = googleLocation.accuracy
        )
        locationReceiver(true, currentLocation)
    }

    private fun onErrorDetermineLocationGoogle(error: GoogleError) {
        if (scanStopped) return

        Log.d("wifiManager", error.message)
        locationReceiver(false, null)
    }

    private fun onResultCellId(cellIdLocation: CellIdLocation) {
        if (scanStopped) return

        if (cellIdLocation.status == "error")
            locationReceiver(false, null)
        else {
            val currentLocation = Location(
                latitude = cellIdLocation.lat,
                longitude = cellIdLocation.lon,
                accuracy = cellIdLocation.accuracy,
                floor = 2
            )
            locationReceiver(true, currentLocation)
        }
    }

    private fun onSuccessDetermineLocationYandex(yandexLocation: YandexReturnObject) {
        if (scanStopped) return

        val currentLocation = Location(
            latitude = yandexLocation.position.latitude,
            longitude = yandexLocation.position.longitude,
            accuracy = yandexLocation.position.precision.toInt()
        )
        locationReceiver(true, currentLocation)
    }

    private fun onErrorDetermineLocationYandex(error: YandexReturnError) {
        if (scanStopped) return

        Log.d("wifiManager", error.error.message)
        locationReceiver(false, null)
    }

}