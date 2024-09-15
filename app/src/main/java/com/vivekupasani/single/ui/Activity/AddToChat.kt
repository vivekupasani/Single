package com.vivekupasani.single.ui.Activity

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
import com.mikelau.shimmerrecyclerviewx.ShimmerRecyclerViewX
import com.vivekupasani.single.adapters.AddToChatAdapter
import com.vivekupasani.single.databinding.ActivityAddToChatBinding
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.viewModels.AddToChatViewModel
import com.vivekupasani.single.viewModels.Friends

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
        }

        onBackBtnClick()
    }

    private fun observeFriendViewModel() {
        friendViewModel.sendRequest.observe(this, Observer {
            Toast.makeText(this, "Friend Request Sent", Toast.LENGTH_SHORT).show()
        })
        friendViewModel.error.observe(this, Observer {
            Toast.makeText(this, "Failed to Send Request: $it", Toast.LENGTH_SHORT).show()
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
        viewModel.getUsers()
        recyclerView.showShimmerAdapter()
        refreshLayout.isRefreshing = false
    }
}
