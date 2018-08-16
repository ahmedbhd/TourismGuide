package com.mobelite.tourismguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.mobelite.tourismguide.data.roomservice.model.Restaurant
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.PhoneGrantings
import com.mobelite.tourismguide.tools.Validators
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.content_dis_res.*


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


    var offlineRestaurant: Restaurant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dis_res)
        setSupportActionBar(toolbar)



        if (supportActionBar!=null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false)

            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        val ss: String
        if (intent.hasExtra("myObject2")) { // actions when this activity is called from favourites list
            ss = intent.getStringExtra("myObject2")
            disfav.visibility = View.GONE
        } else // actions when this activity is called from marker's info window in the map
            ss = intent.getStringExtra("myObject")

        if (PhoneGrantings.isNetworkAvailable(applicationContext)) { // actions of the online mode
            r = Gson().fromJson(ss, Model.ResultRestaurant::class.java)
            println(r!!)
            distlf.text = r!!.phone

            disname.text = r!!.name

            disDesc.text = r!!.description
            if (r!!.image!="no image") { // loading image if it exists
                storage = FirebaseStorage.getInstance()
                storageRef = storage!!.reference
                val imageRef2 = storageRef!!.child(r!!.image)
                Glide.with(this /* context */)
                        .using(FirebaseImageLoader())
                        .load(imageRef2)
                        .into(dislocimage_d)

            }

            cmntnbr.setOnClickListener {
                // show comments in dialog fragment
                val fm = supportFragmentManager
                val alertDialog = CommentDiagFragment.newInstance(r!!.id.toString())
                alertDialog.show(fm, "comments list")
            }

            disfav.setOnClickListener {
                addFavourite()
            }

            //rating bar actions
            simpleRatingBar.setOnTouchListener { v, event ->
                when {
                    (event.action===MotionEvent.ACTION_UP)
                    -> {
                        val touchPositionX = event.x
                        val width = simpleRatingBar.width
                        var starsf = touchPositionX / width * 5.0f
                        if (starsf > 5)
                            starsf = 5f
                        val stars = starsf + 1
                        insertRating(starsf) // add the new rate to data base

                        v.isPressed = false
                    }
                    (event.action===MotionEvent.ACTION_DOWN)
                    -> v.isPressed = true
                    (event.action===MotionEvent.ACTION_CANCEL)
                    -> v.isPressed = false
                }
                true
            }

            // load comments count of this restaurant
            getCmntCount()

            // load rating of this restaurant
            getAllRating()

        } else { // actions of the offline mode
            offlineRestaurant = Gson().fromJson(ss, Restaurant::class.java)
            println(offlineRestaurant!!)
            distlf.text = offlineRestaurant!!.Phone

            disname.text = offlineRestaurant!!.Name

            disDesc.text = offlineRestaurant!!.Desc

            simpleRatingBar.rating = offlineRestaurant!!.Rating!!

            if (offlineRestaurant!!.Image!="no image") {
                storage = FirebaseStorage.getInstance()
                storageRef = storage!!.reference
                val imageRef2 = storageRef!!.child(offlineRestaurant!!.Image!!)
                Glide.with(this /* context */)
                        .using(FirebaseImageLoader())
                        .load(imageRef2)
                        .into(dislocimage_d)

            }


            cmntnbr.setOnClickListener {
                Toast.makeText(this, "Internet is required for this feature", Toast.LENGTH_SHORT).show()
            }

            disfav.setOnClickListener {

                Toast.makeText(this, "Internet is required for this feature", Toast.LENGTH_SHORT).show()
            }

            simpleRatingBar.setOnTouchListener { v, event ->
                Toast.makeText(this, "Internet is required for this feature", Toast.LENGTH_SHORT).show()

                true
            }
        }



        imgadisdiag.setOnClickListener {

            showMapAlertDialog()
        }





        disDesc.setOnClickListener {
            showTextAlertDialog()
        }




        isFav()

        cmntnbr.movementMethod = LinkMovementMethod.getInstance()


        //simpleRatingBar.isClickable = true


    }


    //===================================== add to favourite =====================================
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

    //===================================== check if this restaurant if favourite =====================================
    private fun isFav() {

        if (PhoneGrantings.isNetworkAvailable(applicationContext))
            disposable =
                    restaurantServices.ifFavourite(PhoneGrantings.getSharedId(applicationContext), r!!.id.toString())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    { result ->
                                        run {

                                            println("is faaaaaaaaaaa55555555aaaaaaaaaaaav $result")
                                            if (result.toInt() > 0) disfav.setImageResource(R.drawable.heart)
                                        }

                                    },
                                    { error -> println(error.message) }
                            )
        else {
            if (offlineRestaurant!!.Fav!! > 0) disfav.setImageResource(R.drawable.heart)
        }

    }

    //===================================== load comments number on this restaurant =====================================
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


    //=====================================  loading rating of this restaurant =====================================
    private fun getAllRating() {

        disposable =
                restaurantServices.allRatings(r!!.id.toString())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {
                                        simpleRatingBar.rating = Validators.calculateRating(result)

                                    }
                                },
                                { error -> println(error.message) }
                        )

    }

    //===================================== add rating of this restaurant =====================================
    private fun insertRating(rate: Float) {

        disposable =
                restaurantServices.insertRate(Model.Rating(0, rate, PhoneGrantings.getSharedId(applicationContext), r!!.id))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println("result $result")
                                        if (result=="ok") {
                                            getAllRating()
                                            Toast.makeText(this, "Rate updated", Toast.LENGTH_SHORT).show()

                                        }
                                    }
                                },
                                { error -> println(error.message) }
                        )

    }

    //===================================== show map in dialog fragment =====================================
    private fun showMapAlertDialog() {
        val fm = supportFragmentManager
        if (PhoneGrantings.isNetworkAvailable(applicationContext)) { // show map with online data
            val alertDialog = MapDialogFragment().newInstance("Maps", r!!.lat.toDouble(), r!!.lng.toDouble(), 1, r!!.image)
            alertDialog.show(fm, "map_alert")
        } else { // show map with offline data
            val alertDialog = MapDialogFragment().newInstance("Maps", offlineRestaurant!!.Lat!!.toDouble(), offlineRestaurant!!.Lng!!.toDouble(), 1, offlineRestaurant!!.Image!!)
            alertDialog.show(fm, "map_alert")
        }
    }

    //===================================== show description text in dialog fragment =====================================
    private fun showTextAlertDialog() {
        val fm = supportFragmentManager
        if (PhoneGrantings.isNetworkAvailable(applicationContext)) { // show description text with online data

            val alertDialog = TextViewDiagFrag().newInstance(r!!.description)
            alertDialog.show(fm, "text_alert")
        } else { // show description text with offline data
            val alertDialog = TextViewDiagFrag().newInstance(offlineRestaurant!!.Desc!!)
            alertDialog.show(fm, "text_alert")
        }
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
