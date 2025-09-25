package com.example.aishield

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Applying : AppCompatActivity() {

    private lateinit var btnSOS: Button
    private lateinit var btnComplaintBox: Button
    private lateinit var searchBar: SearchView
    private lateinit var btnVoiceAssistant: Button
    private lateinit var btnWeaponDetector: Button
    private lateinit var btnAIChatBot: Button

    private lateinit var btnPolice: LinearLayout
    private lateinit var btnHospital: LinearLayout
    private lateinit var btnSchool: LinearLayout
    private lateinit var btnHotel: LinearLayout

    var emergencyNumber: String? = null
    private val LOCATION_PERMISSION_CODE = 1002
    private val CAMERA_REQUEST_CODE = 2001
    private val SOS_DELAY_MS: Long = 3000
    private var imagesCaptured = 0
    private val TOTAL_IMAGES = 2

    lateinit var fusedLocationClient: FusedLocationProviderClient
    var currentLocation: Location? = null
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applying)

        // Initialize UI
        btnSOS = findViewById(R.id.btnSOS)
        btnComplaintBox = findViewById(R.id.btnComplaintBox)
        searchBar = findViewById(R.id.searchBar)
        btnVoiceAssistant = findViewById(R.id.btnVoiceAssistant)
        btnWeaponDetector = findViewById(R.id.btnweapondetector)
        btnAIChatBot = findViewById(R.id.btnaichatbot)

        btnPolice = findViewById(R.id.btnPolice)
        btnHospital = findViewById(R.id.btnHospital)
        btnSchool = findViewById(R.id.btnSchool)
        btnHotel = findViewById(R.id.btnHotel)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermission()
        requestMicrophonePermission()
        fetchEmergencyNumber()

        btnSOS.setOnClickListener {
            if (emergencyNumber.isNullOrEmpty()) {
                showMessage("No emergency number found")
            } else {
                showMessage("Sending SOS in 3 seconds...")
                Handler(Looper.getMainLooper()).postDelayed({
                    sendSOSMessage(emergencyNumber!!)
                    openCameraAndCaptureMultipleImages()
                }, SOS_DELAY_MS)
            }
        }

        btnWeaponDetector.setOnClickListener {
            startActivity(Intent(this, weapondetector::class.java))
        }

        btnAIChatBot.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        btnComplaintBox.setOnClickListener {
            startActivity(Intent(this, Complaint::class.java))
        }

        btnVoiceAssistant.setOnClickListener {
            VoiceAssistantHelper(this).startVoiceRecognition()
        }

        btnPolice.setOnClickListener { openMaps("police station near me") }
        btnHospital.setOnClickListener { openMaps("hospital near me") }
        btnSchool.setOnClickListener { openMaps("school near me") }
        btnHotel.setOnClickListener { openMaps("hotel near me") }

        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) openMaps(query)
                return true
            }

            override fun onQueryTextChange(newText: String?) = true
        })
    }

    // Send SOS SMS with location
    fun sendSOSMessage(phone: String) {
        fetchLocation { location ->
            val mapsLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
            val message = "🚨 SOS Alert! Help me immediately. My location: $mapsLink"

            // Open SMS app with pre-filled message instead of sending automatically
            val smsUri = Uri.parse("smsto:$phone")
            val intent = Intent(Intent.ACTION_SENDTO, smsUri)
            intent.putExtra("sms_body", message)
            startActivity(intent)
        }
    }


    // Capture multiple full-size images and save in gallery
    fun openCameraAndCaptureMultipleImages() {
        imagesCaptured = 0
        captureImage()
    }

    private fun captureImage() {
        if (imagesCaptured >= TOTAL_IMAGES) {
            showMessage("Captured $TOTAL_IMAGES images")
            return
        }

        val filename = "SOS_Image_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/SOS_Images")
            }
        }

        cameraImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (cameraImageUri != null) {
                showMessage("Image saved in gallery")
                imagesCaptured++
                Handler(Looper.getMainLooper()).postDelayed({
                    captureImage()
                }, 500) // small delay before next capture
            }
        }

        VoiceAssistantHelper(this).handleVoiceResult(requestCode, resultCode, data)
    }

    fun openMaps(query: String) {
        fetchLocation {
            val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (mapIntent.resolveActivity(packageManager) != null) startActivity(mapIntent)
        }
    }

    fun fetchLocation(onLocationReady: (Location) -> Unit = {}) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    onLocationReady(location)
                } else showMessage("Could not fetch location")
            }
        } else showMessage("Location permission not granted")
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        }
    }

    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 5000)
        }
    }

    private fun fetchEmergencyNumber() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            dbRef.child("phone").get()
                .addOnSuccessListener { emergencyNumber = it.value?.toString() }
                .addOnFailureListener { showMessage("Failed to fetch number: ${it.message}") }
        } else showMessage("No user logged in")
    }

    fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
