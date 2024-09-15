package com.vivekupasani.single.viewModels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vivekupasani.single.models.Users

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _edited = MutableLiveData<Boolean>()
    val edited: LiveData<Boolean> = _edited

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun editProfile(imageUri: Uri?, username: String, email: String,password : String, about: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("Users").document(userId)

        if (imageUri != null) {
            val storageRef = storage.getReference("Profile Pics").child("$userId.jpg")
            storageRef.putFile(imageUri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val updatedUser = Users(
                            userId = userId,
                            email = email,
                            about = about,
                            password = password,
                            userName = username,
                            profilePicURL = uri.toString()
                        )
                        updateUsersProfile(userRef, updatedUser)
                    }
                }
                .addOnFailureListener {
                    _error.value = "Failed to upload profile image: ${it.message}"
                }
        } else {
            // Update the profile without changing the profile picture
            userRef.get().addOnSuccessListener { document ->
                val currentProfilePicURL = document.getString("profilePicURL").orEmpty()
                val updatedUser = Users(
                    userId = userId,
                    email = email,
                    password = password,
                    about = about,
                    userName = username,
                    profilePicURL = currentProfilePicURL
                )
                updateUsersProfile(userRef, updatedUser)
            }.addOnFailureListener {
                _error.value = "Failed to fetch existing profile data: ${it.message}"
            }
        }
    }

    private fun updateUsersProfile(userRef: DocumentReference, user: Users) {
        userRef.set(user)
            .addOnSuccessListener {
                _edited.value = true
            }
            .addOnFailureListener {
                _error.value = "Failed to update profile: ${it.message}"
            }
    }
}
