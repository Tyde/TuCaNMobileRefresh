package com.dalthed.tucanmobilerefresh.ui


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import com.dalthed.tucanmobilerefresh.R
import com.dalthed.tucanmobilerefresh.scraper.MainMenuScraper
import com.dalthed.tucanmobilerefresh.utils.CredentialStore
import com.dalthed.tucanmobilerefresh.utils.NavigationViewHelper
import kotlinx.android.synthetic.main.activity_main_menu.*



class MainMenuActivity : AppCompatActivity() {

    private var navigationViewHelper: NavigationViewHelper? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        setSupportActionBar(toolbar)
        navigationViewHelper = NavigationViewHelper(drawer_layout)
        val actionBar = supportActionBar
        navigationViewHelper?.setupActionBar(actionBar)


        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                single_fragment_activity_layout.orientation = LinearLayout.HORIZONTAL
                single_fragment_activity_container.layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT,1f
                )
                second_fragment_activity_container.layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT,1f
                )
                single_fragment_activity_container.requestLayout()
                second_fragment_activity_container.requestLayout()
            }
            else -> {
                single_fragment_activity_layout.orientation = LinearLayout.VERTICAL
            }
        }


        val model = ViewModelProviders.of(this).get(MainMenuScraper::class.java)

        if(savedInstanceState==null){
            model.addCookie(CredentialStore(this))
            model.startOnStoredData()
        }

        model.scraperData.observe(this, Observer {
            if (it?.todaysEvents?.size ?: 0 <= 2
                && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {


                single_fragment_activity_container.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.5f
                )
                single_fragment_activity_container.requestLayout()
            }
        })

        if (savedInstanceState == null) {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment: Fragment = TodayEventsFragment()
            transaction.add(R.id.single_fragment_activity_container, fragment)


            val messagesFragment = MessagesOverviewFragment()
            transaction.add(R.id.second_fragment_activity_container, messagesFragment)
            transaction.commit()
        } else {

        }



        nav_view.setNavigationItemSelectedListener(navigationViewHelper)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return navigationViewHelper?.onOptionsItemSelected(item) { super.onOptionsItemSelected(it) } ?: false
    }
}
