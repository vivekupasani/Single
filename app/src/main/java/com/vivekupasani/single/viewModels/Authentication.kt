import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.vivekupasani.single.models.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Authentication(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var token: String? = null

    private val _signupStatus = MutableLiveData<Boolean>()
    val signupStatus: LiveData<Boolean> = _signupStatus

    private val _signInStatus = MutableLiveData<Boolean>()
    val signInStatus: LiveData<Boolean> = _signInStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun signUpUser(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sign up user asynchronously
                auth.createUserWithEmailAndPassword(email, password).await()
                _signupStatus.postValue(true)
                storeDetailsInFirebase(email, password) // Store user details after successful sign-up
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
                _signupStatus.postValue(false)
            }
        }
    }

    fun signInUser(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Sign in user asynchronously
                auth.signInWithEmailAndPassword(email, password).await()
                _signInStatus.postValue(true)
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
                _signInStatus.postValue(false)
            }
        }
    }

    private fun storeDetailsInFirebase(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the FCM token asynchronously
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                if (fcmToken.isNotEmpty()) {
                    token = fcmToken
                }

                // Store user details in Firestore
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val user = Users(
                        currentUser.uid, email, password, System.currentTimeMillis(),
                        token = token.toString()
                    )
                    firestore.collection("Users")
                        .document(currentUser.uid)
                        .set(user)
                        .await() // Await Firestore operation
                } else {
                    _errorMessage.postValue("User is not logged in.")
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message)
            }
        }
    }
}
