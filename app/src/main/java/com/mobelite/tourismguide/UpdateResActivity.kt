package com.mobelite.tourismguide

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_update_res.*
import kotlinx.android.synthetic.main.content_update_res.*
import java.io.IOException
import java.util.*

class UpdateResActivity : AppCompatActivity(),
        OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "LocationPickerActivity"
        private const val serverKey = "AIzaSyDbkY2fSN15Fgt8pTU6YzgcnUf-Hf5k04A"
        private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
    }

    private val REQUEST_RUNTIME_PERMISSION = 123

    private var origin = LatLng(35.771261, 10.834128)

    private var mLocationPermissionsGranted: Boolean? = false

    var r: Model.ResultRestaurant? = null

    private lateinit var mMapView: MapView
    private var googleMap: GoogleMap? = null

    private var storage: FirebaseStorage? = null
    private var storageRef: StorageReference? = null

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    private fun updateRestaurant() {
        disposable =
                restaurantServices.updaterest(re!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        println(result)
                                        if (result=="ok") {

                                            Toast.makeText(this, "Your Restaurant has been UPDATED", Toast.LENGTH_SHORT).show()

                                        }
                                    }
                                },
                                { error -> println(error.message) }
                        )
        val intent = Intent(this, MainActivity().javaClass)
        startActivity(intent)
    }

    var re: Model.ResultRestaurant? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_res)
        setSupportActionBar(toolbar)



        if (supportActionBar!=null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false)

            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }


        val ss: String = intent.getStringExtra("myObject")
        r = Gson().fromJson(ss, Model.ResultRestaurant::class.java)
        println(r!!)

        mMapView = findViewById(R.id.Uploc)
        mMapView.onCreate(savedInstanceState)
        initMap()

        Uptlf.setText(r!!.phone)

        Upname.setText(r!!.name)

        UpDesc.setText(r!!.description)

        if (r!!.image!="no image") {
            storage = FirebaseStorage.getInstance()
            storageRef = storage!!.reference
            val imageRef2 = storageRef!!.child(r!!.image)
            Glide.with(this /* context */)
                    .using(FirebaseImageLoader())
                    .load(imageRef2)
                    .into(Uplocimage_d)
        }

        Uplocimage_d.setOnClickListener {
            if (CheckPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // you have permission go ahead
                showFileChooser()
            } else {
                // you do not have permission go request runtime permissions
                RequestPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_RUNTIME_PERMISSION);
            }

        }

        UpSave.setOnClickListener {
            val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
            val iduser = prefs.getString("fb_id", null)
            re = Model.ResultRestaurant(r!!.id, Upname.text.toString(), Uptlf.text.toString(), UpDesc.text.toString(), origin.latitude.toString(), origin.longitude.toString(), r!!.image, iduser)
            if (filePath!=null) {
                uploadFile()
            } else
                updateRestaurant()

        }

    }

    private var filePath: Uri? = null

    private fun showFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "SELECT PICTURE"), PICK_IAMGE_REQUEST)
    }

    private val PICK_IAMGE_REQUEST = 1234

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==PICK_IAMGE_REQUEST &&
                resultCode==Activity.RESULT_OK &&
                data!=null && data.data!=null) {
            filePath = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                Uplocimage_d!!.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadFile() {
        storage = FirebaseStorage.getInstance()
        storageRef = storage!!.reference
        if (re!!.image!="no image") {

            val imageRef2 = storageRef!!.child(re!!.image)
            imageRef2.delete()
                    .addOnSuccessListener {

                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                    }
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Loading ....")
        progressDialog.show()
        val imagepath = "images/" + UUID.randomUUID().toString()
        val imageRef = storageRef!!.child(imagepath)
        imageRef.putFile(filePath!!)
                .addOnSuccessListener {
                    progressDialog.dismiss()
//
                    re!!.image = imagepath
                    updateRestaurant()
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                    progressDialog.setMessage("Upload " + progress.toInt() + "%...")
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
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL //MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
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

    fun CheckPermission(context: Context, Permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context,
                Permission)==PackageManager.PERMISSION_GRANTED
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId==android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
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
