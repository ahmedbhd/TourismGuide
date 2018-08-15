package com.mobelite.tourismguide


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import com.daimajia.swipe.SwipeLayout
import com.daimajia.swipe.util.Attributes
import com.mobelite.tourismguide.adapters.HomeListAdapter
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.PhoneGrantings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class HomeFragment : Fragment() {

    private val names = arrayListOf("Kaushal", "Alex", "Ram", "Abhishek", "Narendra Modi")
    private var mAdapter: HomeListAdapter? = null
    private val mContext = context
    private var list: ListView? = null

    companion object {
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    private fun selectMy() {

        disposable =
                restaurantServices.selectmy(PhoneGrantings.getSharedId(context!!))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    mAdapter = HomeListAdapter(context!!, result as MutableList<Model.ResultRestaurant>, activity)
                                    mAdapter!!.mode = Attributes.Mode.Single
                                    println("my $result")
                                    list!!.adapter = mAdapter
                                },
                                { error -> println(error.message) }
                        )
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar!!.title = "Home"

        if (!PhoneGrantings.isNetworkAvailable(context!!))
            Toast.makeText(context, "Offline mode", Toast.LENGTH_SHORT).show()

        list = root.findViewById(R.id.listhome) as ListView
        //val list =listhome as ListView
        selectMy()

        list!!.setOnItemClickListener { _, _, position, _ -> (list!!.getChildAt(position - list!!.firstVisiblePosition) as SwipeLayout).open(true) }
        list!!.setOnTouchListener { _, _ ->
            Log.e("ListView", "OnTouch")
            false
        }
//        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, view, position, id ->
//            Toast.makeText(mContext, "OnItemLongClickListener", Toast.LENGTH_SHORT).show()
//
//            true
//        }
        list!!.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                Log.e("ListView", "onScrollStateChanged")
            }

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {

            }
        })

        list!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                Log.e("ListView", "onItemSelected:$position")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                Log.e("ListView", "onNothingSelected:")
            }
        }


        return root
    }


}
