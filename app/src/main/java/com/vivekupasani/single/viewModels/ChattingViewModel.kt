package com.vivekupasani.single.viewModels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.vivekupasani.single.models.message

class ChattingViewModel(application: Application) : AndroidViewModel(application) {

    private var firebase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var storage : FirebaseStorage = FirebaseStorage.getInstance()
    private var auth : FirebaseAuth = FirebaseAuth.getInstance()

    lateinit var senderRoom: String
    lateinit var receiverRoom: String

    private var _msgSend = MutableLiveData<Boolean>()
    val msgSend: LiveData<Boolean> = _msgSend

    private var _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _messageList = MutableLiveData<List<message>>()
    val messageList: LiveData<List<message>> = _messageList

    fun initializeRooms(senderId: String, receiverId: String) {
        senderRoom = senderId + receiverId
        receiverRoom = receiverId + senderId
        displayChats()
    }

    private fun displayChats() {
        firebase.getReference("Chats")
            .child(senderRoom)
            .child("Messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<message>()
                    for (messageSnapshot in snapshot.children) {
                        val messageItem = messageSnapshot.getValue(message::class.java)
                        messageItem?.let {
                            messages.add(it)
                        }
                    }
                    _messageList.postValue(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    _error.postValue("Error fetching messages: ${error.message}")
                }
            })
    }

    fun sendMessage(senderId: String, receiverId: String, messageText: String, imageUri: Uri?) {
        senderRoom = senderId + receiverId
        receiverRoom = receiverId + senderId
        val currentUserId = auth.currentUser!!.uid

        if (imageUri != null) {
            // Upload the image to Firebase Storage
            val storageRef = storage.getReference("Attachments")
                .child("$currentUserId-${System.currentTimeMillis()}.jpg")

            storageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        // After uploading the image, send the message with the image URL
                        sendMessageWithImage(senderId, receiverId, messageText, imageUrl)
                    }
                }
                .addOnFailureListener {
                    _error.postValue("Error uploading image: ${it.message}")
                }
        } else {
            // If no image, just send the message
            sendMessageWithImage(senderId, receiverId, messageText, "")
        }
    }

    private fun sendMessageWithImage(senderId: String, receiverId: String, messageText: String, imageUrl: String?) {
        val messageModel = message(messageText, senderId, imageUrl!!, System.currentTimeMillis())

        firebase.getReference("Chats")
            .child(senderRoom)
            .child("Messages")
            .push()
            .setValue(messageModel)
            .addOnSuccessListener {
                firebase.getReference("Chats")
                    .child(receiverRoom)
                    .child("Messages")
                    .push()
                    .setValue(messageModel)
                    .addOnSuccessListener {
                        _msgSend.value = true
                    }
                    .addOnFailureListener {
                        _error.postValue("Error sending message to receiver: ${it.message}")
                    }
            }
            .addOnFailureListener {
                _error.postValue("Error sending message to sender: ${it.message}")
            }
    }

}
