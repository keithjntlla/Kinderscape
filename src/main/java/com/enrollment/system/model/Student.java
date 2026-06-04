package com.enrollment.system.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "birthdate")
    private LocalDate birthdate;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "sex", length = 10)
    private String sex;
    
    @Column(name = "address", length = 255)
    private String address;
    
    @Column(name = "contact_number", length = 20)
    private String contactNumber;
    
    @Column(name = "parent_guardian_name", length = 100)
    private String parentGuardianName;
    
    @Column(name = "parent_guardian_contact", length = 20)
    private String parentGuardianContact;
    
    @Column(name = "parent_guardian_relationship", length = 50)
    private String parentGuardianRelationship;
    
    @Column(name = "grade_level")
    private Integer gradeLevel;
    
    @Column(name = "strand", length = 50)
    private String strand;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_year_id")
    private SchoolYear schoolYear;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;
    
    @Column(name = "previous_school", length = 200)
    private String previousSchool;
    
    @Column(name = "gwa")
    private Double gwa;
    
    @Column(name = "lrn", length = 20)
    private String lrn;
    
    @Column(name = "enrollment_status", length = 50)
    private String enrollmentStatus;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_archived")
    private Boolean isArchived = false;
    
    @Column(name = "archive_reason", length = 100)
    private String archiveReason;
    
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
    
    @Column(name = "re_enrollment_reason", length = 255)
    private String reEnrollmentReason;
    
    // Constructors
    public Student() {
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
    
    public Section getSection() {
        return section;
    }
    
    public void setSection(Section section) {
        this.section = section;
    }
    
    public SchoolYear getSchoolYear() {
        return schoolYear;
    }
    
    public void setSchoolYear(SchoolYear schoolYear) {
        this.schoolYear = schoolYear;
    }
    
    public Semester getSemester() {
        return semester;
    }
    
    public void setSemester(Semester semester) {
        this.semester = semester;
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
    
    public String getReEnrollmentReason() {
        return reEnrollmentReason;
    }
    
    public void setReEnrollmentReason(String reEnrollmentReason) {
        this.reEnrollmentReason = reEnrollmentReason;
    }
    
    // JPA Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

