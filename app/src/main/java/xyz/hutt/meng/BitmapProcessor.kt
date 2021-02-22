package xyz.hutt.meng

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.util.*
import kotlin.math.max
import kotlin.math.min

abstract class BitmapProcessor {

  var processorOutput: ProcessorOutput? = null

  var fps = 0


  fun processBitmap(bitmap: Bitmap, time: Long) {
    processBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height - 500), time)
  }

  var averageTime = 0L
  var inferenceTime = 0F
  var lastAverge = 0L
  var lastInference = 0F
  var lastFps = 0


  var running = false

  lateinit var labels: List<String>

  var last = LinkedList<Pair<Long, Bitmap?>>()
  protected lateinit var context: Context

  fun processBitmap(bitmap: Bitmap, rect: Rect, time: Long) {
    if (last.isEmpty()) {
      last.add(Pair(Long.MIN_VALUE, null))
    }


    val countTime = System.currentTimeMillis()
    val croppedBitmap = Bitmap.createBitmap(
      bitmap,
      max(0, rect.left),
      max(0, rect.top),
      min(rect.right - rect.left, bitmap.width - max(0, rect.left)),
      min(rect.bottom - rect.top, bitmap.height - max(0, rect.top))
    )

    // Find rectangles that would have been displayed when picture was taken
//    while (last.isNotEmpty() && last.first.first < time) {
//      if (last.size == 1 || last[1].first > time) {
//        break
//      }
//      last.removeFirst()
//    }
    val pixel = croppedBitmap.getPixel(0, 0);
    val count = (pixel.blue + pixel.green * 255 + pixel.red * 255 * 255).toLong();
    while (last.size > 1 && last.first.first != count) {
      last.removeFirst()
    }


    if (last.first.second != null) {
      Log.e("TAG", " " + count + " " + last.size)
      val size = croppedBitmap.height * croppedBitmap.width * 4


    }

    var result: ProcessResult

    // Retry on Cpu
    try {
      result = process(croppedBitmap)
    } catch (e: Exception) {
      Handler(Looper.getMainLooper()).post {
        val t = Toast.makeText(context, "GPU inference failed. Retrying on CPU", Toast.LENGTH_SHORT)
        t.setGravity(Gravity.TOP, 0, 0)
        t.show()
      }
      initModel(false)
      result = process(croppedBitmap)
    }



    processorOutput?.output(
      result.classification,
      result.probablity,
      result.localisation,
      result.bitmap,
      croppedBitmap,
      last
    )


    if (running) {
      inferenceTime += result.inferenceTime
      averageTime += System.currentTimeMillis() - countTime
      fps++

      if (fps % 20 == 0) {
        val output = "Full: (last: %.4f, total: %.4f) Native: (last: %.4f, total: %.4f)".format(
          (averageTime - lastAverge).toDouble() / (fps - lastFps),
          averageTime.toDouble() / fps,
          (inferenceTime - lastInference).toDouble() / (fps - lastFps),
          inferenceTime.toDouble() / fps
        )
        Log.e("MENG-FPS-OUTPUT", output)
        lastAverge = averageTime
        lastInference = inferenceTime
        lastFps = fps
      }
    }
  }

  fun attachListener(processorOutput: ProcessorOutput) {
    this.processorOutput = processorOutput
  }

  abstract fun process(bitmap: Bitmap): ProcessResult

  fun initProcessor(context: Context, gpu: Boolean) {
    this.context = context
    try {
      initModel(gpu)
    } catch (e: Exception) {
      initModel(false)
    }
  }

  abstract fun initModel(gpu: Boolean)

  open fun width(): Int {
    return 288
  }

  open fun height(): Int {
    return 224
  }

  inner class ProcessResult(
    var classification: String,
    var localisation: List<Pair<RectF, String>>,
    var probablity: Float,
    var inferenceTime: Float,
    var bitmap: Bitmap?
  )
}