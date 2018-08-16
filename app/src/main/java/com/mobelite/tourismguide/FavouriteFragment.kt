package com.mobelite.tourismguide


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import com.daimajia.swipe.SwipeLayout
import com.daimajia.swipe.util.Attributes
import com.mobelite.tourismguide.adapters.FavListViewAdapter
import com.mobelite.tourismguide.adapters.FavOfflineListViewAdapter
import com.mobelite.tourismguide.data.roomservice.database.RestaurantRepository
import com.mobelite.tourismguide.data.roomservice.local.RestaurantDataBase
import com.mobelite.tourismguide.data.roomservice.local.RestaurantDataSource
import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.PhoneGrantings
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class FavouriteFragment : Fragment() {

    private var OfflineData: MutableList<Restaurant> = ArrayList()
    private var mAdapter: FavListViewAdapter? = null
    private var mAdapterOfline: FavOfflineListViewAdapter? = null

    var list: ListView? = null

    //Room
    var compositeDisposable: CompositeDisposable? = null
    var restaurantRepository: RestaurantRepository? = null

    companion object {
        fun newInstance(): FavouriteFragment {
            return FavouriteFragment()
        }
    }

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_list, container, false)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar!!.title = "Favourites"

        list = root.findViewById(R.id.flistfav) as ListView

        if (PhoneGrantings.isNetworkAvailable(activity!!)) // online actions
            selectFav()
        else {  // offline actions
            Toast.makeText(context, "Loading offline", Toast.LENGTH_SHORT).show()

            println("loading offline:")
            selectFavOffline()
        }




        list!!.setOnItemClickListener { _, _, position, _ -> (list!!.getChildAt(position - list!!.firstVisiblePosition) as SwipeLayout).open(true) }
//        list!!.setOnTouchListener { _, _ ->
//            Log.e("ListView", "OnTouch")
//            false
//        }


//        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, view, position, id ->
//            Toast.makeText(mContext, "OnItemLongClickListener", Toast.LENGTH_SHORT).show()
//
//            true
//        }
//        list!!.setOnScrollListener(object : AbsListView.OnScrollListener {
//            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
//                Log.e("ListView", "onScrollStateChanged")
//            }
//
//            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
//
//            }
//        })
//
//        list!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                Log.e("ListView", "onItemSelected:$position")
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>) {
//                Log.e("ListView", "onNothingSelected:")
//            }
//        }


        return root
    }

    //===================================== loading online data from data base =====================================
    private fun selectFav() {
        disposable =
                restaurantServices.selectfav(PhoneGrantings.getSharedId(context!!))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    mAdapter = FavListViewAdapter(context!!, result as MutableList<Model.ResultRestaurant>, activity)
                                    mAdapter!!.mode = Attributes.Mode.Single
                                    list!!.adapter = mAdapter


                                },
                                { error -> println(error.message) }
                        )
    }

    //===================================== loading offline data from room =====================================
    private fun selectFavOffline() {
        compositeDisposable = CompositeDisposable()
        mAdapterOfline = FavOfflineListViewAdapter(activity!!, OfflineData, activity)
        mAdapterOfline!!.mode = Attributes.Mode.Single
        list!!.adapter = mAdapterOfline
        val restaurantDataBase = RestaurantDataBase.getInstance(activity!!)
        restaurantRepository = RestaurantRepository.getInstance(RestaurantDataSource.getInstance(restaurantDataBase.restaurantDAO()))

        loadOfflineData()
    }

    private fun loadOfflineData() {
        val disposable = restaurantRepository!!.getFavourites()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ restaurants -> onGetAllRestaurantSuccess(restaurants) }) { throwable ->
                    Toast.makeText(context, "" + throwable.message, Toast.LENGTH_SHORT).show()
                }

        compositeDisposable!!.add(disposable)
    }

    private fun onGetAllRestaurantSuccess(restaurants: List<Restaurant>?) {

        if (restaurants!=null) {
            OfflineData.clear()

            OfflineData.addAll(restaurants)
        }
        println(restaurants.toString())
        mAdapterOfline!!.notifyDataSetChanged()
    }

    override fun onDestroy() {
        if (!PhoneGrantings.isNetworkAvailable(activity!!))
            compositeDisposable!!.clear()
        super.onDestroy()
    }
}
