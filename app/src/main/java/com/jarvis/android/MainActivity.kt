package com.jarvis.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var voiceButton: Button
    private lateinit var voiceLauncher: ActivityResultLauncher<Intent>
    private var flashlightEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        voiceButton = findViewById(R.id.voiceButton)

        voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val command = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                handleVoiceCommand(command)
            } else {
                statusText.text = "Voice recognition cancelled"
            }
        }

        voiceButton.setOnClickListener {
            requestRuntimePermissions()
            startVoiceRecognition()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val serviceIntent = Intent(this, JarvisService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            statusText.text = if (allGranted) {
                "Permissions granted. Jarvis is ready."
            } else {
                "Some permissions are denied. Voice assistant may not work fully."
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command")
        }
        voiceLauncher.launch(intent)
    }

    private fun handleVoiceCommand(command: String) {
        statusText.text = "Heard: $command"

        when {
            command.contains("flashlight on") || command.contains("turn on flashlight") -> {
                toggleFlashlight(true)
            }
            command.contains("flashlight off") || command.contains("turn off flashlight") -> {
                toggleFlashlight(false)
            }
            command.contains("open ") -> {
                val appName = command.removePrefix("open ").trim()
                openAppByName(appName)
            }
            command.contains("send sms") || command.contains("sms") -> {
                parseAndSendSms(command)
            }
            command.contains("settings") -> {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
                statusText.text = "Opening Settings"
            }
            else -> {
                statusText.text = "Command not recognized: $command"
            }
        }
    }

    private fun toggleFlashlight(on: Boolean) {
        try {
            val cameraManager = getSystemService(CameraManager::class.java)
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: run {
                statusText.text = "No camera available for flashlight"
                return
            }

            cameraManager.setTorchMode(cameraId, on)
            flashlightEnabled = on
            statusText.text = if (on) "Flashlight is ON" else "Flashlight is OFF"
        } catch (e: CameraAccessException) {
            statusText.text = "Flashlight access denied: ${e.message}"
        }
    }

    private fun openAppByName(appName: String) {
        val packageManager = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        val match = apps.firstOrNull { activityInfo ->
            val label = activityInfo.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
            label.contains(appName.lowercase(Locale.getDefault()))
        }

        if (match != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(match.activityInfo.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
                statusText.text = "Opening ${match.activityInfo.loadLabel(packageManager)}"
            }
        } else {
            statusText.text = "App not found: $appName"
        }
    }

    private fun parseAndSendSms(command: String) {
        val pattern = Regex(
            "(?:send\\s+sms(?:\\s+to)?|sms)\\s+([+0-9]{7,})\\s+(?:message\\s+)?(.+)",
            setOf(RegexOption.IGNORE_CASE)
        )

        val match = pattern.find(command)
        if (match != null) {
            val destination = match.groupValues[1]
            val message = match.groupValues[2].trim()
            sendSms(destination, message)
        } else {
            statusText.text = "Example: 'send sms +123456789 hello there'"
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            statusText.text = "SMS permission is required"
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            statusText.text = "SMS sent to $phoneNumber"
        } catch (e: Exception) {
            statusText.text = "Could not send SMS: ${e.message}"
        }
    }
}
