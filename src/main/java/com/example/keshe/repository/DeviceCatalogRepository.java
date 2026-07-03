package com.example.keshe.repository;

import com.example.keshe.entity.DeviceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceCatalogRepository extends JpaRepository<DeviceCatalog, Long> {

    Optional<DeviceCatalog> findByCategoryCode(String categoryCode);

    boolean existsByCategoryCode(String categoryCode);
}
