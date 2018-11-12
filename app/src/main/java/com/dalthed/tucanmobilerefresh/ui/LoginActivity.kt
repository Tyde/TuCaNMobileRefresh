package com.dalthed.tucanmobilerefresh.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.dalthed.tucanmobilerefresh.R
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import com.dalthed.tucanmobilerefresh.scraper.LoginModel
import com.dalthed.tucanmobilerefresh.scraper.LoginState
import com.dalthed.tucanmobilerefresh.scraper.TuCaNCookieJar
import com.dalthed.tucanmobilerefresh.utils.CredentialStore
import kotlinx.android.synthetic.main.activity_login.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */

    private var model: LoginModel? = null
    private var credentialStore: CredentialStore? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        credentialStore = CredentialStore(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up the login form.


        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        //credentialStore?.saveStartPageArg("-N0000014")
        model = ViewModelProviders.of(this).get(LoginModel::class.java)
        email_sign_in_button.setOnClickListener { attemptLogin() }

        model?.loginState?.observe(this, Observer {
            when (it) {
                LoginState.LOGGING_IN -> {
                    login_progress.visibility = View.VISIBLE
                    // show spinner
                }
                LoginState.WAITING -> {
                    login_progress.visibility = View.GONE
                    // do nothing
                }
                LoginState.LOGIN_SUCCESSFUL -> {
                    login_progress.visibility = View.GONE
                    Log.i(TuCanMobileRefresh.LOG_TAG, "Logged in")
                    val tuIDStr = email.text.toString()
                    val passwordStr = password.text.toString()
                    credentialStore?.saveCredentials(tuIDStr, passwordStr)
                    credentialStore?.saveCookie(model?.client?.cookieJar() as TuCaNCookieJar)
                    credentialStore?.saveStartPageArg(model?.sessionArgument ?: "")
                    val intent = Intent(this, MainMenuActivity::class.java)
                    this.startActivity(intent)
                    //this.startAct
                    //switch to next activity
                }
                LoginState.LOGIN_FAILED_NO_INTERNET -> {
                    login_progress.visibility = View.GONE
                    Snackbar.make(loginCoordinatorLayout, getString(R.string.login_no_internet), Snackbar.LENGTH_LONG)
                    // show no Internet error
                }
                LoginState.LOGIN_FAILED_WRONG_CREDENTIALS -> {
                    login_progress.visibility = View.GONE
                    Snackbar.make(loginCoordinatorLayout, getString(R.string.login_failed), Snackbar.LENGTH_LONG)
                        .show()
                    Log.i(TuCanMobileRefresh.LOG_TAG, "Wrong credentials")
                    // ask for new credentials
                }
                LoginState.LOGIN_FAILED_UNKNOWN_REASON -> {
                    login_progress.visibility = View.GONE
                    Snackbar.make(
                        loginCoordinatorLayout,
                        getString(R.string.login_failed_unkown_reason),
                        Snackbar.LENGTH_LONG
                    )
                        .show()

                }
            }
        })


        val credentials = credentialStore?.getCredentials()
        credentials?.run {
            email.setText(this.first)
            password.setText(this.second)
            saveLoginCheckBox.isChecked = true
        }


    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        credentialStore?.let {
            val startPageArgument = it.getStartPageArg()
            if(!startPageArgument.isEmpty()) {
                it.getCookie()?.let { cookie ->
                    model?.tryOldCookie(cookie, startPageArgument)
                }
            }

        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        val tuIDStr = email.text.toString()
        val passwordStr = password.text.toString()
        if (!saveLoginCheckBox.isChecked) {
            credentialStore?.deleteCredentials()
        }

        // Reset errors.
        email.error = null
        password.error = null

        // Store values at the time of the login attempt.


        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(passwordStr)) {
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(tuIDStr)) {
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            model?.doLogin(tuIDStr, passwordStr)
        }


    }


}
