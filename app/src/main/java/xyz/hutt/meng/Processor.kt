package xyz.hutt.meng

import kotlin.reflect.KClass

class Processor(
  override val className: String,
  override val bitmapProcessor: BitmapProcessor,
  override val c: KClass<out ProcessorOutput>
) : ProcessorsInterface {
}