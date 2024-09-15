package com.vivekupasani.single.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.models.Users

class AddToChatViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private val _userList = MutableLiveData<List<Users>>()
    val userList: LiveData<List<Users>> = _userList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        getUsers()
    }

     fun getUsers() {
        currentUserId?.let { uid ->
            firestore.collection("Users")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val users = querySnapshot.toObjects(Users::class.java)
                    val filteredList = users.filter { user ->
                        user.userId != uid &&
                                !user.friends.contains(uid)
//                                (user.friendRequests["fromUid"]?.toString() != uid)
                    }
                    _userList.postValue(filteredList)
                }
                .addOnFailureListener { exception ->
                    _errorMessage.value = "Failed to load users: ${exception.message}"
                }
        } ?: run {
            _errorMessage.value = "User not authenticated"
        }
    }
}
