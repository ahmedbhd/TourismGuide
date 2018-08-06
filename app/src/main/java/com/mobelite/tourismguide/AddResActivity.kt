package com.mobelite.tourismguide

import android.Manifest
import android.app.Activity
import android.app.PendingIntent.getActivity
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.NonNull
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.akexorcist.googledirection.GoogleDirection
import com.akexorcist.googledirection.constant.TransportMode
import com.akexorcist.googledirection.model.Direction
import com.akexorcist.googledirection.model.Route
import com.akexorcist.googledirection.util.DirectionConverter
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap

import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.mobelite.tourismguide.R.id.addname
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_add_res.*
import kotlinx.android.synthetic.main.content_add_res.*
import java.io.IOException
import java.util.*


class AddResActivity : AppCompatActivity() ,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener {



    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null


    var name : EditText? = null
    var tlf : EditText? = null
    var des : EditText? = null
    var imgView : ImageView? = null
    private val REQUEST_RUNTIME_PERMISSION = 123


//    override fun onMarkerDragEnd(p0: Marker?) {
//
//        println(p0!!.position)
//    }

//    override fun onMarkerDragStart(p0: Marker?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onMarkerDrag(p0: Marker?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "LocationPickerActivity"
        private const val serverKey = "AIzaSyDbkY2fSN15Fgt8pTU6YzgcnUf-Hf5k04A"
        private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION


    }
    private var origin = LatLng(35.771261, 10.834128)

    private var mLocationPermissionsGranted: Boolean? = false
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocation: Location? = null

    private lateinit var mMapView: MapView
    private var googleMap: GoogleMap? = null
    private var filePath: Uri?=null
    internal var storage: FirebaseStorage?=null
    internal var storageRef: StorageReference?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_res)
        setSupportActionBar(findViewById(R.id.toolbar))
        FirebaseApp.initializeApp(this)

        if (supportActionBar!=null){
            supportActionBar!!.setDisplayShowTitleEnabled(false)

            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }


        storage = FirebaseStorage.getInstance()
        storageRef = storage!!.reference

        name = findViewById(R.id.addname)
        des = findViewById(R.id.addDesc)
        tlf = findViewById(R.id.addtlf)
        imgView = findViewById(R.id.addLocImage)
        imgView!!.setOnClickListener {
            if (CheckPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // you have permission go ahead
                showFileChooser()
            } else {
                // you do not have permission go request runtime permissions
                RequestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_RUNTIME_PERMISSION);
            }

        }


        mMapView = findViewById(R.id.addloc)
        mMapView.onCreate(savedInstanceState)
        getLocationPermission()




        addSave.setOnClickListener {
            if (filePath==null)
                addRestaurant("no image")
            else
                uploadFile()

        }

    }
    private fun addRestaurant(image:String) {
        val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
        val iduser = prefs.getString("fb_id", null)
        disposable =
                restaurantServices.insert(Model.ResultRestaurant(0,name!!.text.toString(), tlf!!.text.toString() , des!!.text.toString(),
                                                origin.latitude.toString(),origin.longitude.toString(),image,iduser))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        if(result=="ok"){

                                            Toast.makeText(this, "The Restaurant has been ADDED", Toast.LENGTH_SHORT).show()

                                        }
                                    }
                                },
                                { error ->println( error.message) }
                        )
        val intent = Intent(this, MainActivity().javaClass)
        startActivity(intent)
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


        mMap.setOnMapClickListener(this)
        mMap.uiSettings.isZoomControlsEnabled = true
      //  mMap.setOnMarkerDragListener(this)
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL //MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
        Log.d("mLocation", mLocationPermissionsGranted.toString())
        if (this.mLocationPermissionsGranted!!) {
            getDeviceLocation()

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
                return
            }

            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

        }


        googleMap = mMap


    }


    override fun onMapClick(p0: LatLng?) {
        googleMap!!.clear()
        googleMap!!.addMarker(MarkerOptions().position(p0!!)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .draggable(false))
        origin = p0

    }

    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            if (mLocationPermissionsGranted!!) {

                val location = this.mFusedLocationProviderClient!!.lastLocation
                location.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "onComplete: found location!")
                        currentLocation = task.result!!

                        /* moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                        DEFAULT_ZOOM);*/

                    } else {
                        Log.d(TAG, "onComplete: current location is null")
                        Toast.makeText(this, "unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.message)
        }

    }







    private fun getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions")
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (ContextCompat.checkSelfPermission(this.applicationContext,
                        FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.applicationContext,
                            COURSE_LOCATION)==PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true
                initMap()
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun initMap() {
        Log.d(TAG, "initMap: initializing map")
        mMapView.getMapAsync(this)
    }



    private val PICK_IAMGE_REQUEST = 1234

    private fun showFileChooser(){
        val intent = Intent()
        intent.type="image/*"
        intent.action=Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"SELECT PICTURE"),PICK_IAMGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IAMGE_REQUEST &&
                resultCode == Activity.RESULT_OK &&
                data != null  && data.data != null)
        {
            filePath = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver , filePath)
                imgView!!.setImageBitmap(bitmap)
            }catch (e:IOException){
                e.printStackTrace()
            }
        }
    }
    private fun uploadFile(){
        if (filePath != null){
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Loading ....")
            progressDialog.show()
            val imagepath = "images/"+UUID.randomUUID().toString()
            val imageRef = storageRef!!.child(imagepath)
            imageRef.putFile(filePath!!)
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        println("storageRef :$storageRef \nfilePath :$filePath \nimageRef :$imageRef")
//                        Toast.makeText(this, "File uploaded", Toast.LENGTH_SHORT).show()
                        addRestaurant(imagepath)
                    }
                    .addOnFailureListener{
                        progressDialog.dismiss()
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener {taskSnapshot ->
                        val progress = 100.0*taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount
                        progressDialog.setMessage("Upload "+progress.toInt()+"%...")
                    }


        }
    }


//    override fun onRequestPermissionsResult(permsRequestCode: Int, permissions: Array<String>, @NonNull grantResults: IntArray) {
//
//        when (permsRequestCode) {
//
//            REQUEST_RUNTIME_PERMISSION -> {
//                if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
//                    // you have permission go ahead
//                   // createApplicationFolder()
//                    Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show()
//
//                } else {
//                    // you do not have permission show toast.
//                }
//                return
//            }
//        }
//    }

    fun RequestPermission(thisActivity: Activity, Permission: String, Code: Int) {
        if (ContextCompat.checkSelfPermission(thisActivity,
                        Permission)!=PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                            Permission)) {
            } else {
                ActivityCompat.requestPermissions(thisActivity,
                        arrayOf(Permission),
                        Code)
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId==android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }
    fun CheckPermission(context: Context, Permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context,
                Permission)==PackageManager.PERMISSION_GRANTED
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
