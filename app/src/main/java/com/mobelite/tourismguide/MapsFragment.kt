package com.mobelite.tourismguide

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.annotation.RequiresApi
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
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


class MapsFragment : Fragment(),
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {


    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    var res: Model.ResultRestaurant? = null
    var reso: Restaurant? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "LocationPickerActivity"
        private const val serverKey = "AIzaSyDbkY2fSN15Fgt8pTU6YzgcnUf-Hf5k04A"
        private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION

        fun newInstance(): MapsFragment {
            return MapsFragment()
        }
    }


    private var restaurants: ArrayList<Model.ResultRestaurant>? = ArrayList()
    private var favourites: ArrayList<Model.ResultRestaurant>? = ArrayList()

    private var mLocationPermissionsGranted: Boolean? = false


    private lateinit var mMapView: MapView
    private var googleMap: GoogleMap? = null


    private var listener: OnFragmentInteractionListener? = null


    //Room
    var compositeDisposable: CompositeDisposable? = null
    var restaurantRepository: RestaurantRepository? = null

    var searchBar: SearchView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_maps, container, false)

        root.isScrollContainer = false

        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar!!.title = "Maps"
        val flotadd = root.findViewById(R.id.fabadd) as FloatingActionButton
        mMapView = root.findViewById(R.id.mapfav)
        mMapView.onCreate(savedInstanceState)

        // adding new restaurant is only available online
        if (PhoneGrantings.isNetworkAvailable(context!!)) {
            flotadd.setOnClickListener {
                val intent = Intent(context, SaveResActivity().javaClass)
                activity!!.startActivity(intent)
            }
            // online restaurants
            beginSearch()
        } else {
            flotadd.setOnClickListener {
                Toast.makeText(context, "Internet is required for this feature", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(context, "Loading Offline Data", Toast.LENGTH_SHORT).show()
            // offline restaurants
            beginSearchOffline()

        }
        //starting the map
        getLocationPermission()


        // search bar action
        searchBar = root.findViewById(R.id.searchingbar)


        searchBar!!.isActivated = true
        searchBar!!.queryHint = "Type your keyword here"
        searchBar!!.onActionViewExpanded()
        searchBar!!.isIconified = false
        searchBar!!.clearFocus()
        searchBar!!.isFocusable = false

        searchBar!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener { // searching restaurant by name in real time
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                moveCameraTo(newText)

                return false
            }
        })

        val searchclear = searchBar!!.findViewById<ImageView>(R.id.search_close_btn)
        searchclear!!.setOnClickListener {
            // clearing the restaurant search
            searchBar!!.setQuery("", false)
            searchBar!!.clearFocus()

            googleMap!!.clear()
            addMarkers()
        }
        return root
    }


    //================================== loading restaurants from data base ==================================
    private fun beginSearch() {

        val restaurantDataBase = RestaurantDataBase.getInstance(activity!!)
        restaurantRepository = RestaurantRepository.getInstance(RestaurantDataSource.getInstance(restaurantDataBase.restaurantDAO()))
        deleteAllOfflineData() // cleaning old data from room

        disposable =
                restaurantServices.selectAll()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {
                                        println(result)
                                        restaurants = result as ArrayList<Model.ResultRestaurant>?
                                        selectFav()
                                    }
                                },
                                { error -> println(error.message) }
                        )
    }


    //================================== loading favourite restaurants from data base ==================================
    private fun selectFav() {

        disposable =
                restaurantServices.selectfav(PhoneGrantings.getSharedId(context!!))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    favourites = result as ArrayList<Model.ResultRestaurant>?
                                    addMarkers()
                                },
                                { error -> println(error.message) }
                        )
    }

    //================================== move map's camera to the wanted restaurant ==================================
    private fun moveCameraTo(s: String) {

        if (PhoneGrantings.isNetworkAvailable(context!!))
            restaurants!!.forEach { r ->

                if (r.name.contains(s)) {
                    val MOUNTAIN_VIEW = LatLng(r.lat.toDouble(), r.lng.toDouble())

                    val cameraPosition: CameraPosition = CameraPosition.Builder()
                            .target(MOUNTAIN_VIEW)      // Sets the center of the map to Mountain View
                            .zoom(17f)                   // Sets the zoom
                            .bearing(90f)                // Sets the orientation of the camera to east
                            .tilt(30f)                   // Sets the tilt of the camera to 30 degrees
                            .build()                    // Creates a CameraPosition from the builder
                    googleMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }
        else
            OfflineData.forEach { r ->

                if (r.Name!!.contains(s)) {
                    val MOUNTAIN_VIEW = LatLng(r.Lat!!.toDouble(), r.Lng!!.toDouble())

                    val cameraPosition: CameraPosition = CameraPosition.Builder()
                            .target(MOUNTAIN_VIEW)      // Sets the center of the map to Mountain View
                            .zoom(17f)                   // Sets the zoom
                            .bearing(90f)                // Sets the orientation of the camera to east
                            .tilt(30f)                   // Sets the tilt of the camera to 30 degrees
                            .build()                    // Creates a CameraPosition from the builder
                    googleMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }

            }
    }


    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }


    //============================ Room methods ===============================
    private var OfflineData: MutableList<Restaurant> = ArrayList()

    //================================== loading restaurants from room ==================================
    private fun beginSearchOffline() {
        compositeDisposable = CompositeDisposable()

        val restaurantDataBase = RestaurantDataBase.getInstance(activity!!)
        restaurantRepository = RestaurantRepository.getInstance(RestaurantDataSource.getInstance(restaurantDataBase.restaurantDAO()))
        loadOfflineData()
    }


    private fun loadOfflineData() {
        val disposable = restaurantRepository!!.allRestaurants
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe({ restaurants -> onGetAllRestaurantSuccess(restaurants) }) { throwable ->
                    Toast.makeText(context, "Try again please", Toast.LENGTH_SHORT).show()
                }

        compositeDisposable!!.add(disposable)

    }

    private fun onGetAllRestaurantSuccess(restaurants: List<Restaurant>?) {
        if (restaurants!=null) {
            OfflineData.clear()
            OfflineData.addAll(restaurants)
            addMarkers()
        }


    }

    //=============================== delete old restaurants from room ===============================
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

    //=============================== add new restaurants to room ===============================
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
                            println(throwable.message)
                            //Toast.makeText(context, "hmmmm " + throwable.message, Toast.LENGTH_SHORT).show()
                        },
                        {
                            //                            loadOfflineData()
                        })
        if (!PhoneGrantings.isNetworkAvailable(activity!!))
            compositeDisposable!!.addAll(disposable)

    }

    //================================ Map functions ================================
    internal inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private var popup: View? = null
        private val inflater: LayoutInflater? = null

        override fun getInfoContents(marker: Marker): View? {
            return null
        }

        @SuppressLint("InflateParams")
        override fun getInfoWindow(marker: Marker): View? {

            val index = marker.tag as Int
            try {

                // Getting view from the layout file info_window_layout
                popup = activity!!.layoutInflater.inflate(R.layout.custom_infowindow, null) // building a custom marker info window
                popup!!.isClickable = true
                // Getting reference to the TextView to set latitude
                val wifiTxt = popup!!.findViewById(R.id.titleWifi) as TextView
                wifiTxt.text = (marker.title)

                val passTxt = popup!!.findViewById(R.id.passworWifi) as TextView
                passTxt.text = (marker.snippet)
                val imgWifi = popup!!.findViewById(R.id.clientPic) as ImageView

                val imgHeart = popup!!.findViewById(R.id.imageHeart) as ImageView

                val loca: Model.ResultRestaurant = restaurants!![index] // the selected restaurant

                if (favourites!!.indexOf(loca) < 0) // check if the selected restaurant exists in favourites
                    imgHeart.visibility = View.GONE

                if (loca.image!="no image") { // loading the restaurant's image if it exists
                    val storage = FirebaseStorage.getInstance()
                    val storageRef = storage.reference
                    val imageRef2 = storageRef.child(loca.image)
                    Glide.with(context /* context */)
                            .using(FirebaseImageLoader())
                            .load(imageRef2)
                            .into(imgWifi)

                }

            } catch (ev: Exception) {
                print(ev.message)
            }

            return popup
        }
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
                Toast.makeText(context, "Can not proceed! i need permission", Toast.LENGTH_SHORT).show()
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onInfoWindowClick(marker: Marker) {

        // checking if all permissions are granted
        if (ContextCompat.checkSelfPermission(activity!!,
                        Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity!!,
                        Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            askForLocationPermissions()

        } else {
            val intent = Intent(activity!!, DisResActivity().javaClass)


            if (PhoneGrantings.isNetworkAvailable(context!!)) { // actions of the online mode
                restaurants!!.forEach { t ->
                    if (t.lat==marker.position.latitude.toString() && t.lng==marker.position.longitude.toString())
                        res = t
                }

                intent.putExtra("myObject", Gson().toJson(res))

                activity!!.startActivity(intent)
            } else { // actions of the offline mode
                OfflineData.forEach { t ->
                    if (t.Lat==marker.position.latitude.toString() && t.Lng==marker.position.longitude.toString())
                        reso = t
                }

                intent.putExtra("myObject", Gson().toJson(reso))

                activity!!.startActivity(intent)
            }
            //Toast.makeText(context, "You cliced me :)", Toast.LENGTH_LONG).show()

        }

    }

    private fun addMarkers() {

        val builder: LatLngBounds.Builder = LatLngBounds.builder() // building the map's camera bounds

        if (PhoneGrantings.isNetworkAvailable(context!!)) // treating online data
            restaurants!!.forEach { r ->
                builder.include(LatLng(r.lat.toDouble(), r.lng.toDouble()))

                googleMap!!.addMarker(MarkerOptions().position(LatLng(r.lat.toDouble(), r.lng.toDouble()))
                        .title(r.name)
                        .snippet(r.phone)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.restaurant_marker))).tag = restaurants!!.indexOf(r) // the tag so we can retreave the full data of marker selected

                //=============== adding to room ==================
                var isfav = 0
                if (favourites!!.indexOf(r) > 0)
                    isfav = 1
                // add new restaurants to room after previously cleaning old data
                addOfflineRestaurant(Restaurant(r.id, r.name, r.phone, r.description, r.lat, r.lng, r.image, r.userid, 0f, isfav))

            }
        else // treating offline data
            OfflineData.forEach { r ->
                builder.include(LatLng(r.Lat!!.toDouble(), r.Lng!!.toDouble()))
                googleMap!!.addMarker(MarkerOptions().position(LatLng(r.Lat!!.toDouble(), r.Lng!!.toDouble()))
                        .title(r.Name)
                        .snippet(r.Phone)).tag = OfflineData.indexOf(r)
            }


        if (!OfflineData.isEmpty() || !restaurants!!.isEmpty()) { // check if there are markers in map to build the camera bounds
            val bounds: LatLngBounds = builder.build()

            val padding = 50     // offset from edges of the map in pixels
            val cu: CameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            googleMap!!.animateCamera(cu)
        }
    }

    override fun onMapReady(mMap: GoogleMap) {
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(36.731742, 10.237638))// set the camera's center position
                .zoom(9f)  // set the camera's zoom level
                .tilt(20f)  // set the camera's tilt
                .build()

        // Move the camera to that position
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter())
        mMap.setOnInfoWindowClickListener(this)
        mMap.setOnMarkerClickListener(this)
        //mMap.setOnInfoWindowClickListener { println("hi") }
        Log.d("mLocation", mLocationPermissionsGranted.toString())
        if (this.mLocationPermissionsGranted!!) {


            if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                return
            }

            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL //MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
            //getDeviceLocation()
        }


        googleMap = mMap


    }


    override fun onMarkerClick(marker: Marker): Boolean {
        // building animation when marker is clicked
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val duration: Long = 1500

        val interpolator = BounceInterpolator()

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = Math.max(
                        1 - interpolator.getInterpolation(elapsed.toFloat() / duration), 0f)
                marker.setAnchor(0.5f, 1.0f + 2 * t)

                if (t > 0.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })

        return false
    }

    private fun getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions")
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (ContextCompat.checkSelfPermission(activity!!.applicationContext,
                        FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(activity!!.applicationContext,
                            COURSE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true
                initMap() // if all good init map
            } else {
                ActivityCompat.requestPermissions(activity!!,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(activity!!,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun initMap() {
        Log.d(TAG, "initMap: initializing map")
        mMapView.getMapAsync(this)
    }


    private fun askForLocationPermissions() {

        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

            android.support.v7.app.AlertDialog.Builder(activity!!)
                    .setTitle("Location permessions needed")
                    .setMessage("you need to allow this permission!")
                    .setPositiveButton("Sure") { _, _ ->
                        ActivityCompat.requestPermissions(activity!!,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                LOCATION_PERMISSION_REQUEST_CODE)
                    }
                    .setNegativeButton("Not now") { _, _ ->
                        //                                        //Do nothing
                    }
                    .show()

            // Show an expanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.

        } else {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)

            // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    override fun onPause() {

        disposable?.dispose()
        mMapView.onPause()
        super.onPause()
    }

    override fun onResume() {
        mMapView.onResume()
        super.onResume()
    }

    override fun onDestroy() {
//        if (!PhoneGrantings.isNetworkAvailable(activity!!))
//            compositeDisposable!!.clear()
        mMapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {

        mMapView.onLowMemory()
        super.onLowMemory()
    }
}
