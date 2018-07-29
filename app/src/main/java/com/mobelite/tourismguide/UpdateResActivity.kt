package com.mobelite.tourismguide

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.mobelite.tourismguide.data.Model
import com.mobelite.tourismguide.data.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

import kotlinx.android.synthetic.main.activity_update_res.*
import kotlinx.android.synthetic.main.content_update_res.*

class UpdateResActivity : AppCompatActivity(),
        OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "LocationPickerActivity"
        private const val serverKey = "AIzaSyDbkY2fSN15Fgt8pTU6YzgcnUf-Hf5k04A"
        private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    }

    private var origin = LatLng(35.771261, 10.834128)

    private var mLocationPermissionsGranted: Boolean? = false

    var r:Model.ResultRestaurant? = null

    private lateinit var mMapView: MapView
    private var googleMap: GoogleMap? = null



    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    private fun updateRestaurant(m:Model.ResultRestaurant) {
        disposable =
                restaurantServices.updaterest(m)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println(result)
                                        if(result=="ok"){
                                            println("done")
                                            Toast.makeText(this, "Your Restaurant has been added", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                    }
                                },
                                { error ->println( error.message) }
                        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_res)
        setSupportActionBar(toolbar)

        val ss:String = intent.getStringExtra("myObject")
        r = Gson().fromJson(ss, Model.ResultRestaurant::class.java)
        println(r!!)

        mMapView = findViewById(R.id.Uploc)
        mMapView.onCreate(savedInstanceState)
        initMap()

        Uptlf.setText( r!!.phone)

        Upname.setText (r!!.name)

        UpDesc.setText (r!!.description)
        UpSave.setOnClickListener{
            val re = Model.ResultRestaurant(r!!.id,Upname.text.toString(), Uptlf.text.toString(),UpDesc.text.toString(),origin.latitude.toString(), origin.longitude.toString(),"no image","12154687856")

            println(re)
            updateRestaurant(re)
        }
        UpCancel.setOnClickListener {
            finish()
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
                Toast.makeText(this, "Can not proceed! i need permission", Toast.LENGTH_SHORT).show()
            }
        }


    }


    override fun onMapReady(mMap: GoogleMap) {
        val cameraPosition = CameraPosition.Builder()
                .target(origin) // set the camera's center position
                .zoom(9f)  // set the camera's zoom level
                .tilt(20f)  // set the camera's tilt
                .build()

        // Move the camera to that position
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))



        mMap.addMarker(MarkerOptions().position(origin)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(false))

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID //MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
        Log.d("mLocation", mLocationPermissionsGranted.toString())
        if (this.mLocationPermissionsGranted!!) {


            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                return
            }

            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

        }


        googleMap = mMap


    }

    private fun initMap() {
        Log.d(TAG, "initMap: initializing map")
        mMapView.getMapAsync(this)
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
