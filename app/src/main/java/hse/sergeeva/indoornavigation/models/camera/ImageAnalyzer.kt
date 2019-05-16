package hse.sergeeva.indoornavigation.models.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*


class ImageAnalyzer {
    val message = mutableListOf<Int>()
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

    private fun analyze(fileName: String?) : Boolean {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(fileName)
            for (time in 0 until 5 * 1000 * 1000 step 300 * 1000) {
                val image = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
                analyzeImage(image)
            }
        } catch (ex: Exception) {
            Log.e("VLCLocationManager", "Error in analyze")
            return false
        }
        return true
    }

    private fun analyzeImage(bitmapImage: Bitmap) {
        try {
            saveImage(bitmapImage, "test.png")

            var meanRed: Long = 0
            var meanGreen: Long = 0
            var meanBlue: Long = 0
            for (i in bitmapImage.width / 2 - 50 until bitmapImage.width / 2 + 50) {
                for (j in bitmapImage.height / 2 - 50 until bitmapImage.height / 2 + 50) {
                    val color = bitmapImage.getPixel(i, j)
                    meanRed += Color.red(color)
                    meanBlue += Color.blue(color)
                    meanGreen += Color.green(color)
                }
            }
            val pixCount: Int = 100 * 100//bitmapImage.width / 5 * bitmapImage.height / 5

            if (meanRed / pixCount < 128 && meanGreen / pixCount < 128 && meanBlue / pixCount < 128)
                message += 0
            else
                message += 1

        } catch (ex: Exception) {
            Log.d("ImageListener", "Error reading image")
        }
    }

    fun saveImage(bitmap: Bitmap, name: String) {
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
