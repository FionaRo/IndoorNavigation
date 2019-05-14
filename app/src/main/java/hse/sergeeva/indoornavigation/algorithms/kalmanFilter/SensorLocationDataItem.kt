package hse.sergeeva.indoornavigation.algorithms.kalmanFilter

class SensorLocationDataItem : Comparable<SensorLocationDataItem> {

    var timestamp: Double = 0.0
        private set(value) {
            field = value
        }
    var latitude: Double = 0.0
        private set(value) {
            field = value
        }
    var logitude: Double = 0.0
        private set(value) {
            field = value
        }
    var gpsAlt: Double = 0.0
        private set(value) {
            field = value
        }
    var absNorthAcc: Double = 0.0
        private set(value) {
            field = value
        }
    var absEastAcc: Double = 0.0
        private set(value) {
            field = value
        }
    var absUpAcc: Double = 0.0
        private set(value) {
            field = value
        }
    var speed: Double = 0.0
        private set(value) {
            field = value
        }
    var course: Double = 0.0
        private set(value) {
            field = value
        }
    var posErr: Double = 0.0
        private set(value) {
            field = value
        }
    var velErr: Double = 0.0
        private set(value) {
            field = value
        }

    companion object {
        const val NOT_INITIALIZED = 361.0
    }

    constructor(
        timestamp: Double,
        latitude: Double, longitude: Double, altitude: Double,
        absNorthAcc: Double, absEastAcc: Double, absUpAcc: Double,
        speed: Double, course: Double,
        posErr: Double, velErr: Double,
        declination: Double
    ) {
        this.timestamp = timestamp
        this.latitude = latitude
        this.logitude = longitude
        this.gpsAlt = altitude
        this.absNorthAcc = absNorthAcc
        this.absEastAcc = absEastAcc
        this.absUpAcc = absUpAcc
        this.speed = speed
        this.course = course
        this.posErr = posErr
        this.velErr = velErr

        this.absNorthAcc = absNorthAcc * Math.cos(declination) + absEastAcc * Math.sin(declination)
        this.absEastAcc = absEastAcc * Math.cos(declination) - absNorthAcc * Math.sin(declination)
    }

    override fun compareTo(other: SensorLocationDataItem): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}