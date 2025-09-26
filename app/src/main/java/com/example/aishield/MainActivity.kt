package com.example.aishield

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.aishield.databinding.ActivityMainBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference

    private var storedVerificationId: String? = null
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    private var tempName = ""
    private var tempEmail = ""
    private var tempPhone = ""
    private var tempAddress = ""
    private var tempPassword = ""

    // ✅ ESP32 service (initialize later)
    private lateinit var espService: Esp32BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // ✅ Signup button
        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
            val address = binding.etAddress.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                // store temporarily for later use
                tempName = name
                tempEmail = email
                tempPhone = phone
                tempAddress = address
                tempPassword = password

                sendOtpToPhone(phone)
            }
        }

        // ✅ Go to Login
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        // ✅ ESP32 integration setup
        espService = Esp32BluetoothService(this, "+917996799399") // replace with real number

        // Ask runtime permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.SEND_SMS
            ),
            1
        )

        // Hook up ESP32 connect button
        val btnEsp32 = findViewById<Button>(R.id.btnConnectEsp32)
        btnEsp32.setOnClickListener {
            if (espService.connectToDevice("ESP32_BT_NAME")) { // replace with your ESP32 Bluetooth name
                espService.startListening()
                Toast.makeText(this, "ESP32 connected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "ESP32 connection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtpToPhone(phone: String) {
        val phoneNumber = if (phone.startsWith("+")) phone else "+91$phone"

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-verification
                    verifyWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@MainActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    super.onCodeSent(verificationId, token)
                    storedVerificationId = verificationId
                    resendToken = token

                    // Ask user for OTP
                    showOtpDialog()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showOtpDialog() {
        val otpInput = EditText(this)
        otpInput.hint = "Enter OTP"

        AlertDialog.Builder(this)
            .setTitle("Phone Verification")
            .setMessage("Enter the OTP sent to your phone")
            .setView(otpInput)
            .setPositiveButton("Verify") { _, _ ->
                val otp = otpInput.text.toString().trim()

                // ✅ Auto-fill OTP for test number
                if (tempPhone == "+917996799399") { // Replace with your Firebase test number
                    if (storedVerificationId != null) {
                        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, "001607") // OTP you set in Firebase
                        verifyWithCredential(credential)
                    }
                } else if (storedVerificationId != null && otp.isNotEmpty()) {
                    val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, otp)
                    verifyWithCredential(credential)
                } else {
                    Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    // ✅ Link Email/Password with phone-authenticated user
                    val emailCredential = EmailAuthProvider.getCredential(tempEmail, tempPassword)

                    firebaseUser?.linkWithCredential(emailCredential)
                        ?.addOnCompleteListener { linkTask ->
                            if (linkTask.isSuccessful) {
                                val userId = firebaseUser.uid
                                val user = User(tempName, tempEmail, tempPhone, tempAddress)

                                // Save in Realtime DB
                                database.child("users").child(userId).setValue(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, Applying::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to store user data", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Account linking failed: ${linkTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }

                } else {
                    Toast.makeText(this, "OTP Verification Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
