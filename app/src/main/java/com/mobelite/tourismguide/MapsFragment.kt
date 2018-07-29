package com.mobelite.tourismguide

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.*
import android.support.annotation.RequiresApi
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.akexorcist.googledirection.DirectionCallback
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.akexorcist.googledirection.model.Route
import com.akexorcist.googledirection.util.DirectionConverter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.mobelite.tourismguide.data.Model
import com.mobelite.tourismguide.data.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.stream.Stream


class MapsFragment : Fragment() ,
    OnMapReadyCallback ,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnInfoWindowClickListener,
    DirectionCallback {


    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null


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




    private var restaurants : ArrayList<Model.ResultRestaurant>? = null
    private var mLocationPermissionsGranted: Boolean? = false
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null

    private var origin = LatLng(37.7849569, -122.4068855)
    private var destination = LatLng(37.7814432, -122.4460177)
    private lateinit var mMapView: MapView
    private var googleMap: GoogleMap? = null


    private var listener: OnFragmentInteractionListener? = null



    internal inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private var popup: View? = null
        private val inflater: LayoutInflater? = null

        override fun getInfoContents(marker: Marker): View? {
            return null
        }

        @SuppressLint("InflateParams")
        override fun getInfoWindow(marker: Marker): View? {


            try {

                // Getting view from the layout file info_window_layout
                popup = layoutInflater.inflate(R.layout.custom_infowindow, null)
                popup!!.isClickable = true
                // Getting reference to the TextView to set latitude
                val wifiTxt = popup!!.findViewById(R.id.titleWifi) as TextView
                wifiTxt.text=(marker. title)

                val passTxt = popup!!.findViewById(R.id.passworWifi) as TextView
                passTxt.text=(marker.snippet)
                val imgWifi = popup!!.findViewById(R.id.clientPic) as ImageView
//                Picasso.with(context)
//
//                        .load(p.getImg())
//                        .into(imgWifi)

                /* heart.setOnClickListener(v -> {
                    DelFavourite(idloc);

                });*/

            } catch (ev: Exception) {
                print(ev.message)
            }

            return popup
        }
    }




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_maps, container, false)

        root.isScrollContainer = false

        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar!!.title = "Wifi Location"

        mMapView = root.findViewById(R.id.mapfav)
        mMapView.onCreate(savedInstanceState)

        beginSearch()

        //mMapView.getMapAsync(this);
        getLocationPermission()

        val flotadd = root.findViewById(R.id.fabadd) as FloatingActionButton
        flotadd.setOnClickListener {

            val intent = Intent(context, AddResActivity().javaClass)

            requireActivity().startActivity(intent)

        }
        return root
    }

    // TODO: Rename method, update argument and hook method into UI event

    private fun beginSearch() {
        disposable =
                restaurantServices.selectAll()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {
                                        println(result)
                                        restaurants = result as ArrayList<Model.ResultRestaurant>?
                                        addMarkers()
                                    }
                                },
                                { error ->println( error.message) }
                        )
    }


    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
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

        if (ContextCompat.checkSelfPermission(activity!!,
                        Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity!!,
                        Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            askForLocationPermissions()
            println ("hey")
        } else {
            val intent = Intent(activity!!, DisResActivity().javaClass)

            var res: Model.ResultRestaurant?=null

            restaurants!!.forEach {t ->
                if (t.lat==marker.position.latitude.toString() && t.lng==marker.position.longitude.toString())
                   res = t
            }

            println("res ${res.toString()}")
            intent.putExtra("myObject", Gson().toJson(res))

            activity!!.startActivity(intent)
            //Toast.makeText(context, "You cliced me :)", Toast.LENGTH_LONG).show()

        }

    }

    private  fun addMarkers(){

        restaurants!!.forEach{ r ->
            googleMap!!.addMarker(MarkerOptions().position(LatLng(r.lat.toDouble(),r.lng.toDouble() ))
                .title(r.name)
                .snippet(r.description))
        }
    }
    override fun onMapReady(mMap: GoogleMap) {
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(36.731742 , 10.237638))// set the camera's center position
                .zoom(9f)  // set the camera's zoom level
                .tilt(20f)  // set the camera's tilt
                .build()

        // Move the camera to that position
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))



//        mMap.addMarker(MarkerOptions().position(origin)
//                .title("Sydney")
//                .snippet("this is test").draggable(true))

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setInfoWindowAdapter(CustomInfoWindowAdapter())
        mMap.setOnInfoWindowClickListener(this)
        mMap.setOnMarkerClickListener (this)
        //mMap.setOnInfoWindowClickListener { println("hi") }
        Log.d("mLocation", mLocationPermissionsGranted.toString())
        if (this.mLocationPermissionsGranted!!) {


            if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                return
            }

            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID //MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
            //getDeviceLocation()
        }


        googleMap = mMap


    }

    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)

        try {
            if (mLocationPermissionsGranted!!) {

                val location = this.mFusedLocationProviderClient!!.lastLocation
                location.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                       // Log.d(TAG, "onComplete: found location!")
                        currentLocation = task.result!!

                        /* moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                        DEFAULT_ZOOM);*/

                    } else {
                        Log.d(TAG, "onComplete: current location is null")
                        Toast.makeText(context, "unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.message)
        }

    }

    override fun onMarkerClick(marker: Marker): Boolean {
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

//        getDeviceLocation()
//        if (currentLocation!=null) {
//            //origin= new LatLng( 36.170544, 10.170545);
//            origin = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
//            destination = marker.position
//            requestDirection()
//        } else {
//            Toast.makeText(context, "Current Position unavailable!", Toast.LENGTH_SHORT).show()
//
//        }
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
                initMap()
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


    private fun requestDirection() {
        //Snackbar.make(btnRequestDirection, "Direction Requesting...", Snackbar.LENGTH_SHORT).show();
        GoogleDirection.withServerKey(serverKey)
                .from(origin)
                .to(destination)
                .transportMode(TransportMode.DRIVING)
                .execute(this)
    }

    override fun onDirectionSuccess(direction: Direction, rawBody: String) {
        //Snackbar.make(btnRequestDirection, "Success with status : " + direction.getStatus(), Snackbar.LENGTH_SHORT).show();
        Toast.makeText(context, "Searching for directions", Toast.LENGTH_SHORT).show()

        if (direction.isOK) {
            val route = direction.routeList[0]
            /*googleMap.addMarker(new MarkerOptions().position(origin));
            googleMap.addMarker(new MarkerOptions().position(destination));*/

            val directionPositionList = route.legList[0].directionPoint
            googleMap!!.addPolyline(DirectionConverter.createPolyline(activity!!, directionPositionList, 5, Color.RED))
            setCameraWithCoordinationBounds(route)

            //btnRequestDirection.setVisibility(View.GONE);
        } else {
            // Snackbar.make(btnRequestDirection, direction.getStatus(), Snackbar.LENGTH_SHORT).show();
            Toast.makeText(context, "No directions found!", Toast.LENGTH_SHORT).show()

        }
    }

    override fun onDirectionFailure(t: Throwable) {
        //Snackbar.make(btnRequestDirection, t.getMessage(), Snackbar.LENGTH_SHORT).show();
    }

    private fun setCameraWithCoordinationBounds(route: Route) {
        val southwest = route.bound.southwestCoordination.coordination
        val northeast = route.bound.northeastCoordination.coordination
        val bounds = LatLngBounds(southwest, northeast)
        googleMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
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
                    .setNegativeButton("Not now") { dialog, which ->
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

        mMapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {

        mMapView.onLowMemory()
        super.onLowMemory()
    }
}
