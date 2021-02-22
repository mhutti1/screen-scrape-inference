package xyz.hutt.meng

import androidx.test.core.view.MotionEventBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoxInstrumentedTest {
  val appContext = InstrumentationRegistry.getInstrumentation().targetContext

  @Test
  fun testBoxMove() {
    val box = Box(appContext)
    box.setMoveListener { }
    box.layout(0, 0, 20, 20)
    assertEquals(0F, box.x)
    assertEquals(0F, box.y)
    box.dispatchTouchEvent(MotionEventBuilder.newBuilder().setPointer(20F, 20F).build())
    assertEquals(10F, box.x)
    assertEquals(10F, box.y)
  }
}