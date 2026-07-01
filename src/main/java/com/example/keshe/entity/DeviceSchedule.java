package com.example.keshe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_schedule")
public class DeviceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "device_name", length = 50)
    private String deviceName;

    @Column(name = "action", nullable = false, length = 30)
    private String action; // "turn_on" or "turn_off"

    @Column(name = "schedule_type", nullable = false, length = 20)
    private String scheduleType; // "ONCE_ABSOLUTE" (specific datetime) or "ONCE_COUNTDOWN" (countdown)

    @Column(name = "execute_time")
    private LocalDateTime executeTime; // For ONCE_ABSOLUTE: exact datetime

    @Column(name = "countdown_minutes")
    private Integer countdownMinutes; // For ONCE_COUNTDOWN: minutes from creation

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "executed", nullable = false)
    private Boolean executed = false;

    @Column(name = "executed_time")
    private LocalDateTime executedTime;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "command_params", length = 500)
    private String commandParams; // JSON string for extra params (e.g., brightness, temperature)

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public LocalDateTime getExecuteTime() { return executeTime; }
    public void setExecuteTime(LocalDateTime executeTime) { this.executeTime = executeTime; }

    public Integer getCountdownMinutes() { return countdownMinutes; }
    public void setCountdownMinutes(Integer countdownMinutes) { this.countdownMinutes = countdownMinutes; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Boolean getExecuted() { return executed; }
    public void setExecuted(Boolean executed) { this.executed = executed; }

    public LocalDateTime getExecutedTime() { return executedTime; }
    public void setExecutedTime(LocalDateTime executedTime) { this.executedTime = executedTime; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getCommandParams() { return commandParams; }
    public void setCommandParams(String commandParams) { this.commandParams = commandParams; }
}
