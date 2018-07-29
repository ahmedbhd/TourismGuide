package com.mobelite.tourismguide


import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListView
import com.daimajia.swipe.SwipeLayout
import com.daimajia.swipe.util.Attributes
import com.mobelite.tourismguide.adapters.ListViewAdapter
import com.mobelite.tourismguide.data.Model
import com.mobelite.tourismguide.data.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


class ListFragment : Fragment() {

    private val names = arrayListOf("Kaushal","Alex","Ram","Abhishek","Narendra Modi")
    private var mAdapter: ListViewAdapter? = null
    private val mContext = context
    var list:ListView?=null

    companion object {
        fun newInstance(): ListFragment {
            return ListFragment()
        }
    }

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    private fun selecFav() {
        disposable =
                restaurantServices.selectfav("11111")
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    mAdapter = ListViewAdapter(context!!, result as MutableList<Model.ResultRestaurant>,activity)
                                    mAdapter!!.mode = Attributes.Mode.Single
                                    list!!.adapter = mAdapter
                                },
                                { error ->println( error.message) }
                        )
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_list, container, false)
        list = root.findViewById(R.id.flistfav) as ListView

        selecFav()
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
