package com.example.keshe.service;

import com.example.keshe.entity.DeviceSchedule;
import com.example.keshe.repository.DeviceScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DeviceScheduleService {

    private static final Logger log = LoggerFactory.getLogger(DeviceScheduleService.class);

    private final DeviceScheduleRepository scheduleRepository;
    private final DeviceServiceImpl deviceService;

    @Autowired
    public DeviceScheduleService(DeviceScheduleRepository scheduleRepository,
                                  DeviceServiceImpl deviceService) {
        this.scheduleRepository = scheduleRepository;
        this.deviceService = deviceService;
    }

    public DeviceSchedule createSchedule(DeviceSchedule schedule) {
        if (schedule.getCreateTime() == null) {
            schedule.setCreateTime(LocalDateTime.now());
        }
        schedule.setExecuted(false);
        schedule.setEnabled(true);

        // For countdown type, calculate executeTime from now + countdownMinutes
        if ("ONCE_COUNTDOWN".equals(schedule.getScheduleType()) && schedule.getCountdownMinutes() != null) {
            schedule.setExecuteTime(LocalDateTime.now().plusMinutes(schedule.getCountdownMinutes()));
        }

        if (schedule.getExecuteTime() == null) {
            throw new RuntimeException("必须设置执行时间");
        }

        return scheduleRepository.save(schedule);
    }

    public List<DeviceSchedule> getSchedulesByDevice(Long deviceId) {
        return scheduleRepository.findByDeviceIdOrderByCreateTimeDesc(deviceId);
    }

    public DeviceSchedule toggleSchedule(Long scheduleId) {
        DeviceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("定时任务不存在"));
        schedule.setEnabled(!schedule.getEnabled());
        return scheduleRepository.save(schedule);
    }

    public void deleteSchedule(Long scheduleId) {
        scheduleRepository.deleteById(scheduleId);
    }

    public DeviceSchedule getScheduleById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("定时任务不存在"));
    }

    /**
     * Scheduled task that runs every 30 seconds to check and execute due schedules
     */
    @Scheduled(fixedRate = 30000)
    public void executeDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        List<DeviceSchedule> dueSchedules = scheduleRepository.findDueSchedules(now);

        for (DeviceSchedule schedule : dueSchedules) {
            try {
                log.info("Executing schedule #{} for device {}: {}", schedule.getId(), schedule.getDeviceId(), schedule.getAction());

                // Execute the device command
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("action", schedule.getAction());

                // Parse extra command params if present
                if (schedule.getCommandParams() != null && !schedule.getCommandParams().isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> extra = mapper.readValue(schedule.getCommandParams(), Map.class);
                        params.putAll(extra);
                    } catch (Exception e) {
                        log.warn("Failed to parse command params for schedule #{}", schedule.getId());
                    }
                }

                deviceService.controlDeviceWithParams(schedule.getDeviceId(), params);

                // Mark as executed
                schedule.setExecuted(true);
                schedule.setExecutedTime(now);

                // For one-time schedules, disable after execution
                if ("ONCE_ABSOLUTE".equals(schedule.getScheduleType()) || "ONCE_COUNTDOWN".equals(schedule.getScheduleType())) {
                    schedule.setEnabled(false);
                }

                scheduleRepository.save(schedule);
                log.info("Schedule #{} executed successfully", schedule.getId());

            } catch (Exception e) {
                log.error("Failed to execute schedule #{}: {}", schedule.getId(), e.getMessage());
                // Disable failed schedule to avoid repeated failures
                schedule.setEnabled(false);
                scheduleRepository.save(schedule);
            }
        }
    }
}
