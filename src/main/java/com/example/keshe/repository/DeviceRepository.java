package com.example.keshe.repository;

import com.example.keshe.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByBrand(String brand);

    List<Device> findByRoomLocation(String roomLocation);

    List<Device> findByFamilyId(Long familyId);

    Optional<Device> findByExternalDeviceId(String externalDeviceId);

    @Query("SELECT d FROM Device d WHERE d.familyId = :familyId AND " +
           "(:brand IS NULL OR d.brand = :brand) AND " +
           "(:deviceType IS NULL OR d.deviceType = :deviceType) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:keyword IS NULL OR d.deviceName LIKE %:keyword% OR d.roomLocation LIKE %:keyword%)")
    Page<Device> searchDevices(@Param("familyId") Long familyId,
                               @Param("brand") String brand,
                               @Param("deviceType") String deviceType,
                               @Param("status") Integer status,
                               @Param("keyword") String keyword,
                               Pageable pageable);
}
