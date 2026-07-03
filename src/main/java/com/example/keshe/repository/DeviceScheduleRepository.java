package com.example.keshe.repository;

import com.example.keshe.entity.DeviceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeviceScheduleRepository extends JpaRepository<DeviceSchedule, Long> {

    List<DeviceSchedule> findByDeviceIdOrderByCreateTimeDesc(Long deviceId);

    List<DeviceSchedule> findByEnabledTrueAndExecutedFalse();

    @Query("SELECT s FROM DeviceSchedule s WHERE s.enabled = true AND s.executed = false AND s.executeTime <= :now")
    List<DeviceSchedule> findDueSchedules(@Param("now") LocalDateTime now);
}
