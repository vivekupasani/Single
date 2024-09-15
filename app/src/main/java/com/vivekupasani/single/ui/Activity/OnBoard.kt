package com.vivekupasani.single.ui.Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.vivekupasani.single.databinding.ActivityOnboardBinding
import com.vivekupasani.single.ui.Activity.signup.getEmail

class OnBoard : AppCompatActivity() {

    lateinit var binding: ActivityOnboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ahead.setOnClickListener {
            startActivity(Intent(this, getEmail::class.java))
        }

    }
}