package com.example.keshe.repository;
import com.example.keshe.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndIsRead(Long userId, Integer isRead);

    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);

    Page<Notification> findByUserId(Long userId, Pageable pageable);
}
