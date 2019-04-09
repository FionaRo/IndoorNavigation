package hse.sergeeva.indoornavigation.models.googleApi

class GoogleCellTower(
    val cellId: Int,
    val locationAreaCode: Int,
    val mobileCountryCode: Int,
    val mobileNetworkCode: Int,
    val age: Int = 0,
    val signalStrength: Int = 0,
    val timingAdvance: Int = 0
)

class GoogleWiFiAccessPoint(
    val macAddress: String,
    val signalStrength: Int,
    val age: Int = 0,
    val channel: Int = 0,
    val signalToNoiseRatio: Int = 0
)

class GoogleGeolocationRequestBody(
    val homeMobileCountryCode: Int = 0,
    val homeMobileNetworkCode: Int = 0,
    val radioType: String = "lte",
    val carrier: String = "",
    val considerIp: Boolean = true,
    val cellTowers: ArrayList<GoogleCellTower> = arrayListOf(),
    val wifiAccessPoints: ArrayList<GoogleWiFiAccessPoint> = arrayListOf()
)

