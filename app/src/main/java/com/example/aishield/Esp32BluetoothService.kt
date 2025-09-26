package com.example.aishield

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class Esp32BluetoothService(
    private val context: Context,
    private val phoneNumber: String
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var listening = false

    private val sppUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Classic SPP UUID

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceName: String): Boolean {
        if (bluetoothAdapter == null) {
            Log.e("ESP32", "Bluetooth not supported")
            return false
        }

        val device: BluetoothDevice? = bluetoothAdapter.bondedDevices
            .firstOrNull { it.name == deviceName }

        if (device == null) {
            Log.e("ESP32", "Device $deviceName not found")
            return false
        }

        return try {
            socket = device.createRfcommSocketToServiceRecord(sppUUID)
            socket?.connect()
            Log.i("ESP32", "Connected to $deviceName")
            true
        } catch (e: Exception) {
            Log.e("ESP32", "Connection failed: ${e.message}")
            false
        }
    }

    fun startListening() {
        if (socket == null) {
            Log.e("ESP32", "Socket not connected")
            return
        }
        listening = true
        Thread {
            try {
                val reader = BufferedReader(InputStreamReader(socket!!.inputStream))
                while (listening) {
                    val line = reader.readLine()
                    if (line != null) {
                        Log.d("ESP32", "Received: $line")
                        handleMessage(line)
                    }
                }
            } catch (e: Exception) {
                Log.e("ESP32", "Listening error: ${e.message}")
            }
        }.start()
    }

    fun stopListening() {
        listening = false
        socket?.close()
    }

    private fun handleMessage(msg: String) {
    if (msg.startsWith("NO_GPS")) {
        sendSmsFallback()
        
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, "GPS not available, fallback SMS sent", Toast.LENGTH_LONG).show()
        }
        return
    }
    try {
        val parts = msg.split(",")
        val lat = parts[0].substringAfter("LAT:").trim()
        val lon = parts[1].substringAfter("LON:").trim()
        sendSmsWithLocation(lat, lon)
    } catch (e: Exception) {
        Log.e("ESP32", "Invalid GPS format: $msg")
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSmsWithLocation(lat: String, lon: String) {
        val smsManager = SmsManager.getDefault()
        val message = "Emergency! Location: https://maps.google.com/?q=$lat,$lon"

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.i("ESP32", "SMS sent: $message")
        } else {
            Log.e("ESP32", "SEND_SMS permission not granted")
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSmsFallback() {
        val smsManager = SmsManager.getDefault()
        val message = "Emergency! GPS not available. Please check."

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.i("ESP32", "Fallback SMS sent: $message")
        } else {
            Log.e("ESP32", "SEND_SMS permission not granted")
        }
    }
}
