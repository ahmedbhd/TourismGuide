package com.mobelite.tourismguide

import android.Manifest
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.gms.flags.impl.SharedPreferencesFactory.getSharedPreferences
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.mobelite.tourismguide.R.id.cmntnbr
import com.mobelite.tourismguide.R.id.simpleRatingBar
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.PhoneGrantings
import com.mobelite.tourismguide.tools.Validators
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.content_dis_res.*
import android.opengl.ETC1.getWidth




@Suppress("DEPRECATED_IDENTITY_EQUALS")
class DisResActivity : AppCompatActivity(),
        MapDialogFragment.MapDialogFragmentListener {


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

    }

    var r: Model.ResultRestaurant? = null

    private var storage: FirebaseStorage? = null
    private var storageRef: StorageReference? = null


    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dis_res)
        setSupportActionBar(toolbar)



        if (supportActionBar!=null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false)

            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }


        val ss: String = intent.getStringExtra("myObject")
        r = Gson().fromJson(ss, Model.ResultRestaurant::class.java)
        println(r!!)

        imgadisdiag.setOnClickListener {

            showMapAlertDialog()
        }



        distlf.text = r!!.phone

        disname.text = r!!.name

        disDesc.text = r!!.description

        disDesc.setOnClickListener {
            showTextAlertDialog()
        }

        if (r!!.image!="no image") {
            storage = FirebaseStorage.getInstance()
            storageRef = storage!!.reference
            val imageRef2 = storageRef!!.child(r!!.image)
            Glide.with(this /* context */)
                    .using(FirebaseImageLoader())
                    .load(imageRef2)
                    .into(dislocimage_d)

        }

        disfav.setOnClickListener {
            addFavourite()
        }
        isFav()

        cmntnbr.movementMethod = LinkMovementMethod.getInstance()
        getCmntCount()

        cmntnbr.setOnClickListener {
            val fm = supportFragmentManager
            val alertDialog = CommentFragment.newInstance(r!!.id.toString())
            alertDialog.show(fm, "comments list")
        }


        getAllRating()
        //simpleRatingBar.isClickable = true
        simpleRatingBar.setOnTouchListener { v, event ->
            if (event.action===MotionEvent.ACTION_UP) {
                val touchPositionX = event.x
                val width = simpleRatingBar.width
                var starsf = touchPositionX / width * 5.0f
                if (starsf>5)
                    starsf = 5f
                val stars = starsf + 1
                insertRating(starsf)

                v.isPressed=false
            }
            if (event.action===MotionEvent.ACTION_DOWN) v.isPressed = true

            if (event.action===MotionEvent.ACTION_CANCEL)
                v.isPressed = false

            true
        }
//        simpleRatingBar.setOnRatingChangeListener { ratingBar, rating->
//                insertRating(rating)
//
//        }


    }


    private fun addFavourite() {

        disposable =
                restaurantServices.insertfav(Model.FavRestaurant(0, r!!.id, PhoneGrantings.getSharedId(applicationContext)))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println(result)
                                        if (result=="ok") {
                                            println("done")
                                            Toast.makeText(this, "Favourite succeeded", Toast.LENGTH_SHORT).show()

                                        }
                                    }
                                },
                                { error -> println(error.message) }
                        )
        val intent = Intent(this, MainActivity().javaClass)
        startActivity(intent)
    }


    private fun isFav() {

        disposable =
                restaurantServices.ifFavourite( PhoneGrantings.getSharedId(applicationContext),r!!.id.toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println("is faaaaaaaaaaa55555555aaaaaaaaaaaav $result")
                                        if (result.toInt()>0) disfav.setImageResource(R.drawable.heart)
                                    }

                                },
                                { error -> println(error.message) }
                        )

    }

    private fun getCmntCount() {

        disposable =
                restaurantServices.cmntCount(r!!.id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println("result $result")
                                        if (result.toInt() >= 0) {
                                            cmntnbr.text = "$result comments"
                                        }

                                    }
                                },
                                { error -> println(error.message) }
                        )

    }



    private fun getAllRating() {

        disposable =
                restaurantServices.allRatings(r!!.id.toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {
                                        simpleRatingBar.rating= Validators.calculateRating (result)

                                    }
                                },
                                { error -> println(error.message) }
                        )

    }


    private fun insertRating(rate :Float) {

        disposable =
                restaurantServices.insertRate(Model.Rating(0,rate,PhoneGrantings.getSharedId(applicationContext),r!!.id))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println("result $result")
                                        if (result =="ok") {
                                            getAllRating()
                                            Toast.makeText(this, "Rate updated", Toast.LENGTH_SHORT).show()

                                        }
                                    }
                                },
                                { error -> println(error.message) }
                        )

    }

    private fun showMapAlertDialog() {
        val fm = supportFragmentManager
        val alertDialog = MapDialogFragment().newInstance("Maps", r!!.lat.toDouble(), r!!.lng.toDouble(), 1)
        alertDialog.show(fm, "map_alert")
    }

    private fun showTextAlertDialog() {
        val fm = supportFragmentManager
        val alertDialog = TextViewDiagFrag().newInstance(r!!.description)
        alertDialog.show(fm, "text_alert")
    }

    override fun onFinishEditDialog(pos: LatLng) {
//        Toast.makeText(this, "Hi, $pos", Toast.LENGTH_SHORT).show();
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        fun isPermissionGranted(grantPermissions: Array<String>, grantResults: IntArray,
                                permission: String): Boolean {
            for (i in grantPermissions.indices) {
                if (permission==grantPermissions[i]) {
                    return grantResults[i]==PackageManager.PERMISSION_GRANTED
                }
            }
            return false
        }
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //Do you work
            } else {
                Toast.makeText(this, "Can not proceed! i need permission", Toast.LENGTH_SHORT).show()
            }
        }


    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId==android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }

}
