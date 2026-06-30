package com.example.keshe.service;

import com.example.keshe.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DeviceService {

    Page<Device> getAllDevices(Pageable pageable);

    Page<Device> searchDevices(String brand, String deviceType, Integer status, String keyword, Pageable pageable);

    Device getDeviceById(Long id);

    void controlDevice(Long deviceId, String command);

    Device createDevice(Device device);

    Device updateDevice(Long id, Device deviceDetails);

    void deleteDevice(Long id);

    void undoOperation(Long logId);
}
