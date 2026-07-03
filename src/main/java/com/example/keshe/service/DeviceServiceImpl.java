package com.example.keshe.service;

import com.example.keshe.config.DeviceWebSocketHandler;
import com.example.keshe.entity.Device;
import com.example.keshe.entity.Notification;
import com.example.keshe.entity.OperationLog;
import com.example.keshe.entity.User;
import com.example.keshe.repository.DeviceRepository;
import com.example.keshe.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);

    private final DeviceRepository deviceRepository;
    private final OperationLogService operationLogService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final YeelightService yeelightService;
    private final TuyaCloudService tuyaCloudService;
    private final DeviceWebSocketHandler webSocketHandler;
    private final UserRepository userRepository;

    @Value("${yeelight.discovery-timeout:5}")
    private int discoveryTimeout;

    @Autowired
    public DeviceServiceImpl(DeviceRepository deviceRepository,
                             OperationLogService operationLogService,
                             NotificationService notificationService,
                             ObjectMapper objectMapper,
                             YeelightService yeelightService,
                             TuyaCloudService tuyaCloudService,
                             DeviceWebSocketHandler webSocketHandler,
                             UserRepository userRepository) {
        this.deviceRepository = deviceRepository;
        this.operationLogService = operationLogService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.yeelightService = yeelightService;
        this.tuyaCloudService = tuyaCloudService;
        this.webSocketHandler = webSocketHandler;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Device> getAllDevices(Long familyId, Pageable pageable) {
        if (familyId != null) {
            return deviceRepository.searchDevices(familyId, null, null, null, null, pageable);
        }
        return deviceRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Device> searchDevices(Long familyId, String brand, String deviceType, Integer status, String keyword, Pageable pageable) {
        if (familyId == null) {
            return Page.empty(pageable);
        }
        return deviceRepository.searchDevices(
            familyId,
            brand == null || brand.isEmpty() ? null : brand,
            deviceType == null || deviceType.isEmpty() ? null : deviceType,
            status,
            keyword == null || keyword.isEmpty() ? null : keyword,
            pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Device getDeviceById(Long id) {
        Optional<Device> device = deviceRepository.findById(id);
        return device.orElseThrow(() -> new RuntimeException("设备不存在，ID: " + id));
    }

    @Override
    public void controlDevice(Long deviceId, String command) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        String protocol = device.getProtocolType();
        if (protocol == null) protocol = "MANUAL";

        Map<String, Object> result;
        switch (protocol) {
            case "YEELIGHT_LAN":
                result = executeYeelightCommand(device, command, null);
                break;
            case "TUYA_CLOUD":
                Map<String, Object> tuyaParams = new LinkedHashMap<>();
                tuyaParams.put("action", command);
                result = executeTuyaControl(device, tuyaParams);
                break;
            default:
                Map<String, Object> manualParams = new LinkedHashMap<>();
                manualParams.put("action", command);
                result = executeManualControl(device, manualParams);
                break;
        }

        boolean success = Boolean.TRUE.equals(result.get("success"));
        String paramStr = "执行指令: " + command + (success ? " (成功)" : " (失败: " + result.get("error") + ")");
        operationLogService.saveLog(deviceId, null, command, paramStr);

        if (success) {
            updateDeviceStatusFromCommand(device, command);
            broadcastStatusChange(device);
        }
    }

    @Override
    public Map<String, Object> controlDeviceWithParams(Long deviceId, Map<String, Object> params) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        String protocol = device.getProtocolType();
        if (protocol == null) protocol = "MANUAL";

        Map<String, Object> result;

        switch (protocol) {
            case "YEELIGHT_LAN":
                result = executeYeelightControl(device, params);
                break;
            case "TUYA_CLOUD":
                result = executeTuyaControl(device, params);
                break;
            default:
                // MANUAL mode - just update database
                result = executeManualControl(device, params);
                break;
        }

        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (success) {
            deviceRepository.save(device);
            broadcastStatusChange(device);
        }

        String action = (String) params.getOrDefault("action", "control");
        operationLogService.saveLog(deviceId, null, action,
                "控制设备: " + action + (success ? " (成功)" : " (失败: " + result.get("error") + ")"));

        return result;
    }

    @Override
    public List<Map<String, Object>> discoverDevices() {
        log.info("Starting SSDP device discovery (timeout: {}s)", discoveryTimeout);
        List<Map<String, Object>> discovered = yeelightService.discoverDevices(discoveryTimeout);
        log.info("Discovery completed, found {} devices", discovered.size());
        return discovered;
    }

    @Override
    public Map<String, Object> getDeviceLiveStatus(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        String protocol = device.getProtocolType();
        if ("YEELIGHT_LAN".equals(protocol) && device.getIpAddress() != null) {
            int port = device.getPort() != null ? device.getPort() : 55443;
            Map<String, Object> props = yeelightService.getProps(device.getIpAddress(), port);

            if (Boolean.TRUE.equals(props.get("success"))) {
                // Update device from live data
                @SuppressWarnings("unchecked")
                List<Object> resultList = (List<Object>) props.get("result");
                if (resultList != null && resultList.size() >= 6) {
                    String power = String.valueOf(resultList.get(0));
                    device.setStatus("on".equals(power) ? 1 : 0);
                    try {
                        device.setBrightness(Integer.parseInt(String.valueOf(resultList.get(1))));
                    } catch (Exception ignored) {}
                    try {
                        device.setColorTemp(Integer.parseInt(String.valueOf(resultList.get(2))));
                    } catch (Exception ignored) {}
                    try {
                        device.setRgbColor(Integer.parseInt(String.valueOf(resultList.get(3))));
                    } catch (Exception ignored) {}
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                    deviceRepository.save(device);
                    broadcastStatusChange(device);
                }
                props.put("synced", true);
            } else {
                device.setOnlineStatus(0);
                deviceRepository.save(device);
                props.put("synced", false);
            }
            return props;
        }

        if ("TUYA_CLOUD".equals(protocol) && device.getExternalDeviceId() != null) {
            if (!tuyaCloudService.isConfigured()) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("success", false);
                err.put("error", "Tuya Cloud 未配置");
                return err;
            }
            Map<String, Object> tuyaStatus = tuyaCloudService.getDeviceStatus(device.getExternalDeviceId());
            if (Boolean.TRUE.equals(tuyaStatus.get("success"))) {
                try {
                    String responseJson = (String) tuyaStatus.get("response");
                    com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseJson);
                    if (json.has("result") && json.get("result").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode s : json.get("result")) {
                            String code = s.path("code").asText();
                            String value = s.path("value").asText();
                            switch (code) {
                                case "switch_led": case "switch_1": case "switch":
                                    device.setStatus("true".equals(value) ? 1 : 0);
                                    break;
                                case "bright_value": case "bright_value_v2":
                                    int bv = (int) Double.parseDouble(value);
                                    if (bv > 255) bv = (int) Math.round(bv / 10.0);
                                    else if (bv > 100) bv = (int) Math.round(bv * 100.0 / 255.0);
                                    device.setBrightness(Math.max(1, Math.min(100, bv)));
                                    break;
                                case "temp_value": case "temp_value_v2":
                                    int tv = (int) Double.parseDouble(value);
                                    device.setColorTemp((int) Math.round(2700 + (tv / 1000.0) * (6500 - 2700)));
                                    break;
                            }
                        }
                    }
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                    deviceRepository.save(device);
                    broadcastStatusChange(device);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("success", true);
                    result.put("source", "tuya_cloud");
                    result.put("synced", true);
                    result.put("status", device.getStatus());
                    result.put("brightness", device.getBrightness());
                    result.put("colorTemp", device.getColorTemp());
                    return result;
                } catch (Exception e) {
                    log.warn("Parse Tuya status failed for device {}", device.getExternalDeviceId(), e);
                }
            } else {
                device.setOnlineStatus(0);
                deviceRepository.save(device);
            }
            tuyaStatus.put("synced", false);
            return tuyaStatus;
        }

        // For non-network devices, return cached status
        Map<String, Object> cached = new LinkedHashMap<>();
        cached.put("success", true);
        cached.put("source", "database");
        cached.put("status", device.getStatus());
        cached.put("brightness", device.getBrightness());
        cached.put("colorTemp", device.getColorTemp());
        cached.put("rgbColor", device.getRgbColor());
        cached.put("online", device.getOnlineStatus());
        return cached;
    }

    @Override
    public Device createDevice(Device device) {
        if (device.getCreateTime() == null) {
            device.setCreateTime(LocalDateTime.now());
        }
        if (device.getBrightness() == null) {
            device.setBrightness(100);
        }
        if (device.getColorTemp() == null) {
            device.setColorTemp(4000);
        }

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

        if (deviceDetails.getDeviceName() != null) existingDevice.setDeviceName(deviceDetails.getDeviceName());
        if (deviceDetails.getBrand() != null) existingDevice.setBrand(deviceDetails.getBrand());
        if (deviceDetails.getDeviceType() != null) existingDevice.setDeviceType(deviceDetails.getDeviceType());
        if (deviceDetails.getStatus() != null) existingDevice.setStatus(deviceDetails.getStatus());
        if (deviceDetails.getBatteryLevel() != null) existingDevice.setBatteryLevel(deviceDetails.getBatteryLevel());
        if (deviceDetails.getRoomLocation() != null) existingDevice.setRoomLocation(deviceDetails.getRoomLocation());
        if (deviceDetails.getIpAddress() != null) existingDevice.setIpAddress(deviceDetails.getIpAddress());
        if (deviceDetails.getPort() != null) existingDevice.setPort(deviceDetails.getPort());
        if (deviceDetails.getProtocolType() != null) existingDevice.setProtocolType(deviceDetails.getProtocolType());
        if (deviceDetails.getExternalDeviceId() != null) existingDevice.setExternalDeviceId(deviceDetails.getExternalDeviceId());
        if (deviceDetails.getModel() != null) existingDevice.setModel(deviceDetails.getModel());
        if (deviceDetails.getBrightness() != null) existingDevice.setBrightness(deviceDetails.getBrightness());
        if (deviceDetails.getColorTemp() != null) existingDevice.setColorTemp(deviceDetails.getColorTemp());
        if (deviceDetails.getRgbColor() != null) existingDevice.setRgbColor(deviceDetails.getRgbColor());

        Device updatedDevice = deviceRepository.save(existingDevice);
        operationLogService.saveLogWithUndo(updatedDevice.getId(), null, "UPDATE_DEVICE", "修改了设备信息", undoData);
        checkBatteryAndNotify(updatedDevice);
        broadcastStatusChange(updatedDevice);

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

    // ===== Private: Protocol-specific control =====

    private Map<String, Object> executeYeelightControl(Device device, Map<String, Object> params) {
        if (device.getIpAddress() == null || device.getIpAddress().isEmpty()) {
            return Map.of("success", false, "error", "设备未配置 IP 地址");
        }

        String ip = device.getIpAddress();
        int port = device.getPort() != null ? device.getPort() : 55443;
        String action = (String) params.getOrDefault("action", "unknown");

        Map<String, Object> result;
        switch (action) {
            case "turn_on":
                result = yeelightService.turnOn(ip, port);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    device.setStatus(1);
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                }
                break;
            case "turn_off":
                result = yeelightService.turnOff(ip, port);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    device.setStatus(0);
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                }
                break;
            case "toggle":
                result = yeelightService.toggle(ip, port);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    device.setStatus(device.getStatus() != null && device.getStatus() == 1 ? 0 : 1);
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                }
                break;
            case "set_brightness":
                int brightness = ((Number) params.getOrDefault("brightness", 50)).intValue();
                result = yeelightService.setBrightness(ip, port, brightness);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    device.setBrightness(brightness);
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                }
                break;
            case "set_color_temp":
                int colorTemp = ((Number) params.getOrDefault("colorTemp", 4000)).intValue();
                result = yeelightService.setColorTemp(ip, port, colorTemp);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    device.setColorTemp(colorTemp);
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                }
                break;
            case "set_rgb":
                int r = ((Number) params.getOrDefault("r", 255)).intValue();
                int g = ((Number) params.getOrDefault("g", 255)).intValue();
                int b = ((Number) params.getOrDefault("b", 255)).intValue();
                result = yeelightService.setRGB(ip, port, r, g, b);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    device.setRgbColor(r * 65536 + g * 256 + b);
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                }
                break;
            case "set_hsv":
                int hue = ((Number) params.getOrDefault("hue", 0)).intValue();
                int sat = ((Number) params.getOrDefault("saturation", 100)).intValue();
                result = yeelightService.setHSV(ip, port, hue, sat);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    device.setOnlineStatus(1);
                    device.setLastSeen(LocalDateTime.now());
                }
                break;
            default:
                result = Map.of("success", false, "error", "未知的灯光控制动作: " + action);
        }

        return result;
    }

    private Map<String, Object> executeTuyaControl(Device device, Map<String, Object> params) {
        if (!tuyaCloudService.isConfigured()) {
            return Map.of("success", false, "error", "Tuya 云服务未配置");
        }
        if (device.getExternalDeviceId() == null || device.getExternalDeviceId().isEmpty()) {
            return Map.of("success", false, "error", "设备未配置 Tuya 设备ID");
        }

        String action = (String) params.getOrDefault("action", "unknown");
        String deviceId = device.getExternalDeviceId();
        String deviceType = device.getDeviceType() != null ? device.getDeviceType() : "";

        // Determine the correct switch code based on device type
        String switchCode;
        switch (deviceType) {
            case "Light": switchCode = "switch_led"; break;
            case "Fan": case "Climate": switchCode = "switch"; break;
            default: switchCode = "switch"; break; // Switch, Cover, etc.
        }

        switch (action) {
            case "turn_on":
                Map<String, Object> r = tuyaCloudService.sendCommand(deviceId, switchCode, true);
                if (Boolean.TRUE.equals(r.get("success"))) device.setStatus(1);
                return r;
            case "turn_off":
                Map<String, Object> r2 = tuyaCloudService.sendCommand(deviceId, switchCode, false);
                if (Boolean.TRUE.equals(r2.get("success"))) device.setStatus(0);
                return r2;
            case "toggle":
                boolean isOn = device.getStatus() != null && device.getStatus() == 1;
                Map<String, Object> rt = tuyaCloudService.sendCommand(deviceId, switchCode, !isOn);
                if (Boolean.TRUE.equals(rt.get("success"))) device.setStatus(isOn ? 0 : 1);
                return rt;
            case "set_brightness":
                int bright = ((Number) params.getOrDefault("brightness", 50)).intValue();
                int tuyaBright = (int) Math.round(bright * 255.0 / 100.0);
                Map<String, Object> r3 = tuyaCloudService.sendCommand(deviceId, "bright_value", tuyaBright);
                if (Boolean.TRUE.equals(r3.get("success"))) device.setBrightness(bright);
                return r3;
            case "set_color_temp":
                int ct = ((Number) params.getOrDefault("colorTemp", 4000)).intValue();
                int tuyaTemp = (int) Math.round((ct - 2700.0) / (6500.0 - 2700.0) * 1000.0);
                Map<String, Object> r4 = tuyaCloudService.sendCommand(deviceId, "temp_value", tuyaTemp);
                if (Boolean.TRUE.equals(r4.get("success"))) device.setColorTemp(ct);
                return r4;
            case "set_fan_speed":
                String speed = (String) params.getOrDefault("speed", "mid");
                Map<String, Object> r5 = tuyaCloudService.sendCommand(deviceId, "fan_speed", speed);
                return r5;
            case "set_mode":
                String mode = (String) params.getOrDefault("mode", "auto");
                Map<String, Object> r6 = tuyaCloudService.sendCommand(deviceId, "mode", mode);
                return r6;
            case "set_temperature":
                int temp = ((Number) params.getOrDefault("temperature", 26)).intValue();
                Map<String, Object> r7 = tuyaCloudService.sendCommand(deviceId, "temp_set", temp);
                return r7;
            case "open_cover":
                Map<String, Object> r8 = tuyaCloudService.sendCommand(deviceId, "control", "open");
                return r8;
            case "close_cover":
                Map<String, Object> r9 = tuyaCloudService.sendCommand(deviceId, "control", "close");
                return r9;
            case "stop_cover":
                Map<String, Object> r10 = tuyaCloudService.sendCommand(deviceId, "control", "stop");
                return r10;
            case "set_cover_position":
                int position = ((Number) params.getOrDefault("position", 50)).intValue();
                Map<String, Object> r11 = tuyaCloudService.sendCommand(deviceId, "percent_control", position);
                return r11;
            default:
                return Map.of("success", false, "error", "未知的 Tuya 控制动作: " + action);
        }
    }

    private Map<String, Object> executeManualControl(Device device, Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "unknown");
        switch (action) {
            case "turn_on":
                device.setStatus(1);
                break;
            case "turn_off":
                device.setStatus(0);
                break;
            case "toggle":
                device.setStatus(device.getStatus() == 1 ? 0 : 1);
                break;
            case "set_brightness":
                device.setBrightness(((Number) params.getOrDefault("brightness", 50)).intValue());
                break;
            case "set_color_temp":
                device.setColorTemp(((Number) params.getOrDefault("colorTemp", 4000)).intValue());
                break;
            case "set_rgb":
                int r = ((Number) params.getOrDefault("r", 255)).intValue();
                int g = ((Number) params.getOrDefault("g", 255)).intValue();
                int b = ((Number) params.getOrDefault("b", 255)).intValue();
                device.setRgbColor(r * 65536 + g * 256 + b);
                break;
        }
        return Map.of("success", true, "message", "已更新设备状态(本地模式)");
    }

    // ===== Private: Helpers =====

    private Map<String, Object> executeYeelightCommand(Device device, String command, Map<String, Object> extraParams) {
        if (!"YEELIGHT_LAN".equals(device.getProtocolType()) || device.getIpAddress() == null) {
            return Map.of("success", true, "message", "非 Yeelight LAN 设备，已记录指令");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("action", command);
        if (extraParams != null) params.putAll(extraParams);
        return executeYeelightControl(device, params);
    }

    private void updateDeviceStatusFromCommand(Device device, String command) {
        if ("turn_on".equals(command)) {
            device.setStatus(1);
        } else if ("turn_off".equals(command)) {
            device.setStatus(0);
        } else if ("toggle".equals(command)) {
            device.setStatus(device.getStatus() == 1 ? 0 : 1);
        }
        deviceRepository.save(device);
    }

    private void broadcastStatusChange(Device device) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", device.getId());
        data.put("deviceName", device.getDeviceName());
        data.put("status", device.getStatus());
        data.put("brightness", device.getBrightness());
        data.put("colorTemp", device.getColorTemp());
        data.put("rgbColor", device.getRgbColor());
        data.put("onlineStatus", device.getOnlineStatus());
        webSocketHandler.broadcastDeviceStatus("device_status_change", data);
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
        if (device.getBatteryLevel() != null && device.getBatteryLevel() < 20) {
            Long familyId = device.getFamilyId();
            if (familyId == null) {
                return;
            }
            List<User> familyAdmins = userRepository.findByFamilyId(familyId).stream()
                    .filter(u -> "ADMIN".equals(u.getRole()))
                    .toList();
            if (familyAdmins.isEmpty()) {
                return;
            }
            for (User admin : familyAdmins) {
                Notification alert = new Notification();
                alert.setUserId(admin.getId());
                alert.setDeviceId(device.getId());
                alert.setMessageContent("警告：设备 [" + device.getDeviceName() + "] 电量过低，当前仅剩 " + device.getBatteryLevel() + "%！");
                alert.setIsRead(0);
                alert.setCreateTime(LocalDateTime.now());
                notificationService.save(alert);
            }
        }
    }
}
