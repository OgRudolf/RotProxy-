package com.rotproxy.config;

public class ProxyProfile {
    public String name;
    public String host;
    public int port;
    public String username;
    public String password;
    public ProxyType type;

    public enum ProxyType {
        SOCKS5, HTTP, HTTPS
    }

    public ProxyProfile() {
        this.name = "New Profile";
        this.host = "";
        this.port = 1080;
        this.username = "";
        this.password = "";
        this.type = ProxyType.SOCKS5;
    }

    public ProxyProfile(String name) {
        this();
        this.name = name;
    }

    public ProxyProfile copy() {
        ProxyProfile profile = new ProxyProfile();
        profile.name = this.name;
        profile.host = this.host;
        profile.port = this.port;
        profile.username = this.username;
        profile.password = this.password;
        profile.type = this.type;
        return profile;
    }

    public boolean isValid() {
        return host != null && !host.trim().isEmpty() && port > 0 && port < 65536;
    }

    @Override
    public String toString() {
        return name;
    }
}
