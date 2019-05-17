package hse.sergeeva.indoornavigation.presenters

import android.content.Context
import android.util.Log
import hse.sergeeva.indoornavigation.algorithms.kalmanFilter.KalmanLocationService
import hse.sergeeva.indoornavigation.models.Location
import hse.sergeeva.indoornavigation.presenters.locationManagers.*
import hse.sergeeva.indoornavigation.views.ILocationActivity
import kotlinx.coroutines.*
import java.lang.Exception

class LocationScanners(private val context: Context, private val activity: ILocationActivity) {

    private var locationManagerType: LocationManagerType = LocationManagerType.WiFi
    private var locationManager: ILocationManager = WiFiLocationManager(context, ::onLocationReceiver)
    private var kalmanFilter: KalmanLocationService = KalmanLocationService(context, ::onKalmanLocationReceiver)
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
            LocationManagerType.Vlc -> locationManager = VlcLocationManager(context, ::onLocationReceiver)
        }
    }

    fun scanLocation() {
        if (_isRunning) return
        _isStopped = false
        _isRunning = true
        kalmanFilter.start()

        GlobalScope.launch {
            while (!_isStopped) {
                try {
                    val success = locationManager.getLocation()
                    if (!success)
                        activity.showMessage("Error in getLocation")
                    activity.updateData()
                } catch (e: Exception) {
                    Log.d("LocationScanner", e.message)
                } finally {
                    Thread.sleep(2000)
                }
            }
        }
    }

    fun stopScanning() {
        _isRunning = false
        _isStopped = true
        locationManager.stopScan()
        kalmanFilter.stop()
    }

    private fun onLocationReceiver(success: Boolean, location: Location?) {
        if (!success)
            activity.showMessage("Cannot get location")
        else if (location != null)
            kalmanFilter.onLocationChanged(location)
    }

    private fun onKalmanLocationReceiver(location: android.location.Location) {
        activity.updateLocation(Location(location.latitude, location.longitude, 2, location.accuracy.toInt()))
    }
}