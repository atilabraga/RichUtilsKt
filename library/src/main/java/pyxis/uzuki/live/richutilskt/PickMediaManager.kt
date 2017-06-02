package pyxis.uzuki.live.richutilskt

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.content.ContextCompat


import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by JangSejin on 2016-11-02.
 */
class PickMediaManager private constructor(c: Context) {

    private fun getActivity(context: Context): Activity? {
        var context = context

        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

    fun pickFromCamera(context: Context, callback: PickMediaCallback) = requestPhotoPick(context, PICK_FROM_CAMERA, callback)
    fun pickFromGallery(context: Context, callback: PickMediaCallback) = requestPhotoPick(context, PICK_FROM_GALLERY, callback)
    fun pickFromVideo(context: Context, callback: PickMediaCallback) = requestPhotoPick(context, PICK_FROM_VIDEO, callback)
    fun pickFromVideoCamera(context: Context, callback: PickMediaCallback) = requestPhotoPick(context, PICK_FROM_CAMERA_VIDEO, callback)

    private var currentPhotoPath: String? = null
    private var currentVideoPath: String? = null

    @SuppressLint("ValidFragment")
    private fun requestPhotoPick(context: Context, pickType: Int, callback: PickMediaCallback) {

        val fm = getActivity(context)!!.fragmentManager
        val f = ResultFragment(fm, callback)

        fm.beginTransaction().add(f, "FRAGMENT_TAG").commit()
        fm.executePendingTransactions()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            f.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), pickType)
            return
        }


        val intent = Intent()

        when (pickType) {
            PICK_FROM_CAMERA -> {
                intent.action = MediaStore.ACTION_IMAGE_CAPTURE
                val captureUri = createImageUri(context)
                currentPhotoPath = captureUri.toString()
                intent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
            }

            PICK_FROM_GALLERY -> {
                intent.action = Intent.ACTION_PICK
                intent.type = android.provider.MediaStore.Images.Media.CONTENT_TYPE
            }

            PICK_FROM_VIDEO -> {
                intent.action = Intent.ACTION_PICK
                intent.type = android.provider.MediaStore.Video.Media.CONTENT_TYPE
            }

            PICK_FROM_CAMERA_VIDEO -> {
                intent.action = MediaStore.ACTION_VIDEO_CAPTURE
                val captureUri = createVideoUri(context)
                currentVideoPath = captureUri.toString()
                intent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
            }
        }

        f.startActivityForResult(intent, pickType)
    }

    private fun createImageUri(context: Context): Uri {
        val contentResolver = context.contentResolver
        val cv = ContentValues()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        cv.put(MediaStore.Images.Media.TITLE, timeStamp)
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
    }

    private fun createVideoUri(context: Context): Uri {
        val contentResolver = context.contentResolver
        val cv = ContentValues()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        cv.put(MediaStore.Images.Media.TITLE, timeStamp)
        return contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)
    }

    @SuppressLint("ValidFragment")
    private inner class ResultFragment(private val fm: FragmentManager, private val callback: PickMediaCallback) : Fragment() {

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPhotoPick(activity, requestCode, callback)
            } else {
                callback.failPermissionGranted()
            }

            fm.beginTransaction().remove(this).commit()

        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
            super.onActivityResult(requestCode, resultCode, data)
            when (requestCode) {
                PICK_FROM_CAMERA ->

                    if (resultCode == Activity.RESULT_OK) {
                        callback?.pickMediaCallback(currentPhotoPath)
                    }

                PICK_FROM_GALLERY ->

                    if (resultCode == Activity.RESULT_OK) {
                        val cursor = activity.contentResolver.query(data.data, null, null, null, null)
                        if (cursor!!.moveToNext()) {

                            val path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
                            cursor.close()

                            callback?.pickMediaCallback(path)
                        }

                    }

                PICK_FROM_VIDEO ->

                    if (resultCode == Activity.RESULT_OK) {
                        val cursor = activity.contentResolver.query(data.data, null, null, null, null)
                        if (cursor!!.moveToNext()) {

                            val path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA))
                            cursor.close()

                            callback?.pickMediaCallback(path)
                        }

                    }

                PICK_FROM_CAMERA_VIDEO -> if (resultCode == Activity.RESULT_OK) {
                    callback?.pickMediaCallback(currentVideoPath)
                }
            }

            fm.beginTransaction().remove(this).commit()

        }

    }

    interface PickMediaCallback {
        fun pickMediaCallback(path: String?)

        fun failPermissionGranted()
    }

    companion object {

        private var instance:PickMediaManager? = null

        fun getInstance(c: Context): PickMediaManager {

            if (instance == null) {
                instance = PickMediaManager(c)
            }

            return instance as PickMediaManager
        }

        val PICK_FROM_CAMERA = 0
        val PICK_FROM_GALLERY = 1
        val PICK_FROM_VIDEO = 2
        val PICK_FROM_CAMERA_VIDEO = 3
    }
}