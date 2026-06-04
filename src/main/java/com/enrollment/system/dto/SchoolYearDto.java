package com.enrollment.system.dto;

import com.enrollment.system.model.SchoolYear;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SchoolYearDto {
    
    private Long id;
    private String year;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public SchoolYearDto() {
    }
    
    // Static factory method
    public static SchoolYearDto fromSchoolYear(SchoolYear schoolYear) {
        SchoolYearDto dto = new SchoolYearDto();
        dto.setId(schoolYear.getId());
        dto.setYear(schoolYear.getYear());
        dto.setStartDate(schoolYear.getStartDate());
        dto.setEndDate(schoolYear.getEndDate());
        dto.setIsCurrent(schoolYear.getIsCurrent());
        dto.setCreatedAt(schoolYear.getCreatedAt());
        dto.setUpdatedAt(schoolYear.getUpdatedAt());
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getYear() {
        return year;
    }
    
    public void setYear(String year) {
        this.year = year;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Boolean getIsCurrent() {
        return isCurrent;
    }
    
    public void setIsCurrent(Boolean isCurrent) {
        this.isCurrent = isCurrent;
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
}

