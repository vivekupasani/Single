package com.vivekupasani.single.viewModels

import android.app.Application
import android.widget.ProgressBar
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.models.Users

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val _user = MutableLiveData<Users>()
    val user: LiveData<Users> get() = _user

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchUserData() {
        firestore.collection("Users")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    val user = it.toObject(Users::class.java)
                    _user.value = user!!
                } else {
                    _error.value = "User data not found"
                }
            }
            .addOnFailureListener {
                _error.value = it.message
            }
    }
}