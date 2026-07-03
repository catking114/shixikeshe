package com.example.keshe.repository;

import com.example.keshe.entity.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceModelRepository extends JpaRepository<DeviceModel, Long> {

    Optional<DeviceModel> findByModelCode(String modelCode);
    List<DeviceModel> findByDeviceType(String deviceType);
    List<DeviceModel> findByBrand(String brand);
    List<DeviceModel> findByProtocolType(String protocolType);

    @Query("SELECT d FROM DeviceModel d WHERE " +
           "LOWER(d.modelCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(d.productName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(d.brand) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<DeviceModel> fuzzySearch(@Param("q") String query);

    @Query("SELECT d FROM DeviceModel d WHERE d.deviceType = :type AND (" +
           "LOWER(d.modelCode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(d.productName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(d.brand) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<DeviceModel> fuzzySearchWithType(@Param("q") String query, @Param("type") String deviceType);

    long countByDeviceType(String deviceType);
}
