package com.dalthed.tucanmobilerefresh.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dalthed.tucanmobilerefresh.R
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh

import com.dalthed.tucanmobilerefresh.scraper.SingleEventScraper
import com.dalthed.tucanmobilerefresh.utils.CredentialStore

class SingleEventFragment : Fragment() {
    private var model: SingleEventScraper? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = activity?.run {
            ViewModelProviders.of(this).get(SingleEventScraper::class.java)
        }
        val url = arguments?.getString(SingleFragmentActivity.URL_BUNDLE_KEY)
        if(url != null) {
            context?.let { model?.addCookie(CredentialStore(it)) }
            model?.loadPage(url,true)
        } else {
            TODO("This should not happen, URL should be given")
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_single_message,container,false)


        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        model?.scraperData?.observe(viewLifecycleOwner, Observer {
                Log.i(TuCanMobileRefresh.LOG_TAG,"SingleEventData: $it")
        })
    }

}