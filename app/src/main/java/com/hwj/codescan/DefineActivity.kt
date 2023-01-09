package com.hwj.codescan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.*
import com.huawei.hms.hmsscankit.*
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions
import com.permissionx.guolindev.PermissionX
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * @author by jason-何伟杰，2023/1/9
 * des:识别单个码的自定义ui/
 */
class DefineActivity : AppCompatActivity() {

    var frameLayout: FrameLayout? = null
    var remoteView: RemoteView? = null
    var backBtn: ImageView? = null
    var imgBtn: ImageView? = null
    var flushBtn: ImageView? = null

    var mScreenWidth = 0
    var mScreenHeight = 0

    val SCAN_FRAME_SIZE = 240

    companion object {
        const val SCAN_RESULT = "scanResult"
        const val CODE_SCAN = 789
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //只是个识别单码的样式
//        initDefineUI(savedInstanceState)
        //根据参数生成码图
        initGenerateUI()
    }

    /**********************************生成码*********************************************/
    private val BARCODE_TYPES = intArrayOf(
        HmsScan.QRCODE_SCAN_TYPE,
        HmsScan.DATAMATRIX_SCAN_TYPE,
        HmsScan.PDF417_SCAN_TYPE,
        HmsScan.AZTEC_SCAN_TYPE,
        HmsScan.EAN8_SCAN_TYPE,
        HmsScan.EAN13_SCAN_TYPE,
        HmsScan.UPCCODE_A_SCAN_TYPE,
        HmsScan.UPCCODE_E_SCAN_TYPE,
        HmsScan.CODABAR_SCAN_TYPE,
        HmsScan.CODE39_SCAN_TYPE,
        HmsScan.CODE93_SCAN_TYPE,
        HmsScan.CODE128_SCAN_TYPE,
        HmsScan.ITF14_SCAN_TYPE
    )
    private val COLOR =
        intArrayOf(Color.BLACK, Color.BLUE, Color.GRAY, Color.GREEN, Color.RED, Color.YELLOW)
    private val BACKGROUND = intArrayOf(
        Color.WHITE,
        Color.YELLOW,
        Color.RED,
        Color.GREEN,
        Color.GRAY,
        Color.BLUE,
        Color.BLACK
    )

    private var generateType: Spinner? = null
    private var generateMargin: Spinner? = null
    private var generateColor: Spinner? = null
    private var generateBackground: Spinner? = null
    private var barcodeImage: ImageView? = null
    private var barcodeWidth: EditText? = null
    private var barcodeHeight: EditText? = null
    private var content: String? = "9988776"
    private var width = 0
    private var height: Int = 0
    private var resultImage: Bitmap? = null
    private var type = 0
    private var margin = 1
    private var color = Color.BLACK
    private var background = Color.WHITE
    private fun initGenerateUI() {
        setContentView(R.layout.layout_generate_code)
        generateType = findViewById(R.id.generate_type)
        generateMargin = findViewById(R.id.generate_margin)
        generateColor = findViewById(R.id.generate_color)
        generateBackground = findViewById(R.id.generate_backgroundcolor)
        barcodeImage = findViewById(R.id.barcode_image)
        barcodeWidth = findViewById(R.id.barcode_width)
        barcodeHeight = findViewById<EditText>(R.id.barcode_height)
        //Set the barcode type.
        generateType?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                type = BARCODE_TYPES[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                type = BARCODE_TYPES[0]
            }
        })

        //Set the barcode margin.
        generateMargin?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                margin = position + 1
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                margin = 1
            }
        })

        //Set the barcode color.
        generateColor?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                color = COLOR[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                color = COLOR[0]
            }
        })

        //Set the barcode background color.
        generateBackground?.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                background = BACKGROUND[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                background = BACKGROUND[0]
            }
        })
    }

    fun generateCodeBtnClick(v: View?) {
//        content = inputContent!!.text.toString()
        val inputWidth = barcodeWidth!!.text.toString()
        val inputHeight: String = barcodeHeight?.getText().toString()
        //Set the barcode width and height.
        if (inputWidth.length <= 0 || inputHeight.length <= 0) {
            width = 700
            height = 700
        } else {
            width = inputWidth.toInt()
            height = inputHeight.toInt()
        }
        //Set the barcode content.
        if (content!!.length <= 0) {
            Toast.makeText(this, "Please input content first!", Toast.LENGTH_SHORT).show()
            return
        }
        if (color == background) {
            Toast.makeText(this, "The color and background cannot be the same!", Toast.LENGTH_SHORT)
                .show()
            return
        }
        try {
            //Generate the barcode.
            val options =
                HmsBuildBitmapOption.Creator().setBitmapMargin(margin).setBitmapColor(color)
                    .setBitmapBackgroundColor(background).create()
            resultImage = ScanUtil.buildBitmap(content, type, width, height, options)
            barcodeImage!!.setImageBitmap(resultImage)
        } catch (e: WriterException) {
            Toast.makeText(this, "Parameter Error!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCode() {
        try {
            val fileName = System.currentTimeMillis().toString() + ".jpg"
            val storePath = Environment.getExternalStorageDirectory().absolutePath
            val appDir = File(storePath)
            if (!appDir.exists()) {
                appDir.mkdir()
            }
            val file = File(appDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            val isSuccess = resultImage!!.compress(Bitmap.CompressFormat.JPEG, 70, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            val uri = Uri.fromFile(file)
            this@DefineActivity.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            if (isSuccess) {
                Toast.makeText(
                    this@DefineActivity,
                    "Barcode has been saved locally",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this@DefineActivity, "Barcode save failed", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
//            Log.w(TAG, Objects.requireNonNull(e.message)!!)
            Toast.makeText(this@DefineActivity, "Unkown Error", Toast.LENGTH_SHORT).show()
        }
    }

    /***********************************自定义ui**************************************/
    private fun initDefineUI(savedInstanceState: Bundle?) {
        setContentView(R.layout.layout_define1)
        frameLayout = findViewById(R.id.rim)

        //1. Obtain the screen density to calculate the viewfinder's rectangle.
        val dm = resources.displayMetrics
        val density = dm.density
        //2. Obtain the screen size.
        mScreenWidth = resources.displayMetrics.widthPixels
        mScreenHeight = resources.displayMetrics.heightPixels

        val scanFrameSize = (SCAN_FRAME_SIZE * density).toInt()

        //3. Calculate the viewfinder's rectangle, which in the middle of the layout.
        //Set the scanning area. (Optional. Rect can be null. If no settings are specified, it will be located in the middle of the layout.)
        val rect = Rect()
        rect.left = mScreenWidth / 2 - scanFrameSize / 2
        rect.right = mScreenWidth / 2 + scanFrameSize / 2
        rect.top = mScreenHeight / 2 - scanFrameSize / 2
        rect.bottom = mScreenHeight / 2 + scanFrameSize / 2


        //Initialize the RemoteView instance, and set callback for the scanning result.
        remoteView = RemoteView.Builder().setContext(this).setBoundingBox(rect)
            .setFormat(HmsScan.ALL_SCAN_TYPE).build()
        // When the light is dim, this API is called back to display the flashlight switch.
        flushBtn = findViewById(R.id.flush_btn)
        remoteView?.setOnLightVisibleCallback(OnLightVisibleCallBack { visible ->
            if (visible) {
                flushBtn?.setVisibility(View.VISIBLE)
            }
        })
        // Subscribe to the scanning result callback event.
        remoteView?.setOnResultCallback(OnResultCallback { result -> //Check the result.
            if (result != null && result.size > 0 && result[0] != null && !TextUtils.isEmpty(result[0].getOriginalValue())) {
//                val intent = Intent()
//                intent.putExtra(SCAN_RESULT, result[0])
//                setResult(RESULT_OK, intent)
//                finish()
                CodeResultUtils.handleResult(result[0], this) //结果回调
            }
        })

        // Load the customized view to the activity.
        remoteView?.onCreate(savedInstanceState)
        val params = FrameLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        frameLayout?.addView(remoteView, params)
        // Set the back, photo scanning, and flashlight operations.
        setPictureScanOperation()
        setFlashOperation()
    }

    private fun setPictureScanOperation() {
        imgBtn = findViewById(R.id.img_btn)
        imgBtn?.setOnClickListener(View.OnClickListener {
            val pickIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            this@DefineActivity.startActivityForResult(pickIntent, CODE_SCAN)
        })
    }

    private fun setFlashOperation() {
        flushBtn?.setOnClickListener {
            if (remoteView?.lightStatus ?: false) {
                remoteView?.switchLight()
//                flushBtn?.setImageResource(img[1])
            } else {
                remoteView?.switchLight()
//                flushBtn?.setImageResource(img[0])
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //这里处理本地图片的识别
        if (resultCode == RESULT_OK && requestCode == CODE_SCAN) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data!!.data)
                val hmsScans = ScanUtil.decodeWithBitmap(
                    this@DefineActivity,
                    bitmap,
                    HmsScanAnalyzerOptions.Creator().setPhotoMode(true).create()
                )
                if (hmsScans != null && hmsScans.size > 0 && hmsScans[0] != null && !TextUtils.isEmpty(
                        hmsScans[0]!!.getOriginalValue()
                    )
                ) {
//                    val intent = Intent()
//                    intent.putExtra(SCAN_RESULT, hmsScans[0])
//                    setResult(RESULT_OK, intent)
//                    finish()
                    CodeResultUtils.handleResult(hmsScans[0], this)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        remoteView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        remoteView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        remoteView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        remoteView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        remoteView?.onDestroy()
    }

}