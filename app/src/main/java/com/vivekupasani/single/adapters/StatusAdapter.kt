package com.vivekupasani.single.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vivekupasani.single.R
import com.vivekupasani.single.databinding.EachStatusOfUserBinding
import com.vivekupasani.single.models.status

class StatusAdapter : RecyclerView.Adapter<StatusAdapter.ViewHolder>() {

    private var statusList: ArrayList<status> = arrayListOf()
    var onStatusClick: ((status) -> Unit)? = null

    inner class ViewHolder(val binding: EachStatusOfUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(currentStatus: status) {
            // Load the profile picture
            Glide.with(itemView.context)
                .load(currentStatus.profilePicURL)
                .placeholder(R.drawable.profile)
                .into(binding.profileImage)

            // Load the status image (assuming it's a URL)
            if (currentStatus.status.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(currentStatus.status)
                    .placeholder(R.drawable.profile_placeholder) // Optional placeholder
                    .into(binding.mainStatus)
            } else {
                // Handle case where there is no status image
                binding.mainStatus.setImageResource(R.drawable.profile) // Optional placeholder
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = EachStatusOfUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return statusList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentStatus = statusList[position]
        holder.bind(currentStatus)

        holder.itemView.setOnClickListener {
            onStatusClick?.invoke(currentStatus)
        }
    }

    fun updateStatus(status: ArrayList<status>) {
        statusList.clear()
        statusList.addAll(status)
        notifyDataSetChanged()
    }
}
