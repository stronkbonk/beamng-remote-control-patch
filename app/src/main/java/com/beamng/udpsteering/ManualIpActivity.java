package com.beamng.udpsteering;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetAddress;

public class ManualIpActivity extends Activity {

    private EditText ipInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_ip);
        ipInput = (EditText) findViewById(R.id.ipInput);

        InetAddress manualHost = ((RemoteControlApplication) getApplication()).getManualHost();
        if (manualHost != null) {
            ipInput.setText(manualHost.getHostAddress());
        }
    }

    public void onNextClick(View view) {
        String text = ipInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Please enter the PC's IP address", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            InetAddress host = InetAddress.getByName(text);
            if (host.isLoopbackAddress()) {
                Toast.makeText(this, "Please enter your PC's local IP, not 127.0.0.1", Toast.LENGTH_LONG).show();
                return;
            }
            ((RemoteControlApplication) getApplication()).setManualHost(host);
            Toast.makeText(this, "Will use " + host.getHostAddress() + " - now scan the QR code", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, QRCodeScanner.class));
        } catch (Exception e) {
            Toast.makeText(this, "Invalid IP address: " + text, Toast.LENGTH_LONG).show();
        }
    }
}
