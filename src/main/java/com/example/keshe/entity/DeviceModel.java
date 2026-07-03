package com.example.keshe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_model")
public class DeviceModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @Column(name = "model_code", nullable = false, length = 100)
    private String modelCode; // e.g. "YLDP02YL", "MJTD01SYL"

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName; // e.g. "Yeelight 智能彩光灯泡 1S"

    @Column(name = "device_type", nullable = false, length = 30)
    private String deviceType; // Light, Switch, Sensor, Fan, Climate, Cover, Camera, Lock, Robot

    @Column(name = "protocol_type", length = 30)
    private String protocolType; // YEELIGHT_LAN, TUYA_CLOUD, WIFI, ZIGBEE, BLE

    @Column(name = "category_code", length = 30)
    private String categoryCode; // Tuya category code: dj, kg, wsdcg, fs, cl, etc.

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "capabilities_json", columnDefinition = "TEXT")
    private String capabilitiesJson; // JSON: {"power":true,"brightness":true,"colorTemp":true,"rgb":true,"scenes":["reading","movie","night"]}

    @Column(name = "specifications_json", columnDefinition = "TEXT")
    private String specificationsJson; // JSON: {"voltage":"220V","wattage":"8W","colorRange":"2700-6500K"}

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    // Getters and setters for ALL fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getProtocolType() { return protocolType; }
    public void setProtocolType(String protocolType) { this.protocolType = protocolType; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCapabilitiesJson() { return capabilitiesJson; }
    public void setCapabilitiesJson(String capabilitiesJson) { this.capabilitiesJson = capabilitiesJson; }
    public String getSpecificationsJson() { return specificationsJson; }
    public void setSpecificationsJson(String specificationsJson) { this.specificationsJson = specificationsJson; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
