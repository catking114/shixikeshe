package com.example.keshe.service;

import com.example.keshe.entity.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User createUser(User user);
    User updateUser(Long id, User userDetails);
    void deleteUser(Long id);

    User register(String username, String password, String email, String role);
    Map<String, Object> login(String username, String password);
    User getByUsername(String username);
}
