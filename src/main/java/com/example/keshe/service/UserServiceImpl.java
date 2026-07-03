package com.example.keshe.service;

import com.example.keshe.entity.User;
import com.example.keshe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getFamilyUsers(Long familyId, Pageable pageable) {
        return userRepository.findByFamilyId(familyId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Override
    public User createUser(User user, String rawPassword) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            user.setPassword(hashPassword("123456"));
        } else {
            user.setPassword(hashPassword(rawPassword));
        }
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        if (user.getCreateTime() == null) {
            user.setCreateTime(LocalDateTime.now());
        }
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, User userDetails, String rawPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("用户不存在"));
        if (userDetails.getUsername() != null) {
            user.setUsername(userDetails.getUsername());
        }
        if (userDetails.getEmail() != null) {
            user.setEmail(userDetails.getEmail());
        }
        if (userDetails.getRole() != null) {
            user.setRole(userDetails.getRole());
        }
        if (userDetails.getFamilyRole() != null) {
            user.setFamilyRole(userDetails.getFamilyRole());
        }
        if (userDetails.getFamilyId() != null) {
            user.setFamilyId(userDetails.getFamilyId());
        }
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(hashPassword(rawPassword));
        }
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    @Override
    public User register(String username, String password, String email, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashPassword(password));
        user.setEmail(email);
        user.setRole(role != null && role.equals("ADMIN") ? "ADMIN" : "USER");
        user.setCreateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        String hashedInput = hashPassword(password);
        boolean matches = hashedInput.equals(user.getPassword());

        if (!matches) {
            throw new RuntimeException("用户名或密码错误");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("familyId", user.getFamilyId());
        result.put("familyRole", user.getFamilyRole());
        return result;
    }

    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not available", e);
        }
    }
}
