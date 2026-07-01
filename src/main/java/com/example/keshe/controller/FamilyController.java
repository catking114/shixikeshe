package com.example.keshe.controller;

import com.example.keshe.entity.Family;
import com.example.keshe.entity.User;
import com.example.keshe.service.FamilyService;
import com.example.keshe.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/family")
public class FamilyController {

    private final FamilyService familyService;
    private final UserRepository userRepository;

    public FamilyController(FamilyService familyService, UserRepository userRepository) {
        this.familyService = familyService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createFamily(@RequestBody Map<String, String> body, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(403).body(Map.of("error", "仅管理员可以创建家庭"));
        }
        try {
            Long userId = (Long) session.getAttribute("userId");
            String creatorRole = body.get("creatorRole");
            String familyName = body.get("familyName");

            if (creatorRole == null || creatorRole.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请选择您的家庭身份"));
            }

            Family family = familyService.createFamily(userId, creatorRole, familyName);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("family", family);
            result.put("message", "家庭创建成功，家庭码: " + family.getFamilyCode());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinFamily(@RequestBody Map<String, String> body, HttpSession session) {
        try {
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
            }
            String familyCode = body.get("familyCode");
            String familyRole = body.get("familyRole");

            if (familyCode == null || familyCode.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请输入家庭码"));
            }
            if (familyRole == null || familyRole.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "请选择您的家庭身份"));
            }

            Family family = familyService.joinFamily(userId, familyCode, familyRole);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("family", family);
            result.put("message", "已成功加入家庭: " + family.getFamilyName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<?> getFamilyInfo(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        try {
            User user = userRepository.findById(userId).orElseThrow();
            if (user.getFamilyId() == null) {
                return ResponseEntity.ok(Map.of("hasFamily", false));
            }
            Family family = familyService.getFamilyById(user.getFamilyId());
            List<User> members = familyService.getFamilyMembers(family.getId());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("hasFamily", true);
            result.put("family", family);
            result.put("members", members.stream().map(m -> {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("id", m.getId());
                mm.put("username", m.getUsername());
                mm.put("familyRole", m.getFamilyRole());
                mm.put("role", m.getRole());
                return mm;
            }).toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("hasFamily", false));
        }
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leaveFamily(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "请先登录"));
        }
        try {
            familyService.leaveFamily(userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "已离开家庭"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
