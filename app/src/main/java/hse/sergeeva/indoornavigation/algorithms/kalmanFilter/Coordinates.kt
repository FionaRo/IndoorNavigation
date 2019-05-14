package hse.sergeeva.indoornavigation.algorithms.kalmanFilter

import hse.sergeeva.indoornavigation.algorithms.LatLng

class Coordinates {

    companion object {
        const val EARTH_RADIUS = 6371.0 * 1000.0 // meters

        fun distanceBetween(lon1: Double, lat1: Double, lon2: Double, lat2: Double): Double {
            val deltaLon = Math.toRadians(lon2 - lon1)
            val deltaLat = Math.toRadians(lat2 - lat1)
            val a = Math.pow(Math.sin(deltaLat / 2.0), 2.0) + Math.cos(Math.toRadians(lat1)) *
                    Math.cos(Math.toRadians(lat2)) *
                    Math.pow(Math.sin(deltaLon / 2.0), 2.0)
            val c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))
            return EARTH_RADIUS * c
        }

        fun metersToLatLng(lonMeters: Double, latMeters: Double): LatLng {
            val point = LatLng(0.0, 0.0)
            val pointEast = pointPlusDistanceEast(point, lonMeters)
            return pointPlusDistanceNorth(pointEast, latMeters)
        }

        fun latitudeToMeters(lat: Double): Double {
            val distance = distanceBetween(0.0, lat, 0.0, 0.0)
            return distance * if (lat < 0.0) -1.0 else 1.0
        }

        fun longitudeToMeters(lon: Double): Double {
            val distance = distanceBetween(lon, 0.0, 0.0, 0.0)
            return distance * if (lon < 0.0) -1.0 else 1.0
        }

        private fun getPointAhead(point: LatLng, distance: Double, azimuthDegrees: Double): LatLng {
            val radiusFraction = distance / EARTH_RADIUS
            val bearing = Math.toRadians(azimuthDegrees)
            val lat1 = Math.toRadians(point.latitude)
            val lng1 = Math.toRadians(point.longitude)

            val lat21 = Math.sin(lat1) * Math.cos(radiusFraction)
            val lat22 = Math.cos(lat1) * Math.sin(radiusFraction) * Math.cos(bearing)
            val lat2 = Math.asin(lat21 + lat22)

            val lng21 = Math.sin(bearing) * Math.sin(radiusFraction) * Math.cos(lat1)
            val lng22 = Math.cos(radiusFraction) - Math.sin(lat1) * Math.sin(lat2)
            var lng2 = lng1 + Math.atan2(lng21, lng22)

            lng2 = (lng2 + 3.0 * Math.PI) % (2.0 * Math.PI) - Math.PI

            return LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2))
        }

        private fun pointPlusDistanceEast(point: LatLng, distance: Double): LatLng {
            return getPointAhead(point, distance, 90.0)
        }

        private fun pointPlusDistanceNorth(point: LatLng, distance: Double): LatLng {
            return getPointAhead(point, distance, 0.0)
        }
    }
}