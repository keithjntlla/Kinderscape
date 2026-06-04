package com.enrollment.system.dto;

import com.enrollment.system.model.Student;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudentDto {
    
    private Long id;
    private String name;
    private LocalDate birthdate;
    private Integer age;
    private String sex;
    private String address;
    private String contactNumber;
    private String parentGuardianName;
    private String parentGuardianContact;
    private String parentGuardianRelationship;
    private Integer gradeLevel;
    private String strand;
    private Long sectionId;
    private String sectionName;
    private String previousSchool;
    private Double gwa;
    private String lrn;
    private String enrollmentStatus;
    private Long schoolYearId;
    private String schoolYear;
    private Long semesterId;
    private String semesterName;
    private String semesterDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isArchived;
    private String archiveReason;
    private LocalDateTime archivedAt;
    private String reEnrollmentReason;
    private Boolean selectedForReEnrollment; // For UI checkbox selection
    
    // Constructors
    public StudentDto() {
    }
    
    // Static factory method
    public static StudentDto fromStudent(Student student) {
        StudentDto dto = new StudentDto();
        dto.setId(student.getId());
        dto.setName(student.getName());
        dto.setBirthdate(student.getBirthdate());
        dto.setAge(student.getAge());
        dto.setSex(student.getSex());
        dto.setAddress(student.getAddress());
        dto.setContactNumber(student.getContactNumber());
        dto.setParentGuardianName(student.getParentGuardianName());
        dto.setParentGuardianContact(student.getParentGuardianContact());
        dto.setParentGuardianRelationship(student.getParentGuardianRelationship());
        dto.setGradeLevel(student.getGradeLevel());
        dto.setStrand(student.getStrand());
        try {
            if (student.getSection() != null) {
                dto.setSectionId(student.getSection().getId());
                dto.setSectionName(student.getSection().getName());
            }
        } catch (Exception e) {
            // Handle lazy loading exception - section not loaded
            dto.setSectionId(null);
            dto.setSectionName(null);
        }
        dto.setPreviousSchool(student.getPreviousSchool());
        dto.setGwa(student.getGwa());
        dto.setLrn(student.getLrn());
        dto.setEnrollmentStatus(student.getEnrollmentStatus());
        try {
            if (student.getSchoolYear() != null) {
                dto.setSchoolYearId(student.getSchoolYear().getId());
                dto.setSchoolYear(student.getSchoolYear().getYear());
            }
        } catch (Exception e) {
            // Handle lazy loading exception - school year not loaded
            dto.setSchoolYearId(null);
            dto.setSchoolYear(null);
        }
        try {
            if (student.getSemester() != null) {
                dto.setSemesterId(student.getSemester().getId());
                dto.setSemesterName(student.getSemester().getName());
                // Generate clean display name: "School Year - Semester X" (without grade)
                String displayName = null;
                if (student.getSchoolYear() != null) {
                    try {
                        String schoolYearStr = student.getSchoolYear().getYear();
                        Integer semesterNum = student.getSemester().getSemesterNumber();
                        if (semesterNum != null) {
                            displayName = schoolYearStr + " - Semester " + semesterNum;
                        } else {
                            // Fallback: remove grade from semester name
                            String semesterName = student.getSemester().getName();
                            String cleanSemesterName = semesterName.replaceFirst("^Grade \\d+ - ", "");
                            displayName = schoolYearStr + " - " + cleanSemesterName;
                        }
                    } catch (Exception e) {
                        // If school year lazy load fails, just use clean semester name
                        String semesterName = student.getSemester().getName();
                        displayName = semesterName != null ? 
                            semesterName.replaceFirst("^Grade \\d+ - ", "") : "N/A";
                    }
                } else {
                    // No school year, just clean semester name
                    String semesterName = student.getSemester().getName();
                    displayName = semesterName != null ? 
                        semesterName.replaceFirst("^Grade \\d+ - ", "") : "N/A";
                }
                dto.setSemesterDisplayName(displayName);
            }
        } catch (Exception e) {
            // Handle lazy loading exception - semester not loaded
            dto.setSemesterId(null);
            dto.setSemesterName(null);
            dto.setSemesterDisplayName(null);
        }
        dto.setCreatedAt(student.getCreatedAt());
        dto.setUpdatedAt(student.getUpdatedAt());
        dto.setIsArchived(student.getIsArchived());
        dto.setArchiveReason(student.getArchiveReason());
        dto.setArchivedAt(student.getArchivedAt());
        dto.setReEnrollmentReason(student.getReEnrollmentReason());
        return dto;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalDate getBirthdate() {
        return birthdate;
    }
    
    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String getSex() {
        return sex;
    }
    
    public void setSex(String sex) {
        this.sex = sex;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getContactNumber() {
        return contactNumber;
    }
    
    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }
    
    public String getParentGuardianName() {
        return parentGuardianName;
    }
    
    public void setParentGuardianName(String parentGuardianName) {
        this.parentGuardianName = parentGuardianName;
    }
    
    public String getParentGuardianContact() {
        return parentGuardianContact;
    }
    
    public void setParentGuardianContact(String parentGuardianContact) {
        this.parentGuardianContact = parentGuardianContact;
    }
    
    public String getParentGuardianRelationship() {
        return parentGuardianRelationship;
    }
    
    public void setParentGuardianRelationship(String parentGuardianRelationship) {
        this.parentGuardianRelationship = parentGuardianRelationship;
    }
    
    public Integer getGradeLevel() {
        return gradeLevel;
    }
    
    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }
    
    public String getStrand() {
        return strand;
    }
    
    public void setStrand(String strand) {
        this.strand = strand;
    }
    
    public Long getSectionId() {
        return sectionId;
    }
    
    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }
    
    public String getSectionName() {
        return sectionName;
    }
    
    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }
    
    public String getPreviousSchool() {
        return previousSchool;
    }
    
    public void setPreviousSchool(String previousSchool) {
        this.previousSchool = previousSchool;
    }
    
    public Double getGwa() {
        return gwa;
    }
    
    public void setGwa(Double gwa) {
        this.gwa = gwa;
    }
    
    public String getLrn() {
        return lrn;
    }
    
    public void setLrn(String lrn) {
        this.lrn = lrn;
    }
    
    public String getEnrollmentStatus() {
        return enrollmentStatus;
    }
    
    public void setEnrollmentStatus(String enrollmentStatus) {
        this.enrollmentStatus = enrollmentStatus;
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
    
    public Long getSemesterId() {
        return semesterId;
    }
    
    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }
    
    public String getSemesterName() {
        return semesterName;
    }
    
    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }
    
    public String getSemesterDisplayName() {
        return semesterDisplayName;
    }
    
    public void setSemesterDisplayName(String semesterDisplayName) {
        this.semesterDisplayName = semesterDisplayName;
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
    
    public Boolean getIsArchived() {
        return isArchived;
    }
    
    public void setIsArchived(Boolean isArchived) {
        this.isArchived = isArchived;
    }
    
    public String getArchiveReason() {
        return archiveReason;
    }
    
    public void setArchiveReason(String archiveReason) {
        this.archiveReason = archiveReason;
    }
    
    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }
    
    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
    
    public Boolean getSelectedForReEnrollment() {
        return selectedForReEnrollment;
    }
    
    public void setSelectedForReEnrollment(Boolean selectedForReEnrollment) {
        this.selectedForReEnrollment = selectedForReEnrollment;
    }
    
    public String getReEnrollmentReason() {
        return reEnrollmentReason;
    }
    
    public void setReEnrollmentReason(String reEnrollmentReason) {
        this.reEnrollmentReason = reEnrollmentReason;
    }
}
