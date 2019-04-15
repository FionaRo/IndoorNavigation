package hse.sergeeva.indoornavigation.models.openCellIdApi

class CellIdCellTower(
    val radio: String,
    val cid: Int,
    val lac: Int,
    val mcc: Int,
    val mnc: Int,
    val signal: Int = 0,
    val ta: Int = 0
)

class CellIdWiFiAccessPoint(
    val bssid: String,
    val signal: Int,
    val channel: Int = 0,
    val frequency: Int = 0,
    val signalToNoiseRatio: Int = 0
)

class CellIdGeolocationRequestBody(
    val token: String,
    val radio: String = "lte",
    val mcc: String = "",
    val mnc: String = "",
    val cells: ArrayList<CellIdCellTower> = arrayListOf(),
    val wifi: ArrayList<CellIdWiFiAccessPoint> = arrayListOf(),
    val bt: Int = 0
)
