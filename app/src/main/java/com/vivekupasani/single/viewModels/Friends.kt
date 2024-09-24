package com.vivekupasani.single.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.models.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Friends(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _sendRequest = MutableLiveData<Boolean>()
    val sendRequest: LiveData<Boolean> = _sendRequest

    private val _acceptedRequest = MutableLiveData<Boolean>()
    val acceptedRequest: LiveData<Boolean> = _acceptedRequest

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Function to send a friend request to the selected user
    fun sendRequest(selectedUser: Users) {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserRef = firestore.collection("Users").document(currentUserId)
                val recipientUserRef = firestore.collection("Users").document(selectedUser.userId)

                // Fetch current user data
                val documentSnapshot = currentUserRef.get().await()

                val currentUserName = documentSnapshot.getString("userName") ?: "Unknown"
                val currentUserProfilePic = documentSnapshot.getString("profilePicURL") ?: ""
                val currentUserAbout = documentSnapshot.getString("about") ?: ""

                val friendRequest = hashMapOf(
                    "fromUid" to currentUserId,
                    "fromName" to currentUserName,
                    "fromProfilePicture" to currentUserProfilePic,
                    "fromAbout" to currentUserAbout,
                    "status" to "pending"
                )

                // Update the recipient's friend requests
                recipientUserRef.update("friendRequests", FieldValue.arrayUnion(friendRequest)).await()
                withContext(Dispatchers.Main) {
                    _sendRequest.value = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.postValue("Error sending request: ${e.message}")
                }
            }
        }
    }

    // Function to accept a friend request from the selected user
    fun acceptRequest(selectedUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserRef = firestore.collection("Users").document(currentUserId)
                val recipientUserRef = firestore.collection("Users").document(selectedUserId)

                // Retrieve the friend request details from the recipient user
                val documentSnapshot = recipientUserRef.get().await()
                val friendRequests = documentSnapshot.get("friendRequests") as? MutableList<Map<String, Any>>

                if (friendRequests != null) {
                    // Filter out the request to be removed
                    val updatedRequests = friendRequests.filter { request ->
                        request["fromUid"] != selectedUserId
                    }

                    // Update the recipient's friendRequests array with the filtered list
                    currentUserRef.update("friendRequests", updatedRequests).await()

                    // Add both users to each other's friends list
                    currentUserRef.update("friends", FieldValue.arrayUnion(selectedUserId)).await()
                    recipientUserRef.update("friends", FieldValue.arrayUnion(currentUserId)).await()

                    withContext(Dispatchers.Main) {
                        _acceptedRequest.postValue(true)
                    }

                } else {
                    withContext(Dispatchers.Main) {
                        _error.postValue("No friend requests found.")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.postValue("Error accepting request: ${e.message}")
                }
            }
        }
    }
}
