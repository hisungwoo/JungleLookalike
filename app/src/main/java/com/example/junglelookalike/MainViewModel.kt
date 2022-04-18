package com.example.junglelookalike

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.junglelookalike.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToLong

class MainViewModel(application : Application) : AndroidViewModel(application) {
    val resultLiveData = MutableLiveData<Array<String>>()


    fun submit(image : Bitmap, context : Context) {
        val imageSize = 224

        try {
            val buffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            buffer.order(ByteOrder.nativeOrder())
            image.copyPixelsToBuffer(buffer)

            val model = Model.newInstance(context)

            val tfImage = TensorImage.fromBitmap(image)
            val feature = TensorBuffer.createFrom(tfImage.tensorBuffer, DataType.FLOAT32)
            feature.buffer.order(ByteOrder.nativeOrder())

            // Creates inputs for reference.
            val inputFeature0 =
                TensorBuffer.createFixedSize(
                    intArrayOf(1, tfImage.width, tfImage.height, 3),
                    DataType.FLOAT32
                )

            inputFeature0.loadBuffer(feature.buffer)


            val outputs = model.process(inputFeature0)
            val output = outputs.outputFeature0AsTensorBuffer
            val confidences = output.floatArray
            var maximum = 0F
            var matching = 0F
            for (item in confidences.indices) {
                if (confidences[item] > maximum) {
                    matching = item.toFloat()
                    maximum = confidences[item]
                }
            }
            val probability = (maximum * 100.00).roundToLong()
            val result = matching.toInt()

            model.close()

            val titleArray = arrayOf("여우","하마")
            Log.d("ttest", "result = $result")
            val resultTitle = titleArray[result]
            Log.d("ttest", "resultTitle = $resultTitle")

            val s = String.format("%s : %.1f%%\n", resultTitle, confidences[0] * 100)

            val resultArray = arrayOf(resultTitle, s)
            resultLiveData.value = resultArray

            Log.d("ttest", "matching : $resultTitle - probability: $probability %")


            // Releases model resources if no longer used.
            model.close()



        } catch (e : IOException) {
            Log.d("ttest" , "IOException !!! = $e" )
        }
    }

}