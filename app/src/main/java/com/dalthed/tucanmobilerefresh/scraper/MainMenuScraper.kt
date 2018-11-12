package com.dalthed.tucanmobilerefresh.scraper


import android.arch.lifecycle.MutableLiveData
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.io.IOException
import java.util.*


class MainMenuScraper() : BaseScraperModel<MainMenuData>() {


    override fun scrapeData(doc: Document) {
        val messages = scrapeMessages()
        scraperData.postValue(MainMenuData(getTodaysEvents(doc),messages))
    }

    val extendedCalendarData: MutableLiveData<List<RoomCalendarEvent>> by lazy {
        MutableLiveData<List<RoomCalendarEvent>>()
    }


    private fun getTodaysEvents(doc: Document): List<SimpleCalendarEvent> {
        val eventTable = doc.select("table.nb").first()

        if (eventTable == null) {
            //No event found
            return emptyList()
        } else {
            val tbdata = eventTable.select("tr.tbdata")
            if (tbdata.first().select("td").size == 5) {
                //5 columns ==> Means no events today
                return emptyList()
            } else {


                val returnList = mutableListOf<SimpleCalendarEvent>()
                val df = TuCanMobileRefresh.simpleHourMinuteFormatter ?: DateTimeFormatter.BASIC_ISO_DATE
                tbdata.forEach { row: Element ->
                    val nameLink = row.select("td[headers=Name]").select("a").first()
                    val name = nameLink?.text()
                    val link =
                        HttpUrl.parse(nameLink?.attr("href")?.let { lnk -> TuCanMobileRefresh.BASE_URL_NSL + lnk })
                    val cols = row.select("td")
                    val startingTimeString = cols?.get(2)?.select("a")?.first()?.text()
                    val startingTime = startingTimeString?.let { LocalDateTime.parse(it, df) }
                    val endingTimeString = cols?.get(3)?.select("a")?.first()?.text()
                    val endingTime = endingTimeString?.let { df.parse(it) }

                    if (name != null && link != null && startingTime != null && endingTime != null) {
                        returnList.add(
                            SimpleCalendarEvent(
                                LocalDateTime.from(startingTime),
                                LocalDateTime.from(endingTime),
                                name,
                                link
                            )
                        )
                    }
                }
                loadRoomInfo()
                return returnList
            }
        }
    }

    fun loadRoomInfo() {
        val nd = getNavigationData()
        if (nd != null) {
            deepLoadingState.postValue(DeepLoadingState.LOADING)
            val request = Request.Builder().url(nd.courseTimetableWeekLink).get().build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    deepLoadingState.postValue(DeepLoadingState.ERROR_NO_INTERNET)
                }

                override fun onResponse(call: Call, response: Response) {
                    deepLoadingState.postValue(DeepLoadingState.SCRAPING)
                    response.body()?.string()?.let {
                        val deepDoc = Jsoup.parse(it)
                        scrapeRoomInfo(deepDoc)
                        deepLoadingState.postValue(DeepLoadingState.FINISHED)
                    }
                }

            })
        }

    }


    fun scrapeRoomInfo(deepDoc: Document) {
        val dayOfWeek = LocalDate.now().dayOfWeek
        //val colToday = dayOfWeek.value-1 // Monday == 1
        val dowName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMANY)
        val todayAppointments = deepDoc.select("td.appointment").filter {
            it.attr("abbr")?.contains(dowName) ?: false
        }
        val df = TuCanMobileRefresh.simpleHourMinuteFormatter ?: DateTimeFormatter.BASIC_ISO_DATE
        val extendedRoomInfo = mutableListOf<RoomCalendarEvent>()
        todayAppointments.map {
            val link = it.select("a.link").first()
            val timePeriodSpans = it.select("span.timePeriod")
            val timeRoomPair = if (timePeriodSpans.size == 1) {
                val time = timePeriodSpans.first()
                val room = time.select("a.arrow").first()?.text() ?: ""
                val timeText = time.ownText() ?: ""
                Pair(timeText, room)
            } else {
                val timeText = timePeriodSpans.first().text() ?: ""
                val room = timePeriodSpans.getOrNull(1)?.text() ?: ""
                Pair(timeText, room)
            }


            val title = link?.attr("title") ?: ""
            val linkUrl = HttpUrl.parse(link?.attr("href")?.let { lnk -> TuCanMobileRefresh.BASE_URL_NSL + lnk } ?: "")
            val room = timeRoomPair.second
            val timeText = timeRoomPair.first
            val timeSplit = timeText.split("-")
            val startingTimeString = timeSplit.getOrNull(0)
            val startingTime = startingTimeString?.let { df.parse(it.trim()) }
            val endingTimeString = timeSplit.getOrNull(1)
            val endingTime = endingTimeString?.let { df.parse(it.trim()) }

            if (linkUrl != null) {
                val rcEvent = RoomCalendarEvent(
                    SimpleCalendarEvent(
                        LocalDateTime.from(startingTime),
                        LocalDateTime.from(endingTime),
                        title,
                        linkUrl
                    ), room
                )
                extendedRoomInfo.add(rcEvent)

            }
        }
        extendedCalendarData.postValue(extendedRoomInfo)
    }


    fun scrapeMessages(): List<SimpleMessage> {
        val rows = document?.select("table[summary=Eingegangene Nachrichten]")?.select("tr.tbdata")
        val returnList = mutableListOf<SimpleMessage>()
        rows?.forEach { row ->
            val cols = row.select("td")
            val sender = cols.getOrNull(2)?.text()
            val title = cols.getOrNull(3)?.text()
            val linkString = cols.getOrNull(2)?.select("a")?.attr("href")?.let {
                TuCanMobileRefresh.BASE_URL_NSL + it }
            val link = linkString?.let { HttpUrl.parse(it) }
            if (sender!=null && title !=null && link != null){
                returnList.add(SimpleMessage(sender,title,link))
            }

        }
        return returnList
    }


    fun injectFakeEvent(event: SimpleCalendarEvent) {
        val newList = scraperData.value?.todaysEvents?.toMutableList() ?: mutableListOf()
        val messages = scraperData.value?.messages ?: mutableListOf()
        newList.add(event)
        scraperData.value = MainMenuData(newList.toList(),messages)
    }


    fun startOnStoredData() {
        TuCanMobileRefresh.temporaryDocumentStore?.let { injectDocument(it) }
    }

}

data class MainMenuData(val todaysEvents: List<SimpleCalendarEvent>, val messages:List<SimpleMessage>)
