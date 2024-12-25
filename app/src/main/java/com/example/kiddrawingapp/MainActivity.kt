package com.example.kiddrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

const val READ_MEDIA_IMAGES_CODE = 101

class MainActivity : AppCompatActivity() {
    lateinit var customDialog: Dialog
    var drawView :DrawView? = null
    var resultPath : String = ""
    var btnShare : ImageView? = null
    var btnSave : ImageView? = null
    var btnUndo : ImageView? = null
    var btnRedo : ImageView? = null
    var drawingImageView :ImageView? = null
    var imageButton : ImageButton? = null
    var mCurrentPaint: ImageButton? = null
    var ivGallery : ImageView ? = null
    val mediaPicker =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()){
            uri->
            if(uri != null){
                drawingImageView?.setImageURI(uri)
            }else{
                Toast.makeText(this, "photo is not found", Toast.LENGTH_SHORT).show()
            }
    }
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permissions->
            permissions.entries.forEach {
                 val isGranted = it.value
                val permissionName = it.key
                if(isGranted){
                    Toast.makeText(this, "Permission is  granted for $permissionName", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "Permission is not already granted for $permissionName",
                        Toast.LENGTH_SHORT).show()
                }

            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        drawView = findViewById(R.id.drawingView)
        imageButton = findViewById(R.id.ibChoseBrush)
        ivGallery = findViewById(R.id.ivGallery)
        btnUndo = findViewById(R.id.btnUndo)
        btnRedo = findViewById(R.id.btnRedo)
        btnSave = findViewById(R.id.btnSave)
        btnShare = findViewById(R.id.btnShare)
        drawingImageView = findViewById(R.id.ivDrawingPhoto)
        drawView?.setBrushSize(20f)
        imageButton?.setOnClickListener{
            showChoseBrushSizeDialog()
        }
        ivGallery?.setOnClickListener {
            showGalleryPermissionDialog()
        }
        btnUndo?.setOnClickListener{
            drawView?.undo()
        }
        btnRedo?.setOnClickListener {
            drawView?.redo()
        }
        btnSave?.setOnClickListener {

            if(isReadStorageAllowed()){
                progressBarDialog()
                lifecycleScope.launch{
                    val containerView : FrameLayout = findViewById(R.id.ContainerView)
                    saveBitmapFile(getBitmapFromView(containerView))
                }
            }
        }
        btnShare?.setOnClickListener {
            if (resultPath.isNotEmpty()){
                shareFile(resultPath)
            }else{
                Toast.makeText(this, "save this image", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun showChoseBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ibSmall)
        smallBtn.setColorFilter(drawView?.getBrushColor() ?: Color.BLACK)
        smallBtn.setOnClickListener{
            drawView?.setBrushSize(10f)
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.findViewById<ImageButton>(R.id.ibMedium)
        mediumBtn.setColorFilter(drawView?.getBrushColor() ?: Color.BLACK)
        mediumBtn.setOnClickListener{
            drawView?.setBrushSize(20f)
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ibLarge)
        largeBtn.setColorFilter(drawView?.getBrushColor() ?: Color.BLACK)
        largeBtn.setOnClickListener{
            drawView?.setBrushSize(30f)
            brushDialog.dismiss()
        }
        brushDialog.show()

    }
    fun paintClicked(view: View){
        if(view != mCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawView?.setBrushColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.panel_pressed)
            )
            mCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.panel_normal)
            )

            mCurrentPaint = view
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            READ_MEDIA_IMAGES_CODE ->{
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Toast.makeText(this, "have permission", Toast.LENGTH_SHORT).show()
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the feature requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Toast.makeText(this, "Permission for READ EXTERNAL_STORAGE ", Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(this, "called", Toast.LENGTH_SHORT).show()
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }

        }

    }
    private fun showRationalDialog(
        title : String,
        message: String
    ){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel"){dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
    private fun showGalleryPermissionDialog(){
        when {
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                // add kaam krne wala code

                mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_MEDIA_IMAGES) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                    showRationalDialog("Kid Drawing App","access photo for Drawing App")

            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
//                requestPermissionsLauncher.launch(arrayOf(
//                    Manifest.permission.READ_MEDIA_IMAGES,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//                ))
                requestPermissions(this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), READ_MEDIA_IMAGES_CODE
                )
            }
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnBitmap
    }
    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{

        withContext(Dispatchers.IO){
            try {
                if (mBitmap != null){
                    val byte = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,byte)
                    val filePath = File(externalCacheDir?.absoluteFile.toString()+
                    File.separator + "kidDrawing_app"+System.currentTimeMillis()/1000 + ".png")

                    val fo = FileOutputStream(filePath)
                    fo.write(byte.toByteArray())
                    fo.close()
                    resultPath = filePath.absolutePath
                    runOnUiThread {
                        cancelDialog()
                        if (resultPath.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "file save successfully : $resultPath", Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(this@MainActivity,
                                "something went wrong while saving the file", Toast.LENGTH_SHORT).show()

                        }
                    }
                }
            }catch (e:Exception){
                resultPath = ""
                e.printStackTrace()
            }
        }
        return resultPath
    }
    fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_MEDIA_IMAGES)
        return result == PackageManager.PERMISSION_GRANTED
    }
    private fun progressBarDialog(){
        customDialog = Dialog(this)
        customDialog.setContentView(R.layout.process_dialog)
        customDialog.show()
    }
    private fun cancelDialog(){
        if(customDialog.isShowing){
            customDialog.dismiss()
        }
    }
    private fun shareFile(result : String){
        MediaScannerConnection.scanFile(this, arrayOf(result),
            null){ path ,uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "image/png"
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            startActivity(Intent.createChooser(shareIntent,"share"))

        }
    }
}





//    private fun showGalleryPermissionDialog(permission : String ,requestCode: Int){
//
//        if(ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED){
//            requestPermissions(arrayOf(permission),requestCode)
//        }else{
//            Toast.makeText(this, "Permission is already granted", Toast.LENGTH_SHORT).show()
//            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
//        }
//    }