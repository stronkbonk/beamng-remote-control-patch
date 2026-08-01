# BeamNG Remote Control Patch (Android 15)

This is a patched version of the official BeamNG Remote Control app. It lets you use your Android phone as a remote control (steering wheel, throttle, brake, HUD) for BeamNG.drive over your local WiFi network.

The patch makes the old 2015 app work on modern phones running Android 14 and 15 (for example a Samsung Galaxy A17 5G). The original app cannot even be installed on Android 14+ because it targets an old Android version.

## What changed

The original build stack was obsolete, so the app could not build or install anymore. This patch:

- Updates Gradle to 8.9 and Android Gradle Plugin to 8.7.3
- Updates compileSdk, targetSdk to 35 and minSdk to 23
- Replaces the dead jcenter() repository with google() and mavenCentral()
- Removes the dead play-services-appindexing (Google App Indexing) dependency
- Adds runtime CAMERA permission handling
- Fixes a QR code scanner crash (getActionBar on a NoActionBar theme)
- Fixes IP detection on Android 12+ (NetworkInterface instead of getConnectionInfo)
- Fixes the UDP discovery broadcast (SO_BROADCAST) so "Connection timeout" no longer happens
- Accepts both QR code formats (old "url#code" and new bare numeric code used by BeamNG 0.39)
- Fixes the 16-byte control packet (missing packet id) and the 100-byte telemetry buffer
- Fixes handbrake flag, odometer value and stuck indicator lights
- Adds a manual PC IP entry fallback for routers that block broadcasts

## Download

The ready-to-install APK is attached as a GitHub release:

[Download BeamNG-Remote-Control-v0.6.apk](https://github.com/stronkbonk/beamng-remote-control-patch/releases)

It is signed, targets Android 15 (API 35), and needs Android 6.0 or newer (API 23).

## How to use

1. Put your phone and PC on the same WiFi network.
2. Install the APK on your phone.
3. In BeamNG.drive: Options -> Controls -> Hardware -> Remote Control, then open the QR code screen. For the full HUD (speed, RPM, gear, lights) use BeamNG 0.14. BeamNG 0.39 works for control but does not send telemetry to the app.
4. Open the app and tap "Scan QRCode". Allow the camera permission and scan the QR code shown by BeamNG.
5. Wait for the HUD to appear, then tilt the phone to steer and press the throttle button to drive.

If you still get a connection timeout, tap "Trouble connecting? Enter PC IP manually", type your PC's IPv4 address from `ipconfig`, then scan the QR code again.

## Build from source

You need JDK 17 and the Android SDK (platform 35). From the `Android/Udpsteering` folder:

    ./gradlew assembleDebug

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Note: the release signing keystore is intentionally not included in this repo.

## Original project

This is a fork/patch of the official open source project:

- Original repository: https://github.com/BeamNG/remotecontrol
- License: ISC-style (see [LICENSE](LICENSE)), Copyright (c) 2014 BeamNG
