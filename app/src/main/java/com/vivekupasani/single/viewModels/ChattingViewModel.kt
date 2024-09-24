import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.vivekupasani.single.models.message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChattingViewModel(application: Application) : AndroidViewModel(application) {

    private var firebase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = mutableListOf<message>()
                val snapshot = firebase.getReference("Chats")
                    .child(senderRoom)
                    .child("Messages")
                    .get().await()

                for (messageSnapshot in snapshot.children) {
                    val messageItem = messageSnapshot.getValue(message::class.java)
                    messageItem?.let {
                        messages.add(it)
                    }
                }
                _messageList.postValue(messages)
            } catch (e: Exception) {
                _error.postValue("Error fetching messages: ${e.message}")
            }
        }
    }

    fun sendMessage(senderId: String, receiverId: String, messageText: String, imageUri: Uri?) {
        senderRoom = senderId + receiverId
        receiverRoom = receiverId + senderId

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser!!.uid

                if (imageUri != null) {
                    val storageRef = storage.getReference("Attachments")
                        .child("$currentUserId-${System.currentTimeMillis()}.jpg")

                    storageRef.putFile(imageUri).await()
                    val imageUrl = storageRef.downloadUrl.await().toString()
                    sendMessageToFirebase(senderId, receiverId, messageText, imageUrl)
                } else {
                    sendMessageToFirebase(senderId, receiverId, messageText, "")
                }
            } catch (e: Exception) {
                _error.postValue("Error sending message: ${e.message}")
            }
        }
    }

    private suspend fun sendMessageToFirebase(senderId: String, receiverId: String, messageText: String, imageUrl: String) {
        val messageModel = message(messageText, senderId, imageUrl, System.currentTimeMillis())

        try {
            firebase.getReference("Chats")
                .child(senderRoom)
                .child("Messages")
                .push()
                .setValue(messageModel).await()

            firebase.getReference("Chats")
                .child(receiverRoom)
                .child("Messages")
                .push()
                .setValue(messageModel).await()

            // Update the message list immediately
            val currentMessages = _messageList.value?.toMutableList() ?: mutableListOf()
            currentMessages.add(messageModel)
            _messageList.postValue(currentMessages)

            _msgSend.postValue(true)
        } catch (e: Exception) {
            _error.postValue("Error sending message: ${e.message}")
        }
    }
}
