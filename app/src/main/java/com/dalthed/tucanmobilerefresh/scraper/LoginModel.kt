package com.dalthed.tucanmobilerefresh.scraper

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import com.dalthed.tucanmobilerefresh.utils.ScraperUnsuccessfulException
import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

/*
enum class LoginState {
    WAITING,
    LOGGING_IN,
    LOGIN_SUCCESSFUL,
    LOGIN_FAILED_WRONG_CREDENTIALS,
    LOGIN_FAILED_NO_INTERNET,
    LOGIN_FAILED_UNKNOWN_REASON
}
*/

sealed class LoginState
object LOGIN_WAITING : LoginState()
object LOGIN_LOGGING_IN : LoginState()
object LOGIN_SUCCESSFUL : LoginState()
object LOGIN_FAILED_WRONG_CREDENTIALS : LoginState()
object LOGIN_FAILED_NO_INTERNET : LoginState()
data class LOGIN_FAILED_UNKNOWN_REASON(val exception: Exception) : LoginState()

class LoginModel : ViewModel() {
    val client = OkHttpClient.Builder().cookieJar(TuCaNCookieJar()).addInterceptor(LoggingInterceptor()).build()
    /*val finalBody: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }*/
    var sessionArgument: String? = null
    val requestSteps: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    val loginState: MutableLiveData<LoginState> by lazy {
        MutableLiveData<LoginState>()
    }


    init {
        loginState.value = LOGIN_WAITING
    }


    fun doLogin(user: String, pass: String) {
        requestSteps.value = 0
        loginState.value = LOGIN_LOGGING_IN
        val body = FormBody.Builder()
            .add("usrname", user)
            .add("pass", pass)
            .add("APPNAME", "CampusNet")
            .add("PRGNAME", "LOGINCHECK")
            .add("ARGUMENTS", "clino,usrname,pass,menuno,menu_type,browser,platform")
            .add("clino", "000000000000001")
            .add("menuno", "000344")
            .add("menu_type", "classic")
            .add("browser", "")
            .add("platform", "")
            .build()
        val request = Request.Builder()
            .url(TuCanMobileRefresh.BASE_URL + "scripts/mgrqispi.dll")
            .post(body)
            .build()

        requestSteps.value = 1
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                loginState.postValue(LOGIN_FAILED_NO_INTERNET)
            }

            override fun onResponse(call: Call, response: Response) {
                evaluateResult(response)
            }
        })
    }

    private fun evaluateResult(response: Response) {
        val refresh: String? = response.header("REFRESH")
        if (refresh != null && !refresh.isEmpty()) {
            //in first stage
            val splits = refresh.split("URL=")
            if (splits.size > 1) {
                val url = splits[1].substring(1)
                stageTwo(url)
            } else {
                loginState.postValue(
                    LOGIN_FAILED_UNKNOWN_REASON(
                        ScraperUnsuccessfulException("Refresh URL not found")
                    )
                )
            }
        } else {
            val body = response.body()?.string()
            //Log.i(TuCanMobileRefresh.LOG_TAG,"the body is: "+body)
            if (body != null) {
                val metaElements = Jsoup.parse(body).select("html head meta[http-equiv=refresh]")
                val refresh = metaElements?.attr("content")

                if (refresh != null && !refresh.isEmpty()) {
                    // second Stage
                    val splits = refresh?.split("URL=")
                    if (splits != null && splits.size > 1) {
                        val url = splits[1].substring(1)
                        stageTwo(url)
                    } else {
                        loginState.postValue(
                            LOGIN_FAILED_UNKNOWN_REASON(
                                ScraperUnsuccessfulException("Refresh URL not found (Stage 2)")
                            )
                        )
                    }
                } else {
                    val doc = Jsoup.parse(body)
                    val numInputUserName = doc.select("input[name=usrname]")?.size ?: 0
                    if (numInputUserName > 0) {
                        //wrong credentials
                        loginState.postValue(LOGIN_FAILED_WRONG_CREDENTIALS)
                    } else {
                        //final Stage - start scraper

                        val lcURL = response.request().url()
                        finishUpCorrectLogin(lcURL, doc)


                    }

                }
            } else {
                loginState.postValue(LOGIN_FAILED_UNKNOWN_REASON(
                    ScraperUnsuccessfulException("Empty body response from server")
                ))
            }
        }
    }

    private fun finishUpCorrectLogin(lcURL: HttpUrl?, doc: Document) {
        sessionArgument =
                lcURL?.encodedQuery()?.split("ARGUMENTS=")
                    ?.getOrNull(1)?.split(",")?.getOrNull(0)
        val navigationData = getNavigationLinks(doc)
        if (navigationData == null) {
            loginState.postValue(LOGIN_FAILED_UNKNOWN_REASON(
                ScraperUnsuccessfulException("Unable to scrape links from TuCaN")
            ))
        }
        TuCanMobileRefresh.navigationData = navigationData
        TuCanMobileRefresh.temporaryDocumentStore = doc
        loginState.postValue(LOGIN_SUCCESSFUL)
    }


    private fun stageTwo(url: String) {
        requestSteps.postValue(requestSteps.value?.plus(1)?.or(0))
        val request = Request.Builder().url(TuCanMobileRefresh.BASE_URL + url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                loginState.postValue(LOGIN_FAILED_NO_INTERNET)
                Log.e(TuCanMobileRefresh.LOG_TAG, e.toString())
            }

            override fun onResponse(call: Call, response: Response) {
                evaluateResult(response)
            }

        })
    }


    fun tryOldCookie(cookie: Cookie, argument: String) {
        (client.cookieJar() as TuCaNCookieJar).injectCookie(cookie)
        loginState.postValue(LOGIN_LOGGING_IN)

        val startpage =
            "${TuCanMobileRefresh.BASE_URL}scripts/mgrqispi.dll?APPNAME=CampusNet&PRGNAME=MLSSTART&ARGUMENTS=$argument,-N000019"
        val request = Request.Builder().url(startpage).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                loginState.postValue(LOGIN_FAILED_NO_INTERNET)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                val doc = Jsoup.parse(body)
                val numInputUserName = doc.select("input[name=usrname]")?.size ?: 0
                if (numInputUserName > 0) {
                    //wrong credentials
                    sessionArgument = ""
                    Log.w(TuCanMobileRefresh.LOG_TAG, "Cookie no longer correct")
                    loginState.postValue(LOGIN_FAILED_WRONG_CREDENTIALS)
                } else {
                    sessionArgument = argument
                    Log.i(TuCanMobileRefresh.LOG_TAG, "Cookie correctm go on")
                    //final Stage - start scraper
                    finishUpCorrectLogin(HttpUrl.parse(startpage), doc)
                }
            }

        })

    }

    companion object {

        fun getNavigationLinks(document: Document): NavigationData? {

            val courseCalaogueLink = document.select("a.link000326")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val courseTimetableLink = document.select("a.link000271")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val courseTimetableWeekLink = document.select("a.link000270")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val messagesLink = document.select("a.link000299")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val myModulesLink = document.select("a.link000275")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val myCoursesLink = document.select("a.link000274")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val myMajorsLink = document.select("a.link000307")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val courseRegistrationLink = document.select("a.link000311")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val myExamsLink = document.select("a.link000318")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val myExamResultsLink = document.select("a.link000323")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            val myPerformanceRecordLink = document.select("a.link000316")?.attr("href")?.let {
                HttpUrl.parse(TuCanMobileRefresh.BASE_URL_NSL + it)
            }
            if (courseCalaogueLink != null
                && courseTimetableLink != null
                && courseTimetableWeekLink != null
                && messagesLink != null
                && myModulesLink != null
                && myCoursesLink != null
                && myMajorsLink != null
                && courseRegistrationLink != null
                && myExamsLink != null
                && myExamResultsLink != null
                && myPerformanceRecordLink != null
            ) {
                return NavigationData(
                    courseCalaogueLink,
                    courseTimetableLink,
                    courseTimetableWeekLink,
                    messagesLink,
                    myModulesLink,
                    myCoursesLink,
                    myMajorsLink,
                    courseRegistrationLink,
                    myExamsLink,
                    myExamResultsLink,
                    myPerformanceRecordLink
                )
            } else {
                return null
            }
        }
    }


}


class TuCaNCookieJar() : CookieJar {
    val cookieList = mutableListOf<Cookie>()


    override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
        cookieList.removeAll { it.matches(url) }

        cookieList.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        return cookieList
    }

    fun getTuCaNCookies(): List<Cookie> {
        return cookieList
    }

    fun injectCookie(cookie: Cookie) {
        cookieList.add(cookie)
    }

}

internal class LoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val t1 = System.nanoTime()
        Log.i(
            TuCanMobileRefresh.LOG_TAG,
            String.format(
                "Sending request %s on %s%n%s",
                request.url(), chain.connection(), request.headers()
            )
        )

        val response = chain.proceed(request)

        val t2 = System.nanoTime()
        Log.i(
            TuCanMobileRefresh.LOG_TAG,
            String.format(
                "Received response for %s in %.1fms%n%s",
                response.request().url(), (t2 - t1) / 1e6, response.headers()
            )
        )
        //Log.i(TuCanMobileRefresh.LOG_TAG, response.body()?.string())

        return response
    }
}