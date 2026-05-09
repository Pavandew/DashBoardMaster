package com.example.masterdashboard.signup

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.masterdashboard.R
import com.example.masterdashboard.home.HomeActivity
import com.example.masterdashboard.login.LoginActivity
import com.google.android.material.button.MaterialButton

class SignUpActivity : AppCompatActivity() {
    private lateinit var createAccBtn: MaterialButton
    private lateinit var loginTv: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        loginTv = findViewById(R.id.signup_login_tv)
        createAccBtn = findViewById(R.id.signup_create_acc_btn)

        loginTv.setOnClickListener {
            val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        createAccBtn.setOnClickListener {
            val intent = Intent(this@SignUpActivity, HomeActivity::class.java)
            startActivity(intent)
        }
    }
}