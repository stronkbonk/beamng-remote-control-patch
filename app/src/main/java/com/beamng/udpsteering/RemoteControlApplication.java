package com.beamng.udpsteering;

import android.app.Application;

import java.net.InetAddress;

public class RemoteControlApplication extends Application {
    private InetAddress hostAddress;
    private InetAddress manualHost;
    private String ip;

    public void setHostAddress(InetAddress i) {
        hostAddress = i;
    }

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    public void setManualHost(InetAddress i) {
        manualHost = i;
    }

    public InetAddress getManualHost() {
        return manualHost;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }
}
