package com.mobelite.tourismguide

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v4.app.ActivityCompat
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.ImageView
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
import com.mobelite.tourismguide.tools.PhoneGrantings
import noman.googleplaces.NRPlaces
import noman.googleplaces.Place
import noman.googleplaces.PlacesException
import noman.googleplaces.PlacesListener

@Suppress("DEPRECATION")
class MapDialogFragment : DialogFragment(), View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        DirectionCallback, PlacesListener {


    fun newInstance(title: String, lat: Double, lng: Double, type: Int, image: String): MapDialogFragment {
        val frag = MapDialogFragment()
        val args = Bundle()
        args.putString("title", title)
        args.putDouble("lat", lat)
        args.putDouble("lng", lng)
        args.putInt("type", type)
        args.putString("image", image)
        frag.arguments = args
        return frag
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "LocationPickerActivity"
        private const val serverKey = "AIzaSyChzlfg8hqme9giklQu01mvY7on6uDh180"
        private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION


    }

    private var origin = LatLng(35.771261, 10.834128)
    private var destination: LatLng? = null

    private var mLocationPermissionsGranted: Boolean? = false
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null

    private lateinit var mMapView: MapView
    private var googleMap: GoogleMap? = null
    private var type: Int = 0


    override fun onClick(v: View?) {
        val listener = activity as MapDialogFragmentListener?
        listener!!.onFinishEditDialog(destination!!)
        // Close the dialog and return back to the parent activity
        dismiss()
    }

    // 1. Defines the listener interface with a method passing back data result.
    interface MapDialogFragmentListener {
        fun onFinishEditDialog(pos: LatLng)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = activity!!.layoutInflater.inflate(R.layout.map_dialog, container)

        if (!PhoneGrantings.isNetworkAvailable(context!!))
            Toast.makeText(context, "Offline mode", Toast.LENGTH_SHORT).show()

        val img = root.findViewById<ImageView>(R.id.cancelmapimg)
        img.setOnClickListener(this)

        if (arguments!=null) { // check if marker position is pre-determined
            destination = LatLng(arguments!!.getDouble("lat"), arguments!!.getDouble("lng"))
            type = arguments!!.getInt("type")

            // Loading the surrounding places of the restaurant from google maps.
            NRPlaces.Builder()
                    .listener(this)
                    .key(serverKey)
                    .latlng(destination!!.latitude, destination!!.longitude)
                    .radius(1500)
                    .build()
                    .execute()
        }

        mMapView = root.findViewById(R.id.mapdialog)
        mMapView.onCreate(savedInstanceState)


        //loading map
        getLocationPermission()




        return root

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


    override fun onMapReady(mMap: GoogleMap) {

        val cameraPosition = CameraPosition.Builder()
                .target(destination) // set the camera's center position
                .zoom(15f)  // set the camera's zoom level
                .tilt(20f)  // set the camera's tilt
                .build()

        // Move the camera to that position
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        mMap.addMarker(MarkerOptions().position(destination!!)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(false))





        if (type==0) {
            mMap.setOnMapClickListener(this)
            mMap.setOnMarkerClickListener { false }
        }

        if (type==1) {
            mMap.setOnMapClickListener { }
            mMap.setOnMarkerClickListener(this)
        }

        mMap.uiSettings.isZoomControlsEnabled = true
        //  mMap.setOnMarkerDragListener(this)
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL //MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
        Log.d("mLocation", mLocationPermissionsGranted.toString())
        if (this.mLocationPermissionsGranted!!) {


            if (ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                return
            }

            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

        }


        googleMap = mMap


    }


    // add new marker to the clicked point by user
    override fun onMapClick(p0: LatLng?) {
        googleMap!!.clear()
        googleMap!!.addMarker(MarkerOptions().position(p0!!)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(false))
        destination = p0

    }


    private fun getLocationPermission() {
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

    override fun onMarkerClick(marker: Marker): Boolean {

        // building animation for marker when its clicked
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

        if (marker.position==destination) // only building a route to the restaurant position
            getDeviceLocation()


        return false
    }

    // loading direction route from device location to marker's
    private fun requestDirection() {
        //Snackbar.make(btnRequestDirection, "Direction Requesting...", Snackbar.LENGTH_SHORT).show();
        println("origin: $origin  destination: $destination")
        GoogleDirection.withServerKey(serverKey)
                .from(origin)
                .to(destination)
                .transportMode(TransportMode.DRIVING)
                .execute(this)
    }

    override fun onDirectionSuccess(direction: Direction, rawBody: String) {
        //Snackbar.make(btnRequestDirection, "Success with status : " + direction.getStatus(), Snackbar.LENGTH_SHORT).show();
        Toast.makeText(context, "Searching for directions", Toast.LENGTH_SHORT).show()

        println(direction.status)
        if (direction.isOK) { // building the route in map if its found
            val route = direction.routeList[0]
            /*googleMap.addMarker(new MarkerOptions().position(origin));
            googleMap.addMarker(new MarkerOptions().position(destination));*/

            val directionPositionList = route.legList[0].directionPoint
            googleMap!!.addPolyline(DirectionConverter.createPolyline(context, directionPositionList, 5, Color.RED))
            setCameraWithCoordinationBounds(route)

            //btnRequestDirection.setVisibility(View.GONE);
        } else {
            // Snackbar.make(btnRequestDirection, direction.getStatus(), Snackbar.LENGTH_SHORT).show();
            Toast.makeText(context, "No directions found! ", Toast.LENGTH_SHORT).show()

        }
    }

    // move map's camera to the direction route
    private fun setCameraWithCoordinationBounds(route: Route) {
        val southwest = route.bound.southwestCoordination.coordination
        val northeast = route.bound.northeastCoordination.coordination
        val bounds = LatLngBounds(southwest, northeast)
        googleMap!!.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    override fun onDirectionFailure(t: Throwable) {
        //Snackbar.make(btnRequestDirection, t.getMessage(), Snackbar.LENGTH_SHORT).show();
        Toast.makeText(context, "Failed to load directions", Toast.LENGTH_SHORT).show()

    }


    // loading the current diavace loaction
    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)

        try {
            if (mLocationPermissionsGranted!!) {

                val location = this.mFusedLocationProviderClient!!.lastLocation
                location.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (CheckGpsStatus()) {
                            currentLocation = task.result!!
                            if (currentLocation!=null) {
                                //origin= new LatLng( 36.170544, 10.170545);
                                origin = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)

                                // calling for direction when device location found
                                requestDirection()
                            } else {
                                Toast.makeText(context, "Current Position unavailable!", Toast.LENGTH_SHORT).show()

                            }
                        } else
                            Toast.makeText(context, "Your GPS is off", Toast.LENGTH_SHORT).show()


                    } else {
                        Log.d(TAG, "onComplete: current location is null")
                        Toast.makeText(context, "unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
            } else
                askForLocationPermissions()
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.message)
        }

    }

    fun CheckGpsStatus(): Boolean {

        val locationManager = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
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

    override fun onPlacesFailure(e: PlacesException?) {
        //Toast.makeText(context, "No surrounding places found", Toast.LENGTH_SHORT).show()
        Log.i("PlacesAPI", "onPlacesFailure()");

    }

    override fun onPlacesSuccess(places: MutableList<Place>?) {
        Log.i("PlacesAPI", "onPlacesSuccess()")

        activity!!.runOnUiThread {
            // adding the found places from google to the map
            places?.forEach { place ->

                val latLng = LatLng(place.latitude, place.longitude)
                googleMap!!.addMarker(MarkerOptions().position(latLng)
                        .title(place.name).snippet(place.vicinity));


            }
        }

    }

    override fun onPlacesFinished() {
        Log.i("PlacesAPI", "onPlacesFinished()");
    }

    override fun onPlacesStart() {
        Log.i("PlacesAPI", "onPlacesStart()");
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
