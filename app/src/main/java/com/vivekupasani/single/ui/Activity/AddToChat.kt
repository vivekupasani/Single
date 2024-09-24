package com.vivekupasani.single.ui.Activity

import AddToChatViewModel
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mikelau.shimmerrecyclerviewx.ShimmerRecyclerViewX
import com.vivekupasani.single.adapters.AddToChatAdapter
import com.vivekupasani.single.databinding.ActivityAddToChatBinding
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.notification.NotificationApi
import com.vivekupasani.single.notification.models.Notification
import com.vivekupasani.single.notification.models.NotificationData
import com.vivekupasani.single.viewModels.Friends
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddToChat : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: ActivityAddToChatBinding
    private lateinit var adapter: AddToChatAdapter
    private lateinit var recyclerView: ShimmerRecyclerViewX
    private lateinit var refreshLayout: SwipeRefreshLayout

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val viewModel: AddToChatViewModel by viewModels()
    private val friendViewModel: Friends by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddToChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refreshLayout = binding.swiperefresh
        refreshLayout.setOnRefreshListener(this)

        setUpRecyclerView()
        observeViewModel()
        observeFriendViewModel()

        recyclerView.showShimmerAdapter()
        adapter.onRequestBtnClick = { selectedUser ->
            friendViewModel.sendRequest(selectedUser)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Perform your network operation here
                    sendNotification(selectedUser) // Example network call
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        onBackBtnClick()
    }

    private fun sendNotification(selectedUser: Users) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get the sender name from Firestore
                    val document = FirebaseFirestore.getInstance().collection("Users")
                        .document(currentUser.uid)
                        .get()
                        .await() // Wait for the result

                    val senderName = document.getString("userName") ?: "Unknown"

                    // Create notification data
                    val notification = Notification(
                        message = NotificationData(
                            token = selectedUser.token,
                            data = hashMapOf(
                                "title" to senderName,
                                "body" to "sent you a friend request"
                            )
                        )
                    )

                    // Get access token
                    val accessToken = AccessToken.getAccessToken()

                    if (accessToken != null) {
                        // Send notification using suspend function
                        val notificationInterface = NotificationApi.create()
                        val response = notificationInterface.sendNotification(
                            notification, "Bearer $accessToken"
                        )

                        // Handle the response
                        if (response != null) {
                            launch(Dispatchers.Main) {
//                                Toast.makeText(
//                                    this@AddToChat,
//                                    "Notification sent",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                            }
                        } else {
                            launch(Dispatchers.Main) {
//                                Toast.makeText(
//                                    this@AddToChat,
//                                    "Failed to send notification",
//                                    Toast.LENGTH_SHORT
//                                ).show()
                            }
                        }
                    } else {
                        launch(Dispatchers.Main) {
//                            Toast.makeText(
//                                this@AddToChat,
//                                "Failed to get access token",
//                                Toast.LENGTH_SHORT
//                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
//                        Toast.makeText(
//                            this@AddToChat,
//                            "Error: ${e.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }


    private fun observeFriendViewModel() {
        friendViewModel.sendRequest.observe(this, Observer {
//            Toast.makeText(this, "Friend Request Sent", Toast.LENGTH_SHORT).show()
        })
        friendViewModel.error.observe(this, Observer {
//            Toast.makeText(this, "Failed to Send Request: $it", Toast.LENGTH_SHORT).show()
        })
    }

    private fun observeViewModel() {
        viewModel.userList.observe(this, Observer { users ->
            recyclerView.hideShimmerAdapter()

            if (users.isNullOrEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE
                adapter.updateList(users)
            }
        })

        viewModel.errorMessage.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            recyclerView.showShimmerAdapter()
        })
    }

    private fun setUpRecyclerView() {
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AddToChatAdapter(arrayListOf())
        recyclerView.adapter = adapter
    }

    private fun onBackBtnClick() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onRefresh() {
        viewModel.getUsers() // This should trigger the LiveData update and refresh the data
        recyclerView.showShimmerAdapter()
        refreshLayout.isRefreshing = false
    }
}
