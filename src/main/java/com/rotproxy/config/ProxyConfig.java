package com.rotproxy.config;

import com.google.gson.*;
import com.rotproxy.RotProxyMod;
import net.fabricmc.loader.api.FabricLoader;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ProxyConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("rotproxy_profiles.json");

    private static List<ProxyProfile> profiles = new ArrayList<>();
    private static int activeProfileIndex = 0;
    private static boolean enabled = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ProxyProfile createBlankProfile(String name) {
        ProxyProfile profile = new ProxyProfile(name);
        profile.host = "";
        profile.port = 0;
        profile.username = "";
        profile.password = "";
        profile.type = ProxyProfile.ProxyType.SOCKS5;
        return profile;
    }

    // Machine-tied salt using MAC address
    private static String getMachineSalt() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            java.net.NetworkInterface network = java.net.NetworkInterface.getByInetAddress(ip);
            if (network != null) {
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (byte b : mac) sb.append(String.format("%02X", b));
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "RotProxyDefaultSalt2024";
    }

    private static SecretKey deriveKey(String salt) throws Exception {
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec("RotProxyKey".toCharArray(), saltBytes, 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public static String encrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            SecretKey key = deriveKey(getMachineSalt());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            java.util.Arrays.fill(iv, (byte) 0x52); // 'R' for RotProxy
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return value;
        }
    }

    public static String decrypt(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            SecretKey key = deriveKey(getMachineSalt());
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            java.util.Arrays.fill(iv, (byte) 0x52);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            byte[] decoded = Base64.getDecoder().decode(value);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static void load() {
        profiles.clear();
        if (!Files.exists(CONFIG_PATH)) {
            profiles.add(createBlankProfile("Profile 1"));
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            activeProfileIndex = root.has("activeProfile") ? root.get("activeProfile").getAsInt() : 0;
            enabled = root.has("enabled") && root.get("enabled").getAsBoolean();
            JsonArray arr = root.getAsJsonArray("profiles");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                ProxyProfile p = new ProxyProfile();
                p.name = obj.get("name").getAsString();
                p.host = obj.get("host").getAsString();
                p.port = obj.get("port").getAsInt();
                p.username = decrypt(obj.has("username") ? obj.get("username").getAsString() : "");
                p.password = decrypt(obj.has("password") ? obj.get("password").getAsString() : "");
                p.type = ProxyProfile.ProxyType.valueOf(obj.get("type").getAsString());
                profiles.add(p);
            }
        } catch (Exception e) {
            RotProxyMod.LOGGER.error("Failed to load RotProxy config", e);
            profiles.add(createBlankProfile("Profile 1"));
        }
        if (profiles.isEmpty()) {
            profiles.add(createBlankProfile("Profile 1"));
        }
        if (activeProfileIndex >= profiles.size()) activeProfileIndex = 0;
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("activeProfile", activeProfileIndex);
            root.addProperty("enabled", enabled);
            JsonArray arr = new JsonArray();
            for (ProxyProfile p : profiles) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", p.name);
                obj.addProperty("host", p.host);
                obj.addProperty("port", p.port);
                obj.addProperty("username", encrypt(p.username));
                obj.addProperty("password", encrypt(p.password));
                obj.addProperty("type", p.type.name());
                arr.add(obj);
            }
            root.add("profiles", arr);
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            RotProxyMod.LOGGER.error("Failed to save RotProxy config", e);
        }
    }

    public static List<ProxyProfile> getProfiles() { return profiles; }
    public static int getActiveProfileIndex() { return activeProfileIndex; }
    public static void setActiveProfileIndex(int i) { activeProfileIndex = i; }
    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean value) { enabled = value; }

    public static ProxyProfile getActiveProfile() {
        if (profiles.isEmpty()) return null;
        if (activeProfileIndex < 0 || activeProfileIndex >= profiles.size()) return profiles.get(0);
        return profiles.get(activeProfileIndex);
    }

    public static void addProfile(String name) {
        profiles.add(createBlankProfile(name));
    }

    public static void removeProfile(int index) {
        if (profiles.size() > 1) {
            profiles.remove(index);
            if (activeProfileIndex >= profiles.size()) activeProfileIndex = profiles.size() - 1;
        }
    }

    public static void clearProfile(int index) {
        if (index < 0 || index >= profiles.size()) {
            return;
        }

        String name = profiles.get(index).name;
        profiles.set(index, createBlankProfile(name == null || name.isBlank() ? "Profile " + (index + 1) : name.trim()));
    }

    public static void resetAllProfiles() {
        profiles.clear();
        profiles.add(createBlankProfile("Profile 1"));
        activeProfileIndex = 0;
        enabled = false;
    }
}
