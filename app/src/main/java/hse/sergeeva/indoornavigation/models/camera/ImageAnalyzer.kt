package hse.sergeeva.indoornavigation.models.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import hse.sergeeva.indoornavigation.models.decoders.ManchesterDecoder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


class ImageAnalyzer {
    val message = mutableListOf<Int>()
    private val manchesterMessage = mutableListOf<Int>()
    private val queue = mutableListOf<String>()
    private var stopped: Boolean = false

    fun addFile(fileName: String?) {
        if (fileName == null) return

        queue += fileName
    }

    fun start() {
        stopped = false
        GlobalScope.launch {
            while (!stopped) {
                while (queue.size != 0) {
                    val file = queue[0]
                    if (analyze(file))
                        queue.removeAt(0)
                }
                Thread.sleep(100)
            }
        }
    }

    fun stop() {
        stopped = true
        queue.clear()
    }

    fun analyze(fileName: String?) : Boolean {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(fileName)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val milliseconds = duration.toLong()
            for (time in (300 * 1000).toLong() until milliseconds * 1000 step (300 * 1000).toLong()) {
                val image = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST)
                analyzeImage(image)
                //saveImage(image, "${time/(1000)}-${manchesterMessage.last()}.png")
            }

            message += ManchesterDecoder.decode(manchesterMessage)
            manchesterMessage.clear()
        } catch (ex: Exception) {
            Log.e("VLCLocationManager", "Error in analyze")
            return false
        }
        return true
    }

    private fun analyzeImage(bitmapImage: Bitmap) {
        try {
            //saveImage(bitmapImage, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".png")

            var meanRed: Long = 0
            var meanGreen: Long = 0
            var meanBlue: Long = 0
            for (i in 0 until bitmapImage.width step 5) {
                for (j in 0 until bitmapImage.height step 5) {
                    val color = bitmapImage.getPixel(i, j)
                    meanRed += Color.red(color)
                    meanBlue += Color.blue(color)
                    meanGreen += Color.green(color)
                }
            }
            val pixCount: Int = bitmapImage.width / 5 * bitmapImage.height / 5

            meanRed /= pixCount
            meanGreen /= pixCount
            meanBlue /= pixCount

            if (meanRed < 80 && meanGreen < 80 && meanBlue < 80)
                manchesterMessage += 0
            else
                manchesterMessage += 1

        } catch (ex: Exception) {
            Log.d("ImageListener", "Error reading image")
        }
    }

    private fun saveImage(bitmap: Bitmap, name: String) {
        val file =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path +
                        File.separator + "IndoorImages"
            )

        file.mkdir()
        val out = FileOutputStream(file.path + File.separator + name)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
}
