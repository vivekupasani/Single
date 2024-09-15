package com.vivekupasani.single.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.models.status

class ViewStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val _statusList = MutableLiveData<List<status>>()
    val statusList: LiveData<List<status>> get() = _statusList

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun showAllStatus(userId: String) {
        database.getReference("Status")
            .child(userId)
            .child("Statuses")
            .get()
            .addOnSuccessListener { datasnap ->
                val statuses = mutableListOf<status>()

                if (datasnap.exists()) {
                    for (snap in datasnap.children) {
                        val statusItem = snap.getValue(status::class.java)
                        statusItem?.let { statuses.add(it) } // Add to list if not null
                    }
                    _statusList.postValue(statuses) // Update LiveData with the list of statuses
                } else {
                    _statusList.postValue(emptyList()) // If no data, post an empty list
                }
            }
            .addOnFailureListener { exception ->
                _error.postValue("Failed to load statuses: ${exception.message}") // Post error message
            }
    }

}
