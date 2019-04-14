package hse.sergeeva.indoornavigation.models.locationManagers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.models.googleApi.GoogleApi
import hse.sergeeva.indoornavigation.models.googleApi.GoogleError
import hse.sergeeva.indoornavigation.models.googleApi.GoogleLocation
import hse.sergeeva.indoornavigation.models.googleApi.GoogleWiFiAccessPoint

class WiFiLocationManager(
    private val context: Context,
    private val locationReceiver: (success: Boolean, location: Location?) -> Unit
) : BroadcastReceiver(), ILocationManager {

    private var wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val googleApi: GoogleApi = GoogleApi(context)
    private var mills: Long = System.currentTimeMillis()
    private var scanStopped = false

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
            processWifiResult(wifiManager.scanResults)
        } else {
            Log.d("WiFiLocationManager", "Cannot get scan results")
            locationReceiver(false, null)
        }
    }

    private fun processWifiResult(scanResults: List<ScanResult>) {
        val wifiData = arrayListOf<GoogleWiFiAccessPoint>()
        //val wiglet = WigletApi(context)
        for (wifi in scanResults) {
            /*wiglet.getNetworkDetailByBssid(wifi.BSSID, Response.Listener { resp ->
                val str = resp.toString()
            })
            wiglet.searchNetworkByBssid(wifi.BSSID, Response.Listener { resp ->
                val str = resp.toString()
            })
            wiglet.searchNetworkByName(wifi.SSID, Response.Listener { resp ->
                val str = resp.toString()
            })*/
            var channel = 0
            if (wifi.centerFreq0 != 0)
                channel = (wifi.centerFreq0 - 2412) / 5 + 1
            else if (wifi.centerFreq1 != 0)
                channel = (wifi.centerFreq1 - 2412) / 5 + 1

            wifiData.add(GoogleWiFiAccessPoint(wifi.BSSID, wifi.level, channel = channel))
        }
        googleApi.getLocation(
            wifiData = wifiData,
            onSuccess = ::onSuccessDetermineLocation,
            onError = ::onErrorDetermineLocation
        )
    }

    private fun onSuccessDetermineLocation(googleLocation: GoogleLocation) {
        if (scanStopped) return

        val currentLocation = Location(
            latitude = googleLocation.location.lat,
            longitude = googleLocation.location.lng,
            accuracy = googleLocation.accuracy
        )
        locationReceiver(true, currentLocation)
    }

    private fun onErrorDetermineLocation(error: GoogleError) {
        if (scanStopped) return

        Log.d("wifiManager", error.message)
        locationReceiver(false, null)
    }
}