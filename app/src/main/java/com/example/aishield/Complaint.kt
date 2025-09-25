package com.example.aishield

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Complaint : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_complaint)

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Police
        findViewById<ImageView>(R.id.imgPolice).setOnClickListener {
            startActivity(Intent(this, Police::class.java))
        }

        // Hospital
        findViewById<ImageView>(R.id.imgHospital).setOnClickListener {
            startActivity(Intent(this, Hospital::class.java))
        }

        // Fire
        findViewById<ImageView>(R.id.imgFire).setOnClickListener {
            startActivity(Intent(this, Fire::class.java))
        }

        // Water
        findViewById<ImageView>(R.id.imgWater).setOnClickListener {
            startActivity(Intent(this, Water::class.java))
        }
        findViewById<ImageView>(R.id.imgBank).setOnClickListener {
            startActivity(Intent(this, Bank::class.java))
        }
        findViewById<ImageView>(R.id.imgChildCare).setOnClickListener {
            startActivity(Intent(this, child::class.java))
        }

    }
}
