package hse.sergeeva.indoornavigation.alorithms

import java.lang.Math.cos
import kotlin.math.sin

class CortesianCoords {

    private val earthR = 6371
    var x: Double = 0.0
    var y: Double = 0.0
    var z: Double = 0.0

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(latlng: LatLngDistance) {
        x = earthR * cos(Math.toRadians(latlng.latitude)) * cos(Math.toRadians(latlng.longitude))
        y = earthR * cos(Math.toRadians(latlng.latitude)) * sin(Math.toRadians(latlng.longitude))
        z = earthR * sin(Math.toRadians(latlng.latitude))
    }

    infix fun add(coords: CortesianCoords) = CortesianCoords(x + coords.x, y + coords.y, z + coords.z)

    infix fun subtract(coords: CortesianCoords) = CortesianCoords(x - coords.x, y - coords.y, z - coords.z)

    infix fun subtract(value: Double) = CortesianCoords(x - value, y - value, z - value)

    infix fun distTo(coords: CortesianCoords) =
        Math.sqrt((x - coords.x) * (x - coords.x) + (y - coords.y) * (y - coords.y) + (z - coords.z) * (z - coords.z))

    infix fun multiply(value: Double) = CortesianCoords(value * x, value * y, value * z)

    infix fun deleteBy(value: Double) = CortesianCoords(x / value, y / value, z / value)

    infix fun dot(coords: CortesianCoords) = x * coords.x + y * coords.y + z * coords.z

    infix fun cross(coords: CortesianCoords) =
        CortesianCoords(y * coords.z - z * coords.y, z * coords.x - x * coords.z, x * coords.y - y * coords.x)
}