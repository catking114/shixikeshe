package com.example.keshe.service;

import com.example.keshe.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

public interface DeviceService {

    Page<Device> getAllDevices(Long familyId, Pageable pageable);

    Page<Device> searchDevices(Long familyId, String brand, String deviceType, Integer status, String keyword, Pageable pageable);

    Device getDeviceById(Long id);

    void controlDevice(Long deviceId, String command);

    /**
     * Real IoT device control with detailed parameters
     */
    Map<String, Object> controlDeviceWithParams(Long deviceId, Map<String, Object> params);

    /**
     * Discover Yeelight devices on local network via SSDP
     */
    List<Map<String, Object>> discoverDevices();

    /**
     * Query real device status from the physical device
     */
    Map<String, Object> getDeviceLiveStatus(Long deviceId);

    Device createDevice(Device device);

    Device updateDevice(Long id, Device deviceDetails);

    void deleteDevice(Long id);

    void undoOperation(Long logId);
}
