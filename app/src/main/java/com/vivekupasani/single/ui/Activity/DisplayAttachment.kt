package com.vivekupasani.single.ui.Activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.vivekupasani.single.R
import com.vivekupasani.single.databinding.ActivityDisplayAttachmentBinding

class DisplayAttachment : AppCompatActivity() {

    lateinit var binding : ActivityDisplayAttachmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDisplayAttachmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra("imageUrl")

        Glide.with(this).load(imageUrl).into(binding.attachmentImageView)

        binding.btnBack.setOnClickListener {
            finish()
        }

    }
}