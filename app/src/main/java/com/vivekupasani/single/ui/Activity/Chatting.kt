package com.vivekupasani.single.ui.Activity

import ChattingAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.mikelau.shimmerrecyclerviewx.ShimmerRecyclerViewX
import com.vivekupasani.single.R
import com.vivekupasani.single.databinding.ActivityChattingBinding
import com.vivekupasani.single.models.message
import com.vivekupasani.single.ui.fragment.Profile
import com.vivekupasani.single.viewModels.ChattingViewModel

@Suppress("DEPRECATION")
class Chatting : AppCompatActivity() {

    private lateinit var binding: ActivityChattingBinding
    private var imageUrl: Uri? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: ChattingAdapter
    private lateinit var recyclerView: ShimmerRecyclerViewX
    private lateinit var progressDialog: AlertDialog

    var username: String = ""
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

        // Initialize the progress dialog
        initializingDialog()
        // Getting data from the intent
        fechDataFromIntent()
        //onAttachmentClick handle method
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
           onsendBtnClick()
        }

        // Setup RecyclerView
        setUpRecyclerView()

        // Start observing LiveData from the ViewModel
        observeDisplayChatViewModels()
        observeSendChatViewModel()
    }

    private fun gotoProfile() {
        val intent = Intent(this,OtherUserProfile::class.java)
        intent.apply {
            putExtra(Profile.Name, username)
            putExtra(Profile.profilePic, profilePic)
            putExtra(Profile.userId, receiverUID)
            putExtra(Profile.About, about)
            putExtra(Profile.Email, email)
        }
        startActivity(intent)
    }

    private fun onsendBtnClick() {
        // Getting the latest message input
        msg = binding.messageBox.text.toString()
        // Check if both message and image are empty
        if (msg.isNotBlank() || imageUrl != null) {
            // Show progress dialog when sending an image
            if (imageUrl != null) {
                progressDialog.show() // Only show dialog for image
            }
            // Send the message or image
            viewModel.sendMessage(senderId, receiverUID, msg, imageUrl)
            imageUrl = null // Clear image URL after sending the message
            binding.messageBox.text.clear()
        } else {
            Toast.makeText(this, "Cannot send an empty message", Toast.LENGTH_SHORT).show()
        }
    }


    private fun fechDataFromIntent() {
        username = intent.getStringExtra(Profile.Name) ?: ""
        profilePic = intent.getStringExtra(Profile.profilePic) ?: ""
        receiverUID = intent.getStringExtra(Profile.userId) ?: ""
        about = intent.getStringExtra(Profile.About) ?: ""
        email = intent.getStringExtra(Profile.Email) ?: ""
        senderId = auth.currentUser?.uid ?: ""

        // Setting the user's profile and name in the chat screen
        binding.username.text = username
        Glide.with(this).load(profilePic).into(binding.userProfile)
    }

    private fun setUpRecyclerView() {
        adapter = ChattingAdapter { imageUrl ->
            // Start DisplayAttachment activity and pass the image URL
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
        // Observing message sent status and errors
        viewModel.msgSend.observe(this, Observer {
            binding.messageBox.text.clear() // Clear message box after successful send
            recyclerView.hideShimmerAdapter()
            progressDialog.dismiss() // Hide progress dialog
        })

        viewModel.error.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show() // Display errors
            recyclerView.hideShimmerAdapter()
            progressDialog.dismiss() // Hide progress dialog
        })
    }

    private fun observeDisplayChatViewModels() {
        // Observing message list updates
        viewModel.messageList.observe(this, Observer { messages ->
            if (messages.isNullOrEmpty()) {
                // Show empty chat view if there are no messages
                binding.emptyList.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                // Show the chat and hide the empty chat view if there are messages
                binding.emptyList.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.updateList(messages as ArrayList<message>) // Update RecyclerView with new messages
            }
            recyclerView.hideShimmerAdapter()
        })

        viewModel.error.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show() // Display errors
            recyclerView.hideShimmerAdapter()
        })
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 45 && resultCode == RESULT_OK && data != null) {
            imageUrl = data.data
            // Show progress dialog when sending the image
            progressDialog.show()
            viewModel.sendMessage(senderId, receiverUID, msg, imageUrl)
            imageUrl = null // Clear image URL after sending the message
        }
    }

    private fun onImageBtnClick() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 45)
    }

    private fun initializingDialog() {
        // Initialize the progress dialog
        progressDialog = AlertDialog.Builder(this)
            .setView(LayoutInflater.from(this).inflate(R.layout.dialog_sending_attachment, null))
            .setCancelable(false)
            .create()
        progressDialog.window!!.setBackgroundDrawableResource(R.drawable.dialouge_box_background)
    }
}
