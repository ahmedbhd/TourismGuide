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
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.Toast
import com.bumptech.glide.Glide
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.android.gms.flags.impl.SharedPreferencesFactory.getSharedPreferences
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.mobelite.tourismguide.R.id.*
import com.mobelite.tourismguide.R.string.name
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import com.mobelite.tourismguide.tools.Validators
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_update_res.*
import kotlinx.android.synthetic.main.content_update_res.*
import java.io.IOException
import java.util.*

class UpdateResActivity : AppCompatActivity(),
        MapDialogFragment.MapDialogFragmentListener {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

    }

    private val REQUEST_RUNTIME_PERMISSION = 123

    private var origin : LatLng ?=null


    var r: Model.ResultRestaurant? = null

    private var storage: FirebaseStorage? = null
    private var storageRef: StorageReference? = null

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null

    private fun updateRestaurant() {

                val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
                val iduser = prefs.getString("fb_id", null)
                r = Model.ResultRestaurant(r!!.id, Upname.text.toString(), Uptlf.text.toString(), UpDesc.text.toString(), origin!!.latitude.toString(), origin!!.longitude.toString(), r!!.image, iduser)

                disposable =
                        restaurantServices.updaterest(r!!)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        { result ->
                                            run {

                                                println(result)
                                                if (result=="ok") {

                                                    Toast.makeText(this, "Updated succeeded", Toast.LENGTH_SHORT).show()

                                                }
                                            }
                                        },
                                        { error -> println(error.message) }
                                )
                val intent = Intent(this, MainActivity().javaClass)
                startActivity(intent)

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_res)
        setSupportActionBar(toolbar)



        if (supportActionBar!=null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false)

            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }




        imgupdiag.setOnClickListener {

            showAlertDialog()
        }
        upactivitytitle.text = "New Restaurant"

        if (intent.hasExtra("myObject")) {
            val ss: String = intent.getStringExtra("myObject")
            r = Gson().fromJson(ss, Model.ResultRestaurant::class.java)
            origin = LatLng(r!!.lat.toDouble(), r!!.lng.toDouble())
            upactivitytitle.text = "Update"
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
            when {
                (Uptlf.text.toString()=="" || Uptlf!!.text.toString()=="" || UpDesc!!.text.toString()=="")
                -> Toast.makeText(this, "All the fields are required", Toast.LENGTH_SHORT).show()
                (!Validators.isPhone(Uptlf.text.toString()))
                -> Toast.makeText(this, "Bad phone number", Toast.LENGTH_SHORT).show()
                else -> {
                    if (intent.hasExtra("myObject")) {
                        if (filePath!=null) {
                            uploadFile()
                        } else
                            updateRestaurant()
                    } else {
                        when {
                            origin==null -> Toast.makeText(this, "The location of the restaurant is Required", Toast.LENGTH_SHORT).show()
                            filePath==null -> addRestaurant("no image")
                            else -> uploadFile()
                        }
                    }
                }
            }
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
        if(intent.hasExtra("myObject")){
            if (r!!.image!="no image"){
                        val imageRef2 = storageRef!!.child(r!!.image)
                        imageRef2.delete()
                                .addOnSuccessListener {

                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                                }
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

                    if (!intent.hasExtra("myObject"))
                        addRestaurant(imagepath)
                    else {
                        r!!.image = imagepath
                        updateRestaurant()
                    }
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

    private fun showAlertDialog() {
        val fm = supportFragmentManager
        if (intent.hasExtra("myObject")) {
            val alertDialog = MapDialogFragment().newInstance("Maps", r!!.lat.toDouble(), r!!.lng.toDouble(), 3)
            alertDialog.show(fm, "fragment_alert")
        }
        else {
            val alertDialog = MapDialogFragment().newInstance("Maps", 35.771261, 10.834128, 0)
            alertDialog.show(fm, "fragment_alert")
        }
    }

    override fun onFinishEditDialog(pos: LatLng) {
//        Toast.makeText(this, "Hi, $pos", Toast.LENGTH_SHORT).show();
        origin = pos
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




    private fun addRestaurant(image: String) {

                val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
                val iduser = prefs.getString("fb_id", null)
                disposable =
                        restaurantServices.insert(Model.ResultRestaurant(0, Upname.text.toString(), Uptlf.text.toString(), UpDesc.text.toString(),
                                origin!!.latitude.toString(), origin!!.longitude.toString(), image, iduser))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        { result ->
                                            run {

                                                if (result=="ok") {

                                                    Toast.makeText(this, "Add succeeded", Toast.LENGTH_SHORT).show()

                                                }
                                            }
                                        },
                                        { error -> println(error.message) }
                                )
                val intent = Intent(this, MainActivity().javaClass)
                startActivity(intent)

    }



    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item!!.itemId==android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }


}
