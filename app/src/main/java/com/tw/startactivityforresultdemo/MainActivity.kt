package com.tw.startactivityforresultdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tw.startactivityforresultdemo.databinding.ActivityMainBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileDescriptor
import java.io.IOException


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    val TAG :String = this.javaClass.simpleName

    private val REQUEST_CAMERA_1 = 60
    private val REQUEST_GALLERY_1 = 80
    var pathPhoto1: String = ""
    var cam_uri: Uri? = null
    var multipartImage1: MultipartBody.Part? = null
    var requestFile1: RequestBody? = null

    private lateinit var activity: AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        activity = this@MainActivity

        binding.flCamera.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                myPermissions33Above(REQUEST_CAMERA_1)
            } else {
                myPermissions(REQUEST_CAMERA_1)
            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun myPermissions33Above(type: Int) {
        Dexter.withContext(applicationContext)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES,
                )

            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        Log.e(TAG, "permissions Granted")
                        Toast.makeText(activity, "Permissions Granted", Toast.LENGTH_LONG).show()

                        //once permissions granted then you can take picture from camera gallery or other thing
                        showCameraGalleryDialog( type )
                    }
                    if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                        Log.e(TAG,"permissions Denied---> ${multiplePermissionsReport.deniedPermissionResponses}")
                        Toast.makeText(activity, "Permissions Denied", Toast.LENGTH_LONG).show()
                        showSettingsDialogAll(multiplePermissionsReport.deniedPermissionResponses)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(list: List<PermissionRequest>, permissionToken: PermissionToken) {
                    permissionToken.continuePermissionRequest()
                }
            }).withErrorListener { dexterError: DexterError ->
                Log.e(TAG,"permissions dexterError :" + dexterError.name)
            }.onSameThread()
            .check()
    }

    //this function is almost same as above function
    private fun myPermissions(type: Int) {
        Dexter.withContext(applicationContext)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        Log.e(TAG, "permissions Granted")
                        Toast.makeText(activity, "Permissions Granted", Toast.LENGTH_LONG).show()

                        //once permissions granted then you can take picture from camera gallery or other thing
                        showCameraGalleryDialog( type )

                    }
                    if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                        Log.e(TAG,"permissions Denied")
                        Toast.makeText(activity, "Permissions Denied", Toast.LENGTH_LONG).show()
                        showSettingsDialogAll(multiplePermissionsReport.deniedPermissionResponses)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest>,
                    permissionToken: PermissionToken) {
                    permissionToken.continuePermissionRequest()
                }
            }).withErrorListener { dexterError: DexterError ->
                Log.e(TAG, "permissions dexterError :" + dexterError.name)
            }
            .onSameThread()
            .check()
    }

    //if permission are denied then permission settings of this app will open
    fun showSettingsDialogAll(deniedPermissionResponses: MutableList<PermissionDeniedResponse>) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Need Permissions")
        builder.setMessage(deniedPermissionResponses[0].permissionName)
        builder.setPositiveButton("GOTO SETTINGS") { dialog, _ ->
            dialog.cancel()
            openSettings()
        }
        builder.show()
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", "com.example.myapplication", null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun showCameraGalleryDialog(type: Int) {

        val options = arrayOf<CharSequence>(
            resources.getString(R.string._camera),
            resources.getString(R.string._gallery)
        )
        val builder: AlertDialog.Builder = AlertDialog.Builder( activity )
        builder.setTitle(resources.getString(R.string._take_photo))
        builder.setCancelable(true)
        builder.setItems(options) { dialog, item ->
            if (options[item] == resources.getString(R.string._camera)) {
                dialog.dismiss()

                requestCamera(REQUEST_CAMERA_1)

            } else if (options[item] == resources.getString(R.string._gallery)) {
                dialog.dismiss()

                requestGallery(REQUEST_GALLERY_1)
            }
        }
        builder.show()
    }



    var startCamera: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback { result ->
            Log.e(TAG, "startcamera uri : ${result.data?.data}", )
            Log.e(TAG, "startcamera uri2 : ${cam_uri}", )
            if (result.resultCode == RESULT_OK) {
                // There are no request codes
                val inputImage = uriToBitmap(cam_uri!!)
                val rotated = rotateBitmap(inputImage!!, cam_uri!!)
//                binding.ivPhoto.setImageURI(cam_uri)
                binding.ivPhoto.setImageBitmap(rotated)
            }
        }
    )

    private fun requestCamera(code: Int) {

        if (code == REQUEST_CAMERA_1){

            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            cam_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cam_uri)

            //startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE); // OLD WAY
            startCamera.launch(cameraIntent) // VERY NEW WAY

        }

    }

    private fun requestGallery(code: Int) {

        if (code == REQUEST_GALLERY_1){
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryActivityResultLauncher1.launch(galleryIntent)
        }

    }


    private var galleryActivityResultLauncher1: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(), ActivityResultCallback {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                val image_uri1 = it.data?.data
                val inputImage = uriToBitmap(image_uri1!!)
                val rotated = rotateBitmap(inputImage!!, image_uri1)


                val fileUri = Uri.parse(image_uri1.toString())
                pathPhoto1 = PathUtil.getPath(activity, image_uri1)!!
                val image = File(pathPhoto1)
                val file1 = image.absoluteFile



                Log.e(TAG, "gallery image pathPhoto1 : ${pathPhoto1}")
                Log.e(TAG, "gallery image absoluteFile : ${image.absoluteFile}")
                Log.e(TAG, "gallery image absolutePath : ${image.absolutePath}")
                Log.e(TAG, "onActivityResult file1: $file1")
                requestFile1 = file1.asRequestBody("multipart/form-data".toMediaTypeOrNull())

                multipartImage1 = MultipartBody.Part.createFormData("product_img", file1.name, requestFile1!! )

                binding.ivPhoto.setImageBitmap(rotated)
            }
        }
    )



    //TODO takes URI of the image and returns bitmap
    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    //TODO rotate image if image captured on samsung devices
    //TODO Most phone cameras are landscape, meaning if you take the photo in portrait, the resulting photos will be rotated 90 degrees.
    @SuppressLint("Range", "Recycle")
    fun rotateBitmap(input: Bitmap, image_uri: Uri): Bitmap {
        val orientationColumn = arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cur: Cursor? = contentResolver.query(image_uri, orientationColumn, null, null, null)
        var orientation = -1
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]))
        }
        Log.d("tryOrientation", orientation.toString() + "")
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(orientation.toFloat())
        cur!!.close()
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }


}