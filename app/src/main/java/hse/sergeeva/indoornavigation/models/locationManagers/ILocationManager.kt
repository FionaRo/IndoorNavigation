package hse.sergeeva.indoornavigation.models.locationManagers

import hse.sergeeva.indoornavigation.models.Location

interface ILocationManager {
    fun getLocation(): Boolean
    fun stopScan()
}