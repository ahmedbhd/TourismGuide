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
import com.bumptech.glide.Glide
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.daimajia.swipe.SimpleSwipeListener
import com.daimajia.swipe.SwipeLayout
import com.daimajia.swipe.adapters.BaseSwipeAdapter
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.mobelite.tourismguide.DisResActivity
import com.mobelite.tourismguide.R
import com.mobelite.tourismguide.data.roomservice.model.Restaurant


// Offline favourite list adapater
class FavOfflineListViewAdapter(private val mContext: Context, private var favourites: MutableList<Restaurant>, private val manager: FragmentActivity?) : BaseSwipeAdapter() {
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

        // delete action only available online
        v.findViewById<Button>(R.id.delete).setOnClickListener {
            Toast.makeText(mContext, "Internet is required for this feature", Toast.LENGTH_SHORT).show()

        }

        // display the details in detail activity
        v.findViewById<Button>(R.id.open).setOnClickListener {
            val intent = Intent(mContext, DisResActivity().javaClass)
            val res: Restaurant? = favourites[position]
            println("res ${res.toString()}")
            intent.putExtra("myObject2", Gson().toJson(res))
            manager!!.startActivity(intent)

        }
        return v
    }

    @SuppressLint("SetTextI18n")
    override fun fillValues(position: Int, convertView: View) {

        val p = favourites[position]

        val tvdesc = convertView.findViewById(R.id.favdec_s) as TextView
        val tvpw = convertView.findViewById(R.id.favpw_s) as TextView

        val tvHome = convertView.findViewById(R.id.favimg_s) as ImageView

        // Populate the data into the template view using the data object
        tvdesc.text = p.Name
        tvpw.text = p.Phone

        if (p.Image!="no image") {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val imageRef2 = storageRef.child(p.Image!!)
            Glide.with(mContext /* context */)
                    .using(FirebaseImageLoader())
                    .load(imageRef2)
                    .into(tvHome)

        }
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

}