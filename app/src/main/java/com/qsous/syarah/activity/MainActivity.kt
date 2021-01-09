package com.qsous.syarah.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.qsous.syarah.R
import com.qsous.syarah.application.Const
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var cameraUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        galleryButton.setOnClickListener {
            if (haveGalleryStoragePermission()) {
                getImageFromGallery()
            } else {
                requestGalleryPermission()
            }
        }
        cameraButton.setOnClickListener {
            if (haveCameraStoragePermission()) {
                getImageFromCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Const.REQUEST_CODE_MAIN_ACTIVITY_TO_GALLERY) {
            if (resultCode == RESULT_OK) {
                data?.data?.let {
                    startActivity(EditImageActivity.getIntentToStart(this, it))
                }
            }
        } else if (requestCode == Const.REQUEST_CODE_MAIN_ACTIVITY_TO_CAMERA) {
            if (resultCode == RESULT_OK) {
                cameraUri?.let {
                    startActivity(EditImageActivity.getIntentToStart(this, it))
                }
            }
        }
    }

    private fun haveGalleryStoragePermission() =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

    private fun haveCameraStoragePermission() =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

    private fun requestGalleryPermission() {
        if (!haveGalleryStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(
                this,
                permissions,
                Const.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun requestCameraPermission() {
        if (!haveCameraStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
            ActivityCompat.requestPermissions(
                this,
                permissions,
                Const.REQUEST_PERMISSION_CAMERA
            )
        }
    }

    private fun getImageFromGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, Const.REQUEST_CODE_MAIN_ACTIVITY_TO_GALLERY)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
//            .apply {
//            // Save a file: path for use with ACTION_VIEW intents
//            currentPhotoPath = absolutePath
//        }
    }

    private fun getImageFromCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    cameraUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, Const.REQUEST_CODE_MAIN_ACTIVITY_TO_CAMERA)
                }
            }
        }
    }

}