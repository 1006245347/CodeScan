package com.hwj.codescan

import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import androidx.annotation.RequiresApi
import com.huawei.hms.ml.scan.HmsScan
import com.huawei.hms.ml.scan.HmsScan.EventTime
import java.util.*

object CalendarEventAction {
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun getCalendarEventIntent(calendarEvent: HmsScan.EventInfo): Intent? {
        val intent = Intent(Intent.ACTION_INSERT)
        try {
            intent.setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, getTime(calendarEvent.beginTime))
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, getTime(calendarEvent.closeTime))
                .putExtra(CalendarContract.Events.TITLE, calendarEvent.getTheme())
                .putExtra(CalendarContract.Events.DESCRIPTION, calendarEvent.getAbstractInfo())
                .putExtra(CalendarContract.Events.EVENT_LOCATION, calendarEvent.getPlaceInfo())
                .putExtra(CalendarContract.Events.ORGANIZER, calendarEvent.getSponsor())
                .putExtra(CalendarContract.Events.STATUS, calendarEvent.getCondition())
        } catch (e: NullPointerException) {
            Log.w("getCalendarEventIntent", e)
        }
        return intent
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun getTime(calendarDateTime: EventTime): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(
            calendarDateTime.getYear(),
            calendarDateTime.getMonth() - 1,
            calendarDateTime.getDay(),
            calendarDateTime.getHours(),
            calendarDateTime.getMinutes(),
            calendarDateTime.getSeconds()
        )
        return calendar.getTime().getTime()
    }
}