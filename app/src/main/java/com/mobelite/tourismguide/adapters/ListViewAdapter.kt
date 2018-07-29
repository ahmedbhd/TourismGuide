package com.mobelite.tourismguide.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentActivity

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.daimajia.swipe.SimpleSwipeListener
import com.daimajia.swipe.SwipeLayout
import com.daimajia.swipe.adapters.BaseSwipeAdapter
import com.google.gson.Gson
import com.mobelite.tourismguide.DisResActivity
import com.mobelite.tourismguide.R
import com.mobelite.tourismguide.data.Model
import com.mobelite.tourismguide.data.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class ListViewAdapter(private val mContext: Context, private var favourites: MutableList<Model.ResultRestaurant>, private val manager: FragmentActivity?) : BaseSwipeAdapter() {
    override fun getCount(): Int {
        return favourites.count()
    }


    override fun getSwipeLayoutResourceId(position: Int): Int {
        return R.id.swipe
    }

    @SuppressLint("InflateParams")
    override fun generateView(position: Int, parent: ViewGroup): View {
        val v = LayoutInflater.from(mContext).inflate(R.layout.listview_item, null)
        val swipeLayout = v.findViewById(getSwipeLayoutResourceId(position)) as SwipeLayout
        swipeLayout.addSwipeListener(object : SimpleSwipeListener() {
            override fun onOpen(layout: SwipeLayout) {
                YoYo.with(Techniques.Tada).duration(500).delay(100).playOn(layout.findViewById(R.id.trash))
                YoYo.with(Techniques.Tada).duration(500).delay(100).playOn(layout.findViewById(R.id.eye))
            }
        })


        Log.d("position", favourites.toString())



        swipeLayout.setOnDoubleClickListener { _, _ -> Toast.makeText(mContext, "DoubleClick", Toast.LENGTH_SHORT).show() }
        v.findViewById<Button>(R.id.delete).setOnClickListener {
            Toast.makeText(mContext, "click delete", Toast.LENGTH_SHORT).show()
            delFav(favourites[position].id,position)
        }

        v.findViewById<Button>(R.id.open).setOnClickListener {
            //Toast.makeText(mContext, "click open", Toast.LENGTH_SHORT).show();
            val intent = Intent(mContext, DisResActivity().javaClass)
            val res: Model.ResultRestaurant?= favourites[position]
            println("res ${res.toString()}")
            intent.putExtra("myObject", Gson().toJson(res))
            manager!!.startActivity(intent)

        }
        return v
    }

    @SuppressLint("SetTextI18n")
    override fun fillValues(position: Int, convertView: View) {
        //val t = convertView.findViewById(R.id.position) as TextView
        //t.text = (position + 1).toString() + "."
        val p = favourites[position]

        val tvdesc = convertView.findViewById(R.id.favdec_s) as TextView
        val tvpw = convertView.findViewById(R.id.favpw_s) as TextView

        val tvHome = convertView.findViewById(R.id.favimg_s) as ImageView

        // Populate the data into the template view using the data object
        tvdesc.text = p.name
        //tvHome.setImageResource(p.imageressource);
        tvpw.text = p.description
//        Picasso.with(mContext)
//                .load(p.getImg())
//                .into(tvHome)
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    private fun delFav(id:Int , position: Int) {
        println ("$id $position")
        disposable =
                restaurantServices.deletefav("11111",id.toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println(result)
                                        if(result=="ok"){

                                            Toast.makeText(mContext, "The restaurant has been deleted", Toast.LENGTH_SHORT).show()

                                            favourites.remove(favourites[position])
                                            notifyDataSetChanged()

                                        }
                                    }
                                },
                                { error ->println( error.message) }
                        )
    }

}