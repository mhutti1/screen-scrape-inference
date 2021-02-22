package xyz.hutt.meng

import xyz.hutt.meng.models.NoProcessing
import xyz.hutt.meng.models.TFLite
import kotlin.reflect.KClass

enum class Processors(
  className: String,
  bitmapProcessor: BitmapProcessor,
  c: KClass<out ProcessorOutput>
) : ProcessorsInterface by Processor(className, bitmapProcessor, c) {
  NOPROCESSING(
    "No Processing",
    NoProcessing(), MainService.OverlayOverlayOutput::class
  ),
  TFLITE("TFLITE", TFLite(), MainService.PIPOverlayOutput::class);
}