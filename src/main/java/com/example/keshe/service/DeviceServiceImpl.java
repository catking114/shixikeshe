package com.example.keshe.service;

import com.example.keshe.entity.Device;
import com.example.keshe.entity.Notification;
import com.example.keshe.entity.OperationLog;
import com.example.keshe.repository.DeviceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Autowired
    public DeviceServiceImpl(DeviceRepository deviceRepository,
                             OperationLogService operationLogService,
                             NotificationService notificationService,
                             ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.operationLogService = operationLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Device getDeviceById(Long id) {
        Optional<Device> device = deviceRepository.findById(id);
        return device.orElseThrow(() -> new RuntimeException("设备不存在，ID: " + id));
    }

    @Override
    public void controlDevice(Long deviceId, String command) {
        operationLogService.saveLog(deviceId, null, command, "执行指令: " + command);
    }

    @Override
    public Device createDevice(Device device) {
        Device savedDevice = deviceRepository.save(device);
        operationLogService.saveLog(savedDevice.getId(), null, "CREATE_DEVICE", "新增了设备: " + savedDevice.getDeviceName());
        checkBatteryAndNotify(savedDevice);
        return savedDevice;
    }

    @Override
    public Device updateDevice(Long id, Device deviceDetails) {
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("更新失败，未找到设备，ID: " + id));

        String undoData = toJson(existingDevice);

        existingDevice.setDeviceName(deviceDetails.getDeviceName());
        existingDevice.setBrand(deviceDetails.getBrand());
        existingDevice.setDeviceType(deviceDetails.getDeviceType());
        existingDevice.setStatus(deviceDetails.getStatus());
        existingDevice.setBatteryLevel(deviceDetails.getBatteryLevel());
        existingDevice.setRoomLocation(deviceDetails.getRoomLocation());

        Device updatedDevice = deviceRepository.save(existingDevice);

        operationLogService.saveLogWithUndo(updatedDevice.getId(), null, "UPDATE_DEVICE", "修改了设备信息", undoData);

        checkBatteryAndNotify(updatedDevice);

        return updatedDevice;
    }

    @Override
    public void deleteDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("删除失败，未找到设备，ID: " + id));

        String undoData = toJson(device);

        operationLogService.saveLogWithUndo(id, null, "DELETE_DEVICE", "删除了设备 ID: " + id, undoData);
        deviceRepository.deleteById(id);
    }

    @Override
    public void undoOperation(Long logId) {
        OperationLog log = operationLogService.findById(logId);

        switch (log.getCommand()) {
            case "CREATE_DEVICE":
                deviceRepository.deleteById(log.getDeviceId());
                break;

            case "UPDATE_DEVICE":
                Device previous = fromJson(log.getUndoData());
                previous.setId(log.getDeviceId());
                deviceRepository.save(previous);
                break;

            case "DELETE_DEVICE":
                Device deleted = fromJson(log.getUndoData());
                deleted.setId(null);
                deviceRepository.save(deleted);
                break;

            default:
                throw new RuntimeException("该操作无法撤销");
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败", e);
        }
    }

    private Device fromJson(String json) {
        try {
            return objectMapper.readValue(json, Device.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化失败", e);
        }
    }

    private void checkBatteryAndNotify(Device device) {
        Long targetUserId = 1L;

        if (device.getBatteryLevel() != null && device.getBatteryLevel() < 20) {
            Notification alert = new Notification();
            alert.setUserId(targetUserId);
            alert.setDeviceId(device.getId());
            alert.setMessageContent("警告：设备 [" + device.getDeviceName() + "] 电量过低，当前仅剩 " + device.getBatteryLevel() + "%！");
            alert.setIsRead(0);
            alert.setCreateTime(LocalDateTime.now());

            notificationService.save(alert);
        }
    }
}
