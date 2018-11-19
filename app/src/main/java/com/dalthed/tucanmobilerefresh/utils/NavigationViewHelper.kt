package com.dalthed.tucanmobilerefresh.utils

import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBar
import android.view.MenuItem
import com.dalthed.tucanmobilerefresh.R


class NavigationViewHelper(val drawerLayout: DrawerLayout) : NavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        item.isChecked = true
        drawerLayout.closeDrawers()
        return true
    }


    fun onOptionsItemSelected(item: MenuItem, onElse: (MenuItem) -> Boolean): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> onElse(item)
        }
    }

    fun setupActionBar(actionBar: ActionBar?) {
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

}