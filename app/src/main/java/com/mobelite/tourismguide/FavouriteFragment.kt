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
import com.mobelite.tourismguide.adapters.ListViewAdapter
import com.mobelite.tourismguide.data.roomservice.database.RestaurantRepository
import com.mobelite.tourismguide.data.roomservice.local.RestaurantDataBase
import com.mobelite.tourismguide.data.roomservice.local.RestaurantDataSource
import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.PhoneGrantings
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class FavouriteFragment : Fragment() {

    private var OfflineData: MutableList<Model.ResultRestaurant> = ArrayList()
    private var mAdapter: ListViewAdapter? = null
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

    private fun selectFav() {


        val restaurantDataBase = RestaurantDataBase.getInstance(activity!!)
        restaurantRepository = RestaurantRepository.getInstance(RestaurantDataSource.getInstance(restaurantDataBase.restaurantDAO()))
        deleteAllOfflineData()

        disposable =
                restaurantServices.selectfav(PhoneGrantings.getSharedId(context!!))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    mAdapter = ListViewAdapter(context!!, result as MutableList<Model.ResultRestaurant>, activity)
                                    mAdapter!!.mode = Attributes.Mode.Single
                                    list!!.adapter = mAdapter
                                    result.forEach { r ->
                                        addOfflineRestaurant(Restaurant(r.id, r.name, r.phone, r.description, r.lat, r.lng, r.image, r.userid))
                                        println("adding to room")
                                    }

                                },
                                { error -> println(error.message) }
                        )
    }

    private fun selectFavOffline() {
        compositeDisposable = CompositeDisposable()
        mAdapter = ListViewAdapter(activity!!, OfflineData, activity)
        mAdapter!!.mode = Attributes.Mode.Single
        list!!.adapter = mAdapter
        val restaurantDataBase = RestaurantDataBase.getInstance(activity!!)
        restaurantRepository = RestaurantRepository.getInstance(RestaurantDataSource.getInstance(restaurantDataBase.restaurantDAO()))
//        deleteAllOfflineData()
//        addOfflineRestaurant(Restaurant(0,"name","phone","desc","lat","lng","imag","user"))

        loadOfflineData()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_list, container, false)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar!!.title = "Favourites"

        list = root.findViewById(R.id.flistfav) as ListView

        if (PhoneGrantings.isNetworkAvailable(activity!!))
            selectFav()
        else {
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

    private fun loadOfflineData() {
        val disposable = restaurantRepository!!.allRestaurants
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ restaurants -> onGetAllRestaurantSuccess(restaurants) }) { throwable ->
                    Toast.makeText(context, "" + throwable.message, Toast.LENGTH_SHORT).show()
                }

        compositeDisposable!!.add(disposable)
    }


    private fun onGetAllRestaurantSuccess(restaurants: List<Restaurant>?) {
        OfflineData.clear()
        val rests: ArrayList<Model.ResultRestaurant> = ArrayList()
        restaurants!!.forEach { r ->
            val rest = Model.ResultRestaurant(r.ID, r.Name!!, r.Phone!!, r.Desc!!, r.Lat!!, r.Lng!!, r.Image!!, r.UserID!!)
            rests.add(rest)

        }
        OfflineData.addAll(rests)
        println(restaurants.toString())
        mAdapter!!.notifyDataSetChanged()
    }

    private fun deleteAllOfflineData() {
        val disposable = Observable.create(ObservableOnSubscribe<Any> { e ->
            restaurantRepository!!.deleteAll()
            e.onComplete()
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ },
                        { throwable ->
                            Toast.makeText(context, throwable.message, Toast.LENGTH_SHORT).show()
                        },
                        { })
        if (!PhoneGrantings.isNetworkAvailable(activity!!))
            compositeDisposable!!.addAll(disposable)
    }


    private fun addOfflineRestaurant(restaurant: Restaurant) {
        val disposable = Observable.create(ObservableOnSubscribe<Any> { e ->
            println(restaurant)
            restaurantRepository!!.insertRestaurant(restaurant)
            e.onComplete()
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ },
                        { throwable ->
                            Toast.makeText(context, "" + throwable.message, Toast.LENGTH_SHORT).show()
                        },
                        {
                            //                            loadOfflineData()
                        })
        if (!PhoneGrantings.isNetworkAvailable(activity!!))
            compositeDisposable!!.addAll(disposable)


    }

    override fun onDestroy() {
        if (!PhoneGrantings.isNetworkAvailable(activity!!))
            compositeDisposable!!.clear()
        super.onDestroy()
    }
}