import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.models.status

class StatusViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private lateinit var currentUserName: String
    private lateinit var currentProfilePic: String

    private val _uploaded = MutableLiveData<Boolean>()
    val uploaded: LiveData<Boolean> get() = _uploaded

    private val _statusList = MutableLiveData<List<status>>()
    val statusList: LiveData<List<status>> get() = _statusList

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    init {
        displayStatus()
        cleanupExpiredStatuses() // Automatically clean up expired statuses when viewModel initializes
    }

    fun uploadStatus(image: Uri) {
        val currentUserId = auth.currentUser?.uid ?: run {
            _error.value = "User not logged in"
            return
        }
        firestore.collection("Users")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(Users::class.java)
                currentUserName = user?.userName ?: ""
                currentProfilePic = user?.profilePicURL ?: ""

                val storageRef = storage.getReference("Status")
                    .child("$currentUserId${System.currentTimeMillis()}.jpg")
                storageRef.putFile(image)
                    .addOnSuccessListener {
                        storageRef.downloadUrl
                            .addOnSuccessListener { uri ->
                                val status = status(
                                    currentUserName,
                                    currentProfilePic,
                                    auth.currentUser!!.uid,
                                    uri.toString(),
                                    System.currentTimeMillis() // Save the current time as upload time
                                )

                                val userDetails = hashMapOf(
                                    "name" to currentUserName,
                                    "profile" to currentProfilePic,
                                    "userId" to currentUserId,
                                    "lastUpdated" to System.currentTimeMillis()
                                )

                                database.getReference().child("Status")
                                    .child(currentUserId)
                                    .updateChildren(userDetails as Map<String, Any>)

                                database.getReference().child("Status")
                                    .child(currentUserId)
                                    .child("Statuses")
                                    .push()
                                    .setValue(status)
                                    .addOnSuccessListener {
                                        _uploaded.value = true
                                    }
                            }
                            .addOnFailureListener { e ->
                                _error.value = "Failed to get download URL: ${e.message}"
                            }
                    }
                    .addOnFailureListener { e ->
                        _error.value = "Failed to upload status image: ${e.message}"
                    }

            }
            .addOnFailureListener { e ->
                _error.value = "Failed to fetch user details: ${e.message}"
            }
    }

    fun displayStatus() {
        val currentUserId = auth.currentUser?.uid ?: run {
            _error.value = "User not logged in"
            return
        }

        firestore.collection("Users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject<Users>()
                val friendsList = user?.friends ?: emptyList()

                val allStatuses = mutableListOf<status>()
                val statusFetchTasks = mutableListOf<Task<status>>()

                // Fetch statuses for all friends and the authenticated user from Realtime Database
                database.getReference("Status").get()
                    .addOnSuccessListener { dataSnapshot ->
                        for (snapshot in dataSnapshot.children) {
                            val userId = snapshot.key
                            val statusesSnapshot = snapshot.child("Statuses")

                            // Check if it's the authenticated user's own statuses or a friend's statuses
                            if (userId == currentUserId || userId in friendsList) {
                                val latestStatusSnapshot = statusesSnapshot.children.firstOrNull()
                                latestStatusSnapshot?.let {
                                    val userStatus = it.getValue(status::class.java)
                                    userStatus?.let { statusObj ->
                                        allStatuses.add(statusObj)
                                    }
                                }
                            }
                        }

                        _statusList.value = allStatuses
                    }
                    .addOnFailureListener { e ->
                        _error.value = "Failed to fetch statuses: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to fetch user details: ${e.message}"
            }
    }


    fun cleanupExpiredStatuses() {
        val currentTime = System.currentTimeMillis()
        val expirationTime = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

        database.getReference("Status")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                for (snapshot in dataSnapshot.children) {
                    val statusesSnapshot = snapshot.child("Statuses")

                    for (statusSnapshot in statusesSnapshot.children) {
                        val statusObj = statusSnapshot.getValue(status::class.java)
                        statusObj?.let {
                            if (currentTime - it.lastUpdated > expirationTime) {
                                // Delete the expired status
                                statusSnapshot.ref.removeValue()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                _error.value = "Failed to clean up statuses: ${e.message}"
            }
    }
}