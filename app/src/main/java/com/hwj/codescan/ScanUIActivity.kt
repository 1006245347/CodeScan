package com.hwj.codescan

import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.huawei.hms.hmsscankit.RemoteView
import com.huawei.hms.ml.scan.HmsScan

class ScanUIActivity : AppCompatActivity() {

    private var remoteView: RemoteView? = null
    lateinit var fl: FrameLayout
    private val remoteViewManager: RemoteViewManager by lazy {
        RemoteViewManager(remoteView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_view_finder)
//        setContentView(R.layout.layout_scan_box)
        initScanView(savedInstanceState)
    }

    private fun initScanView(savedInstanceState: Bundle?) {
        fl = findViewById(R.id.fl)
        val dm = resources.displayMetrics
        // 设置扫码识别区域，您可以按照需求调整参数
        val density = dm.density
        val mScreenWidth = dm.widthPixels
        val mScreenHeight = dm.heightPixels
        val scanFrameSize = (SCAN_FRAME_SIZE * density)

        val rect = Rect()
        apply { //扫码框的宽高是300dp
            rect.left = (mScreenWidth / 2 - scanFrameSize / 2).toInt()
            rect.right = (mScreenWidth / 2 + scanFrameSize / 2).toInt()
            rect.top = (mScreenHeight / 2 - scanFrameSize / 2).toInt()
            rect.bottom = (mScreenHeight / 2 + scanFrameSize / 2).toInt()
        }

        remoteView = RemoteView.Builder()
            .setContext(this)
            .setBoundingBox(rect)
            .setContinuouslyScan(false) //是否连续扫码
            .setFormat(HmsScan.ALL_SCAN_TYPE) //注意限制码的类型容易导致无法识别
//            .setFormat(HmsScan.QRCODE_SCAN_TYPE, HmsScan.DATAMATRIX_SCAN_TYPE)
            .build()
        remoteView?.setOnResultCallback { result ->
            if (result != null && result.isNotEmpty() && result[0] != null && !TextUtils.isEmpty(
                    result[0].getOriginalValue()
                )
            ) {
                CodeResultUtils.handleResult(result[0], this)
            }
        }
        remoteViewManager.onCreate(this, savedInstanceState)

        // Add the defined RemoteView to the page layout.
        val params = FrameLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        fl.addView(remoteView, params)
    }

    companion object {
        const val SCAN_FRAME_SIZE = 300
    }
}