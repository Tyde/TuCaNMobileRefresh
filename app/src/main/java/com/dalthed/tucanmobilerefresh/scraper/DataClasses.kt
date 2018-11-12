package com.dalthed.tucanmobilerefresh.scraper

import okhttp3.HttpUrl
import org.threeten.bp.LocalDateTime

data class SimpleCalendarEvent (val startingTime:LocalDateTime, val endingTime:LocalDateTime, val name:String, val link:HttpUrl) {
    companion object {
        fun empty(): SimpleCalendarEvent {
            val url = HttpUrl.Builder().host("wwww.tucan.tu-darmstadt.de").scheme("https").build()
            return SimpleCalendarEvent(LocalDateTime.MIN, LocalDateTime.MAX, "Empty event", url)
        }
    }
}

data class RoomCalendarEvent(val simpleEvent:SimpleCalendarEvent, val room:String)

data class SimpleMessage(val sender:String, val title:String, val link:HttpUrl, val dateTime: LocalDateTime) {
    companion object {
        fun empty():SimpleMessage {
            val url = HttpUrl.Builder().host("wwww.tucan.tu-darmstadt.de").scheme("https").build()
            return SimpleMessage("","empty message",url, LocalDateTime.now())
        }
    }
}
data class NavigationData(
    val courseCalaogueLink: HttpUrl,
    val courseTimetableLink: HttpUrl,
    val courseTimetableWeekLink: HttpUrl,
    val messagesLink: HttpUrl,
    val myModulesLink: HttpUrl,
    val myCoursesLink: HttpUrl,
    val myMajorsLink: HttpUrl,
    val courseRegistrationLink: HttpUrl,
    val myExamsLink: HttpUrl,
    val myExamResultsLink: HttpUrl,
    val myPerformanceRecordLink: HttpUrl
)
