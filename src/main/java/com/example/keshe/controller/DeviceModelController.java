package com.example.keshe.controller;

import com.example.keshe.entity.DeviceModel;
import com.example.keshe.service.DeviceModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device-models")
public class DeviceModelController {

    private final DeviceModelService deviceModelService;

    public DeviceModelController(DeviceModelService deviceModelService) {
        this.deviceModelService = deviceModelService;
    }

    /**
     * 模糊搜索设备型号（支持型号、品牌、名称模糊匹配）
     * GET /api/device-models/search?q=yeelight
     * GET /api/device-models/search?q=灯泡&type=Light
     */
    @GetMapping("/search")
    public ResponseEntity<List<DeviceModel>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type) {
        List<DeviceModel> results;
        if (type != null && !type.isEmpty()) {
            results = deviceModelService.searchByType(q, type);
        } else {
            results = deviceModelService.search(q);
        }
        return ResponseEntity.ok(results);
    }

    /**
     * 根据精确型号码查找
     * GET /api/device-models/code/YLDP02YL
     */
    @GetMapping("/code/{modelCode}")
    public ResponseEntity<?> findByModelCode(@PathVariable String modelCode) {
        return deviceModelService.findByModelCode(modelCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 按设备类型查询
     * GET /api/device-models/type/Light
     */
    @GetMapping("/type/{deviceType}")
    public ResponseEntity<List<DeviceModel>> findByType(@PathVariable String deviceType) {
        return ResponseEntity.ok(deviceModelService.findByDeviceType(deviceType));
    }

    /**
     * 按品牌查询
     * GET /api/device-models/brand/Yeelight
     */
    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<DeviceModel>> findByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(deviceModelService.findByBrand(brand));
    }

    /**
     * 型号库统计
     * GET /api/device-models/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(deviceModelService.getStatistics());
    }
}
