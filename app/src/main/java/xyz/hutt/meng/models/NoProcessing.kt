package xyz.hutt.meng.models

import android.graphics.Bitmap
import android.graphics.RectF
import xyz.hutt.meng.BitmapProcessor

class NoProcessing() : BitmapProcessor() {

  var i = 0

  override fun process(bitmap: Bitmap): ProcessResult {
    val inc = ((i++) * 30) % 400
    return ProcessResult(
      "?",
      arrayListOf(Pair(RectF(100F + inc, 100F, 400F + inc, 400F), "Test")),
      1F,
      0F,
      null
    )
  }

  override fun initModel(gpu: Boolean) {

  }
}