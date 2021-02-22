package xyz.hutt.meng

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt


class Box : View {

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  lateinit var function: (Rect) -> Unit

  var scaling = false
  override fun onTouchEvent(ev: MotionEvent): Boolean {
    if (scaling) {
      return false
    }
    x = ev.rawX - width / 2
    y = ev.rawY - height / 2
    updateCaptureBox()
    return true
  }

  fun updateCaptureBox() {
    val location = IntArray(2)
    getLocationOnScreen(location)
    function(
      Rect(
        location[0], location[1], (location[0] + width * scaleX).roundToInt(),
        (location[1] + height * scaleY).roundToInt()
      )
    )
  }

  fun setMoveListener(function: (Rect) -> Unit) {
    this.function = function
  }
}