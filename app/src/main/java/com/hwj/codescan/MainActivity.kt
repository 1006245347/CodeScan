package com.hwj.codescan

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScanAnalyzer
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions
import com.huawei.hms.mlsdk.common.MLFrame
import com.hwj.codescan.CodeResultUtils.handleResult
import com.permissionx.guolindev.PermissionX

/**
 * @author by jason-何伟杰，2023/1/9
 * des:https://github.com/HMS-Core/hms-scan-demo
 *
 * https://developer.huawei.com/consumer/cn/doc/development/HMSCore-Guides/android-asynchronous-mode-0000001050042018
 */
class MainActivity : AppCompatActivity() {
    companion object {
        const val CODE_SCAN = 111   //识别单码
        const val CODE_MULTI = 222  //识别多码
        const val BITMAP_CODE = 333
        const val MULTIPROCESSOR_SYN_CODE = 444
        const val MULTIPROCESSOR_ASYN_CODE = 555

        const val CODE_IMAGE = 999

        const val SCAN_RESULT = "scanResult"
    }

    @JvmField
    var scanResultView: ScanResultView? = null

    private lateinit var tvText: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(getLayoutId())
        askPermission()
        tvText = findViewById(R.id.tv2)
        tvText.setOnClickListener {
            //触发扫码单码
//            scanOne()
            //触发多码扫描
            scanMulti()
        }
        if (intent.getIntExtra("type", 0) == 1) {
            initMultiScan()
//            scanMulti()  //直接触发图片识别
        }
    }

    private fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    private fun scanOne() {
        ScanUtil.startScan(this, CODE_SCAN, HmsScanAnalyzerOptions.Creator().create())
    }

    private fun scanMulti() {
        if (intent.getIntExtra("type", 0) == 1) {
            setPictureScanOperation() //触发本地图片识别    //多码识别
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("type", 1)
            startActivity(intent)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        if (requestCode == CODE_SCAN) {
            val obj: HmsScan? = data.getParcelableExtra(ScanUtil.RESULT)
            obj?.let {
                handleResult(it, this)
            }
        } else if (requestCode == CODE_MULTI) {  //多码识别后的最终结果
            val obj = data.getParcelableArrayExtra(SCAN_RESULT)
            obj?.let {
                if (it.isNotEmpty()) {
                    if (obj.size == 1) {
                        if (obj[0] != null && !TextUtils.isEmpty((obj[0] as HmsScan).getOriginalValue())) {
                            val r = obj[0] as HmsScan
                            handleResult(r, this)
                        }
                    } else {
                        obj.forEach { o ->
                            val r = o as HmsScan
                            handleResult(r, this)
                        }
                    }
                }
            }
        } else if (requestCode == CODE_IMAGE) { //本地图片识别，接收 CommonHandler 内部处理数据 多码识别的中间层
            handleLocalMultiResult(
                MediaStore.Images.Media.getBitmap(this.contentResolver, data.data),
                mode
            )
        }
    }

    /************************************************多码同时识别************************************************/
    //同时识别多个码
    private var cameraOperation: CameraOperation? = null
    private var surfaceCallBack: SurfaceCallBack? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var handler: CommonHandler? = null
    private var isShow = false
    private val mode = MULTIPROCESSOR_SYN_CODE // MULTIPROCESSOR_ASYN_CODE

    inner class SurfaceCallBack : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            if (!isShow) {
                isShow = true
                initCamera()
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            isShow = false
        }
    }

    private fun initMultiScan() {
        cameraOperation = CameraOperation()
        surfaceCallBack = SurfaceCallBack()
        val cameraPreview = findViewById<SurfaceView>(R.id.surfaceView)
        adjustSurface(cameraPreview)
        surfaceHolder = cameraPreview.holder
        isShow = false
        scanResultView = findViewById(R.id.scan_result_view)
    }

    //在这里获取到多码结果
    fun handleSdkData(hmsScans: Array<HmsScan?>?) {
        hmsScans?.let {
            if (hmsScans.isNotEmpty()) {
                if (hmsScans.size == 1) {
                    if (hmsScans[0] != null && !TextUtils.isEmpty((hmsScans[0] as HmsScan).getOriginalValue())) {
                        handleResult(hmsScans[0]!!, this)
                    }
                } else {
                    hmsScans.forEach {
                        handleResult(it!!, this)
                    }
                }
            }
        }
    }

    private fun handleLocalMultiResult(bitmap: Bitmap, m: Int) {
        if (m == MULTIPROCESSOR_ASYN_CODE) {
            val image = MLFrame.fromBitmap(bitmap)
            val analyzer = //启动扫码API
                HmsScanAnalyzer.Creator(this).setHmsScanTypes(HmsScan.ALL_SCAN_TYPE).create()
            analyzer.analyzInAsyn(image).addOnSuccessListener { hmsScans ->
                if (hmsScans != null && hmsScans.size > 0 && hmsScans[0] != null && !TextUtils.isEmpty(
                        hmsScans[0]!!.getOriginalValue()
                    )
                ) {
                    val intent = Intent()
                    intent.putExtra(SCAN_RESULT, hmsScans.toTypedArray())
                    setResult(RESULT_OK, intent)
//                    finish()
                }
            }.addOnFailureListener { e -> Log.w(TAG, e) }
        } else if (m == MULTIPROCESSOR_SYN_CODE) {
            val image = MLFrame.fromBitmap(bitmap)
            val analyzer =
                HmsScanAnalyzer.Creator(this).setHmsScanTypes(HmsScan.ALL_SCAN_TYPE).create()
            val result = analyzer.analyseFrame(image)
            if (result != null && result.size() > 0 && result.valueAt(0) != null && !TextUtils.isEmpty(
                    result.valueAt(0)!!.getOriginalValue()
                )
            ) {
                val info = arrayOfNulls<HmsScan?>(result.size())
                printD("multi_list>$info")
                for (index in 0 until result.size()) {
                    info[index] = result.valueAt(index)
                }
//                //同一个页面，不能这样设置值吧 setResult根本无法传值
//                val intent = Intent()
//                intent.putExtra(SCAN_RESULT, info)
//                setResult(RESULT_OK, intent)
////                finish()
                handleSdkData(info)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isShow) {
            initCamera()
        } else {
            surfaceHolder?.addCallback(surfaceCallBack)
        }
    }

    override fun onPause() {
        super.onPause()
        if (handler != null) {
            handler?.quit()
            handler = null
        }
        cameraOperation?.close()
        if (!isShow) {
            surfaceHolder?.removeCallback(surfaceCallBack)
        }
    }

    private fun initCamera() {
        try {
            cameraOperation?.open(surfaceHolder)
            if (null == handler) {
                handler = CommonHandler(this, cameraOperation, mode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setPictureScanOperation() { //识别图片的码?
        val pickIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, CODE_IMAGE)
    }

    private fun adjustSurface(cameraPreview: SurfaceView) {
        val paramSurface = cameraPreview.layoutParams as FrameLayout.LayoutParams
        if (getSystemService(Context.WINDOW_SERVICE) != null) {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val defaultDisplay = windowManager.defaultDisplay
            val outPoint = Point()
            defaultDisplay.getRealSize(outPoint)
            val sceenWidth = outPoint.x.toFloat()
            val sceenHeight = outPoint.y.toFloat()
            val rate: Float
            if (sceenWidth / 1080.toFloat() > sceenHeight / 1920.toFloat()) {
                rate = sceenWidth / 1080.toFloat()
                val targetHeight = (1920 * rate).toInt()
                paramSurface.width = FrameLayout.LayoutParams.MATCH_PARENT
                paramSurface.height = targetHeight
                val topMargin = (-(targetHeight - sceenHeight) / 2).toInt()
                if (topMargin < 0) {
                    paramSurface.topMargin = topMargin
                }
            } else {
                rate = sceenHeight / 1920.toFloat()
                val targetWidth = (1080 * rate).toInt()
                paramSurface.width = targetWidth
                paramSurface.height = FrameLayout.LayoutParams.MATCH_PARENT
                val leftMargin = (-(targetWidth - sceenWidth) / 2).toInt()
                if (leftMargin < 0) {
                    paramSurface.leftMargin = leftMargin
                }
            }
        }
    }

    private fun copyClipText() {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val cpData = ClipData.newPlainText("Label", tvText.text)
        cm.setPrimaryClip(cpData)
    }

    private fun askPermission() {
        var readPermission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readPermission = Manifest.permission.READ_MEDIA_IMAGES
        }
        PermissionX.init(this)
            .permissions(readPermission, Manifest.permission.CAMERA)
            .onExplainRequestReason { scope, deniedList ->
                val msg = "需要您同意以下权限才能正常使用！"
                scope.showRequestReasonDialog(deniedList, message = msg, "同意", "拒绝")
            }.request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    printD("已同意所有权限")
                } else {
                    printD("您拒绝了如下权限：$deniedList ！")
                }
            }
    }
}