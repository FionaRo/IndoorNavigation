package hse.sergeeva.indoornavigation.alorithms

class LatLngDistance (val latitude: Double, val longitude: Double, var distance: Double = 0.0) {

    init {
        distance /= 1000
    }
}