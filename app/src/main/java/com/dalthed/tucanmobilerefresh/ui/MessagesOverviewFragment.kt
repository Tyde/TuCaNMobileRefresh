package com.dalthed.tucanmobilerefresh.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.dalthed.tucanmobilerefresh.R
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import com.dalthed.tucanmobilerefresh.scraper.MainMenuScraper
import com.dalthed.tucanmobilerefresh.scraper.SimpleMessage
import kotlinx.android.synthetic.main.fragment_message_overview.*
import kotlinx.android.synthetic.main.fragment_today_events.*

class MessagesOverviewFragment : Fragment() {
    private var model: MainMenuScraper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = activity?.run {
            ViewModelProviders.of(this).get(MainMenuScraper::class.java)
        }

    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        model?.scraperData?.observe(viewLifecycleOwner, Observer {
            Log.i(TuCanMobileRefresh.LOG_TAG, "notify change")
            (messagesListView?.adapter as MessagesListAdapter).notifyDataSetChanged()
        })
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_message_overview, container, false)
        val tempListView = view.findViewById<ListView>(R.id.messagesListView)
        Log.i(TuCanMobileRefresh.LOG_TAG, "append adapter")
        model?.let {
            tempListView.adapter = MessagesListAdapter(this.context!!, it)
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TuCanMobileRefresh.LOG_TAG,"Destroy MOF")
        //model?.scraperData?.remove
    }
}

class MessagesListAdapter(val context:Context, val dataModel:MainMenuScraper) : BaseAdapter() {
    val inflater = LayoutInflater.from(context)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var outView = convertView ?: inflater.inflate(R.layout.message_list_element, parent, false)
        val titleView = outView.findViewById<TextView>(R.id.messageTitleTextView)
        val senderView = outView.findViewById<TextView>(R.id.senderTextView)
        val dataBase = dataModel.scraperData.value?.messages?.getOrNull(position)
        if (dataBase != null) {
            senderView.text = dataBase.sender
            titleView.text = dataBase.title
        } else {
            titleView.text = context.getString(R.string.error_no_data_todays_events)
        }
        return outView
    }

    override fun getItem(position: Int): Any {
        return dataModel.scraperData.value?.messages?.getOrNull(position)?:SimpleMessage.empty()
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return dataModel.scraperData.value?.messages?.size?:0
    }

}