package com.vivekupasani.single.ui.fragment

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
import com.vivekupasani.single.ui.Activity.ViewStatus
import com.vivekupasani.single.viewModels.StatusViewModel

class Status : Fragment() {

    private lateinit var binding: FragmentStatusBinding
    private lateinit var imageUri: Uri
    private lateinit var progressDialog: AlertDialog
    private lateinit var adapter: StatusAdapter
    private val viewModel: StatusViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        setupRecyclerView()
        initializingDialog()

        // Set up swipe-to-refresh functionality
        binding.swiperefresh.setOnRefreshListener {
            viewModel.displayStatus() // Fetch updated statuses
        }

        // Observe ViewModel
        observeViewModel()

        // Set up click listeners
        onAddStatusClick()
        onStatusClick()

        // Show shimmer effect while loading
        binding.statusRecyclerView.showShimmerAdapter()
    }

    private fun onAddStatusClick() {
        binding.btnAddStatus.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 45)
        }
    }

    private fun onStatusClick() {
        adapter.onStatusClick = { currentStatus ->
            val intent = Intent(requireContext(), ViewStatus::class.java).apply {
                putExtra("uName", currentStatus.userName)
                putExtra("uID", currentStatus.userId)
                putExtra("uProfile", currentStatus.profilePicURL)
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.statusList.observe(viewLifecycleOwner, Observer { statusList ->
            binding.statusRecyclerView.hideShimmerAdapter()
            binding.swiperefresh.isRefreshing = false

            if (statusList.isNullOrEmpty()) {
                binding.statusRecyclerView.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
            } else {
                binding.statusRecyclerView.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE
                adapter.updateStatus(statusList)
            }
        })

        viewModel.uploaded.observe(viewLifecycleOwner, Observer { uploaded ->
            if (uploaded) {
                Toast.makeText(requireContext(), "uploaded", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()

            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            progressDialog.dismiss()
            binding.swiperefresh.isRefreshing = false
            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
        })
    }

    private fun setupRecyclerView() {
        adapter = StatusAdapter()
        binding.statusRecyclerView.layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
        binding.statusRecyclerView.adapter = adapter
    }

    private fun initializingDialog() {
        progressDialog = AlertDialog.Builder(requireContext())
            .setView(LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null))
            .setCancelable(false)
            .create()
        progressDialog.window?.setBackgroundDrawableResource(R.drawable.dialouge_box_background)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 45 && resultCode == -1 && data != null) {
            imageUri = data.data ?: return // Use safe call to avoid NullPointerException
            progressDialog.show() // Show the progress dialog when the image is selected
            viewModel.uploadStatus(imageUri)
        }
    }
}
