package com.vivekupasani.single.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.models.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _user = MutableLiveData<Users>()
    val user: LiveData<Users> get() = _user

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val documentSnapshot = firestore.collection("Users").document(userId).get().await()
                    if (documentSnapshot.exists()) {
                        val userData = documentSnapshot.toObject(Users::class.java)
                        _user.postValue(userData)
                    } else {
                        _error.postValue("User data not found")
                    }
                } catch (e: Exception) {
                    _error.postValue("Error fetching user data: ${e.message}")
                }
            }
        } else {
            _error.value = "User not logged in"
        }
    }
}
