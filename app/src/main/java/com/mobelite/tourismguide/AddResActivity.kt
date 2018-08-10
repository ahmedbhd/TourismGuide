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
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_add_res.*
import java.io.IOException
import java.util.*


class AddResActivity : AppCompatActivity(), MapDialogFragment.MapDialogFragmentListener {


    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null


    var name: EditText? = null
    private var tlf: EditText? = null
    private var des: EditText? = null
    private var imgView: ImageView? = null
    private val REQUEST_RUNTIME_PERMISSION = 123

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private var origin: LatLng? = null

    private var filePath: Uri? = null
    internal var storage: FirebaseStorage? = null
    private var storageRef: StorageReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_res)
        setSupportActionBar(findViewById(R.id.toolbar))
        FirebaseApp.initializeApp(this)

        if (supportActionBar!=null) {
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







        addSave.setOnClickListener {
            when {
                origin==null -> Toast.makeText(this, "The location of the restaurant is Required", Toast.LENGTH_SHORT).show()
                filePath==null -> addRestaurant("no image")
                else -> uploadFile()
            }

        }


        imgadddiag.setOnClickListener {

            showAlertDialog()
        }

    }

    private fun addRestaurant(image: String) {
        when {
            (name!!.text.toString()=="" || tlf!!.text.toString()=="" || des!!.text.toString()=="")
            -> Toast.makeText(this, "All the fields are required", Toast.LENGTH_SHORT).show()
            (tlf!!.text.toString().length!=8 && !Regex("\\+216\\d{8}").matches(tlf!!.text))
            -> Toast.makeText(this, "Bad phone number", Toast.LENGTH_SHORT).show()
            else -> {
                val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
                val iduser = prefs.getString("fb_id", null)
                disposable =
                        restaurantServices.insert(Model.ResultRestaurant(0, name!!.text.toString(), tlf!!.text.toString(), des!!.text.toString(),
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


    private val PICK_IAMGE_REQUEST = 1234

    private fun showFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "SELECT PICTURE"), PICK_IAMGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==PICK_IAMGE_REQUEST &&
                resultCode==Activity.RESULT_OK &&
                data!=null && data.data!=null) {
            filePath = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                imgView!!.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadFile() {
        if (filePath!=null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Loading ....")
            progressDialog.show()
            val imagepath = "images/" + UUID.randomUUID().toString()
            val imageRef = storageRef!!.child(imagepath)
            imageRef.putFile(filePath!!)
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        println("storageRef :$storageRef \nfilePath :$filePath \nimageRef :$imageRef")
//                        Toast.makeText(this, "File uploaded", Toast.LENGTH_SHORT).show()
                        addRestaurant(imagepath)
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
    }


    private fun RequestPermission(thisActivity: Activity, Permission: String, Code: Int) {
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

    private fun CheckPermission(context: Context, Permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context,
                Permission)==PackageManager.PERMISSION_GRANTED
    }

    private fun showAlertDialog() {
        val fm = supportFragmentManager
        val alertDialog = MapDialogFragment().newInstance("Maps", 35.771261, 10.834128, 0)
        alertDialog.show(fm, "fragment_alert")
    }

    override fun onFinishEditDialog(pos: LatLng) {
//        Toast.makeText(this, "Hi, $pos", Toast.LENGTH_SHORT).show()
        origin = pos
    }


}
