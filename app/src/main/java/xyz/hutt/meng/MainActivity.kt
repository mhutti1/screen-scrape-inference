package xyz.hutt.meng

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.Serializable


class MainActivity : AppCompatActivity() {

  private lateinit var mediaProjectionManager: MediaProjectionManager
  private lateinit var viewManager: RecyclerView.LayoutManager
  private lateinit var viewAdapter: RecyclerView.Adapter<*>

  private lateinit var selectedProcessor: Processors


  companion object {
    var statusBarSize = 0
  }

  override fun onAttachedToWindow() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if (window.decorView.rootWindowInsets.displayCutout != null) {
        statusBarSize = window.decorView.rootWindowInsets.displayCutout.safeInsetTop
      }
    }
  }


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)
    mediaProjectionManager =
      applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager


    if (!Settings.canDrawOverlays(this)) {
      val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName")
      )
      startActivityForResult(intent, 200)
    }


    viewManager = LinearLayoutManager(applicationContext)
    viewAdapter = ModelAdapter(Processors.values()) { proc, debug ->
        selectedProcessor = proc
        startActivityForResult(
          mediaProjectionManager.createScreenCaptureIntent(), 100
        )
    }


    models.apply {
      // Won't grow in size
      setHasFixedSize(true)

      layoutManager = viewManager

      adapter = viewAdapter

    }


  }


  data class Metrics(val width: Int, val height: Int, val densityDpi: Int) : Serializable


  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 100 && data != null) {
      val metrics = DisplayMetrics()
      windowManager.defaultDisplay.getRealMetrics(metrics)
      Intent(this, MainService::class.java).also { intent ->
        intent.putExtras(data)
        intent.putExtra(
          "METRICS",
          Metrics(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
        )
        intent.putExtra("RESULT_CODE", resultCode)
        intent.putExtra("CLASS", selectedProcessor)
        startForegroundService(intent)
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item.itemId) {
      R.id.action_about -> {
        val builder = AlertDialog.Builder(this)
        builder.apply {
          setTitle("About")
          setMessage("This App was created by Isaac Hutt for the final year project of his MEng Computing Degree from Imperial College London")
        }
        builder.show()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
