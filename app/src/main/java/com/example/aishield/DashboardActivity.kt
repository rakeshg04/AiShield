package com.example.aishield

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var espService: Esp32BluetoothService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ✅ Initialize ESP32 service
        espService = Esp32BluetoothService(this)

        val btnConnect = findViewById<Button>(R.id.btnConnectBluetooth)
        btnConnect.setOnClickListener {
            if (espService.connectToDevice("ESP32_BT_NAME")) { // replace with your ESP32 name/MAC
                espService.startListening()
                Toast.makeText(this, "ESP32 connected", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "ESP32 connection failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
