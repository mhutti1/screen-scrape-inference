package xyz.hutt.meng

import android.graphics.Bitmap
import android.graphics.RectF
import java.util.*

interface ProcessorOutput {
  fun output(
    string: String,
    confidence: Float,
    rectangles: List<Pair<RectF, String>>,
    overlayBitmap: Bitmap?,
    screenBitmap: Bitmap,
    overlayList: LinkedList<Pair<Long, Bitmap?>>
  )

  fun localise(
    rectangles: List<Pair<RectF, String>>,
    bitmap: Bitmap,
    color: Int,
    alpha: Int,
    overlayBitmap: Bitmap?,
    overlayList: LinkedList<Pair<Long, Bitmap?>>
  )
}