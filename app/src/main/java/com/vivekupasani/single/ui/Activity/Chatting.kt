package com.vivekupasani.single.ui.Activity

import ChattingAdapter
import ChattingViewModel
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mikelau.shimmerrecyclerviewx.ShimmerRecyclerViewX
import com.vivekupasani.single.R
import com.vivekupasani.single.databinding.ActivityChattingBinding
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.models.message
import com.vivekupasani.single.notification.NotificationApi
import com.vivekupasani.single.notification.models.Notification
import com.vivekupasani.single.notification.models.NotificationData
import com.vivekupasani.single.ui.fragment.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Suppress("DEPRECATION")
class Chatting : AppCompatActivity() {

    private lateinit var binding: ActivityChattingBinding
    private var imageUrl: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: ChattingAdapter
    private lateinit var recyclerView: ShimmerRecyclerViewX
    private lateinit var progressDialog: AlertDialog

    private lateinit var senderName: String

    var username: String = ""
    var token: String = ""
    var profilePic: String = ""
    var email: String = ""
    var about: String = ""
    var receiverUID: String = ""
    var senderId: String = ""
    var msg: String = ""

    private val viewModel: ChattingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChattingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        recyclerView = binding.chattingRV

        // Call permission check here after the view is created
        checkPermissions()

        // Initialize the progress dialog
        initializingDialog()
        // Getting data from the intent
        fetchDataFromIntent()

        // Initialize ViewModel with sender and receiver IDs
        viewModel.initializeRooms(senderId, receiverUID)

        binding.username.setOnClickListener {
            gotoProfile()
        }

        binding.userProfile.setOnClickListener {
            gotoProfile()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSelectImage.setOnClickListener {
            onImageBtnClick()
        }

        binding.btnSend.setOnClickListener {
            onSendBtnClick()
        }

        // Setup RecyclerView
        setUpRecyclerView()

        // Start observing LiveData from the ViewModel
        observeDisplayChatViewModels()
        observeSendChatViewModel()

        // Ask user to accept notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withContext(applicationContext)
                .withPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {}

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {}

                    override fun onPermissionRationaleShouldBeShown(
                        p0: PermissionRequest?,
                        p1: PermissionToken?
                    ) {
                        p1?.continuePermissionRequest()
                    }
                }).check()
        }

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    private fun gotoProfile() {
        val intent = Intent(this, OtherUserProfile::class.java)
        intent.apply {
            putExtra(Profile.Name, username)
            putExtra(Profile.profilePic, profilePic)
            putExtra(Profile.userId, receiverUID)
            putExtra(Profile.About, about)
            putExtra(Profile.Email, email)
        }
        startActivity(intent)
    }

    private fun onSendBtnClick() {
        msg = binding.messageBox.text.toString()

        if (msg.isNotBlank() || imageUrl != null) {
            if (imageUrl != null) {
                progressDialog.show()
            }
            viewModel.sendMessage(senderId, receiverUID, msg, imageUrl)
            imageUrl = null
            binding.messageBox.text.clear()

            sendNotification(msg, token)
        } else {
            Toast.makeText(this, "Cannot send an empty message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withContext(applicationContext)
                .withPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {}
                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {}
                    override fun onPermissionRationaleShouldBeShown(
                        request: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                }).check()
        }
    }

    private fun sendNotification(msg : String,token : String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch sender name from Firestore
                val document = FirebaseFirestore.getInstance().collection("Users")
                    .document(currentUser.uid)
                    .get()
                    .await()  // Await to fetch data
                val senderName = document.getString("userName") ?: "Unknown"

                // Create notification data
                val notificationData = NotificationData(
                    token = token,
                    data = hashMapOf(
                        "title" to senderName,
                        "body" to msg
                    )
                )
                val notification = Notification(message = notificationData)

                // Get access token
                val accessToken = AccessToken.getAccessToken() ?: return@launch

                // Send notification using the notification API
                val notificationInterface = NotificationApi.create()
                val response = notificationInterface.sendNotification(notification, "Bearer $accessToken")

                // Handle response
                withContext(Dispatchers.Main) {
//                    if (response) {
//                        Toast.makeText(this@Chatting, "Notification sent", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(this@Chatting, "Failed to send notification", Toast.LENGTH_SHORT).show()
//                    }
                }
            } catch (e: Exception) {
                // Handle any exceptions and show error message on the UI thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Chatting, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun fetchDataFromIntent() {
        username = intent.getStringExtra(Profile.Name) ?: ""
        token = intent.getStringExtra("token") ?: ""
        profilePic = intent.getStringExtra(Profile.profilePic) ?: ""
        receiverUID = intent.getStringExtra(Profile.userId) ?: ""
        about = intent.getStringExtra(Profile.About) ?: ""
        email = intent.getStringExtra(Profile.Email) ?: ""
        senderId = auth.currentUser?.uid ?: ""

        binding.username.text = username
        Glide.with(this).load(profilePic)
            .placeholder(R.drawable.profile_placeholder)
            .into(binding.userProfile)
    }

    private fun setUpRecyclerView() {
        adapter = ChattingAdapter { imageUrl ->
            val intent = Intent(this, DisplayAttachment::class.java)
            intent.putExtra("imageUrl", imageUrl)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter
        recyclerView.showShimmerAdapter()
    }

    private fun observeSendChatViewModel() {
        viewModel.msgSend.observe(this, Observer {
            binding.messageBox.text.clear()
            recyclerView.hideShimmerAdapter()
            progressDialog.dismiss()
        })

        viewModel.error.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            recyclerView.hideShimmerAdapter()
            progressDialog.dismiss()
        })
    }

    private fun observeDisplayChatViewModels() {
        viewModel.messageList.observe(this, Observer { messages ->
            if (messages.isNullOrEmpty()) {
                binding.emptyList.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                binding.emptyList.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.updateList(messages as ArrayList<message>)
            }
            recyclerView.hideShimmerAdapter()
        })

        viewModel.error.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            recyclerView.hideShimmerAdapter()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 45 && resultCode == RESULT_OK && data != null) {
            imageUrl = data.data
            progressDialog.show()
            viewModel.sendMessage(senderId, receiverUID, msg, imageUrl)
            sendNotification("Sent a photo", token)
            imageUrl = null
        }
    }

    private fun onImageBtnClick() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 45)
    }

    private fun initializingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sending_attachment, null)
        val builder = AlertDialog.Builder(this).apply {
            setView(dialogView)
            setCancelable(false)
        }
        progressDialog = builder.create()
    }
}
