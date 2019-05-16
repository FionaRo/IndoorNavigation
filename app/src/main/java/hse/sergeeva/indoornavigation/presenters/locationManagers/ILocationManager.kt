package hse.sergeeva.indoornavigation.presenters.locationManagers

interface ILocationManager {
    fun getLocation(): Boolean
    fun stopScan()
}