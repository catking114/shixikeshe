package com.example.keshe.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family")
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_code", nullable = false, unique = true, length = 8)
    private String familyCode;

    @Column(name = "family_name", length = 50)
    private String familyName;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "creator_role", length = 20)
    private String creatorRole;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFamilyCode() { return familyCode; }
    public void setFamilyCode(String familyCode) { this.familyCode = familyCode; }
    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getCreatorRole() { return creatorRole; }
    public void setCreatorRole(String creatorRole) { this.creatorRole = creatorRole; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
