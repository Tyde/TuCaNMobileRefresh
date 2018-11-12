package com.dalthed.tucanmobilerefresh.ui


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat


import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import com.dalthed.tucanmobilerefresh.R
import com.dalthed.tucanmobilerefresh.scraper.MainMenuScraper
import com.dalthed.tucanmobilerefresh.utils.CredentialStore
import kotlinx.android.synthetic.main.activity_single_fragment.*

class SingleFragmentActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_fragment)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }


        val model = ViewModelProviders.of(this).get(MainMenuScraper::class.java)
        model.addCookie(CredentialStore(this))
        model.scraperData.observe(this, Observer {
            if (it?.todaysEvents?.size ?: 0 <= 2) {


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


        nav_view.setNavigationItemSelectedListener { item: MenuItem ->
            item.isChecked = true
            drawer_layout.closeDrawers()
            true
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
