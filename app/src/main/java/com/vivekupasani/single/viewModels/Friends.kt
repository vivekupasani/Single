package com.vivekupasani.single.viewModels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.models.Users

class Friends(application: Application) : AndroidViewModel(application) {

    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _sendRequest = MutableLiveData<Boolean>()
    var sendRequest: LiveData<Boolean> = _sendRequest

    private val _acceptedRequest = MutableLiveData<Boolean>()
    var acceptedRequest: LiveData<Boolean> = _acceptedRequest

    private val _error = MutableLiveData<String>()
    var error: LiveData<String> = _error

    fun sendRequest(selectedUser: Users) {
        val currentUser = auth.currentUser?.uid
        val curretUserRef = firestore.collection("Users").document(currentUser!!)
        val recipentUserRef = firestore.collection("Users").document(selectedUser.userId)

        curretUserRef.get().addOnSuccessListener { documentSnapshot ->
            val currentUserName = documentSnapshot.getString("userName") ?: "Unknown"
            val currentUserProfilePic = documentSnapshot.getString("profilePicURL") ?: ""
            val currentUserAbout = documentSnapshot.getString("about") ?: ""

            val friendRequest = hashMapOf(
                "fromUid" to currentUser,
                "fromName" to currentUserName,
                "fromProfilePicture" to currentUserProfilePic,
                "fromAbout" to currentUserAbout,
                "status" to "pending"
            )

            recipentUserRef.update("friendRequests", FieldValue.arrayUnion(friendRequest))
                .addOnSuccessListener {
                    _sendRequest.value = true
                }
                .addOnFailureListener {
                    _error.postValue("Error sending request: ${it.message}")
                }
        }.addOnFailureListener {
            _error.postValue("Failed to retrieve current user: ${it.message}")
        }
    }

    fun acceptRequest(selectedUserId: String) {
        val currentUser = auth.currentUser?.uid
        if (currentUser == null) {
            _error.postValue("Current user is not logged in.")
            return
        }

        val currentUserRef = firestore.collection("Users").document(currentUser)
        val recipientUserRef = firestore.collection("Users").document(selectedUserId)

        // Retrieve the friend request details from the recipient user
        recipientUserRef.get().addOnSuccessListener { documentSnapshot ->
            val friendRequests = documentSnapshot.get("friendRequests") as? MutableList<Map<String, Any>>

            if (friendRequests != null) {
                // Filter out the request to be removed
                val updatedRequests = friendRequests.filter { request ->
                    request["fromUid"] != selectedUserId
                }

                // Update the recipient's friendRequests array with the filtered list
                currentUserRef.update("friendRequests", updatedRequests)
                    .addOnSuccessListener {
                        // Add both users to each other's friends list
                        currentUserRef.update("friends", FieldValue.arrayUnion(selectedUserId))
                            .addOnSuccessListener {
                                recipientUserRef.update("friends", FieldValue.arrayUnion(currentUser))
                                    .addOnSuccessListener {
                                        _acceptedRequest.postValue(true)
                                    }
                                    .addOnFailureListener { exception ->
                                        _error.postValue("Error updating recipient's friend list: ${exception.message}")
                                    }
                            }
                            .addOnFailureListener { exception ->
                                _error.postValue("Error updating current user's friend list: ${exception.message}")
                            }
                    }
                    .addOnFailureListener { exception ->
                        _error.postValue("Error updating friend requests: ${exception.message}")
                    }
            } else {
                _error.postValue("No friend requests found.")
            }
        }.addOnFailureListener { exception ->
            _error.postValue("Failed to retrieve recipient's friend requests: ${exception.message}")
        }
    }
}
