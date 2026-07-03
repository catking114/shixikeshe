package com.example.keshe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Tuya Cloud API Service
 * Full integration with Tuya IoT Platform
 * Docs: https://developer.tuya.com/en/docs/cloud/
 */
@Service
public class TuyaCloudService {

    private static final Logger log = LoggerFactory.getLogger(TuyaCloudService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${tuya.access-id:}")
    private String accessId;

    @Value("${tuya.access-key:}")
    private String accessKey;

    @Value("${tuya.api-base:https://openapi.tuyacn.com}")
    private String apiBase;

    @Value("${tuya.uid:}")
    private String uid;

    private volatile String accessToken;
    private volatile long tokenExpireTime;

    // ===== Config & Auth =====

    public boolean isConfigured() {
        return accessId != null && !accessId.isEmpty() && accessKey != null && !accessKey.isEmpty();
    }

    public Map<String, Object> getConfigStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", isConfigured());
        status.put("apiBase", apiBase);
        status.put("hasValidToken", hasValidToken());
        status.put("hasUid", uid != null && !uid.isEmpty());
        return status;
    }

    private boolean hasValidToken() {
        return accessToken != null && System.currentTimeMillis() < tokenExpireTime;
    }

    private synchronized String ensureToken() throws Exception {
        if (hasValidToken()) return accessToken;
        if (!isConfigured()) throw new RuntimeException("Tuya Cloud not configured");

        String path = "/v1.0/token?grant_type=1";
        String sign = generateSign("GET", path, "");
        String response = doHttpRequest(apiBase + path, "GET", "", sign, null);
        JsonNode json = mapper.readTree(response);

        if (json.has("success") && json.get("success").asBoolean() && json.has("result")) {
            JsonNode result = json.get("result");
            accessToken = result.get("access_token").asText();
            long expireIn = result.get("expire_time").asLong();
            tokenExpireTime = System.currentTimeMillis() + (expireIn * 1000) - 60000;
            log.info("Tuya token refreshed, expires in {}s", expireIn);
            return accessToken;
        }

        String msg = json.has("msg") ? json.get("msg").asText() : "Unknown error";
        throw new RuntimeException("Failed to get Tuya token: " + msg);
    }

    // ===== Device Management =====

    /**
     * List all devices under a user (requires tuya.uid configured)
     */
    public Map<String, Object> listUserDevices() {
        try {
            ensureToken();
            if (uid == null || uid.isEmpty()) return errorResult("tuya.uid not configured in application.properties");
            String path = "/v1.0/users/" + uid + "/devices";
            return apiGet(path);
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Get device details
     */
    public Map<String, Object> getDeviceInfo(String deviceId) {
        try {
            ensureToken();
            return apiGet("/v1.0/devices/" + deviceId);
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Get device current status (all data points)
     */
    public Map<String, Object> getDeviceStatus(String deviceId) {
        try {
            ensureToken();
            return apiGet("/v1.0/devices/" + deviceId + "/status");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Get device supported functions and instruction set
     */
    public Map<String, Object> getDeviceFunctions(String deviceId) {
        try {
            ensureToken();
            return apiGet("/v1.0/devices/" + deviceId + "/functions");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Get device specifications (instruction set + status set)
     */
    public Map<String, Object> getDeviceSpecifications(String deviceId) {
        try {
            ensureToken();
            return apiGet("/v1.0/devices/" + deviceId + "/specifications");
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Get standard instruction set by category (e.g., "kg" for switch, "dj" for light)
     */
    public Map<String, Object> getCategoryFunctions(String category) {
        try {
            ensureToken();
            return apiGet("/v1.0/functions/" + category);
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Send control command to device
     */
    public Map<String, Object> sendCommand(String deviceId, String code, Object value) {
        try {
            ensureToken();
            String body = "{\"commands\":[{\"code\":\"" + code + "\",\"value\":" + mapper.writeValueAsString(value) + "}]}";
            return apiPost("/v1.0/devices/" + deviceId + "/commands", body);
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Send multiple commands at once
     */
    public Map<String, Object> sendCommands(String deviceId, List<Map<String, Object>> commands) {
        try {
            ensureToken();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("commands", commands);
            return apiPost("/v1.0/devices/" + deviceId + "/commands", mapper.writeValueAsString(body));
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Get device logs
     */
    public Map<String, Object> getDeviceLogs(String deviceId, int size) {
        try {
            ensureToken();
            return apiGet("/v1.0/devices/" + deviceId + "/logs?size=" + size);
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    /**
     * Get device factory info (brand, model, etc.)
     */
    public Map<String, Object> getDeviceFactoryInfo(List<String> deviceIds) {
        try {
            ensureToken();
            String ids = String.join(",", deviceIds);
            return apiGet("/v1.0/devices/factory-infos?device_ids=" + ids);
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    // ===== Built-in Tuya Category Catalog =====

    /**
     * Common Tuya device categories with their standard codes
     * This is a curated subset for smart home devices
     */
    public Map<String, Object> getSupportedCategories() {
        Map<String, Object> categories = new LinkedHashMap<>();

        // Lighting
        categories.put("dj", Map.of("name", "灯", "nameEn", "Light", "icon", "bi-lightbulb", "description", "智能灯泡/灯带 - 支持开关、亮度、色温、RGB"));
        categories.put("xdd", Map.of("name", "吸顶灯", "nameEn", "Ceiling Light", "icon", "bi-circle", "description", "智能吸顶灯 - 支持开关、亮度、色温、场景"));
        categories.put("fwd", Map.of("name", "氛围灯", "nameEn", "Ambiance Light", "icon", "bi-stars", "description", "氛围灯/灯带 - 支持RGB颜色和动态效果"));
        categories.put("tgq", Map.of("name", "调光器", "nameEn", "Dimmer", "icon", "bi-brightness-high", "description", "调光器 - 支持亮度调节"));

        // Switches & Plugs
        categories.put("kg", Map.of("name", "开关", "nameEn", "Switch", "icon", "bi-toggle-on", "description", "智能开关 - 支持单/多键控制"));
        categories.put("cz", Map.of("name", "插座", "nameEn", "Socket", "icon", "bi-plug", "description", "智能插座 - 支持开关和电量统计"));
        categories.put("tdq", Map.of("name", "断路器", "nameEn", "Breaker", "icon", "bi-lightning", "description", "智能断路器"));

        // Sensors
        categories.put("wsdcg", Map.of("name", "温湿度传感器", "nameEn", "Temp/Humidity Sensor", "icon", "bi-thermometer-half", "description", "温度+湿度传感器"));
        categories.put("ldcg", Map.of("name", "光照传感器", "nameEn", "Light Sensor", "icon", "bi-brightness-alt-high", "description", "光照度传感器"));
        categories.put("pir", Map.of("name", "人体传感器", "nameEn", "Motion Sensor", "icon", "bi-person-walking", "description", "PIR人体红外传感器"));
        categories.put("mcs", Map.of("name", "门窗传感器", "nameEn", "Door/Window Sensor", "icon", "bi-door-open", "description", "门窗开合传感器"));
        categories.put("rqbj", Map.of("name", "燃气报警器", "nameEn", "Gas Detector", "icon", "bi-fire", "description", "可燃气体检测报警器"));
        categories.put("ywbj", Map.of("name", "烟雾报警器", "nameEn", "Smoke Detector", "icon", "bi-cloud-haze", "description", "烟雾报警器"));

        // Climate & Environment
        categories.put("fs", Map.of("name", "风扇", "nameEn", "Fan", "icon", "bi-fan", "description", "智能风扇 - 支持风速、摇头、定时"));
        categories.put("kt", Map.of("name", "空调", "nameEn", "Air Conditioner", "icon", "bi-snow2", "description", "智能空调控制器"));
        categories.put("qn", Map.of("name", "取暖器", "nameEn", "Heater", "icon", "bi-fire", "description", "智能取暖器"));
        categories.put("cs", Map.of("name", "除湿机", "nameEn", "Dehumidifier", "icon", "bi-droplet-half", "description", "智能除湿机"));
        categories.put("jsq", Map.of("name", "加湿器", "nameEn", "Humidifier", "icon", "bi-droplet", "description", "智能加湿器"));
        categories.put("kj", Map.of("name", "空气净化器", "nameEn", "Air Purifier", "icon", "bi-wind", "description", "空气净化器"));

        // Covers & Locks
        categories.put("cl", Map.of("name", "窗帘电机", "nameEn", "Curtain Motor", "icon", "bi-aspect-ratio", "description", "智能窗帘/卷帘电机"));
        categories.put("ms", Map.of("name", "门锁", "nameEn", "Smart Lock", "icon", "bi-lock", "description", "智能门锁"));

        // Camera & Security
        categories.put("sp", Map.of("name", "摄像机", "nameEn", "Camera", "icon", "bi-camera-video", "description", "智能摄像头"));

        // Robot & Appliance
        categories.put("sd", Map.of("name", "扫地机器人", "nameEn", "Robot Vacuum", "icon", "bi-robot", "description", "扫地/拖地机器人"));

        return categories;
    }

    /**
     * Import all user devices from Tuya and return them as structured data
     */
    public List<Map<String, Object>> importAllDevices() {
        List<Map<String, Object>> imported = new ArrayList<>();
        Map<String, Object> result = listUserDevices();
        if (!Boolean.TRUE.equals(result.get("success"))) {
            log.warn("Failed to list Tuya devices: {}", result.get("error"));
            return imported;
        }

        try {
            JsonNode responseJson = mapper.readTree((String) result.get("response"));
            if (responseJson.has("success") && responseJson.get("success").asBoolean() && responseJson.has("result")) {
                JsonNode devices = responseJson.get("result");
                if (devices.isArray()) {
                    for (JsonNode d : devices) {
                        Map<String, Object> device = new LinkedHashMap<>();
                        device.put("tuyaId", d.path("id").asText());
                        device.put("name", d.path("name").asText());
                        device.put("category", d.path("category").asText());
                        device.put("productId", d.path("product_id").asText());
                        device.put("online", d.path("online").asBoolean());
                        device.put("ip", d.path("ip").asText(""));
                        device.put("localKey", d.path("local_key").asText(""));
                        device.put("uuid", d.path("uuid").asText(""));
                        device.put("model", d.path("model").asText(""));
                        device.put("brand", d.path("brand").asText(""));

                        // Parse status array
                        List<Map<String, Object>> statusList = new ArrayList<>();
                        if (d.has("status") && d.get("status").isArray()) {
                            for (JsonNode s : d.get("status")) {
                                Map<String, Object> st = new LinkedHashMap<>();
                                st.put("code", s.path("code").asText());
                                st.put("value", s.path("value").asText());
                                statusList.add(st);
                            }
                        }
                        device.put("status", statusList);
                        imported.add(device);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Parse Tuya device list failed", e);
        }

        return imported;
    }

    // ===== Internal API helpers =====

    private Map<String, Object> apiGet(String path) {
        try {
            String sign = generateSign("GET", path, "");
            String response = doHttpRequest(apiBase + path, "GET", "", sign, accessToken);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("response", response);
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    private Map<String, Object> apiPost(String path, String body) {
        try {
            String sign = generateSign("POST", path, body);
            String response = doHttpRequest(apiBase + path, "POST", body, sign, accessToken);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("response", response);
            return result;
        } catch (Exception e) {
            return errorResult(e.getMessage());
        }
    }

    private String generateSign(String method, String path, String body) throws Exception {
        long t = System.currentTimeMillis() / 1000;
        String content = accessId + (accessToken != null ? accessToken : "") + t + method + "\n" + sha256(body) + "\n" + path;
        return sha256(content).toUpperCase();
    }

    private String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append("0");
            hex.append(h);
        }
        return hex.toString();
    }

    private String doHttpRequest(String urlStr, String method, String body, String sign, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("client_id", accessId);
        conn.setRequestProperty("sign", sign);
        conn.setRequestProperty("t", String.valueOf(System.currentTimeMillis()));
        conn.setRequestProperty("sign_method", "HMAC-SHA256");
        if (token != null) conn.setRequestProperty("access_token", token);
        conn.setRequestProperty("Content-Type", "application/json");

        if ("POST".equals(method) && body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new IOException("HTTP " + code + " with no response body");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private Map<String, Object> errorResult(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("error", msg);
        return r;
    }
}
