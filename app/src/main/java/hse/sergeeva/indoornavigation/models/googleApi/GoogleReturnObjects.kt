package hse.sergeeva.indoornavigation.models.googleApi

class GoogleLocation(val location:GLocation, val accuracy: Int)
class GLocation(val lat: Double, val lng: Double)
class GoogleError(val errors: Array<GError>, val code: Int, val message: String)
class GError(val domain: String, val reason: String, val message: String)