package com.vivekupasani.single.ui.fragment

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.mikelau.shimmerrecyclerviewx.ShimmerRecyclerViewX
import com.vivekupasani.single.R
import com.vivekupasani.single.adapters.ChatHomeAdapter
import com.vivekupasani.single.databinding.FragmentChatsBinding
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.ui.Activity.AddToChat
import com.vivekupasani.single.ui.Activity.Chatting
import com.vivekupasani.single.ui.Activity.Notification
import com.vivekupasani.single.ui.Activity.OnBoard
import com.vivekupasani.single.viewModels.ChatsHomeViewModel

class Chats : Fragment() {

    lateinit var binding: FragmentChatsBinding
    lateinit var auth: FirebaseAuth

    private val viewModel: ChatsHomeViewModel by viewModels()

    private lateinit var signOutDialog: Dialog
    lateinit var recyclerViewX: ShimmerRecyclerViewX
    lateinit var adapter: ChatHomeAdapter
    lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed the permission check from onCreate
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentChatsBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        //Dialog box of SignOut
        signOutDialog = Dialog(requireContext())
        signOutDialog.setContentView(R.layout.dialouge_box)
        signOutDialog.window!!.setBackgroundDrawableResource(R.drawable.dialouge_box_background)

        //refreshing a recyclerview
        refreshLayout = binding.swiperefresh
        refreshLayout.setOnRefreshListener {
            viewModel.getUsers()
            recyclerViewX.hideShimmerAdapter()
            refreshLayout.isRefreshing = false
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.btnSignOut.setOnClickListener {
            onSignOutBtnClick()
        }
        binding.btnNotification.setOnClickListener {
            onNotificationBtnClick()
        }
        onAddToChatBtnClick()
        setuprecyclerView()
        observeViewModel()
        onUserClick()
        recyclerViewX.showShimmerAdapter()



    }

    private fun onNotificationBtnClick() {
        startActivity(Intent(context, Notification::class.java))
    }

    private fun onUserClick() {
        adapter.onUserClick = { user ->
            val intent = Intent(requireContext(), Chatting::class.java)
            intent.apply {
                putExtra(Profile.Name, user.userName)
                putExtra("token", user.token)
                putExtra(Profile.profilePic, user.profilePicURL)
                putExtra(Profile.userId, user.userId)
                putExtra(Profile.About, user.about)
                putExtra(Profile.Email, user.email)
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.userList.observe(viewLifecycleOwner, Observer { userList ->
            recyclerViewX.hideShimmerAdapter()

            if (userList.isNullOrEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE

                adapter.updateUser(userList as ArrayList<Users>)
                recyclerViewX.hideShimmerAdapter()
            }

        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            recyclerViewX.showShimmerAdapter()
        })
    }



    private fun setuprecyclerView() {
        recyclerViewX = binding.recyclerView
        adapter = ChatHomeAdapter(arrayListOf())
        recyclerViewX.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewX.adapter = adapter
    }

    private fun onSignOutBtnClick() {
        val btnLogout = signOutDialog.findViewById<Button>(R.id.button_logout)
        val btnCancel = signOutDialog.findViewById<Button>(R.id.button_cancel)
        signOutDialog.show()
        signOutDialog.setCancelable(false)

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), OnBoard::class.java))
            activity?.finishAffinity()
        }
        btnCancel.setOnClickListener {
            signOutDialog.dismiss()
        }
    }

    private fun onAddToChatBtnClick() {
        binding.btnAddToChats.setOnClickListener {
            startActivity(Intent(requireContext(), AddToChat::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

}
