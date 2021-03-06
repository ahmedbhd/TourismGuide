package com.mobelite.tourismguide

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.facebook.login.LoginManager
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
        beginTransaction().func().commit()
    }

    fun AppCompatActivity.addFragment(fragment: Fragment, frameId: Int) {
        supportFragmentManager.inTransaction { add(frameId, fragment) }
    }

    fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int) {
        supportFragmentManager.inTransaction { replace(frameId, fragment) }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                replaceFragment(HomeFragment.newInstance(), R.id.page)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                replaceFragment(MapsFragment.newInstance(), R.id.page)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                replaceFragment(FavouriteFragment.newInstance(), R.id.page)

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    lateinit var toolbar: ActionBar
    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addFragment(MapsFragment.newInstance(), R.id.page)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        loginManager = LoginManager.getInstance()


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private var loginManager: LoginManager? = null


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id==R.id.disconnect) { // disconnect action

            val preferences = getSharedPreferences("FacebookProfile", Context.MODE_PRIVATE)
            val editor = preferences.edit()
            editor.clear()
            editor.apply()
            if (loginManager!=null) {
                loginManager!!.logOut()
            }
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            return true
        }
        return true
    }


    override fun onBackPressed() {

    }
}
