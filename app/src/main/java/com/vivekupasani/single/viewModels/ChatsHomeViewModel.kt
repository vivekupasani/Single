package com.vivekupasani.single.viewModels

import android.app.Application
import android.util.Log
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

class ChatsHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _userList = MutableLiveData<List<Users>>()
    val userList: LiveData<List<Users>> = _userList

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        getUsers()
    }

    fun getUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch the user data from Firestore asynchronously
                val querySnapshot = firestore.collection("Users").get().await()
                val users = querySnapshot.toObjects(Users::class.java)

                // Filter users: exclude the current user and include only friends
                val filteredList = users.filter { user ->
                    user.userId != currentUserId && user.friends.contains(currentUserId)
                }

                // Post the filtered list to the LiveData on the main thread
                _userList.postValue(filteredList)
            } catch (e: Exception) {
                Log.e("ChatsHomeViewModel", "Error fetching users", e)
                _error.postValue("Error fetching users: ${e.message}")
            }
        }
    }
}
