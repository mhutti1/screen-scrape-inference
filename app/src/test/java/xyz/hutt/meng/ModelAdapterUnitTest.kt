package xyz.hutt.meng

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelAdapterUnitTest {
  @Test
  fun testModelAdapter() {
    val m = ModelAdapter(arrayOf(Processors.SONONET), { _, _ -> })
    assertEquals(1, m.itemCount)
  }
}
