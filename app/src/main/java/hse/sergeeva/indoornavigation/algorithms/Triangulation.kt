package hse.sergeeva.indoornavigation.algorithms

import hse.sergeeva.indoornavigation.algorithms.kalmanFilter.Coordinates
import java.lang.Math.pow

class Triangulation {
    companion object {
        private const val measuredPower = -69
        private const val N = 2

        fun rssiToDistance(rssi: Int): Double {
            if (rssi == 0) return -1.0

            return Math.pow(10.0, 1.0 * (measuredPower - rssi) / (10 * N))
        }

        fun triangulateLocation(
            point1: LatLngDistance,
            point2: LatLngDistance,
            point3: LatLngDistance
        ): LatLng {

            //latlng to Cortesian coords
            var cortesianPoint1 = CortesianCoords(point1)
            var cortesianPoint2 = CortesianCoords(point2)
            var cortesianPoint3 = CortesianCoords(point3)

            //translate coords to point1
            cortesianPoint2 = cortesianPoint2 subtract cortesianPoint1
            cortesianPoint3 = cortesianPoint3 subtract cortesianPoint1

            // rotate
            val d = cortesianPoint2 distTo CortesianCoords(0.0, 0.0, 0.0)
            val ex = cortesianPoint2 deleteBy d
            val i = cortesianPoint3 dot ex
            val exi = ex multiply i
            val ey = (cortesianPoint3 subtract exi) deleteBy (cortesianPoint3 distTo exi)
            val ez = ex cross ey
            val j = ey dot cortesianPoint3

            val x = (pow(point1.distance, 2.0) - pow(point2.distance, 2.0) + pow(d, 2.0)) / (2 * d)
            var y = (pow(point1.distance, 2.0) - pow(point3.distance, 2.0) + pow(i, 2.0) + pow(j, 2.0))
            y = (y / (2 * j)) - ((i / j) * x)

            val z = pow(point1.distance, 2.0) - pow(x, 2.0) - pow(y, 2.0)

            if (z < 0) return LatLngDistance(0.0, 0.0, 0.0)

            val z1 = -Math.sqrt(z)
            val z2 = Math.sqrt(z)

            val triPt1 = cortesianPoint1 add (ex multiply x) add (ey multiply y) add (ez multiply z1)
            val triPt2 = cortesianPoint1 add (ex multiply x) add (ey multiply y) add (ez multiply z2)

            val resultPoint1 = LatLngDistance(
                Math.toDegrees(Math.asin(triPt1.z / Coordinates.EARTH_RADIUS)),
                Math.toDegrees(Math.atan2(triPt1.y, triPt1.x))
            )

            val resultPoint2 = LatLngDistance(
                Math.toDegrees(Math.asin(triPt2.z / Coordinates.EARTH_RADIUS)),
                Math.toDegrees(Math.atan2(triPt2.y, triPt2.x))
            )

            return LatLng(
                (resultPoint1.latitude + resultPoint2.latitude) / 2,
                (resultPoint1.longitude + resultPoint2.longitude) / 2
            )
        }
    }
}