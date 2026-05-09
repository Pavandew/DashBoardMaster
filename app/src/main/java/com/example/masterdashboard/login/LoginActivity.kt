package com.example.masterdashboard.login

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.home.HomeActivity
import com.example.masterdashboard.R
import com.example.masterdashboard.signup.SignUpActivity
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var createAccTv: TextView
    private lateinit var loginBtn: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        createAccTv = findViewById(R.id.login_create_acc_tv)
        loginBtn = findViewById(R.id.login_btn)

        createAccTv.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val intent = Intent(this@LoginActivity, HomeActivity::class.java)
            startActivity(intent)
        }

    }
}