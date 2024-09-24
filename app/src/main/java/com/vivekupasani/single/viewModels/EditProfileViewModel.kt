package com.vivekupasani.single.viewModels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.vivekupasani.single.models.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private var token: String? = null

    private val _edited = MutableLiveData<Boolean>()
    val edited: LiveData<Boolean> = _edited

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun editProfile(imageUri: Uri?, username: String, email: String, password: String, about: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("Users").document(userId)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch the Firebase token for notifications
                token = FirebaseMessaging.getInstance().token.await()

                if (imageUri != null) {
                    // Upload new profile image if it exists
                    val storageRef = storage.getReference("Profile Pics").child("$userId.jpg")
                    storageRef.putFile(imageUri).await()
                    val profilePicUrl = storageRef.downloadUrl.await().toString()

                    // Create user with new profile image URL
                    val updatedUser = Users(
                        userId = userId,
                        email = email,
                        about = about,
                        password = password,
                        userName = username,
                        profilePicURL = profilePicUrl,
                        token = token ?: ""
                    )
                    updateUsersProfile(userRef, updatedUser)
                } else {
                    // Fetch the current profile picture if no new image is uploaded
                    val document = userRef.get().await()
                    val currentProfilePicURL = document.getString("profilePicURL").orEmpty()

                    val updatedUser = Users(
                        userId = userId,
                        email = email,
                        about = about,
                        password = password,
                        userName = username,
                        profilePicURL = currentProfilePicURL,
                        token = token ?: ""
                    )
                    updateUsersProfile(userRef, updatedUser)
                }
            } catch (e: Exception) {
                _error.postValue("Failed to update profile: ${e.message}")
            }
        }
    }

    private suspend fun updateUsersProfile(userRef: DocumentReference, user: Users) {
        try {
            userRef.set(user).await()
            _edited.postValue(true)
        } catch (e: Exception) {
            _error.postValue("Failed to update profile: ${e.message}")
        }
    }
}
