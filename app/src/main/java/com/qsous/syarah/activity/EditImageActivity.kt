package com.qsous.syarah.activity

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qsous.syarah.MultiTouchListener
import com.qsous.syarah.R
import com.qsous.syarah.application.Const
import kotlinx.android.synthetic.main.activity_edit_image.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class EditImageActivity : AppCompatActivity() {

    companion object {
        fun getIntentToStart(context: Context, imageUri: Uri): Intent {
            val mIntent = Intent(context, EditImageActivity::class.java)
            mIntent.putExtra(Const.EXTRAS_KEY_TO_EDIT_IMAGE_ACTIVITY__IMAGE_URI, imageUri)
            return mIntent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_image)

        val selectedImageUri = intent.getParcelableExtra<Uri>(Const.EXTRAS_KEY_TO_EDIT_IMAGE_ACTIVITY__IMAGE_URI)

        selectedImageView.setImageURI(selectedImageUri)

        val multiTouchListener = MultiTouchListener(
            editView, selectedImageView
        )

        rectangleImageView.setOnTouchListener(multiTouchListener)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.edit_image, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_action -> {
                getBitmap()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun getBitmap() {
        try {
            if (editView != null) {
                val drawingCache: Bitmap = getBitmapFromView(editView)

                saveMediaToStorage(drawingCache)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap) {
        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
//            context?.toast("Saved to Photos")
            Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show()
        }
    }
}
