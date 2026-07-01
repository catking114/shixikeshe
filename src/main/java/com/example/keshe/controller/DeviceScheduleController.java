package com.example.keshe.controller;

import com.example.keshe.entity.DeviceSchedule;
import com.example.keshe.service.DeviceScheduleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class DeviceScheduleController {

    private final DeviceScheduleService scheduleService;

    @Autowired
    public DeviceScheduleController(DeviceScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<DeviceSchedule>> getSchedulesByDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByDevice(deviceId));
    }

    @PostMapping
    public ResponseEntity<?> createSchedule(@RequestBody DeviceSchedule schedule, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", "仅管理员可以创建定时任务");
            return ResponseEntity.status(403).body(err);
        }
        try {
            DeviceSchedule created = scheduleService.createSchedule(schedule);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("schedule", created);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleSchedule(@PathVariable Long id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", "仅管理员可以操作定时任务");
            return ResponseEntity.status(403).body(err);
        }
        try {
            DeviceSchedule toggled = scheduleService.toggleSchedule(id);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("schedule", toggled);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id, HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", "仅管理员可以删除定时任务");
            return ResponseEntity.status(403).body(err);
        }
        try {
            scheduleService.deleteSchedule(id);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "定时任务已删除");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
