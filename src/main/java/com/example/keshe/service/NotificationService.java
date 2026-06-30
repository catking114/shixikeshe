package com.example.keshe.service;

import com.example.keshe.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NotificationService {
    void save(Notification notification);

    Page<Notification> findAll(Pageable pageable);
    void markAsRead(Long id);
}
