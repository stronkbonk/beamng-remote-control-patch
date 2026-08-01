# BeamNG Remote Control for Android 15

So the old BeamNG remote control app from 2015 just wouldnt install on my new phone. Turns out it targets a super old Android version and Android 14+ flat out refuses to install those. This is that same app, fixed up so it runs on modern Android (tested on a Galaxy A17 5G running Android 15).

Its the same official app underneath, just with the build stack modernized and a few bugs patched. Everything else is untouched.

## What was wrong

- the old build setup was dead (Gradle 2.2, jcenter shut down, Google App Indexing removed)
- even if it built, it wouldnt install on Android 14+ because of the old targetSdk
- QR scanner crashed on newer Android (getActionBar on a theme that has no action bar)
- IP detection broke on Android 12+, it returned 0.0.0.0
- UDP discovery broadcast never sent properly so it just timed out
- newer BeamNG QR codes (0.39) are a bare number now, the app only accepted the old url#code format
- handbrake light never turned on and indicator lights got stuck once lit

## What got fixed

- Gradle 8.9 / AGP 8.7.3, targetSdk 35, minSdk 23 (so it runs on Android 6.0 all the way to 15)
- jcenter replaced with google() / mavenCentral(), dead dependency removed
- runtime camera permission, QR scanner crash fixed
- proper IP detection, no more 0.0.0.0
- broadcast fix so the "connection timeout" is gone
- reads both old and new QR formats
- control packet and telemetry buffer fixed (packet id + odometer were missing)
- handbrake flag fixed, lights reset properly
- added a manual PC IP fallback in case your router blocks broadcasts

## Install

Grab the APK from the releases page and just install it:

https://github.com/stronkbonk/beamng-remote-control-patch/releases

Its signed, targets Android 15, and needs Android 6.0 or newer. If you already had an older version installed this updates in place.

## How to use

1. phone and PC on the same wifi
2. install the apk
3. in BeamNG: Options > Controls > Hardware > Remote Control and open the QR screen. use BeamNG 0.14 for the full HUD (speed, rpm, gear, lights). 0.39 works for steering and buttons but never sends telemetry so the HUD stays empty
4. open the app, tap scan qr, allow camera, scan
5. wait for the HUD to show up, tilt the phone to steer and press throttle to go

getting a connection timeout? tap the "enter pc ip manually" link, type your PCs ip from ipconfig, and scan the qr again. that bypasses broadcast entirely.

## Build it yourself

needs JDK 17 and the android sdk (platform 35).

    cd Android/Udpsteering
    ./gradlew assembleDebug

debug apk ends up in app/build/outputs/apk/debug/

(the signing keystore is not in the repo on purpose, keep that in mind)

## Credit

this is just a patched fork of the official app, all credit goes to the original devs.

- original repo: https://github.com/BeamNG/remotecontrol
- license: ISC-style (see LICENSE), copyright (c) 2014 BeamNG
