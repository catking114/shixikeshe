package com.example.keshe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_name", nullable = false, length = 50)
    private String deviceName;

    @Column(name = "brand", nullable = false, length = 20)
    private String brand;

    @Column(name = "device_type", nullable = false, length = 20)
    private String deviceType;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "room_location", nullable = false, length = 50)
    private String roomLocation;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "family_id")
    private Long familyId; // 所属家庭ID，实现家庭隔离

    // === IoT 连接字段 ===

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "port")
    private Integer port;

    @Column(name = "protocol_type", length = 30)
    private String protocolType; // YEELIGHT_LAN, TUYA_CLOUD, MQTT, MANUAL

    @Column(name = "external_device_id", length = 100)
    private String externalDeviceId; // 设备在第三方平台的标识

    // === 灯光控制字段 ===

    @Column(name = "brightness")
    private Integer brightness; // 1-100

    @Column(name = "color_temp")
    private Integer colorTemp; // 色温 2700-6500K

    @Column(name = "rgb_color")
    private Integer rgbColor; // RGB 整数值, 如 0xFF0000 = 红色

    @Column(name = "online_status")
    private Integer onlineStatus; // 1=在线, 0=离线, null=未知

    @Column(name = "last_seen")
    private LocalDateTime lastSeen; // 最后一次在线时间

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getProtocolType() { return protocolType; }
    public void setProtocolType(String protocolType) { this.protocolType = protocolType; }

    public String getExternalDeviceId() { return externalDeviceId; }
    public void setExternalDeviceId(String externalDeviceId) { this.externalDeviceId = externalDeviceId; }

    public Integer getBrightness() { return brightness; }
    public void setBrightness(Integer brightness) { this.brightness = brightness; }

    public Integer getColorTemp() { return colorTemp; }
    public void setColorTemp(Integer colorTemp) { this.colorTemp = colorTemp; }

    public Integer getRgbColor() { return rgbColor; }
    public void setRgbColor(Integer rgbColor) { this.rgbColor = rgbColor; }

    public Integer getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(Integer onlineStatus) { this.onlineStatus = onlineStatus; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
