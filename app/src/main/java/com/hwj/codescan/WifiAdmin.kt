package com.hwj.codescan

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.huawei.hms.ml.scan.HmsScan


class WifiAdmin {
    private var mWifiManager: WifiManager? = null

    constructor(context: Context) : super() {
        mWifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    fun OpenWifi(): Boolean {
        var bRet = true
        if (!mWifiManager!!.isWifiEnabled) {
            bRet = mWifiManager!!.setWifiEnabled(true)
        }
        return bRet
    }

    fun Connect(ssid: String, password: String, type: Int): Boolean {
        if (!OpenWifi()) {
            return false
        }
        while (mWifiManager!!.wifiState == WifiManager.WIFI_STATE_ENABLING) {
            try {
                Thread.currentThread()
                Thread.sleep(100)
            } catch (ie: InterruptedException) {
            }
        }
        val netID = createWifiInfo(ssid, password, type)
        val bRet = mWifiManager!!.enableNetwork(netID, true)
        mWifiManager!!.saveConfiguration()
        return bRet
    }

    fun createWifiInfo(ssid: String, password: String, type: Int): Int {
        val config = WifiConfiguration()
        config.allowedAuthAlgorithms.clear()
        config.allowedGroupCiphers.clear()
        config.allowedKeyManagement.clear()
        config.allowedPairwiseCiphers.clear()
        config.allowedProtocols.clear()
        config.SSID = "\"" + ssid + "\""
        val tempConfig = isExsits(ssid)
        if (tempConfig != null) {
            if (!mWifiManager!!.removeNetwork(tempConfig.networkId)) {
                return tempConfig.networkId
            }
        }
        if (type == HmsScan.WiFiConnectionInfo.NO_PASSWORD_MODE_TYPE) //WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = ""
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            config.wepTxKeyIndex = 0
        }
        if (type == HmsScan.WiFiConnectionInfo.WEP_MODE_TYPE) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true
            config.wepKeys[0] = "\"" + password + "\""
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            config.wepTxKeyIndex = 0
        }
        if (type == HmsScan.WiFiConnectionInfo.WPA_MODE_TYPE) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + password + "\""
            config.hiddenSSID = true
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            config.status = WifiConfiguration.Status.ENABLED
        }
        return mWifiManager!!.addNetwork(config)
    }

    private fun isExsits(SSID: String): WifiConfiguration? {
        val existingConfigs = mWifiManager!!.configuredNetworks
        for (existingConfig in existingConfigs) {
            if (existingConfig.SSID == "\"" + SSID + "\"") {
                return existingConfig
            }
        }
        return null
    }
}