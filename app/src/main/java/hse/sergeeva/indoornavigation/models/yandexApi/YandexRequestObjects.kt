package hse.sergeeva.indoornavigation.models.yandexApi

class YandexGeolocationRequestBody(
    val common: YandexCommonObject,
    val gsm_cells: ArrayList<YandexCellTower> = arrayListOf(),
    val wifi_networks: ArrayList<YandexWifiPoint> = arrayListOf()
)

class YandexCommonObject(val version: String = "1.0", val api_key: String)

class YandexCellTower(
    val countrycode: Int,
    val operatorid: Int,
    val cellid: Int,
    val lac: Int,
    val signal_strength: Int = 0,
    val age: Int = 0
)

class YandexWifiPoint(
    val mac: String,
    val signal_strength: Int = 0,
    val age: Int = 0
)