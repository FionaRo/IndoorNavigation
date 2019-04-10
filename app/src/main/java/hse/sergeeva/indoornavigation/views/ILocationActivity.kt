package hse.sergeeva.indoornavigation.views

import hse.sergeeva.indoornavigation.models.Location

interface ILocationActivity {

    fun showMessage(message: String)
    fun updateLocation(location: Location)
}