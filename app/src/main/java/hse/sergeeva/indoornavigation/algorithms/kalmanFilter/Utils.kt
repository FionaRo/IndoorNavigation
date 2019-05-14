package hse.sergeeva.indoornavigation.algorithms.kalmanFilter

class Utils {

    companion object {
        fun hertz2periodUs(hz: Double) = (1.0e6 / hz).toInt()
        fun nano2milli(nano: Long) = (nano / 1e6).toLong()

        val ACCELEROMETER_DEFAULT_DEVIATION = 0.1
        val SENSOR_POSITION_MIN_TIME = 500
        val SENSOR_DEFAULT_FREQ_HZ = 10.0
        val DEFAULT_VEL_FACTOR = 1.0
        val DEFAULT_POS_FACTOR = 1.0
    }
}