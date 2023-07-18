package com.example.transactionapp.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.transactionapp.DBKey.Companion.DB_ARTICLES
import com.example.transactionapp.adapter.PhotoListAdapter
import com.example.transactionapp.databinding.ActivityAddArticleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class AddArticleActivity : AppCompatActivity() {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1000
        const val GALLERY_REQUEST_CODE = 1001
        const val CAMERA_REQUEST_CODE = 1002
    }

    private var imageUriList: ArrayList<Uri> = arrayListOf()

    private var selectedUri: Uri? = null
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    private lateinit var binding: ActivityAddArticleBinding

    private val photoListAdapter = PhotoListAdapter { uri -> removePhoto(uri) }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddArticleBinding.inflate(layoutInflater)

        setContentView(binding.root)

        initViews()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initViews() = with(binding) {
        photoRecyclerView.adapter = photoListAdapter

        imageAddButton.setOnClickListener {
            showPictureUploadDialog()
        }

        submitButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val sellerId = auth.currentUser?.uid.orEmpty()

            showProgress()
            //중간에 이미지가 있으면 업로드 과정을 추가
            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadPhoto(photoUri,
                    successHandler = { uri ->
                        uploadArticle(sellerId, title, content, uri)
                    },
                    errorHandler = {
                        Toast.makeText(
                            this@AddArticleActivity,
                            "사진 업로드에 실패했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        hideProgress()
                    }
                )
            } else {
                uploadArticle(sellerId, title, content, "")
            }
        }
    }

    private fun uploadPhoto(
        uri: Uri,
        successHandler: (String) -> Unit,
        errorHandler: () -> Unit
    ) { // 사진 추가 버튼 눌렀을 때
        val fileName = "${System.currentTimeMillis()}.png"
        storage.reference.child("article/photo").child(fileName)
            .putFile(uri)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    storage.reference.child("article/photo").child(fileName)
                        .downloadUrl
                        .addOnSuccessListener { uri ->
                            successHandler(uri.toString())
                        }.addOnFailureListener {
                            errorHandler()
                        }
                } else {
                    errorHandler()
                }
            }
    }

    private fun uploadArticle(
        sellerId: String,
        title: String,
        content: String,
        imageUri: String
    ) { // 물품 업로드
        val model = ArticleModel(sellerId, title, System.currentTimeMillis(), content, imageUri)
        articleDB.push().setValue(model)

        hideProgress()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1010 ->
                if (grantResults.isNotEmpty()) {
                    startContentProvider()
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            GALLERY_REQUEST_CODE -> {
                data?.let {
                    val uriList = it.getParcelableArrayListExtra<Uri>("uriList")
                    uriList?.let { list ->
                        imageUriList.addAll(list)
                        photoListAdapter.setPhotoList(imageUriList)
                    }
                } ?: kotlin.run {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_REQUEST_CODE -> {
                data?.let {
                    val uriList = it.getParcelableArrayListExtra<Uri>("uriList")
                    uriList?.let { list ->
                        imageUriList.addAll(list)
                        photoListAdapter.setPhotoList(imageUriList)
                    }
                } ?: kotlin.run {
                    Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startContentProvider() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 2020)
    }

    private fun startGalleryScreen(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 2020)
    }

    private fun startCameraScreen(){

    }

    private fun showProgress() { // 로딩창 o
        binding.progressBar.isVisible = true
    }

    private fun hideProgress() { // 로딩창 x
        binding.progressBar.isVisible = false
    }

    @Deprecated("Deprecated in Java")


    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPermissionContextPopup() { // 권한 동의x 를 누른 후 띄워지는 팝업
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("사진을 가져오기 위해 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1010)
            }
            .create()
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkExternalStoragePermission(uploadAction: () -> Unit){
        when {
            ContextCompat.checkSelfPermission( // 사진 갤러리 사용 권한 체크
                this@AddArticleActivity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                uploadAction()
            }

            shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                showPermissionContextPopup()
            }

            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    1010
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showPictureUploadDialog() {
        AlertDialog.Builder(this)
            .setTitle("사진첨부")
            .setMessage("사진첨부할 방식을 선택하세요.")
            .setPositiveButton("카메라") { _, _ ->
                checkExternalStoragePermission {
                    startCameraScreen()
                }
            }
            .setNegativeButton("갤러리") { _, _ ->
                checkExternalStoragePermission{
                    startGalleryScreen()
                }
            }
            .create()
            .show()
    }

    private fun removePhoto(uri: Uri) {
        imageUriList.remove(uri)
        photoListAdapter.setPhotoList(imageUriList)
    }
}