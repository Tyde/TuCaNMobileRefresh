package com.dalthed.tucanmobilerefresh

import android.app.Application
import com.dalthed.tucanmobilerefresh.scraper.NavigationData
import com.jakewharton.threetenabp.AndroidThreeTen
import org.jsoup.nodes.Document
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import org.threeten.bp.temporal.ChronoField
import java.time.chrono.ChronoLocalDate

class TuCanMobileRefresh : Application() {
    companion object {
        val BASE_URL = "https://www.tucan.tu-darmstadt.de/"
        val BASE_URL_NSL = "https://www.tucan.tu-darmstadt.de"
        val LOG_TAG = "TucanMobileRefresh"
        var temporaryDocumentStore:Document? = null
        var simpleHourMinuteFormatter:DateTimeFormatter? = null
        var navigationData:NavigationData? = null
    }
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        simpleHourMinuteFormatter = DateTimeFormatterBuilder().appendPattern("HH:mm")
            .parseDefaulting(ChronoField.DAY_OF_YEAR,ZonedDateTime.now().dayOfYear.toLong())
            .parseDefaulting(ChronoField.YEAR,ZonedDateTime.now().year.toLong()).toFormatter()
    }
}
/*
class TuCaNMobileRefresh : Application() {

}
        */