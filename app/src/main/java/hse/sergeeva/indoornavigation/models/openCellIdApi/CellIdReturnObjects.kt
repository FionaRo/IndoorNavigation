package hse.sergeeva.indoornavigation.models.openCellIdApi

class CellIdLocation(
    val status: String,
    val message: String,
    val lat: Double,
    val lon: Double,
    val accuracy: Int,
    val address: String,
    val address_details: CellIdAddressDetails
)

class CellIdAddressDetails(
    val area: String,
    val locality: String,
    val district: String,
    val county: String,
    val city: String,
    val state: String,
    val country: String,
    val country_code: Int,
    val postal_code: Int
)