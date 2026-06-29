package com.example.keshe.service;

import com.example.keshe.entity.Notification;
import java.util.List;

public interface NotificationService {
    void save(Notification notification);

    List<Notification> findAll();
    void markAsRead(Long id);
}
