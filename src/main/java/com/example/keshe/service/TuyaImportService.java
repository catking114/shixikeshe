package com.example.keshe.service;

import com.example.keshe.entity.Device;
import com.example.keshe.entity.DeviceCatalog;
import com.example.keshe.repository.DeviceCatalogRepository;
import com.example.keshe.repository.DeviceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TuyaImportService {

    private static final Logger log = LoggerFactory.getLogger(TuyaImportService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final TuyaCloudService tuyaCloudService;
    private final DeviceRepository deviceRepository;
    private final DeviceCatalogRepository catalogRepository;

    public TuyaImportService(TuyaCloudService tuyaCloudService,
                             DeviceRepository deviceRepository,
                             DeviceCatalogRepository catalogRepository) {
        this.tuyaCloudService = tuyaCloudService;
        this.deviceRepository = deviceRepository;
        this.catalogRepository = catalogRepository;
    }

    /**
     * Sync all Tuya categories from the built-in catalog into the database.
     * Optionally fetches instruction sets from Tuya API for each category.
     */
    public Map<String, Object> syncCategories(boolean fetchInstructions) {
        Map<String, Object> categoryMap = tuyaCloudService.getSupportedCategories();
        int synced = 0;
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Object> entry : categoryMap.entrySet()) {
            String code = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, String> info = (Map<String, String>) entry.getValue();

            try {
                DeviceCatalog catalog = catalogRepository.findByCategoryCode(code)
                        .orElse(new DeviceCatalog());

                catalog.setCategoryCode(code);
                catalog.setCategoryName(info.get("name"));
                catalog.setCategoryNameEn(info.get("nameEn"));
                catalog.setIcon(info.get("icon"));
                catalog.setDescription(info.get("description"));
                catalog.setLastSync(LocalDateTime.now());

                if (fetchInstructions && tuyaCloudService.isConfigured()) {
                    Map<String, Object> funcResult = tuyaCloudService.getCategoryFunctions(code);
                    if (Boolean.TRUE.equals(funcResult.get("success"))) {
                        String responseJson = (String) funcResult.get("response");
                        try {
                            JsonNode responseNode = mapper.readTree(responseJson);
                            if (responseNode.has("result")) {
                                JsonNode result = responseNode.get("result");
                                if (result.has("functions")) {
                                    catalog.setFunctionsJson(mapper.writeValueAsString(result.get("functions")));
                                }
                                if (result.has("status")) {
                                    catalog.setStatusJson(mapper.writeValueAsString(result.get("status")));
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Parse functions for {} failed: {}", code, e.getMessage());
                        }
                    } else {
                        errors.add(code + ": " + funcResult.get("error"));
                    }
                }

                // Count how many devices use this category
                // (We'll match by category code stored in device model field or external id prefix)
                if (catalog.getImportedCount() == null) {
                    catalog.setImportedCount(0);
                }

                catalogRepository.save(catalog);
                synced++;
            } catch (Exception e) {
                errors.add(code + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("synced", synced);
        result.put("total", categoryMap.size());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return result;
    }

    /**
     * Import all devices from the user's Tuya account into the local database.
     * Returns summary of import.
     */
    public Map<String, Object> importUserDevices() {
        if (!tuyaCloudService.isConfigured()) {
            return Map.of("success", false, "error", "Tuya Cloud not configured. Set tuya.access-id and tuya.access-key in application.properties");
        }

        List<Map<String, Object>> tuyaDevices = tuyaCloudService.importAllDevices();
        if (tuyaDevices.isEmpty()) {
            return Map.of("success", true, "imported", 0, "skipped", 0, "message", "No devices found in your Tuya account");
        }

        int imported = 0;
        int skipped = 0;
        int updated = 0;
        List<Map<String, Object>> importedList = new ArrayList<>();

        for (Map<String, Object> td : tuyaDevices) {
            String tuyaId = (String) td.get("tuyaId");
            String category = (String) td.get("category");

            // Check if this device already exists (by externalDeviceId)
            List<Device> existing = deviceRepository.findAll().stream()
                    .filter(d -> tuyaId.equals(d.getExternalDeviceId()))
                    .toList();

            Device device;
            boolean isNew;
            if (!existing.isEmpty()) {
                device = existing.get(0);
                isNew = false;
            } else {
                device = new Device();
                device.setCreateTime(LocalDateTime.now());
                isNew = true;
            }

            // Map Tuya fields to local Device
            device.setDeviceName((String) td.get("name"));
            device.setExternalDeviceId(tuyaId);
            device.setProtocolType("TUYA_CLOUD");
            device.setModel((String) td.get("model"));

            // Map brand: use Tuya brand if available, else generic
            String brand = (String) td.get("brand");
            device.setBrand(brand != null && !brand.isEmpty() ? brand : "Tuya Smart");

            // Map device type from category
            device.setDeviceType(mapCategoryToDeviceType(category));

            // Set default room
            if (device.getRoomLocation() == null || device.getRoomLocation().isEmpty()) {
                device.setRoomLocation("Tuya - " + getCategoryDisplayName(category));
            }

            // Set IP if available
            String ip = (String) td.get("ip");
            if (ip != null && !ip.isEmpty()) {
                device.setIpAddress(ip);
            }

            // Parse status to set initial state
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> statusList = (List<Map<String, Object>>) td.get("status");
            if (statusList != null) {
                for (Map<String, Object> s : statusList) {
                    String code = (String) s.get("code");
                    String value = String.valueOf(s.get("value"));
                    applyStatusToDevice(device, code, value);
                }
            }

            // Defaults
            if (device.getStatus() == null) device.setStatus(0);
            if (device.getBrightness() == null) device.setBrightness(100);
            if (device.getColorTemp() == null) device.setColorTemp(4000);

            // Online status from Tuya
            Boolean online = (Boolean) td.get("online");
            device.setOnlineStatus(online != null && online ? 1 : 0);
            if (online != null && online) {
                device.setLastSeen(LocalDateTime.now());
            }

            deviceRepository.save(device);

            if (isNew) imported++;
            else updated++;

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("id", device.getId());
            summary.put("name", device.getDeviceName());
            summary.put("category", category);
            summary.put("type", device.getDeviceType());
            summary.put("online", device.getOnlineStatus() == 1);
            summary.put("isNew", isNew);
            importedList.add(summary);
        }

        // Update catalog imported counts
        updateCatalogCounts();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("imported", imported);
        result.put("updated", updated);
        result.put("skipped", skipped);
        result.put("total", tuyaDevices.size());
        result.put("devices", importedList);
        return result;
    }

    /**
     * Get all synced categories from the database.
     */
    public List<DeviceCatalog> getAllCategories() {
        return catalogRepository.findAll();
    }

    /**
     * Get category instruction set (from DB cache or fetch from API).
     */
    public Map<String, Object> getCategoryDetail(String categoryCode) {
        Optional<DeviceCatalog> opt = catalogRepository.findByCategoryCode(categoryCode);
        Map<String, Object> result = new LinkedHashMap<>();

        if (opt.isPresent()) {
            DeviceCatalog cat = opt.get();
            result.put("success", true);
            result.put("source", "database");
            result.put("code", cat.getCategoryCode());
            result.put("name", cat.getCategoryName());
            result.put("nameEn", cat.getCategoryNameEn());
            result.put("icon", cat.getIcon());
            result.put("description", cat.getDescription());

            try {
                if (cat.getFunctionsJson() != null) {
                    result.put("functions", mapper.readTree(cat.getFunctionsJson()));
                }
                if (cat.getStatusJson() != null) {
                    result.put("status", mapper.readTree(cat.getStatusJson()));
                }
            } catch (Exception e) {
                log.warn("Parse cached JSON for category {} failed", categoryCode);
            }
            result.put("importedCount", cat.getImportedCount());
            result.put("lastSync", cat.getLastSync());
        } else {
            // Fetch from API directly
            Map<String, Object> funcResult = tuyaCloudService.getCategoryFunctions(categoryCode);
            if (Boolean.TRUE.equals(funcResult.get("success"))) {
                result.put("success", true);
                result.put("source", "api");
                result.put("code", categoryCode);
                result.put("response", funcResult.get("response"));
            } else {
                result.put("success", false);
                result.put("error", funcResult.get("error"));
            }
        }
        return result;
    }

    /**
     * Get Tuya device functions from the cloud API.
     */
    public Map<String, Object> getDeviceFunctions(String tuyaDeviceId) {
        return tuyaCloudService.getDeviceFunctions(tuyaDeviceId);
    }

    /**
     * Get Tuya device specifications.
     */
    public Map<String, Object> getDeviceSpecifications(String tuyaDeviceId) {
        return tuyaCloudService.getDeviceSpecifications(tuyaDeviceId);
    }

    /**
     * Get Tuya device current status.
     */
    public Map<String, Object> getDeviceStatus(String tuyaDeviceId) {
        return tuyaCloudService.getDeviceStatus(tuyaDeviceId);
    }

    // ===== Private helpers =====

    private String mapCategoryToDeviceType(String category) {
        if (category == null) return "Other";
        switch (category) {
            case "dj": case "xdd": case "fwd": case "tgq":
                return "Light";
            case "kg": case "cz": case "tdq":
                return "Switch";
            case "wsdcg": case "ldcg": case "pir": case "mcs":
            case "rqbj": case "ywbj":
                return "Sensor";
            case "fs":
                return "Fan";
            case "cl":
                return "Cover";
            case "ms":
                return "Lock";
            case "sp":
                return "Camera";
            case "kt": case "qn": case "cs": case "jsq": case "kj":
                return "Climate";
            case "sd":
                return "Robot";
            default:
                return "Other";
        }
    }

    private String getCategoryDisplayName(String category) {
        if (category == null) return "Unknown";
        Map<String, Object> cats = tuyaCloudService.getSupportedCategories();
        if (cats.containsKey(category)) {
            @SuppressWarnings("unchecked")
            Map<String, String> info = (Map<String, String>) cats.get(category);
            return info.get("name");
        }
        return category;
    }

    private void applyStatusToDevice(Device device, String code, String value) {
        switch (code) {
            case "switch_led": case "switch_1": case "switch":
                device.setStatus("true".equals(value) || "1".equals(value) ? 1 : 0);
                break;
            case "bright_value": case "bright_value_v2":
                try {
                    int v = Integer.parseInt(value);
                    // Tuya uses 10-1000 or 25-255 range, normalize to 1-100
                    if (v > 255) v = (int) Math.round(v / 10.0);
                    else if (v > 100) v = (int) Math.round(v * 100.0 / 255.0);
                    device.setBrightness(Math.max(1, Math.min(100, v)));
                } catch (Exception ignored) {}
                break;
            case "temp_value": case "temp_value_v2":
                try {
                    int v = Integer.parseInt(value);
                    // Normalize Tuya temp to 2700-6500K
                    int ct = (int) Math.round(2700 + (v / 1000.0) * (6500 - 2700));
                    device.setColorTemp(Math.max(2700, Math.min(6500, ct)));
                } catch (Exception ignored) {}
                break;
            case "colour_data":
                // colour_data is JSON with h, s, v - skip for now
                break;
        }
    }

    private void updateCatalogCounts() {
        List<Device> allDevices = deviceRepository.findAll();
        List<DeviceCatalog> catalogs = catalogRepository.findAll();

        for (DeviceCatalog cat : catalogs) {
            String catCode = cat.getCategoryCode();
            long count = allDevices.stream()
                    .filter(d -> "TUYA_CLOUD".equals(d.getProtocolType()))
                    .filter(d -> mapCategoryToDeviceType(catCode).equals(d.getDeviceType()))
                    .count();
            cat.setImportedCount((int) count);
            catalogRepository.save(cat);
        }
    }
}
