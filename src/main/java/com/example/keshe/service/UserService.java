package com.example.keshe.service;

import com.example.keshe.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

public interface UserService {
    Page<User> getAllUsers(Pageable pageable);
    Page<User> getFamilyUsers(Long familyId, Pageable pageable);
    User getUserById(Long id);
    User createUser(User user, String rawPassword);
    User updateUser(Long id, User userDetails, String rawPassword);
    void deleteUser(Long id);

    User register(String username, String password, String email, String role);
    Map<String, Object> login(String username, String password);
    User getByUsername(String username);
}
