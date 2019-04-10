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

    init {
        scanLocation()
    }

    fun changeLocationManager(locationManagerType: LocationManagerType) {
        if (this.locationManagerType == locationManagerType) return

        this.locationManagerType = locationManagerType
        when (locationManagerType) {
            LocationManagerType.WiFi -> locationManager = WiFiLocationManager(context, ::onLocationReceiver)
            LocationManagerType.CellId -> locationManager = CellLocationManager(context, ::onLocationReceiver)
            LocationManagerType.Beacons -> locationManager = BeaconsLocationManager(context)
            LocationManagerType.Vlc -> locationManager = VlcLocationManager(context)
        }
    }

    private fun scanLocation() {
        DoAsync {
            while (true) {
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

    private fun onLocationReceiver(success: Boolean, location: Location?) {
        if (!success)
            activity.showMessage("Cannot get location")
        else
            activity.updateLocation(location!!)
    }
}