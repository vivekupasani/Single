package com.vivekupasani.single.ui.fragment

import StatusViewModel
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikelau.shimmerrecyclerviewx.ShimmerRecyclerViewX
import com.vivekupasani.single.R
import com.vivekupasani.single.adapters.StatusAdapter
import com.vivekupasani.single.databinding.FragmentStatusBinding
import com.vivekupasani.single.models.status
import com.vivekupasani.single.ui.Activity.ViewStatus

class Status : Fragment() {

    private lateinit var binding: FragmentStatusBinding
    private lateinit var imageUri: Uri
    private lateinit var progressDialog: AlertDialog

    private lateinit var recyclerViewX: ShimmerRecyclerViewX
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var adapter: StatusAdapter
    private val viewModel: StatusViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatusBinding.inflate(inflater, container, false)
        recyclerViewX =
            binding.statusRecyclerView // Assuming recyclerView is in FragmentStatusBinding
        refreshLayout = binding.swiperefresh

        // Set up refresh listener
        refreshLayout.setOnRefreshListener {
            viewModel.displayStatus() // Fetch the updated statuses
            recyclerViewX.hideShimmerAdapter()
            refreshLayout.isRefreshing = false
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onAddStatusClick()
        initializingDialog()
        setupRecyclerView()
        observeViewModel()
        onStatusClick()
        recyclerViewX.showShimmerAdapter()
    }

    private fun onAddStatusClick() {
        binding.btnAddStatus.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 45)
        }
    }

    private fun onStatusClick() {
        adapter.onStatusClick = {
            val intent = Intent(requireContext(), ViewStatus::class.java)
            intent.apply {
                putExtra("uName", it.userName)
                putExtra("uID", it.userId)
                putExtra("uProfile", it.profilePicURL)
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.statusList.observe(viewLifecycleOwner, Observer { statusList ->
            recyclerViewX.hideShimmerAdapter()

            // Stop the refreshing animation after the data is updated
            refreshLayout.isRefreshing = false

            if (statusList.isNullOrEmpty()) {
                binding.statusRecyclerView.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
            } else {
                binding.statusRecyclerView.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE

                adapter.updateStatus(statusList as ArrayList<status>)
            }
        })

        // Observe upload status and error
        viewModel.uploaded.observe(viewLifecycleOwner, Observer { uploaded ->
            if (uploaded) {
                Toast.makeText(requireContext(), "Status uploaded successfully", Toast.LENGTH_SHORT)
                    .show()
                progressDialog.dismiss()
                viewModel.displayStatus() // Refresh the status list
            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            progressDialog.dismiss()
            refreshLayout.isRefreshing = false // Stop refreshing if there's an error
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        })
    }

    private fun setupRecyclerView() {
        adapter = StatusAdapter()
        recyclerViewX.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerViewX.showShimmerAdapter() // Show shimmer effect while loading
        recyclerViewX.adapter = adapter
    }

    private fun initializingDialog() {
        // Initialize the progress dialog
        progressDialog = AlertDialog.Builder(requireContext())
            .setView(LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null))
            .setCancelable(false)
            .create()
        progressDialog.window!!.setBackgroundDrawableResource(R.drawable.dialouge_box_background)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 45 && resultCode == -1 && data != null) {
            imageUri = data.data!!
            progressDialog.show() // Show the progress dialog when image is selected
            viewModel.uploadStatus(imageUri)
        }
    }
}
