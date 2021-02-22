package xyz.hutt.meng.models

import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import xyz.hutt.meng.BitmapProcessor
import java.util.*


class TFLite : BitmapProcessor() {

  lateinit var inputImageBuffer: TensorImage
  private lateinit var tfliteOptions: Interpreter.Options
  private lateinit var tflite: Interpreter
  private lateinit var outputProbabilityBuffer: TensorBuffer
  private lateinit var probabilityProcessor: TensorProcessor

  private var imageSizeX = 0
  private var imageSizeY = 0

  private val IMAGE_MEAN = 0.0f
  private val IMAGE_STD = 1.0f
  private val PROBABILITY_MEAN = 0.0f
  private val PROBABILITY_STD = 255.0f

  override fun initModel(gpu: Boolean) {
    val model = FileUtil.loadMappedFile(context, "mobilenet_v1_1.0_224_quant.tflite")

    tfliteOptions = Interpreter.Options()

    if (gpu) {
      val delegate = GpuDelegate()
      tfliteOptions.addDelegate(delegate)
    } else {
      tfliteOptions.setNumThreads(Runtime.getRuntime().availableProcessors())
    }
    tflite = Interpreter(model, tfliteOptions)
    labels = FileUtil.loadLabels(context, "labels.txt")


    val imageTensorIndex = 0
    val imageShape: IntArray =
      tflite.getInputTensor(imageTensorIndex).shape()
    imageSizeY = imageShape[1]
    imageSizeX = imageShape[2]

    val imageDataType = tflite.getInputTensor(imageTensorIndex).dataType()
    val probabilityTensorIndex = 0
    val probabilityShape = tflite.getOutputTensor(probabilityTensorIndex).shape()
    val probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType()


    inputImageBuffer = TensorImage(imageDataType);
    outputProbabilityBuffer =
      TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)
    probabilityProcessor =
      TensorProcessor.Builder().add(NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)).build()
  }

  override fun process(bitmap: Bitmap): ProcessResult {
    //val area = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height - 500)

    val results = recognizeImage(bitmap)
    return ProcessResult(
      results[0].title,
      ArrayList(),
      results[0].confidence,
      tflite.lastNativeInferenceDurationNanoseconds / 1000000F,
      null
    )
  }

  data class Recognition constructor(val id: String, val title: String, val confidence: Float)

  fun recognizeImage(bitmap: Bitmap): ArrayList<Recognition> {
    inputImageBuffer = loadImage(bitmap)
    tflite.run(inputImageBuffer.buffer, outputProbabilityBuffer.buffer.rewind())
    // Gets the map of label and probability.
    val labeledProbability: Map<String, Float> =
      TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
        .getMapWithFloatValue()


    val maxResults = 3
    // Find the best classifications.
    val pq =
      PriorityQueue(
        maxResults,
        Comparator<Recognition> { lhs, rhs ->
          java.lang.Float.compare(rhs.confidence, lhs.confidence)
        })

    for ((key, value) in labeledProbability.entries) {
      pq.add(Recognition("" + key, key, value))
    }

    val recognitions =
      ArrayList<Recognition>()
    val recognitionsSize = Math.min(pq.size, maxResults)
    for (i in 0 until recognitionsSize) {
      recognitions.add(pq.poll())
    }
    return recognitions
  }

  private fun loadImage(bitmap: Bitmap): TensorImage {
    inputImageBuffer.load(bitmap)
    val cropSize = Math.min(bitmap.width, bitmap.height)
    val imageProcessor: ImageProcessor = ImageProcessor.Builder()
      .add(ResizeWithCropOrPadOp(cropSize, cropSize))
      .add(ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
      .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
      .build()
    return imageProcessor.process(inputImageBuffer)
  }
}