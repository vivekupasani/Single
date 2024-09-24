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

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _userList = MutableLiveData<List<Users>>()
    val userList: LiveData<List<Users>> = _userList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        getNotifications()
    }

    fun getNotifications() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val documentSnapshot = firestore.collection("Users").document(userId).get().await()
                    val friendRequests = documentSnapshot.get("friendRequests") as? List<Map<String, Any>>

                    if (friendRequests != null && friendRequests.isNotEmpty()) {
                        val users = mutableListOf<Users>()

                        for (request in friendRequests) {
                            val friendId = request["fromUid"] as? String
                            if (friendId != null) {
                                val friendDoc = firestore.collection("Users").document(friendId).get().await()
                                val user = friendDoc.toObject(Users::class.java)
                                if (user != null) {
                                    users.add(user)
                                }
                            }
                        }

                        _userList.postValue(users.toList()) // Update the user list on the main thread
                    } else {
                        _errorMessage.postValue("No friend requests found.")
                    }

                } catch (e: Exception) {
                    _errorMessage.postValue("Error fetching notifications: ${e.message}")
                }
            }
        } else {
            _errorMessage.value = "User not logged in."
        }
    }
}
