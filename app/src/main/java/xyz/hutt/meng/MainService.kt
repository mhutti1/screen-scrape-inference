package xyz.hutt.meng

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.roundToInt


class MainService :
  Service() {

  private var button: Button? = null
  private var button2: Button? = null
  private var classificationView: TextView? = null
  private var fpsView: TextView? = null
  private var picInPic: ImageView? = null
  private var overlayPic: ImageView? = null
  var bitmapCapture: BitmapCapture? = null

  private var windows: MutableList<View> = ArrayList()

  var recording = false

  var box: Rect? = null

  val CHANNEL_ID = "MENG-ID"

  var counter = 0;

  lateinit var processor: Processors

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val mediaProjectionManager =
      applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    val mediaProjection =
      mediaProjectionManager.getMediaProjection(intent!!.getIntExtra("RESULT_CODE", 0), intent)
    val metrics = intent.getSerializableExtra("METRICS") as MainActivity.Metrics
    processor = intent.getSerializableExtra("CLASS") as Processors

    createScreenOverlay(processor.getWidth(), processor.getHeight())
    createLocalisationOverlay()
    bitmapCapture = BitmapCapture(
      mediaProjection,
      metrics,
      processor.initProcessor(this),
      MainActivity.statusBarSize
    )


    createNotificationChannel()
    val notification: Notification = Notification.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle("Title")
      .setContentText("Contet")
      .build()

    startForeground(16, notification)
    return START_NOT_STICKY
  }

  private fun createNotificationChannel() {
    val serviceChannel = NotificationChannel(
      CHANNEL_ID,
      "Foreground Service Channel",
      NotificationManager.IMPORTANCE_DEFAULT
    )
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(serviceChannel)
  }

  val planes = HashMap<String, Pair<Float, Bitmap>>()
  val files = HashMap<String, File>()

  open inner class PIPOverlayOutput : ProcessorOutput {
    override fun output(
      string: String,
      confidence: Float,
      rectangles: List<Pair<RectF, String>>,
      overlayBitmap: Bitmap?,
      screenBitmap: Bitmap,
      overlayList: LinkedList<Pair<Long, Bitmap?>>
    ) {
      Handler(Looper.getMainLooper()).post {
        classificationView?.text = string
      }
      localise(rectangles, screenBitmap, Color.RED, 128, overlayBitmap, overlayList)
    }

    override fun localise(
      rectangles: List<Pair<RectF, String>>,
      bitmap: Bitmap,
      color: Int,
      alpha: Int,
      overlayBitmap: Bitmap?,
      overlayList: LinkedList<Pair<Long, Bitmap?>>
    ) {
      val canvas = Canvas()
      canvas.setBitmap(bitmap)

      overlayBitmap?.let {
        val p = Paint()
        p.alpha = 128
        canvas.drawBitmap(it.scale(bitmap.width, bitmap.height), 0F, 0F, p)
      }
      val paint = Paint()
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 10F
      paint.color = color
      paint.alpha = alpha
      //paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_ATOP))

      val text = Paint()
      text.textSize = 100F
      text.color = color
      text.alpha = alpha
      //text.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_ATOP))
      rectangles.forEach {
        canvas.drawRect(it.first, paint)
        canvas.drawText(it.second, it.first.centerX() - 100, it.first.bottom + 100, text)
      }

      Handler(Looper.getMainLooper()).post {
        picInPic?.setImageBitmap(bitmap)
      }
    }
  }

  inner class OverlayOverlayOutput : PIPOverlayOutput() {
    val uiActionMonitor: Any = Any()

    override fun localise(
      rectangles: List<Pair<RectF, String>>,
      bitmap: Bitmap,
      color: Int,
      alpha: Int,
      overlayBitmap: Bitmap?,
      overlayList: LinkedList<Pair<Long, Bitmap?>>
    ) {
      val canvas = Canvas()
      val transparent = Bitmap.createBitmap(
        bitmap.width,
        bitmap.height,
        Bitmap.Config.ARGB_8888
      )
      canvas.setBitmap(transparent)

      overlayBitmap?.let {
        val p = Paint()
        p.alpha = 128
        canvas.drawBitmap(it.scale(bitmap.width, bitmap.height, false), 0F, 0F, p)
      }

      val paint = Paint()
      paint.style = Paint.Style.STROKE
      paint.strokeWidth = 10F
      paint.color = color
      paint.alpha = alpha
      // Ensure that boxes don't add their colour to each other
      paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_ATOP))


      val text = Paint()
      text.textSize = 100F
      text.color = color
      text.alpha = alpha
      text.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_ATOP))

      // TODO: Offset viewfinder
      rectangles.forEach {
        //paint.color = Color.red(255)
        //canvas.drawRect(it.first, paint)
        //paint.color = Color.red(-255)
        canvas.drawRect(it.first, paint)
        canvas.drawText(it.second, it.first.centerX() - 100, it.first.bottom + 100, text)
      }

      val point = Paint()
      //point.strokeWidth = 1f;
      // Encode counter info in first pixel
      point.color = Color.rgb((counter / (255 * 255)) % 255, (counter / 255) % 255, counter % 255)
      canvas.drawPoint(0f, 0f, point)

      val n = System.nanoTime()
      val m = System.currentTimeMillis()
      overlayList.add(Pair((counter++).toLong(), transparent))
      Handler(Looper.getMainLooper()).post {
        overlayPic?.setImageBitmap(transparent)
        //overlayPic?.post { overlayList.add(Pair(n + (15 + System.currentTimeMillis() - m) * 1000000, transparent))}
        Log.d("Test", "Immediate: " + System.currentTimeMillis().toString())
        picInPic?.setImageBitmap(bitmap)
      }
    }
  }

  var miliseconds = 1000L
  var lastFps = 0
  var lastMiliseconds = 0L

  private fun toggle() {
    if (recording) {
      processor.getProcessor()
      bitmapCapture?.stop()
      processor.getProcessor().running = false
      button2?.foreground = getDrawable(android.R.drawable.presence_video_online)
      updateFps(true)
    } else {
      processor.getProcessor().running = true
      bitmapCapture?.start(box)
      button2?.foreground = getDrawable(android.R.drawable.presence_video_busy)
      Thread {
        while (processor.getProcessor().running) {
          updateFps(false); Thread.sleep(5000)
        }
      }.start()
    }
    recording = !recording
  }

  private fun updateFps(total: Boolean) {
    val timeDiff = System.currentTimeMillis() - lastMiliseconds
    val fps = processor.getProcessor().fps
    if (lastMiliseconds > 0L) {
      miliseconds += timeDiff
    }
    if (total) {
      fpsView?.text =
        String.format("%.2f FPS", 1000 * processor.getProcessor().fps / miliseconds.toFloat())
    } else {
      fpsView?.text =
        String.format("%.1f FPS", 1000 * (fps - lastFps) / timeDiff.toFloat())
    }
    lastFps = fps
    lastMiliseconds = System.currentTimeMillis()
  }

  private fun createLocalisationOverlay() {
    if (!Settings.canDrawOverlays(this)) {
      throw RuntimeException("Fix")
    }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
          or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
          or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Needed for keyboard
      PixelFormat.TRANSLUCENT
    )
    Handler(Looper.getMainLooper()).post {

      val windowManager =
        applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

      val overlayView = LayoutInflater.from(applicationContext)
        .inflate(R.layout.overlay_overlay, null)
      windows.add(overlayView)

      overlayPic = overlayView.findViewById(R.id.canvas) as ImageView
      windowManager.addView(overlayView, params)
    }
  }

  private fun stop() {
    val windowManager =
      applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windows.forEach(windowManager::removeViewImmediate)
    stopSelf()
  }

  private fun createScreenOverlay(width: Int, height: Int) {
    if (!Settings.canDrawOverlays(this)) {
      throw RuntimeException("Fix")
    }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
          or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
          or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Needed for keyboard
      PixelFormat.TRANSLUCENT
    )

    params.gravity = Gravity.BOTTOM

    Handler(Looper.getMainLooper()).post {

      val windowManager =
        applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

      val overlayView = LayoutInflater.from(applicationContext)
        .inflate(R.layout.overlay_main, null)

      windows.add(overlayView)
      button = overlayView.findViewById(R.id.button)
      picInPic = overlayView.findViewById(R.id.imageView)
      classificationView = overlayView.findViewById(R.id.classification)
      fpsView = overlayView.findViewById(R.id.fps)

      button2 = overlayView.findViewById(R.id.button2) as Button
      button2!!.setOnClickListener {
        toggle()
      }


      button2!!.setOnLongClickListener {
        stop()
        return@setOnLongClickListener true
      }


      val params2 = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // Needed for keyboard
        PixelFormat.TRANSLUCENT
      )


      val overlayBoxView = LayoutInflater.from(applicationContext)
        .inflate(R.layout.overlay_box, null)


      val boxView = overlayBoxView.findViewById<View>(R.id.view)
      val param = boxView.layoutParams
      param.width = (width * getResources().getDisplayMetrics().density).roundToInt();
      param.height = (height * getResources().getDisplayMetrics().density).roundToInt();
      boxView.layoutParams = param



      val view = overlayBoxView.findViewById(R.id.view) as Box
      view.setMoveListener {
        box = it
        overlayPic?.x = it.left.toFloat()
        overlayPic?.y = it.top.toFloat()
      }

      overlayBoxView.setOnTouchListener { v, ev ->
        if (ev.action == MotionEvent.ACTION_DOWN) {
          view.scaling = true
        }
        if (ev.action == MotionEvent.ACTION_UP) {
          view.scaling = false
        }
        if (ev.action == MotionEvent.ACTION_MOVE) {
          if (ev.historySize != 0) {
            val scale = view.scaleX + (ev.x - ev.getHistoricalX(0)) / 100
            if (scale > 0) {
              view.scaleX = scale
              view.scaleY = scale
              view.updateCaptureBox()
            }
          }
        }
        true
      }

      button!!.setOnClickListener {
        if (windows.contains(overlayBoxView)) {
          windows.remove(overlayBoxView)
          windowManager.removeViewImmediate(overlayBoxView)
        } else {
          windows.add(overlayBoxView)

          // Need to remove overlay so we can add back in right order so buttons are clickable
          windowManager.removeViewImmediate(overlayView)

          // Sleep to allow access to under view if needed by user
          Thread.sleep(500)

          windowManager.addView(overlayBoxView, params2)
          windowManager.addView(overlayView, params)
        }
      }

      button!!.setOnLongClickListener {
        val builder = AlertDialog.Builder(applicationContext)
        builder.setTitle("Captured Planes")
        true
      }
      windowManager.addView(overlayView, params)
    }
  }

  override fun onBind(p0: Intent?): IBinder? {
    throw Exception("Bind not Implemented")
  }
}