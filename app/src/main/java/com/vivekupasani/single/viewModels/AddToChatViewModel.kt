import android.app.Application
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

class AddToChatViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private val _userList = MutableLiveData<List<Users>>()
    val userList: LiveData<List<Users>> = _userList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    init {
        getUsers()
    }

    fun getUsers() {
        currentUserId?.let { uid ->
            viewModelScope.launch(Dispatchers.IO) { // Launching coroutine in IO thread
                try {
                    val querySnapshot = firestore.collection("Users").get().await() // Await Firestore query
                    val users = querySnapshot.toObjects(Users::class.java)

                    // Filtering users to exclude the current user and those who are already friends
                    val filteredList = users.filter { user ->
                        user.userId != uid && !user.friends.contains(uid)
                    }

                    // Post result on the main thread
                    _userList.postValue(filteredList)
                } catch (exception: Exception) {
                    // Handle error in case of failure
                    _errorMessage.postValue("Failed to load users: ${exception.message}")
                }
            }
        } ?: run {
            _errorMessage.value = "User not authenticated"
        }
    }
}
