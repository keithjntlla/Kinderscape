package com.enrollment.system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "semesters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"school_year_id", "grade_level", "semester_number"})
})
public class Semester {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id", nullable = false)
    private SchoolYear schoolYear;
    
    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel; // 11 or 12
    
    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber; // 1 or 2
    
    @Column(name = "name", length = 100)
    private String name; // e.g., "Grade 11 - Semester 1"
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Semester() {
    }
    
    public Semester(SchoolYear schoolYear, Integer gradeLevel, Integer semesterNumber) {
        this.schoolYear = schoolYear;
        this.gradeLevel = gradeLevel;
        this.semesterNumber = semesterNumber;
        this.name = "Grade " + gradeLevel + " - Semester " + semesterNumber;
        this.isActive = true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public SchoolYear getSchoolYear() {
        return schoolYear;
    }
    
    public void setSchoolYear(SchoolYear schoolYear) {
        this.schoolYear = schoolYear;
    }
    
    public Integer getGradeLevel() {
        return gradeLevel;
    }
    
    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }
    
    public Integer getSemesterNumber() {
        return semesterNumber;
    }
    
    public void setSemesterNumber(Integer semesterNumber) {
        this.semesterNumber = semesterNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // JPA Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Auto-generate name if not set
        if (name == null || name.isEmpty()) {
            name = "Grade " + gradeLevel + " - Semester " + semesterNumber;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Auto-generate name if not set
        if (name == null || name.isEmpty()) {
            name = "Grade " + gradeLevel + " - Semester " + semesterNumber;
        }
    }
}

