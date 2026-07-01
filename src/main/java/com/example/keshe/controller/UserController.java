package com.example.keshe.controller;

import com.example.keshe.entity.User;
import com.example.keshe.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            HttpSession session,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        if (!isAdmin(session)) return forbidden();
        Long familyId = (Long) session.getAttribute("familyId");
        if (familyId != null) {
            return ResponseEntity.ok(userService.getFamilyUsers(familyId, PageRequest.of(page, size)));
        }
        return ResponseEntity.ok(userService.getAllUsers(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id, HttpSession session) {
        try {
            Long familyId = (Long) session.getAttribute("familyId");
            User user = userService.getUserById(id);
            // Non-admin users can only see their own info
            if (!isAdmin(session)) {
                Long currentUserId = (Long) session.getAttribute("userId");
                if (!id.equals(currentUserId)) {
                    return forbidden();
                }
            } else if (familyId != null && !familyId.equals(user.getFamilyId())) {
                // Admin can only see users in their own family
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            Long familyId = (Long) session.getAttribute("familyId");
            user.setFamilyId(familyId);
            User createdUser = userService.createUser(user);
            return ResponseEntity.ok(createdUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            Long familyId = (Long) session.getAttribute("familyId");
            if (familyId != null) {
                User target = userService.getUserById(id);
                if (!familyId.equals(target.getFamilyId())) {
                    return ResponseEntity.status(403).body(Map.of("error", "无权操作其他家庭的用户"));
                }
            }
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        try {
            Long familyId = (Long) session.getAttribute("familyId");
            if (familyId != null) {
                User target = userService.getUserById(id);
                if (!familyId.equals(target.getFamilyId())) {
                    return ResponseEntity.status(403).body(Map.of("error", "无权操作其他家庭的用户"));
                }
            }
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("role"));
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(403).body(Map.of("error", "无权限，仅管理员可执行此操作"));
    }
}
