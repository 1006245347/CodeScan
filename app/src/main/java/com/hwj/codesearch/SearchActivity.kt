package com.hwj.codesearch

import android.graphics.Bitmap
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.SurfaceView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.text.MLLocalTextSetting
import com.huawei.hms.mlsdk.text.MLText
import com.huawei.hms.mlsdk.text.MLTextAnalyzer
import com.huawei.hms.searchkit.SearchKitInstance
import com.huawei.hms.searchkit.bean.AutoSuggestResponse
import com.huawei.hms.searchkit.utils.Language
import com.hwj.codescan.R
import com.hwj.codescan.printD
import com.hwj.codesearch.ocr.*
import java.io.IOException
import java.lang.ref.WeakReference

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_search)
//        etInput = findViewById(R.id.etInput)
//        val btnClick = findViewById<Button>(R.id.btnClick)
//        btnClick.setOnClickListener {
////            testSearch()
//            videoOcr()
//        }

//        SearchKitInstance.enableLog() //  //有服务器校验 客服说 搜索服务不支持中国大陆！！
//        SearchKitInstance.getInstance().setInstanceCredential(token)
        initTxt()
    }

    var analyzer: MLTextAnalyzer? = null
    private fun textOcr(bitmap: Bitmap?) {
        bitmap?.let {
            val setting = MLLocalTextSetting.Factory()
                .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE) // 设置识别语种。
                .setLanguage("zh")
                .create()
            analyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting)
            //图片识别获取文本

            val frame = MLFrame.fromBitmap(bitmap)
            val task = analyzer?.asyncAnalyseFrame(frame)
            task?.addOnSuccessListener {
                val blocks = it.blocks //结果
            }
        }
    }

    //demo text recognitionActivity
    private class MsgHandler(mainActivity: SearchActivity?) : Handler() {
        var mMainActivityWeakReference: WeakReference<SearchActivity>

        init {
            mMainActivityWeakReference = WeakReference(mainActivity)
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val mainActivity: SearchActivity = mMainActivityWeakReference.get()
                ?: return
//            Log.d(SearchActivity.TAG, "msg what :" + msg.what)
//            if (msg.what === Constant.SHOW_TAKE_PHOTO_BUTTON) {
//                mainActivity.setVisible()
//            } else if (msg.what === Constant.HIDE_TAKE_PHOTO_BUTTON) {
//                mainActivity.setGone()
//            }
        }
    }

    private var mHandler = MsgHandler(this)
    private var lensEngineTxt: com.hwj.codesearch.ocr.LensEngineTxt? = null
    private var preview: LensEnginePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraConfiguration: CameraConfiguration? = null
    private var facing = CameraConfiguration.CAMERA_FACING_BACK
    private var camera: Camera? = null
    private var localTextTransactor: LocalTextTransactor? = null
    private var isInitialization = false

    private fun initTxt() {   //为啥崩溃
        setContentView(R.layout.layout_ocr)
        val btn = findViewById<Button>(R.id.btnClick)
        btn.setOnClickListener {
            preview?.release()
            restartLensEngineTxt(Constant.POSITION_EN)
        }
        preview = findViewById(R.id.live_preview)
        graphicOverlay = findViewById(R.id.live_overlay)
        cameraConfiguration = CameraConfiguration()
        cameraConfiguration?.setCameraFacing(facing)
        createLensEngineTxt()//相机权限
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(Constant.CAMERA_FACING, facing)
        super.onSaveInstanceState(outState)
    }

    private fun createLensEngineTxt() {
        if (null == lensEngineTxt) {
            lensEngineTxt = LensEngineTxt(this, cameraConfiguration, graphicOverlay)
        }
        try {
            localTextTransactor = LocalTextTransactor(mHandler, this)
            lensEngineTxt?.setMachineLearningFrameTransactor(localTextTransactor)
            isInitialization = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startLensEngineTxt() {
        lensEngineTxt?.let {
            try {
                preview?.start(it, false)
            } catch (e: Exception) {
                e.printStackTrace()
                lensEngineTxt?.release()
                lensEngineTxt = null
            }
        }
    }

    private fun restartLensEngineTxt(type: String) { //语言
        lensEngineTxt?.release()
        lensEngineTxt = null
        createLensEngineTxt()
        startLensEngineTxt()
        if (lensEngineTxt == null || lensEngineTxt?.camera == null) {
            return
        }
        camera = lensEngineTxt?.camera
        try {
            camera?.setPreviewDisplay(preview?.surfaceHolder)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isInitialization) {
            createLensEngineTxt()
        }
        startLensEngineTxt()
    }

    override fun onStop() {
        super.onStop()
        preview?.stop()
    }

    var lensEngine: LensEngine? = null
    var vanalyzer: MLTextAnalyzer? = null

    //持续性视频流检测
    private fun videoOcr() {
        vanalyzer = MLTextAnalyzer.Factory(this).create()
        vanalyzer?.setTransactor(OcrDetectorProcessor())
        lensEngine = LensEngine.Creator(applicationContext, analyzer)
            .setLensType(LensEngine.BACK_LENS)
            .applyDisplayDimension(1440, 1080)
            .applyFps(30.0f)
            .enableAutomaticFocus(true)
            .create()
        val mSurfaceView: SurfaceView? = findViewById(R.id.surfaceView)
        try {
            lensEngine!!.run(mSurfaceView!!.holder)
        } catch (e: IOException) {
            // 异常处理逻辑。
        }
    }

    class OcrDetectorProcessor : MLAnalyzer.MLTransactor<MLText.Block?> {
        override fun transactResult(results: MLAnalyzer.Result<MLText.Block?>) {
            val items = results.analyseList
            printD("getOcrList>>$items")
            // 开发者根据需要处理识别结果，需要注意，这里只对检测结果进行处理。
            // 不可调用ML Kit提供的其他检测相关接口。
        }

        override fun destroy() {
            // 检测结束回调方法，用于释放资源等。
        }
    }

    private fun stopOcr() {
        analyzer?.stop()
        vanalyzer?.stop()
        lensEngine?.release()
        lensEngineTxt?.release()
    }

    //{
    //    "access_token": "DAEBAGGIZsZr27oDawJM2Wq/4XWEMz1whefaG8/8BiEIswxAsBDSps/Eod7oAm5fDkJvxfB30PcdFljAQsedNSJrS8BIH2/C5xS6Zg==",
    //    "expires_in": 3600,
    //    "token_type": "Bearer"
    //}
    lateinit var etInput: EditText
    val token =
        "DAEBAKATaJ2F6u5NF/yiLQ2UFNnpuqJvCAecW2oJEj1GE0kODSmEiSDGC+je5JQUwgNZpuUPDRDQqmA0T5L/U9TOjdE9vcq6iWpSvQ=="

    private fun testSearch() {
        Thread() {
            var response: AutoSuggestResponse? =
                SearchKitInstance.getInstance().searchHelper.suggest(
                    etInput.text.toString(),
                    Language.ENGLISH
                )
            printD("suggest: ${response?.suggestions}")

            val instance = SearchKitInstance.getInstance()
            val helper = instance?.searchHelper
            val text = etInput.text.toString()
            val list = helper?.suggest(text, Language.ENGLISH)
            printD("check= $instance $helper $text $list")
        }.start()
    }

//    private fun recycleBitmap() {
//        if (this.bitmap != null && !this.bitmap.isRecycled()) {
//            this.bitmap.recycle()
//            this.bitmap = null
//        }
//        if (this.bitmapCopy != null && !this.bitmapCopy.isRecycled()) {
//            this.bitmapCopy.recycle()
//            this.bitmapCopy = null
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        stopOcr()
    }
}