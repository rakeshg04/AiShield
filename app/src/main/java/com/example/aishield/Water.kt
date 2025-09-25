package com.example.aishield

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class Water : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText

    private val REQ_CODE_TITLE = 101
    private val REQ_CODE_DESC = 102
    private val REQ_CODE_LOC = 103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_water)

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase reference
        database = FirebaseDatabase.getInstance().getReference("WaterComplaints")

        // Find views
        etTitle = findViewById(R.id.etWaterComplaintTitle)
        etDescription = findViewById(R.id.etWaterComplaintDescription)
        etLocation = findViewById(R.id.etWaterComplaintLocation)
        val btnSubmit = findViewById<Button>(R.id.btnWaterSubmit)
        val btnVoiceTitle = findViewById<ImageButton>(R.id.btnVoiceTitle)
        val btnVoiceDesc = findViewById<ImageButton>(R.id.btnVoiceDesc)
        val btnVoiceLoc = findViewById<ImageButton>(R.id.btnVoiceLoc)
        val imgLogo = findViewById<ImageView>(R.id.imgWater)

        // Voice input buttons
        btnVoiceTitle.setOnClickListener { startVoiceInput(REQ_CODE_TITLE) }
        btnVoiceDesc.setOnClickListener { startVoiceInput(REQ_CODE_DESC) }
        btnVoiceLoc.setOnClickListener { startVoiceInput(REQ_CODE_LOC) }

        // Submit button listener
        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val location = etLocation.text.toString().trim()

            if (title.isNotEmpty() && description.isNotEmpty() && location.isNotEmpty()) {
                val complaintId = database.push().key!!
                val complaint = WaterComplaint(title, description, location)

                database.child(complaintId).setValue(complaint)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Water Complaint Submitted Successfully!", Toast.LENGTH_SHORT).show()
                        etTitle.text.clear()
                        etDescription.text.clear()
                        etLocation.text.clear()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to submit complaint!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startVoiceInput(requestCode: Int) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        try {
            startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                when (requestCode) {
                    REQ_CODE_TITLE -> etTitle.setText(result[0])
                    REQ_CODE_DESC -> etDescription.setText(result[0])
                    REQ_CODE_LOC -> etLocation.setText(result[0])
                }
            }
        }
    }
}
