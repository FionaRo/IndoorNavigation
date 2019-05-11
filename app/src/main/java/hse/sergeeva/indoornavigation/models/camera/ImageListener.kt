package hse.sergeeva.indoornavigation.models.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.Image
import android.media.ImageReader
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.ByteBuffer

class ImageListener : ImageReader.OnImageAvailableListener {
    companion object {
        val message = mutableListOf<Int>()
    }

    override fun onImageAvailable(reader: ImageReader?) {
        Log.d("Camera", "Image")

        try {
            val image: Image = reader!!.acquireLatestImage()

            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            val bitmapImage: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

            image.close()
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