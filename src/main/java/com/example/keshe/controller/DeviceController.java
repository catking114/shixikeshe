package com.example.keshe.controller;

import com.example.keshe.entity.Device;
import com.example.keshe.entity.Notification;
import com.example.keshe.entity.OperationLog;
import com.example.keshe.service.DeviceService;
import com.example.keshe.service.NotificationService;
import com.example.keshe.service.OperationLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final OperationLogService logService;
    private final NotificationService notificationService;

    public DeviceController(DeviceService deviceService,
                            OperationLogService logService,
                            NotificationService notificationService) {
        this.deviceService = deviceService;
        this.logService = logService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<Device>> getAllDevices(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(deviceService.searchDevices(brand, deviceType, status, keyword, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id) {
        try {
            Device device = deviceService.getDeviceById(id);
            return ResponseEntity.ok(device);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createDevice(@RequestBody Device device, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            if (device.getDeviceName() == null || device.getBrand() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "设备名称和品牌不能为空"));
            }
            if (device.getCreateTime() == null) {
                device.setCreateTime(LocalDateTime.now());
            }
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
            deviceService.deleteDevice(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", "删除失败"));
        }
    }

    @PostMapping("/control")
    public ResponseEntity<?> controlDevice(@RequestBody Map<String, Object> request, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            if (!request.containsKey("deviceId") || !request.containsKey("command")) {
                return ResponseEntity.badRequest().body("参数缺失");
            }
            Long deviceId = Long.valueOf(request.get("deviceId").toString());
            String command = request.get("command").toString();

            deviceService.controlDevice(deviceId, command);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("控制失败: " + e.getMessage());
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

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可执行此操作"));
    }
}
