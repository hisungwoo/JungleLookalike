package com.example.junglelookalike

import android.Manifest
import android.R
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.junglelookalike.databinding.ActivityMainBinding
import kotlin.math.min


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



        mainViewModel.resultLiveData.observe(this, Observer {
            val resultTitle = it[0]
            binding.testId.text = it[1]
            binding.testId2.text = "나는 밀림에서 $resultTitle 입니다!!"
        })

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

    //사진찍기 or 앨범에서 가져오기 선택 다이얼로그
    private fun photoDialogRadio() {
        val PhotoModels = arrayOf<CharSequence>("갤러리에서 가져오기", "카메라로 촬영 후 가져오기", "기본사진으로 하기")
        val alt_bld: AlertDialog.Builder = AlertDialog.Builder(this)
        //alt_bld.setIcon(R.drawable.icon);
        alt_bld.setTitle("프로필사진 설정")
        alt_bld.setSingleChoiceItems(PhotoModels, -1,
            DialogInterface.OnClickListener { dialog, item ->
                // Toast.makeText(ProfileActivity.this, PhotoModels[item] + "가 선택되었습니다.", Toast.LENGTH_SHORT).show();
                if (item == 0) { //갤러리
                    val intent = Intent()
                    intent.type = "image/*"
                    intent.action = Intent.ACTION_GET_CONTENT
//                    startActivityForResult(intent, PICK_IMAGE)
                } else if (item == 1) { //카메라찍은 사진가져오기
//                    takePictureFromCameraIntent()
                } else { //기본화면으로하기
//                    mPhotoImageVIew.setImageResource(R.drawable.ic_add_a_photo_black_24dp)
//                    img = null
                }
            })
        val alert: AlertDialog = alt_bld.create()
        alert.show()
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

                        mainViewModel.submit(image, this)
                    }
                }
            }
        }
    }

}