package hse.sergeeva.indoornavigation.models.camera

import android.graphics.ImageFormat
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.util.Log

class CameraCallback : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        val imageReader = ImageReader.newInstance(100, 100, ImageFormat.JPEG, 10)
        imageReader.setOnImageAvailableListener(ImageListener(), null)
        val target = arrayOf(imageReader.surface).toMutableList()

        val callback = DeviceCallback(imageReader)
        camera.createCaptureSession(target, callback, null)
    }

    override fun onDisconnected(camera: CameraDevice) {
        Log.d("CameraCallback", "Camera disconnected")
    }

    override fun onError(camera: CameraDevice, error: Int) {
        Log.d("CameraCallback", "Camera error: $error")
    }

}
