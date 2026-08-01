package com.beamng.udpsteering;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.Result;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRCodeScanner extends Activity
        implements ZXingScannerView.ResultHandler, OnUdpConnected {
    private static final int REQUEST_CAMERA = 0;
    private static final Pattern IPV4_PATTERN = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})");
    private ZXingScannerView mScannerView;
    private ConnectivityManager connManager;
    private Context mContext;
    private UdpExploreSender exploreSender;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mContext = getApplicationContext();
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasCameraPermission()) {
            mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
            mScannerView.startCamera();          // Start camera on resume
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mScannerView.setResultHandler(this);
                mScannerView.startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasCameraPermission() {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
        if (exploreSender != null) {
            exploreSender.cancel(true);
            exploreSender = null;
        }
    }

    @Override
    public void handleResult(Result result) {
        String qrText = result.getText() == null ? "" : result.getText().trim();

        // BeamNG QR formats:
        //  - 0.14 and older: "https://play.google.com/store/apps/details?id=com.beamng.remotecontrol#<code>"
        //  - 0.39 and newer: just the bare numeric code, e.g. "12345"
        String securityCode = null;
        InetAddress qrHost = null;
        int hashIndex = qrText.indexOf('#');
        if (hashIndex >= 0) {
            String[] parts = qrText.split("#", 2);
            securityCode = parts[1].trim();
            qrHost = extractIpFromText(parts[0]);
        } else {
            securityCode = qrText;
        }

        if (securityCode.isEmpty() || !securityCode.matches("\\d+")) {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_LONG).show();
            mScannerView.startCamera();
            return;
        }

        // Check for WiFi connectivity
        connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi == null || !mWifi.isConnected()) {
            Toast.makeText(this, "You need to be connected to a WiFi network.", Toast.LENGTH_LONG).show();
            mScannerView.startCamera();
            return;
        }

        InetAddress localAddress = getIpAddress();
        if (localAddress == null) {
            Toast.makeText(this, "Could not determine your IP address.", Toast.LENGTH_LONG).show();
            mScannerView.startCamera();
            return;
        }
        String ip = localAddress.getHostAddress();
        ((RemoteControlApplication)getApplication()).setIp(ip);

        InetAddress broadcastAddress = getBroadcastAddress(localAddress);
        if (broadcastAddress == null) {
            Toast.makeText(this, "Could not determine broadcast address.", Toast.LENGTH_LONG).show();
            mScannerView.startCamera();
            return;
        }
        Log.i("Broadcastaddress", broadcastAddress.getHostAddress());

        // Discovery is sent to several targets so it works even if broadcast
        // is blocked (AP isolation) or if the QR code embeds the PC's IP.
        List<InetAddress> targets = new ArrayList<InetAddress>();
        targets.add(broadcastAddress);
        try {
            InetAddress limitedBroadcast = InetAddress.getByName("255.255.255.255");
            if (!limitedBroadcast.equals(broadcastAddress)) {
                targets.add(limitedBroadcast);
            }
        } catch (Exception e) {
            // ignore
        }
        if (qrHost != null && !targets.contains(qrHost)) {
            targets.add(qrHost);
        }
        // A manually entered PC IP (fallback when broadcast does not work)
        InetAddress manualHost = ((RemoteControlApplication) getApplication()).getManualHost();
        if (manualHost != null && !targets.contains(manualHost)) {
            targets.add(manualHost);
        }

        exploreSender = new UdpExploreSender(targets, this, this, ip, this);
        exploreSender.execute(securityCode);
    }

    private InetAddress extractIpFromText(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = IPV4_PATTERN.matcher(text);
        if (m.find()) {
            try {
                InetAddress addr = InetAddress.getByName(m.group(1));
                if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                    return addr;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    private InetAddress getBroadcastAddress(InetAddress inetAddr) {
        try {
            NetworkInterface temp = NetworkInterface.getByInetAddress(inetAddr);
            if (temp != null) {
                List<InterfaceAddress> addresses = temp.getInterfaceAddresses();
                for (InterfaceAddress inetAddress : addresses) {
                    if (inetAddress.getBroadcast() != null) {
                        return inetAddress.getBroadcast();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public InetAddress getIpAddress() {
        try {
            InetAddress fallback = null;
            for (Enumeration<NetworkInterface> networkInterface = NetworkInterface
                    .getNetworkInterfaces(); networkInterface.hasMoreElements(); ) {

                NetworkInterface singleInterface = networkInterface.nextElement();
                if (!singleInterface.isUp() || singleInterface.isLoopback()) {
                    continue;
                }
                String displayName = singleInterface.getDisplayName() == null
                        ? "" : singleInterface.getDisplayName().toLowerCase();

                for (Enumeration<InetAddress> IpAddresses = singleInterface.getInetAddresses(); IpAddresses
                        .hasMoreElements(); ) {
                    InetAddress inetAddress = IpAddresses.nextElement();

                    if (!inetAddress.isLoopbackAddress()
                            && (inetAddress instanceof Inet4Address)) {

                        // Prefer wifi/ethernet interfaces when present
                        if (displayName.contains("wlan")
                                || displayName.contains("eth")
                                || displayName.contains("ap0")) {
                            return inetAddress;
                        }
                        // remember the first usable address in case no wifi/eth
                        // interface is found (unusual interface names)
                        if (fallback == null) {
                            fallback = inetAddress;
                        }
                    }
                }
            }
            if (fallback != null) {
                return fallback;
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void onUdpConnected(InetAddress hostAddress) {
        Intent intent = new Intent(this, MainActivity.class);
        ((RemoteControlApplication)getApplication()).setHostAddress(hostAddress);
        startActivity(intent);
    }

    @Override
    public void onError(String message) {
        if (message != null) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
        mScannerView.startCamera();
    }
}
