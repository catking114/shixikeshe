package com.example.keshe.controller;

import com.example.keshe.entity.Family;
import com.example.keshe.entity.User;
import com.example.keshe.service.FamilyService;
import com.example.keshe.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final FamilyService familyService;

    public AuthController(UserService userService, FamilyService familyService) {
        this.userService = userService;
        this.familyService = familyService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body, HttpSession session) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            String email = body.get("email");
            String role = body.get("role");
            String familyCode = body.get("familyCode");
            String familyRole = body.get("familyRole");

            if (username == null || password == null || email == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户名、密码、邮箱不能为空"));
            }

            User user = userService.register(username, password, email, role);

            // If family code provided, join the family
            if (familyCode != null && !familyCode.isEmpty()) {
                if (familyRole == null || familyRole.isEmpty()) {
                    familyRole = "成员";
                }
                Family family = familyService.joinFamily(user.getId(), familyCode, familyRole);
                user.setFamilyId(family.getId());
                user.setFamilyRole(familyRole);
            }

            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            session.setAttribute("familyId", user.getFamilyId());
            session.setAttribute("familyRole", user.getFamilyRole());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", user.getId());
            result.put("username", user.getUsername());
            result.put("role", user.getRole());
            result.put("familyId", user.getFamilyId());
            result.put("familyRole", user.getFamilyRole());
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body, HttpSession session) {
        try {
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
            }

            Map<String, Object> result = userService.login(username, password);

            session.setAttribute("userId", result.get("id"));
            session.setAttribute("username", result.get("username"));
            session.setAttribute("role", result.get("role"));
            session.setAttribute("familyId", result.get("familyId"));
            session.setAttribute("familyRole", result.get("familyRole"));

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (role == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", true);
        result.put("id", session.getAttribute("userId"));
        result.put("username", session.getAttribute("username"));
        result.put("role", role);
        result.put("familyId", session.getAttribute("familyId"));
        result.put("familyRole", session.getAttribute("familyRole"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }
}