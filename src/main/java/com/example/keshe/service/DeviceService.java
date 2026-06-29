package com.example.keshe.service;

import com.example.keshe.entity.Device;
import java.util.List;

public interface DeviceService {

    List<Device> getAllDevices();

    Device getDeviceById(Long id);

    void controlDevice(Long deviceId, String command);

    Device createDevice(Device device);

    Device updateDevice(Long id, Device deviceDetails);

    void deleteDevice(Long id);

    void undoOperation(Long logId);
}
