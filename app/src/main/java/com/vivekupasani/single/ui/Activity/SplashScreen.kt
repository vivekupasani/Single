package com.vivekupasani.single.ui.Activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.vivekupasani.single.MainActivity
import com.vivekupasani.single.R
import com.vivekupasani.single.databinding.ActivitySplashScreenBinding

class SplashScreen : AppCompatActivity() {

    lateinit var binding: ActivitySplashScreenBinding
    lateinit var auth: FirebaseAuth
    lateinit var firestore: FirebaseFirestore
    lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()



        setProgress(true)
        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser == null) {
                val intent = Intent(this, OnBoard::class.java)
                startActivity(intent)
                setProgress(false)
                finish()
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                setProgress(false)
                finish()
            }

        }, 1000)


    }


    fun setProgress(isProgress: Boolean) {
//        if (isProgress) {
//            binding.progressBar.isIndeterminate = true
//            binding.progressBar.visibility = android.view.View.VISIBLE
//        } else {
//            binding.progressBar.isIndeterminate = false
//            binding.progressBar.visibility = android.view.View.GONE
//        }
    }
}