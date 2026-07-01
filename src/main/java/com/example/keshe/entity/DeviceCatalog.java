package com.example.keshe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_catalog")
public class DeviceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_code", nullable = false, length = 30, unique = true)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 50)
    private String categoryName;

    @Column(name = "category_name_en", length = 50)
    private String categoryNameEn;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "functions_json", columnDefinition = "TEXT")
    private String functionsJson;

    @Column(name = "status_json", columnDefinition = "TEXT")
    private String statusJson;

    @Column(name = "imported_count")
    private Integer importedCount;

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryNameEn() { return categoryNameEn; }
    public void setCategoryNameEn(String categoryNameEn) { this.categoryNameEn = categoryNameEn; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFunctionsJson() { return functionsJson; }
    public void setFunctionsJson(String functionsJson) { this.functionsJson = functionsJson; }

    public String getStatusJson() { return statusJson; }
    public void setStatusJson(String statusJson) { this.statusJson = statusJson; }

    public Integer getImportedCount() { return importedCount; }
    public void setImportedCount(Integer importedCount) { this.importedCount = importedCount; }

    public LocalDateTime getLastSync() { return lastSync; }
    public void setLastSync(LocalDateTime lastSync) { this.lastSync = lastSync; }
}
