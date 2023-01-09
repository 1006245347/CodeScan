package com.hwj.codescan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import com.huawei.hms.ml.scan.HmsScan
import com.permissionx.guolindev.PermissionX

/**
 * @author by jason-何伟杰，2023/1/9
 * des: 处理sdk返回的结果实体
 */
object CodeResultUtils {

    /*二维码结果类型*/

     fun handleResult(hmsScan: HmsScan, activity: FragmentActivity) {
        var rawResult: String? = null
        rawResult = hmsScan.getOriginalValue()  //原始码值
        printD("code_result=$rawResult")
        if (hmsScan.getScanType() == HmsScan.QRCODE_SCAN_TYPE) {
            if (hmsScan.getScanTypeForm() == HmsScan.PURE_TEXT_FORM) { //结构化数据 文本条码
                copyClipText()
            } else if (hmsScan.getScanTypeForm() == HmsScan.EVENT_INFO_FORM) { //日历事件条码
                activity.startActivity(CalendarEventAction.getCalendarEventIntent(hmsScan.getEventInfo()))
            } else if (hmsScan.getScanTypeForm() == HmsScan.DRIVER_INFO_FORM) {//驾照信息条码
                copyClipText()
            } else if (hmsScan.getScanTypeForm() == HmsScan.LOCATION_COORDINATE_FORM) {//定位信息条码
                if (LocationAction.checkMapAppExist(activity.applicationContext)) {
                    try {
                        activity.startActivity(LocationAction.getLoactionInfo(hmsScan.getLocationCoordinate()))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    copyClipText()
                }
            } else if (hmsScan.getScanTypeForm() == HmsScan.URL_FORM) {  //URL链接条码
                val webPage = Uri.parse(hmsScan.getOriginalValue())
                val intent = Intent(Intent.ACTION_VIEW, webPage)
                if (intent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(intent)
                }
            } else if (hmsScan.getScanTypeForm() == HmsScan.WIFI_CONNECT_INFO_FORM) { //wifi二维码
                PermissionX.init(activity).permissions(
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
                ).request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        val w = hmsScan.wiFiConnectionInfo
                        w?.let {
                            printD("wifiName: ${w.getSsidNumber()} p:${w.getPassword()} ")
                            WifiAdmin(activity).Connect(
                                w.getSsidNumber(),
                                w.getPassword(), w.getCipherMode()
                            )
                        }
                        activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                }
            }
        } else if (hmsScan.getScanType() == HmsScan.EAN13_SCAN_TYPE) {
            if (hmsScan.getScanTypeForm() == HmsScan.ARTICLE_NUMBER_FORM) {//产品信息条码
                copyClipText()
            } else if (hmsScan.getScanTypeForm() == HmsScan.ISBN_NUMBER_FORM) {  //isbn
                copyClipText()
            }
        } else if (hmsScan.getScanType() == HmsScan.EAN8_SCAN_TYPE || hmsScan.getScanType() == HmsScan.UPCCODE_A_SCAN_TYPE || hmsScan.getScanType() == HmsScan.UPCCODE_E_SCAN_TYPE) {
            copyClipText()
        } else {
            copyClipText()
        }
    }

    private fun copyClipText() {

    }
}