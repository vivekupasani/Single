package com.vivekupasani.single

import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.vivekupasani.single.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var auth : FirebaseAuth
    lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNavigation = binding.bottomNavigationView
        val navController = Navigation.findNavController(this,R.id.fragmentContainerView)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        NavigationUI.setupWithNavController(bottomNavigation,navController)

        updateLastSeen()


    }



    private fun updateLastSeen() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("Users")
                .document(currentUser.uid)
                .update("timestamp", System.currentTimeMillis())
                .addOnSuccessListener {
                    Log.d("updatelastseen", "updated Last Seen")
                }
                .addOnFailureListener {
                    Log.d("updatelastseen", "Failed to update timestamp")
                }
        } else {
            Log.d("updatelastseen", "user not loged in ")
        }
    }
}