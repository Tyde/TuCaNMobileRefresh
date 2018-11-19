package com.dalthed.tucanmobilerefresh.scraper

import org.jsoup.nodes.Document

class SingleEventScraper : BaseScraperModel<SingleEventData>() {
    override fun scrapeData(doc: Document) {
        val infoStrings = doc.select(".tbdata > p").map { paragraph ->
            val split = paragraph.text().split(":")

            Pair(split.getOrNull(0)?:"", split.getOrNull(1)?.trim()?:"")
        }.toMap()
        val title = doc.select("form > h1").first()?.text()
            ?.split(" ", limit = 2)?.getOrNull(1) ?: ""
        val data = SingleEventData(title,infoStrings)
        scraperData.postValue(data)
    }

}

data class SingleEventData(
    val title: String,
    val informationStrings: Map<String, String>
)