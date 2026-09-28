# Jarvis Android Assistant

This repository contains a starter Android app for a Jarvis-like voice assistant that can:

- toggle the flashlight
- open installed apps
- send SMS messages
- listen for voice commands
- keep a background service alive on boot

This is a prototype and a good starting point for a real smart assistant.

## Tech stack

- Kotlin
- Android SDK
- Material components
- SpeechRecognizer
- CameraManager
- SmsManager

## Project structure

- `app/src/main/java/com/jarvis/android/MainActivity.kt` — main voice command flow
- `app/src/main/java/com/jarvis/android/JarvisService.kt` — background service
- `app/src/main/java/com/jarvis/android/BootReceiver.kt` — starts service after reboot
- `app/src/main/res/layout/activity_main.xml` — UI layout
- `app/src/main/AndroidManifest.xml` — permissions and app configuration

## Example commands

- "turn on flashlight"
- "turn off flashlight"
- "open WhatsApp"
- "open settings"
- "send sms +123456789 hello friend"

## Important notes

Android permissions and app restrictions vary by device model and Android version. For production use, you may need to add:

- better wake-word recognition
- continuous background listening
- contact lookup by name instead of raw phone numbers
- a proper local AI model or cloud API
- accessibility/service improvements for more advanced automation

## Build

Open the project in Android Studio and run it on an Android device or emulator.

## Known limitations for a starter prototype

- SMS sending needs a real device with SMS capability
- voice recognition is basic and depends on the device speech engine
- flashlight access varies by hardware
- app opening works best for installed apps with visible launcher entries
