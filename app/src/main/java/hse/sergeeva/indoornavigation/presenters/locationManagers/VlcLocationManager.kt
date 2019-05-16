package hse.sergeeva.indoornavigation.presenters.locationManagers

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import hse.sergeeva.indoornavigation.models.camera.ImageListener
import hse.sergeeva.indoornavigation.models.decoders.ManchesterDecoder
import hse.sergeeva.indoornavigation.presenters.UiRunner
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class VlcLocationManager(private val context: Context) : ILocationManager {

    private val mediaRecorder = MediaRecorder()
    private var cameraDevice: CameraDevice? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var recording: Boolean = false
    private var previewSession: CameraCaptureSession? = null
    private var lastFile: File? = null
    private var scanStopped: Boolean = false

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(device: CameraDevice) {
            cameraDevice = device
        }

        override fun onDisconnected(device: CameraDevice) {
            device.close()
            cameraDevice = null
        }

        override fun onError(device: CameraDevice, error: Int) {
            device.close()
            cameraDevice = null
            Log.d("VLCLocationManager", "Error in Camera")
        }

    }

    init {
        openCamera()
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread?.start()
        backgroundHandler = Handler(backgroundThread!!.looper)

        GlobalScope.launch {
            try {
                while (!scanStopped) {
                    if (startRecording()) {
                        Thread.sleep(30000)
                        stopRecording()
                    } else
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
        cameraManager.openCamera(list[0], stateCallback, null)
    }

    private fun startRecording(): Boolean {
        if (cameraDevice == null) return false
        recording = true

        try {
            setUpMediaRecorder()
            previewBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

            val recordSurface = mediaRecorder.surface
            previewBuilder?.addTarget(recordSurface)
            cameraDevice?.createCaptureSession(listOf(recordSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    previewSession = session
                    updatePreview()
                    UiRunner.runOnUiThread {
                        mediaRecorder.start()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("VLCLocationManager", "onConfigurationFailed")
                }
            }, backgroundHandler)

        } catch (ex: Exception) {
            Log.e("VLCLocationManager", ex.message)
            return false
        }
        recording = true
        return true
    }

    private fun stopRecording() {
        try {
            previewSession?.stopRepeating()
            previewSession?.abortCaptures()
            mediaRecorder.stop()
            mediaRecorder.reset()

            if (cameraDevice != null) {
                cameraDevice?.close()
                cameraDevice = null
            }
            mediaRecorder.release()
        } catch (ex: Exception) {
            Log.e("VLCLocationManager", ex.message)
        }

        recording = false
    }

    private fun updatePreview() {
        if (cameraDevice == null || previewBuilder == null) return
        try {
            previewBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            val request = previewBuilder?.build() ?: return
            previewSession?.setRepeatingRequest(
                request,
                null,
                backgroundHandler
            )
        } catch (ex: Exception) {
            Log.e("VLCLocationManager", ex.message)
        }
    }

    private fun setUpMediaRecorder() {

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        lastFile = getOutputFile()

        mediaRecorder.setOutputFile(lastFile!!.path)
        val profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
        mediaRecorder.setVideoFrameRate(profile.videoFrameRate)
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate)
        mediaRecorder.setAudioSamplingRate(profile.audioSampleRate)
        mediaRecorder.setOrientationHint(90)

        mediaRecorder.prepare()
    }

    private fun getOutputFile(): File {
        val mediaStorage =
            File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                ).path +
                        File.separator + "IndoorVideos"
            )

        if (!mediaStorage.exists())
            mediaStorage.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val mediaFile = File(mediaStorage.path + File.separator + "VID_" + timeStamp + ".mp4")
        return mediaFile
    }

    override fun getLocation(): Boolean {
        if (ImageListener.message.size < 150) return true

        val msg = ImageListener.message.toList()
        val code = ManchesterDecoder.decode(msg)
        if (code == -1) return false

        ImageListener.message.clear()
        return true
    }

    override fun stopScan() {
        scanStopped = true

        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (ex: Exception) {
            Log.e("VLCLocationManager", ex.message)
        }
    }

}