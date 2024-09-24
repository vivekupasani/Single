package com.vivekupasani.single.ui.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mikelau.shimmerrecyclerviewx.ShimmerRecyclerViewX
import com.vivekupasani.single.R
import com.vivekupasani.single.adapters.NotificationAdapter
import com.vivekupasani.single.databinding.ActivityNotificationBinding
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.notification.NotificationApi
import com.vivekupasani.single.notification.models.Notification
import com.vivekupasani.single.notification.models.NotificationData
import com.vivekupasani.single.viewModels.Friends
import com.vivekupasani.single.viewModels.NotificationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Notification : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var recyclerViewX: ShimmerRecyclerViewX
    private lateinit var adapter: NotificationAdapter
    private lateinit var refreshLayout: SwipeRefreshLayout

    private val viewModel: Friends by viewModels()
    private val notiViewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refreshLayout = binding.swiperefresh
        refreshLayout.setOnRefreshListener(this)

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        recyclerViewX.showShimmerAdapter()
        observeViewModel()
        observeFriendsViewModel()
        onAcceptFriendBtnClick()
    }

    private fun observeViewModel() {
        notiViewModel.userList.observe(this, Observer { userList ->
            Log.d("NotificationActivity", "UserList updated: $userList")

            if (userList.isNullOrEmpty()) {
                // Hide RecyclerView and show Empty TextView
                binding.recyclerView.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
            } else {
                // Show RecyclerView and hide Empty TextView
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE
                adapter.updateList(userList)
            }

            // Hide shimmer effect and stop refresh indicator
            recyclerViewX.hideShimmerAdapter()
            refreshLayout.isRefreshing = false
        })

        notiViewModel.errorMessage.observe(this, Observer { errorMessage ->
            Log.e("NotificationActivity", "Error: $errorMessage")
            binding.emptyList.visibility = View.VISIBLE
            recyclerViewX.hideShimmerAdapter()
            refreshLayout.isRefreshing = false
        })
    }


    private fun onAcceptFriendBtnClick() {
        adapter.onAcceptBtnClick = { user ->
            sendNotification(user)
            viewModel.acceptRequest(user.userId)
        }
    }

    private fun sendNotification(selectedUser: Users) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .get()
                    .await() // Make sure you have the necessary import

                val senderName = document.getString("userName") ?: "Unknown"
                val notificationData = NotificationData(
                    token = selectedUser.token,
                    data = hashMapOf(
                        "title" to senderName,
                        "body" to "Accepted friend request"
                    )
                )
                val notification = Notification(message = notificationData)

                val accessToken = AccessToken.getAccessToken() ?: return@launch
                // Call the sendNotification function, which should be a suspend function now
                val response = NotificationApi.create().sendNotification(notification,"Bearer $accessToken")

                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Toast.makeText(this@Notification, "Notification sent", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(this@Notification, "Failed to send notification", Toast.LENGTH_SHORT).show()
//                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Notification, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun observeFriendsViewModel() {
        viewModel.acceptedRequest.observe(this, Observer {
            Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show()
            recyclerViewX.hideShimmerAdapter()
            refreshLayout.isRefreshing = false
        })

        viewModel.error.observe(this, Observer { error ->
            Log.e("NotificationActivity", "Error: $error")
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            recyclerViewX.hideShimmerAdapter()
            refreshLayout.isRefreshing = false
        })
    }

    private fun setupRecyclerView() {
        recyclerViewX = binding.recyclerView
        recyclerViewX.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(arrayListOf())
        recyclerViewX.adapter = adapter
    }

    override fun onRefresh() {
        recyclerViewX.showShimmerAdapter()
        notiViewModel.getNotifications()
    }
}
