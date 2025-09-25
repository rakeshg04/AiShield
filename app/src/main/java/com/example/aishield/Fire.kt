package com.example.aishield

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class Fire : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fire)

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Firebase reference
        database = FirebaseDatabase.getInstance().getReference("FireComplaints")

        // Views
        etTitle = findViewById(R.id.etFireComplaintTitle)
        etDescription = findViewById(R.id.etFireComplaintDescription)
        etLocation = findViewById(R.id.etFireComplaintLocation)
        val btnSubmit = findViewById<Button>(R.id.btnFireSubmit)

        // Add voice buttons (optional)
        val btnVoiceTitle = ImageButton(this).apply { setImageResource(android.R.drawable.ic_btn_speak_now) }
        val btnVoiceDesc = ImageButton(this).apply { setImageResource(android.R.drawable.ic_btn_speak_now) }
        val btnVoiceLoc = ImageButton(this).apply { setImageResource(android.R.drawable.ic_btn_speak_now) }

        // Request microphone permission
        val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Toast.makeText(this, "Microphone permission needed", Toast.LENGTH_SHORT).show()
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                speak("You can speak your fire complaint details now.")
            }
        }

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // Voice button listeners
        btnVoiceTitle.setOnClickListener { startListening(etTitle) }
        btnVoiceDesc.setOnClickListener { startListening(etDescription) }
        btnVoiceLoc.setOnClickListener { startListening(etLocation) }

        // Submit button listener
        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDescription.text.toString().trim()
            val loc = etLocation.text.toString().trim()

            if (title.isNotEmpty() && desc.isNotEmpty() && loc.isNotEmpty()) {
                val complaintId = database.push().key!!
                val complaint = FireComplaint(title, desc, loc)
                database.child(complaintId).setValue(complaint)
                    .addOnSuccessListener {
                        speak("Complaint submitted successfully.")
                        Toast.makeText(this, "Fire Complaint Submitted!", Toast.LENGTH_SHORT).show()
                        etTitle.text.clear(); etDescription.text.clear(); etLocation.text.clear()
                    }
                    .addOnFailureListener {
                        speak("Failed to submit complaint.")
                        Toast.makeText(this, "Submission failed!", Toast.LENGTH_SHORT).show()
                    }
            } else {
                speak("Please fill all fields before submitting.")
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Start voice recognition and insert into EditText */
    private fun startListening(targetEditText: EditText) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) targetEditText.append(" ${matches[0]}")
            }
            override fun onReadyForSpeech(params: Bundle?) { speak("Listening") }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { speak("Sorry, please try again.") }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
        speechRecognizer.destroy()
    }
}
