package com.example.aishield

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Hospital : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hospital)

        // Firebase reference
        database = FirebaseDatabase.getInstance().getReference("HospitalComplaints")

        // UI elements
        val etTitle = findViewById<EditText>(R.id.etComplaintTitle)
        val etDescription = findViewById<EditText>(R.id.etComplaintDescription)
        val etLocation = findViewById<EditText>(R.id.etComplaintLocation)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val location = etLocation.text.toString().trim()

            if (title.isNotEmpty() && description.isNotEmpty() && location.isNotEmpty()) {
                val complaintId = database.push().key ?: return@setOnClickListener
                val complaint = HospitalComplaint(title, description, location)

                database.child(complaintId).setValue(complaint)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Complaint Submitted", Toast.LENGTH_SHORT).show()
                        etTitle.text.clear()
                        etDescription.text.clear()
                        etLocation.text.clear()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "⚠️ Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
