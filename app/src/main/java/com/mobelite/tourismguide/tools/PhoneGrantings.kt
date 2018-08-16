package com.mobelite.tourismguide.tools

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.util.Log

class PhoneGrantings {

    companion object {
        //======================================  check if internet is available ======================================
         fun isNetworkAvailable(context: Context): Boolean {
             val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
             val activeNetworkInfo = connectivityManager.activeNetworkInfo
             val test = activeNetworkInfo!=null && activeNetworkInfo.isConnected
             Log.d("CONNECTION TEST ", java.lang.Boolean.toString(test))
             return  activeNetworkInfo!=null && activeNetworkInfo.isConnected

        }

        //====================================== get the user's id from shared preferences ======================================
        fun getSharedId(context:  Context) : String {
            val prefs = context.getSharedPreferences("FacebookProfile", ContextWrapper.MODE_PRIVATE)
            return prefs.getString("fb_id", null)
        }
    }
}