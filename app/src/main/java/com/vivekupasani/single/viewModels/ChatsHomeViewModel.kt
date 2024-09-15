package com.vivekupasani.single.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.models.Users

class ChatsHomeViewModel(application: Application) : AndroidViewModel(application) {

    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var currentUserId = auth.currentUser?.uid

    private val _userList = MutableLiveData<List<Users>>()
    var userList: LiveData<List<Users>> = _userList

    private val _error = MutableLiveData<String>()
    var error: LiveData<String> = _error

    init {
        getUsers()
    }

     fun getUsers() {
        firestore.collection("Users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val users = querySnapshot.toObjects(Users::class.java)

                // Filter users to exclude the current user and include only users who are friends
                val filteredList = users.filter { user ->
                    user.userId != currentUserId && user.friends.contains(currentUserId)
                }
                _userList.postValue(filteredList)
            }
            .addOnFailureListener { exception ->
                Log.e("ChatsHomeViewModel", "Error fetching users", exception)
                _error.postValue("Error fetching users: ${exception.message}")
            }
    }
}
