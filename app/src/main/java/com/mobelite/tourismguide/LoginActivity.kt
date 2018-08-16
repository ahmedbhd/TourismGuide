package com.mobelite.tourismguide


import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.facebook.*
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.mobelite.tourismguide.data.webservice.Model
import com.mobelite.tourismguide.data.webservice.RestaurantServices
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.util.*


@Suppress("DEPRECATION")
class LoginActivity : AppCompatActivity() {
    private val EMAIL = "email"
    private var callbackManager: CallbackManager? = null
    var presmissions = ArrayList<String>()
    private val TAG = "facebook_login"
    private var loginManager: LoginManager? = null

    private val restaurantServices by lazy {
        RestaurantServices.create()
    }
    private var disposable: Disposable? = null
    var progressDialog: ProgressDialog? = null


    @SuppressLint("PackageManagerGetSignatures")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)
        callbackManager = CallbackManager.Factory.create()
        progressDialog = ProgressDialog(this)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
        loginManager = LoginManager.getInstance()

        val loginButton = findViewById<LoginButton>(R.id.login).apply {
            setReadPermissions(Arrays.asList(EMAIL))
        }


        val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)

        if (prefs.getString("fb_id", null)!=null) { // check if user is already logged in
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
        } else { // load data from facebook
            loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {

                    presmissions.add("email")
                    presmissions.add("public_profile")

                    val accessToken = loginResult.accessToken.token
                    // save accessToken to SharedPreference
                    @Suppress("NAME_SHADOWING")
                    val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.putString("fb_access_token", accessToken)
                    editor.apply()
                    val request = GraphRequest.newMeRequest(
                            loginResult.accessToken
                    ) { jsonObject, _ ->
                        progressDialog!!.setTitle("Loading ....")
                        progressDialog!!.show()

                        // Getting FB User Data
                        getFacebookData(jsonObject)

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        //finish()
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "id,first_name,last_name,email,gender")
                    request.parameters = parameters
                    request.executeAsync()
                    progressDialog!!.dismiss()


                }


                override fun onCancel() {
                    progressDialog!!.dismiss()
                    Log.d(TAG, "Login attempt cancelled.")
                }

                override fun onError(e: FacebookException) {
                    progressDialog!!.dismiss()
                    e.printStackTrace()
                    Log.d(TAG, "Login attempt failed.")
                    deleteAccessToken()
                }
            }
            )
        }


    }

    private fun getFacebookData(`object`: JSONObject): Bundle? {
        val bundle = Bundle()

        try {
            val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
            val editor = prefs.edit()
            val id = `object`.getString("id")
            editor.putString("fb_id", id)

            val profilepic: URL
            try {

                profilepic = URL("https://graph.facebook.com/$id/picture?type=large")
                Log.i("profile_pic", profilepic.toString() + "")
                bundle.putString("profile_pic", profilepic.toString())
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                return null
            }

            bundle.putString("idFacebook", id)
            if (`object`.has("first_name"))
                bundle.putString("first_name", `object`.getString("first_name"))
            if (`object`.has("last_name"))
                bundle.putString("last_name", `object`.getString("last_name"))
            if (`object`.has("email"))
                bundle.putString("email", `object`.getString("email"))

            editor.putString("fb_first_name", `object`.getString("first_name"))
            editor.putString("fb_last_name", `object`.getString("last_name"))
            editor.putString("fb_email", `object`.getString("email"))
            editor.putString("fb_profileURL", profilepic.toString())
            editor.apply() // This line is IMPORTANT !!!


        } catch (e: Exception) {
            Log.d(TAG, "BUNDLE Exception : " + e.toString())
        }

        // add user to data base
        addUser(`object`.getString("id"), `object`.getString("email"))

        return bundle
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        callbackManager!!.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun deleteAccessToken() {
        object : AccessTokenTracker() {
            override fun onCurrentAccessTokenChanged(
                    oldAccessToken: AccessToken,
                    currentAccessToken: AccessToken?) {

                if (currentAccessToken==null) {
                    //User logged out
                    val prefs = getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
                    val editor = prefs.edit()
                    editor.clear()
                    editor.apply()
                    LoginManager.getInstance().logOut()
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()

        //Facebook
        if (loginManager!=null) {
            loginManager!!.logOut()
        }
    }

    //============================= add/update user to data base ==============================
    private fun addUser(id: String, email: String) {
        disposable =
                restaurantServices.insertUser(Model.User(id, email))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                { result ->
                                    run {

                                        if (result=="ok") {

                                            //Toast.makeText(this, "The user has been ADDED", Toast.LENGTH_SHORT).show()

                                        }
                                    }
                                },
                                { error -> println(error.message) }
                        )
    }
}






