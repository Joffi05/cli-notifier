package com.example.webhooknotifier

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var server: SimpleWebServer
    private val CHANNEL_ID = "webhook_notifications"

    // SharedPreferences for storing the secret token securely
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "WebhookNotifierPrefs"
    private val KEY_SECRET = "secret_token"

    // UI Components
    private lateinit var statusTextView: TextView
    private lateinit var secretEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI Components
        statusTextView = findViewById(R.id.tv_status)
        secretEditText = findViewById(R.id.et_secret)
        saveButton = findViewById(R.id.btn_save_secret)

        // Initialize EncryptedSharedPreferences
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        sharedPreferences = EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // Load saved secret (if any) and display in EditText
        val savedSecret = sharedPreferences.getString(KEY_SECRET, "")
        secretEditText.setText(savedSecret)

        // Set up Save Button Click Listener
        saveButton.setOnClickListener {
            val newSecret = secretEditText.text.toString().trim()
            if (newSecret.isEmpty()) {
                Toast.makeText(this, "Secret cannot be empty", Toast.LENGTH_SHORT).show()
            } else if (newSecret.length < 8) {
                Toast.makeText(this, "Secret should be at least 8 characters long", Toast.LENGTH_SHORT).show()
            } else {
                // Save to SharedPreferences
                sharedPreferences.edit().putString(KEY_SECRET, newSecret).apply()
                Toast.makeText(this, "Secret saved successfully", Toast.LENGTH_SHORT).show()
                Log.i("MainActivity", "Secret token updated.")
            }
        }

        createNotificationChannel()

        // Obtain the local IP address
        val ipAddress = getLocalIpAddress()
        Log.i("MainActivity", "Device IP Address: $ipAddress")

        // Initialize and start the HTTP server with the obtained IP
        server = SimpleWebServer(ipAddress, 8000)
        server.start()

        Toast.makeText(this, "Webhook server started on port 8000", Toast.LENGTH_LONG).show()
        Log.i("MainActivity", "Webhook server started on port 8000")

        // Update status with server URL
        val serverUrl = "http://$ipAddress:8000/webhook"
        statusTextView.text = "Webhook Server is Running at $serverUrl"
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        Log.i("MainActivity", "Webhook server stopped.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Webhook Notifications"
            val descriptionText = "Notifications from webhook"
            val importance = NotificationManager.IMPORTANCE_HIGH // Set to HIGH for heads-up
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Retrieves the device's local IP address.
     */
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val inetAddress = addresses.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is java.net.Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "0.0.0.0" // Fallback to all interfaces
    }

    /**
     * Inner class representing the HTTP server.
     */
    inner class SimpleWebServer(host: String, port: Int) : NanoHTTPD(host, port) {

        override fun serve(session: IHTTPSession?): Response {
            if (session?.uri != "/webhook" || session.method != Method.POST) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found")
            }

            val headers = session.headers
            val authHeader = headers["authorization"]
            Log.i("SimpleWebServer", "Headers: $headers")

            // Retrieve the secret token from SharedPreferences
            val secretToken = sharedPreferences.getString(KEY_SECRET, "") ?: ""

            // Validate the token
            if (authHeader != "Bearer $secretToken") {
                val remoteIp = session.remoteIpAddress // Correct method to get remote IP
                Log.w("SimpleWebServer", "Unauthorized access attempt from IP: $remoteIp")
                return newFixedLengthResponse(
                    Response.Status.UNAUTHORIZED,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"Unauthorized\"}"
                )
            }

            try {
                // Parse the body
                val files = mutableMapOf<String, String>()
                session.parseBody(files)
                val body = files["postData"] ?: ""
                Log.i("SimpleWebServer", "Received POST request at /webhook with body: $body")

                // Parse JSON
                val json = JSONObject(body)
                val text = json.getString("text")

                // Show notification
                showNotification("Webhook Notification", text)

                return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"status\":\"success\"}"
                )
            } catch (e: Exception) {
                Log.e("SimpleWebServer", "Error processing POST request: ${e.message}")
                return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"status\":\"error\",\"message\":\"${e.message}\"}"
                )
            }
        }

        /**
         * Displays a system notification with the given title and message.
         */
        private fun showNotification(title: String, message: String) {
            try {
                val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info) // Use a default icon for testing
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // Set priority to HIGH
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL) // Use default sounds and vibrations
                    .setVibrate(longArrayOf(0, 250, 250, 250)) // Custom vibration pattern
                    .setSound(Settings.System.DEFAULT_NOTIFICATION_URI) // Set default notification sound
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Expandable text

                with(NotificationManagerCompat.from(this@MainActivity)) {
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }
            } catch (e: Exception) {
                Log.e("SimpleWebServer", "Failed to display notification: ${e.message}")
                throw e // Rethrow to be caught in serve()
            }
        }
    }
}
