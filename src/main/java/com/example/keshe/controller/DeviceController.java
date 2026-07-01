package com.example.keshe.controller;

import com.example.keshe.entity.Device;
import com.example.keshe.entity.DeviceCatalog;
import com.example.keshe.entity.Notification;
import com.example.keshe.entity.OperationLog;
import com.example.keshe.repository.UserRepository;
import com.example.keshe.service.DeviceService;
import com.example.keshe.service.NotificationService;
import com.example.keshe.service.OperationLogService;
import com.example.keshe.service.TuyaCloudService;
import com.example.keshe.service.TuyaImportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final OperationLogService logService;
    private final NotificationService notificationService;
    private final TuyaCloudService tuyaCloudService;
    private final TuyaImportService tuyaImportService;
    private final UserRepository userRepository;

    public DeviceController(DeviceService deviceService,
                            OperationLogService logService,
                            NotificationService notificationService,
                            TuyaCloudService tuyaCloudService,
                            TuyaImportService tuyaImportService,
                            UserRepository userRepository) {
        this.deviceService = deviceService;
        this.logService = logService;
        this.notificationService = notificationService;
        this.tuyaCloudService = tuyaCloudService;
        this.tuyaImportService = tuyaImportService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Device>> getAllDevices(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            HttpSession session) {
        Long familyId = (Long) session.getAttribute("familyId");
        return ResponseEntity.ok(deviceService.searchDevices(familyId, brand, deviceType, status, keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id, HttpSession session) {
        try {
            Device device = deviceService.getDeviceById(id);
            Long familyId = (Long) session.getAttribute("familyId");
            if (familyId != null && !familyId.equals(device.getFamilyId())) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(device);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createDevice(@RequestBody Device device, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        Long familyId = (Long) session.getAttribute("familyId");
        if (familyId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请先创建或加入一个家庭"));
        }
        try {
            if (device.getDeviceName() == null || device.getBrand() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "设备名称和品牌不能为空"));
            }
            if (device.getCreateTime() == null) {
                device.setCreateTime(LocalDateTime.now());
            }
            if (device.getStatus() == null) {
                device.setStatus(0);
            }
            if (device.getBatteryLevel() == null) {
                device.setBatteryLevel(100);
            }
            device.setFamilyId(familyId);
            Device savedDevice = deviceService.createDevice(device);
            return ResponseEntity.ok(savedDevice);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "保存失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDevice(@PathVariable Long id, @RequestBody Device deviceDetails, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            if (!checkDeviceFamily(id, session)) {
                return ResponseEntity.status(403).body(Map.of("error", "无权操作其他家庭的设备"));
            }
            Device updatedDevice = deviceService.updateDevice(id, deviceDetails);
            return ResponseEntity.ok(updatedDevice);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDevice(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            if (!checkDeviceFamily(id, session)) {
                return ResponseEntity.status(403).body(Map.of("error", "无权操作其他家庭的设备"));
            }
            deviceService.deleteDevice(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "删除失败"));
        }
    }

    @PostMapping("/control")
    public ResponseEntity<?> controlDevice(@RequestBody Map<String, Object> request, HttpSession session) {
        // Check if user is logged in
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return forbidden();

        try {
            if (!request.containsKey("deviceId") || !request.containsKey("command")) {
                return ResponseEntity.badRequest().body("参数缺失");
            }
            Long deviceId = Long.valueOf(request.get("deviceId").toString());
            String command = request.get("command").toString();

            // Check family ownership
            if (!checkDeviceFamily(deviceId, session)) {
                return ResponseEntity.status(403).body(Map.of("error", "无权操作其他家庭的设备"));
            }

            // Non-admin users can only do on/off/toggle
            if (!isAdmin(session)) {
                if (!"turn_on".equals(command) && !"turn_off".equals(command) && !"toggle".equals(command)) {
                    return forbidden();
                }
            }

            deviceService.controlDevice(deviceId, command);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("控制失败: " + e.getMessage());
        }
    }

    /**
     * Detailed light/device control with brightness, color temp, RGB
     */
    @PostMapping("/{id}/control-light")
    public ResponseEntity<?> controlLight(@PathVariable Long id, @RequestBody Map<String, Object> params, HttpSession session) {
        // Check if user is logged in
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return forbidden();

        try {
            // Check family ownership
            if (!checkDeviceFamily(id, session)) {
                return ResponseEntity.status(403).body(Map.of("error", "无权操作其他家庭的设备"));
            }

            // Non-admin users can only do on/off/toggle
            if (!isAdmin(session)) {
                String action = (String) params.getOrDefault("action", "");
                if (!"turn_on".equals(action) && !"turn_off".equals(action) && !"toggle".equals(action)) {
                    return forbidden();
                }
            }
            Map<String, Object> result = deviceService.controlDeviceWithParams(id, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "控制失败: " + e.getMessage()));
        }
    }

    /**
     * SSDP scan for Yeelight devices on local network
     */
    @PostMapping("/discover")
    public ResponseEntity<?> discoverDevices(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            List<Map<String, Object>> devices = deviceService.discoverDevices();
            return ResponseEntity.ok(Map.of("devices", devices, "count", devices.size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "扫描失败: " + e.getMessage()));
        }
    }

    /**
     * Query real-time status from physical device
     */
    @GetMapping("/{id}/live-status")
    public ResponseEntity<?> getDeviceLiveStatus(@PathVariable Long id) {
        try {
            Map<String, Object> status = deviceService.getDeviceLiveStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "查询失败: " + e.getMessage()));
        }
    }

    /**
     * Platform integration status (Tuya, Yeelight, etc.)
     */
    @GetMapping("/platform-status")
    public ResponseEntity<?> getPlatformStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("tuya", tuyaCloudService.getConfigStatus());
        status.put("yeelight", Map.of("available", true, "protocol", "LAN TCP"));
        return ResponseEntity.ok(status);
    }

    // ===== Tuya Import & Catalog Endpoints =====

    /**
     * Import all devices from the Tuya user account into the local database.
     */
    @PostMapping("/tuya/import")
    public ResponseEntity<?> importTuyaDevices(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            Map<String, Object> result = tuyaImportService.importUserDevices();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "导入失败: " + e.getMessage()));
        }
    }

    /**
     * Sync Tuya device categories (with optional instruction set fetch).
     */
    @PostMapping("/tuya/sync-categories")
    public ResponseEntity<?> syncTuyaCategories(
            @RequestParam(defaultValue = "false") boolean fetchInstructions,
            HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            Map<String, Object> result = tuyaImportService.syncCategories(fetchInstructions);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "同步失败: " + e.getMessage()));
        }
    }

    /**
     * List all synced Tuya categories from the database.
     */
    @GetMapping("/tuya/categories")
    public ResponseEntity<?> getTuyaCategories() {
        try {
            List<DeviceCatalog> categories = tuyaImportService.getAllCategories();
            // Also include the built-in catalog if DB is empty
            if (categories.isEmpty()) {
                Map<String, Object> builtIn = tuyaCloudService.getSupportedCategories();
                return ResponseEntity.ok(Map.of("source", "builtin", "categories", builtIn, "count", builtIn.size()));
            }
            return ResponseEntity.ok(Map.of("source", "database", "categories", categories, "count", categories.size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取品类失败: " + e.getMessage()));
        }
    }

    /**
     * Get detail for a specific Tuya category (instruction set).
     */
    @GetMapping("/tuya/categories/{categoryCode}")
    public ResponseEntity<?> getTuyaCategoryDetail(@PathVariable String categoryCode) {
        try {
            Map<String, Object> result = tuyaImportService.getCategoryDetail(categoryCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取品类详情失败: " + e.getMessage()));
        }
    }

    /**
     * Get Tuya device functions by external device ID.
     */
    @GetMapping("/tuya/{externalDeviceId}/functions")
    public ResponseEntity<?> getTuyaDeviceFunctions(@PathVariable String externalDeviceId) {
        try {
            Map<String, Object> result = tuyaImportService.getDeviceFunctions(externalDeviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取功能列表失败: " + e.getMessage()));
        }
    }

    /**
     * Get Tuya device specifications.
     */
    @GetMapping("/tuya/{externalDeviceId}/specifications")
    public ResponseEntity<?> getTuyaDeviceSpecifications(@PathVariable String externalDeviceId) {
        try {
            Map<String, Object> result = tuyaImportService.getDeviceSpecifications(externalDeviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取规格失败: " + e.getMessage()));
        }
    }

    /**
     * Get Tuya device current status (live data points).
     */
    @GetMapping("/tuya/{externalDeviceId}/status")
    public ResponseEntity<?> getTuyaDeviceStatus(@PathVariable String externalDeviceId) {
        try {
            Map<String, Object> result = tuyaImportService.getDeviceStatus(externalDeviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "获取状态失败: " + e.getMessage()));
        }
    }

    @PostMapping("/undo/{logId}")
    public ResponseEntity<?> undoOperation(@PathVariable Long logId, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            deviceService.undoOperation(logId);
            return ResponseEntity.ok(Map.of("message", "撤销成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "撤销失败: " + e.getMessage()));
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<OperationLog>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(logService.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/notifications")
    public ResponseEntity<Page<Notification>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(notificationService.findAll(PageRequest.of(page, size)));
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("role"));
    }

    private boolean checkDeviceFamily(Long deviceId, HttpSession session) {
        Long userFamilyId = (Long) session.getAttribute("familyId");
        if (userFamilyId == null) {
            return false;
        }
        try {
            Device device = deviceService.getDeviceById(deviceId);
            return userFamilyId.equals(device.getFamilyId());
        } catch (Exception e) {
            return false;
        }
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可执行此操作"));
    }
}
