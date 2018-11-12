package com.dalthed.tucanmobilerefresh.utils


import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.dalthed.tucanmobilerefresh.scraper.TuCaNCookieJar
import okhttp3.Cookie

class CredentialStore(val context: Context) {
    private val PREFERENCE_NAME = "com.dalthed.tucanmobilerefresh"
    private val USERNAME_KEY = "uname"
    private val PASSWORD_KEY = "password"
    private val START_PAGE_ARGUMENT_KEY = "spage_arg_key"
    private val COOKIE_EXPIRES_KEY = "COOKIE_EXPIRES"
    private val COOKIES_KEY = "cookies"

    val prefs: SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(username: String, password: String) {
        getCredentials()?.run {
            if (this.second.equals(password) && this.first.equals(username)) {
                return
            }
        }
        val encryptedPW = Base64.encodeToString(Encryption.encrypt(password,context), Base64.DEFAULT)
        val editor = prefs.edit()
        editor.putString(USERNAME_KEY, username)
        editor.putString(PASSWORD_KEY, encryptedPW)
        editor.apply()
    }

    fun getCredentials(): Pair<String, String>? {
        val username = prefs.getString(USERNAME_KEY, "")
        val encryptedPassword = prefs.getString(PASSWORD_KEY, "")

        if (!username.isEmpty() && !encryptedPassword.isEmpty()) {
            val bytes = Base64.decode(encryptedPassword, Base64.DEFAULT)
            val password = Encryption.decrypt(bytes,context)
            return Pair(username, password)
        } else {
            return null
        }
    }
    val BASE_DOMAIN = "www.tucan.tu-darmstadt.de"

    fun saveCookie(cookieJar: TuCaNCookieJar) {

        val cookie = cookieJar.getTuCaNCookies().getOrNull(0)
        if(cookie!=null) {
            val cookievalue = Base64.encodeToString(Encryption.encrypt(cookie.value(),context), Base64.DEFAULT)
            val editor = prefs.edit()
            editor.putString(COOKIES_KEY, cookievalue)
            editor.putLong(COOKIE_EXPIRES_KEY, cookie.expiresAt())
            editor.apply()
        }

    }

    fun getCookie(): Cookie {
        val value = Encryption.decrypt(Base64.decode(prefs.getString(COOKIES_KEY, ""),Base64.DEFAULT),context)
        val expires = prefs.getLong(COOKIE_EXPIRES_KEY, System.currentTimeMillis())
        val cookie = Cookie.Builder()
            .expiresAt(expires)
            .domain(BASE_DOMAIN)
            .path("/scripts")
            .value(value)
            .name("cnsc")
            .build()

        return cookie

    }

    fun saveStartPageArg(startPageArgument:String) {
        val editor = prefs.edit()
        editor.putString(START_PAGE_ARGUMENT_KEY, startPageArgument)
        editor.apply()
    }

    fun getStartPageArg():String {
        return prefs.getString(START_PAGE_ARGUMENT_KEY,"")
    }


    fun deleteCredentials() {
        val editor = prefs.edit()
        editor.remove(USERNAME_KEY)
        editor.remove(PASSWORD_KEY)
        editor.apply()
    }
}