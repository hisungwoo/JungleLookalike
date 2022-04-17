package com.example.junglelookalike

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.junglelookalike.databinding.ActivityMainBinding
import com.example.junglelookalike.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {
    val mainViewModel : MainViewModel by viewModels()
    lateinit var binding : ActivityMainBinding
    //Manifest 에서 설정한 권한을 가지고 온다.
    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val STORAGE_PERMISSION = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)

    //권한 플래그값 정의
    val FLAG_PERM_CAMERA = 98
    val FLAG_PERM_STORAGE = 99

    //카메라와 갤러리를 호출하는 플래그
    val FLAG_REQ_CAMERA = 101
    val FLAG_REA_STORAGE = 102

    val imageSize = 224


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 화면이 만들어 지면서 정장소 권한을 체크 합니다.
        // 권한이 승인되어 있으면 카메라를 호출하는 메소드를 실행합니다.
        if(checkPermission(STORAGE_PERMISSION,FLAG_PERM_STORAGE)){
            setViews()
        }


//        val model = Model.newInstance(this)
//
//        // Creates inputs for reference.
//        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
//        inputFeature0.loadBuffer(byteBuffer)
//
//        // Runs model inference and gets result.
//        val outputs = model.process(inputFeature0)
//        val outputFeature0 = outputs.outputFeature0AsTensorBuffer
//
//        // Releases model resources if no longer used.
//        model.close()



    }

    private fun setViews() {
        //카메라 버튼 클릭
        binding.mainUploadImg.setOnClickListener {
            //카메라 호출 메소드
            openCamera()
        }
    }

    private fun openCamera() {
        //카메라 권한이 있는지 확인
        if(checkPermission(CAMERA_PERMISSION,FLAG_PERM_CAMERA)){
            //권한이 있으면 카메라를 실행시킵니다.
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent,FLAG_REQ_CAMERA)
        }
    }

    //권한이 있는지 체크하는 메소드
    fun checkPermission(permissions:Array<out String>,flag:Int):Boolean{
        //안드로이드 버전이 마쉬멜로우 이상일때
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            for(permission in permissions){
                //만약 권한이 승인되어 있지 않다면 권한승인 요청을 사용에 화면에 호출합니다.
                if(ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this,permissions,flag)
                    return false
                }
            }
        }
        return true
    }

    //checkPermission() 에서 ActivityCompat.requestPermissions 을 호출한 다음 사용자가 권한 허용여부를 선택하면 해당 메소드로 값이 전달 됩니다.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            FLAG_PERM_STORAGE ->{
                for(grant in grantResults){
                    if(grant != PackageManager.PERMISSION_GRANTED){
                        //권한이 승인되지 않았다면 return 을 사용하여 메소드를 종료시켜 줍니다
                        Toast.makeText(this,"저장소 권한을 승인해야지만 앱을 사용할 수 있습니다..",Toast.LENGTH_SHORT).show()
                        finish()
                        return
                    }
                }
                //카메라 호출 메소드
                setViews()
            }
            FLAG_PERM_CAMERA ->{
                for(grant in grantResults){
                    if(grant != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this,"카메라 권한을 승인해야지만 카메라를 사용할 수 있습니다.",Toast.LENGTH_SHORT).show()
                        return
                    }
                }
                openCamera()
            }
        }
    }

    //startActivityForResult 을 사용한 다음 돌아오는 결과값을 해당 메소드로 호출합니다.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            when(requestCode){
                FLAG_REQ_CAMERA ->{
                    if(data?.extras?.get("data") != null){
                        //카메라로 방금 촬영한 이미지를 미리 만들어 놓은 이미지뷰로 전달 합니다.
                        var image = data?.extras?.get("data") as Bitmap
                        var dimension = min(image.width, image.height)

                        //원하는 크기로 비트맵 생성
//                        image = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
                        binding.mainUploadImg.setImageBitmap(image)

                        // 비트맵 크기 조정
                        image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)


                        try {
                            val buffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
                            buffer.order(ByteOrder.nativeOrder())
                            image.copyPixelsToBuffer(buffer)

                            val model = Model.newInstance(this)

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

                            val resultArray = arrayOf("여우","하마")
                            Log.d("ttest", "result = $result")
                            val resultTitle = resultArray[result]
                            Log.d("ttest", "resultTitle = $resultTitle")

                            val s = String.format("%s : %.1f%%\n", resultTitle, confidences[0] * 100)
                            val s2 = String.format("%s : %.1f%%\n", "ddd", confidences[1] * 100)
                            binding.testId.text = s
                            binding.testId2.text = "나는 밀림에서 $resultTitle 입니다!!"

                            Log.d("ttest", "matching : $resultTitle - probability: $probability %")


                            // Releases model resources if no longer used.
                            model.close()



//                            // Creates inputs for reference.
//                            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
//                            val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
//                            byteBuffer.order(ByteOrder.nativeOrder())


//                            val intValue = IntArray(imageSize * imageSize)
//                            image.getPixels(intValue, 0, image.width, 0, 0 , image.width, image.height)
//                            var pixel = 0
//
//                            for(i in 0..imageSize) {
//                                for(j in 0..imageSize) {
//                                    var v = intValue[pixel++] // RGB
//                                    byteBuffer.putFloat((1.f / 255.f))
//                                }
//                            }
//
//
//                            inputFeature0.loadBuffer(byteBuffer)
//
//                            // Runs model inference and gets result.
//                            val outputs = model.process(inputFeature0)
//                            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
//
//                            // Releases model resources if no longer used.
//                            model.close()




                        } catch (e : IOException) {
                            Log.d("ttest" , "IOException !!! = $e" )
                        }

                    }
                }
            }
        }
    }

}