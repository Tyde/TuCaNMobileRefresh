package com.dalthed.tucanmobilerefresh.ui


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import com.dalthed.tucanmobilerefresh.R
import com.dalthed.tucanmobilerefresh.TuCanMobileRefresh
import com.dalthed.tucanmobilerefresh.scraper.*
import com.dalthed.tucanmobilerefresh.utils.CredentialStore
import kotlinx.android.synthetic.main.fragment_today_events.*
import okhttp3.HttpUrl
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter




class TodayEventsFragment : Fragment() {


    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var model: MainMenuScraper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = activity?.run {
            ViewModelProviders.of(this).get(MainMenuScraper::class.java)
        }




    }





    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_today_events, container, false)

        val tempListView = view.findViewById<ListView>(R.id.todayEventListView)
        Log.i(TuCanMobileRefresh.LOG_TAG, "append adapter")
        model?.let {
            tempListView.adapter = TodayEventsListAdapter(this.context!!, it)
        }
        model?.startOnStoredData()

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        model?.statusData?.observe(viewLifecycleOwner, Observer {
            when (it) {
                ScraperState.LOADING -> {

                }
                ScraperState.FINISHED -> {
                    Log.i(TuCanMobileRefresh.LOG_TAG, "Finished set")
                    /*
                    Log.i(TuCanMobileRefresh.LOG_TAG, "Finished set, injecting fake events")
                    val now = LocalDateTime.now()
                    val then = now.plusHours(3)
                    val url = HttpUrl.Builder().host("www.tucan.tu-darmstadt.de").scheme("https").build()
                    model?.injectFakeEvent(SimpleCalendarEvent(now, then, "Was es halt zu tun gibt", url))
                    model?.injectFakeEvent(SimpleCalendarEvent(then, then.plusHours(2), "Noch was?", url))*/
                }

            }
        })
        Log.i(TuCanMobileRefresh.LOG_TAG, "setting observer")
        model?.scraperData?.observe(viewLifecycleOwner, Observer {
            Log.i(TuCanMobileRefresh.LOG_TAG, "notify change")
            (todayEventListView?.adapter as TodayEventsListAdapter).notifyDataSetChanged()
        })

        model?.deepLoadingState?.observe(viewLifecycleOwner, Observer {
            when (it) {
                DeepLoadingState.FINISHED -> {
                    (todayEventListView?.adapter as TodayEventsListAdapter).switchDataSource()
                    (todayEventListView?.adapter as TodayEventsListAdapter).notifyDataSetChanged()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TuCanMobileRefresh.LOG_TAG,"Destroy TEF")

    }

}


class TodayEventsListAdapter(val context: Context, val dataModel: MainMenuScraper) : BaseAdapter() {
    val inflater = LayoutInflater.from(context)
    val df = TuCanMobileRefresh.simpleHourMinuteFormatter ?: DateTimeFormatter.BASIC_ISO_DATE
    var isExtendedDataSource = false

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var outView = convertView ?: inflater.inflate(R.layout.today_event_list_element, parent, false)
        val timeView = outView.findViewById<TextView>(R.id.timeTextView)
        val nameView = outView.findViewById<TextView>(R.id.nameTextView)
        if (!isExtendedDataSource) {
            val dataBase = dataModel.scraperData.value?.todaysEvents?.getOrNull(position)
            if (dataBase != null) {
                val begin = df.format(dataBase.startingTime)
                val end = df.format(dataBase.endingTime)
                timeView.text = context.getString(R.string.today_event_time, begin, end)
                nameView.text = dataBase.name
            } else {
                nameView.text = context.getString(R.string.error_no_data_todays_events)
            }
        } else {
            val dataBase = dataModel.extendedCalendarData.value?.getOrNull(position)
            if (dataBase != null) {
                val begin = df.format(dataBase.simpleEvent.startingTime)
                val end = df.format(dataBase.simpleEvent.endingTime)
                timeView.text = context.getString(R.string.today_event_time, begin, end)
                nameView.text = dataBase.simpleEvent.name
                val roomView = outView.findViewById<TextView>(R.id.roomTextView)
                roomView.text = dataBase.room
                roomView.visibility = View.VISIBLE
                outView.findViewById<ProgressBar>(R.id.roomLoaderProgressBar).visibility = View.GONE
            } else {
                nameView.text = context.getString(R.string.error_no_data_todays_events)
            }
        }



        return outView
    }

    override fun getItem(position: Int): Any {
        return if (!isExtendedDataSource) {
            dataModel.scraperData.value?.todaysEvents?.getOrNull(position) ?: SimpleCalendarEvent.empty()
        } else {
            dataModel.extendedCalendarData.value?.getOrNull(position) ?: RoomCalendarEvent(SimpleCalendarEvent.empty(),"")
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return if (!isExtendedDataSource) {
            dataModel.scraperData.value?.todaysEvents?.size ?: 0
        } else {
            dataModel.extendedCalendarData.value?.size ?: 0
        }
    }

    fun switchDataSource() {
        isExtendedDataSource = true
    }

}
