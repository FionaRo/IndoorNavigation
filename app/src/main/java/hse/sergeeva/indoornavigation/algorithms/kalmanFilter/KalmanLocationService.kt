package hse.sergeeva.indoornavigation.algorithms.kalmanFilter

import android.content.Context
import android.hardware.*
import android.location.Location
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.concurrent.PriorityBlockingQueue

class KalmanLocationService(context: Context, val onKalmanLocation: (Location) -> Unit) : SensorEventListener {

    enum class ServiceStatus private constructor(value: Int) {
        PERMISSION_DENIED(0),
        SERVICE_STOPPED(1),
        SERVICE_STARTED(2),
        HAS_LOCATION(3),
        SERVICE_PAUSED(4);

        var value: Int = 0
            private set(value) {
                field = value
            }

        init {
            this.value = value
        }
    }

    private val rotationMatrix = FloatArray(16)
    private val rotationMatrixInv = FloatArray(16)
    private val absAcceleration = FloatArray(4)
    private val linearAcceleration = FloatArray(4)
    private var magneticDeclination = 0.0
    private val tag = "KalmanLocationService"
    private var kalmanFilter: LocationKalmanFilter? = null
    private val sensorDataQueue = PriorityBlockingQueue<SensorLocationDataItem>()
    private var needTerminate: Boolean = false
    private var deltaTMs: Long = 100
    private var lastLocation: Location? = null
    private var serviceStatus = ServiceStatus.SERVICE_STOPPED
    private var lastLocationAccuracy = 0f
    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorTypes = intArrayOf(Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ROTATION_VECTOR)
    private var listSensors: MutableList<Sensor> = mutableListOf()
    private var task: Job? = null

    init {
        for (st in sensorTypes) {
            val sensor = sensorManager.getDefaultSensor(st)
            listSensors.add(sensor)
        }
    }

    fun start() {
        sensorDataQueue.clear()
        serviceStatus = ServiceStatus.SERVICE_STARTED
        for (sensor in listSensors) {
            sensorManager.unregisterListener(this, sensor)
            sensorManager.registerListener(
                this, sensor,
                Utils.hertz2periodUs(Utils.SENSOR_DEFAULT_FREQ_HZ)
            )
        }

        needTerminate = false
        deltaTMs = Utils.SENSOR_POSITION_MIN_TIME.toLong()
        task = GlobalScope.launch {
            try {
                worker()
            } catch (ex: Exception) {

            }
        }
    }

    fun stop() {
        serviceStatus = ServiceStatus.SERVICE_PAUSED
        for (sensor in listSensors)
            sensorManager.unregisterListener(this, sensor)

        needTerminate = true
        task?.cancel()
        sensorDataQueue.clear()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val east = 0
        val north = 1
        val up = 2

        val nowMs = Utils.nano2milli(android.os.SystemClock.elapsedRealtimeNanos())
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                System.arraycopy(event.values, 0, linearAcceleration, 0, event.values.size)
                android.opengl.Matrix.multiplyMV(
                    absAcceleration, 0, rotationMatrixInv,
                    0, linearAcceleration, 0
                )

                if (kalmanFilter != null) {

                    val dataItem = SensorLocationDataItem(
                        nowMs.toDouble(),
                        SensorLocationDataItem.NOT_INITIALIZED,
                        SensorLocationDataItem.NOT_INITIALIZED,
                        SensorLocationDataItem.NOT_INITIALIZED,
                        absAcceleration[north].toDouble(),
                        absAcceleration[east].toDouble(),
                        absAcceleration[up].toDouble(),
                        SensorLocationDataItem.NOT_INITIALIZED,
                        SensorLocationDataItem.NOT_INITIALIZED,
                        SensorLocationDataItem.NOT_INITIALIZED,
                        SensorLocationDataItem.NOT_INITIALIZED,
                        magneticDeclination
                    )
                    sensorDataQueue.add(dataItem)
                }
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                android.opengl.Matrix.invertM(rotationMatrixInv, 0, rotationMatrix, 0)
            }
        }
    }

    fun onLocationChanged(loc: hse.sergeeva.indoornavigation.models.Location) {
        val x = loc.longitude
        val y = loc.latitude
        val posDev = loc.accuracy / 100
        val velErr = posDev * 0.1
        val timeStamp: Long = Utils.nano2milli(android.os.SystemClock.elapsedRealtimeNanos())

        val f = GeomagneticField(
            loc.latitude.toFloat(),
            loc.longitude.toFloat(),
            109.63f,
            timeStamp
        )
        magneticDeclination = f.declination.toDouble()

        if (kalmanFilter == null) {
            kalmanFilter = LocationKalmanFilter(
                Coordinates.longitudeToMeters(x),
                Coordinates.latitudeToMeters(y),
                0.0,
                0.0,
                Utils.ACCELEROMETER_DEFAULT_DEVIATION,
                posDev.toDouble(),
                timeStamp.toDouble(),
                Utils.DEFAULT_VEL_FACTOR,
                Utils.DEFAULT_POS_FACTOR
            )
            return
        }

        val dataItem = SensorLocationDataItem(
            timeStamp.toDouble(), loc.latitude, loc.longitude, 0.0,
            SensorLocationDataItem.NOT_INITIALIZED,
            SensorLocationDataItem.NOT_INITIALIZED,
            SensorLocationDataItem.NOT_INITIALIZED,
            0.0,
            0.0,
            posDev.toDouble(),
            velErr,
            magneticDeclination
        )
        sensorDataQueue.add(dataItem)
    }

    private fun worker() {
        while (!needTerminate) {
            Thread.sleep(deltaTMs)

            var dataItem: SensorLocationDataItem? = sensorDataQueue.poll()
            var lastTimeStamp = 0.0
            while (dataItem != null) {
                if (dataItem.timestamp < lastTimeStamp) {
                    continue
                }

                lastTimeStamp = dataItem.timestamp

                //warning!!!
                if (dataItem.latitude == SensorLocationDataItem.NOT_INITIALIZED) {
                    handlePredict(dataItem)
                } else {
                    handleUpdate(dataItem)
                    val loc = locationAfterUpdateStep(dataItem)
                    onLocationChanged(loc)
                }
                dataItem = sensorDataQueue.poll()
            }
        }
    }

    private fun onLocationChanged(location: Location) {
        if (location.latitude == 0.0 || location.longitude == 0.0 || location.provider != tag) return

        serviceStatus = ServiceStatus.HAS_LOCATION
        lastLocation = location
        lastLocationAccuracy = location.accuracy

        onKalmanLocation(location)
    }

    private fun handlePredict(locationItem: SensorLocationDataItem) {
        kalmanFilter?.predict(locationItem.timestamp, locationItem.absEastAcc, locationItem.absNorthAcc)
    }

    private fun handleUpdate(locationItem: SensorLocationDataItem) {
        val xVel = locationItem.speed * Math.cos(locationItem.course)
        val yVel = locationItem.speed * Math.sin(locationItem.course)

        kalmanFilter?.update(
            locationItem.timestamp,
            Coordinates.longitudeToMeters(locationItem.logitude),
            Coordinates.latitudeToMeters(locationItem.latitude),
            xVel,
            yVel,
            locationItem.posErr,
            locationItem.velErr
        )
    }

    private fun locationAfterUpdateStep(locationItem: SensorLocationDataItem): Location {
        val loc = Location(tag)

        val point = Coordinates.metersToLatLng(
            kalmanFilter!!.getCurrentX(),
            kalmanFilter!!.getCurrentY()
        )
        val xVel: Double = kalmanFilter!!.getCurrentXVel()
        val yVel: Double = kalmanFilter!!.getCurrentYVel()
        loc.latitude = point.latitude
        loc.longitude = point.longitude
        loc.altitude = locationItem.altitude
        val speed = Math.sqrt(xVel * xVel + yVel * yVel) //scalar speed without bearing
        loc.bearing = locationItem.course.toFloat()
        loc.speed = speed.toFloat()
        loc.time = System.currentTimeMillis()
        loc.elapsedRealtimeNanos = System.nanoTime()
        loc.accuracy = locationItem.posErr.toFloat()

        return loc
    }
}