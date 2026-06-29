package com.example.keshe.controller;

import com.example.keshe.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            HttpSession session) {

        if (!isAdmin(session)) return forbidden();

        Integer isRead = request.get("isRead");
        if (isRead == null || isRead != 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "参数错误"));
        }

        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("role"));
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可执行此操作"));
    }
}
