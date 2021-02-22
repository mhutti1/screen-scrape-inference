package xyz.hutt.meng

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.FloatBuffer

class Utils {

  companion object {
    fun bufferToBitmap(buffer: FloatBuffer, width: Int, height: Int): Bitmap {
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      if (bitmap.width == width && bitmap.height == height) {
        val intValues = IntArray(width * height)
        var i = 0
        while (i < intValues.size) {
          val r = buffer[i].toInt()
          intValues[i] = Color.rgb(r, r, r)
          ++i
        }
        bitmap.setPixels(intValues, 0, width, 0, 0, width, height)
      }
      return bitmap
    }
  }
}