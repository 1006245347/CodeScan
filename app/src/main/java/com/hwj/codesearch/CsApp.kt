package com.hwj.codesearch

import androidx.multidex.MultiDexApplication
import com.huawei.hms.searchkit.SearchKitInstance

class CsApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        SearchKitInstance.init(this, "107573795")
    }
}