package com.vivekupasani.single.ui.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vivekupasani.single.R
import com.vivekupasani.single.databinding.FragmentProfileBinding
import com.vivekupasani.single.models.Users
import com.vivekupasani.single.ui.Activity.EditProfile
import com.vivekupasani.single.viewModels.ProfileViewModel


class Profile : Fragment() {

    lateinit var binding: FragmentProfileBinding
    lateinit var firestore: FirebaseFirestore
    lateinit var auth: FirebaseAuth

    lateinit var profilePicURL: String
    lateinit var Userpassword: String

    private val viewModel: ProfileViewModel by viewModels()

    companion object {
        const val Name: String = "name/vivekupasani/single"
        const val About: String = "about/vivekupasani/single"
        const val Email: String = "email/vivekupasani/single"
        const val password: String = "password/vivekupasani/single"
        const val userId: String = "userId/vivekupasani/single"
        const val profilePic: String = "profilePic/vivekupasani/single"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        isLoding(true)
        setUpObservers()
        viewModel.fetchUserData()

        binding.btnEditProfile.setOnClickListener {
            onEditBtnClick()
        }
    }

    private fun setUpObservers() {
        viewModel.user.observe(viewLifecycleOwner, Observer { user ->
            if (user != null) {
                isLoding(false)
                Glide.with(this)
                    .load(user.profilePicURL)
                    .placeholder(R.drawable.profile_placeholder)
                    .into(binding.profile)
                binding.emailAddress.setText(user.email)
                binding.fullName.setText(user.userName)
                binding.about.setText(user.about)
                profilePicURL = user.profilePicURL.toString()
                Userpassword = user.password

            }
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            isLoding(true)
            it?.let {
                isLoding(false)
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isLoding(isLoding: Boolean) {
        if (isLoding) {
            binding.dataLoding.visibility = View.VISIBLE
            binding.dataLoding.startShimmerAnimation()
            binding.dataLoaded.visibility = View.INVISIBLE
        } else {
            binding.dataLoding.visibility = View.INVISIBLE
            binding.dataLoding.stopShimmerAnimation()
            binding.dataLoaded.visibility = View.VISIBLE
        }
    }

    private fun onEditBtnClick() {
        val intent = Intent(requireContext(), EditProfile::class.java).apply {
            putExtra("title", "Edit Profile")
            putExtra(Profile.Name, binding.fullName.text.toString())
            putExtra(Profile.About, binding.about.text.toString())
            putExtra(Profile.Email, binding.emailAddress.text.toString())
            putExtra(Profile.password, Userpassword)
            putExtra(Profile.profilePic, profilePicURL)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }


}