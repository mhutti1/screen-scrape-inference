package xyz.hutt.meng

import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Process

class BitmapCapture(
  val mediaProjection: MediaProjection,
  val metrics: MainActivity.Metrics,
  val processor: BitmapProcessor,
  val statusBar: Int
) {

  companion object {
    private const val READER_BUFFER_SIZE = 3
  }

  private var imageThread: HandlerThread? = null

  private var imageThreadHandler: Handler? = null

  private var imageDisplay: VirtualDisplay? = null

  private lateinit var imageReader: ImageReader

  fun start(rect: Rect?) {
    imageThread = HandlerThread("BitmapCapture", Process.THREAD_PRIORITY_BACKGROUND)
    imageThread!!.start()

    imageThreadHandler = Handler(imageThread?.looper)

    imageReader = ImageReader.newInstance(
      metrics.width,
      metrics.height,
      PixelFormat.RGBA_8888,
      READER_BUFFER_SIZE
    )

    imageDisplay = mediaProjection.createVirtualDisplay(
      "TEST",
      metrics.width,
      metrics.height,
      metrics.densityDpi,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION,
      imageReader.surface,
      null,
      imageThreadHandler
    )

    imageReader.setOnImageAvailableListener(
      ImageListener { time ->
        if (rect != null) {
          processor.processBitmap(this, rect, time)
        } else {
          processor.processBitmap(this, time)
        }
      },
      imageThreadHandler
    )

  }

  fun stop() {
    imageThread?.quit()
    imageDisplay?.release()
  }

  private inner class ImageListener constructor(val process: Bitmap.(Long) -> Unit) :
    ImageReader.OnImageAvailableListener {
    override fun onImageAvailable(reader: ImageReader?) {
      synchronized(this@BitmapCapture) {
        reader?.acquireLatestImage()?.let {
          val bitmap = getBitmap(it)
          process(bitmap, it.timestamp)
          it.close()
        }
      }
    }

    lateinit var bitmap: Bitmap
    lateinit var output: Bitmap
    val canvas = Canvas()


    fun getBitmap(image: Image): Bitmap {
      val plane = image.planes[0]
      val buffer = plane.buffer
      val rowPadding = plane.rowStride - plane.pixelStride * image.width

      // For some reason the screenshot is slightly too large sometimes
      if (!this::bitmap.isInitialized) {
        bitmap = Bitmap.createBitmap(
          image.width + rowPadding / plane.pixelStride,
          image.height,
          Bitmap.Config.ARGB_8888
        )
      }
      bitmap.copyPixelsFromBuffer(buffer)

      if (!this::output.isInitialized) {
        output =
          Bitmap.createBitmap(metrics.width, metrics.height - statusBar, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(output)
      }
      canvas.drawBitmap(
        bitmap, Rect(0, statusBar, bitmap.width, bitmap.height),
        RectF(0f, 0f, bitmap.width.toFloat(), canvas.height.toFloat()), Paint()
      )
      return output
//      return Bitmap.createBitmap(bitmap, 0, 0, metrics.width, metrics.height)
    }
  }
}