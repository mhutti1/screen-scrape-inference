package xyz.hutt.meng

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

interface ProcessorsInterface {

  val className: String
  val bitmapProcessor: BitmapProcessor
  val c: KClass<out ProcessorOutput>

  fun getName(): String {
    return className
  }

  fun getProcessor(): BitmapProcessor {
    return bitmapProcessor
  }

  fun getWidth(): Int {
    return bitmapProcessor.width()
  }

  fun getHeight(): Int {
    return bitmapProcessor.height()
  }

  fun initProcessor(ms: MainService): BitmapProcessor {
    bitmapProcessor.attachListener(c.primaryConstructor?.call(ms)!!)
    bitmapProcessor.initProcessor(ms.applicationContext, true)
    return bitmapProcessor
  }

}