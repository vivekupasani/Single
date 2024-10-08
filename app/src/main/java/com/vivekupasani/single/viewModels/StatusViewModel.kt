package com.vivekupasani.single.viewModels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.models.status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StatusViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val _uploaded = MutableLiveData<Boolean>()
    val uploaded: LiveData<Boolean> get() = _uploaded

    private val _statusList = MutableLiveData<List<status>>()
    val statusList: LiveData<List<status>> get() = _statusList

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    init {
        displayStatus()
        cleanupExpiredStatuses()
    }

    fun uploadStatus(image: Uri) {
        val currentUserId = auth.currentUser?.uid ?: run {
            _error.value = "User not logged in"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val documentSnapshot = firestore.collection("Users").document(currentUserId).get().await()
                val user = documentSnapshot.toObject(Users::class.java)

                val currentUserName = user?.userName ?: "Unknown User"
                val currentProfilePic = user?.profilePicURL ?: ""

                val storageRef = storage.getReference("Status").child("$currentUserId${System.currentTimeMillis()}.jpg")
                storageRef.putFile(image).await()

                val uri = storageRef.downloadUrl.await()
                val newStatus = status(currentUserName, currentProfilePic, currentUserId, uri.toString(), System.currentTimeMillis())

                val userDetails = mapOf(
                    "name" to currentUserName,
                    "profile" to currentProfilePic,
                    "userId" to currentUserId,
                    "lastUpdated" to System.currentTimeMillis()
                )

                database.getReference("Status").child(currentUserId).updateChildren(userDetails).await()
                database.getReference("Status").child(currentUserId).child("Statuses").push().setValue(newStatus).await()

                _uploaded.postValue(true)
            } catch (e: Exception) {
                _error.postValue("Error uploading status: ${e.localizedMessage}")
            }
        }
    }

    fun displayStatus() {
        val currentUserId = auth.currentUser?.uid ?: run {
            _error.value = "User not logged in"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val documentSnapshot = firestore.collection("Users")
                    .document(currentUserId)
                    .get().await()

                val user = documentSnapshot.toObject<Users>()
                val friendsList = user?.friends ?: emptyList()

                val allStatuses = mutableListOf<status>()

                val dataSnapshot = database.getReference("Status").get().await()
                for (friendUserId in dataSnapshot.children) {
                    val statusesSnapshot = friendUserId.child("Statuses")

                    if (friendUserId.key == currentUserId || friendUserId.key in friendsList) {
                        val latestStatusSnapshot = statusesSnapshot.children.firstOrNull()
                        latestStatusSnapshot?.let {
                            val userStatus = it.getValue(status::class.java)
                            userStatus?.let { statusObj ->
                                allStatuses.add(statusObj)
                            }
                        }
                    }
                }

                _statusList.postValue(allStatuses)
            } catch (e: Exception) {
                Log.e("StatusViewModel", "Error fetching statuses: ${e.localizedMessage}")
                _error.postValue("Error fetching statuses: ${e.localizedMessage}")
            }
        }
    }



    fun cleanupExpiredStatuses() {
        val currentTime = System.currentTimeMillis()
        val expirationTime = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataSnapshot = database.getReference("Status").get().await()
                for (snapshot in dataSnapshot.children) {
                    val statusesSnapshot = snapshot.child("Statuses")
                    for (statusSnapshot in statusesSnapshot.children) {
                        val statusObj = statusSnapshot.getValue(status::class.java)
                        statusObj?.let {
                            if (currentTime - it.lastUpdated > expirationTime) {
                                statusSnapshot.ref.removeValue().await()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.postValue("Failed to clean up statuses: ${e.localizedMessage}")
            }
        }
    }
}

