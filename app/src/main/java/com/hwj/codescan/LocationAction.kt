package com.hwj.codescan

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import com.huawei.hms.ml.scan.HmsScan.LocationCoordinate


object LocationAction {
    private const val TAG = "LocationAction"

    const val GAODE_PKG = "com.autonavi.minimap"

    fun checkMapAppExist(context: Context): Boolean {
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = context.getPackageManager().getPackageInfo(GAODE_PKG, 0)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        return packageInfo != null
    }

    fun getLoactionInfo(locationCoordinate: LocationCoordinate): Intent? {
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("androidamap://viewMap?lat=" + locationCoordinate.getLatitude() + "&lon=" + locationCoordinate.getLongitude())
        )
    }
}