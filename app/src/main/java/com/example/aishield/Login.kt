package com.example.aishield

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.aishield.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    // ✅ ESP32 Bluetooth Service
    private lateinit var espService: Esp32BluetoothService
    private val esp32Name = "ESP32_BT_NAME"   // replace with your ESP32 Bluetooth name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        espService = Esp32BluetoothService(this, "+917996799399") // Replace with test phone if needed

        // Request Bluetooth permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ),
            2
        )

        // ✅ Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                            // ✅ Show Connect Button after login
                            setupBluetoothConnectButton()

                            // ✅ Auto-connect to ESP32
                            autoConnectToEsp32()

                            // Navigate to Applying screen (or keep here)
                            startActivity(Intent(this, Applying::class.java))
                            finish()

                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // ✅ Signup redirection
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupBluetoothConnectButton() {
        val btnConnect = findViewById<Button>(R.id.btnConnectEsp32Login)
        btnConnect?.apply {
            isEnabled = true
            setOnClickListener {
                if (espService.connectToDevice(esp32Name)) {
                    espService.startListening()
                    Toast.makeText(this@Login, "ESP32 connected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@Login, "ESP32 connection failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun autoConnectToEsp32() {
        if (espService.connectToDevice(esp32Name)) {
            espService.startListening()
            Toast.makeText(this, "Auto-connected to ESP32", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Auto-connect failed", Toast.LENGTH_SHORT).show()
        }
    }
}
