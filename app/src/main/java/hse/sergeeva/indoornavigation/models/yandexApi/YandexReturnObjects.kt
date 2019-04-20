package hse.sergeeva.indoornavigation.models.yandexApi

class YandexReturnObject(val position: YandexLocation)

class YandexLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val precision: Double,
    val altitude_precision: Double,
    val type: String
)

class YandexReturnError(val error: YandexError)

class YandexError(val code: String, val message: String)