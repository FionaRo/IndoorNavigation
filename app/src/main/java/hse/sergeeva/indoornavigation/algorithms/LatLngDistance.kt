package hse.sergeeva.indoornavigation.algorithms

open class LatLng(val latitude: Double, val longitude: Double)

class LatLngDistance(
    latitude: Double,
    longitude: Double,
    var distance: Double = 0.0
) : LatLng(latitude, longitude)