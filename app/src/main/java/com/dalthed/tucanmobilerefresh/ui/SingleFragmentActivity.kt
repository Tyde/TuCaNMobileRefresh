package com.dalthed.tucanmobilerefresh.ui

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.dalthed.tucanmobilerefresh.R
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import com.dalthed.tucanmobilerefresh.utils.NavigationViewHelper
import kotlinx.android.synthetic.main.activity_single_fragment.*

enum class SingleFragmentActivityMode(val descriptor:String) {
    SINGLE_EVENT("SINGLE_EVENT"),
    SINGLE_MESSAGE("SINGLE_MESSAGE");

    companion object {
        val INTENT_ID = "activity_mode"
        fun intentToMode(intent: Intent):SingleFragmentActivityMode? {
            when(intent.getStringExtra(INTENT_ID)) {
                SINGLE_EVENT.descriptor -> {
                    return SINGLE_EVENT
                }
                SINGLE_MESSAGE.descriptor -> {
                    return SINGLE_MESSAGE
                }
                else -> {
                    return null
                }
            }
        }


    }
    fun toBundle():Bundle {
        val bundle = Bundle()
        bundle.putString(INTENT_ID,descriptor)
        return bundle
    }
}

class SingleFragmentActivity : AppCompatActivity() {
    private var navigationViewHelper: NavigationViewHelper? = null

    companion object {
        val URL_BUNDLE_KEY = "url_start"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TuCanMobileRefresh.LOG_TAG,"onCreate SFA")
        setContentView(R.layout.activity_single_fragment)
        setSupportActionBar(toolbar)
        navigationViewHelper = NavigationViewHelper(drawer_layout)
        val actionBar = supportActionBar
        navigationViewHelper?.setupActionBar(actionBar)

        nav_view.setNavigationItemSelectedListener(navigationViewHelper)


        val mode = SingleFragmentActivityMode.intentToMode(intent)
        val url = intent.getStringExtra(URL_BUNDLE_KEY)
        if (savedInstanceState == null) {
            val transaction = supportFragmentManager.beginTransaction()
            val bundle = Bundle()
            bundle.putString(URL_BUNDLE_KEY,url)
            when(mode) {
                SingleFragmentActivityMode.SINGLE_MESSAGE -> {
                    TODO()
                }
                SingleFragmentActivityMode.SINGLE_EVENT -> {
                    val fragment = SingleEventFragment()
                    fragment.arguments = bundle
                    transaction.add(R.id.single_fragment_activity_container,fragment)
                }
                null -> {
                    TODO()
                }
            }
            transaction.commit()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return navigationViewHelper?.onOptionsItemSelected(item) { super.onOptionsItemSelected(it) } ?: false
    }


}