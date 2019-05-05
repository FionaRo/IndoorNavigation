package hse.sergeeva.indoornavigation.models.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.util.Log

class CaptureRequestCallback : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
        Log.d("CaptureRequestCallback", "Capture Failed")
        super.onCaptureFailed(session, request, failure)
    }
}
