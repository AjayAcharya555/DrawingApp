package com.ajcoder.drawing


import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageBtnCurrentPaint: ImageButton? = null

    var customProgressDialog: Dialog? = null
    private val openGralleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackGround: ImageView = findViewById(R.id.iv_background)

                imageBackGround.setImageURI(result.data?.data)
            }

        }


    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionsName = it.key
                val isGranted = it.value

                if (isGranted) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permissions granted now you can read the storage files",
                        Toast.LENGTH_LONG
                    ).show()
                    val pickIntent = Intent(
                        Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    openGralleryLauncher.launch(pickIntent)
                } else {
                    if (permissionsName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this@MainActivity, "You just denied the permission", Toast.LENGTH_LONG
                        ).show()
                    }
                }

            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val ibbrush: ImageButton = findViewById(R.id.ib_brush)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.brush_icon)
        mImageBtnCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageBtnCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        showBrushSizeChooseDailog()

        ibbrush.setOnClickListener {
            showBrushSizeChooseDailog()
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallery)
        ibGallery.setOnClickListener {

            requestStoragePermission()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {

            if (isReadStorageAllowed()) {
                cancelProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }
    }

    private fun showBrushSizeChooseDailog() {

        val brushDailog = Dialog(this)
        brushDailog.setContentView(R.layout.dailog_brush_size)
        brushDailog.setTitle("Brush Size. ")
        val smallbtn: ImageButton = brushDailog.findViewById(R.id.ib_small_brush)
        smallbtn.setOnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDailog.dismiss()
        }

        val mediumbtn: ImageButton = brushDailog.findViewById(R.id.ib_medium_brush)
        mediumbtn.setOnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDailog.dismiss()
        }

        val largrbtn: ImageButton = brushDailog.findViewById(R.id.ib_large_brush)
        largrbtn.setOnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDailog.dismiss()
        }

        brushDailog.show()
    }

    fun paintClicked(view: View) {
        Toast.makeText(this, "clickpaint", Toast.LENGTH_LONG).show()
        if (view !== mImageBtnCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageBtnCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )

            mImageBtnCurrentPaint = view
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    //create a method to requestStorage permission
    private fun requestStoragePermission() {
// Check if the permission was denied and show rationale
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            //call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog(
                "Kids Drawing App", "Kids Drawing App" + "needs to Access your External Storage"
            )
        } else {
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun showRationaleDialog(
        title: String, message: String
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(message).setTitle(title).setPositiveButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height

        val returnBitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888
        )
        //Bind a canvas to it
        val canvas = Canvas(returnBitmap)

        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = " "

        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val byte = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, byte)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString() + File.separator + "kidsdrawingapp" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.


                    val fo = FileOutputStream(f)
                    fo.write(byte.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (!result.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_LONG
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went worng while saving the file",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    /*
    * Method is used to show the custom progress dialog
    * */

    private fun showProgressDialog() {

        customProgressDialog = Dialog(this@MainActivity)

        /* Set the screen content from a layout resource
        * The resource will be inflated, adding all toplevel views to the screen */

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

//        start the dialog and display it on screen
        customProgressDialog?.show()

    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
         MediaScannerConnection.scanFile(this, arrayOf(result), null){
             path, uri->
             val shareIntent = Intent()
             shareIntent.action =Intent.ACTION_SEND
             shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
             shareIntent.type = "image/png"
             startActivity(Intent.createChooser(shareIntent,"Share"))
         }
    }

}