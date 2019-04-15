package hse.sergeeva.indoornavigation.presenters

import android.content.Context
import android.util.Log
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.models.locationManagers.*
import hse.sergeeva.indoornavigation.views.ILocationActivity
import java.lang.Exception

class LocationScanners(private val context: Context, private val activity: ILocationActivity) {

    private var locationManagerType: LocationManagerType = LocationManagerType.WiFi
    private var locationManager: ILocationManager = WiFiLocationManager(context, ::onLocationReceiver)
    private var _isStopped = false
    private var _isRunning = false

    fun changeLocationManager(locationManagerType: LocationManagerType) {
        if (this.locationManagerType == locationManagerType) return
        this.locationManager.stopScan()

        this.locationManagerType = locationManagerType
        when (locationManagerType) {
            LocationManagerType.WiFi -> locationManager = WiFiLocationManager(context, ::onLocationReceiver)
            LocationManagerType.CellId -> locationManager = CellLocationManager(context, ::onLocationReceiver)
            LocationManagerType.Beacons -> locationManager = BeaconsLocationManager(context, ::onLocationReceiver)
            LocationManagerType.Vlc -> locationManager = VlcLocationManager(context)
        }
    }

    fun scanLocation() {
        if (_isRunning) return
        _isStopped = false
        _isRunning = true

        DoAsync {
            while (!_isStopped) {
                try {
                    val success = locationManager.getLocation()
                    if (!success)
                        activity.showMessage("Error in getLocation")
                } catch (e: Exception) {
                    Log.d("LocationScanner", e.message)
                } finally {
                    Thread.sleep(500)
                }
            }
        }
    }

    fun stopScanning() {
        _isRunning = false
        _isStopped = true
        locationManager.stopScan()
    }

    private fun onLocationReceiver(success: Boolean, location: Location?) {
        if (!success)
            activity.showMessage("Cannot get location")
        else
            activity.updateLocation(location!!)
    }
}