package com.mobelite.tourismguide

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity


import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.content_login.*

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)

        login.setOnClickListener {
            val intent = Intent(this, MainActivity().javaClass)

            startActivity(intent)
        }
    }

}
