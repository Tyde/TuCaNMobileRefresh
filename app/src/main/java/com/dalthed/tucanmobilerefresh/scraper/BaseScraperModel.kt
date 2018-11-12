package com.dalthed.tucanmobilerefresh.scraper

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.AsyncTask
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import com.dalthed.tucanmobilerefresh.utils.CredentialStore
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

enum class ScraperState {
    LOADING,
    SCRAPING,
    ERROR_NO_INTERNET,
    CUSTOM_ERROR,
    FINISHED
}

enum class DeepLoadingState {
    NOT_YET_STARTED,
    LOADING,
    SCRAPING,
    ERROR_NO_INTERNET,
    CUSTOM_ERROR,
    FINISHED
}

abstract class BaseScraperModel<T>() : ViewModel() {
    private val cookieJar = TuCaNCookieJar()
    protected val client = OkHttpClient.Builder().cookieJar(cookieJar).build()
    protected var document: Document? = null

    fun addCookie(credentialStore: CredentialStore) {
        cookieJar.injectCookie(credentialStore.getCookie())
    }


    val scraperData: MutableLiveData<T> by lazy {
        MutableLiveData<T>()
    }

    val statusData: MutableLiveData<ScraperState> by lazy {
        MutableLiveData<ScraperState>()
    }

    val deepLoadingState: MutableLiveData<DeepLoadingState> by lazy {
        MutableLiveData<DeepLoadingState>()
    }

    var customErrorMessage: String? = null

    /**
     * This function will do the asyncronous loading of the page
     */
    fun loadPage(url: String) {
        val request = Request.Builder().url(TuCanMobileRefresh.BASE_URL + url).get().build()
        statusData.postValue(ScraperState.LOADING)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                statusData.postValue(ScraperState.ERROR_NO_INTERNET)
            }

            override fun onResponse(call: Call, response: Response) {
                statusData.postValue(ScraperState.SCRAPING)
                response.body()?.string()?.let {
                    val doc = Jsoup.parse(it)
                    document = doc
                    scrapeData(doc)
                    statusData.postValue(ScraperState.FINISHED)
                }
            }

        })

    }

    fun injectDocument(doc: Document) {
        AsyncTask.execute {
            document = doc
            scrapeData(doc)
            statusData.postValue(ScraperState.FINISHED)
        }
    }

    abstract fun scrapeData(doc: Document)

    /**
     * Tries to safely get the navigation data. If this returns null, then the saved data isn't available anymore
     * and the page hasn't been loaded yet.
     */
    fun getNavigationData(): NavigationData? {
        return document?.let { TuCanMobileRefresh.navigationData ?: LoginModel.getNavigationLinks(it) }
    }
}


fun <T : Any> coalesce(vararg options: T?): T? = options.firstOrNull { it != null }

