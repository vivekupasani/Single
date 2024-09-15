package com.vivekupasani.single.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.models.Users

class Authentication(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> = _signupStatus

    private val _signInStatus = MutableLiveData<Boolean>()
    val signInStatus: LiveData<Boolean> = _signInStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun signUpUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _signupStatus.postValue(true)
                storeDetailsInFirebase(email)
            }
            .addOnFailureListener { exception ->
                _errorMessage.postValue("${exception.message}")
                _signupStatus.postValue(false)
            }
    }

    fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _signInStatus.postValue(true)
            }
            .addOnFailureListener { exception ->
                _errorMessage.postValue("${exception.message}")
                _signInStatus.postValue(false)
            }
    }

    private fun storeDetailsInFirebase(email: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val user = Users(currentUser.uid, email, "", System.currentTimeMillis())
            firestore.collection("Users").document(currentUser.uid).set(user)
                .addOnSuccessListener {
                    // Handle success if needed
                }
                .addOnFailureListener { exception ->
                    _errorMessage.postValue("${exception.message}")
                }
        } else {
            _errorMessage.postValue("User is not logged in.")
        }
    }
}
