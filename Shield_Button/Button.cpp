#include <HardwareSerial.h>
#include <TinyGPS++.h>
#include "BluetoothSerial.h"

TinyGPSPlus gps;
HardwareSerial gpsSerial(1); 

BluetoothSerial SerialBT;

#define GPS_RX 16  // ESP32 RX2 -> NEO-6M TX
#define GPS_TX 17  // ESP32 TX2 -> NEO-6M RX
#define BUTTON_PIN  4

void setup() {
  Serial.begin(115200);
  gpsSerial.begin(9600, SERIAL_8N1, GPS_RX, GPS_TX);
  SerialBT.begin("Shield Button");

  pinMode(BUTTON_PIN, INPUT_PULLUP);
  Serial.println("ESP32 ready. Waiting for button press...");
}

void loop() {
  // Continuously feed GPS parser
  while (gpsSerial.available() > 0) {
    gps.encode(gpsSerial.read());
  }

  if (digitalRead(BUTTON_PIN) == LOW) {
    Serial.println("Button pressed!");

    if (gps.location.isValid()) {
      double lat = gps.location.lat();
      double lon = gps.location.lng();

      String message = "LAT:" + String(lat, 6) + ",LON:" + String(lon, 6);

      SerialBT.println(message);
      Serial.println("Sent over Bluetooth: " + message);
    } else {
      Serial.println("Waiting for valid GPS fix...");
    }

    delay(2000);
  }
}
