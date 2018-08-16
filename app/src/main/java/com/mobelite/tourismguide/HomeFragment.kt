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
import com.mobelite.tourismguide.adapters.HomeListAdapter
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


    //Room
    var compositeDisposable: CompositeDisposable? = null
    var restaurantRepository: RestaurantRepository? = null

    private var OfflineData: MutableList<Model.ResultRestaurant> = ArrayList()


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar!!.title = "Home"



        list = root.findViewById(R.id.listhome) as ListView
        //val list =listhome as ListView


        list!!.setOnItemClickListener { _, _, position, _ -> (list!!.getChildAt(position - list!!.firstVisiblePosition) as SwipeLayout).open(true) }

        if (PhoneGrantings.isNetworkAvailable(activity!!)) // online actions
            selectMy()
        else { // offline actions
            Toast.makeText(context, "Loading offline", Toast.LENGTH_SHORT).show()

            println("loading offline:")
            selectMyOffline()
        }

        return root
    }


    //================================== load online data from data base ==================================
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

    //================================== load offline data from room ==================================
    private fun selectMyOffline() {
        compositeDisposable = CompositeDisposable()
        mAdapter = HomeListAdapter(activity!!, OfflineData, activity)
        mAdapter!!.mode = Attributes.Mode.Single
        list!!.adapter = mAdapter
        val restaurantDataBase = RestaurantDataBase.getInstance(activity!!)
        restaurantRepository = RestaurantRepository.getInstance(RestaurantDataSource.getInstance(restaurantDataBase.restaurantDAO()))
//        deleteAllOfflineData()
//        addOfflineRestaurant(Restaurant(0,"name","phone","desc","lat","lng","imag","user"))

        loadOfflineData()
    }


    private fun loadOfflineData() {
        val disposable = restaurantRepository!!.getMyRestaurants(PhoneGrantings.getSharedId(context!!))
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
        println(OfflineData.toString())
        mAdapter!!.notifyDataSetChanged()
    }


}
