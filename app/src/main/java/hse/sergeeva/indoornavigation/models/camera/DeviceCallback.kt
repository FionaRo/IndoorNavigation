package hse.sergeeva.indoornavigation.models.camera

import android.app.Activity
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.util.Log
import android.view.Surface
import hse.sergeeva.indoornavigation.presenters.UiRunner
import hse.sergeeva.indoornavigation.views.MainActivity
import java.lang.Exception

class DeviceCallback(imageReader: ImageReader) : CameraCaptureSession.StateCallback() {

    init {
        DeviceCallback.surface = imageReader.surface
    }

    override fun onConfigured(session: CameraCaptureSession) {
        DeviceCallback.session = session
    }

    companion object {
        private var session: CameraCaptureSession? = null
        private var surface: Surface? = null

        fun makeRequest() {
            if (session == null || surface == null) return

            try {
                val captureRequest = session!!.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface!!)
                //captureRequest.set(CaptureRequest.BLACK_LEVEL_LOCK, true)
                //captureRequest.set(
                //    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                //    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                //)

                val callback = CaptureRequestCallback()
                UiRunner.runOnUiThread { session?.capture(captureRequest.build(), callback, null) }
            } catch (ex: Exception) {
                Log.d("DeviceCallback", "Exception in making request")
            }
        }
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        Log.d("DeviceCallback", "Configuration failed")
    }
}
