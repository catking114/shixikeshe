package com.example.keshe.service;

import com.example.keshe.entity.DeviceModel;
import com.example.keshe.repository.DeviceModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DeviceModelService {

    private static final Logger log = LoggerFactory.getLogger(DeviceModelService.class);
    private final DeviceModelRepository repository;

    public DeviceModelService(DeviceModelRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        long count = repository.count();
        if (count > 0) {
            log.info("Device model catalog already has {} entries, skipping seed", count);
            return;
        }
        log.info("Seeding device model catalog...");
        List<DeviceModel> models = buildSeedData();
        repository.saveAll(models);
        log.info("Seeded {} device models", models.size());
    }

    public List<DeviceModel> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return repository.findAll();
        }
        return repository.fuzzySearch(query.trim());
    }

    public List<DeviceModel> searchByType(String query, String deviceType) {
        if (query == null || query.trim().isEmpty()) {
            return repository.findByDeviceType(deviceType);
        }
        return repository.fuzzySearchWithType(query.trim(), deviceType);
    }

    public Optional<DeviceModel> findByModelCode(String modelCode) {
        return repository.findByModelCode(modelCode);
    }

    public List<DeviceModel> findByBrand(String brand) {
        return repository.findByBrand(brand);
    }

    public List<DeviceModel> findByDeviceType(String deviceType) {
        return repository.findByDeviceType(deviceType);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", repository.count());
        String[] types = {"Light","Switch","Sensor","Fan","Climate","Cover","Camera","Lock","Robot"};
        for (String t : types) {
            stats.put(t, repository.countByDeviceType(t));
        }
        return stats;
    }

    private List<DeviceModel> buildSeedData() {
        List<DeviceModel> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // ============ Yeelight 灯具系列 ============
        list.add(create("Yeelight","YLDP02YL","Yeelight 智能彩光灯泡 1S","Light","YEELIGHT_LAN","dj",
            "E27 螺口彩光灯泡，支持 1600 万色 + 色温调节",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":true,\"scenes\":[\"reading\",\"movie\",\"night\",\"daylight\"]}",
            "{\"socket\":\"E27\",\"wattage\":\"8W\",\"colorRange\":\"2700-6500K\",\"lumens\":\"800lm\",\"voltage\":\"100-240V\"}", now));
        list.add(create("Yeelight","YLDP13YL","Yeelight LED 智能灯泡 1S","Light","YEELIGHT_LAN","dj",
            "E27 白光/暖光可调灯泡",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":false,\"scenes\":[\"reading\",\"movie\",\"night\"]}",
            "{\"socket\":\"E27\",\"wattage\":\"8W\",\"colorRange\":\"2700-6500K\",\"lumens\":\"800lm\"}", now));
        list.add(create("Yeelight","YLDD04YL","Yeelight 智能彩光灯带 1S","Light","YEELIGHT_LAN","dj",
            "可延长灯带，支持 RGB + 色温",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":true,\"scenes\":[\"reading\",\"movie\",\"night\"]}",
            "{\"length\":\"2m\",\"extendable\":\"可延长至10m\",\"wattage\":\"24W\",\"colorRange\":\"2700-6500K\"}", now));
        list.add(create("Yeelight","YLXD01YL","Yeelight 智能吸顶灯","Light","YEELIGHT_LAN","xdd",
            "客厅/卧室智能吸顶灯",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":false,\"scenes\":[\"reading\",\"movie\",\"night\",\"daylight\"]}",
            "{\"size\":\"圆形\",\"wattage\":\"28W\",\"colorRange\":\"2700-6500K\",\"lumens\":\"2200lm\"}", now));
        list.add(create("Yeelight","MJTD01SYL","Yeelight 屏幕挂灯 Pro","Light","YEELIGHT_LAN","dj",
            "显示器挂灯，非对称光路设计",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":false}",
            "{\"wattage\":\"5W\",\"colorRange\":\"2700-6500K\"}", now));
        list.add(create("Yeelight","YLCT02YL","Yeelight 智能床头灯 2","Light","YEELIGHT_LAN","dj",
            "RGB + 色温床头灯",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":true,\"scenes\":[\"reading\",\"night\"]}",
            "{\"wattage\":\"8W\",\"colorRange\":\"1700-6500K\"}", now));
        list.add(create("Yeelight","YLDP005","Yeelight LED 灯泡 M2","Light","YEELIGHT_LAN","dj",
            "蓝牙 Mesh + Wi-Fi 双模灯泡",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":false}",
            "{\"socket\":\"E27\",\"wattage\":\"8W\",\"colorRange\":\"2700-6500K\",\"protocol\":\"BLE Mesh + WiFi\"}", now));

        // ============ 小米/米家灯具 ============
        list.add(create("小米(Xiaomi)","MJDP08YL","米家 LED 智能灯泡 蓝牙版","Light","YEELIGHT_LAN","dj",
            "米家智能灯泡，蓝牙 Mesh",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":false}",
            "{\"socket\":\"E27\",\"wattage\":\"8W\",\"colorRange\":\"2700-6500K\"}", now));
        list.add(create("小米(Xiaomi)","MJXDD03YL","米家吸顶灯 450","Light","YEELIGHT_LAN","xdd",
            "450mm 直径吸顶灯",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":false}",
            "{\"diameter\":\"450mm\",\"wattage\":\"24W\",\"colorRange\":\"2700-6500K\"}", now));
        list.add(create("小米(Xiaomi)","MJTD04YL","米家台灯 Pro 2","Light","YEELIGHT_LAN","dj",
            "高显色护眼台灯",
            "{\"power\":true,\"brightness\":true,\"colorTemp\":true,\"rgb\":false}",
            "{\"wattage\":\"14W\",\"colorRange\":\"2700-6500K\",\"cri\":\"Ra95\"}", now));

        // ============ 开关系列 ============
        list.add(create("涂鸦(Tuya)","KG-SMART-01","智能单键开关","Switch","TUYA_CLOUD","kg",
            "Wi-Fi 智能墙壁开关，单键",
            "{\"power\":true,\"channels\":1}",
            "{\"voltage\":\"220V\",\"maxLoad\":\"800W\",\"protocol\":\"WiFi 2.4GHz\"}", now));
        list.add(create("涂鸦(Tuya)","KG-SMART-02","智能双键开关","Switch","TUYA_CLOUD","kg",
            "Wi-Fi 智能墙壁开关，双键",
            "{\"power\":true,\"channels\":2}",
            "{\"voltage\":\"220V\",\"maxLoad\":\"800W/ch\",\"protocol\":\"WiFi 2.4GHz\"}", now));
        list.add(create("涂鸦(Tuya)","KG-SMART-03","智能三键开关","Switch","TUYA_CLOUD","kg",
            "Wi-Fi 智能墙壁开关，三键",
            "{\"power\":true,\"channels\":3}",
            "{\"voltage\":\"220V\",\"maxLoad\":\"800W/ch\",\"protocol\":\"WiFi 2.4GHz\"}", now));
        list.add(create("小米(Xiaomi)","DHKG01ZM","米家智能开关 单键","Switch","TUYA_CLOUD","kg",
            "米家 Zigbee 单键开关",
            "{\"power\":true,\"channels\":1}",
            "{\"voltage\":\"220V\",\"protocol\":\"Zigbee 3.0\"}", now));
        list.add(create("小米(Xiaomi)","DHKG02ZM","米家智能开关 双键","Switch","TUYA_CLOUD","kg",
            "米家 Zigbee 双键开关",
            "{\"power\":true,\"channels\":2}",
            "{\"voltage\":\"220V\",\"protocol\":\"Zigbee 3.0\"}", now));

        // ============ 插座系列 ============
        list.add(create("涂鸦(Tuya)","SQ-SMART-01","智能插座 标准版","Switch","TUYA_CLOUD","cz",
            "Wi-Fi 智能插座，支持电量统计",
            "{\"power\":true,\"energyMonitor\":true}",
            "{\"voltage\":\"220V\",\"maxLoad\":\"2500W\",\"protocol\":\"WiFi 2.4GHz\"}", now));
        list.add(create("涂鸦(Tuya)","SQ-SMART-02","智能排插 3位","Switch","TUYA_CLOUD","cz",
            "Wi-Fi 智能排插，3 个独立控制位",
            "{\"power\":true,\"channels\":3,\"energyMonitor\":true}",
            "{\"voltage\":\"220V\",\"maxLoad\":\"2500W\",\"usbPorts\":3,\"protocol\":\"WiFi 2.4GHz\"}", now));
        list.add(create("小米(Xiaomi)","ZNCZ02LM","米家智能插座 基础版","Switch","TUYA_CLOUD","cz",
            "米家 Zigbee 智能插座",
            "{\"power\":true,\"energyMonitor\":false}",
            "{\"voltage\":\"220V\",\"maxLoad\":\"2200W\",\"protocol\":\"Zigbee\"}", now));
        list.add(create("小米(Xiaomi)","ZNCZ04LM","米家智能插座 增强版","Switch","TUYA_CLOUD","cz",
            "支持电量统计的智能插座",
            "{\"power\":true,\"energyMonitor\":true}",
            "{\"voltage\":\"220V\",\"maxLoad\":\"2500W\",\"protocol\":\"Zigbee 3.0\"}", now));

        // ============ 传感器系列 ============
        list.add(create("涂鸦(Tuya)","WS-TH-01","温湿度传感器","Sensor","TUYA_CLOUD","wsdcg",
            "室内温湿度传感器，LCD 显示",
            "{\"temperature\":true,\"humidity\":true,\"battery\":true}",
            "{\"tempRange\":\"-10~60\u00b0C\",\"humidityRange\":\"0~99%RH\",\"battery\":\"CR2032\",\"protocol\":\"Zigbee 3.0\"}", now));
        list.add(create("涂鸦(Tuya)","PIR-SMART-01","人体红外传感器","Sensor","TUYA_CLOUD","pir",
            "人体移动检测传感器",
            "{\"motionDetect\":true,\"battery\":true}",
            "{\"detectAngle\":\"120\u00b0\",\"detectRange\":\"7m\",\"battery\":\"CR2450\",\"protocol\":\"Zigbee 3.0\"}", now));
        list.add(create("涂鸦(Tuya)","MC-SMART-01","门窗磁传感器","Sensor","TUYA_CLOUD","mcs",
            "门窗开合检测传感器",
            "{\"openClose\":true,\"battery\":true}",
            "{\"battery\":\"CR2032\",\"protocol\":\"Zigbee 3.0\"}", now));
        list.add(create("小米(Xiaomi)","LYWSD03MMC","米家蓝牙温湿度计 2","Sensor","TUYA_CLOUD","wsdcg",
            "小型蓝牙温湿度计",
            "{\"temperature\":true,\"humidity\":true,\"battery\":true}",
            "{\"tempRange\":\"-10~60\u00b0C\",\"humidityRange\":\"0~99%RH\",\"battery\":\"CR2032\"}", now));
        list.add(create("小米(Xiaomi)","RTCGQ02LM","米家人体传感器 2","Sensor","TUYA_CLOUD","pir",
            "人体移动 + 光照传感器",
            "{\"motionDetect\":true,\"lightLevel\":true,\"battery\":true}",
            "{\"detectAngle\":\"170\u00b0\",\"detectRange\":\"7m\",\"battery\":\"CR2450\"}", now));
        list.add(create("涂鸦(Tuya)","GAS-ALARM-01","智能燃气报警器","Sensor","TUYA_CLOUD","rqbj",
            "天然气泄漏检测报警器",
            "{\"gasDetect\":true,\"alarm\":true}",
            "{\"gasType\":\"CH4\",\"alarmDb\":\"85dB\",\"voltage\":\"220V\"}", now));
        list.add(create("涂鸦(Tuya)","SMOKE-ALARM-01","智能烟雾报警器","Sensor","TUYA_CLOUD","wgbj",
            "烟雾检测报警器",
            "{\"smokeDetect\":true,\"alarm\":true,\"battery\":true}",
            "{\"battery\":\"CR123A\",\"alarmDb\":\"85dB\",\"protocol\":\"WiFi\"}", now));

        // ============ 风扇系列 ============
        list.add(create("涂鸦(Tuya)","FS-SMART-01","智能落地扇","Fan","TUYA_CLOUD","fs",
            "Wi-Fi 智能落地扇",
            "{\"power\":true,\"speed\":true,\"mode\":[\"normal\",\"natural\",\"sleep\"],\"oscillation\":true,\"timer\":true}",
            "{\"speedLevels\":12,\"voltage\":\"220V\",\"protocol\":\"WiFi 2.4GHz\"}", now));
        list.add(create("小米(Xiaomi)","BPLDS01DM","米家直流变频落地扇 1X","Fan","TUYA_CLOUD","fs",
            "直流变频落地扇",
            "{\"power\":true,\"speed\":true,\"mode\":[\"normal\",\"natural\",\"sleep\"],\"oscillation\":true,\"timer\":true}",
            "{\"speedLevels\":100,\"voltage\":\"220V\",\"wattage\":\"15W\"}", now));
        list.add(create("涂鸦(Tuya)","CF-SMART-01","智能吊扇灯","Fan","TUYA_CLOUD","fs",
            "吊扇 + 灯一体化智能设备",
            "{\"power\":true,\"speed\":true,\"lightPower\":true,\"lightBrightness\":true,\"timer\":true}",
            "{\"fanLevels\":6,\"wattage\":\"36W fan + 24W light\",\"protocol\":\"WiFi\"}", now));

        // ============ 空调/暖通系列 ============
        list.add(create("涂鸦(Tuya)","AC-SMART-01","智能空调伴侣","Climate","TUYA_CLOUD","kt",
            "红外遥控空调伴侣",
            "{\"power\":true,\"temperature\":true,\"mode\":[\"cool\",\"heat\",\"auto\",\"fan\",\"dry\"],\"fanSpeed\":[\"auto\",\"low\",\"mid\",\"high\"]}",
            "{\"tempRange\":\"16-30\u00b0C\",\"protocol\":\"WiFi + IR\"}", now));
        list.add(create("涂鸦(Tuya)","QN-SMART-01","智能取暖器","Climate","TUYA_CLOUD","qn",
            "Wi-Fi 智能电暖器",
            "{\"power\":true,\"temperature\":true,\"mode\":[\"low\",\"high\",\"auto\"],\"timer\":true}",
            "{\"wattage\":\"2000W\",\"tempRange\":\"5-35\u00b0C\",\"protocol\":\"WiFi\"}", now));
        list.add(create("涂鸦(Tuya)","CS-SMART-01","智能除湿机","Climate","TUYA_CLOUD","cs",
            "智能除湿机",
            "{\"power\":true,\"humidity\":true,\"mode\":[\"auto\",\"manual\",\"dry_clothes\"],\"timer\":true}",
            "{\"dehumidify\":\"12L/day\",\"humidityRange\":\"30-80%\",\"protocol\":\"WiFi\"}", now));
        list.add(create("涂鸦(Tuya)","JSQ-SMART-01","智能加湿器","Climate","TUYA_CLOUD","jsq",
            "超声波智能加湿器",
            "{\"power\":true,\"humidity\":true,\"mistLevel\":true,\"timer\":true,\"light\":true}",
            "{\"capacity\":\"4L\",\"mistLevels\":3,\"protocol\":\"WiFi\"}", now));
        list.add(create("涂鸦(Tuya)","KJ-SMART-01","智能空气净化器","Climate","TUYA_CLOUD","kj",
            "智能空气净化器",
            "{\"power\":true,\"mode\":[\"auto\",\"manual\",\"sleep\"],\"fanSpeed\":true,\"timer\":true}",
            "{\"cadr\":\"400m\u00b3/h\",\"coverage\":\"28-48m\u00b2\",\"protocol\":\"WiFi\"}", now));

        // ============ 窗帘/遮阳系列 ============
        list.add(create("涂鸦(Tuya)","CL-SMART-01","智能窗帘电机","Cover","TUYA_CLOUD","cl",
            "Wi-Fi 智能窗帘电机轨道",
            "{\"open\":true,\"close\":true,\"stop\":true,\"position\":true}",
            "{\"trackLength\":\"可定制\",\"voltage\":\"220V\",\"protocol\":\"WiFi\"}", now));
        list.add(create("涂鸦(Tuya)","CL-SMART-02","智能卷帘电机","Cover","TUYA_CLOUD","cl",
            "电池供电智能卷帘电机",
            "{\"open\":true,\"close\":true,\"stop\":true,\"position\":true}",
            "{\"battery\":\"2200mAh\",\"protocol\":\"Zigbee 3.0\"}", now));
        list.add(create("小米(Xiaomi)","ZNCLDJ11LM","米家智能窗帘 锂电版","Cover","TUYA_CLOUD","cl",
            "锂电池供电智能窗帘",
            "{\"open\":true,\"close\":true,\"stop\":true,\"position\":true}",
            "{\"battery\":\"6200mAh\",\"protocol\":\"BLE Mesh\"}", now));

        // ============ 扫地机器人 ============
        list.add(create("涂鸦(Tuya)","SD-SMART-01","智能扫地机器人","Robot","TUYA_CLOUD","sd",
            "激光导航扫地机器人",
            "{\"power\":true,\"mode\":[\"auto\",\"spot\",\"edge\",\"zone\"],\"returnDock\":true,\"fanSpeed\":[\"quiet\",\"normal\",\"strong\"]}",
            "{\"suction\":\"2000Pa\",\"battery\":\"5200mAh\",\"coverage\":\"200m\u00b2\"}", now));

        return list;
    }

    private DeviceModel create(String brand, String modelCode, String productName,
                               String deviceType, String protocolType, String categoryCode,
                               String description, String capabilities, String specs, LocalDateTime now) {
        DeviceModel m = new DeviceModel();
        m.setBrand(brand);
        m.setModelCode(modelCode);
        m.setProductName(productName);
        m.setDeviceType(deviceType);
        m.setProtocolType(protocolType);
        m.setCategoryCode(categoryCode);
        m.setDescription(description);
        m.setCapabilitiesJson(capabilities);
        m.setSpecificationsJson(specs);
        m.setCreateTime(now);
        return m;
    }
}
