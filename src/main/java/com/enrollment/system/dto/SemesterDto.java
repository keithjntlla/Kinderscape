package com.enrollment.system.dto;

import com.enrollment.system.model.Semester;
import java.time.LocalDateTime;

public class SemesterDto {
    
    private Long id;
    private Long schoolYearId;
    private String schoolYear; // e.g., "2025-2026"
    private Integer gradeLevel;
    private Integer semesterNumber;
    private String name; // e.g., "Grade 11 - Semester 1"
    private String displayName; // e.g., "2025-2026 - Grade 11 - Semester 1"
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public SemesterDto() {
    }
    
    // Static factory method
    public static SemesterDto fromSemester(Semester semester) {
        SemesterDto dto = new SemesterDto();
        dto.setId(semester.getId());
        dto.setGradeLevel(semester.getGradeLevel());
        dto.setSemesterNumber(semester.getSemesterNumber());
        dto.setName(semester.getName());
        dto.setIsActive(semester.getIsActive());
        dto.setCreatedAt(semester.getCreatedAt());
        dto.setUpdatedAt(semester.getUpdatedAt());
        
        try {
            if (semester.getSchoolYear() != null) {
                dto.setSchoolYearId(semester.getSchoolYear().getId());
                dto.setSchoolYear(semester.getSchoolYear().getYear());
                // Generate clean display name: "School Year - Semester X" (without grade)
                String schoolYearStr = semester.getSchoolYear().getYear();
                if (semester.getSemesterNumber() != null) {
                    dto.setDisplayName(schoolYearStr + " - Semester " + semester.getSemesterNumber());
                } else {
                    // Fallback: remove grade prefix from semester name
                    String cleanSemesterName = semester.getName().replaceFirst("^Grade \\d+ - ", "");
                    dto.setDisplayName(schoolYearStr + " - " + cleanSemesterName);
                }
            }
        } catch (Exception e) {
            // Handle lazy loading exception
            dto.setSchoolYearId(null);
            dto.setSchoolYear(null);
            // Fallback: just remove grade prefix
            String cleanName = semester.getName() != null ? 
                semester.getName().replaceFirst("^Grade \\d+ - ", "") : "N/A";
            dto.setDisplayName(cleanName);
        }
        
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getSchoolYearId() {
        return schoolYearId;
    }
    
    public void setSchoolYearId(Long schoolYearId) {
        this.schoolYearId = schoolYearId;
    }
    
    public String getSchoolYear() {
        return schoolYear;
    }
    
    public void setSchoolYear(String schoolYear) {
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
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
    
    // Helper method to get clean display name without grade (for table display)
    public String getCleanDisplayName() {
        if (schoolYear != null && semesterNumber != null) {
            return schoolYear + " - Semester " + semesterNumber;
        } else if (schoolYear != null && name != null) {
            // Fallback: remove "Grade XX - " prefix if present
            String cleanName = name.replaceFirst("^Grade \\d+ - ", "");
            return schoolYear + " - " + cleanName;
        } else if (name != null) {
            // Last resort: just remove grade prefix
            return name.replaceFirst("^Grade \\d+ - ", "");
        }
        return "N/A";
    }
}

