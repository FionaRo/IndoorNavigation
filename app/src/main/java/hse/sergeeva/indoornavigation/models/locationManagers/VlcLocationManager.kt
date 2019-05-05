package hse.sergeeva.indoornavigation.models.locationManagers

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import hse.sergeeva.indoornavigation.models.camera.CameraCallback
import hse.sergeeva.indoornavigation.models.camera.DeviceCallback
import hse.sergeeva.indoornavigation.models.camera.ImageListener
import hse.sergeeva.indoornavigation.presenters.decoders.ManchesterDecoder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception

class VlcLocationManager(private val context: Context) : ILocationManager {

    init {
        openCamera()

        GlobalScope.launch {
            try {
                while (true) {
                    DeviceCallback.makeRequest()
                    Thread.sleep(500)
                }
            } catch (ex: Exception) {
                Log.d("VlcLocationManager", ex.message)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraManager = context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val list = cameraManager.cameraIdList
        val callback = CameraCallback()
        cameraManager.openCamera(list[0], callback, null)
    }

    override fun getLocation(): Boolean {
        if (ImageListener.message.size < 8 + 8 + 3) return true

        val msg = ImageListener.message.toList()
        val code = ManchesterDecoder.decode(msg)
        if (code == -1) return false
        return true
    }


    override fun stopScan() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}