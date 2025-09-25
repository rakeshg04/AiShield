package com.example.aishield

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.widget.Toast
import java.util.*

class VoiceAssistantHelper(private val activity: Activity) {

    private val VOICE_REQUEST_CODE = 3001
    private val SOS_DELAY_MS: Long = 3000 // 3 seconds

    fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        try {
            activity.startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(activity, "Voice assistant not supported", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleVoiceResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VOICE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0)?.lowercase(Locale.getDefault()) ?: ""

            val appActivity = activity as? Applying ?: return

            // SOS command
            if ("i need help" in spokenText) {
                if (appActivity.emergencyNumber.isNullOrEmpty()) {
                    appActivity.showMessage("No emergency number found")
                } else {
                    appActivity.showMessage("Detected 'I need help'. Sending SOS in 3 seconds...")
                    Handler(Looper.getMainLooper()).postDelayed({
                        appActivity.sendSOSMessage(appActivity.emergencyNumber!!)
                        // Open camera after SOS
                        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                        appActivity.startActivity(intent)
                    }, SOS_DELAY_MS)
                }
                return
            }

            // Complaint Box
            if (spokenText.contains("complaint")) {
                appActivity.showMessage("Opening Complaint Box...")
                appActivity.startActivity(Intent(appActivity, Complaint::class.java))
                return
            }

            // Keywords for nearest places
            val keywordMap = mapOf(
                "police" to "police station near me",
                "hospital" to "hospital near me",
                "school" to "school near me",
                "college" to "college near me",
                "hotel" to "hotel near me",
                "fire station" to "fire station near me",
                "bus stand" to "bus stand near me",
                "railway" to "railway station near me",
                "airport" to "airport near me",
                "government office" to "government office near me",
                "post office" to "post office near me",
                "court" to "court near me",
                "temple" to "temple near me",
                "church" to "church near me",
                "mosque" to "mosque near me",
                "atm" to "ATM near me",
                "bank" to "bank near me"
            )

            for ((keyword, query) in keywordMap) {
                if (spokenText.contains(keyword)) {
                    appActivity.showMessage("Opening nearest $keyword...")
                    appActivity.openMaps(query)
                    return
                }
            }

            // Close command
            if ("close" in spokenText) {
                appActivity.showMessage("Closing Voice Assistant")
                return
            }

            // Default
            appActivity.showMessage("You said: $spokenText")
        }
    }
}

